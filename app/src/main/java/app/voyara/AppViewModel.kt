package app.voyara

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class Mode { Teleport, Route }

enum class MapLayer { Mono, Street, Satellite, Terrain }

enum class ThemeMode { System, Light, Dark }

data class CameraMove(val at: Geo, val zoom: Double? = null)

/** A snackbar message; errors stay on screen until dismissed. */
data class Message(val text: String, val error: Boolean = false)

private const val TAG = "Voyara"

/** Small access-ordered cache that forgets its least recently used entry past [max]. */
private class Lru<K, V>(private val max: Int) : LinkedHashMap<K, V>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > max
}

class AppViewModel(private val app: Application) : AndroidViewModel(app) {
    val store = Store(app)

    var mode by mutableStateOf(Mode.Teleport)

    /** Camera target once the map settles; biases search and measures distances. */
    var center by mutableStateOf<Geo?>(null)
        private set

    /** The place the user picked: tap or long-press the map, drag the pin, or choose a result. */
    var pin by mutableStateOf<Place?>(null)
        private set
    var pinLoading by mutableStateOf(false)
        private set
    private var lookup: Job? = null

    /** The device's real position (last known while mocking, since mocking hides the live one). */
    var you by mutableStateOf(store.realLocation)
        private set
    private val fused = LocationServices.getFusedLocationProviderClient(app)
    private val realFixes = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::onRealFix)
        }
    }

    var query by mutableStateOf("")
    var results by mutableStateOf<List<Place>>(emptyList())
        private set
    var searching by mutableStateOf(false)
        private set

    val stops = mutableStateListOf<Geo>()

    /** An imported GPX track; replaces the stop-to-stop route while set. */
    var track by mutableStateOf<List<Geo>?>(null)
        private set

    /** Preview path through [stops] (or the [track]): straight lines, replaced by roads once routed. */
    var route by mutableStateOf<List<Geo>>(emptyList())
        private set
    var routing by mutableStateOf(false)
        private set

    var speedKmh by mutableDoubleStateOf(store.speedKmh)
        private set
    var followRoads by mutableStateOf(store.followRoads)
        private set
    var loop by mutableStateOf(store.loop)
        private set
    var drift by mutableStateOf(store.drift)
        private set
    var roam by mutableStateOf(store.roam)
        private set
    var roamRadius by mutableDoubleStateOf(store.roamRadius)
        private set
    var mapLayer by mutableStateOf(store.mapLayer)
        private set
    var traffic by mutableStateOf(store.traffic)
        private set
    var theme by mutableStateOf(store.theme)
        private set

    /** TomTom maps (satellite, terrain, traffic) need a key; without one we use OpenFreeMap. */
    val tomtom = BuildConfig.TOMTOM_API_KEY.isNotBlank()

    var mockReady by mutableStateOf(true)
        private set
    var devOptionsOn by mutableStateOf(true)
        private set

    val hasSavedCamera = store.camera != null
    val initialCamera = Sim.live.value?.let { CameraMove(it.at, 16.0) }
        ?: store.camera
        ?: CameraMove(Geo(25.0, 10.0), 1.5)
    private var lastCamera: CameraMove? = null

    val camera = Channel<CameraMove>(Channel.CONFLATED)
    val messages = Channel<Message>(Channel.BUFFERED)

    // Network answers are kept for the session, so repeating a search, re-dropping a pin nearby
    // or toggling a route option back never costs a second request.
    private val searchCache = Lru<String, List<Place>>(40)
    private val placeCache = Lru<String, Place>(200)
    private val routeCache = Lru<RouteKey, List<Geo>>(20)

    private data class RouteKey(
        val stops: List<Geo>,
        val track: List<Geo>?,
        val roads: Boolean,
        val loop: Boolean,
        val profile: String,
    )

    init {
        Sim.speedKmh.value = speedKmh
        Sim.drift.value = drift
        viewModelScope.launch { snapshotFlow { query.trim() }.collectLatest(::search) }
        viewModelScope.launch {
            snapshotFlow { RouteKey(stops.toList(), track, followRoads, loop, Net.profileFor(speedKmh)) }
                .collectLatest(::buildRoute)
        }
        viewModelScope.launch { snapshotFlow { store.favorites.toList() }.collect { publishShortcuts(app, it) } }
        viewModelScope.launch { for (e in Sim.errors) messages.send(Message(e, error = true)) }
    }

    fun onCameraIdle(at: Geo, zoom: Double) {
        lastCamera = CameraMove(at, zoom)
        center = at
    }

    /** Saves the camera once, when the app leaves the screen, instead of on every pan. */
    fun persist() {
        lastCamera?.let { store.camera = it }
    }

    fun warmUpSearch() {
        viewModelScope.launch { Net.warmUp(Net.PHOTON) }
    }

    fun warmUpRouting() {
        viewModelScope.launch { Net.warmUp(Net.ROUTING) }
    }

    /**
     * Drops the pin at [at]. A known [place] (search result, favorite) is shown as is; otherwise
     * the address is looked up once, here, rather than on every pan of the map.
     */
    fun dropPin(at: Geo, place: Place? = null) {
        lookup?.cancel()
        pinLoading = false
        if (place != null) {
            pin = place.copy(at = at)
            return
        }
        val key = String.format(Locale.US, "%.4f,%.4f", at.lat, at.lng) // ~11 m cells
        placeCache[key]?.let {
            pin = it.copy(at = at)
            return
        }
        pin = Place("", "", at)
        pinLoading = true
        lookup = viewModelScope.launch {
            val found = Net.reverse(app, at)
            if (found != null) placeCache[key] = found
            pin = (found ?: Place("", "", at)).copy(at = at)
            pinLoading = false
        }
    }

    fun clearPin() {
        lookup?.cancel()
        pin = null
        pinLoading = false
    }

    private suspend fun search(q: String) {
        searching = false
        if (q.length < 2) {
            results = emptyList()
            return
        }
        parseCoords(q)?.let {
            results = listOf(Place("Go to coordinates", it.pretty(), it))
            return
        }
        // Saved places match instantly; network results are appended as each source answers.
        val needle = q.lowercase()
        val local = (store.favorites + store.recents)
            .filter { needle in it.name.lowercase() || needle in it.detail.lowercase() }
            .distinctBy { it.at }
            .take(3)
        // Results are biased to the area on screen, so the cache key includes a ~10 km cell.
        val area = center?.let { String.format(Locale.US, "%.1f,%.1f", it.lat, it.lng) }.orEmpty()
        val key = "$needle@$area"
        searchCache[key]?.let {
            results = merge(local, it)
            return
        }
        results = local
        delay(400)
        searching = true
        var failures = 0
        val fetched = mutableListOf<Place>()
        coroutineScope {
            listOf<suspend () -> List<Place>>({ Net.geocode(app, q) }, { Net.search(q, center) }).forEach { source ->
                launch {
                    try {
                        val more = source()
                        fetched += more
                        results = merge(results, more)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "search source failed", e)
                        failures++
                    }
                }
            }
        }
        searching = false
        if (failures < 2) searchCache[key] = merge(emptyList(), fetched)
        if (failures == 2 && results.isEmpty()) warn("Search failed. Check your connection and try again.")
    }

    private fun merge(current: List<Place>, more: List<Place>): List<Place> = current + more.filter { m ->
        current.none { distance(it.at, m.at) < 150 && (it.name.contains(m.name, true) || m.name.contains(it.name, true)) }
    }

    private suspend fun buildRoute(k: RouteKey) {
        k.track?.let {
            route = if (k.loop) it + it.first() else it
            return
        }
        val path = if (k.loop && k.stops.size >= 2) k.stops + k.stops.first() else k.stops
        route = path
        if (!k.roads || path.size < 2) return
        routeCache[k]?.let {
            route = it
            return
        }
        delay(300)
        routing = true
        try {
            route = Net.route(path, k.profile).also { routeCache[k] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "routing failed", e)
            say("No road route found — using straight lines")
        } finally {
            routing = false
        }
    }

    private fun startAt(at: Geo) {
        MockService.start(app, if (roam) Plan.Roam(at, roamRadius) else Plan.Hold(at))
    }

    fun teleport() {
        val p = pin ?: return
        startAt(p.at)
        store.addRecent(Place(p.name.ifBlank { "Dropped pin" }, p.detail.ifBlank { p.at.pretty() }, p.at))
    }

    /** From a launcher shortcut: jump straight there when setup allows it. */
    fun teleportTo(at: Geo) {
        mode = Mode.Teleport
        val favorite = store.favorites.firstOrNull { distance(it.at, at) < 15 }
        dropPin(at, favorite)
        flyTo(at, 16.0)
        if (isMockApp(app) && hasLocationPermission(app)) {
            startAt(at)
            // Recents feed the Quick Settings tile, which resumes the latest place.
            store.addRecent(favorite ?: Place("Shortcut", at.pretty(), at))
        } else {
            say("Finish setup, then tap Start")
        }
    }

    fun startRoute() {
        if (route.size < 2) return
        Sim.joystick.value = false
        MockService.start(app, Plan.Route(Path(route), loop))
    }

    fun stop() = MockService.stop()

    fun togglePause() {
        Sim.paused.value = !Sim.paused.value
        Sim.poke()
    }

    fun toggleJoystick() {
        Sim.joystick.value = !Sim.joystick.value
        if (Sim.joystick.value && roam) toggleRoam()
    }

    fun toggleRoam() {
        roam = !roam
        store.roam = roam
        if (roam) Sim.joystick.value = false
        // Apply to a running session right away.
        val here = Sim.live.value?.at
        when (val p = Sim.plan.value) {
            is Plan.Hold -> if (roam) MockService.start(app, Plan.Roam(here ?: p.at, roamRadius))
            is Plan.Roam -> if (!roam) MockService.start(app, Plan.Hold(here ?: p.center))
            else -> Unit
        }
    }

    fun roamWithin(meters: Double) {
        roamRadius = meters
        store.roamRadius = meters
        (Sim.plan.value as? Plan.Roam)?.let { MockService.start(app, it.copy(radius = meters)) }
    }

    fun addStop(at: Geo) {
        if (track != null) clearStops()
        stops += at
    }

    fun undoStop() {
        if (track != null) clearStops() else stops.removeLastOrNull()
    }

    fun clearStops() {
        track = null
        stops.clear()
    }

    fun importGpx(uri: Uri) {
        viewModelScope.launch {
            val points = withContext(Dispatchers.IO) {
                runCatching { app.contentResolver.openInputStream(uri)?.use(::parseGpx) }
                    .onFailure { Log.w(TAG, "GPX import failed", it) }
                    .getOrNull().orEmpty()
            }
            if (points.size < 2) {
                warn("No track found in that file")
                return@launch
            }
            track = points
            stops.clear()
            stops += listOf(points.first(), points.last())
            mode = Mode.Route
            flyTo(points.first(), 15.0)
            say("Loaded ${points.size} track points · ${formatDistance(Path(points).length)}")
        }
    }

    fun setSpeed(kmh: Double) {
        speedKmh = kmh
        store.speedKmh = kmh
        Sim.speedKmh.value = kmh
    }

    fun toggleRoads() {
        followRoads = !followRoads
        store.followRoads = followRoads
    }

    fun toggleLoop() {
        loop = !loop
        store.loop = loop
    }

    fun toggleDrift() {
        drift = !drift
        store.drift = drift
        Sim.drift.value = drift
    }

    fun toggleFavorite() {
        val p = pin ?: return
        store.toggleFavorite(Place(p.name.ifBlank { "Saved place" }, p.detail.ifBlank { p.at.pretty() }, p.at))
    }

    /** Picks a place from search or the saved list: pin it and fly there. */
    fun fly(place: Place) {
        mode = Mode.Teleport
        dropPin(place.at, place)
        camera.trySend(CameraMove(place.at, place.zoom))
        query = ""
    }

    fun flyTo(at: Geo, zoom: Double? = null) {
        camera.trySend(CameraMove(at, zoom))
    }

    fun say(message: String) {
        messages.trySend(Message(message))
    }

    fun warn(message: String) {
        messages.trySend(Message(message, error = true))
    }

    fun setLayer(layer: MapLayer) {
        mapLayer = layer
        store.mapLayer = layer
    }

    fun setThemeMode(mode: ThemeMode) {
        theme = mode
        store.theme = mode
    }

    fun toggleTraffic() {
        traffic = !traffic
        store.traffic = traffic
    }

    fun refreshSetup() {
        mockReady = isMockApp(app)
        devOptionsOn = Settings.Global.getInt(app.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
    }

    private fun onRealFix(loc: Location) {
        // While we mock, every provider reports the fake position, and flags it as mock.
        val mock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) loc.isMock else @Suppress("DEPRECATION") loc.isFromMockProvider
        if (mock || Sim.plan.value != null) return
        val at = Geo(loc.latitude, loc.longitude)
        if (you?.let { distance(it, at) < 10 } == true) return
        you = at
        store.realLocation = at
    }

    /** Follows the real position while the app is on screen (every ~10 s). */
    @SuppressLint("MissingPermission") // guarded by hasLocationPermission
    fun trackYou(on: Boolean) {
        if (on && hasLocationPermission(app)) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000)
                .setMinUpdateIntervalMillis(5_000)
                .build()
            fused.requestLocationUpdates(request, realFixes, Looper.getMainLooper())
        } else {
            fused.removeLocationUpdates(realFixes)
        }
    }

    /** Centres the map on the real position. */
    @SuppressLint("MissingPermission") // callers check location permission
    fun focusYou() {
        trackYou(true)
        val mocking = Sim.plan.value != null
        you?.let {
            flyTo(it, 16.0)
            if (mocking) say("Your last real location. Mocking hides the live one.")
            return
        }
        if (mocking) {
            warn("Your real location isn't known yet. Stop mocking to find it.")
            return
        }
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc == null) {
                    warn("Couldn't get your location")
                } else {
                    onRealFix(loc)
                    flyTo(Geo(loc.latitude, loc.longitude), 16.0)
                }
            }
            .addOnFailureListener { warn("Couldn't get your location") }
    }

    override fun onCleared() {
        fused.removeLocationUpdates(realFixes)
    }

    /** Handles geo: links, pasted text and shares from other apps (e.g. a Google Maps link). */
    fun openShared(text: String) {
        viewModelScope.launch {
            val at = Net.resolveCoords(text)
            if (at == null) {
                warn("No coordinates found in that text")
            } else {
                mode = Mode.Teleport
                dropPin(at)
                flyTo(at, 16.0)
            }
        }
    }
}

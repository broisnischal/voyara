package app.voyara.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.IntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.voyara.BuildConfig
import app.voyara.CameraMove
import app.voyara.Geo
import app.voyara.MapLayer
import app.voyara.Net
import app.voyara.distance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import java.io.File
import kotlin.math.roundToInt

// Free OpenStreetMap tiles, used when the build has no TomTom key.
private const val OFM = "https://tiles.openfreemap.org/styles/"
private const val TOMTOM_STYLE = "https://api.tomtom.com/maps/orbis/assets/styles/0.*/style.json?apiVersion=1&key="
private const val SRC_ROUTE = "voyara-route"
private const val SRC_STOPS = "voyara-stops"
private const val SRC_LIVE = "voyara-live"
private const val SRC_YOU = "voyara-you"
private const val EMPTY = """{"type":"FeatureCollection","features":[]}"""
private const val GLIDE_NANOS = 240_000_000.0 // a bit longer than the 200 ms fix interval

private val Geo.latLng get() = LatLng(lat, lng)

/** ARGB colors for the overlays, taken from the theme's roles. */
data class MapColors(
    val route: Int,
    val casing: Int,
    val onRoute: Int,
    val live: Int,
    val liveRing: Int,
    val you: Int,
    val chrome: Int,
)

/**
 * TomTom Orbis style for [layer]; OpenFreeMap when there is no key (street map only).
 * TomTom embeds the key in every tile, glyph and sprite URL of the style it returns.
 */
private fun styleUrl(layer: MapLayer, traffic: Boolean, dark: Boolean): String {
    if (BuildConfig.TOMTOM_API_KEY.isBlank()) {
        return OFM + when {
            dark -> "dark"
            layer == MapLayer.Mono -> "positron"
            else -> "liberty"
        }
    }
    val theme = if (dark) "dark" else "light"
    return buildString {
        append(TOMTOM_STYLE).append(BuildConfig.TOMTOM_API_KEY)
        append(
            when (layer) {
                MapLayer.Mono -> "&map=basic_mono-$theme"
                MapLayer.Satellite -> "&map=basic_street-satellite"
                MapLayer.Street, MapLayer.Terrain -> "&map=basic_street-$theme"
            },
        )
        if (layer == MapLayer.Terrain) append("&hillshade=hillshade_$theme")
        if (traffic) append("&trafficFlow=flow_relative-$theme&trafficIncidents=incidents_$theme")
    }
}

private const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L
private const val MAX_TILE_ZOOM = 18

private var tileCacheInstalled = false

/**
 * TomTom bills per tile request. Keep tiles fresh for a week instead of the one day TomTom's
 * headers allow, so revisited areas are served from the 256 MB on-device cache instead of being
 * re-requested. Traffic tiles are left alone; they must stay live.
 */
private fun installTileCache(context: Context) {
    if (tileCacheInstalled) return
    tileCacheInstalled = true
    OfflineManager.getInstance(context).setMaximumAmbientCacheSize(
        256L * 1024 * 1024,
        object : OfflineManager.FileSourceCallback {
            override fun onSuccess() = Unit
            override fun onError(message: String) = Unit
        },
    )
    val client = OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply { maxRequestsPerHost = 20 }) // MapLibre's default parallelism
        .addNetworkInterceptor { chain ->
            val response = chain.proceed(chain.request())
            val url = chain.request().url
            if (url.host.endsWith("tomtom.com") && "/map-display/tile/" in url.encodedPath) {
                response.newBuilder().header("Cache-Control", "max-age=604800").removeHeader("Expires").build()
            } else {
                response
            }
        }
        .build()
    HttpRequestUtil.setOkHttpClient(client)
}

/**
 * The style document, cached on disk for a week, with vector tiles capped at zoom 18: deeper
 * zooms stretch z18 tiles (their data is already complete) instead of fetching four times as many.
 */
private suspend fun loadStyle(context: Context, url: String): Style.Builder = withContext(Dispatchers.IO) {
    if ("api.tomtom.com" !in url) return@withContext Style.Builder().fromUri(url)
    val file = File(context.cacheDir, "style-${url.hashCode().toUInt().toString(16)}.json")
    val fresh = file.exists() && System.currentTimeMillis() - file.lastModified() < WEEK_MS
    val json = (if (fresh) file.readText() else null)
        ?: runCatching { capTileZoom(Net.fetch(url)).also(file::writeText) }.getOrNull()
        ?: file.takeIf { it.exists() }?.readText() // a stale style beats none when offline
    if (json == null) Style.Builder().fromUri(url) else Style.Builder().fromJson(json)
}

private fun capTileZoom(json: String): String {
    val style = JSONObject(json)
    val sources = style.getJSONObject("sources")
    for (id in sources.keys()) {
        val source = sources.getJSONObject(id)
        if (source.optString("type") == "vector" && source.optInt("maxzoom", 22) > MAX_TILE_ZOOM) {
            source.put("maxzoom", MAX_TILE_ZOOM)
        }
    }
    return style.toString()
}

@Composable
fun VoyaraMap(
    initial: CameraMove,
    moves: ReceiveChannel<CameraMove>,
    route: List<Geo>,
    stops: List<Geo>,
    live: Geo?,
    you: Geo?,
    layer: MapLayer,
    traffic: Boolean,
    dark: Boolean,
    colors: MapColors,
    padTop: Float,
    /** Height the bottom sheet rests at; flights centre targets in the map above it. */
    padBottom: Float,
    pin: Geo?,
    onPinMoved: (Geo) -> Unit,
    onTap: (Geo) -> Unit,
    onIdle: (Geo, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context)
        installTileCache(context)
        MapView(context).apply { contentDescription = "Map. Drag to move the pin." }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    val idle by rememberUpdatedState(onIdle)
    val tap by rememberUpdatedState(onTap)
    // Bumped on every camera frame so the pin overlay re-projects without recomposing.
    val cameraTick = remember { mutableIntStateOf(0) }

    MapLifecycle(mapView)
    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.matchParentSize())
        val m = map
        if (pin != null && m != null) DraggablePin(pin, m, cameraTick, onPinMoved)
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            m.cameraPosition = CameraPosition.Builder().target(initial.at.latLng).zoom(initial.zoom ?: 15.0).build()
            m.uiSettings.apply {
                isLogoEnabled = false
                isTiltGesturesEnabled = false
                attributionGravity = Gravity.BOTTOM or Gravity.START
                compassGravity = Gravity.TOP or Gravity.END
            }
            // Only the zoom level just below the view is prefetched (default 4), trimming tile requests.
            m.prefetchZoomDelta = 1
            m.addOnCameraMoveListener { cameraTick.intValue++ }
            m.addOnCameraIdleListener {
                cameraTick.intValue++
                m.cameraPosition.target?.let { idle(Geo(it.latitude, it.longitude), m.cameraPosition.zoom) }
            }
            m.addOnMapClickListener {
                tap(Geo(it.latitude, it.longitude))
                true
            }
            m.addOnMapLongClickListener {
                tap(Geo(it.latitude, it.longitude))
                true
            }
            map = m
        }
    }

    LaunchedEffect(map, layer, traffic, dark, colors) {
        val m = map ?: return@LaunchedEffect
        style = null
        m.uiSettings.setAttributionTintColor(colors.chrome)
        m.setStyle(loadStyle(context, styleUrl(layer, traffic, dark))) { s ->
            s.addOverlays(colors)
            style = s
        }
    }
    LaunchedEffect(style, route) { style?.source(SRC_ROUTE)?.setGeoJson(lineJson(route)) }
    LaunchedEffect(style, you) { style?.source(SRC_YOU)?.setGeoJson(pointsJson(listOfNotNull(you))) }
    LaunchedEffect(style, stops) {
        val s = style ?: return@LaunchedEffect
        for (n in 1..stops.size) {
            if (s.getImage(stopIcon(n)) == null) s.addImage(stopIcon(n), stopBadge(n, colors, context.resources.displayMetrics.density))
        }
        s.source(SRC_STOPS)?.setGeoJson(stopsJson(stops))
    }

    // Fixes arrive ~5×/s while moving; glide between them every display frame (up to 120 Hz).
    val liveTarget by rememberUpdatedState(live)
    LaunchedEffect(style) {
        val source = style?.source(SRC_LIVE) ?: return@LaunchedEffect
        var shown: Geo? = null
        snapshotFlow { liveTarget }.collectLatest { target ->
            val from = shown
            if (target == null || from == null || distance(from, target) > 300) {
                shown = target
                source.setGeoJson(pointsJson(listOfNotNull(target)))
                return@collectLatest
            }
            val start = withFrameNanos { it }
            do {
                val t = ((withFrameNanos { it } - start) / GLIDE_NANOS).coerceAtMost(1.0)
                val p = Geo(from.lat + (target.lat - from.lat) * t, from.lng + (target.lng - from.lng) * t)
                shown = p
                source.setGeoJson(pointsJson(listOf(p)))
            } while (t < 1.0)
        }
    }

    // MapLibre cancels a running animation whenever the camera is moved, so a flight carries its
    // own padding and plain padding updates wait until it lands.
    val flying = remember { mutableStateOf(false) }
    val padTopNow by rememberUpdatedState(padTop)
    val padBottomNow by rememberUpdatedState(padBottom)
    LaunchedEffect(map) {
        val m = map ?: return@LaunchedEffect
        for (move in moves) {
            val now = m.cameraPosition
            val position = CameraPosition.Builder()
                .target(move.at.latLng)
                .zoom(move.zoom ?: now.zoom)
                .bearing(now.bearing)
                .padding(0.0, padTopNow.toDouble(), 0.0, padBottomNow.toDouble())
                .build()
            flying.value = true
            m.animateCamera(
                CameraUpdateFactory.newCameraPosition(position), 900,
                object : MapLibreMap.CancelableCallback {
                    override fun onCancel() {
                        flying.value = false
                    }

                    override fun onFinish() {
                        flying.value = false
                    }
                },
            )
        }
    }

    LaunchedEffect(map, padTop, padBottom, flying.value) {
        val m = map ?: return@LaunchedEffect
        val gap = (8 * context.resources.displayMetrics.density).toInt()
        m.uiSettings.setCompassMargins(0, padTop.toInt() + gap, gap, 0)
        m.uiSettings.setAttributionMargins(gap, 0, 0, padBottom.toInt() + gap)
        if (!flying.value) m.moveCamera(CameraUpdateFactory.paddingTo(0.0, padTop.toDouble(), 0.0, padBottom.toDouble()))
    }
}

/** The picked place. Drag it to move; it re-projects on every camera frame. */
@Composable
private fun DraggablePin(pin: Geo, map: MapLibreMap, cameraTick: IntState, onMoved: (Geo) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val latest by rememberUpdatedState(pin)
    val moved by rememberUpdatedState(onMoved)
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    PinGlyph(
        lifted = dragging,
        modifier = Modifier
            .offset {
                cameraTick.intValue // re-place when the camera moves
                val p = map.projection.toScreenLocation(pin.latLng)
                IntOffset((p.x + drag.x - 20.dp.toPx()).roundToInt(), (p.y + drag.y - 52.dp.toPx()).roundToInt())
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        val p = map.projection.toScreenLocation(latest.latLng)
                        val at = map.projection.fromScreenLocation(PointF(p.x + drag.x, p.y + drag.y))
                        moved(Geo(at.latitude, at.longitude))
                        drag = Offset.Zero
                        dragging = false
                    },
                    onDragCancel = {
                        drag = Offset.Zero
                        dragging = false
                    },
                ) { change, amount ->
                    change.consume()
                    drag += amount
                }
            }
            .semantics { contentDescription = "Selected place. Drag to move it." },
    )
}

/** Forwards the host lifecycle to the MapView, which requires it. */
@Composable
private fun MapLifecycle(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

private fun Style.source(id: String) = getSourceAs<GeoJsonSource>(id)

private fun stopIcon(n: Int) = "voyara-stop-$n"

/** Numbered stop badge drawn locally, so it doesn't depend on the style's fonts. */
private fun stopBadge(n: Int, colors: MapColors, density: Float): Bitmap {
    val size = (26 * density).toInt()
    val r = size / 2f
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bmp ->
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawCircle(r, r, r, paint.apply { color = colors.casing })
        c.drawCircle(r, r, r - 2 * density, paint.apply { color = colors.route })
        paint.apply {
            color = colors.onRoute
            textSize = 12 * density
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        c.drawText("$n", r, r - (paint.descent() + paint.ascent()) / 2, paint)
    }
}

private fun Style.addOverlays(colors: MapColors) {
    addSource(GeoJsonSource(SRC_ROUTE))
    addSource(GeoJsonSource(SRC_STOPS))
    addSource(GeoJsonSource(SRC_LIVE))
    addSource(GeoJsonSource(SRC_YOU))
    addLayer(
        LineLayer("voyara-route-casing", SRC_ROUTE).withProperties(
            lineColor(colors.casing), lineWidth(9f),
            lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    addLayer(
        LineLayer("voyara-route", SRC_ROUTE).withProperties(
            lineColor(colors.route), lineWidth(5f),
            lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    addLayer(
        SymbolLayer("voyara-stops", SRC_STOPS).withProperties(
            iconImage(Expression.get("icon")), iconAllowOverlap(true), iconIgnorePlacement(true),
        ),
    )
    // The real position: blue, the one convention everyone reads as "you are here".
    addLayer(
        CircleLayer("voyara-you-halo", SRC_YOU).withProperties(
            circleRadius(18f), circleColor(colors.you), circleOpacity(0.18f),
        ),
    )
    addLayer(
        CircleLayer("voyara-you", SRC_YOU).withProperties(
            circleRadius(7f), circleColor(colors.you), circleStrokeColor(colors.liveRing), circleStrokeWidth(2.5f),
        ),
    )
    addLayer(
        CircleLayer("voyara-live-halo", SRC_LIVE).withProperties(
            circleRadius(22f), circleColor(colors.live), circleOpacity(0.22f),
        ),
    )
    addLayer(
        CircleLayer("voyara-live", SRC_LIVE).withProperties(
            circleRadius(8f), circleColor(colors.live), circleStrokeColor(colors.liveRing), circleStrokeWidth(3f),
        ),
    )
}

private fun lineJson(points: List<Geo>): String {
    if (points.size < 2) return EMPTY
    return points.joinToString(
        ",", """{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[""", "]}}",
    ) { "[${it.lng},${it.lat}]" }
}

private fun pointsJson(points: List<Geo>): String = points.joinToString(
    ",", """{"type":"FeatureCollection","features":[""", "]}",
) { """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[${it.lng},${it.lat}]}}""" }

private fun stopsJson(stops: List<Geo>): String = stops.withIndex().joinToString(
    ",", """{"type":"FeatureCollection","features":[""", "]}",
) { (i, p) ->
    """{"type":"Feature","properties":{"icon":"${stopIcon(i + 1)}"},"geometry":{"type":"Point","coordinates":[${p.lng},${p.lat}]}}"""
}

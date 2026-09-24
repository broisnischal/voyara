package app.voyara

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Search (Photon + the platform geocoder), road routing (OSRM on FOSSGIS servers) and reverse
 * geocoding. No API keys.
 */
object Net {
    private const val UA = "Voyara/1.0 (Android location simulator)"
    const val PHOTON = "https://photon.komoot.io"
    const val ROUTING = "https://routing.openstreetmap.de"

    // Generous: on weak mobile links a single round trip to these EU servers can take ~2 s.
    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 20_000
        setRequestProperty("User-Agent", UA)
    }

    fun fetch(url: String): String {
        val c = open(url)
        val code = c.responseCode
        if (code !in 200..299) {
            c.disconnect()
            throw IOException("HTTP $code")
        }
        // Closing the stream (rather than disconnect()) returns the socket to the keep-alive pool,
        // so the next request skips the TCP + TLS handshake.
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    /** Opens a pooled connection to [host] ahead of the first real request. */
    suspend fun warmUp(host: String) {
        withContext(Dispatchers.IO) {
            runCatching { open(host).apply { requestMethod = "HEAD" }.inputStream.close() }
        }
    }

    private fun Double.fmt() = String.format(Locale.US, "%.6f", this)

    suspend fun search(query: String, near: Geo?): List<Place> = withContext(Dispatchers.IO) {
        val lang = Locale.getDefault().language.takeIf { it in setOf("en", "de", "fr", "it") } ?: "en"
        val bias = near?.let { "&lat=${it.lat.fmt()}&lon=${it.lng.fmt()}" }.orEmpty()
        val q = URLEncoder.encode(query, "UTF-8")
        val features = JSONObject(fetch("$PHOTON/api/?q=$q&limit=8&lang=$lang$bias")).getJSONArray("features")
        List(features.length()) { features.getJSONObject(it).toPlace() }
    }

    private fun JSONObject.toPlace(): Place {
        val c = getJSONObject("geometry").getJSONArray("coordinates")
        val p = getJSONObject("properties")
        fun s(key: String) = p.optString(key).takeIf { it.isNotBlank() }
        val street = listOfNotNull(s("street"), s("housenumber")).joinToString(" ").ifBlank { null }
        val name = s("name") ?: street ?: s("city") ?: "Unnamed place"
        val detail = listOfNotNull(street, s("district") ?: s("locality"), s("city"), s("state"), s("country"))
            .filter { it != name }.distinct().joinToString(", ")
        val zoom = when (s("type")) {
            "country" -> 4.5
            "state" -> 6.5
            "county" -> 9.0
            "city" -> 11.5
            "district", "locality" -> 13.5
            "street" -> 16.0
            else -> 17.0
        }
        return Place(name, detail, Geo(c.getDouble(1), c.getDouble(0)), zoom)
    }

    /** Forward search through the platform geocoder (Google's backend on most phones). */
    suspend fun geocode(context: Context, query: String): List<Place> {
        if (!Geocoder.isPresent()) return emptyList()
        val geocoder = Geocoder(context)
        val found: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocationName(query, 5, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses)
                    override fun onError(errorMessage: String?) =
                        cont.resumeWithException(IOException(errorMessage ?: "Geocoder failed"))
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 5).orEmpty()
            }
        }
        return found.map { a ->
            val line = a.getAddressLine(0).orEmpty().stripPlusCode()
            val feature = a.featureName?.takeIf { f -> f.any(Char::isLetter) && '+' !in f }
            val name = feature ?: line.substringBefore(',').ifBlank { query }
            Place(name, line.takeIf { it != name }.orEmpty(), Geo(a.latitude, a.longitude))
        }
    }

    fun profileFor(kmh: Double) = when {
        kmh <= 12 -> "routed-foot"
        kmh <= 30 -> "routed-bike"
        else -> "routed-car"
    }

    /** Road-following path through [stops], for the OSRM [profile] from [profileFor]. */
    suspend fun route(stops: List<Geo>, profile: String): List<Geo> = withContext(Dispatchers.IO) {
        val coords = stops.joinToString(";") { "${it.lng.fmt()},${it.lat.fmt()}" }
        val json = JSONObject(fetch("$ROUTING/$profile/route/v1/driving/$coords?overview=full&geometries=geojson"))
        if (json.optString("code") != "Ok") throw IOException(json.optString("message", "No route"))
        val c = json.getJSONArray("routes").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
        List(c.length()) { i -> c.getJSONArray(i).let { Geo(it.getDouble(1), it.getDouble(0)) } }
    }

    /** The address at [at] as a name (street or landmark) plus the rest of the address. */
    suspend fun reverse(context: Context, at: Geo): Place? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context)
        val found: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(at.lat, at.lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses)
                    override fun onError(errorMessage: String?) = cont.resume(null)
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(at.lat, at.lng, 1) }.getOrNull()
            }
        }
        val a = found?.firstOrNull() ?: return null
        val line = a.getAddressLine(0).orEmpty().stripPlusCode()
        val feature = a.featureName?.takeIf { f -> f.any(Char::isLetter) && '+' !in f }
        val name = feature ?: a.thoroughfare ?: line.substringBefore(',').ifBlank { return null }
        val detail = line.split(", ").filter { it != name }.joinToString(", ")
        return Place(name, detail, at)
    }

    /** Coordinates from shared text, following short-link redirects (e.g. maps.app.goo.gl). */
    suspend fun resolveCoords(text: String): Geo? = parseCoords(text) ?: withContext(Dispatchers.IO) {
        var url = Regex("""https?://\S+""").find(text)?.value ?: return@withContext null
        repeat(4) {
            val next = runCatching {
                val c = open(url).apply { instanceFollowRedirects = false }
                try { c.getHeaderField("Location") } finally { c.disconnect() }
            }.getOrNull() ?: return@withContext null
            parseCoords(URLDecoder.decode(next, "UTF-8"))?.let { return@withContext it }
            url = next
        }
        null
    }
}

/** Points of a GPX file: its track if it has one, else its route, else its waypoints. */
fun parseGpx(input: InputStream): List<Geo> {
    val parser = Xml.newPullParser().apply { setInput(input, null) }
    val found = mapOf("trkpt" to mutableListOf<Geo>(), "rtept" to mutableListOf(), "wpt" to mutableListOf())
    while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType != XmlPullParser.START_TAG) continue
        val points = found[parser.name.substringAfter(':')] ?: continue
        val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
        val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
        if (lat != null && lon != null) points += Geo(lat, lon)
    }
    return listOf("trkpt", "rtept", "wpt").map(found::getValue).firstOrNull { it.size >= 2 }.orEmpty()
}

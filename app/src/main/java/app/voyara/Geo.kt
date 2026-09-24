package app.voyara

import java.util.Locale
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class Geo(val lat: Double, val lng: Double) {
    fun pretty(): String = String.format(Locale.US, "%.5f, %.5f", lat, lng)
}

private const val EARTH_M = 6_371_000.0

private fun rad(deg: Double) = Math.toRadians(deg)

/** Great-circle distance in metres. */
fun distance(a: Geo, b: Geo): Double {
    val sLat = sin(rad(b.lat - a.lat) / 2)
    val sLng = sin(rad(b.lng - a.lng) / 2)
    val h = sLat * sLat + cos(rad(a.lat)) * cos(rad(b.lat)) * sLng * sLng
    return 2 * EARTH_M * asin(sqrt(h.coerceAtMost(1.0)))
}

/** Initial bearing from [a] to [b], 0..360 degrees clockwise from north. */
fun bearing(a: Geo, b: Geo): Double {
    val p1 = rad(a.lat)
    val p2 = rad(b.lat)
    val dl = rad(b.lng - a.lng)
    val y = sin(dl) * cos(p2)
    val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}

/** The point [meters] away from this one heading [bearingDeg]. */
fun Geo.move(bearingDeg: Double, meters: Double): Geo {
    val d = meters / EARTH_M
    val t = rad(bearingDeg)
    val p1 = rad(lat)
    val p2 = asin(sin(p1) * cos(d) + cos(p1) * sin(d) * cos(t))
    val l2 = rad(lng) + atan2(sin(t) * sin(d) * cos(p1), cos(d) - sin(p1) * sin(p2))
    return Geo(Math.toDegrees(p2), (Math.toDegrees(l2) + 540) % 360 - 180)
}

/** A polyline with cumulative distances precomputed, so each lookup along it is O(log n). */
class Path(val points: List<Geo>) {
    private val cum = DoubleArray(points.size).also {
        for (i in 1 until points.size) it[i] = it[i - 1] + distance(points[i - 1], points[i])
    }
    val length = cum.lastOrNull() ?: 0.0

    /** Position and heading after travelling [d] metres from the start. */
    fun at(d: Double): Pair<Geo, Double> {
        if (points.size < 2) return points.first() to 0.0
        val dist = d.coerceIn(0.0, length)
        val i = cum.binarySearch(dist).let { if (it >= 0) it else -it - 2 }.coerceIn(0, points.size - 2)
        val a = points[i]
        val b = points[i + 1]
        val seg = cum[i + 1] - cum[i]
        val t = if (seg > 0) (dist - cum[i]) / seg else 0.0
        return Geo(a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t) to bearing(a, b)
    }
}

// Google's "!3d<lat>!4d<lng>" marks the place itself, so it wins over an "@lat,lng" viewport centre.
private val placeRe = Regex("""!3d(-?\d{1,2}\.\d+)!4d(-?\d{1,3}\.\d+)""")
// Decimals required on both numbers so text like "Route 66 5th" isn't read as coordinates.
private val pairRe = Regex("""(?<![\d.])(-?\d{1,2}\.\d+)\s*[,\s]\s*(-?\d{1,3}\.\d+)(?![\d.])""")

/** Finds "lat, lng" in free text, geo: URIs or map links. */
fun parseCoords(text: String): Geo? = sequenceOf(placeRe, pairRe)
    .flatMap { it.findAll(text) }
    .firstNotNullOfOrNull { m ->
        val lat = m.groupValues[1].toDouble()
        val lng = m.groupValues[2].toDouble()
        if (lat in -90.0..90.0 && lng in -180.0..180.0) Geo(lat, lng) else null
    }

private val plusCode = Regex("""^[23456789CFGHJMPQRVWX]{4,8}\+[23456789CFGHJMPQRVWX]{2,3},?\s*""")

/** Drops a leading Open Location Code ("M9MV+8RW, New Rd, …") that geocoders prepend. */
fun String.stripPlusCode(): String = replace(plusCode, "")

fun formatDistance(m: Double): String =
    if (m < 1000) "${m.roundToInt()} m" else String.format(Locale.US, "%.1f km", m / 1000)

fun formatDuration(seconds: Double): String {
    val min = (seconds / 60).roundToInt()
    return when {
        min < 1 -> "<1 min"
        min < 60 -> "$min min"
        else -> "${min / 60} h ${min % 60} min"
    }
}

package app.voyara

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTest {
    @Test
    fun parsesCoordinates() {
        assertEquals(Geo(27.7172, 85.324), parseCoords("27.7172, 85.3240"))
        assertEquals(Geo(-33.8688, 151.2093), parseCoords("-33.8688 151.2093"))
        assertEquals(Geo(27.7, 85.3), parseCoords("geo:0,0?q=27.7,85.3(Home)"))
        assertEquals(
            Geo(27.70451, 85.30703),
            parseCoords("https://www.google.com/maps/place/X/@27.7,85.3,17z/data=!3d27.70451!4d85.30703"),
        )
        assertNull(parseCoords("Route 66 5th Avenue"))
        assertNull(parseCoords("95.1, 20.2"))
    }

    @Test
    fun stripsPlusCodes() {
        assertEquals("New Rd, Madhyapur Thimi 44600, Nepal", "M9MV+8RW, New Rd, Madhyapur Thimi 44600, Nepal".stripPlusCode())
        assertEquals("Thimi", "7MV8M9MV+8RW Thimi".stripPlusCode())
        assertEquals("221B Baker St, London", "221B Baker St, London".stripPlusCode())
    }

    @Test
    fun roamStaysInsideRadius() {
        val rng = java.util.Random(7)
        val center = Geo(27.7, 85.3)
        for ((kmh, radius) in listOf(5.0 to 50.0, 50.0 to 50.0, 120.0 to 100.0)) {
            var pos = center
            var heading = 0.0
            var farthest = 0.0
            repeat(20_000) {
                val step = roamStep(pos, heading, center, radius, kmh / 3.6 * 0.2, rng.nextGaussian() * 6, 0.2)
                pos = step.first
                heading = step.second
                farthest = maxOf(farthest, distance(center, pos))
            }
            assertTrue("$kmh km/h strayed ${farthest.toInt()} m from a $radius m radius", farthest <= radius * 1.05)
            assertTrue("$kmh km/h never wandered", farthest > radius * 0.5)
        }
    }

    @Test
    fun walksAlongPath() {
        val path = Path(listOf(Geo(0.0, 0.0), Geo(0.0, 1.0), Geo(1.0, 1.0)))
        assertEquals(2 * 111_195.0, path.length, 50.0)
        val (quarter, heading) = path.at(path.length / 4)
        assertEquals(0.5, quarter.lng, 1e-3)
        assertEquals(90.0, heading, 1e-6)
        assertEquals(Geo(1.0, 1.0), path.at(path.length * 2).first)
        assertEquals(Geo(0.0, 0.0), path.at(-5.0).first)
        val start = Geo(10.0, 10.0)
        assertEquals(1000.0, distance(start, start.move(45.0, 1000.0)), 0.01)
    }
}

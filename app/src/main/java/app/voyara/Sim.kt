package app.voyara

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow

sealed interface Plan {
    data class Hold(val at: Geo) : Plan
    class Route(val path: Path, val loop: Boolean) : Plan

    /** Wander at random within [radius] metres of [center]. */
    data class Roam(val center: Geo, val radius: Double) : Plan
}

/**
 * One wander step for [Plan.Roam]: drift the heading by [turn] degrees, steer back towards
 * [center] once past 70% of [radius], then advance [meters]. The turn rate scales with speed so
 * the turning circle stays small enough to never leave the radius.
 */
fun roamStep(pos: Geo, heading: Double, center: Geo, radius: Double, meters: Double, turn: Double, dt: Double): Pair<Geo, Double> {
    var h = heading + turn
    if (distance(center, pos) > radius * 0.7) {
        val speed = meters / dt.coerceAtLeast(1e-3)
        val maxTurn = maxOf(60.0, Math.toDegrees(speed / (0.15 * radius))) * dt
        val diff = (bearing(pos, center) - h + 540) % 360 - 180
        h += diff.coerceIn(-maxTurn, maxTurn)
    }
    h = (h % 360 + 360) % 360
    return pos.move(h, meters) to h
}

/** The fix most recently pushed to the system (before GPS drift is applied). */
data class Live(val at: Geo, val bearing: Double?, val speedMps: Double, val progress: Double?)

/** Joystick deflection, each axis -1..1 in screen space (+y is down). */
data class Stick(val x: Float = 0f, val y: Float = 0f)

/** Process-wide state shared by the UI and [MockService]. */
object Sim {
    /** What to simulate; null means mocking is off. */
    val plan = MutableStateFlow<Plan?>(null)
    val live = MutableStateFlow<Live?>(null)
    val paused = MutableStateFlow(false)
    val joystick = MutableStateFlow(false)
    val stick = MutableStateFlow(Stick())
    val speedKmh = MutableStateFlow(5.0)
    val drift = MutableStateFlow(true)
    val errors = Channel<String>(Channel.BUFFERED)

    /** Wakes the simulation loop early after a control change. */
    val wake = Channel<Unit>(Channel.CONFLATED)

    fun poke() {
        wake.trySend(Unit)
    }
}

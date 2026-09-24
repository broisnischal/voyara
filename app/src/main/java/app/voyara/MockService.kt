package app.voyara

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/** Foreground service that keeps pushing mock fixes while the app is in the background. */
class MockService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val rng = java.util.Random()
    private lateinit var engine: MockEngine
    private var loop: Job? = null
    private var overlay: JoystickView? = null

    @Volatile
    private var shownText: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        engine = MockEngine(this)
        // Started from the tile or a shortcut, the UI may not have loaded the saved settings yet.
        Store(this).let {
            Sim.speedKmh.value = it.speedKmh
            Sim.drift.value = it.drift
        }
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.channel_mock))
                .build(),
        )
        scope.launch { Sim.joystick.collect { showJoystick(it) } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "start command ${intent?.action ?: "start"} plan=${Sim.plan.value?.javaClass?.simpleName}")
        when (intent?.action) {
            ACTION_STOP -> Sim.plan.value = null
            ACTION_PAUSE -> Sim.paused.value = !Sim.paused.value
            else -> try {
                // Every startForegroundService() must be answered with startForeground().
                ServiceCompat.startForeground(
                    this, NOTIFICATION_ID, notification(statusText()),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                )
            } catch (e: RuntimeException) {
                Log.w(TAG, "startForeground failed", e)
                Sim.errors.trySend("Couldn't start mocking: ${e.message}")
                Sim.plan.value = null
            }
        }
        Sim.poke()
        if (loop == null) {
            if (Sim.plan.value == null) {
                shutdown()
            } else try {
                engine.start()
                launchLoop()
            } catch (e: SecurityException) {
                Log.w(TAG, "not the mock location app", e)
                Sim.errors.trySend(getString(R.string.err_not_mock_app))
                Sim.plan.value = null
                shutdown()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        engine.stop()
        showJoystick(false)
        // Destroyed while still simulating (not via shutdown): make sure the UI stops showing it.
        if (loop != null) {
            Sim.plan.value = null
            Sim.live.value = null
        }
        super.onDestroy()
    }

    private fun launchLoop() {
        loop = scope.launch {
            try {
                withContext(Dispatchers.Default) { simulate() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                Log.w(TAG, "mock permission revoked", e)
                Sim.errors.trySend(getString(R.string.err_not_mock_app))
                Sim.plan.value = null
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "test provider removed", e)
                // Our test provider was removed from under us.
                Sim.errors.trySend(getString(R.string.err_interrupted))
                Sim.plan.value = null
            }
            loop = null
            // A new plan may have arrived while the loop was winding down.
            if (Sim.plan.value != null) launchLoop() else shutdown()
        }
        showJoystick(Sim.joystick.value)
        MockTileService.update(this)
    }

    private suspend fun simulate() {
        var plan: Plan? = null
        var pos = Geo(0.0, 0.0)
        var travelled = 0.0
        var driftX = 0.0
        var driftY = 0.0
        var heading = rng.nextDouble() * 360
        var last = SystemClock.elapsedRealtime()
        while (true) {
            val next = Sim.plan.value ?: return
            val now = SystemClock.elapsedRealtime()
            val dt = (now - last) / 1000.0
            last = now
            if (next !== plan) {
                val prev = plan
                plan = next
                travelled = 0.0
                when (next) {
                    is Plan.Hold -> pos = next.at
                    // A new radius around the same centre keeps wandering from where it is.
                    is Plan.Roam -> if ((prev as? Plan.Roam)?.center != next.center) pos = next.center
                    is Plan.Route -> Unit
                }
            }
            val speed = Sim.speedKmh.value / 3.6
            val live = when (next) {
                is Plan.Hold -> {
                    val s = Sim.stick.value
                    val push = min(1.0, hypot(s.x, s.y).toDouble())
                    if (Sim.joystick.value && push > 0.1) {
                        val heading = (Math.toDegrees(atan2(s.x.toDouble(), -s.y.toDouble())) + 360) % 360
                        pos = pos.move(heading, push * speed * dt)
                        Live(pos, heading, push * speed, null)
                    } else {
                        Live(pos, null, 0.0, null)
                    }
                }
                is Plan.Roam -> {
                    val (at, h) = roamStep(pos, heading, next.center, next.radius, speed * dt, rng.nextGaussian() * 30 * dt, dt)
                    pos = at
                    heading = h
                    Live(pos, heading, speed, null)
                }
                is Plan.Route -> {
                    val length = next.path.length
                    if (!Sim.paused.value) travelled += speed * dt
                    if (travelled >= length) {
                        if (next.loop && length > 0) {
                            travelled %= length
                        } else {
                            Sim.plan.compareAndSet(next, Plan.Hold(next.path.points.last()))
                            continue
                        }
                    }
                    val (at, heading) = next.path.at(travelled)
                    Live(at, heading, if (Sim.paused.value) 0.0 else speed, if (length > 0) travelled / length else 1.0)
                }
            }
            val still = live.speedMps == 0.0
            if (still && Sim.drift.value) {
                // Correlated random walk (~1 m spread) so a parked fix wanders like real GPS.
                driftX = driftX * 0.8 + rng.nextGaussian() * 0.6
                driftY = driftY * 0.8 + rng.nextGaussian() * 0.6
                val drifted = live.at.move(90.0, driftX).move(0.0, driftY)
                engine.push(live.copy(at = drifted), (3.0 + abs(driftX) + abs(driftY) + rng.nextDouble() * 2).toFloat())
            } else {
                engine.push(live, if (still) 3f else 5f)
            }
            Sim.live.value = live
            updateNotification()
            withTimeoutOrNull(if (still) 1000L else 200L) { Sim.wake.receive() }
        }
    }

    private fun shutdown() {
        Log.i(TAG, "mocking stopped")
        engine.stop()
        showJoystick(false)
        Sim.live.value = null
        Sim.paused.value = false
        shownText = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        MockTileService.update(this)
    }

    private fun showJoystick(on: Boolean) {
        val wm = getSystemService(WindowManager::class.java)
        val show = on && loop != null && Settings.canDrawOverlays(this)
        val current = overlay
        if (show && current == null) {
            val metrics = resources.displayMetrics
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (16 * metrics.density).toInt()
                y = metrics.heightPixels / 2
            }
            val view = JoystickView(this) { x, y ->
                Sim.stick.value = Stick(x, y)
                Sim.poke()
            }
            view.onDrag = { dx, dy ->
                lp.x = (lp.x + dx).coerceIn(0, metrics.widthPixels - view.width)
                lp.y = (lp.y + dy).coerceIn(0, metrics.heightPixels - view.height)
                wm.updateViewLayout(view, lp)
            }
            wm.addView(view, lp)
            overlay = view
        } else if (!show && current != null) {
            wm.removeView(current)
            overlay = null
            Sim.stick.value = Stick()
        }
    }

    private fun statusText(): String = when (val p = Sim.plan.value) {
        is Plan.Route -> {
            val pct = ((Sim.live.value?.progress ?: 0.0) * 100).toInt()
            if (Sim.paused.value) "Route paused · $pct%" else "Following route · $pct% · ${Sim.speedKmh.value.toInt()} km/h"
        }
        is Plan.Hold -> if (Sim.joystick.value) "Joystick control" else p.at.pretty()
        is Plan.Roam -> "Roaming within ${formatDistance(p.radius)} · ${Sim.speedKmh.value.toInt()} km/h"
        null -> "Starting…"
    }

    @SuppressLint("MissingPermission") // without POST_NOTIFICATIONS the update is just dropped
    private fun updateNotification() {
        val text = statusText()
        if (text == shownText) return
        shownText = text
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }

    private fun notification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .apply {
                if (Sim.plan.value is Plan.Route) {
                    addAction(0, if (Sim.paused.value) "Resume" else "Pause", action(ACTION_PAUSE))
                }
            }
            .addAction(0, "Stop", action(ACTION_STOP))
            .build()
    }

    private fun action(name: String) = PendingIntent.getService(
        this, name.hashCode(), Intent(this, MockService::class.java).setAction(name), PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val TAG = "Voyara"
        private const val CHANNEL = "mock"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "app.voyara.STOP"
        private const val ACTION_PAUSE = "app.voyara.PAUSE"

        fun start(context: Context, plan: Plan) {
            Sim.paused.value = false
            Sim.plan.value = plan
            Sim.poke()
            ContextCompat.startForegroundService(context, Intent(context, MockService::class.java))
        }

        fun stop() {
            Sim.plan.value = null
            Sim.poke()
        }
    }
}

package app.voyara

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Process
import android.os.SystemClock
import com.google.android.gms.location.LocationServices

fun hasLocationPermission(context: Context) =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

/** True when this app is selected under Developer options → Select mock location app. */
@Suppress("DEPRECATION") // deprecated on new SDKs, but still the non-throwing app-op check
fun isMockApp(context: Context): Boolean {
    val ops = context.getSystemService(AppOpsManager::class.java)
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), context.packageName)
    } else {
        ops.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * Replaces the device's location with test providers: gps, network and (Android 12+) fused in
 * LocationManager, plus Play services' fused provider, which most apps actually read from.
 */
@SuppressLint("MissingPermission") // the UI obtains location permission before mocking starts
class MockEngine(context: Context) {
    private val lm = context.getSystemService(LocationManager::class.java)
    private val fused = LocationServices.getFusedLocationProviderClient(context)
    private val providers = buildList {
        add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
    }
    private val active = mutableListOf<String>()
    private var running = false

    /** @throws SecurityException if this app isn't the selected mock location app. */
    @Synchronized
    fun start() {
        if (running) return
        for (p in providers) {
            runCatching { lm.removeTestProvider(p) } // left over from a crash
            try {
                lm.addTestProvider(
                    p, false, false, false, false, true, true, true,
                    ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE,
                )
                lm.setTestProviderEnabled(p, true)
                active += p
            } catch (_: IllegalArgumentException) {
                // Provider unsupported on this device; the others still cover it.
            }
        }
        fused.setMockMode(true)
        running = true
    }

    @Synchronized
    fun push(live: Live, accuracy: Float) {
        if (!running) return
        for (p in active) lm.setTestProviderLocation(p, fix(p, live, accuracy))
        fused.setMockLocation(fix("fused", live, accuracy))
    }

    @Synchronized
    fun stop() {
        if (!running) return
        for (p in active) runCatching { lm.removeTestProvider(p) }
        active.clear()
        fused.setMockMode(false)
        running = false
    }

    private fun fix(provider: String, live: Live, accuracy: Float) = Location(provider).apply {
        latitude = live.at.lat
        longitude = live.at.lng
        this.accuracy = accuracy
        speed = live.speedMps.toFloat()
        speedAccuracyMetersPerSecond = 0.5f
        live.bearing?.let {
            bearing = it.toFloat()
            bearingAccuracyDegrees = 10f
        }
        time = System.currentTimeMillis()
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
    }
}

package app.voyara

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.voyara.ui.HomeScreen
import app.voyara.ui.VoyaraTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private var pendingTeleport: Geo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferHighestRefreshRate()
        if (savedInstanceState == null) handle(intent)
        setContent {
            val dark = when (vm.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // Keep status/navigation bar icons readable when the app's theme differs from the system's.
            LaunchedEffect(dark) {
                val bars = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            VoyaraTheme(dark) { HomeScreen(vm, dark) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        // Started here rather than in onCreate: a foreground service needs the app visibly on top.
        pendingTeleport?.let {
            pendingTeleport = null
            vm.teleportTo(it)
        }
    }

    /** Ask for the panel's fastest mode (e.g. 120 Hz) so the map and UI animate at full rate. */
    private fun preferHighestRefreshRate() {
        val display = ContextCompat.getDisplayOrDefault(this)
        val current = display.mode
        val best = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
    }

    /** geo: links, text shared from other apps (e.g. a Google Maps link) and favorite shortcuts. */
    private fun handle(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> intent.dataString?.let(vm::openShared)
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let(vm::openShared)
            ACTION_TELEPORT -> pendingTeleport = Geo(intent.getDoubleExtra("lat", 0.0), intent.getDoubleExtra("lng", 0.0))
        }
    }

    companion object {
        const val ACTION_TELEPORT = "app.voyara.TELEPORT"
    }
}

/** Long-press the launcher icon → teleport straight to one of the top favorites. */
fun publishShortcuts(context: Context, favorites: List<Place>) {
    val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context).coerceAtMost(4)
    val shortcuts = favorites.take(max).map { p ->
        ShortcutInfoCompat.Builder(context, "fav:${p.at.lat},${p.at.lng}")
            .setShortLabel(p.name.take(24))
            .setLongLabel(p.name.take(60))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(
                Intent(context, MainActivity::class.java)
                    .setAction(MainActivity.ACTION_TELEPORT)
                    .putExtra("lat", p.at.lat)
                    .putExtra("lng", p.at.lng),
            )
            .build()
    }
    runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
}

package app.voyara

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings tile: stops mocking, or resumes it at the most recent place. */
class MockTileService : TileService() {
    override fun onStartListening() = refresh()

    override fun onClick() {
        if (Sim.plan.value != null) {
            MockService.stop()
            refresh(active = false)
            return
        }
        val intent = Intent(this, TileActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun refresh(active: Boolean = Sim.plan.value != null) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = if (active) "On" else "Off"
        tile.updateTile()
    }

    companion object {
        fun update(context: Context) =
            TileService.requestListeningState(context, ComponentName(context, MockTileService::class.java))
    }
}

/**
 * Invisible trampoline: a foreground service may only be started while the app is visible,
 * so the tile opens this activity, which starts mocking and closes itself.
 */
class TileActivity : Activity() {
    override fun onResume() {
        super.onResume()
        val store = Store(this)
        val place = store.recents.firstOrNull() ?: store.favorites.firstOrNull()
        if (place != null && isMockApp(this) && hasLocationPermission(this)) {
            MockService.start(this, Plan.Hold(place.at))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}

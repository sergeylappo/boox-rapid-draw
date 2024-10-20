package com.sergeylappo.booxrapiddraw

import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.service.quicksettings.Tile.STATE_ACTIVE
import android.service.quicksettings.Tile.STATE_INACTIVE
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import com.sergeylappo.booxrapiddraw.PreferenceKey.IS_RUNNING

class RapidDrawTileService : TileService() {
    // Called when your app can update your tile.
    override fun onStartListening() {
        qsTile.state = getCurrentState()
        qsTile.updateTile()
    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        qsTile.state = getCurrentState()
        qsTile.updateTile()
    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        toggleService()
    }

    private fun toggleService() {
        val svc = Intent(applicationContext, MainActivity::class.java)
            .apply { flags = FLAG_ACTIVITY_NEW_TASK }
        val pendingIntent =
            PendingIntentActivityWrapper(
                applicationContext,
                0,
                svc,
                FLAG_ONE_SHOT,
                false
            )
        TileServiceCompat.startActivityAndCollapse(this, pendingIntent)
    }

    private fun getCurrentState(): Int {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isRunning = prefs.getBoolean(IS_RUNNING.key, false)
        return if (isRunning) {
            STATE_INACTIVE
        } else {
            STATE_ACTIVE
        }
    }
}

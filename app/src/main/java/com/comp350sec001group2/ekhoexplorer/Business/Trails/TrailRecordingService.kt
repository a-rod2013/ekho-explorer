package com.comp350sec001group2.ekhoexplorer.Business.Trails

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import com.comp350sec001group2.ekhoexplorer.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TrailRecordingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var updates: WalkLocationUpdates? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val session = (application as EkhoApp).container.walkSession
        startInForeground()

        if (updates == null) {
            updates = WalkLocationUpdates(this).also { it.start(session::onFix) }
            scope.launch {
                // Stop once the walk is paused, interrupted, finished or discarded.
                session.state.first { it.phase != WalkPhase.RECORDING }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        updates?.stop()
        updates = null
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Trail recording", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Recording trail")
            .setContentText("Your walk is being recorded. Tap to open the app.")
            .setOngoing(true)
            .setContentIntent(open)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "trail_recording"
        private const val NOTIFICATION_ID = 4107

        fun start(context: Context) {
            androidx.core.content.ContextCompat.startForegroundService(context, Intent(context, TrailRecordingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TrailRecordingService::class.java))
        }
    }
}

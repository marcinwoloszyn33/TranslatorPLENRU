package pl.example.translator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

/** Keeps the short-lived local OAuth callback listener running during browser sign-in. */
class LoginKeepAliveService : Service() {
    override fun onCreate() {
        super.onCreate()
        val channelId = "oauth_signin"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                channelId, "Logowanie do ChatGPT", NotificationManager.IMPORTANCE_LOW
            ))
        }
        val open = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else Notification.Builder(this)
        notification.setContentTitle("Logowanie do ChatGPT")
            .setContentText("Translator oczekuje na potwierdzenie z przeglądarki")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setContentIntent(pi)
        startForeground(402, notification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}

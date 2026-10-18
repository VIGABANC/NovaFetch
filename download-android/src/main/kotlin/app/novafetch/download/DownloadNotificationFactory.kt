package app.novafetch.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

internal class DownloadNotificationFactory(private val context: Context) {
    fun create(downloadId: String): Notification {
        ensureChannel()
        return Notification.Builder(context, CHANNEL_ID)
            .setContentTitle("NovaFetch download")
            .setContentText("Downloading media")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "novafetch_downloads"
        fun notificationId(downloadId: String): Int = (downloadId.hashCode() and Int.MAX_VALUE).coerceAtLeast(1)
    }
}

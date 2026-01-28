package me.gm.cleaner.wroker

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import me.gm.cleaner.R
import me.gm.cleaner.nio.RootWorkerService

abstract class BaseWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    protected abstract val workName: String
    protected abstract val notificationIcon: Int
    protected abstract val notificationChannelName: String
    protected abstract val notificationTitle: String
    private var progress: Int = 0

    private fun createForegroundInfo(): ForegroundInfo {
        val cancelIntent = WorkManager.getInstance(appContext).createCancelPendingIntent(id)
        val notification = NotificationCompat
            .Builder(appContext, workName)
            .setContentTitle(notificationTitle)
            .setSmallIcon(notificationIcon)
            .setColor(appContext.getColor(R.color.color_primary))
            .setSound(null)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .addAction(0, appContext.getString(android.R.string.cancel), cancelIntent)
            .build()

        NotificationManagerCompat.from(appContext).run {
            val channel = NotificationChannelCompat
                .Builder(workName, NotificationManager.IMPORTANCE_MAX)
                .setName(notificationChannelName)
                .build()
            createNotificationChannel(channel)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                id.hashCode(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(id.hashCode(), notification)
        }
    }

    final override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo()
    }

    private val limiter = RootWorkerService.CallbackRateLimiter()

    protected fun updateForeground(progress: Int) {
        this.progress = progress
        limiter.tryTriggerCallback {
            setForegroundAsync(createForegroundInfo())
        }
    }
}
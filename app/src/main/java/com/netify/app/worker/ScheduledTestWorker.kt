package com.netify.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.netify.app.NetifyApplication
import com.netify.app.data.local.SpeedTestEntity
import com.netify.app.domain.model.SpeedTestProgress
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Runs one full speed test in the background and, if the result is below
 * the user's alert threshold, posts a notification. Reschedules itself for
 * the next day at the same hour — WorkManager's PeriodicWorkRequest has a
 * 15-minute minimum granularity and no "run at exact clock time" concept,
 * so a self-rescheduling OneTimeWorkRequest is the reliable way to hit a
 * specific daily hour.
 */
class ScheduledTestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as NetifyApplication
        val settings = app.container.settingsDataStore
        val enabled = settings.autoTestEnabled.first()
        if (!enabled) {
            return Result.success()
        }

        val engine = app.container.speedTestEngine
        var finalResultMbps: Double? = null
        engine.runFullTest().collect { progress ->
            if (progress is SpeedTestProgress.Done) {
                finalResultMbps = progress.result.downloadMbps
                app.container.database.speedTestDao().insert(
                    SpeedTestEntity(
                        downloadMbps = progress.result.downloadMbps,
                        uploadMbps = progress.result.uploadMbps,
                        pingMs = progress.result.pingMs,
                        jitterMs = progress.result.jitterMs,
                        serverName = progress.result.serverName,
                        timestamp = progress.result.timestamp
                    )
                )
            }
        }

        finalResultMbps?.let { downloadMbps ->
            val plan = settings.ispPlanMbps.first()
            val thresholdPct = settings.alertThresholdPct.first()
            val pctOfPlan = if (plan > 0) (downloadMbps / plan) * 100 else 100.0
            if (pctOfPlan < thresholdPct) {
                notifySlowSpeed(downloadMbps, plan)
            }
        }

        scheduleNext(applicationContext)
        return Result.success()
    }

    private fun notifySlowSpeed(downloadMbps: Double, planMbps: Int) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Speed alerts", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("WiFi speed is slow")
            .setContentText("Measured ${"%.1f".format(downloadMbps)} Mbps against your ${planMbps} Mbps plan.")
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "netify_speed_alerts"
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "netify_scheduled_test"

        /** Enqueues a one-time worker timed for [hourOfDay]:00 today or tomorrow, whichever is next. */
        fun scheduleDailyAt(context: Context, hourOfDay: Int) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val delayMs = target.timeInMillis - now.timeInMillis
            val request = OneTimeWorkRequestBuilder<ScheduledTestWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        private fun scheduleNext(context: Context) {
            // Re-arm for the same hour tomorrow; if the user changes the hour in
            // Settings, SettingsViewModel calls scheduleDailyAt again to re-arm.
            val cal = Calendar.getInstance()
            scheduleDailyAt(context, cal.get(Calendar.HOUR_OF_DAY))
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

package com.example.focusguard.service

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.focusguard.data.AppDatabase
import com.example.focusguard.data.FocusRepository
import com.example.focusguard.data.entity.AppRule
import com.example.focusguard.data.entity.Violation
import com.example.focusguard.data.entity.ViolationType
import com.example.focusguard.util.NotificationHelper
import com.example.focusguard.util.UsageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class MonitoringWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val repository = FocusRepository(AppDatabase.getDatabase(context))
        val notificationHelper = NotificationHelper(context)
        val usageManager = UsageManager(context)

        // Get all rules
        val rules = repository.getAllRulesSync()

        // Check usage for each rule
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStats = usageManager.getUsageStats(startTime, endTime)

        for (rule in rules) {
            val usage = usageStats[rule.packageName]?.totalTimeInForeground ?: 0L
            val limit = rule.dailyLimitMinutes * 60 * 1000L

            if (limit > 0 && usage > limit) {
                // Limit exceeded
                // Check if recently notified? Or violate.
                // For simplicity, we just notify. We rely on AccessibilityService to actively block.
                notificationHelper.showWarning(
                    "Limit Exceeded",
                    "You have exceeded your daily limit for ${rule.appName}."
                )
                // Mark as blocked in DB if not already (or let AccessibilityService handle logic based on usage)
                // Ideally, AccessibilityService should check usage > limit every time app opens.
            }
        }

        Result.success()
    }
}

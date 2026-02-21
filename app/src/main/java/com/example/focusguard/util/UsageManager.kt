package com.example.focusguard.util

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.util.Calendar

class UsageManager(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun getUsageStats(startTime: Long, endTime: Long): Map<String, UsageStats> {
        return usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
    }

    fun getTodayUsage(packageName: String): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = getUsageStats(startTime, endTime)
        return stats[packageName]?.totalTimeInForeground ?: 0L
    }
}

package com.example.focusguard.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int = 0, // 0 means no limit
    val isDoomscrollMonitored: Boolean = false,
    val doomscrollLimitSeconds: Int = 1200,
    val isBlocked: Boolean = false,
    val isWhitelisted: Boolean = false,
    val isGame: Boolean = false
)

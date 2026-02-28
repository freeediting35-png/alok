package com.example.focusguard.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    PARENT,
    CHILD,
    SELF
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: UserRole,
    val pinHash: String, // Or plain text PIN for simplicity, but hash is better. For this demo, simple string.
    val uninstallCode: String = "",
    val balance: Double = 0.0,
    val dailyReward: Double = 5.0,
    val violationPenalty: Double = 10.0,
    val warningThresholdSeconds: Long = 1200, // 20 mins default
    val isDeviceLockEnabled: Boolean = false,
    val studyStartHour: Int = -1, // -1 means disabled
    val studyEndHour: Int = -1
)

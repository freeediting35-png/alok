package com.example.focusguard.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ViolationType {
    DOOMSCROLL,
    TIME_LIMIT_EXCEEDED,
    UNAUTHORIZED_UNLOCK,
    APP_BLOCKED
}

@Entity(tableName = "violations")
data class Violation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val timestamp: Long,
    val type: ViolationType,
    val packageName: String?,
    val details: String,
    val penaltyApplied: Double
)

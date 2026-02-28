package com.example.focusguard.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    REWARD,
    DEDUCTION,
    ADJUSTMENT
}

@Entity(tableName = "wallet_transactions")
data class WalletTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val amount: Double,
    val type: TransactionType,
    val reason: String,
    val timestamp: Long
)

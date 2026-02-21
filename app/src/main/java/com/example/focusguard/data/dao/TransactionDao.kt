package com.example.focusguard.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.focusguard.data.entity.WalletTransaction

@Dao
interface TransactionDao {
    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): LiveData<List<WalletTransaction>>

    @Insert
    suspend fun insertTransaction(transaction: WalletTransaction)
}

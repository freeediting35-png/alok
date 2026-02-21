package com.example.focusguard.data

import androidx.lifecycle.LiveData
import com.example.focusguard.data.dao.*
import com.example.focusguard.data.entity.*

class FocusRepository(private val database: AppDatabase) {

    val user: LiveData<User> = database.userDao().getUser()
    val allRules: LiveData<List<AppRule>> = database.ruleDao().getAllRules()
    val transactions: LiveData<List<WalletTransaction>> = database.transactionDao().getAllTransactions()
    val violations: LiveData<List<Violation>> = database.violationDao().getAllViolations()

    suspend fun getUserSync(): User? {
        return database.userDao().getUserSync()
    }

    suspend fun getAllRulesSync(): List<AppRule> {
        return database.ruleDao().getAllRulesSync()
    }

    suspend fun saveUser(user: User) {
        if (user.id == 0) {
            database.userDao().insertUser(user)
        } else {
            database.userDao().updateUser(user)
        }
    }

    suspend fun updateBalance(userId: Int, newBalance: Double) {
        database.userDao().updateBalance(userId, newBalance)
    }

    suspend fun addTransaction(transaction: WalletTransaction) {
        database.transactionDao().insertTransaction(transaction)
    }

    suspend fun addViolation(violation: Violation) {
        database.violationDao().insertViolation(violation)
        // Deduct money
        val user = database.userDao().getUserSync()
        if (user != null) {
            val deduction = violation.penaltyApplied
            val newBalance = user.balance - deduction
            // Ensure balance doesn't drop below 0? Prompt says "locks immediately if it hits zero".
            // We just update balance here.
            database.userDao().updateBalance(user.id, newBalance)

            val transaction = WalletTransaction(
                userId = user.id,
                amount = -deduction,
                type = TransactionType.DEDUCTION,
                reason = violation.details,
                timestamp = System.currentTimeMillis()
            )
            database.transactionDao().insertTransaction(transaction)
        }
    }

    suspend fun saveRule(rule: AppRule) {
        database.ruleDao().insertRule(rule)
    }

    suspend fun getRule(packageName: String): AppRule? {
        return database.ruleDao().getRule(packageName)
    }
}

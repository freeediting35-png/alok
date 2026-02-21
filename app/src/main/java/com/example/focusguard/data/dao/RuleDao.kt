package com.example.focusguard.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.focusguard.data.entity.AppRule

@Dao
interface RuleDao {
    @Query("SELECT * FROM app_rules")
    fun getAllRules(): LiveData<List<AppRule>>

    @Query("SELECT * FROM app_rules")
    suspend fun getAllRulesSync(): List<AppRule>

    @Query("SELECT * FROM app_rules WHERE isDoomscrollMonitored = 1")
    suspend fun getDoomscrollApps(): List<AppRule>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName")
    suspend fun getRule(packageName: String): AppRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AppRule)

    @Update
    suspend fun updateRule(rule: AppRule)

    @Delete
    suspend fun deleteRule(rule: AppRule)
}

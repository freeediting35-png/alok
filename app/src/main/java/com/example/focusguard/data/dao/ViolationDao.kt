package com.example.focusguard.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.focusguard.data.entity.Violation

@Dao
interface ViolationDao {
    @Query("SELECT * FROM violations ORDER BY timestamp DESC")
    fun getAllViolations(): LiveData<List<Violation>>

    @Insert
    suspend fun insertViolation(violation: Violation)
}

package com.example.focusguard.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.focusguard.data.entity.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getUser(): LiveData<User>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUserSync(): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET balance = :balance WHERE id = :userId")
    suspend fun updateBalance(userId: Int, balance: Double)
}

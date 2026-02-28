package com.example.focusguard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.focusguard.data.dao.*
import com.example.focusguard.data.entity.*

@Database(
    entities = [User::class, AppRule::class, Violation::class, WalletTransaction::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun ruleDao(): RuleDao
    abstract fun violationDao(): ViolationDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focusguard_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

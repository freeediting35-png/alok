package com.example.focusguard.data

import androidx.room.TypeConverter
import com.example.focusguard.data.entity.UserRole
import com.example.focusguard.data.entity.ViolationType
import com.example.focusguard.data.entity.TransactionType

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String {
        return role.name
    }

    @TypeConverter
    fun toUserRole(value: String): UserRole {
        return UserRole.valueOf(value)
    }

    @TypeConverter
    fun fromViolationType(type: ViolationType): String {
        return type.name
    }

    @TypeConverter
    fun toViolationType(value: String): ViolationType {
        return ViolationType.valueOf(value)
    }

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }
}

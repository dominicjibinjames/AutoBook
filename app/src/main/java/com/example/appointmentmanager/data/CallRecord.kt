package com.example.appointmentmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_records")
data class CallRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val timestamp: Long,
    val smsSent: Boolean,
)

data class GroupedCall(
    val phoneNumber: String,
    val callCount: Int,  // How many times this number called
    val lastCallTime: Long,  // Most recent call timestamp
    val smsSent: Boolean,  // SMS status of most recent call
    val firstCallId: Int
)

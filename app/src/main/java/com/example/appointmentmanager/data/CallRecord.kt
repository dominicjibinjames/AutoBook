package com.example.appointmentmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_records")
data class CallRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val timestamp: Long,
    val smsSent: Boolean,
    val appointmentDate: String? = null,
    val appointmentSlot: String? = null
)

data class GroupedCall(
    val phoneNumber: String,
    val callCount: Int,  // How many times this number called
    val lastCallTime: Long,  // Most recent call timestamp
    val smsSent: Boolean,  // SMS status of most recent call
    val firstCallId: Int
)


// Represents a time slot with its appointments
data class AppointmentSlot(
    val slotTime: String,           // "8-9am"
    val phoneNumbers: List<String>, // List of unique phone numbers
    val capacity: Int = 5,          // Max people per slot
    val count: Int = phoneNumbers.size  // Current occupancy
) {
    val isFull: Boolean get() = count >= capacity
    val isEmpty: Boolean get() = count == 0
    val percentage: Float get() = count.toFloat() / capacity.toFloat()
}


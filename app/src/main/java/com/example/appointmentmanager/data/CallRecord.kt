package com.example.appointmentmanager.data

data class CallRecord(
    val phoneNumber: String,
    val timestamp: Long,
    val smsSent: Boolean,
)

package com.example.appointmentmanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
    @Insert
    suspend fun insertCall(callRecord: CallRecord)

    @Query("SELECT * from call_records ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallRecord>>

    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    suspend fun getAllCallsSync(): List<CallRecord>

    @Query("SELECT * FROM call_records WHERE appointmentDate = :date ORDER BY timestamp DESC")
    fun getAppointmentsForDateFlow(date: String): Flow<List<CallRecord>>

    @Delete
    suspend fun deleteCall(callRecord: CallRecord)
}
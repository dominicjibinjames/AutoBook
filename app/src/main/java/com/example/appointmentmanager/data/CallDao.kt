package com.example.appointmentmanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    @Query("DELETE FROM call_records WHERE phoneNumber = :phoneNumber AND appointmentDate = :date")
    suspend fun deleteByPhoneNumberAndDate(phoneNumber: String, date: String)

    @Delete
    suspend fun deleteCall(callRecord: CallRecord)



    // ========== SLOT CONFIGURATION QUERIES ==========

    // Get configuration for specific slot
    @Query("SELECT * FROM slot_configurations WHERE slotTime = :slotTime")
    fun getSlotConfiguration(slotTime: String): Flow<SlotConfiguration?>

    // Get all slot configurations
    @Query("SELECT * FROM slot_configurations")
    fun getAllSlotConfigurations(): Flow<List<SlotConfiguration>>

    // Insert or update slot configuration
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSlotConfiguration(config: SlotConfiguration)

    // Delete slot configuration (revert to default)
    @Query("DELETE FROM slot_configurations WHERE slotTime = :slotTime")
    suspend fun deleteSlotConfiguration(slotTime: String)

    @Query("SELECT * FROM slot_configurations")
    suspend fun getAllSlotConfigurationsSync(): List<SlotConfiguration>




}
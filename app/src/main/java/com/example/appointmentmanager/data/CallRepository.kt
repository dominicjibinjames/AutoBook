package com.example.appointmentmanager.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class CallRepository(
    private val callDao: CallDao,
    private val settingsManager: SettingsManager
) {

    //get all calls as a Flow (live updates)
    fun getAllCalls(): Flow<List<CallRecord>>{
        return callDao.getAllCalls()
    }

    suspend fun getAllCallsSync(): List<CallRecord> {
        return callDao.getAllCallsSync()
    }

    //save a new call
    suspend fun insertCall(callRecord: CallRecord){
        callDao.insertCall(callRecord)
    }

    suspend fun deleteCall(callRecord: CallRecord){
        callDao.deleteCall(callRecord)
    }

    //Get appointments grouped by slot for specific date
    fun getAppointmentSlotsFlow(date: String): Flow<List<AppointmentSlot>> {
        return combine(
            callDao.getAppointmentsForDateFlow(date),
            callDao.getAllSlotConfigurations(),
            settingsManager.defaultCapacityFlow
        ) { appointments, configurations, defaultCapacity ->

            val allSlots = listOf(
                "8-9am", "9-10am", "10-11am", "11-12pm",
                "12-1pm", "1-2pm", "2-3pm", "3-4pm"
            )

            allSlots.map { slotTime ->
                val phoneNumbers = appointments
                    .filter { it.appointmentSlot == slotTime }
                    .map { it.phoneNumber }
                    .distinct()

                val capacity = configurations
                    .find { it.slotTime == slotTime }
                    ?.capacity ?: defaultCapacity

                AppointmentSlot(slotTime, phoneNumbers, capacity)
            }
        }
    }

    // Delete specific appointment by phone number and date
    suspend fun deleteAppointment(phoneNumber: String, appointmentDate: String) {
        callDao.deleteByPhoneNumberAndDate(phoneNumber, appointmentDate)
    }


    //FOR SLOT CONFIGURATION

    //Get capacity for specific slot or return default
    fun getSlotCapacity(slotTime: String): Flow<Int> {
        return callDao.getSlotConfiguration(slotTime).combine(
            settingsManager.defaultCapacityFlow
        ) { config, defaultCapacity ->
            config?.capacity ?: defaultCapacity
        }
    }


    //Get all slot config
    fun getAllSlotConfigurations(): Flow<List<SlotConfiguration>> {
        return callDao.getAllSlotConfigurations()
    }

    //update capacity for a slot with validation
    suspend fun updateSlotCapacity(
        slotTime: String,
        newCapacity: Int,
        date: String
    ): Result<Unit> {

        //validation helper
        val validationResult = validateCapacityChange(slotTime, newCapacity, date)

        if (validationResult.isFailure) {
            return Result.failure(validationResult.exceptionOrNull()!!)
        }

        // Valid - update configuration
        val config = SlotConfiguration(
            slotTime = slotTime,
            capacity = newCapacity,
            updatedAt = System.currentTimeMillis()
        )
        callDao.upsertSlotConfiguration(config)

        return Result.success(Unit)

    }

    //reset to default capacity
    suspend fun resetSlotCapacity(slotTime: String) {
        callDao.deleteSlotConfiguration(slotTime)
    }


    // Sync version for slot assignment (not reactive)
    suspend fun getAllSlotConfigurationsSync(): List<SlotConfiguration> {
        return callDao.getAllSlotConfigurationsSync()
    }

    //Validate if capacity change is allowed
    suspend fun validateCapacityChange(
        slotTime: String,
        newCapacity: Int,
        date: String
    ): Result<Int> {
        val currentBookings = callDao.getAppointmentsForDateFlow(date)
            .first()
            .filter { it.appointmentSlot == slotTime }
            .map { it.phoneNumber }
            .distinct()
            .size

        if (newCapacity < currentBookings) {
            return Result.failure(
                IllegalArgumentException(
                    "Cannot reduce capacity to $newCapacity. Currently $currentBookings appointments booked. " +
                            "Delete ${currentBookings - newCapacity} appointments first."
                )
            )
        }

        return Result.success(currentBookings)
    }



}
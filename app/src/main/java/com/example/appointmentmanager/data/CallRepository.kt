package com.example.appointmentmanager.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CallRepository(private val callDao: CallDao) {

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
    fun getAppointmentSlotsFlow(date: String): Flow<List<AppointmentSlot>>{
        return callDao.getAppointmentsForDateFlow(date)
            .map { appointments ->

                val allSlots = listOf(
                    "8-9am", "9-10am", "10-11am", "11-12pm",
                    "1-2pm", "2-3pm", "3-4pm"
                )

                allSlots.map{ slotTime ->

                    val phoneNumbers = appointments
                        .filter { it.appointmentSlot == slotTime }
                        .map { it.phoneNumber }
                        .distinct()

                    AppointmentSlot(slotTime, phoneNumbers)

                }
            }
    }
}
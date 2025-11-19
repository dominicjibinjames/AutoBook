package com.example.appointmentmanager.data

import kotlinx.coroutines.flow.Flow

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
}
package com.example.appointmentmanager.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appointmentmanager.data.AppointmentSlot
import com.example.appointmentmanager.data.CallRecord
import com.example.appointmentmanager.data.CallRepository
import com.example.appointmentmanager.getNextWorkingDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CallViewModel(private var repository: CallRepository) : ViewModel() {

    //expose repo's flow for live updates
    val calls: Flow<List<CallRecord>> = repository.getAllCalls()

    //save a new call
    fun saveCall(callRecord: CallRecord){
        viewModelScope.launch {
            repository.insertCall(callRecord)
        }
    }

    //delete a call
    fun deleteCall(callRecord: CallRecord){
        viewModelScope.launch {
            repository.deleteCall(callRecord)
        }
    }


    // For Schedule Screen
    private val tomorrowDateValue: String

    init {
        val (_, dateString) = getNextWorkingDay(System.currentTimeMillis())
        tomorrowDateValue = dateString
    }

    //expose tomorrow's date
    val tomorrowDate: StateFlow<String> = MutableStateFlow(tomorrowDateValue)

    //auto-update appointment slots
    val appointmentSlots: Flow<List<AppointmentSlot>> =
        repository.getAppointmentSlotsFlow(tomorrowDateValue)

}
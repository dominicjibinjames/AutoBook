package com.example.appointmentmanager.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appointmentmanager.data.AppointmentSlot
import com.example.appointmentmanager.data.CallRecord
import com.example.appointmentmanager.data.CallRepository
import com.example.appointmentmanager.getNextWorkingDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    //Mutable Date
    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate

    //Timestamp for navigation
    private val _selectedDateTimeStamp = MutableStateFlow(0L)


    //start with tomorrow
    init {
        goToTomorrow()
    }

    //Watch changing date
    @OptIn(ExperimentalCoroutinesApi::class)
    val appointmentSlots: Flow<List<AppointmentSlot>> =
        selectedDate.flatMapLatest { date ->

            if (date.isEmpty()){
                flowOf(emptyList())
            }
            else{
                repository.getAppointmentSlotsFlow(date)
            }
        }

    fun goToTomorrow(){
        val(timestamp, dateString) = getNextWorkingDay(System.currentTimeMillis())
        _selectedDateTimeStamp.value = timestamp
        _selectedDate.value = dateString
    }

    fun goToToday(){
        val calendar = Calendar.getInstance()
        val timestamp = calendar.timeInMillis
        val dateString = formatDate(calendar.time)

        _selectedDateTimeStamp.value = timestamp
        _selectedDate.value = dateString
    }

    fun nextDay(){
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = _selectedDateTimeStamp.value
        calendar.add(Calendar.DAY_OF_MONTH, 1)

        val timestamp = calendar.timeInMillis
        val dateString = formatDate(calendar.time)

        _selectedDateTimeStamp.value = timestamp
        _selectedDate.value = dateString
    }

    // Go to previous day
    fun previousDay() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = _selectedDateTimeStamp.value
        calendar.add(Calendar.DAY_OF_MONTH, -1)

        val timestamp = calendar.timeInMillis
        val dateString = formatDate(calendar.time)

        _selectedDateTimeStamp.value = timestamp
        _selectedDate.value = dateString
    }

    // Format date consistently
    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    // Delete specific appointment
    fun deleteAppointment(phoneNumber: String, appointmentDate: String) {
        viewModelScope.launch {
            repository.deleteAppointment(phoneNumber, appointmentDate)
        }
    }


}
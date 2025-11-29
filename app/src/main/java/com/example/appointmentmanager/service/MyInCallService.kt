package com.example.appointmentmanager.service

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.telephony.SmsManager
import android.util.Log
import com.example.appointmentmanager.assignAppointmentSlot
import com.example.appointmentmanager.data.AppDatabase
import com.example.appointmentmanager.data.CallRecord
import com.example.appointmentmanager.data.CallRepository
import com.example.appointmentmanager.data.SettingsManager
import com.example.appointmentmanager.generateAppointmentMessage
import com.example.appointmentmanager.getNextWorkingDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


class MyInCallService: InCallService() {

    private lateinit var database: AppDatabase
    private lateinit var repository: CallRepository
    private lateinit var settingsManager: SettingsManager

    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)


    override fun onCreate() {
        super.onCreate()

        //initialize database and repository
        database = AppDatabase.getDatabase(applicationContext)
        settingsManager = SettingsManager(applicationContext)
        repository = CallRepository(database.callDao(), settingsManager)

        Log.d("MyInCallService", "Database and repository initialized")
    }

    override fun onCallAdded(call: Call?) {
        // Called when a new call is detected
        Log.d("MyInCallService", "onCallAdded: New call detected")

        //get the phone number
        val phoneNumber = call?.details?.handle?.schemeSpecificPart
        Log.d("MyInCallService", "Phone number: $phoneNumber")

        //answer the call, audio only
        call?.answer(VideoProfile.STATE_AUDIO_ONLY)
        Log.d("MyInCallService", "Call answered automatically")


        //monitoring state change whenever we get a call
        call?.registerCallback(object : Call.Callback(){
            override fun onStateChanged(call: Call?, state: Int) {
                Log.d("MyInCallService", "Call state changed: $state")

                when(state){
                    Call.STATE_ACTIVE ->{
                        //Call is now active
                        Log.d("MyInCallService", "Call is now active")

                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.d("MyInCallService", "Hanging up call after 3 seconds")
                            call?.disconnect()
                        }, 3000)
                    }
                }
            }
        })




    }

    override fun onCallRemoved(call: Call?) {
        // Called when call ends
        Log.d("MyInCallService", "onCallRemoved: Call ended")

        val phoneNumber = call?.details?.handle?.schemeSpecificPart

        if(phoneNumber !=null){

            serviceScope.launch {
                try {
                    val currentTime = System.currentTimeMillis()

                    //assign appointment slot
                    val(appointmentDate, appointmentSlot) = assignAppointmentSlot(
                        phoneNumber = phoneNumber,
                        callTimestamp = currentTime,
                        repository = repository,
                        context = applicationContext
                    )
                    Log.d("MyInCallService", "Slot assigned: $appointmentDate at $appointmentSlot")

                    //get appointment timestamp for sms
                    val(appointmentTimestamp, _) = getNextWorkingDay(currentTime)

                    //generate sms
                    val message = generateAppointmentMessage(
                        appointmentDate = appointmentDate,
                        appointmentSlot = appointmentSlot,
                        appointmentTimestamp = appointmentTimestamp
                    )

                    Log.d("MyInCallService", "Message generated: $message")

                    //send sms
                    val smsSuccess = sendSMS(phoneNumber, message)

                    saveCallToDatabase(
                        phoneNumber = phoneNumber,
                        smsSuccess = smsSuccess,
                        appointmentDate = appointmentDate,
                        appointmentSlot = appointmentSlot
                    )

                }catch (error: Exception) {
                    Log.e("MyInCallService", "Error processing call: ${error.message}", error)

                    //Save to database
                    saveCallToDatabase(
                        phoneNumber = phoneNumber,
                        smsSuccess = false,
                        appointmentDate = null,
                        appointmentSlot = null
                    )
                }
            }
        }
    }

    private fun sendSMS(phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager = getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)

            Log.d("MyInCallService", "✓ SMS sent to $phoneNumber: $message")
            true  // Success

        } catch (error: Exception) {
            Log.e("MyInCallService", "❌ SMS failed: ${error.message}", error)
            false  // Failed
        }
    }


    private fun saveCallToDatabase(
        phoneNumber: String,
        smsSuccess: Boolean,
        appointmentDate: String? = null,
        appointmentSlot: String? = null
    ){
        serviceScope.launch{
            try {
                val callRecord = CallRecord(
                    phoneNumber = phoneNumber,
                    timestamp = System.currentTimeMillis(),
                    smsSent = smsSuccess,
                    appointmentDate = appointmentDate,
                    appointmentSlot = appointmentSlot
                )

                repository.insertCall(callRecord)

                Log.d(
                    "MyInCallService",
                    "✓ Call saved: $phoneNumber, SMS: $smsSuccess, Slot: $appointmentSlot on $appointmentDate"
                )
            }catch (error: Exception){
                Log.e("MyInCallService", "Failed to save call to database",error)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cancel all running coroutines
        serviceScope.cancel()

        Log.d("MyInCallService", "Service destroyed, scope cancelled")
    }

}
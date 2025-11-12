package com.example.appointmentmanager.service

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.telephony.SmsManager
import android.util.Log
import com.example.appointmentmanager.data.AppDatabase
import com.example.appointmentmanager.data.CallRecord
import com.example.appointmentmanager.data.CallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel





class MyInCallService: InCallService() {

    private lateinit var database: AppDatabase
    private lateinit var repository: CallRepository

    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)


    override fun onCreate() {
        super.onCreate()

        //initialize database and repository
        database = AppDatabase.getDatabase(applicationContext)
        repository = CallRepository(database.callDao())

        Log.d("MyInCallService", "Database initialized")
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

            var smsSuccess = false
            try {
                sendSMS(phoneNumber, "Your appointment is booked at 2pm tomorrow")
                smsSuccess = true
                Log.d("MyInCallService", "SMS sent to: $phoneNumber")
            }
            catch (error: Exception){
                smsSuccess = false  // SMS failed
                Log.e("MyInCallService", "SMS failed: ${error.message}")
            }

            //Save to database
            saveCallToDatabase(phoneNumber, smsSuccess)
        }
    }

    private fun sendSMS(phoneNumber: String, message: String){
            val smsManager = getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
    }

    private fun saveCallToDatabase(phoneNumber: String, smsSuccess: Boolean){
        serviceScope.launch{
            try {
                val callRecord = CallRecord(
                    phoneNumber = phoneNumber,
                    timestamp = System.currentTimeMillis(),
                    smsSent = smsSuccess
                )

                repository.insertCall(callRecord)

                Log.d("MyInCallService", "Call saved to database: $phoneNumber, SMS: $smsSuccess")

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
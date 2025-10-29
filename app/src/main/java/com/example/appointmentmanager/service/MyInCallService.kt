package com.example.appointmentmanager.service

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.telephony.SmsManager
import android.util.Log


class MyInCallService: InCallService() {

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
            sendSMS(phoneNumber, "Your appointment is booked at 2pm tomorrow")
            Log.d("MyInCallService", "SMS sent to: $phoneNumber")
        }
    }

    private fun sendSMS(phoneNumber: String, message: String){
        try {
            //sms manager service
            val smsManager = getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)


        }catch (e: Exception) {
            // failed to send sms
            Log.e("MyInCallService", "Failed to send SMS", e)
        }
    }

}
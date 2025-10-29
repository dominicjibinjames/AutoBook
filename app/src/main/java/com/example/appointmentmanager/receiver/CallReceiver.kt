package com.example.appointmentmanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver(){

    private var callerNumber:String? = null

    override fun onReceive(context: Context?, intent: Intent?) {

        //checking if phone state changed
        if(intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED){

            //get the phone state
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

            Log.d("CallReceiver", "Broadcast Received! State: $state")

            when(state){
                TelephonyManager.EXTRA_STATE_RINGING -> {

                    //get caller's number
                    callerNumber = getLastCallNumber(context)
                    Log.d("CallReceiver", "Incoming call from: $callerNumber")
                }

                TelephonyManager.EXTRA_STATE_OFFHOOK ->{
                    Log.d("CallReceiver", "Call answered")
                }

                TelephonyManager.EXTRA_STATE_IDLE ->{
                    Log.d("CallReceiver", "Call ended")

                    //TODO: send sms to callNumber
                    callerNumber = null
                }
            }


        }


    }

    private fun getLastCallNumber(context: Context?): String?{
        try {

            //content resolver is used as a service to fetch data from the database
            val cursor = context?.contentResolver?.query(

                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                if(it.moveToFirst()){
                    val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                    return it.getString(numberIndex)
                }
            }

        } catch (e: Exception){
            Log.e("Call Receiver", "Error reading call log: ${e.message}")

        }

        return null
    }
}
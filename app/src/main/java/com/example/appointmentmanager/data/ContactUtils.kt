package com.example.appointmentmanager.data

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log


fun getContactName(context: Context, phoneNumber: String): String? {
    return try{

        //Build URI for phone number lookup
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        //Query contacts
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )

        //Extract name if found
        cursor?.use{
            if (it.moveToFirst()){
                val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)

                if(nameIndex >= 0){
                    val name = it.getString(nameIndex)
                    Log.d("ContactUtils", "Found name for $phoneNumber: $name")

                    return name
                }
            }
        }

        Log.d("ContactUtils", "No contact found for $phoneNumber")
        null

    } catch (e: Exception) {
        Log.e("ContactUtils", "Error looking up contact for $phoneNumber", e)
        null
    }
}

fun formatContactDisplay(context: Context, phoneNumber: String): String{
    val name = getContactName(context, phoneNumber)
    return if (name !=null){
        "$name ($phoneNumber)"
    }else{
        phoneNumber
    }
}
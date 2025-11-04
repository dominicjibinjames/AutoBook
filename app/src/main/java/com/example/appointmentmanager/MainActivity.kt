package com.example.appointmentmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appointmentmanager.data.CallRecord
import com.example.appointmentmanager.ui.theme.AppointmentManagerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {


    private var showRationaleDialog by mutableStateOf(false)

    //creating our list of requested permissions
    private val requiredPermissions: Array<String>
        get(){
            val permissions = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.SEND_SMS,
        )

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.toTypedArray()
    }
    //request handler - waits for users response
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permission ->


        //check for denied permissions
        val deniedPermissions = permission.entries.filter{!it.value}.map{it.key}

        if(deniedPermissions.isNotEmpty()){
            val showRationale = deniedPermissions.any{ permission ->
                shouldShowRequestPermissionRationale(permission)
            }

            if(showRationale){
                showRationaleDialog = true
            }
            else {
                Toast.makeText(this, "Please enable permissions in settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper function to check if we already have all permissions
    private fun hasAllPermissions(): Boolean{
        return requiredPermissions.all { permission ->
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //check for permissions after onCreate runs
        if(hasAllPermissions()){
            Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
        }
        else{
            permissionLauncher.launch(requiredPermissions)
        }


        setContent {
            AppointmentManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    //showing call history screen
                    Column(modifier = Modifier.padding(innerPadding)
                    ){
                        CallHistoryScreen(callRecord = getSampleCallRecords())
                    }


                    if(showRationaleDialog){
                        RationaleDialog(
                            onDismiss = {
                                showRationaleDialog = false
                            },
                            onConfirm = {
                                showRationaleDialog = false
                                permissionLauncher.launch(requiredPermissions)
                            }
                        )
                    }
                }
            }
        }
    }
}



//for rationale dialog
@Composable
fun RationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {Text("Permission Required")},
        text = {Text("This app needs permissions to:\n" +
                "• Answer calls automatically\n" +
                "• Send SMS appointment confirmations\n" +
                "• Show service status in notifications")},
        confirmButton = {
            TextButton(onClick = onConfirm){
                Text("Grant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


//Call History Full Screen
@Composable
fun CallHistoryScreen(callRecord: List<CallRecord>){
    Column(){
        Text(
            text= "Call History",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp)
        )
        LazyColumn{
            items(callRecord.size) {record ->
                CallHistoryItem(callRecord = callRecord[record])

            }
        }
    }
}


//Call History Card
@Composable
fun CallHistoryItem(callRecord: CallRecord){
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ){
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ){
            Icon(
                imageVector = Icons.AutoMirrored.Filled.PhoneMissed,
                contentDescription = "Phone Call",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)
            ){
                Text(
                    text=callRecord.phoneNumber,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = formatTimestamp(callRecord.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text= if (callRecord.smsSent) "✓ SMS Sent" else "✗ Sms Failed",
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (callRecord.smsSent) {
                        MaterialTheme.colorScheme.primary
                    }
                    else {
                        MaterialTheme.colorScheme.error
                    }

            )
        }
    }
}

fun formatTimestamp(timestamp: Long) : String{
    val sdf = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}


fun getSampleCallRecords(): List<CallRecord> {
    return listOf(
        CallRecord(
            phoneNumber = "+91 98765 43210",
            timestamp = System.currentTimeMillis() - 3600000,  // 1 hour ago
            smsSent = true
        ),
        CallRecord(
            phoneNumber = "+91 87654 32109",
            timestamp = System.currentTimeMillis() - 7200000,  // 2 hours ago
            smsSent = true
        ),
        CallRecord(
            phoneNumber = "+91 76543 21098",
            timestamp = System.currentTimeMillis() - 86400000,  // 1 day ago
            smsSent = false
        ),
        CallRecord(
            phoneNumber = "+91 65432 10987",
            timestamp = System.currentTimeMillis() - 172800000,  // 2 days ago
            smsSent = true
        )
    )
}


@Preview(showBackground = true)
@Composable
fun CallHistoryItemPreview(){
    AppointmentManagerTheme{
        CallHistoryItem(
            callRecord = CallRecord(
                phoneNumber = "+919876543210",
                timestamp = System.currentTimeMillis(),
                smsSent = true
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CallHistoryScreenPreview(){
    AppointmentManagerTheme{
        CallHistoryScreen(callRecord = getSampleCallRecords())
    }
}
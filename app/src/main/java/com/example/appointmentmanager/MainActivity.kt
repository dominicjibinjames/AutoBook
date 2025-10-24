package com.example.appointmentmanager

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.appointmentmanager.ui.theme.AppointmentManagerTheme
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import android.Manifest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {


    private var showRationaleDialog by mutableStateOf(false)

    //creating our list of requested permissions
    @SuppressLint("InlinedApi")
    private val requiredPermissions = arrayOf(
        Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG
    )


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

                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppointmentManagerTheme {
        Greeting("Android")
    }
}


@Composable
fun RationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {Text("Permission Required")},
        text = {Text("This app needs phone permissions to auto-answer calls.")},
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
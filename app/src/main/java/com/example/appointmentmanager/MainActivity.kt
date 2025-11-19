package com.example.appointmentmanager

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appointmentmanager.data.AppDatabase
import com.example.appointmentmanager.data.CallRecord
import com.example.appointmentmanager.data.CallRepository
import com.example.appointmentmanager.data.GroupedCall
import com.example.appointmentmanager.ui.theme.AppointmentManagerTheme
import com.example.appointmentmanager.ui.theme.GaretFontFamily
import com.example.appointmentmanager.viewModel.CallViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs


class MainActivity : ComponentActivity() {


    //for showing dialog for first denial of permissions
    private var showRationaleDialog by mutableStateOf(false)

    //for Database viewModel
    private lateinit var viewModel: CallViewModel




    //PERMISSIONS
    //Creating our list of requested permissions
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

        //PERMISSIONS
        //Check for permissions after onCreate runs
        if(hasAllPermissions()){
            Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
        }
        else{
            permissionLauncher.launch(requiredPermissions)
        }



        //FOR DATABASE
        //create database instance
        val database = AppDatabase.getDatabase(applicationContext)
        //create repo
        val repository = CallRepository(database.callDao())
        //create viewmodel
        viewModel = CallViewModel(repository)




        setContent {
            AppointmentManagerTheme {

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->


                    //showing call history screen
                    Column(modifier = Modifier.padding(innerPadding)
                    ){
                        CallHistoryScreen(callRecord = viewModel.calls)
                    }


                    //PERMISSIONS
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



//PERMISSIONS
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



@Composable
fun CallHistoryHeader() {
    val isDarkMode = isSystemInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Text(
            text = "Call History",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = GaretFontFamily,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isDarkMode) Color(0xFF2C2C2C) else Color.LightGray
                ),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = "D",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = GaretFontFamily,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


//Empty Call History Screen
@Composable
fun EmptyCallHistoryState() {

    val isDarkMode = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ){
        CallHistoryHeader()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
                ),
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .size(300.dp)
                    .padding(bottom = 146.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.empty_state_image),
                    contentDescription = "Empty State",
                    modifier = Modifier
                )
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) {
                        Color(0xFF1A1919)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                ),

                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    Spacer(modifier = Modifier.size(24.dp))

                    Text(
                        text = "No Recent Calls",
                        fontFamily = GaretFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = if (isDarkMode) Color.White else Color.Black
                    )

                    Spacer(modifier = Modifier.size(8.dp))

                    Text(
                        text = "A record of your calls will appear here",
                        fontFamily = GaretFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                }
            }
        }
    }
}


//Call History Full Screen
@Composable
fun CallHistoryScreen(callRecord: Flow<List<CallRecord>>){

    //Convert Flow to State
    val calls = callRecord.collectAsState(initial = emptyList())

    if(calls.value.isEmpty()){
        EmptyCallHistoryState()
    }
    else
    {
        ///Group calls by date
        val groupedCallsByDate = groupCallsByNumber(calls.value)

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            CallHistoryHeader()

            LazyColumn {

                groupedCallsByDate.forEach { (date, groupedCalls) ->
                    item(key = "header_$date") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total Calls: ${groupedCalls.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    items(
                        items = groupedCalls,
                        key = { groupedCall -> groupedCall.firstCallId }
                    ) { groupedCall ->
                        GroupedCallHistoryItem(groupedCall = groupedCall)

                    }
                }
            }
        }
    }
}

//for the profile avatars
fun getAvatarIcon(phoneNumber: String): Int {
    val avatars = listOf(
        R.drawable.avatar_1,
        R.drawable.avatar_2,
        R.drawable.avatar_3,
        R.drawable.avatar_4,
        R.drawable.avatar_5,
        R.drawable.avatar_6,
        R.drawable.avatar_7,
        R.drawable.avatar_8,
        R.drawable.avatar_9
    )

    val hash = phoneNumber.hashCode()
    val index = abs(hash) % avatars.size
    return avatars[index]
}


//Call History Card
@Composable
fun GroupedCallHistoryItem(groupedCall: GroupedCall){

    val isDarkMode = isSystemInDarkTheme()

    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode){
                Color(0xFF1A1919)
            }else{
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ){
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically

        ){

            //Profile picture
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(color = Color.Transparent),
                contentAlignment = Alignment.Center
            ){
                Image(
                    painter = painterResource(getAvatarIcon(groupedCall.phoneNumber)),
                    contentDescription = "Profile Avatar",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }



            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier
                .weight(1f)

            ){
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = groupedCall.phoneNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = GaretFontFamily,
                        fontWeight = FontWeight.Bold
                    )

                    if (groupedCall.callCount > 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${groupedCall.callCount})",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = GaretFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(color = Color.Red),
                        contentAlignment = Alignment.Center
                    ){
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Contract",
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.surface
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))


                    Text(
                        text = formatTimestamp(groupedCall.lastCallTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            }
            Spacer(modifier = Modifier.width(16.dp))


            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (groupedCall.smsSent) Color(0xFF4CAF50) else Color(0xFFE53935),
                            shape = CircleShape
                        )
                        .background(
                            if (groupedCall.smsSent) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                    contentAlignment = Alignment.Center
                ){
                    Icon(
                        imageVector = if (groupedCall.smsSent) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (groupedCall.smsSent) "Sent" else "Failed",
                        modifier = Modifier.size(18.dp),
                        tint = if (groupedCall.smsSent) Color(0xFF4CAF50) else Color(0xFFE53935)

                    )
                }

                Text(
                    text = "SMS",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}



fun formatTimestamp(timestamp: Long) : String{
    val sdf = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getDateString(timestamp: Long):String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}


fun groupCallsByNumber(calls: List<CallRecord>): Map<String, List<GroupedCall>>{
    return calls
        .sortedByDescending { it.timestamp }
        .groupBy { getDateString(it.timestamp) }
        .mapValues { (_, callsForDate) ->
            callsForDate
                .groupBy { it.phoneNumber }
                .map{(phoneNumber, callsFromNumber) ->
                    GroupedCall(
                        phoneNumber = phoneNumber,
                        callCount = callsFromNumber.size,
                        lastCallTime = callsFromNumber.maxOf { it.timestamp },
                        smsSent = callsFromNumber.last().smsSent,
                        firstCallId = callsFromNumber.first().id
                    )
                }
                .sortedByDescending { it.lastCallTime }
        }
}




fun getSampleCallRecords(): List<CallRecord> {
    val now = System.currentTimeMillis()
    return listOf(
        // Same number called twice today
        CallRecord(
            id = 1,
            phoneNumber = "+17363868754",
            timestamp = now - 600000,  // 10 minutes ago
            smsSent = true
        ),
        CallRecord(
            id = 2,
            phoneNumber = "+17363868754",  // SAME NUMBER!
            timestamp = now - 1200000,  // 20 minutes ago
            smsSent = true
        ),
        // Different number called 3 times today
        CallRecord(
            id = 3,
            phoneNumber = "+14343538754",
            timestamp = now - 1800000,  // 30 min ago
            smsSent = false
        ),
        CallRecord(
            id = 4,
            phoneNumber = "+14343538754",  // SAME NUMBER!
            timestamp = now - 2400000,  // 40 min ago
            smsSent = false
        ),
        CallRecord(
            id = 5,
            phoneNumber = "+14343538754",  // SAME NUMBER!
            timestamp = now - 3000000,  // 50 min ago
            smsSent = false
        ),
        // Single call today
        CallRecord(
            id = 6,
            phoneNumber = "+19333538754",
            timestamp = now - 7200000,  // 2 hours ago
            smsSent = true
        ),
        // Yesterday - same first number
        CallRecord(
            id = 7,
            phoneNumber = "+17363868754",  // Same as id=1,2 but different day
            timestamp = now - 86400000,  // Yesterday
            smsSent = true
        )
    )
}


fun getNextWorkingDay(currentTimestamp: Long): Pair<Long, String>{
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = currentTimestamp

    calendar.add(Calendar.DAY_OF_MONTH, 1)

    if(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY){
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        Log.d("AutoBook", "Skipped Sunday, moved to Monday")
    }

    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dateString = sdf.format(calendar.time)

    Log.d("AutoBook", "Next working day: $dateString")
    return Pair(calendar.timeInMillis, dateString)
}

suspend fun assignAppointmentSlot(
    phoneNumber: String,
    callTimestamp: Long,
    repository: CallRepository
): Pair<String, String>{

    val (workingDayTimestamp, workingDayDate) = getNextWorkingDay(callTimestamp)

    //get all existing records from database
    val allRecords = repository.getAllCallsSync()

    //check if person already has a slot
    val existingSlot = allRecords.firstOrNull {
        it.phoneNumber == phoneNumber && it.appointmentDate == workingDayDate
    }?.appointmentSlot

    if (existingSlot != null) {
        Log.d("AutoBook", "✓ Existing slot found for $phoneNumber on $workingDayDate: $existingSlot")
        return Pair(workingDayDate, existingSlot)
    }

    //slots
    val availableSlots = listOf(
        "8-9am", "9-10am", "10-11am", "11-12pm",
        "1-2pm", "2-3pm", "3-4pm"
    )

    for (slot in availableSlots){
        val countInSlot = allRecords.count{
            it.appointmentDate == workingDayDate && it.appointmentSlot == slot
        }

        if (countInSlot < 5){
            Log.d("AutoBook", "✓ Assigned $phoneNumber to $workingDayDate at $slot (${countInSlot + 1}/5)")
            return Pair(workingDayDate, slot)
        }
    }

    Log.w("AutoBook", "⚠ All slots full for $workingDayDate - $phoneNumber on WAITLIST")
    return Pair(workingDayDate, "WAITLIST")
}


fun generateAppointmentMessage(
    appointmentDate: String,
    appointmentSlot: String,
    appointmentTimestamp: Long
): String {
    // Get day name (Monday, Tuesday, etc.)
    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val dayName = dayFormat.format(Date(appointmentTimestamp))

    // Create message
    val message = "You have an appointment on $dayName at $appointmentSlot"
    Log.d("AutoBook", "Generated SMS: $message")
    return message
}



@Preview(showBackground = true)
@Composable
fun GroupedCallHistoryItemPreview(){
    AppointmentManagerTheme {
        val sampleGroupedCall = GroupedCall(
            phoneNumber = "+17363868754",
            callCount = 2,
            lastCallTime = System.currentTimeMillis(),
            smsSent = true,
            firstCallId = 1
        )
        GroupedCallHistoryItem(groupedCall = sampleGroupedCall)
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GroupedCallHistoryItemDarkPreview(){
    AppointmentManagerTheme {
        val sampleGroupedCall = GroupedCall(
            phoneNumber = "+14343538754",
            callCount = 3,
            lastCallTime = System.currentTimeMillis(),
            smsSent = false,
            firstCallId = 3
        )
        GroupedCallHistoryItem(groupedCall = sampleGroupedCall)
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CallHistoryScreenPreview(){
    AppointmentManagerTheme{
        // Create a Flow from sample data for preview
        val sampleFlow = flowOf(getSampleCallRecords())
        CallHistoryScreen(callRecord = sampleFlow)
    }
}


@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyCallHistoryStatePreview(){
    AppointmentManagerTheme {
        EmptyCallHistoryState()
    }
}
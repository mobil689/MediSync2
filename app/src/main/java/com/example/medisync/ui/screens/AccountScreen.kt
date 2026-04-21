package com.example.medisync.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medisync.viewmodel.MedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(onLogout: () -> Unit, viewModel: MedicationViewModel = viewModel()) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()

    // Optimized stats using derivedStateOf
    val stats by remember {
        derivedStateOf {
            val medications = uiState.medications
            val taken = medications.count { it.isTaken }
            val total = medications.size
            val progress = if (total > 0) taken.toFloat() / total else 0f
            val streak = if (progress == 1f && total > 0) 5 else 0
            
            Triple(taken, total, Pair(progress, streak))
        }
    }

    val (takenToday, totalToday, progressStreak) = stats
    val (dailyProgress, streakValue) = progressStreak

    // State for Profile Name - using ViewModel for sync
    var showNameEditDialog by remember { mutableStateOf(false) }

    // State for Emergency Medical ID
    var isEditingMedicalId by remember { mutableStateOf(false) }
    var bloodType by remember { mutableStateOf("O+") }
    var allergies by remember { mutableStateOf("Penicillin") }
    var emergencyContactName by remember { mutableStateOf("John (Son)") }
    var emergencyContactPhone by remember { mutableStateOf("+1 (555) 234-9087") }

    // State for Sheets
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showReportsSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }

    val isAnyModalOpen = showNameEditDialog || showNotificationsSheet || showReportsSheet || showLanguageSheet

    val blurAnim by animateDpAsState(
        targetValue = if (isAnyModalOpen) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "blurAnimation"
    )

    val contentScale by animateFloatAsState(
        targetValue = if (isAnyModalOpen) 0.94f else 1f,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "contentScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8FAFC), Color(0xFFE0E7FF))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                }
                .blur(blurAnim)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileHeader(
                name = uiState.userName,
                email = "${uiState.userName.lowercase().replace(" ", ".")}@example.com",
                onEditName = { showNameEditDialog = true }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatsCard(
                    title = "DAILY SUCCESS",
                    value = "${(dailyProgress * 100).toInt()}%",
                    subValue = "$takenToday/$totalToday Meds Taken Today",
                    modifier = Modifier.weight(1f),
                    isProgress = true,
                    progress = dailyProgress
                )
                StatsCard(
                    title = if (streakValue > 0) "$streakValue Day" else "No Streak",
                    value = if (streakValue > 0) "STREAK!" else "KEEP GOING",
                    subValue = "",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = if (streakValue > 0) Color(0xFFFB8C00) else Color(0xFFB0BEC5),
                    iconBg = if (streakValue > 0) Color(0xFFFFF3E0) else Color(0xFFECEFF1)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            EmergencyMedicalIDCard(
                isEditing = isEditingMedicalId,
                onEditClick = { isEditingMedicalId = !isEditingMedicalId },
                bloodType = bloodType,
                onBloodTypeChange = { bloodType = it },
                allergies = allergies,
                onAllergiesChange = { allergies = it },
                contactName = emergencyContactName,
                onContactNameChange = { emergencyContactName = it },
                contactPhone = emergencyContactPhone,
                onContactPhoneChange = { emergencyContactPhone = it }
            )

            Spacer(modifier = Modifier.height(20.dp))
            
            SettingsCard(
                onNotificationsClick = { showNotificationsSheet = true },
                onReportsClick = { showReportsSheet = true },
                onLanguageClick = { showLanguageSheet = true },
                onLogout = onLogout
            )
            
            Spacer(modifier = Modifier.height(100.dp))
        }

        // Modals
        if (showNameEditDialog) {
            EditNameDialog(
                currentName = uiState.userName,
                onDismiss = { showNameEditDialog = false },
                onConfirm = { 
                    viewModel.updateUserName(it)
                    showNameEditDialog = false
                }
            )
        }

        if (showNotificationsSheet) {
            NotificationPreferencesSheet(onDismiss = { showNotificationsSheet = false })
        }

        if (showReportsSheet) {
            MedicalReportsSheet(onDismiss = { showReportsSheet = false })
        }

        if (showLanguageSheet) {
            LanguageRegionSheet(onDismiss = { showLanguageSheet = false })
        }
    }
}

@Composable
fun ProfileHeader(name: String, email: String, onEditName: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF5C6BC0), Color(0xFF7E57C2))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(4.dp, Color(0xFFF8FAFC), CircleShape)
                    .clickable { /* Handle camera click */ }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1E293B))
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 16.dp)
                .clickable { onEditName() }
        ) {
            Text(
                text = name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Edit, contentDescription = "Edit Name", modifier = Modifier.size(16.dp), tint = Color(0xFF64748B))
        }
        Text(
            text = email,
            fontSize = 14.sp,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
fun EditNameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile Name") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text("Save")
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
fun StatsCard(
    title: String, 
    value: String, 
    subValue: String, 
    modifier: Modifier, 
    isProgress: Boolean = false,
    progress: Float = 0f,
    icon: ImageVector? = null,
    iconColor: Color = Color.Unspecified,
    iconBg: Color = Color.Transparent
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "progressAnimation"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.height(180.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isProgress) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF4CAF50),
                        strokeWidth = 8.dp,
                        trackColor = Color(0xFFE8F5E9),
                        strokeCap = StrokeCap.Round
                    )
                    AnimatedContent(
                        targetState = value,
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                        },
                        label = "valueAnimation"
                    ) { targetValue ->
                        Text(text = targetValue, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
            } else if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(32.dp))
                }
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                    },
                    label = "streakValueAnimation"
                ) { targetValue ->
                    Text(text = targetValue, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    targetState = title,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "titleAnimation"
                ) { targetTitle ->
                    Text(text = targetTitle, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                }
                if (subValue.isNotEmpty()) {
                    AnimatedContent(
                        targetState = subValue,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "subValueAnimation"
                    ) { targetSubValue ->
                        Text(text = targetSubValue, fontSize = 11.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyMedicalIDCard(
    isEditing: Boolean,
    onEditClick: () -> Unit,
    bloodType: String,
    onBloodTypeChange: (String) -> Unit,
    allergies: String,
    onAllergiesChange: (String) -> Unit,
    contactName: String,
    onContactNameChange: (String) -> Unit,
    contactPhone: String,
    onContactPhoneChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEF5350)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Emergency Medical ID", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text("Visible on lock screen", fontSize = 12.sp, color = Color(0xFF64748B))
                }
                if (isEditing) {
                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 14.sp)
                    }
                } else {
                    TextButton(onClick = onEditClick) {
                        Text("Edit", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.6f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEditing) {
                    MedicalIdEditRow("Blood Type", bloodType, onBloodTypeChange)
                    MedicalIdEditRow("Allergies", allergies, onAllergiesChange)
                    Text("Emergency Contact", fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 4.dp))
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = onContactNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedTextColor = Color(0xFF1E293B),
                            focusedTextColor = Color(0xFF1E293B)
                        )
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = onContactPhoneChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedTextColor = Color(0xFF1E293B),
                            focusedTextColor = Color(0xFF1E293B)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                } else {
                    MedicalIdRow("Blood Type", bloodType)
                    HorizontalDivider(color = Color(0xFFEF5350).copy(alpha = 0.1f))
                    MedicalIdRow("Allergies", allergies)
                    HorizontalDivider(color = Color(0xFFEF5350).copy(alpha = 0.1f))
                    Column {
                        Text("Emergency Contact", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(contactName, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text(contactPhone, fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }
            }
        }
    }
}

@Composable
fun MedicalIdEditRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(120.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                textAlign = TextAlign.End,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedTextColor = Color(0xFF1E293B),
                focusedTextColor = Color(0xFF1E293B)
            )
        )
    }
}

@Composable
fun MedicalIdRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
    }
}

@Composable
fun SettingsCard(
    onNotificationsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            SettingsRow(Icons.Default.Notifications, "Notification Preferences", "Push: ON", Color(0xFFE3F2FD), Color(0xFF2196F3), onNotificationsClick)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))
            SettingsRow(Icons.Default.Watch, "Connected Devices", null, Color(0xFFEDE7F6), Color(0xFF9575CD), {})
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))
            SettingsRow(Icons.Default.FileUpload, "Medical Reports", null, Color(0xFFE8F5E9), Color(0xFF4CAF50), onReportsClick)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))
            SettingsRow(Icons.Default.Language, "Language & Region", "English", Color(0xFFFFF3E0), Color(0xFFFFA726), onLanguageClick)
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    OutlinedButton(
        onClick = onLogout,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.3f))
    ) {
        Icon(Icons.Default.Logout, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsRow(icon: ImageVector, label: String, value: String?, tint: Color, iconColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
        if (value != null) {
            Text(value, fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.padding(end = 8.dp))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Notification Preferences", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.background(Color(0xFFF1F5F9), CircleShape).size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            NotificationToggle("Push Notifications", "Dose times and alerts on your device", true)
            NotificationToggle("Email Updates", "Weekly summary and important changes", false)
            NotificationToggle("Refill Reminders", "Get notified when supply is low", true)
            NotificationToggle("Missed Dose Alerts", "Nudge if you forget a scheduled dose", true)

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
            ) {
                Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NotificationToggle(title: String, subtitle: String, initialValue: Boolean) {
    var checked by remember { mutableStateOf(initialValue) }
    Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
            Text(if (checked) "ON" else "OFF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (checked) Color(0xFF2E7D32) else Color(0xFF64748B))
        }
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2E7D32))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalReportsSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var uploadedFiles by remember { mutableStateOf(listOf(
        "Blood_Test_April.pdf" to "1.2 MB",
        "MRI_Spine_Report.pdf" to "3.4 MB",
        "Prescription_Lisinopril.jpg" to "820 KB"
    )) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val cursor = context.contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
                    if (c.moveToFirst()) {
                        val name = c.getString(nameIndex)
                        val size = c.getLong(sizeIndex)
                        val sizeInMb = size / (1024f * 1024f)

                        if (sizeInMb > 5f) {
                            Toast.makeText(context, "File too large! Max 5MB allowed.", Toast.LENGTH_LONG).show()
                        } else {
                            val sizeStr = if (sizeInMb < 1) "${(size / 1024)} KB" else "${"%.1f".format(sizeInMb)} MB"
                            uploadedFiles = listOf(name to sizeStr) + uploadedFiles
                            Toast.makeText(context, "File uploaded successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    )

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Medical Reports", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Securely store prescriptions and test results", fontSize = 12.sp, color = Color(0xFF64748B))
                }
                IconButton(onClick = onDismiss, modifier = Modifier.background(Color(0xFFF1F5F9), CircleShape).size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Upload area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(
                        width = 1.dp,
                        color = Color(0xFFCBD5E1),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { filePickerLauncher.launch("*/*") },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(48.dp).background(Color(0xFFE3F2FD), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF2196F3))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Drag & drop or click to upload", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text("PDF, JPG, PNG · up to 5 MB", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("RECENT UPLOADS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(12.dp))

            uploadedFiles.forEach { (name, size) ->
                ReportItem(name, size)
            }
        }
    }
}

@Composable
fun ReportItem(name: String, size: String) {
    Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(name, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
            Text(size, fontSize = 12.sp, color = Color(0xFF64748B))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageRegionSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Language & Region", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.background(Color(0xFFF1F5F9), CircleShape).size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text("LANGUAGE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(12.dp))

            val languages = listOf("English", "Hindi", "Bengali", "Telugu", "Marathi", "Tamil", "Urdu", "Gujarati", "Kannada", "Odia", "Malayalam", "Punjabi")
            var selectedLanguage by remember { mutableStateOf("English") }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(240.dp)
            ) {
                items(languages) { lang ->
                    LanguageChip(lang, lang == selectedLanguage) { selectedLanguage = lang }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("REGION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(12.dp))

            var region by remember { mutableStateOf("India") }
            OutlinedCard(
                onClick = { },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(region, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun LanguageChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = borderStroke(1.dp, if (isSelected) Color(0xFF3F51B5) else Color(0xFFCBD5E1)),
        color = if (isSelected) Color(0xFFE8EAF6) else Color.Transparent,
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = if (isSelected) Color(0xFF3F51B5) else Color(0xFF1E293B), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF3F51B5), modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)

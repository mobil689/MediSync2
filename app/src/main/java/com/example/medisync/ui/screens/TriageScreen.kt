package com.example.medisync.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.medisync.ui.components.TriageMannequinView
import com.example.medisync.ui.components.BodyZone
import com.example.medisync.viewmodel.TriageViewModel
import com.example.medisync.viewmodel.ChatMessage
import com.example.medisync.viewmodel.MedicationViewModel
import com.example.medisync.data.model.PrescriptionScan
import com.example.medisync.data.model.ScannedMedication
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageScreen(
    viewModel: TriageViewModel = viewModel(
        factory = TriageViewModel.Factory(LocalContext.current.applicationContext as android.app.Application)
    ),
    medicationViewModel: MedicationViewModel = viewModel()
) {
    var isChatOpen by remember { mutableStateOf(false) }
    var selectedZone by remember { mutableStateOf<BodyZone?>(null) }
    var selectedSide by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    val messages = viewModel.messages
    val isTyping by viewModel.isTyping.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pastScans by viewModel.pastScans.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    
    var showHistorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    fun createTempPictureUri(): Uri {
        val tempFile = File.createTempFile("temp_image", ".jpg", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
    }

    var showImageOptions by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.uploadPrescription(tempPhotoUri!!, context)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createTempPictureUri()
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadPrescription(uri, context)
        }
    }

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(
                index = messages.size - 1,
                scrollOffset = 0
            )
        }
    }

    fun handleSend() {
        if (input.isBlank()) return
        isChatOpen = true
        viewModel.sendMessage(input)
        input = ""
    }

    fun handleZone(zone: BodyZone, isBack: Boolean) {
        selectedZone = zone
        selectedSide = isBack
        viewModel.selectZone("${zone.label}${if (isBack) " (Back)" else ""}")
        isChatOpen = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFDDF1FF), Color(0xFFFDF6FF))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(20.dp)
                    .padding(top = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF60A5FA).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "MediSync Triage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        "Intelligent Guidance",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                if (isChatOpen) {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(
                        onClick = { 
                            viewModel.loadPastScans()
                            showHistorySheet = true 
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Outlined.History, 
                            contentDescription = "Past Scans", 
                            tint = Color(0xFF4A6CF7)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { isChatOpen = false },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Chat", tint = Color.Black)
                    }
                }
            }

            AnimatedContent(
                targetState = isChatOpen,
                transitionSpec = {
                    (fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + 
                     slideInVertically(
                         initialOffsetY = { it / 2 },
                         animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                     ))
                        .togetherWith(fadeOut(animationSpec = tween(300)) + 
                                     slideOutVertically(targetOffsetY = { it / 2 }))
                },
                label = "TriageViewTransition"
            ) { chatOpen ->
                if (!chatOpen) {
                    // --- DEFAULT VIEW (Mannequin + Open Button) ---
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Mannequin Section
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(32.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.05f)),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TriageMannequinView(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(24.dp),
                                            selectedZone = selectedZone,
                                            selectedSide = selectedSide,
                                            onZoneSelected = { zone, isBack -> handleZone(zone, isBack) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Text(
                                            "FRONT",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            "BACK",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }

                        // Space for FAB or other elements
                        Spacer(modifier = Modifier.height(16.dp))

                        // Latest Message Preview
                        if (messages.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(0.7f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                Column {
                                    Text(
                                        "LATEST GUIDANCE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                                    )
                                    ChatBubble(messages.last(), viewModel, medicationViewModel)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(0.7f))
                        }
                    }
                } else {
                    // --- FULLSCREEN CHAT VIEW ---
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                            ) {
                                items(messages) { msg ->
                                    ChatBubble(msg, viewModel, medicationViewModel)
                                }
                                if (isTyping || isLoading) {
                                    item {
                                        if (isLoading) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = Color(0xFF60A5FA),
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        } else {
                                            TypingIndicator()
                                        }
                                    }
                                }
                            }
                        }

                        // Input Bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shadowElevation = 16.dp,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 20.dp)
                                    .fillMaxWidth()
                                    .navigationBarsPadding(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showImageOptions = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFFF1F5F9), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = "Attach",
                                        tint = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = input,
                                    onValueChange = { input = it },
                                    placeholder = {
                                        Text(
                                            "Describe your symptoms...",
                                            fontSize = 14.sp,
                                            color = Color.Black.copy(alpha = 0.4f)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp),
                                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                    shape = RoundedCornerShape(26.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        focusedContainerColor = Color(0xFFF1F5F9),
                                        unfocusedContainerColor = Color(0xFFF1F5F9),
                                        focusedBorderColor = Color(0xFF60A5FA),
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                FloatingActionButton(
                                    onClick = { handleSend() },
                                    modifier = Modifier.size(52.dp),
                                    containerColor = Color(0xFF4F46E5),
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showImageOptions) {
            AlertDialog(
                onDismissRequest = { showImageOptions = false },
                title = { Text("Upload Prescription") },
                text = { Text("Choose an option to upload your prescription image.") },
                confirmButton = {
                    TextButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val uri = createTempPictureUri()
                            tempPhotoUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                        showImageOptions = false
                    }) {
                        Text("Take Photo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        galleryLauncher.launch("image/*")
                        showImageOptions = false
                    }) {
                        Text("Choose from Gallery")
                    }
                }
            )
        }

        // Quick-Access FAB for Accessibility
        if (!isChatOpen) {
            FloatingActionButton(
                onClick = { 
                    isChatOpen = true
                    if (messages.isEmpty()) viewModel.startGeneralTriage()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .padding(bottom = 16.dp), // Adjust for bottom bar
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat with AI", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showHistorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showHistorySheet = false },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = Color.White
            ) {
                PrescriptionHistorySheet(
                    scans = pastScans,
                    onDelete = { viewModel.deleteScan(it) },
                    onAddToSchedule = { medications ->
                        medications.forEach { med ->
                            medicationViewModel.addMedication(
                                name = med.name,
                                dosage = med.dose,
                                times = listOf("09:00", "21:00"), // Default times
                                days = emptyList()
                            )
                        }
                        Toast.makeText(context, "✓ Medications added to schedule", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun PrescriptionHistorySheet(
    scans: List<PrescriptionScan>,
    onDelete: (String) -> Unit,
    onAddToSchedule: (List<ScannedMedication>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp) // Extra padding at bottom for navigation bar
    ) {
        Text(
            "Prescription History",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Text(
            "Your scanned prescriptions",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (scans.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.DocumentScanner,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No prescriptions scanned yet",
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
                Text(
                    "Scan a prescription in the chat to see it here",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(scans, key = { it.id }) { scan ->
                    HistoryCard(
                        scan = scan,
                        onDelete = { onDelete(scan.id) },
                        onAddToSchedule = { onAddToSchedule(scan.medications) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    scan: PrescriptionScan,
    onDelete: () -> Unit,
    onAddToSchedule: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this scan?") },
            text = { Text("This will permanently remove this prescription from your history.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF4A6CF7).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Receipt,
                        contentDescription = null,
                        tint = Color(0xFF4A6CF7),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            scan.diagnosis,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            dateFormat.format(Date(scan.scanDate)),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Text(
                        "${scan.medications.size} medications",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    scan.medications.forEach { med ->
                        Text(
                            "• ${med.name} — ${med.dose}, ${med.frequency}, ${med.duration}",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onAddToSchedule,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6CF7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add to schedule", fontSize = 14.sp)
                    }
                    
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Delete Record", color = Color.Red.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    msg: ChatMessage,
    triageViewModel: TriageViewModel,
    medViewModel: MedicationViewModel
) {
    val isUser = msg.role == "user"
    val isEmergency = msg.text.contains("EMERGENCY", ignoreCase = true) || 
                      msg.text.contains("URGENT", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = when {
                isUser -> Color(0xFF60A5FA) // UserBubble (Sky Blue)
                isEmergency -> Color(0xFFFFEBEE)
                else -> Color.White // AIBubble (White)
            },
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            shadowElevation = 2.dp,
            border = if (!isUser) BorderStroke(1.dp, Color(0xFFF1F5F9)) else null,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column {
                if (msg.imageBitmap != null) {
                    Image(
                        bitmap = msg.imageBitmap.asImageBitmap(),
                        contentDescription = "Prescription Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = msg.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = if (isEmergency && !isUser) Color.Red else if (isUser) Color.White else Color(0xFF1E293B),
                    fontSize = 15.sp,
                    fontWeight = if (isEmergency) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = 22.sp
                )
            }
        }
        
        if (msg.role == "ai" && msg.extractedMedications != null) {
            val context = LocalContext.current
            
            Button(
                onClick = {
                    if (!msg.medicationsAdded) {
                        try {
                            msg.extractedMedications.forEach { med ->
                                medViewModel.addMedication(
                                    name = med.name,
                                    dosage = med.dose,
                                    times = med.times,
                                    days = emptyList()
                                )
                            }
                            triageViewModel.markMedicationsAdded(msg.id)
                            Toast.makeText(context, "✓ Added ${msg.extractedMedications.size} medications to your schedule", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Couldn't auto-add medications. Please add them manually.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !msg.medicationsAdded,
                modifier = Modifier.padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (msg.medicationsAdded) Color.Gray else Color(0xFF4F46E5)
                )
            ) {
                Text(if (msg.medicationsAdded) "✓ Added to schedule" else "Add these to my medications")
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(3) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4353FF).copy(alpha = 0.6f)))
        }
    }
}

package com.example.medisync.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medisync.data.model.MedicalID
import com.example.medisync.ui.theme.*
import com.example.medisync.util.QRGenerator
import com.example.medisync.viewmodel.MedicalIDViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalIDScreen(
    viewModel: MedicalIDViewModel = viewModel()
) {
    val medicalID by viewModel.medicalID.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showQRSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFEBEE), Color.White)
                )
            )
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    text = "Emergency Medical ID",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DangerRed
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                medicalID?.let { id ->
                    MedicalIDCard(id)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showQRSheet = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Show Emergency QR Code")
                }
            }
        }
    }

    if (showQRSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQRSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            QRBottomSheetContent(medicalID?.userId ?: "unknown")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicalIDCard(medicalID: MedicalID) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DangerTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = DangerRed, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Verified Patient", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Emergency Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            InfoRow(label = "Blood Type", value = medicalID.bloodType, icon = Icons.Default.Bloodtype, color = DangerRed)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Allergies", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextMuted)
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                medicalID.allergies.forEach { allergy ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(allergy) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = DangerTint,
                            labelColor = DangerRed
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Emergency Contact", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextMuted)
            medicalID.emergencyContacts.firstOrNull()?.let { contact ->
                EmergencyContactItem(contact)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = TextMuted)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmergencyContactItem(contact: com.example.medisync.data.model.Contact) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = BackgroundGradientEnd
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, fontWeight = FontWeight.Bold)
                Text(contact.relationship, fontSize = 12.sp, color = TextMuted)
            }
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                    context.startActivity(intent)
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = SuccessGreen, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Call")
            }
        }
    }
}

@Composable
fun QRBottomSheetContent(userId: String) {
    val qrUrl = "https://medisync-emergency.web.app/user/$userId"
    val qrBitmap = remember(qrUrl) { QRGenerator.generateQRCode(qrUrl) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "First Responder Access",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Emergency services can scan this to see your vital information.",
            textAlign = TextAlign.Center,
            color = TextMuted,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(260.dp)
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            qrBitmap?.let {
                androidx.compose.foundation.Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.fillMaxSize()
                )
            } ?: CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { /* Implement share logic */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share Medical ID")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

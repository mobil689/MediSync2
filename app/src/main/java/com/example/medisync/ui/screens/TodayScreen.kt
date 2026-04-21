package com.example.medisync.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medisync.data.model.Medication
import com.example.medisync.viewmodel.MedicationViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodayScreen(viewModel: MedicationViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
    val scrollState = rememberLazyListState()
    
    // Performance: derivedStateOf for scroll-dependent values
    val isScrolled by remember {
        derivedStateOf { scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 0 }
    }

    val takenCount = uiState.medications.count { it.isTaken }
    val totalCount = uiState.medications.size
    val isSelectionMode = uiState.selectedIds.isNotEmpty()
    val isAnyDrawerOpen = uiState.isAddDrawerOpen || uiState.isDeleteConfirmOpen

    val blurAnim by animateDpAsState(
        targetValue = if (isAnyDrawerOpen) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "blurAnimation"
    )

    val contentScale by animateFloatAsState(
        targetValue = if (isAnyDrawerOpen) 0.94f else 1f,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "contentScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFE0E7FF)
                    )
                )
            )
    ) {
        // Blurred Background Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                }
                .blur(blurAnim)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                
                // Header Row with Delete Icon if in Selection Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.graphicsLayer {
                            // Stress Test: Dynamic alpha/scale based on scroll
                            val scrollOffset = scrollState.firstVisibleItemScrollOffset.toFloat()
                            if (scrollState.firstVisibleItemIndex == 0) {
                                alpha = (1f - (scrollOffset / 300f)).coerceIn(0f, 1f)
                                scaleX = (1f - (scrollOffset / 1000f)).coerceIn(0.9f, 1f)
                                scaleY = (1f - (scrollOffset / 1000f)).coerceIn(0.9f, 1f)
                            } else {
                                alpha = 0f
                            }
                        }
                    ) {
                        Text(
                            text = today,
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Good morning, ${uiState.userName}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    if (isSelectionMode) {
                        IconButton(
                            onClick = { viewModel.toggleDeleteConfirm(true) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF5350))
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                if (isSelectionMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.selectedIds.size} SELECTED · HOLD TO SELECT MORE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        TextButton(onClick = { viewModel.clearSelection() }) {
                            Text("Cancel", color = Color(0xFF5C6BC0), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Black)) {
                                append(takenCount.toString())
                            }
                            append(" of ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Black)) {
                                append(totalCount.toString())
                            }
                            append(" doses logged today")
                        },
                        color = Color.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp).graphicsLayer {
                            if (scrollState.firstVisibleItemIndex == 0) {
                                alpha = (1f - (scrollState.firstVisibleItemScrollOffset.toFloat() / 200f)).coerceIn(0f, 1f)
                            } else {
                                alpha = 0f
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "TODAY'S SCHEDULE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    state = scrollState,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
                ) {
                    items(uiState.medications, key = { it.id }) { medication ->
                        val isSelected = uiState.selectedIds.contains(medication.id)
                        Box(modifier = Modifier.animateItem()) {
                            MedicationCard(
                                medication = medication,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onToggle = { 
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(medication.id)
                                    } else {
                                        viewModel.toggleMedication(medication.id)
                                    }
                                },
                                onLongPress = {
                                    if (!isSelectionMode) {
                                        viewModel.toggleSelection(medication.id)
                                    }
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tip: hold a card for 2 seconds to select & delete.",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = Color.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Add FAB
        if (!isSelectionMode) {
            val fabInteractionSource = remember { MutableInteractionSource() }
            val fabPressed by fabInteractionSource.collectIsPressedAsState()
            val fabScale by animateFloatAsState(
                targetValue = if (fabPressed) 0.92f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "fabScale"
            )

            FloatingActionButton(
                onClick = { viewModel.toggleAddDrawer(true) },
                interactionSource = fabInteractionSource,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 20.dp)
                    .size(56.dp)
                    .scale(fabScale)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                        ),
                        shape = CircleShape
                    ),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add medication", modifier = Modifier.size(24.dp))
            }
        }

        if (uiState.isAddDrawerOpen) {
            AddMedicationDrawer(
                onDismiss = { viewModel.toggleAddDrawer(false) },
                onSave = { name, dosage, time, days ->
                    viewModel.addMedication(name, dosage, listOf(time), days)
                    viewModel.toggleAddDrawer(false)
                }
            )
        }
        
        if (uiState.isDeleteConfirmOpen) {
            DeleteMedicationDrawer(
                count = uiState.selectedIds.size,
                onDismiss = { viewModel.toggleDeleteConfirm(false) },
                onConfirm = {
                    viewModel.deleteSelected()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MedicationCard(
    medication: Medication,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "elevation"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(elevation, RoundedCornerShape(24.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onToggle,
                onLongClick = onLongPress
            ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF5C6BC0)) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                RadioButton(
                    selected = isSelected,
                    onClick = onToggle,
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF5C6BC0))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            val (icon, tint, iconColor) = when (medication.timeOfDay) {
                "morning" -> Triple(Icons.Default.WbSunny, Color(0xFFFFF3E0), Color(0xFFFFA726))
                "afternoon" -> Triple(Icons.Default.WbCloudy, Color(0xFFE3F2FD), Color(0xFF2196F3))
                "evening" -> Triple(Icons.Default.WbTwilight, Color(0xFFFFEBEE), Color(0xFFEF5350))
                else -> Triple(Icons.Default.NightsStay, Color(0xFFEDE7F6), Color(0xFF9575CD))
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = medication.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Text(
                        text = medication.dosage,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = medication.time,
                        fontSize = 14.sp,
                        color = Color.Black
                    )

                    AnimatedVisibility(
                        visible = medication.isTaken,
                        enter = fadeIn() + slideInHorizontally(),
                        exit = fadeOut() + slideOutHorizontally()
                    ) {
                        if (medication.loggedTime != null) {
                            Text(
                                text = buildAnnotatedString {
                                    append(" · ")
                                    withStyle(style = SpanStyle(color = Color(0xFF4CAF50))) {
                                        append("logged ${medication.loggedTime}")
                                    }
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            if (!isSelectionMode) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = medication.isTaken,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                             scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                        },
                        label = "status_badge"
                    ) { isTaken ->
                        if (isTaken) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF4CAF50))
                                    .clickable { onToggle() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Taken",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = onToggle,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726)),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text("Take", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteMedicationDrawer(count: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Delete selected schedules?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This medication schedule will be permanently removed. You can always add them back later.",
                fontSize = 14.sp,
                color = Color.Black,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddMedicationDrawer(onDismiss: () -> Unit, onSave: (String, String, String, List<String>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }

    // Time State
    var selectedHour by remember { mutableIntStateOf(9) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var selectedAmPm by remember { mutableStateOf("AM") }

    // Days State
    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDays by remember { mutableStateOf(setOf<String>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Add medication", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(text = "We'll add it to today's schedule.", fontSize = 14.sp, color = Color.Black, modifier = Modifier.padding(top = 4.dp))
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF1F5F9))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("NAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g. Lisinopril", color = Color.Black.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = Color(0xFF5C6BC0), focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("DOSE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                placeholder = { Text("e.g. 10 mg", color = Color.Black.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = Color(0xFF5C6BC0), focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Timing", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))

            WheelTimePickerView(
                onTimeChanged = { h, m, ap ->
                    selectedHour = h
                    selectedMinute = m
                    selectedAmPm = ap
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("SCHEDULE ON", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                allDays.forEach { day ->
                    val isSelected = selectedDays.contains(day)
                    DayChip(
                        day = day,
                        isSelected = isSelected,
                        onToggle = {
                            selectedDays = if (isSelected) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "btnScale"
            )

            val canSubmit = name.isNotBlank() && dosage.isNotBlank() && selectedDays.isNotEmpty()
            Button(
                onClick = {
                    val formattedTime = String.format(Locale.US, "%d:%02d %s", selectedHour, selectedMinute, selectedAmPm)
                    onSave(name, dosage, formattedTime, selectedDays.toList())
                },
                enabled = canSubmit,
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(scale),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD1C4E9),
                    contentColor = Color(0xFF5C6BC0),
                    disabledContainerColor = Color(0xFFE2E8F0)
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save medication", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DayChip(day: String, isSelected: Boolean, onToggle: () -> Unit) {
    val backgroundColor by animateColorAsState(
        if (isSelected) Color(0xFFE8EAF6) else Color.Transparent,
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        if (isSelected) Color(0xFF5C6BC0) else Color(0xFFE2E8F0),
        label = "chipBorder"
    )
    val textColor by animateColorAsState(
        if (isSelected) Color(0xFF5C6BC0) else Color.Black,
        label = "chipText"
    )

    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelTimePickerView(onTimeChanged: (Int, Int, String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    val hours = (1..12).toList()
    val minutes = (0..59).toList()
    val amPm = listOf("AM", "PM")

    // Use a reasonable multiplier for infinite scroll effect
    val hourState = rememberLazyListState(initialFirstVisibleItemIndex = 50 * hours.size + 8) // Defaults to 9
    val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = 50 * minutes.size + 0) // Defaults to 00
    val amPmState = rememberLazyListState(initialFirstVisibleItemIndex = 5 * amPm.size + 0) // Defaults to AM

    val currentHour by remember { derivedStateOf { hours[hourState.firstVisibleItemIndex % hours.size] } }
    val currentMinute by remember { derivedStateOf { minutes[minuteState.firstVisibleItemIndex % minutes.size] } }
    val currentAmPm by remember { derivedStateOf { amPm[amPmState.firstVisibleItemIndex % amPm.size] } }

    LaunchedEffect(currentHour, currentMinute, currentAmPm) {
        onTimeChanged(currentHour, currentMinute, currentAmPm)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Presets Row
        val presets = listOf("9:00 AM", "12:00 PM", "4:00 PM", "8:00 PM")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(presets) { time ->
                Surface(
                    onClick = {
                        val parts = time.split(" ", ":")
                        val h = parts[0].toInt()
                        val m = parts[1].toInt()
                        val ap = parts[2]

                        scope.launch {
                            // Find nearest index to target value
                            val targetHourIdx = hourState.firstVisibleItemIndex + (h - currentHour)
                            val targetMinIdx = minuteState.firstVisibleItemIndex + (m - currentMinute)

                            hourState.animateScrollToItem(targetHourIdx)
                            minuteState.animateScrollToItem(targetMinIdx)
                            amPmState.animateScrollToItem(amPm.indexOf(ap))
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = time,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // The Wheel Picker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Selection overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelColumn(state = hourState, items = hours, modifier = Modifier.weight(1f))
                Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 8.dp))
                WheelColumn(state = minuteState, items = minutes, modifier = Modifier.weight(1f), isMinute = true)
                Spacer(modifier = Modifier.width(16.dp))
                WheelColumn(state = amPmState, items = amPm, modifier = Modifier.weight(0.8f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelColumn(
    state: LazyListState,
    items: List<Any>,
    modifier: Modifier = Modifier,
    isMinute: Boolean = false,
    isInfinite: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    // Use smaller multipliers to avoid unnecessary item overhead
    val multiplier = if (items.size > 2) 100 else 10
    val totalItems = if (isInfinite) multiplier * items.size else items.size

    LaunchedEffect(state.firstVisibleItemIndex) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    LazyColumn(
        state = state,
        modifier = modifier.height(140.dp),
        contentPadding = PaddingValues(vertical = 46.dp),
        flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(totalItems) { index ->
            val isSelected by remember { derivedStateOf { index == state.firstVisibleItemIndex } }
            val item = items[index % items.size]

            val scale by animateFloatAsState(if (isSelected) 1.2f else 0.8f, label = "wheelScale")
            val alpha by animateFloatAsState(if (isSelected) 1f else 0.3f, label = "wheelAlpha")

            Box(
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMinute) String.format(Locale.US, "%02d", item) else item.toString(),
                    fontSize = 20.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = Color.Black,
                    modifier = Modifier
                        .scale(scale)
                        .alpha(alpha)
                )
            }
        }
    }
}

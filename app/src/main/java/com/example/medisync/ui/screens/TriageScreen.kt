package com.example.medisync.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medisync.ui.components.TriageMannequinView
import com.example.medisync.ui.components.BodyZone
import com.example.medisync.viewmodel.TriageViewModel

data class ChatMessage(
    val id: String,
    val role: String, // "user" or "ai"
    val text: String
)

@Composable
fun TriageScreen(viewModel: TriageViewModel = viewModel()) {
    var isChatExpanded by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    val messages = viewModel.messages
    val isTyping by viewModel.isTyping.collectAsState()
    val listState = rememberLazyListState()

    // Animation values
    val mannequinScale by animateFloatAsState(
        targetValue = if (isChatExpanded) 0.6f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "mannequinScale"
    )
    
    val mannequinOffsetY by animateDpAsState(
        targetValue = if (isChatExpanded) (-180).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "mannequinOffset"
    )

    val chatAlpha by animateFloatAsState(
        targetValue = if (isChatExpanded) 1f else 0.8f,
        animationSpec = tween(300),
        label = "chatAlpha"
    )

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    fun handleSend() {
        if (input.isBlank()) return
        isChatExpanded = true
        viewModel.sendMessage(input)
        input = ""
    }

    fun handleZone(zone: BodyZone) {
        isChatExpanded = true
        viewModel.selectZone(zone.label)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8FAFC), Color(0xFFE0E7FF))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(20.dp).padding(top = 20.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF4353FF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF4353FF), modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Triage AI", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
                    Text("Tap a zone — guidance, not diagnosis", fontSize = 11.sp, color = Color(0xFF64748B))
                }
                if (isChatExpanded) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { isChatExpanded = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Chat", tint = Color(0xFF64748B))
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                // Body Map Section (Mannequin)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .offset(y = mannequinOffsetY)
                        .graphicsLayer {
                            scaleX = mannequinScale
                            scaleY = mannequinScale
                        }
                ) {
                    Card(
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth().height(360.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                TriageMannequinView(
                                    modifier = Modifier.fillMaxSize().padding(24.dp),
                                    onZoneSelected = { handleZone(it) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Text("FRONT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                Text("BACK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }

                // Chat Section (Slides up)
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val chatHeight by animateDpAsState(
                        targetValue = if (isChatExpanded) 500.dp else 160.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                        label = "chatHeight"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chatHeight)
                            .graphicsLayer { alpha = chatAlpha }
                    ) {
                        if (isChatExpanded) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                items(messages) { msg ->
                                    ChatBubble(msg)
                                }
                                if (isTyping) {
                                    item {
                                        TypingIndicator()
                                    }
                                }
                            }
                        } else {
                            // Intro bubble when not expanded
                            Box(modifier = Modifier.padding(16.dp)) {
                                ChatBubble(messages.first())
                            }
                        }

                        // Input Bar
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { 
                                    input = it
                                },
                                placeholder = { Text("Describe your symptoms...", fontSize = 14.sp) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF4353FF),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF4353FF), Color(0xFF7E57C2))
                                        )
                                    )
                                    .clickable { handleSend() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) Color(0xFF5C6BC0) else Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = msg.text,
                color = if (isUser) Color.White else Color(0xFF1E293B),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF64748B)))
        }
    }
}

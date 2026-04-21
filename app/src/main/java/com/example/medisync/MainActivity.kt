package com.example.medisync

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.medisync.navigation.Screen
import com.example.medisync.ui.screens.*
import com.example.medisync.ui.theme.MediSyncTheme
import com.example.medisync.viewmodel.MedicationViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediSyncTheme {
                var isLoggedIn by remember { mutableStateOf(false) }

                if (!isLoggedIn) {
                    LoginScreen(onLoginSuccess = { 
                        android.util.Log.d("MediSync", "Login successful, transitioning to MainAppContent")
                        isLoggedIn = true 
                    })
                } else {
                    PermissionWrapper {
                        MainAppContent(onLogout = { isLoggedIn = false })
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionWrapper(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("MediSyncPrefs", Context.MODE_PRIVATE) }
    var isFirstLaunch by remember { mutableStateOf(sharedPrefs.getBoolean("isFirstLaunch", true)) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showSettingsBanner by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showSettingsBanner = true
        }
        checkExactAlarmPermission(context)
        sharedPrefs.edit().putBoolean("isFirstLaunch", false).apply()
        isFirstLaunch = false
    }

    LaunchedEffect(isFirstLaunch) {
        if (isFirstLaunch) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                showPermissionDialog = true
            } else {
                checkExactAlarmPermission(context)
                sharedPrefs.edit().putBoolean("isFirstLaunch", false).apply()
                isFirstLaunch = false
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Enable Notifications") },
            text = { Text("MediSync needs notification permission to alert you when it's time to take your medication. Without this, alarms won't work.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPermissionDialog = false
                    showSettingsBanner = true
                    sharedPrefs.edit().putBoolean("isFirstLaunch", false).apply()
                    isFirstLaunch = false
                }) {
                    Text("Not Now")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        AnimatedVisibility(
            visible = showSettingsBanner,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                color = Color(0xFFEF5350),
                contentColor = Color.White,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Notifications are disabled. Tap here to enable.", fontSize = 14.sp)
                    Text("SETTINGS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun checkExactAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}

@Composable
fun MainAppContent(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val medicationViewModel: MedicationViewModel = viewModel()
    
    val items = listOf(
        Screen.Today,
        Screen.Triage,
        Screen.Doctors,
        Screen.Account
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "iconScale"
                    )

                    val iconColor by animateColorAsState(
                        targetValue = if (selected) Color(0xFF5C6BC0) else Color(0xFF9E9E9E),
                        label = "iconColor"
                    )

                    NavigationBarItem(
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .scale(scale),
                                    tint = iconColor
                                )
                            }
                        },
                        label = { 
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF5C6BC0),
                            selectedTextColor = Color(0xFF5C6BC0),
                            indicatorColor = Color(0xFFE8EAF6),
                            unselectedIconColor = Color(0xFF9E9E9E),
                            unselectedTextColor = Color(0xFF9E9E9E)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Today.route) { TodayScreen(viewModel = medicationViewModel) }
            composable(Screen.Triage.route) { TriageScreen() }
            composable(Screen.Doctors.route) { DoctorsScreen() }
            composable(Screen.Account.route) { AccountScreen(onLogout = onLogout, viewModel = medicationViewModel) }
        }
    }
}

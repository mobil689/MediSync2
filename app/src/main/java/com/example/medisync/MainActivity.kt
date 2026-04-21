package com.example.medisync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
                    LoginScreen(onLoginSuccess = { isLoggedIn = true })
                } else {
                    MainAppContent(onLogout = { isLoggedIn = false })
                }
            }
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

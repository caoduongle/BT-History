package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.DeviceDetailScreen
import com.example.ui.screens.DeviceListScreen
import com.example.ui.screens.PermissionOnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DeviceViewModel
import com.example.ui.viewmodel.DeviceViewModelFactory
import com.example.util.BluetoothHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BtWatcherApplication
        val viewModelFactory = DeviceViewModelFactory(
            repository = app.repository,
            preferencesRepository = app.preferencesRepository,
            context = this
        )

        setContent {
            MyApplicationTheme {
                val deviceViewModel: DeviceViewModel = viewModel(factory = viewModelFactory)
                BtWatcherApp(
                    viewModel = deviceViewModel,
                    hasPermissions = BluetoothHelper.hasBluetoothConnectPermission(this) &&
                            BluetoothHelper.hasLocationPermission(this)
                )
            }
        }
    }
}

@Composable
fun BtWatcherApp(
    viewModel: DeviceViewModel,
    hasPermissions: Boolean
) {
    val navController = rememberNavController()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()

    val startDestination = if (isOnboardingCompleted || hasPermissions) {
        "device_list"
    } else {
        "onboarding"
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoBackground)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("onboarding") {
                PermissionOnboardingScreen(
                    onPermissionsGranted = {
                        viewModel.completeOnboarding()
                        navController.navigate("device_list") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            composable("device_list") {
                DeviceListScreen(
                    viewModel = viewModel,
                    onDeviceClick = { deviceId ->
                        viewModel.selectDevice(deviceId)
                        navController.navigate("device_detail")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            composable("device_detail") {
                DeviceDetailScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}


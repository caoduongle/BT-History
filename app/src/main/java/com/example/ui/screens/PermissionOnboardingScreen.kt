package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.WarningBanner
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.BentoGreen
import com.example.ui.theme.BentoGreenBg
import com.example.ui.theme.BentoOutline
import com.example.ui.theme.BentoPurpleLight
import com.example.ui.theme.BentoPurplePrimary
import com.example.ui.theme.BentoSurface
import com.example.ui.theme.BentoSurfaceVariant
import com.example.ui.theme.BentoTextMuted
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.util.BluetoothHelper

@Composable
fun PermissionOnboardingScreen(
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasBtPermission by remember { mutableStateOf(BluetoothHelper.hasBluetoothConnectPermission(context)) }
    var hasLocPermission by remember { mutableStateOf(BluetoothHelper.hasLocationPermission(context)) }
    var hasNotifPermission by remember { mutableStateOf(BluetoothHelper.hasNotificationPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasBtPermission = BluetoothHelper.hasBluetoothConnectPermission(context)
        hasLocPermission = BluetoothHelper.hasLocationPermission(context)
        hasNotifPermission = BluetoothHelper.hasNotificationPermission(context)

        val allGranted = hasBtPermission && hasLocPermission
        if (allGranted) {
            onPermissionsGranted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BentoBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // App Logo & Header
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(BentoPurplePrimary)
                .border(1.dp, BentoPurpleLight.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = stringResource(R.string.app_name),
                tint = BentoPurpleLight,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = BentoTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = BentoTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Important Note / Disclaimer Card
        WarningBanner()

        Spacer(modifier = Modifier.height(20.dp))

        // Permission Items Cards in Bento Style
        PermissionItemCard(
            icon = Icons.Default.Bluetooth,
            title = stringResource(R.string.permission_bt_title),
            description = stringResource(R.string.permission_bt_desc),
            isGranted = hasBtPermission
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionItemCard(
            icon = Icons.Default.LocationOn,
            title = stringResource(R.string.permission_loc_title),
            description = stringResource(R.string.permission_loc_desc),
            isGranted = hasLocPermission
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionItemCard(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.permission_notif_title),
            description = stringResource(R.string.permission_notif_desc),
            isGranted = hasNotifPermission
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons
        Button(
            onClick = {
                val permissionsToRequest = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                    permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("grant_permissions_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BentoPurplePrimary,
                contentColor = BentoPurpleLight
            )
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = BentoPurpleLight,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.btn_grant_and_start),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BentoPurpleLight
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Continue anyway or open settings
        OutlinedButton(
            onClick = onPermissionsGranted,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("skip_onboarding_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = BentoTextSecondary,
                containerColor = BentoSurfaceVariant
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, BentoOutline)
        ) {
            Text(
                text = stringResource(R.string.btn_skip_to_main),
                style = MaterialTheme.typography.bodyMedium,
                color = BentoTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, if (isGranted) BentoGreen.copy(alpha = 0.5f) else BentoOutline, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = BentoSurface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isGranted) BentoGreenBg else BentoPurplePrimary)
                    .border(1.dp, if (isGranted) BentoGreen.copy(alpha = 0.3f) else BentoPurpleLight.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) BentoGreen else BentoPurpleLight,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary
                    )

                    if (isGranted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.status_granted),
                                tint = BentoGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.status_granted),
                                style = MaterialTheme.typography.labelSmall,
                                color = BentoGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = BentoTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}


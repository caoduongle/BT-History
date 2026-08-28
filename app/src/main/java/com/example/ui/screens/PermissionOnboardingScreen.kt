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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.WarningBanner
import com.example.ui.theme.ConnectGreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryContainer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurfaceCard
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
    ) { permissions ->
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
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // App Logo & Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(CyanPrimaryContainer)
                .border(2.dp, CyanPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "BT Watcher",
                tint = CyanPrimary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Chào mừng tới BT Watcher",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Để ghi lại lịch sử kết nối và vị trí GPS của các thiết bị Bluetooth, ứng dụng cần các quyền hệ thống sau:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Important Note / Disclaimer Card
        WarningBanner()

        Spacer(modifier = Modifier.height(20.dp))

        // Permission Items Cards
        PermissionItemCard(
            icon = Icons.Default.Bluetooth,
            title = "1. Quyền Bluetooth",
            description = "Nhận diện tên và trạng thái kết nối/ngắt kết nối của tai nghe, loa, đồng hồ đã ghép đôi.",
            isGranted = hasBtPermission
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionItemCard(
            icon = Icons.Default.LocationOn,
            title = "2. Quyền Vị trí (GPS)",
            description = "Lấy tọa độ GPS ngay tại thời điểm thiết bị kết nối hoặc ngắt kết nối để bạn biết thiết bị ở đâu.",
            isGranted = hasLocPermission
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionItemCard(
            icon = Icons.Default.Notifications,
            title = "3. Quyền Thông báo",
            description = "Duy trì dịch vụ chạy nền liên tục và gửi cảnh báo ngay nếu thiết bị ngắt kết nối bất ngờ.",
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
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cấp quyền & Bắt đầu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Continue anyway or open settings
        OutlinedButton(
            onClick = onPermissionsGranted,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("skip_onboarding_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                text = "Bỏ qua / Vào giao diện chính",
                style = MaterialTheme.typography.bodyMedium
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
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, if (isGranted) ConnectGreen.copy(alpha = 0.5f) else DarkOutline, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceCard
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isGranted) ConnectGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (isGranted) ConnectGreen.copy(alpha = 0.3f) else DarkOutline, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) ConnectGreen else CyanPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isGranted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Đã cấp",
                                tint = ConnectGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Đã cấp",
                                style = MaterialTheme.typography.labelSmall,
                                color = ConnectGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

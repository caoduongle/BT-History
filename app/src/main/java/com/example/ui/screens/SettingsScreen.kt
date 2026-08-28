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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.WarningBanner
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.BentoCardElevated
import com.example.ui.theme.BentoGreen
import com.example.ui.theme.BentoOutline
import com.example.ui.theme.BentoPurpleContainer
import com.example.ui.theme.BentoPurpleLight
import com.example.ui.theme.BentoPurplePrimary
import com.example.ui.theme.BentoRed
import com.example.ui.theme.BentoRedBg
import com.example.ui.theme.BentoSurface
import com.example.ui.theme.BentoSurfaceVariant
import com.example.ui.theme.BentoTextMuted
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DeviceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isServiceEnabled by viewModel.isServiceEnabled.collectAsStateWithLifecycle()
    val isDisconnectAlertEnabled by viewModel.isDisconnectAlertEnabled.collectAsStateWithLifecycle()

    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions updated
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BentoBackground)
            .testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cài đặt & Giới thiệu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = BentoTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BentoBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group: Cấu hình Dịch vụ & Cảnh báo (Bento Tile)
            SectionTitle("DỊCH VỤ & CẢNH BÁO")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, BentoOutline, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Switch 1: Foreground Service
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dịch vụ ghi log chạy nền",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Chạy Foreground Service với thông báo nhỏ gọn để Android không giải phóng bộ nhớ khi tắt màn hình.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoTextSecondary,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = isServiceEnabled,
                            onCheckedChange = { viewModel.toggleService(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BentoPurpleLight,
                                checkedTrackColor = BentoPurplePrimary,
                                uncheckedThumbColor = BentoTextMuted,
                                uncheckedTrackColor = BentoSurfaceVariant
                            ),
                            modifier = Modifier.testTag("service_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BentoOutline))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch 2: Disconnect Alerts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cảnh báo ngắt kết nối tức thì",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Phát cảnh báo âm thanh và rung kèm tọa độ ngay khi tai nghe/loa bị ngắt — phòng ngừa bỏ quên thiết bị.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoTextSecondary,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = isDisconnectAlertEnabled,
                            onCheckedChange = { viewModel.toggleDisconnectAlert(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BentoPurpleLight,
                                checkedTrackColor = BentoPurplePrimary,
                                uncheckedThumbColor = BentoTextMuted,
                                uncheckedTrackColor = BentoSurfaceVariant
                            ),
                            modifier = Modifier.testTag("alert_toggle_switch")
                        )
                    }
                }
            }

            // Group: Thao tác Dữ liệu & Thiết bị (Bento Tile)
            SectionTitle("THIẾT BỊ & DỮ LIỆU")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, BentoOutline, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Sync paired devices
                    Button(
                        onClick = { viewModel.syncPairedDevices() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoPurplePrimary,
                            contentColor = BentoPurpleLight
                        )
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quét & đồng bộ thiết bị đã ghép đôi", fontWeight = FontWeight.SemiBold)
                    }

                    // Request/Check permissions button
                    OutlinedButton(
                        onClick = {
                            val perms = mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                perms.add(Manifest.permission.BLUETOOTH_CONNECT)
                                perms.add(Manifest.permission.BLUETOOTH_SCAN)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BentoPurpleLight,
                            containerColor = BentoSurfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoOutline)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kiểm tra & Cấp lại quyền hệ thống", fontWeight = FontWeight.SemiBold)
                    }

                    // Clear All History
                    OutlinedButton(
                        onClick = { showClearHistoryDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BentoRed,
                            containerColor = BentoRedBg
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoRed.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp), tint = BentoRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Xóa toàn bộ lịch sử thiết bị & sự kiện", fontWeight = FontWeight.SemiBold, color = BentoRed)
                    }
                }
            }

            // Group: Bento Notes & Information
            SectionTitle("LƯU Ý QUAN TRỌNG & NGUYÊN LÝ HOẠT ĐỘNG")
            WarningBanner()

            // Tech Info Bento Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, BentoOutline, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = BentoSurfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = BentoPurpleLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lưu trữ cục bộ an toàn (Room SQLite)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mọi thông tin thiết bị, thời gian và tọa độ được lưu trữ an toàn 100% trên điện thoại của bạn, không gửi lên bất kỳ máy chủ nào.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = BentoPurpleLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Xem bản đồ không cần API Key",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ứng dụng mở trực tiếp ứng dụng Bản đồ (Google Maps hoặc bản đồ mặc định của máy) thông qua Android Intent tiêu chuẩn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Clear All Confirmation Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Xác nhận xóa toàn bộ?", color = BentoTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Hành động này sẽ xóa vĩnh viễn danh sách mọi thiết bị và toàn bộ dòng thời gian sự kiện đã lưu trên máy.", color = BentoTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("Xóa tất cả", color = BentoRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Hủy", color = BentoPurpleLight)
                }
            },
            containerColor = BentoSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = BentoPurpleLight,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

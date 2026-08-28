package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.DeviceEntity
import com.example.ui.components.DeviceCard
import com.example.ui.components.WarningBanner
import com.example.ui.theme.BentoAmber
import com.example.ui.theme.BentoAmberBg
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.BentoCardElevated
import com.example.ui.theme.BentoGreen
import com.example.ui.theme.BentoGreenBg
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
import com.example.ui.viewmodel.TimeFilter
import com.example.util.LocationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    viewModel: DeviceViewModel,
    onDeviceClick: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val devices by viewModel.filteredDevices.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val isServiceEnabled by viewModel.isServiceEnabled.collectAsStateWithLifecycle()
    val connectedCount by viewModel.connectedCount.collectAsStateWithLifecycle()
    val todayEventsCount by viewModel.todayEventsCount.collectAsStateWithLifecycle()
    val isAlertEnabled by viewModel.isDisconnectAlertEnabled.collectAsStateWithLifecycle()

    var showSimulateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BentoBackground)
            .testTag("device_list_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BentoPurplePrimary)
                                .border(1.dp, BentoPurpleLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = BentoPurpleLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Bluetooth Watcher",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isServiceEnabled) BentoGreen else BentoTextMuted)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isServiceEnabled) "Dịch vụ ngầm đang chạy" else "Dịch vụ đã tắt",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isServiceEnabled) BentoGreen else BentoTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Sync paired devices
                    IconButton(
                        onClick = { viewModel.syncPairedDevices() },
                        modifier = Modifier.testTag("sync_paired_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Đồng bộ thiết bị đã ghép đôi",
                            tint = BentoPurpleLight
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Cài đặt",
                            tint = BentoTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSimulateDialog = true },
                containerColor = BentoPurplePrimary,
                contentColor = BentoPurpleLight,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .border(1.dp, BentoPurpleLight.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .testTag("simulate_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Mô phỏng",
                        tint = BentoPurpleLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mô phỏng",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = BentoPurpleLight
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BentoBackground)
                .testTag("devices_lazy_column"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- BENTO HERO TILES ROW ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bento Tile 1: Events Today in Bento Purple
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(BentoPurplePrimary)
                            .border(1.dp, BentoPurpleLight.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SỰ KIỆN HÔM NAY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = BentoPurpleLight.copy(alpha = 0.85f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = BentoPurpleLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "$todayEventsCount",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = BentoTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lưu GPS & mốc giờ",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoPurpleContainer.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // Bento Tile 2: Connected / Drop Alert Status
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(BentoSurfaceVariant)
                            .border(1.dp, BentoOutline, RoundedCornerShape(24.dp))
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CẢNH BÁO RƠI RỚT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = BentoPurpleLight
                                )
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (isAlertEnabled) BentoGreen else BentoTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (connectedCount > 0) BentoGreen else BentoTextMuted)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$connectedCount kết nối",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isAlertEnabled) "Chuông & rung: BẬT" else "Chuông & rung: TẮT",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAlertEnabled) BentoGreen else BentoTextMuted
                            )
                        }
                    }
                }
            }

            // --- SEARCH BAR ---
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_field"),
                    placeholder = {
                        Text(
                            "Tìm theo tên thiết bị, MAC hoặc địa chỉ...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BentoTextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = BentoPurpleLight
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Xóa",
                                    tint = BentoTextMuted
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BentoPurpleLight,
                        unfocusedBorderColor = BentoOutline,
                        focusedContainerColor = BentoSurface,
                        unfocusedContainerColor = BentoSurface,
                        focusedTextColor = BentoTextPrimary,
                        unfocusedTextColor = BentoTextPrimary
                    )
                )
            }

            // --- FILTER CHIPS ROW ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedFilter.value = filter },
                            label = {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = BentoSurface,
                                labelColor = BentoTextSecondary,
                                selectedContainerColor = BentoPurplePrimary,
                                selectedLabelColor = BentoPurpleLight
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) BentoPurpleLight else BentoOutline
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_chip_${filter.name}")
                        )
                    }
                }
            }

            // --- WARNING BANNER / OPERATION NOTES ---
            item {
                WarningBanner()
            }

            // --- SECTION TITLE ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HOẠT ĐỘNG GẦN ĐÂY (${devices.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = BentoPurpleLight
                    )
                    if (connectedCount > 0) {
                        Text(
                            text = "● $connectedCount đang kết nối",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- DEVICE ITEMS OR EMPTY STATE ---
            if (devices.isEmpty()) {
                item {
                    EmptyStateView(
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        onSyncPaired = { viewModel.syncPairedDevices() },
                        onSimulateDemo = { showSimulateDialog = true }
                    )
                }
            } else {
                items(devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        onClick = { onDeviceClick(device.id) },
                        onMapClick = if (device.lastLatitude != null && device.lastLongitude != null) {
                            {
                                LocationHelper.openLocationInMap(
                                    context = context,
                                    latitude = device.lastLatitude,
                                    longitude = device.lastLongitude,
                                    label = "${device.name} (${device.macAddress})"
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }

    // Quick Simulation Event Dialog
    if (showSimulateDialog) {
        SimulateEventDialog(
            onDismiss = { showSimulateDialog = false },
            onSimulate = { name, mac, type, eventType, lat, lon, address ->
                viewModel.simulateTestEvent(name, mac, type, eventType, lat, lon, address)
                showSimulateDialog = false
            }
        )
    }
}

@Composable
private fun EmptyStateView(
    searchQuery: String,
    selectedFilter: TimeFilter,
    onSyncPaired: () -> Unit,
    onSimulateDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BentoSurface)
            .border(1.dp, BentoOutline, RoundedCornerShape(24.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BentoPurplePrimary)
                    .border(1.dp, BentoPurpleLight.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BluetoothSearching,
                    contentDescription = null,
                    tint = BentoPurpleLight,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (searchQuery.isNotBlank() || selectedFilter != TimeFilter.ALL)
                    "Không tìm thấy thiết bị phù hợp"
                else
                    "Chưa có lịch sử kết nối Bluetooth",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BentoTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (searchQuery.isNotBlank() || selectedFilter != TimeFilter.ALL)
                    "Thử xóa bộ lọc hoặc tìm kiếm với từ khóa khác."
                else
                    "Khi kết nối hoặc ngắt kết nối với tai nghe, loa, đồng hồ Bluetooth, app sẽ tự động ghi lại thời gian và vị trí GPS.",
                style = MaterialTheme.typography.bodySmall,
                color = BentoTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSyncPaired,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BentoPurplePrimary,
                    contentColor = BentoPurpleLight
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lấy thiết bị đã ghép đôi", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onSimulateDemo,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BentoPurpleLight,
                    containerColor = BentoSurfaceVariant
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BentoOutline)
                )
            ) {
                Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thêm dữ liệu mẫu để thử nghiệm", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SimulateEventDialog(
    onDismiss: () -> Unit,
    onSimulate: (name: String, mac: String, type: String, eventType: String, lat: Double, lon: Double, address: String) -> Unit
) {
    val presets = listOf(
        Pair("AirPods Pro 2", "HEADSET") to Pair(10.7769, 106.7009) to "Phố đi bộ Nguyễn Huệ, Quận 1, TP.HCM",
        Pair("Sony WH-1000XM5", "HEADSET") to Pair(10.7828, 106.6983) to "Nhà thờ Đức Bà, Quận 1, TP.HCM",
        Pair("Apple Watch Series 9", "WATCH") to Pair(21.0285, 105.8542) to "Hồ Hoàn Kiếm, Hà Nội",
        Pair("JBL Charge 5", "SPEAKER") to Pair(16.0544, 108.2022) to "Cầu Rồng, Đà Nẵng",
        Pair("Mazda BT-Audio", "CAR") to Pair(10.8231, 106.6297) to "Sân bay Tân Sơn Nhất, TP.HCM"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Mô phỏng sự kiện Bluetooth",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BentoTextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Chọn một thiết bị mẫu và sự kiện để tạo dữ liệu tức thì (phù hợp kiểm tra trên máy ảo Emulator):",
                    style = MaterialTheme.typography.bodySmall,
                    color = BentoTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                presets.forEach { item ->
                    val (devPair, locPair) = item.first
                    val (name, type) = devPair
                    val (lat, lon) = locPair
                    val address = item.second
                    val mac = "AA:BB:CC:${(10..99).random()}:${(10..99).random()}:01"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoSurfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoOutline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                            Text(
                                text = "📍 $address",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoTextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { onSimulate(name, mac, type, "CONNECT", lat, lon, address) }
                                ) {
                                    Text("Kết nối", color = BentoGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(
                                    onClick = { onSimulate(name, mac, type, "DISCONNECT", lat, lon, address) }
                                ) {
                                    Text("Ngắt kết nối", color = BentoRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = BentoPurpleLight)
            }
        },
        containerColor = BentoSurface,
        shape = RoundedCornerShape(24.dp)
    )
}


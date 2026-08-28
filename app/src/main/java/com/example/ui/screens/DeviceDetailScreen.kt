package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.DeviceEntity
import com.example.data.entity.EventEntity
import com.example.ui.components.StatusBadge
import com.example.ui.components.TimelineItem
import com.example.ui.components.getDeviceIcon
import com.example.ui.theme.ConnectGreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryContainer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DisconnectRed
import com.example.ui.theme.DisconnectRedBg
import com.example.ui.theme.GpsBlue
import com.example.ui.viewmodel.DeviceViewModel
import com.example.util.LocationHelper
import com.example.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    viewModel: DeviceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val device by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val events by viewModel.selectedDeviceEvents.collectAsStateWithLifecycle()
    val lastDisconnectEvent by viewModel.lastSeenDisconnectEvent.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val currentDevice = device
    if (currentDevice == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Không tìm thấy thông tin thiết bị", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("device_detail_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentDevice.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        Text(
                            text = currentDevice.macAddress,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Tùy chọn",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(DarkSurfaceCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mô phỏng Kết nối") },
                            onClick = {
                                showMenu = false
                                viewModel.simulateTestEvent(
                                    deviceName = currentDevice.name,
                                    macAddress = currentDevice.macAddress,
                                    deviceType = currentDevice.deviceType,
                                    eventType = "CONNECT"
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = ConnectGreen)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Mô phỏng Ngắt kết nối") },
                            onClick = {
                                showMenu = false
                                viewModel.simulateTestEvent(
                                    deviceName = currentDevice.name,
                                    macAddress = currentDevice.macAddress,
                                    deviceType = currentDevice.deviceType,
                                    eventType = "DISCONNECT"
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.BluetoothDisabled, contentDescription = null, tint = DisconnectRed)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Xóa thiết bị này", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
                .testTag("detail_lazy_column")
        ) {
            // Top Summary Card
            item {
                DeviceHeaderCard(
                    device = currentDevice,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Prominent "Lần cuối cùng thấy thiết bị" (Last seen) Card
            item {
                LastSeenCard(
                    device = currentDevice,
                    lastDisconnectEvent = lastDisconnectEvent,
                    onOpenMap = { lat, lon ->
                        LocationHelper.openLocationInMap(
                            context = context,
                            latitude = lat,
                            longitude = lon,
                            label = "Lần cuối thấy ${currentDevice.name}"
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Timeline Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lịch sử sự kiện (${events.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Mới nhất ở trên",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Timeline Items
            if (events.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có sự kiện nào được ghi nhận cho thiết bị này.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
                    TimelineItem(
                        event = event,
                        deviceName = currentDevice.name,
                        isFirst = index == 0,
                        isLast = index == events.lastIndex
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Xóa thiết bị?") },
            text = { Text("Toàn bộ lịch sử kết nối và tọa độ GPS của \"${currentDevice.name}\" sẽ bị xóa khỏi máy.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDevice(currentDevice)
                        showDeleteConfirm = false
                        onBack()
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Hủy")
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun DeviceHeaderCard(
    device: DeviceEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DarkOutline, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyanPrimaryContainer)
                    .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDeviceIcon(device.deviceType),
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Loại: ${device.deviceType} • MAC: ${device.macAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusBadge(isConnected = device.isConnected, timestamp = device.lastEventTimestamp)
            }
        }
    }
}

@Composable
private fun LastSeenCard(
    device: DeviceEntity,
    lastDisconnectEvent: EventEntity?,
    onOpenMap: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine last seen location & timestamp from either lastDisconnectEvent or device state
    val timestamp = lastDisconnectEvent?.timestamp ?: device.lastEventTimestamp
    val latitude = lastDisconnectEvent?.latitude ?: device.lastLatitude
    val longitude = lastDisconnectEvent?.longitude ?: device.lastLongitude
    val address = lastDisconnectEvent?.locationAddress ?: device.lastLocationAddress
    val hasLocation = latitude != null && longitude != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, if (hasLocation) CyanPrimary.copy(alpha = 0.6f) else DarkOutline, RoundedCornerShape(16.dp))
            .testTag("last_seen_card"),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GpsBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PinDrop,
                        contentDescription = "Last seen",
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "LẦN CUỐI CÙNG THẤY THIẾT BỊ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Thời gian & vị trí lần ngắt kết nối gần nhất",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = TimeFormatter.formatFullDateTime(timestamp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "(${TimeFormatter.formatRelativeTime(timestamp)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Location row
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    if (hasLocation) {
                        Text(
                            text = address ?: "Tọa độ GPS:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = TimeFormatter.formatCoordinates(latitude, longitude),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyanPrimary,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "Chưa ghi nhận tọa độ GPS cho lần ngắt này",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent Map Button
            Button(
                onClick = {
                    if (latitude != null && longitude != null) {
                        onOpenMap(latitude, longitude)
                    }
                },
                enabled = hasLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_map_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = DarkOutline,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasLocation) "Xem vị trí trên bản đồ" else "Không có tọa độ để mở bản đồ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

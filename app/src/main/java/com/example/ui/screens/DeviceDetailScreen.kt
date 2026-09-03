package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.DeviceEntity
import com.example.data.entity.EventEntity
import com.example.ui.components.StatusBadge
import com.example.ui.components.TimelineItem
import com.example.ui.components.getDeviceIcon
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
    val hasMoreEvents by viewModel.hasMoreDeviceEvents.collectAsStateWithLifecycle()
    val isLoadingEvents by viewModel.isDeviceEventsLoading.collectAsStateWithLifecycle()
    val lastDisconnectEvent by viewModel.lastSeenDisconnectEvent.collectAsStateWithLifecycle()
    val isSimulationAvailable by viewModel.isSimulationAvailable.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val shouldLoadMore = remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 2
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        val dev = viewModel.selectedDevice.value
        if (dev != null && shouldLoadMore.value && hasMoreEvents && !isLoadingEvents) {
            viewModel.loadMoreDeviceEvents(dev.id)
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val currentDevice = device
    if (currentDevice == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BentoBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.device_not_found), color = BentoTextMuted)
        }
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BentoBackground)
            .testTag("device_detail_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentDevice.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = currentDevice.macAddress,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = BentoTextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = BentoTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.cd_options),
                            tint = BentoTextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(BentoSurface)
                    ) {
                        if (isSimulationAvailable) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_simulate_connect), color = BentoTextPrimary) },
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
                                    Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = BentoGreen)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_simulate_disconnect), color = BentoTextPrimary) },
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
                                    Icon(Icons.Default.BluetoothDisabled, contentDescription = null, tint = BentoRed)
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_delete_device), color = BentoRed) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = BentoRed)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BentoBackground)
                .testTag("detail_lazy_column"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Summary Card in Bento Style
            item {
                DeviceHeaderCard(device = currentDevice)
            }

            // Prominent "Lần cuối cùng thấy thiết bị" (Last seen) Bento Card
            item {
                LastSeenCard(
                    device = currentDevice,
                    lastDisconnectEvent = lastDisconnectEvent,
                    onOpenMap = { lat, lon ->
                        LocationHelper.openLocationInMap(
                            context = context,
                            latitude = lat,
                            longitude = lon,
                            label = context.getString(R.string.label_last_seen_device, currentDevice.name)
                        )
                    }
                )
            }

            // Timeline Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.section_event_history, events.size),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = BentoPurpleLight
                    )

                    Text(
                        text = stringResource(R.string.label_newest_first),
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoTextMuted
                    )
                }
            }

            // Timeline Items
            if (events.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(BentoSurface)
                            .border(1.dp, BentoOutline, RoundedCornerShape(20.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.empty_device_events),
                            style = MaterialTheme.typography.bodyMedium,
                            color = BentoTextMuted
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

                if (hasMoreEvents) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingEvents) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = BentoPurpleLight,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.loadMoreDeviceEvents(currentDevice.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = BentoPurpleLight
                                    ),
                                    border = BorderStroke(1.dp, BentoPurpleLight),
                                    modifier = Modifier.testTag("btn_load_more_device_events")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.btn_load_more_events),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                } else if (events.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_more_events),
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoTextMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
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
            title = { Text(stringResource(R.string.dialog_delete_title), color = BentoTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.dialog_delete_message, currentDevice.name), color = BentoTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDevice(currentDevice)
                        showDeleteConfirm = false
                        onBack()
                    }
                ) {
                    Text(stringResource(R.string.btn_delete), color = BentoRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.btn_cancel), color = BentoPurpleLight)
                }
            },
            containerColor = BentoSurface,
            shape = RoundedCornerShape(24.dp)
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
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, BentoOutline, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = BentoSurfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BentoPurplePrimary)
                    .border(1.dp, BentoPurpleLight.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDeviceIcon(device.deviceType),
                    contentDescription = null,
                    tint = BentoPurpleLight,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.device_type_and_mac_format, device.deviceType, device.macAddress),
                    style = MaterialTheme.typography.bodySmall,
                    color = BentoTextMuted,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
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
    val timestamp = lastDisconnectEvent?.timestamp ?: device.lastEventTimestamp
    val latitude = lastDisconnectEvent?.latitude ?: device.lastLatitude
    val longitude = lastDisconnectEvent?.longitude ?: device.lastLongitude
    val address = lastDisconnectEvent?.locationAddress ?: device.lastLocationAddress
    val hasLocation = latitude != null && longitude != null
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.5.dp,
                if (hasLocation) BentoPurpleLight.copy(alpha = 0.6f) else BentoOutline,
                RoundedCornerShape(24.dp)
            )
            .testTag("last_seen_card"),
        colors = CardDefaults.cardColors(
            containerColor = BentoSurface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BentoPurplePrimary)
                        .border(1.dp, BentoPurpleLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PinDrop,
                        contentDescription = stringResource(R.string.cd_last_seen),
                        tint = BentoPurpleLight,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = stringResource(R.string.tile_last_seen_title),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BentoPurpleLight,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = stringResource(R.string.tile_last_seen_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time block
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = BentoPurpleLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = TimeFormatter.formatFullDateTime(context, timestamp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BentoTextPrimary
                    )
                    Text(
                        text = "(${TimeFormatter.formatRelativeTime(context, timestamp)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location block
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = BentoPurpleLight,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    if (hasLocation) {
                        Text(
                            text = address ?: stringResource(R.string.label_gps_coordinates),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = BentoTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = TimeFormatter.formatCoordinates(context, latitude, longitude),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = BentoPurpleLight,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.location_not_recorded_for_disconnect),
                            style = MaterialTheme.typography.bodyMedium,
                            color = BentoTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

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
                    .height(50.dp)
                    .testTag("open_map_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BentoPurplePrimary,
                    contentColor = BentoPurpleLight,
                    disabledContainerColor = BentoCardElevated,
                    disabledContentColor = BentoTextMuted
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = if (hasLocation) BentoPurpleLight else BentoTextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasLocation) stringResource(R.string.btn_view_on_map) else stringResource(R.string.btn_no_coordinates_for_map),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (hasLocation) BentoPurpleLight else BentoTextMuted
                )
            }
        }
    }
}


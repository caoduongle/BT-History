package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.example.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ui.viewmodel.ExportFormat
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.WarningBanner
import com.example.ui.theme.BentoAmber
import com.example.ui.theme.BentoAmberBg
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
import com.example.util.BluetoothHelper

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
    val isDeveloperModeEnabled by viewModel.isDeveloperModeEnabled.collectAsStateWithLifecycle()
    val historyRetentionDays by viewModel.historyRetentionDays.collectAsStateWithLifecycle()

    var devModeTapCount by remember { mutableIntStateOf(0) }
    var lastDevModeTapTime by remember { mutableLongStateOf(0L) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = viewModel.exportHistoryToUri(uri, ExportFormat.JSON)
                result.onSuccess { summary ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_export_success, summary.deviceCount, summary.eventCount),
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { err ->
                    if (err.message == "EMPTY") {
                        Toast.makeText(context, context.getString(R.string.toast_export_empty), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_export_error, err.localizedMessage ?: "Unknown"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    val retentionLabel = when (historyRetentionDays) {
        30 -> stringResource(R.string.retention_30_days)
        90 -> stringResource(R.string.retention_90_days)
        180 -> stringResource(R.string.retention_180_days)
        365 -> stringResource(R.string.retention_365_days)
        0 -> stringResource(R.string.retention_unlimited)
        else -> "$historyRetentionDays ngày"
    }

    var hasPermissions by remember {
        mutableStateOf(BluetoothHelper.hasRequiredPermissionsForService(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermissions = BluetoothHelper.hasRequiredPermissionsForService(context)
        if (hasPermissions) {
            viewModel.toggleService(true)
        }
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
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
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
            SectionTitle(stringResource(R.string.settings_section_service_alert))

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
                                text = stringResource(R.string.setting_service_name),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = stringResource(R.string.setting_service_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoTextSecondary,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = isServiceEnabled && hasPermissions,
                            onCheckedChange = { checked ->
                                if (checked && !hasPermissions) {
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
                                } else {
                                    viewModel.toggleService(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BentoPurpleLight,
                                checkedTrackColor = BentoPurplePrimary,
                                uncheckedThumbColor = BentoTextMuted,
                                uncheckedTrackColor = BentoSurfaceVariant
                            ),
                            modifier = Modifier.testTag("service_toggle_switch")
                        )
                    }

                    if (!hasPermissions) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BentoAmberBg)
                                .border(1.dp, BentoAmber.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                                .testTag("settings_missing_permissions_warning")
                        ) {
                            Text(
                                text = stringResource(R.string.setting_service_missing_perms_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoAmber,
                                lineHeight = 16.sp
                            )
                        }
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
                                text = stringResource(R.string.setting_alert_name),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = stringResource(R.string.setting_alert_desc),
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
            SectionTitle(stringResource(R.string.settings_section_devices_data))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, BentoOutline, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Row: Retention setting
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showRetentionDialog = true }
                            .padding(vertical = 6.dp)
                            .testTag("setting_retention_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_retention_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.setting_retention_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoTextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(BentoPurplePrimary.copy(alpha = 0.5f))
                                .border(1.dp, BentoPurpleLight.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("retention_setting_badge")
                        ) {
                            Text(
                                text = retentionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = BentoPurpleLight
                            )
                        }
                    }

                    // Button: Export History to File
                    OutlinedButton(
                        onClick = {
                            val filename = "bt_history_backup_${System.currentTimeMillis()}.json"
                            exportLauncher.launch(filename)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_export_history"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BentoPurpleLight,
                            containerColor = BentoSurfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoOutline)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.setting_export_title), fontWeight = FontWeight.SemiBold)
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BentoOutline))

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
                        Text(stringResource(R.string.btn_sync_paired), fontWeight = FontWeight.SemiBold)
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
                        Text(stringResource(R.string.btn_check_permissions), fontWeight = FontWeight.SemiBold)
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
                        Text(stringResource(R.string.btn_clear_all_history), fontWeight = FontWeight.SemiBold, color = BentoRed)
                    }
                }
            }

            // Group: Bento Notes & Information
            SectionTitle(stringResource(R.string.settings_section_notes))
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
                            text = stringResource(R.string.setting_storage_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.setting_storage_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = BentoPurpleLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.setting_map_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.setting_map_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Version & Easter Egg for Developer Mode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastDevModeTapTime > 3000L) {
                            devModeTapCount = 1
                        } else {
                            devModeTapCount++
                        }
                        lastDevModeTapTime = currentTime

                        if (isDeveloperModeEnabled) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_dev_mode_already_active),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (devModeTapCount >= 7) {
                            viewModel.setDeveloperModeEnabled(true)
                            devModeTapCount = 0
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_dev_mode_activated),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (devModeTapCount >= 4) {
                            val remaining = 7 - devModeTapCount
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_dev_mode_countdown, remaining),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .testTag("settings_app_version"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(
                        R.string.setting_version_format,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = BentoTextMuted,
                    fontWeight = FontWeight.Medium
                )
                if (isDeveloperModeEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.setting_developer_mode_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoPurpleLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Clear All Confirmation Dialog with Pre-Wipe Backup Advice
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_all_title), color = BentoTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = stringResource(R.string.dialog_clear_all_with_export_message),
                    color = BentoTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryDialog = false
                    },
                    modifier = Modifier.testTag("dialog_confirm_clear_btn")
                ) {
                    Text(stringResource(R.string.btn_confirm_clear_all), color = BentoRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            val filename = "bt_history_backup_${System.currentTimeMillis()}.json"
                            exportLauncher.launch(filename)
                        },
                        modifier = Modifier.testTag("dialog_export_before_clear_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = BentoPurpleLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_export_before_clear), color = BentoPurpleLight, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text(stringResource(R.string.btn_cancel), color = BentoTextMuted)
                    }
                }
            },
            containerColor = BentoSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Retention Selection Dialog
    if (showRetentionDialog) {
        val options = listOf(
            30 to stringResource(R.string.retention_30_days),
            90 to stringResource(R.string.retention_90_days),
            180 to stringResource(R.string.retention_180_days),
            365 to stringResource(R.string.retention_365_days),
            0 to stringResource(R.string.retention_unlimited)
        )
        AlertDialog(
            onDismissRequest = { showRetentionDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_retention_title),
                    color = BentoTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (days, label) ->
                        val isSelected = historyRetentionDays == days
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BentoPurplePrimary.copy(alpha = 0.4f) else BentoSurfaceVariant)
                                .border(1.dp, if (isSelected) BentoPurpleLight else BentoOutline, RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setHistoryRetentionDays(days)
                                    showRetentionDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("retention_option_$days"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BentoPurpleLight else BentoTextPrimary
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = BentoPurpleLight, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRetentionDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = BentoPurpleLight)
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

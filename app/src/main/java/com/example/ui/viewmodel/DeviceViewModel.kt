package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.DeviceEntity
import com.example.data.entity.EventEntity
import com.example.data.repository.DeviceRepository
import com.example.data.repository.PreferencesRepository
import com.example.service.BluetoothWatcherService
import com.example.util.BluetoothHelper
import com.example.util.LocationHelper
import com.example.util.TimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.annotation.StringRes
import com.example.BuildConfig
import com.example.R
import com.example.util.ExportHelper
import kotlinx.coroutines.flow.first

enum class ExportFormat {
    JSON, CSV
}

data class ExportResult(
    val deviceCount: Int,
    val eventCount: Int,
    val uri: Uri
)

enum class TimeFilter(val label: String, @get:StringRes val titleRes: Int) {
    ALL("Tất cả", R.string.filter_all),
    CONNECTED("Đang kết nối", R.string.filter_connected),
    TODAY("Hôm nay", R.string.filter_today),
    YESTERDAY("Hôm qua", R.string.filter_yesterday),
    LAST_7_DAYS("7 ngày qua", R.string.filter_last_7_days)
}

class DeviceViewModel(
    private val repository: DeviceRepository,
    private val preferencesRepository: PreferencesRepository,
    private val context: Context
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow(TimeFilter.ALL)

    val isServiceEnabled: StateFlow<Boolean> = preferencesRepository.isServiceEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isDisconnectAlertEnabled: StateFlow<Boolean> = preferencesRepository.isDisconnectAlertEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isOnboardingCompleted: StateFlow<Boolean> = preferencesRepository.isOnboardingCompletedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDeveloperModeEnabled: StateFlow<Boolean> = preferencesRepository.isDeveloperModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isSimulationAvailable: StateFlow<Boolean> = preferencesRepository.isDeveloperModeEnabledFlow
        .map { it || BuildConfig.DEBUG }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BuildConfig.DEBUG)

    val historyRetentionDays: StateFlow<Int> = preferencesRepository.historyRetentionDaysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 180)

    fun setHistoryRetentionDays(days: Int) {
        viewModelScope.launch {
            preferencesRepository.setHistoryRetentionDays(days)
            if (days > 0) {
                pruneExpiredEvents()
            }
        }
    }

    suspend fun pruneExpiredEvents(): Int {
        val days = preferencesRepository.historyRetentionDaysFlow.first()
        if (days <= 0) return 0
        val cutoff = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000L)
        val deleted = repository.deleteEventsOlderThan(cutoff)
        if (deleted > 0) {
            selectedDeviceId.value?.let { refreshDeviceEvents(it) }
            refreshTimelineEvents()
        }
        return deleted
    }

    val connectedCount: StateFlow<Int> = repository.connectedCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allEvents: StateFlow<List<EventEntity>> = repository.allEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayEventsCount: StateFlow<Int> = repository.allEventsFlow
        .map { events ->
            events.count { TimeFormatter.isToday(it.timestamp) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allDevicesList: StateFlow<List<DeviceEntity>> = repository.allDevicesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredDevices: StateFlow<List<DeviceEntity>> = combine(
        repository.allDevicesFlow,
        searchQuery,
        selectedFilter
    ) { devices, query, filter ->
        var list = devices

        // Apply text search
        if (query.isNotBlank()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.macAddress.contains(query, ignoreCase = true) ||
                        (it.lastLocationAddress?.contains(query, ignoreCase = true) == true)
            }
        }

        // Apply time & status filter
        when (filter) {
            TimeFilter.ALL -> list
            TimeFilter.CONNECTED -> list.filter { it.isConnected }
            TimeFilter.TODAY -> list.filter { TimeFormatter.isToday(it.lastEventTimestamp) }
            TimeFilter.YESTERDAY -> list.filter { TimeFormatter.isYesterday(it.lastEventTimestamp) }
            TimeFilter.LAST_7_DAYS -> list.filter { TimeFormatter.isWithinLastNDays(it.lastEventTimestamp, 7) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Device Detail
    val selectedDeviceId = MutableStateFlow<Long?>(null)

    val selectedDevice: StateFlow<DeviceEntity?> = selectedDeviceId.flatMapLatest { id ->
        if (id != null) repository.getDeviceByIdFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- PAGINATED DEVICE EVENTS ---
    val deviceEventsPageSize = 30
    private var currentDeviceOffset = 0
    private val _paginatedDeviceEvents = MutableStateFlow<List<EventEntity>>(emptyList())
    val paginatedDeviceEvents: StateFlow<List<EventEntity>> = _paginatedDeviceEvents
    val selectedDeviceEvents: StateFlow<List<EventEntity>> = _paginatedDeviceEvents
    val hasMoreDeviceEvents = MutableStateFlow(false)
    val isDeviceEventsLoading = MutableStateFlow(false)

    fun loadInitialDeviceEvents(deviceId: Long) {
        currentDeviceOffset = 0
        viewModelScope.launch {
            isDeviceEventsLoading.value = true
            val page = repository.getEventsByDeviceIdPaged(deviceId, limit = deviceEventsPageSize, offset = 0)
            _paginatedDeviceEvents.value = page
            currentDeviceOffset = page.size
            hasMoreDeviceEvents.value = page.size >= deviceEventsPageSize
            isDeviceEventsLoading.value = false
        }
    }

    fun loadMoreDeviceEvents(deviceId: Long) {
        if (isDeviceEventsLoading.value || !hasMoreDeviceEvents.value) return
        viewModelScope.launch {
            isDeviceEventsLoading.value = true
            val nextPage = repository.getEventsByDeviceIdPaged(deviceId, limit = deviceEventsPageSize, offset = currentDeviceOffset)
            _paginatedDeviceEvents.value = _paginatedDeviceEvents.value + nextPage
            currentDeviceOffset += nextPage.size
            hasMoreDeviceEvents.value = nextPage.size >= deviceEventsPageSize
            isDeviceEventsLoading.value = false
        }
    }

    fun refreshDeviceEvents(deviceId: Long) {
        val countToLoad = if (currentDeviceOffset > 0) currentDeviceOffset else deviceEventsPageSize
        viewModelScope.launch {
            val page = repository.getEventsByDeviceIdPaged(deviceId, limit = countToLoad, offset = 0)
            _paginatedDeviceEvents.value = page
            hasMoreDeviceEvents.value = page.size >= countToLoad
        }
    }

    // --- PAGINATED TIMELINE (GLOBAL) ---
    val timelinePageSize = 50
    private var currentTimelineOffset = 0
    private val _paginatedTimelineEvents = MutableStateFlow<List<EventEntity>>(emptyList())
    val paginatedTimelineEvents: StateFlow<List<EventEntity>> = _paginatedTimelineEvents
    val hasMoreTimelineEvents = MutableStateFlow(false)
    val isTimelineLoading = MutableStateFlow(false)

    fun loadInitialTimelineEvents() {
        currentTimelineOffset = 0
        viewModelScope.launch {
            isTimelineLoading.value = true
            val page = repository.getEventsPaged(limit = timelinePageSize, offset = 0)
            _paginatedTimelineEvents.value = page
            currentTimelineOffset = page.size
            hasMoreTimelineEvents.value = page.size >= timelinePageSize
            isTimelineLoading.value = false
        }
    }

    fun loadMoreTimelineEvents() {
        if (isTimelineLoading.value || !hasMoreTimelineEvents.value) return
        viewModelScope.launch {
            isTimelineLoading.value = true
            val nextPage = repository.getEventsPaged(limit = timelinePageSize, offset = currentTimelineOffset)
            _paginatedTimelineEvents.value = _paginatedTimelineEvents.value + nextPage
            currentTimelineOffset += nextPage.size
            hasMoreTimelineEvents.value = nextPage.size >= timelinePageSize
            isTimelineLoading.value = false
        }
    }

    fun refreshTimelineEvents() {
        val countToLoad = if (currentTimelineOffset > 0) currentTimelineOffset else timelinePageSize
        viewModelScope.launch {
            val page = repository.getEventsPaged(limit = countToLoad, offset = 0)
            _paginatedTimelineEvents.value = page
            hasMoreTimelineEvents.value = page.size >= countToLoad
        }
    }

    val lastSeenDisconnectEvent = MutableStateFlow<EventEntity?>(null)

    fun selectDevice(deviceId: Long) {
        selectedDeviceId.value = deviceId
        viewModelScope.launch {
            lastSeenDisconnectEvent.value = repository.getLastDisconnectEvent(deviceId)
        }
        loadInitialDeviceEvents(deviceId)
    }

    fun clearSelectedDevice() {
        selectedDeviceId.value = null
        lastSeenDisconnectEvent.value = null
        _paginatedDeviceEvents.value = emptyList()
        currentDeviceOffset = 0
        hasMoreDeviceEvents.value = false
    }

    init {
        loadInitialTimelineEvents()
        viewModelScope.launch {
            pruneExpiredEvents()
        }
    }

    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !BluetoothHelper.hasRequiredPermissionsForService(context)) {
                preferencesRepository.setServiceEnabled(false)
                return@launch
            }
            preferencesRepository.setServiceEnabled(enabled)
            if (enabled) {
                BluetoothWatcherService.startService(context)
            } else {
                BluetoothWatcherService.stopService(context)
            }
        }
    }

    fun toggleDisconnectAlert(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDisconnectAlertEnabled(enabled)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            if (BluetoothHelper.hasRequiredPermissionsForService(context)) {
                preferencesRepository.setServiceEnabled(true)
                BluetoothWatcherService.startService(context)
            } else {
                preferencesRepository.setServiceEnabled(false)
            }
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            if (!BluetoothHelper.hasRequiredPermissionsForService(context)) {
                preferencesRepository.setServiceEnabled(false)
            } else if (isServiceEnabled.value) {
                BluetoothWatcherService.startService(context)
            }
        }
    }

    fun setDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDeveloperModeEnabled(enabled)
        }
    }

    fun deleteDevice(device: DeviceEntity) {
        viewModelScope.launch {
            repository.deleteDevice(device)
            if (selectedDeviceId.value == device.id) {
                clearSelectedDevice()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            clearSelectedDevice()
            _paginatedTimelineEvents.value = emptyList()
            currentTimelineOffset = 0
            hasMoreTimelineEvents.value = false
        }
    }

    suspend fun exportHistoryToUri(uri: Uri, format: ExportFormat = ExportFormat.JSON): Result<ExportResult> {
        return try {
            val devices = repository.getAllDevices()
            val events = repository.getAllEvents()
            if (devices.isEmpty() && events.isEmpty()) {
                return Result.failure(IllegalStateException("NO_DATA"))
            }
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return Result.failure(IllegalStateException("CANNOT_OPEN_URI"))
            outputStream.use { stream ->
                when (format) {
                    ExportFormat.JSON -> ExportHelper.exportToJson(stream, devices, events)
                    ExportFormat.CSV -> ExportHelper.exportToCsv(stream, devices, events)
                }
            }
            Result.success(ExportResult(devices.size, events.size, uri))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    fun syncPairedDevices() {
        if (!BluetoothHelper.hasBluetoothConnectPermission(context)) return

        viewModelScope.launch {
            try {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
                val pairedDevices = adapter?.bondedDevices ?: emptySet()

                for (device in pairedDevices) {
                    val mac = device.address ?: continue
                    val existing = repository.getDeviceByMac(mac)
                    if (existing == null) {
                        val name = BluetoothHelper.getSafeDeviceName(context, device)
                        val type = BluetoothHelper.determineDeviceType(context, device)
                        val loc = LocationHelper.getCurrentLocation(context)

                        repository.recordBluetoothEvent(
                            name = name,
                            macAddress = mac,
                            deviceType = type,
                            eventType = "DISCONNECT",
                            timestamp = System.currentTimeMillis() - 3600000L,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy,
                            locationAddress = loc.address,
                            isUnexpectedDisconnect = false
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Demo / Simulation mode for emulator or testing without physical devices
    fun simulateTestEvent(
        deviceName: String,
        macAddress: String,
        deviceType: String,
        eventType: String,
        latitude: Double = 10.7769,
        longitude: Double = 106.7009,
        address: String = "Quận 1, TP. Hồ Chí Minh"
    ) {
        viewModelScope.launch {
            val isDisconnect = eventType == "DISCONNECT"
            repository.recordBluetoothEvent(
                name = deviceName,
                macAddress = macAddress,
                deviceType = deviceType,
                eventType = eventType,
                timestamp = System.currentTimeMillis(),
                latitude = latitude,
                longitude = longitude,
                accuracy = 10.0f,
                locationAddress = address,
                isUnexpectedDisconnect = isDisconnect
            )
            // Reload last disconnect event if current device is selected
            refreshTimelineEvents()
            selectedDeviceId.value?.let { id ->
                val current = repository.getDeviceById(id)
                if (current?.macAddress == macAddress) {
                    lastSeenDisconnectEvent.value = repository.getLastDisconnectEvent(id)
                    refreshDeviceEvents(id)
                }
            }
        }
    }
}

class DeviceViewModelFactory(
    private val repository: DeviceRepository,
    private val preferencesRepository: PreferencesRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceViewModel::class.java)) {
            return DeviceViewModel(repository, preferencesRepository, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

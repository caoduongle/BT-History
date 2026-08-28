package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.repository.DeviceRepository
import com.example.data.repository.PreferencesRepository
import com.example.util.NotificationHelper

class BtWatcherApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: DeviceRepository by lazy { DeviceRepository(database.deviceDao(), database.eventDao()) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.createNotificationChannels(this)
    }

    companion object {
        lateinit var instance: BtWatcherApplication
            private set
    }
}

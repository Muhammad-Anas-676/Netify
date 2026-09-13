package com.netify.app.di

import android.content.Context
import com.netify.app.data.datastore.SettingsDataStore
import com.netify.app.data.local.AppDatabase
import com.netify.app.data.local.vault.VaultStorage
import com.netify.app.data.repository.WifiRepository
import com.netify.app.domain.DeviceScanner
import com.netify.app.domain.SpeedTestEngine

/**
 * Simple, explicit dependency container (no Hilt/Dagger) so the project
 * builds cleanly without extra annotation-processor setup. Everything here
 * is a plain, cheap-to-construct singleton created once in
 * [com.netify.app.NetifyApplication].
 */
class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getInstance(context)
    val settingsDataStore: SettingsDataStore = SettingsDataStore(context)
    val vaultStorage: VaultStorage = VaultStorage(context)
    val wifiRepository: WifiRepository = WifiRepository(context)
    val speedTestEngine: SpeedTestEngine = SpeedTestEngine()
    val deviceScanner: DeviceScanner = DeviceScanner()
}

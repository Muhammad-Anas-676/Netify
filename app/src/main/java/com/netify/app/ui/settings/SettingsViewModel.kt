package com.netify.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.netify.app.data.datastore.ThemeMode
import com.netify.app.di.AppContainer
import com.netify.app.worker.ScheduledTestWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: ThemeMode = ThemeMode.LIGHT,
    val unitsMbps: Boolean = true,
    val language: String = "en",
    val ispPlanMbps: Int = 50,
    val alertThresholdPct: Int = 60,
    val autoTestEnabled: Boolean = false,
    val autoTestHour: Int = 9
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        val ds = container.settingsDataStore
        viewModelScope.launch { ds.themeMode.collect { v -> _state.value = _state.value.copy(theme = v) } }
        viewModelScope.launch { ds.unitsMbps.collect { v -> _state.value = _state.value.copy(unitsMbps = v) } }
        viewModelScope.launch { ds.language.collect { v -> _state.value = _state.value.copy(language = v) } }
        viewModelScope.launch { ds.ispPlanMbps.collect { v -> _state.value = _state.value.copy(ispPlanMbps = v) } }
        viewModelScope.launch { ds.alertThresholdPct.collect { v -> _state.value = _state.value.copy(alertThresholdPct = v) } }
        viewModelScope.launch { ds.autoTestEnabled.collect { v -> _state.value = _state.value.copy(autoTestEnabled = v) } }
        viewModelScope.launch { ds.autoTestHour.collect { v -> _state.value = _state.value.copy(autoTestHour = v) } }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { container.settingsDataStore.setThemeMode(mode) }
    fun setUnitsMbps(mbps: Boolean) = viewModelScope.launch { container.settingsDataStore.setUnitsMbps(mbps) }
    fun setLanguage(code: String) = viewModelScope.launch { container.settingsDataStore.setLanguage(code) }
    fun setIspPlan(mbps: Int) = viewModelScope.launch { container.settingsDataStore.setIspPlanMbps(mbps) }
    fun setAlertThreshold(pct: Int) = viewModelScope.launch { container.settingsDataStore.setAlertThresholdPct(pct) }

    fun setAutoTestEnabled(enabled: Boolean, appContext: android.content.Context) {
        viewModelScope.launch {
            container.settingsDataStore.setAutoTestEnabled(enabled)
            if (enabled) {
                ScheduledTestWorker.scheduleDailyAt(appContext, _state.value.autoTestHour)
            } else {
                ScheduledTestWorker.cancel(appContext)
            }
        }
    }

    fun setAutoTestHour(hour: Int, appContext: android.content.Context) {
        viewModelScope.launch {
            container.settingsDataStore.setAutoTestHour(hour)
            if (_state.value.autoTestEnabled) {
                ScheduledTestWorker.scheduleDailyAt(appContext, hour)
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(container) as T
        }
    }
}

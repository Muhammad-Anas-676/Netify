package com.netify.app.ui.speed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.netify.app.data.local.SpeedTestEntity
import com.netify.app.di.AppContainer
import com.netify.app.domain.model.SpeedTestProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SpeedUiState(
    val progress: SpeedTestProgress = SpeedTestProgress.Idle,
    val isRunning: Boolean = false,
    val serverIndex: Int = 0,
    val ispPlanMbps: Int = 50,
    val lastDownload: Double? = null,
    val lastUpload: Double? = null,
    val lastPing: Double? = null,
    val lastJitter: Double? = null,
    val autoTestEnabled: Boolean = false
)

class SpeedViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(SpeedUiState())
    val state: StateFlow<SpeedUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                ispPlanMbps = container.settingsDataStore.ispPlanMbps.first(),
                autoTestEnabled = container.settingsDataStore.autoTestEnabled.first()
            )
        }
    }

    fun cycleServer() {
        val count = com.netify.app.domain.SpeedTestEngine.DEFAULT_SERVERS.size
        _state.value = _state.value.copy(serverIndex = (_state.value.serverIndex + 1) % count)
    }

    fun runTest() {
        if (_state.value.isRunning) return
        val server = com.netify.app.domain.SpeedTestEngine.DEFAULT_SERVERS[_state.value.serverIndex]
        _state.value = _state.value.copy(isRunning = true, progress = SpeedTestProgress.MeasuringPing)

        viewModelScope.launch {
            container.speedTestEngine.runFullTest(server).collect { progress ->
                _state.value = _state.value.copy(progress = progress)
                if (progress is SpeedTestProgress.Done) {
                    _state.value = _state.value.copy(
                        isRunning = false,
                        lastDownload = progress.result.downloadMbps,
                        lastUpload = progress.result.uploadMbps,
                        lastPing = progress.result.pingMs,
                        lastJitter = progress.result.jitterMs
                    )
                    container.database.speedTestDao().insert(
                        SpeedTestEntity(
                            downloadMbps = progress.result.downloadMbps,
                            uploadMbps = progress.result.uploadMbps,
                            pingMs = progress.result.pingMs,
                            jitterMs = progress.result.jitterMs,
                            serverName = progress.result.serverName,
                            timestamp = progress.result.timestamp
                        )
                    )
                } else if (progress is SpeedTestProgress.Failed) {
                    _state.value = _state.value.copy(isRunning = false)
                }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SpeedViewModel(container) as T
        }
    }
}

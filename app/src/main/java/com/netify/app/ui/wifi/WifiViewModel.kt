package com.netify.app.ui.wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.netify.app.di.AppContainer
import com.netify.app.domain.ChannelAnalyzer
import com.netify.app.domain.model.ChannelCongestion
import com.netify.app.domain.model.DiscoveredDevice
import com.netify.app.domain.model.WifiInfoSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WifiUiState(
    val info: WifiInfoSnapshot? = null,
    val channels: List<ChannelCongestion> = emptyList(),
    val channelAdvice: String = "",
    val devices: List<DiscoveredDevice> = emptyList(),
    val isScanningDevices: Boolean = false,
    val hasLocationPermission: Boolean = false
)

class WifiViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(WifiUiState())
    val state: StateFlow<WifiUiState> = _state.asStateFlow()

    /** Call once ACCESS_FINE_LOCATION has been granted (required to read SSID/scan results on API 27+). */
    fun onLocationPermissionResult(granted: Boolean) {
        _state.value = _state.value.copy(hasLocationPermission = granted)
        if (granted) refresh()
    }

    fun refresh() {
        val info = container.wifiRepository.getCurrentWifiInfo()
        _state.value = _state.value.copy(info = info)

        viewModelScope.launch {
            container.wifiRepository.requestScan()
            val channels = container.wifiRepository.getChannelCongestion()
            _state.value = _state.value.copy(
                channels = channels,
                channelAdvice = ChannelAnalyzer.recommend(channels)
            )
        }
    }

    fun scanDevices() {
        val ip = _state.value.info?.deviceIp ?: return
        if (ip == "--") return
        _state.value = _state.value.copy(isScanningDevices = true)
        viewModelScope.launch {
            val found = container.deviceScanner.scanSubnet(ip)
            _state.value = _state.value.copy(devices = found, isScanningDevices = false)
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = WifiViewModel(container) as T
        }
    }
}

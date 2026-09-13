package com.netify.app.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.netify.app.di.AppContainer
import com.netify.app.domain.model.VaultEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VaultViewModel(private val container: AppContainer) : ViewModel() {
    private val _entries = MutableStateFlow(container.vaultStorage.getAll())
    val entries: StateFlow<List<VaultEntry>> = _entries.asStateFlow()

    fun addEntry(ssid: String, password: String, security: String = "WPA") {
        container.vaultStorage.add(ssid, password, security)
        refresh()
    }

    fun deleteEntry(id: String) {
        container.vaultStorage.delete(id)
        refresh()
    }

    fun refresh() {
        _entries.value = container.vaultStorage.getAll()
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = VaultViewModel(container) as T
        }
    }
}

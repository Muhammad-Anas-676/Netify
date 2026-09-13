package com.netify.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.netify.app.data.local.SpeedTestEntity
import com.netify.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(private val container: AppContainer) : ViewModel() {
    private val _entries = MutableStateFlow<List<SpeedTestEntity>>(emptyList())
    val entries: StateFlow<List<SpeedTestEntity>> = _entries.asStateFlow()

    init {
        viewModelScope.launch {
            container.database.speedTestDao().observeAll().collect { _entries.value = it }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { container.database.speedTestDao().clearAll() }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HistoryViewModel(container) as T
        }
    }
}

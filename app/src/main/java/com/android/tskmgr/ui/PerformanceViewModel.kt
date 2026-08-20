package com.android.tskmgr.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.tskmgr.data.SystemMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PerformanceUiState(
    val cpuPercent: Double? = null,
    val cpuHistory: List<Double> = emptyList(),
    val loadAvg: Double? = null,
    val cores: List<Double> = emptyList(),
    val memory: SystemMetrics.MemoryInfo? = null,
    val storage: SystemMetrics.StorageInfo? = null,
    val rxHistory: List<Long> = emptyList(),
    val txHistory: List<Long> = emptyList(),
    val networkAvailable: Boolean = true,
)

/** Drives the Performance tab, sampling every second on a background thread. */
class PerformanceViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(PerformanceUiState())
    val state: StateFlow<PerformanceUiState> = _state.asStateFlow()

    private val historyCap = 60 // ~1 minute of 1s samples
    private var lastRx = 0L
    private var lastTx = 0L

    init {
        viewModelScope.launch(Dispatchers.Default) {
            var first = true
            while (isActive) {
                try {
                    val cpu = SystemMetrics.readCpuUsage()
                    val memory = SystemMetrics.readMemory(getApplication())
                    val storage = SystemMetrics.readStorage()
                    val net = SystemMetrics.readNetwork(getApplication(), lastRx, lastTx)
                    lastRx += net.rxBytesPerSec
                    lastTx += net.txBytesPerSec

                    val prev = _state.value
                    _state.value = prev.copy(
                        cpuPercent = cpu?.totalPercent,
                        cpuHistory = append(prev.cpuHistory, cpu?.totalPercent ?: 0.0),
                        loadAvg = cpu?.loadAvg,
                        cores = cpu?.perCore?.drop(1) ?: emptyList(),
                        memory = memory,
                        storage = storage,
                        rxHistory = if (first) prev.rxHistory else append(prev.rxHistory, net.rxBytesPerSec),
                        txHistory = if (first) prev.txHistory else append(prev.txHistory, net.txBytesPerSec),
                        networkAvailable = net.available,
                    )
                    first = false
                } catch (e: Exception) {
                    // Never let a sampling failure crash the app.
                }
                delay(1000)
            }
        }
    }

    private fun <T> append(list: List<T>, value: T): List<T> =
        (list + value).takeLast(historyCap)
}

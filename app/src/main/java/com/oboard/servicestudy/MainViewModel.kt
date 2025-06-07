package com.oboard.servicestudy

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter.asStateFlow()

    private val counterReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == CounterService.ACTION_UPDATE_COUNTER) {
                val counterValue = intent.getIntExtra(CounterService.EXTRA_COUNTER, 0)
                _counter.value = counterValue
            }
        }
    }

    init {
        // Register the broadcast receiver
        val filter = IntentFilter(CounterService.ACTION_UPDATE_COUNTER)
        getApplication<Application>().registerReceiver(counterReceiver, filter)
    }

    fun startService() {
        val intent = Intent(getApplication(), CounterService::class.java).apply {
            action = CounterService.ACTION_START
        }
        getApplication<Application>().startService(intent)
        _isServiceRunning.value = true
    }

    fun stopService() {
        val intent = Intent(getApplication(), CounterService::class.java).apply {
            action = CounterService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
        _isServiceRunning.value = false
    }

    override fun onCleared() {
        super.onCleared()
        // Unregister the broadcast receiver when the ViewModel is cleared
        getApplication<Application>().unregisterReceiver(counterReceiver)
    }
} 
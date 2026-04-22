package com.dysphagiaguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dysphagiaguard.network.ConnectionState
import com.dysphagiaguard.network.WebSocketClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class ConnectionViewModel : ViewModel() {

    private val _connectionAttempt = MutableStateFlow(0)
    val connectionAttempt: StateFlow<Int> = _connectionAttempt.asStateFlow()

    private val _isDeviceFound = MutableStateFlow(false)
    val isDeviceFound: StateFlow<Boolean> = _isDeviceFound.asStateFlow()

    fun pingDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://192.168.4.1/status")
                .build()

            while (_connectionAttempt.value < 10 && !_isDeviceFound.value) {
                _connectionAttempt.value++
                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        _isDeviceFound.value = true
                        break
                    }
                } catch (e: Exception) {
                    // Timeout or unreachable
                }
                delay(2000)
            }
        }
    }
    
    fun resetAttempts() {
        _connectionAttempt.value = 0
        _isDeviceFound.value = false
        pingDevice()
    }
}

package com.dysphagiaguard.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, LOST
}

data class SwallowEventData(
    val type: String = "swallow_event",
    val classification: String = "IDLE",
    val imuRms: Float = 0f,
    val micEnvelope: Float = 0f,
    val confidence: Float = 0f,
    val timestamp: Long = 0L,
    val durationMs: Int = 0
)

class WebSocketClient {
    private val client = OkHttpClient.Builder()
        .pingInterval(5, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val wsUrl = "ws://192.168.4.1/ws"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _latestEvent = MutableStateFlow(SwallowEventData())
    val latestEvent: StateFlow<SwallowEventData> = _latestEvent.asStateFlow()

    private var reconnectAttempt = 0

    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED || _connectionState.value == ConnectionState.CONNECTING) return
        
        _connectionState.value = ConnectionState.CONNECTING
        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, createWebSocketListener())
    }

    fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        reconnectAttempt = 0
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connectionState.value = ConnectionState.CONNECTED
            reconnectAttempt = 0
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val json = JSONObject(text)
                if (json.optString("type") == "swallow_event") {
                    val event = SwallowEventData(
                        type = json.optString("type"),
                        classification = json.optString("classification", "IDLE"),
                        imuRms = json.optDouble("imu_rms", 0.0).toFloat(),
                        micEnvelope = json.optDouble("mic_envelope", 0.0).toFloat(),
                        confidence = json.optDouble("confidence", 0.0).toFloat(),
                        timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                        durationMs = json.optInt("duration_ms", 0)
                    )
                    _latestEvent.value = event
                }
            } catch (e: Exception) {
                Log.e("WebSocketClient", "Error parsing JSON", e)
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _connectionState.value = ConnectionState.LOST
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (_connectionState.value == ConnectionState.DISCONNECTED) return // manual disconnect
        
        scope.launch {
            val delaySeconds = min(2.0.pow(reconnectAttempt).toLong(), 8L)
            delay(delaySeconds * 1000)
            reconnectAttempt++
            connect()
        }
    }
}

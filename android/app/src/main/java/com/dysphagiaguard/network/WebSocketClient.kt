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
    val durationMs: Int = 0,
    val totalSafe: Int = 0,
    val totalUnsafe: Int = 0,
    val sessionId: Int = 1,
    val deviceMode: String = "DAY",
    val systemHealthy: Boolean = true
)

class WebSocketClient {
    private val client = OkHttpClient.Builder()
        .pingInterval(5, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // no timeout for WebSocket reads
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
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING) return

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

    fun send(message: String) {
        webSocket?.send(message)
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WS", "Connected to ESP32")
            _connectionState.value = ConnectionState.CONNECTED
            reconnectAttempt = 0
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            // Skip PONG responses
            if (text == "PONG") return
            try {
                val json = JSONObject(text)
                // ✅ FIX: Accept ANY JSON that has a "classification" field.
                // The old code checked type == "swallow_event" which the ESP32 NEVER sends.
                // Every ESP32 broadcast goes to /dev/null because of that one wrong check.
                val classification = json.optString("classification", "IDLE")
                if (classification.isEmpty()) return

                val event = SwallowEventData(
                    type = "swallow_event",
                    classification = classification,
                    imuRms = json.optDouble("imu_rms", 0.0).toFloat(),
                    micEnvelope = json.optDouble("mic_envelope", 0.0).toFloat(),
                    confidence = json.optDouble("confidence", 0.0).toFloat(),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    durationMs = json.optInt("duration_ms", 0),
                    totalSafe = json.optInt("total_safe", 0),
                    totalUnsafe = json.optInt("total_unsafe", 0),
                    sessionId = json.optInt("session_id", 1),
                    deviceMode = json.optString("device_mode", "DAY"),
                    systemHealthy = json.optBoolean("system_healthy", true)
                )
                _latestEvent.value = event
                Log.d("WS", "Event: $classification imu=${event.imuRms} mic=${event.micEnvelope}")
            } catch (e: Exception) {
                Log.e("WS", "JSON parse error: ${e.message} | raw: $text")
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WS", "Closed: $reason")
            _connectionState.value = ConnectionState.DISCONNECTED
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e("WS", "Failure: ${t.message}")
            _connectionState.value = ConnectionState.LOST
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (_connectionState.value == ConnectionState.DISCONNECTED) return
        scope.launch {
            val delayMs = min(2.0.pow(reconnectAttempt).toLong(), 8L) * 1000
            delay(delayMs)
            reconnectAttempt++
            connect()
        }
    }
}

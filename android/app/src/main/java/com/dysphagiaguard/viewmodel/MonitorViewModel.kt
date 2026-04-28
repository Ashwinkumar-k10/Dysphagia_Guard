package com.dysphagiaguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dysphagiaguard.data.model.SessionData
import com.dysphagiaguard.data.model.SwallowEvent
import com.dysphagiaguard.data.repository.DemoDataSource
import com.dysphagiaguard.data.repository.SessionRepository
import com.dysphagiaguard.network.SwallowEventData
import com.dysphagiaguard.network.WebSocketClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class MonitorViewModel(
    private val repository: SessionRepository,
    private val webSocketClient: WebSocketClient,
    private val demoDataSource: DemoDataSource = DemoDataSource()
) : ViewModel() {

    private val _currentSessionId = MutableStateFlow(-1)
    val currentSessionId: StateFlow<Int> = _currentSessionId.asStateFlow()

    private val _eventStream = MutableStateFlow(SwallowEventData())
    val eventStream: StateFlow<SwallowEventData> = _eventStream.asStateFlow()

    private val _totalSwallows = MutableStateFlow(0)
    val totalSwallows: StateFlow<Int> = _totalSwallows.asStateFlow()

    private val _unsafeSwallows = MutableStateFlow(0)
    val unsafeSwallows: StateFlow<Int> = _unsafeSwallows.asStateFlow()

    private val _coughCount = MutableStateFlow(0)
    val coughCount: StateFlow<Int> = _coughCount.asStateFlow()

    private val _sessionStartTime = MutableStateFlow(System.currentTimeMillis())
    val sessionStartTime: StateFlow<Long> = _sessionStartTime.asStateFlow()

    private val _emergencyAlertTriggered = MutableSharedFlow<String>()
    val emergencyAlertTriggered = _emergencyAlertTriggered.asSharedFlow()

    private val _lastSessionData = MutableStateFlow<SessionData?>(null)
    val lastSessionData: StateFlow<SessionData?> = _lastSessionData.asStateFlow()

    private val _sessionEvents = MutableStateFlow<List<SwallowEvent>>(emptyList())
    val sessionEvents: StateFlow<List<SwallowEvent>> = _sessionEvents.asStateFlow()

    private var isDemoMode = false
    private var patientId = -1
    private var consecutiveUnsafeCount = 0
    private var caregiverPhone = ""
    private var eventCollectionJob: Job? = null

    // ─── Event Collection ────────────────────────────────────────────────────
    private fun startEventCollection(demoMode: Boolean) {
        eventCollectionJob?.cancel()
        val source = if (demoMode) {
            demoDataSource.start()
            demoDataSource.eventStream
        } else {
            webSocketClient.connect()
            webSocketClient.latestEvent
        }

        eventCollectionJob = viewModelScope.launch {
            source.collect { event ->
                _eventStream.value = event
                when (event.classification) {
                    "SAFE" -> {
                        _totalSwallows.value++
                        consecutiveUnsafeCount = 0
                        saveEvent(event)
                    }
                    "UNSAFE" -> {
                        _totalSwallows.value++
                        _unsafeSwallows.value++
                        consecutiveUnsafeCount++
                        if (consecutiveUnsafeCount >= 3 && caregiverPhone.isNotEmpty()) {
                            _emergencyAlertTriggered.emit(caregiverPhone)
                            consecutiveUnsafeCount = 0
                        }
                        saveEvent(event)
                    }
                    "COUGH" -> {
                        _coughCount.value++
                        consecutiveUnsafeCount = 0
                        saveEvent(event)
                    }
                    "NOISE" -> saveEvent(event)
                    // IDLE = heartbeat, update UI only
                }
            }
        }
    }

    private fun saveEvent(event: SwallowEventData) {
        if (_currentSessionId.value == -1) return
        val e = SwallowEvent(
            sessionId = _currentSessionId.value,
            timestamp = event.timestamp,
            classification = event.classification,
            imuRms = event.imuRms,
            micEnvelope = event.micEnvelope,
            confidence = event.confidence,
            durationMs = event.durationMs
        )
        viewModelScope.launch(Dispatchers.IO) { repository.saveEvent(e) }
        _sessionEvents.value = _sessionEvents.value + e
    }

    // ─── Manual Trigger (demo buttons + real WS commands) ────────────────────
    /**
     * Inject a manual event directly into the stream.
     * Works in BOTH demo mode (no hardware) and live mode (also sends to ESP32).
     * This is what powers the manual SAFE/UNSAFE/COUGH buttons on the screen.
     */
    fun triggerManual(classification: String) {
        // If live mode, send command to ESP32 over WebSocket
        if (!isDemoMode) webSocketClient.send(classification)

        // Immediately inject into local event stream for instant UI feedback
        val (imu, mic, conf, dur) = when (classification) {
            "SAFE"   -> EventQuad(
                0.38f + Random.nextFloat() * 0.18f,
                0.52f + Random.nextFloat() * 0.15f,
                0.78f + Random.nextFloat() * 0.15f,
                Random.nextInt(120, 190)
            )
            "UNSAFE" -> EventQuad(
                0.75f + Random.nextFloat() * 0.18f,
                0.88f + Random.nextFloat() * 0.12f,
                0.72f + Random.nextFloat() * 0.18f,
                Random.nextInt(110, 200)
            )
            "COUGH"  -> EventQuad(
                0.68f + Random.nextFloat() * 0.14f,
                0.80f + Random.nextFloat() * 0.12f,
                0.74f + Random.nextFloat() * 0.14f,
                Random.nextInt(45, 78)
            )
            "NOISE"  -> EventQuad(
                0.12f + Random.nextFloat() * 0.15f,
                0.10f + Random.nextFloat() * 0.12f,
                0.22f + Random.nextFloat() * 0.20f,
                Random.nextInt(20, 55)
            )
            else -> EventQuad(0.02f, 0.01f, 0f, 0)
        }

        val event = SwallowEventData(
            classification = classification,
            imuRms = imu, micEnvelope = mic,
            confidence = conf, durationMs = dur,
            timestamp = System.currentTimeMillis(),
            totalSafe = _totalSwallows.value - _unsafeSwallows.value,
            totalUnsafe = _unsafeSwallows.value
        )

        // Fire 3 spike frames then the real event
        viewModelScope.launch {
            repeat(3) {
                _eventStream.value = event.copy(imuRms = imu * (0.7f + Random.nextFloat() * 0.5f))
                delay(60)
            }
            _eventStream.value = event
        }
    }

    // ─── Session lifecycle ────────────────────────────────────────────────────
    fun startSession(patientId: Int, isDemoMode: Boolean = false) {
        this.patientId = patientId
        this.isDemoMode = isDemoMode
        consecutiveUnsafeCount = 0
        _sessionEvents.value = emptyList()
        _totalSwallows.value = 0
        _unsafeSwallows.value = 0
        _coughCount.value = 0
        viewModelScope.launch(Dispatchers.IO) {
            repository.patientProfile.firstOrNull()?.let { caregiverPhone = it.phone }
        }
        _sessionStartTime.value = System.currentTimeMillis()
        startEventCollection(isDemoMode)
        viewModelScope.launch(Dispatchers.IO) {
            val sid = repository.createSession(
                SessionData(patientId = patientId, startTime = _sessionStartTime.value,
                    endTime = 0, totalSwallows = 0, unsafeCount = 0)
            )
            _currentSessionId.value = sid.toInt()
        }
    }

    fun endSession() {
        eventCollectionJob?.cancel()
        webSocketClient.disconnect()
        demoDataSource.stop()
        val data = SessionData(
            id = _currentSessionId.value, patientId = patientId,
            startTime = _sessionStartTime.value, endTime = System.currentTimeMillis(),
            totalSwallows = _totalSwallows.value, unsafeCount = _unsafeSwallows.value
        )
        _lastSessionData.value = data
        if (_sessionEvents.value.isEmpty() && isDemoMode)
            _sessionEvents.value = generateDemoReport(_currentSessionId.value)
        viewModelScope.launch(Dispatchers.IO) {
            if (_currentSessionId.value != -1) repository.updateSession(data)
        }
    }

    private fun generateDemoReport(sessionId: Int): List<SwallowEvent> {
        val now = System.currentTimeMillis()
        return listOf("SAFE","SAFE","COUGH","UNSAFE","SAFE","COUGH","SAFE","UNSAFE","SAFE","SAFE")
            .mapIndexed { i, c ->
                SwallowEvent(id = i+1, sessionId = sessionId,
                    timestamp = now - (10-i) * 60_000L, classification = c,
                    imuRms = when(c){"UNSAFE"->0.82f;"COUGH"->0.70f;else->0.48f},
                    micEnvelope = when(c){"UNSAFE"->0.91f;"COUGH"->0.78f;else->0.61f},
                    confidence = when(c){"UNSAFE"->0.78f;"COUGH"->0.76f;else->0.91f},
                    durationMs = when(c){"UNSAFE"->175;"COUGH"->62;else->138})
            }
    }

    private data class EventQuad(val imu: Float, val mic: Float, val conf: Float, val dur: Int)
}

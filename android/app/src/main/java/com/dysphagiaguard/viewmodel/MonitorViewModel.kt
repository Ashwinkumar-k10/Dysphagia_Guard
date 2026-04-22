package com.dysphagiaguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dysphagiaguard.data.model.SessionData
import com.dysphagiaguard.data.model.SwallowEvent
import com.dysphagiaguard.data.repository.DemoDataSource
import com.dysphagiaguard.data.repository.SessionRepository
import com.dysphagiaguard.network.WebSocketClient
import com.dysphagiaguard.network.SwallowEventData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    val totalSwallows = _totalSwallows.asStateFlow()

    private val _unsafeSwallows = MutableStateFlow(0)
    val unsafeSwallows = _unsafeSwallows.asStateFlow()

    private val _sessionStartTime = MutableStateFlow(System.currentTimeMillis())
    val sessionStartTime = _sessionStartTime.asStateFlow()

    private val _emergencyAlertTriggered = MutableSharedFlow<String>()
    val emergencyAlertTriggered = _emergencyAlertTriggered.asSharedFlow()

    // Exposed after session ends — used by SessionCompleteScreen and DailyReportScreen
    private val _lastSessionData = MutableStateFlow<SessionData?>(null)
    val lastSessionData: StateFlow<SessionData?> = _lastSessionData.asStateFlow()

    private val _sessionEvents = MutableStateFlow<List<SwallowEvent>>(emptyList())
    val sessionEvents: StateFlow<List<SwallowEvent>> = _sessionEvents.asStateFlow()

    private var isDemoMode = false
    private var patientId = -1
    private var consecutiveUnsafeCount = 0
    private var caregiverPhone = ""
    private var eventCollectionJob: Job? = null

    private fun startEventCollection(isDemoMode: Boolean) {
        eventCollectionJob?.cancel()
        val sourceFlow = if (isDemoMode) {
            demoDataSource.start()
            demoDataSource.eventStream
        } else {
            webSocketClient.connect()
            webSocketClient.latestEvent
        }

        eventCollectionJob = viewModelScope.launch {
            sourceFlow.collect { event ->
                _eventStream.value = event
                if (event.classification == "SAFE" || event.classification == "UNSAFE") {
                    _totalSwallows.value++
                    if (event.classification == "UNSAFE") {
                        _unsafeSwallows.value++
                        consecutiveUnsafeCount++
                        if (consecutiveUnsafeCount >= 3) {
                            if (caregiverPhone.isNotEmpty()) {
                                _emergencyAlertTriggered.emit(caregiverPhone)
                            }
                            consecutiveUnsafeCount = 0
                        }
                    } else if (event.classification == "SAFE") {
                        consecutiveUnsafeCount = 0
                    }

                    if (_currentSessionId.value != -1) {
                        val swallowEvent = SwallowEvent(
                            sessionId = _currentSessionId.value,
                            timestamp = event.timestamp,
                            classification = event.classification,
                            imuRms = event.imuRms,
                            micEnvelope = event.micEnvelope,
                            confidence = event.confidence,
                            durationMs = event.durationMs
                        )
                        repository.saveEvent(swallowEvent)
                        // Keep in-memory list updated for immediate UI access
                        _sessionEvents.value = _sessionEvents.value + swallowEvent
                    }
                }
            }
        }
    }

    fun startSession(patientId: Int, isDemoMode: Boolean = false) {
        this.patientId = patientId
        this.isDemoMode = isDemoMode
        viewModelScope.launch(Dispatchers.IO) {
            repository.patientProfile.firstOrNull()?.let { profile ->
                caregiverPhone = profile.phone
            }
        }
        consecutiveUnsafeCount = 0
        _sessionEvents.value = emptyList()
        _totalSwallows.value = 0
        _unsafeSwallows.value = 0
        startEventCollection(isDemoMode)
        _sessionStartTime.value = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val sid = repository.createSession(
                SessionData(
                    patientId = patientId,
                    startTime = _sessionStartTime.value,
                    endTime = 0,
                    totalSwallows = 0,
                    unsafeCount = 0
                )
            )
            _currentSessionId.value = sid.toInt()
        }
    }

    fun endSession() {
        eventCollectionJob?.cancel()
        webSocketClient.disconnect()
        demoDataSource.stop()

        val endedSessionData = SessionData(
            id = _currentSessionId.value,
            patientId = patientId,
            startTime = _sessionStartTime.value,
            endTime = System.currentTimeMillis(),
            totalSwallows = _totalSwallows.value,
            unsafeCount = _unsafeSwallows.value
        )
        _lastSessionData.value = endedSessionData

        // If no events recorded (e.g. demo just started), generate demo events for report
        if (_sessionEvents.value.isEmpty() && isDemoMode) {
            _sessionEvents.value = generateDemoReport(_currentSessionId.value)
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (_currentSessionId.value != -1) {
                repository.updateSession(endedSessionData)
            }
        }
    }

    /** Generates realistic demo swallow event list for report display */
    private fun generateDemoReport(sessionId: Int): List<SwallowEvent> {
        val now = System.currentTimeMillis()
        val events = mutableListOf<SwallowEvent>()
        val classifications = listOf("SAFE", "SAFE", "SAFE", "UNSAFE", "SAFE", "SAFE", "UNSAFE", "SAFE", "SAFE", "SAFE")
        classifications.forEachIndexed { index, classification ->
            events.add(
                SwallowEvent(
                    id = index + 1,
                    sessionId = sessionId,
                    timestamp = now - (classifications.size - index) * 60_000L,
                    classification = classification,
                    imuRms = if (classification == "UNSAFE") 0.82f else 0.48f,
                    micEnvelope = if (classification == "UNSAFE") 0.91f else 0.63f,
                    confidence = if (classification == "UNSAFE") 0.78f else 0.92f,
                    durationMs = if (classification == "UNSAFE") 180 else 130,
                    acknowledged = false
                )
            )
        }
        return events
    }
}

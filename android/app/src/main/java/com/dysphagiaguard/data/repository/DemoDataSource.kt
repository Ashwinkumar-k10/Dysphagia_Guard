package com.dysphagiaguard.data.repository

import com.dysphagiaguard.network.SwallowEventData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class DemoDataSource {
    private val _eventStream = MutableStateFlow(SwallowEventData(type = "heartbeat", classification = "IDLE"))
    val eventStream: StateFlow<SwallowEventData> = _eventStream.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            while (isActive && isRunning) {
                // Generate 20Hz (50ms) realistic noise/baseline
                val baselineImu = Random.nextFloat() * 0.1f
                val baselineMic = Random.nextFloat() * 0.05f
                
                _eventStream.value = SwallowEventData(
                    type = "heartbeat",
                    classification = "IDLE",
                    imuRms = baselineImu,
                    micEnvelope = baselineMic,
                    timestamp = System.currentTimeMillis()
                )

                // Randomly trigger a swallow event every 5 to 10 seconds
                if (Random.nextInt(100) < 2) { 
                    val isUnsafe = Random.nextInt(100) < 30 // 30% chance of unsafe
                    
                    val durationMs = Random.nextInt(100, 200)
                    val steps = durationMs / 50
                    
                    // Ramp up
                    for (i in 0..steps) {
                        if (!isRunning) break
                        val imu = (i.toFloat() / steps) * (if (isUnsafe) 0.8f else 0.5f) + baselineImu
                        val mic = (i.toFloat() / steps) * (if (isUnsafe) 0.9f else 0.6f) + baselineMic
                        
                        _eventStream.value = SwallowEventData(
                            type = "waveform",
                            classification = "IDLE",
                            imuRms = imu,
                            micEnvelope = mic,
                            timestamp = System.currentTimeMillis()
                        )
                        delay(50)
                    }

                    // Emit classification event
                    if (isRunning) {
                        _eventStream.value = SwallowEventData(
                            type = "swallow_event",
                            classification = if (isUnsafe) "UNSAFE" else "SAFE",
                            imuRms = if (isUnsafe) 0.85f else 0.55f,
                            micEnvelope = if (isUnsafe) 0.95f else 0.65f,
                            confidence = Random.nextFloat() * 0.2f + 0.7f, // 0.7 - 0.9
                            timestamp = System.currentTimeMillis(),
                            durationMs = durationMs
                        )
                        delay(500) // Hold the event a bit
                    }
                }
                
                delay(50)
            }
        }
    }

    fun stop() {
        isRunning = false
    }
}

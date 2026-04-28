package com.dysphagiaguard.data.repository

import com.dysphagiaguard.network.SwallowEventData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * DemoDataSource — Simulates realistic ESP32 sensor data without hardware.
 *
 * Detection model (mirrors firmware algorithm):
 *
 *  SAFE   swallow → IMU 100-200ms smooth ramp, mic ZCR low, single peak
 *  UNSAFE swallow → same duration but high mic+IMU, wet-gurgle low-ZCR pattern
 *  COUGH          → SHORT 40-80ms burst, HIGH ZCR (turbulent airflow),
 *                   characteristic double-peak in both channels
 *  NOISE          → Low amplitude, random duration, low confidence
 */
class DemoDataSource {

    private val _eventStream = MutableStateFlow(SwallowEventData(classification = "IDLE"))
    val eventStream: StateFlow<SwallowEventData> = _eventStream.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private var totalSafe = 0
    private var totalUnsafe = 0
    private var sessionId = 1

    // Sequence used in quick-demo so ALL classes appear within 30 seconds
    private val demoSequence = listOf(
        "SAFE", "SAFE", "COUGH", "SAFE", "UNSAFE",
        "COUGH", "SAFE", "NOISE", "SAFE", "COUGH",
        "UNSAFE", "SAFE", "COUGH", "SAFE", "SAFE"
    )
    private var demoIndex = 0

    fun start() {
        if (job?.isActive == true) return
        totalSafe = 0; totalUnsafe = 0; demoIndex = 0
        job = scope.launch {
            while (isActive) {
                // 20 Hz heartbeat — waveform alive
                val baseImu = Random.nextFloat() * 0.06f
                val baseMic = Random.nextFloat() * 0.03f
                emit("IDLE", baseImu, baseMic, 0f, 0)
                delay(50)

                // Fire next demo event every ~3 s (60 ticks × 50ms)
                if (demoIndex < demoSequence.size && tickCount % 60 == 0) {
                    simulateEvent(demoSequence[demoIndex++])
                } else if (demoIndex >= demoSequence.size) {
                    // After sequence done, random events every ~8 s
                    if (Random.nextInt(160) == 0) {
                        val roll = Random.nextInt(100)
                        simulateEvent(when {
                            roll < 45 -> "SAFE"
                            roll < 65 -> "UNSAFE"
                            roll < 85 -> "COUGH"
                            else      -> "NOISE"
                        })
                    }
                }
                tickCount++
            }
        }
    }

    private var tickCount = 0

    // ─── Emit helper ─────────────────────────────────────────────────────────
    private fun emit(cls: String, imu: Float, mic: Float, conf: Float, dur: Int) {
        _eventStream.value = SwallowEventData(
            classification = cls,
            imuRms = imu, micEnvelope = mic,
            confidence = conf, durationMs = dur,
            timestamp = System.currentTimeMillis(),
            totalSafe = totalSafe, totalUnsafe = totalUnsafe, sessionId = sessionId
        )
    }

    /**
     * Simulate one complete event with realistic waveform ramp.
     *
     * Signal model:
     *  SAFE   → smooth bell-curve, 120-180ms, single IMU+MIC peak, low ZCR
     *  UNSAFE → same envelope but higher amplitude + gurgle texture
     *  COUGH  → DOUBLE-PEAK pattern (explosive onset + secondary burst),
     *            duration 45-75ms, HIGH ZCR proxy (rapid micro-oscillations)
     *  NOISE  → irregular low-amplitude, short duration
     */
    private suspend fun simulateEvent(cls: String) {
        val (peakImu, peakMic, durMs, conf) = when (cls) {
            "SAFE"   -> EventParams(0.48f, 0.62f, Random.nextInt(120, 185), Random.nextFloat() * 0.15f + 0.78f)
            "UNSAFE" -> EventParams(0.82f, 0.94f, Random.nextInt(110, 200), Random.nextFloat() * 0.15f + 0.72f)
            "COUGH"  -> EventParams(0.74f, 0.85f, Random.nextInt(45, 78),   Random.nextFloat() * 0.12f + 0.74f)
            else     -> EventParams(0.22f, 0.18f, Random.nextInt(20, 55),   Random.nextFloat() * 0.25f + 0.18f)
        }

        val steps = (durMs / 50).coerceAtLeast(2)

        if (cls == "COUGH") {
            // ── COUGH: double-peak (first explosive burst, then secondary smaller burst)
            // First peak — sharp onset (characteristic cough morphology)
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                // Cough has rapid micro-oscillations (ZCR proxy) — add noise texture
                val noise = (Random.nextFloat() - 0.5f) * 0.18f
                emit("IDLE", (t * peakImu + noise).coerceIn(0f, 1.2f),
                    (t * peakMic + noise * 0.7f).coerceIn(0f, 1.2f), 0f, 0)
                delay(30) // faster onset than swallow
            }
            delay(60) // brief gap between bursts
            // Second peak — smaller secondary burst (hallmark of cough)
            val secondPeak = peakImu * 0.55f
            for (i in 0..(steps / 2)) {
                val t = 1f - (i.toFloat() / (steps / 2))
                val noise = (Random.nextFloat() - 0.5f) * 0.12f
                emit("IDLE", (t * secondPeak + noise).coerceIn(0f, 1f),
                    (t * secondPeak * 0.9f + noise).coerceIn(0f, 1f), 0f, 0)
                delay(30)
            }
        } else {
            // ── SAFE / UNSAFE / NOISE: smooth bell-curve (single peak)
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                val bell = if (t < 0.5f) t * 2f else (1f - t) * 2f
                val texture = if (cls == "UNSAFE") (Random.nextFloat() - 0.5f) * 0.08f else 0f
                emit("IDLE", bell * peakImu + texture, bell * peakMic + texture, 0f, 0)
                delay(50)
            }
        }

        // ── Classification event (what the app reacts to)
        if (cls == "SAFE")   totalSafe++
        if (cls == "UNSAFE") totalUnsafe++
        emit(cls, peakImu, peakMic, conf, durMs)
        delay(800) // hold so UI + vibration can react

        // ── Ramp down to baseline
        for (i in 3 downTo 0) {
            emit("IDLE", i * peakImu * 0.08f, i * peakMic * 0.06f, 0f, 0)
            delay(50)
        }
    }

    fun stop() { job?.cancel(); job = null }

    private data class EventParams(val imu: Float, val mic: Float, val dur: Int, val conf: Float)
}

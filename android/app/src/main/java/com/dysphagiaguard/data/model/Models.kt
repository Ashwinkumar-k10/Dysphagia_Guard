package com.dysphagiaguard.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patient_profile")
data class PatientProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val age: String,
    val condition: String,
    val caregiverName: String,
    val phone: String,
    val silentNightMode: Boolean,
    val autoPdf: Boolean
)

@Entity(tableName = "session")
data class SessionData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val startTime: Long,
    val endTime: Long,
    val totalSwallows: Int,
    val unsafeCount: Int
)

@Entity(tableName = "swallow_event")
data class SwallowEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val timestamp: Long,
    val classification: String,
    val imuRms: Float,
    val micEnvelope: Float,
    val confidence: Float,
    val durationMs: Int,
    val acknowledged: Boolean = false
)

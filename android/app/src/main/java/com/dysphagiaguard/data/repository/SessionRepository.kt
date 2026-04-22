package com.dysphagiaguard.data.repository

import com.dysphagiaguard.data.local.PatientDao
import com.dysphagiaguard.data.local.SessionDao
import com.dysphagiaguard.data.local.SwallowEventDao
import com.dysphagiaguard.data.model.PatientProfile
import com.dysphagiaguard.data.model.SessionData
import com.dysphagiaguard.data.model.SwallowEvent
import kotlinx.coroutines.flow.Flow

class SessionRepository(
    private val patientDao: PatientDao,
    private val sessionDao: SessionDao,
    private val eventDao: SwallowEventDao
) {
    val patientProfile: Flow<PatientProfile?> = patientDao.getProfile()
    
    suspend fun saveProfile(profile: PatientProfile): Long {
        return patientDao.insertProfile(profile)
    }

    suspend fun createSession(session: SessionData): Long {
        return sessionDao.insertSession(session)
    }

    suspend fun updateSession(session: SessionData) {
        sessionDao.updateSession(session)
    }

    suspend fun getLastSession(): SessionData? {
        return sessionDao.getLastSession()
    }

    fun getSessionEvents(sessionId: Int): Flow<List<SwallowEvent>> {
        return eventDao.getEventsForSession(sessionId)
    }

    fun getAllEvents(): Flow<List<SwallowEvent>> {
        return eventDao.getAllEvents()
    }

    suspend fun saveEvent(event: SwallowEvent) {
        eventDao.insertEvent(event)
    }
    
    suspend fun acknowledgeEvent(event: SwallowEvent) {
        eventDao.updateEvent(event.copy(acknowledged = true))
    }
}

package com.dysphagiaguard.data.local

import android.content.Context
import androidx.room.*
import com.dysphagiaguard.data.model.PatientProfile
import com.dysphagiaguard.data.model.SessionData
import com.dysphagiaguard.data.model.SwallowEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patient_profile LIMIT 1")
    fun getProfile(): Flow<PatientProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: PatientProfile): Long
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM session ORDER BY id DESC")
    fun getAllSessions(): Flow<List<SessionData>>
    
    @Query("SELECT * FROM session ORDER BY id DESC LIMIT 1")
    suspend fun getLastSession(): SessionData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionData): Long

    @Update
    suspend fun updateSession(session: SessionData)
}

@Dao
interface SwallowEventDao {
    @Query("SELECT * FROM swallow_event WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getEventsForSession(sessionId: Int): Flow<List<SwallowEvent>>

    @Query("SELECT * FROM swallow_event ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<SwallowEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SwallowEvent)
    
    @Update
    suspend fun updateEvent(event: SwallowEvent)
}

@Database(entities = [PatientProfile::class, SessionData::class, SwallowEvent::class], version = 1, exportSchema = false)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun sessionDao(): SessionDao
    abstract fun swallowEventDao(): SwallowEventDao

    companion object {
        @Volatile
        private var INSTANCE: SessionDatabase? = null

        fun getDatabase(context: Context): SessionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SessionDatabase::class.java,
                    "session_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

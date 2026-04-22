package com.dysphagiaguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dysphagiaguard.data.model.SessionData
import com.dysphagiaguard.data.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportViewModel(private val repository: SessionRepository) : ViewModel() {
    // Currently acting as a stub, since it would load report data for the UI
    private val _sessionData = MutableStateFlow<SessionData?>(null)
    val sessionData: StateFlow<SessionData?> = _sessionData.asStateFlow()

    fun loadSession(sessionId: Int) {
        // Here we could load specific session data if needed
    }
}

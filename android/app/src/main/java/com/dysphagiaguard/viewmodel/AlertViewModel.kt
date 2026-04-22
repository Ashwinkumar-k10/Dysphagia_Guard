package com.dysphagiaguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dysphagiaguard.data.model.SwallowEvent
import com.dysphagiaguard.data.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlertViewModel(private val repository: SessionRepository) : ViewModel() {
    
    private val _events = MutableStateFlow<List<SwallowEvent>>(emptyList())
    val events: StateFlow<List<SwallowEvent>> = _events.asStateFlow()

    init {
        loadAllEvents()
    }

    private fun loadAllEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAllEvents().collect { list ->
                _events.value = list
            }
        }
    }

    fun loadEvents(sessionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getSessionEvents(sessionId).collect { list ->
                _events.value = list
            }
        }
    }

    fun acknowledgeEvent(event: SwallowEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.acknowledgeEvent(event)
        }
    }
}

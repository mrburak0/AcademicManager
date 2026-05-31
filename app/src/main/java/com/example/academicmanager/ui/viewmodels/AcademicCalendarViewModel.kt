package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.AcademicEvent
import com.example.academicmanager.data.UniversityRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AcademicCalendarViewModel(private val repository: UniversityRepository) : ViewModel() {

    val events: StateFlow<List<AcademicEvent>> = repository.getAcademicEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEvent(event: AcademicEvent) {
        viewModelScope.launch {
            try { repository.addAcademicEvent(event) } catch (_: Exception) {}
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            try { repository.deleteAcademicEvent(id) } catch (_: Exception) {}
        }
    }
}

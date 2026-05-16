package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.Announcement
import com.example.academicmanager.data.AnnouncementType
import com.example.academicmanager.data.UniversityRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnnouncementsViewModel(private val repository: UniversityRepository) : ViewModel() {

    // Tüm duyurular — Firestore real-time akışı
    val announcements: StateFlow<List<Announcement>> = repository.getAnnouncements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Yeni duyuru ekle
    fun addAnnouncement(
        title: String,
        message: String,
        type: String = AnnouncementType.INFO,
        targetRole: String = "ALL",
        relatedCourseCode: String = "",
        createdBy: String = "admin"
    ) {
        viewModelScope.launch {
            try {
                repository.addAnnouncement(
                    Announcement(
                        title           = title,
                        message         = message,
                        type            = type,
                        timestamp       = System.currentTimeMillis(),
                        targetRole      = targetRole,
                        relatedCourseCode = relatedCourseCode,
                        createdBy       = createdBy
                    )
                )
            } catch (_: Exception) { }
        }
    }

    // Duyuru sil
    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteAnnouncement(id)
            } catch (_: Exception) { }
        }
    }
}

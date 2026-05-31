package com.example.academicmanager.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.AcademicEvent
import com.example.academicmanager.data.UniversityRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AcademicCalendarViewModel(private val repository: UniversityRepository) : ViewModel() {

    val events: StateFlow<List<AcademicEvent>> = repository.getAcademicEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pdfResult = MutableSharedFlow<Pair<Boolean, String>>(extraBufferCapacity = 1)
    val pdfResult: SharedFlow<Pair<Boolean, String>> = _pdfResult

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

    // ── PDF Yönetimi ─────────────────────────────────────────

    fun uploadCalendarPdf(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val storage = FirebaseStorage.getInstance()
                val ref     = storage.reference.child("academic_calendar/calendar.pdf")
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                repository.saveCalendarPdfUrl(url)
                _pdfResult.emit(Pair(true, url))
            } catch (e: Exception) {
                _pdfResult.emit(Pair(false, e.message ?: "Hata"))
            }
        }
    }

    fun getCalendarPdfUrl(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getCalendarPdfUrl())
        }
    }
}

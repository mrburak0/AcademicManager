package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.ExamEntry
import com.example.academicmanager.data.UniversityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExamViewModel(private val repository: UniversityRepository) : ViewModel() {

    val allExams: StateFlow<List<ExamEntry>> = repository.getExamEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _opResult = MutableSharedFlow<Boolean>()
    val opResult: SharedFlow<Boolean> = _opResult

    fun getExamsByDepartment(department: String): Flow<List<ExamEntry>> =
        repository.getExamEntriesByDepartment(department)

    fun addExam(
        courseCode: String, courseName: String, department: String,
        lecturerName: String, examDate: String, startTime: String,
        endTime: String, classroom: String, examType: String, notes: String
    ) {
        viewModelScope.launch {
            try {
                repository.addExamEntry(
                    ExamEntry(
                        courseCode   = courseCode,
                        courseName   = courseName,
                        department   = department,
                        lecturerName = lecturerName,
                        examDate     = examDate,
                        startTime    = startTime,
                        endTime      = endTime,
                        classroom    = classroom,
                        examType     = examType,
                        notes        = notes,
                        timestamp    = System.currentTimeMillis()
                    )
                )
                _opResult.emit(true)
            } catch (_: Exception) {
                _opResult.emit(false)
            }
        }
    }

    fun deleteExam(id: String) {
        viewModelScope.launch {
            try { repository.deleteExamEntry(id) } catch (_: Exception) { }
        }
    }
}

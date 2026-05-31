package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.AssignmentEntry
import com.example.academicmanager.data.AssignmentSubmission
import com.example.academicmanager.data.UniversityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssignmentViewModel(private val repository: UniversityRepository) : ViewModel() {

    val allAssignments: StateFlow<List<AssignmentEntry>> = repository.getAssignments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getAssignmentsByDepartment(department: String): Flow<List<AssignmentEntry>> =
        repository.getAssignmentsByDepartment(department)

    fun getAssignmentsByLecturer(username: String): Flow<List<AssignmentEntry>> =
        repository.getAssignmentsByLecturer(username)

    fun getSubmissions(assignmentId: String): Flow<List<AssignmentSubmission>> =
        repository.getSubmissions(assignmentId)

    fun getSubmissionsByStudent(username: String): Flow<List<AssignmentSubmission>> =
        repository.getSubmissionsByStudent(username)

    fun addAssignment(assignment: AssignmentEntry) {
        viewModelScope.launch {
            try { repository.addAssignment(assignment) } catch (_: Exception) {}
        }
    }

    fun deleteAssignment(id: String) {
        viewModelScope.launch {
            try { repository.deleteAssignment(id) } catch (_: Exception) {}
        }
    }

    fun submitAssignment(submission: AssignmentSubmission) {
        viewModelScope.launch {
            try { repository.submitAssignment(submission) } catch (_: Exception) {}
        }
    }
}

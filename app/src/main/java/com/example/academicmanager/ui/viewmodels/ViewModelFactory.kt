package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.academicmanager.data.UniversityDao
import com.example.academicmanager.data.UniversityRepository

class ViewModelFactory(
    private val dao: UniversityDao,
    private val repository: UniversityRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(dao, repository) as T
        }
        if (modelClass.isAssignableFrom(CourseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CourseViewModel(dao, repository) as T
        }
        if (modelClass.isAssignableFrom(DataImportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DataImportViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(AnnouncementsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnnouncementsViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(UniversityApiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UniversityApiViewModel() as T
        }
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(ExamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExamViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(AssignmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssignmentViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(AcademicCalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AcademicCalendarViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(MakeupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MakeupViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(AttendanceRiskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceRiskViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(PeerMatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PeerMatchViewModel(repository) as T
        }
        throw IllegalArgumentException("Bilinmeyen ViewModel sınıfı: ${modelClass.name}")
    }
}

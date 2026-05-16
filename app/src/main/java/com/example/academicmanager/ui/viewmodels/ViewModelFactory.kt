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
        throw IllegalArgumentException("Bilinmeyen ViewModel sınıfı: ${modelClass.name}")
    }
}

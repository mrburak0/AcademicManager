package com.example.academicmanager.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.CourseEntity
import com.example.academicmanager.data.UniversityDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory

import com.example.academicmanager.data.UniversityRepository

class CourseViewModel(
    private val dao: UniversityDao,
    private val repository: UniversityRepository
) : ViewModel() {

    var previewList by mutableStateOf<List<CourseEntity>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var snackbarMessage by mutableStateOf<String?>(null)
        private set

    fun onFileSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            isLoading = true
            try {
                val courses = withContext(Dispatchers.IO) {
                    val list = mutableListOf<CourseEntity>()
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val workbook = WorkbookFactory.create(inputStream)
                        val sheet = workbook.getSheetAt(0)
                        for (row in sheet) {
                            if (row.rowNum == 0) continue // Skip header
                            
                            val code = row.getCell(0)?.toString() ?: ""
                            val name = row.getCell(1)?.toString() ?: ""
                            val lecturer = row.getCell(2)?.toString() ?: ""
                            val dept = row.getCell(3)?.toString() ?: ""
                            val day = row.getCell(4)?.toString() ?: ""
                            val time = row.getCell(5)?.toString() ?: ""

                            if (code.isNotBlank()) {
                                list.add(CourseEntity(code, name, lecturer, dept, day, time))
                            }
                        }
                    }
                    list
                }
                if (courses.isEmpty()) {
                    snackbarMessage = "The file is empty or in wrong format."
                } else {
                    previewList = courses
                }
            } catch (e: Exception) {
                snackbarMessage = "Error reading file: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun saveToDb() {
        viewModelScope.launch {
            isLoading = true
            try {
                previewList.forEach { dao.insertCourse(it) }
                previewList = emptyList()
                snackbarMessage = "Data successfully saved to database."
            } catch (e: Exception) {
                snackbarMessage = "Error saving data: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun clearSnackbar() {
        snackbarMessage = null
    }
}

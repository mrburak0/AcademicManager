package com.example.academicmanager.ui.viewmodels

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.academicmanager.data.Course
import com.example.academicmanager.data.Lecturer
import com.example.academicmanager.data.UniversityRepository
import com.example.academicmanager.data.UserRole
import com.example.academicmanager.util.CredentialUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook

enum class ImportType { COURSES, LECTURERS }

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class PreviewReady(val items: List<Any>, val type: ImportType) : ImportState()
    data class Success(val message: String) : ImportState()
    data class Error(val message: String) : ImportState()
}

class DataImportViewModel(private val repository: UniversityRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<ImportState>(ImportState.Idle)
    val uiState: StateFlow<ImportState> = _uiState

    fun downloadTemplate(context: Context, type: ImportType) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Template")
                val headerRow = sheet.createRow(0)
                
                val columns = if (type == ImportType.COURSES) {
                    listOf("Course Code", "Course Name")
                } else {
                    listOf("Name", "Title", "Working Type")
                }

                columns.forEachIndexed { index, title ->
                    headerRow.createCell(index).setCellValue(title)
                }

                val fileName = "${if (type == ImportType.COURSES) "Courses" else "Lecturers"}_Template.xlsx"
                saveWorkbookToDownloads(context, workbook, fileName)
                _uiState.value = ImportState.Success("Template saved to Downloads: $fileName")
            } catch (e: Exception) {
                _uiState.value = ImportState.Error("Failed to create template: ${e.message}")
            }
        }
    }

    fun parseExcel(context: Context, uri: Uri, type: ImportType) {
        _uiState.value = ImportState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val workbook = XSSFWorkbook(inputStream)
                val sheet = workbook.getSheetAt(0)
                val items = mutableListOf<Any>()

                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue
                    if (type == ImportType.COURSES) {
                        val code = row.getCell(0)?.toString() ?: ""
                        val name = row.getCell(1)?.toString() ?: ""
                        if (code.isNotBlank()) {
                            items.add(Course(
                                courseCode = code,
                                courseName = name,
                                department = "General"
                            ))
                        }
                    } else {
                        val name = row.getCell(0)?.toString() ?: ""
                        val title = row.getCell(1)?.toString() ?: ""
                        val workingType = row.getCell(2)?.toString() ?: ""
                        if (name.isNotBlank()) {
                            // GENERATE CREDENTIALS HERE
                            val username = CredentialUtils.generateUsername(name, title)
                            val password = CredentialUtils.generatePassword()
                            
                            items.add(Lecturer(
                                fullName = name,
                                title = title,
                                workingType = workingType,
                                username = username,
                                password = password,
                                department = "General",
                                mustChangePassword = true,
                                role = UserRole.LECTURER
                            ))
                        }
                    }
                }
                if (items.isEmpty()) {
                    _uiState.value = ImportState.Error("No valid data found in Excel.")
                } else {
                    _uiState.value = ImportState.PreviewReady(items, type)
                }
            } catch (e: Exception) {
                _uiState.value = ImportState.Error("Invalid Excel format or file error.")
            }
        }
    }

    fun commitToDb(items: List<Any>, type: ImportType) {
        Log.d("FirestoreTest", "commitToDb triggered. Items size: ${items.size}, type: $type")
        _uiState.value = ImportState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (type == ImportType.COURSES) {
                    val courses = items.filterIsInstance<Course>()
                    Log.d("FirestoreTest", "Filtered courses count: ${courses.size}")
                    if (courses.isNotEmpty()) {
                        Log.d("FirestoreTest", "Saving to Firestore...")
                        repository.addCourses(courses)
                        Log.d("FirestoreTest", "Firestore save completed successfully.")
                    } else {
                        Log.w("FirestoreTest", "No Course objects found in items list!")
                    }
                } else {
                    val lecturers = items.filterIsInstance<Lecturer>()
                    Log.d("FirestoreTest", "Filtered lecturers count: ${lecturers.size}")
                    if (lecturers.isNotEmpty()) {
                        Log.d("FirestoreTest", "Saving to Firestore...")
                        repository.addLecturers(lecturers)
                        Log.d("FirestoreTest", "Firestore save completed successfully.")
                    } else {
                        Log.w("FirestoreTest", "No Lecturer objects found in items list!")
                    }
                }
                _uiState.value = ImportState.Success("Data successfully imported to database.")
            } catch (e: Exception) {
                Log.e("FirestoreTest", "Error during commitToDb execution", e)
                _uiState.value = ImportState.Error("Database error: ${e.message}")
            }
        }
    }

    fun resetState() { _uiState.value = ImportState.Idle }

    private fun saveWorkbookToDownloads(context: Context, workbook: XSSFWorkbook, fileName: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                workbook.write(out)
            }
        }
    }
}

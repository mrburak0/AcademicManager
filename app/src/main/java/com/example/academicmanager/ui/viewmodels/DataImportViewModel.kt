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
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook

enum class ImportType { COURSES, LECTURERS, CLASSROOMS }

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class PreviewReady(val items: List<Any>, val type: ImportType) : ImportState()
    data class Success(val message: String) : ImportState()
    data class Error(val message: String) : ImportState()
    /** Import tamamlandıktan sonra admin plaintext credential'ları görür */
    data class CredentialSheet(val credentials: List<Pair<String, String>>) : ImportState()
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
                
                val columns = when (type) {
                    ImportType.COURSES    -> listOf("Course Code", "Course Name", "Department")
                    ImportType.LECTURERS  -> listOf("Name", "Title", "Working Type", "Department")
                    ImportType.CLASSROOMS -> listOf("Name", "Capacity", "Type")
                }

                columns.forEachIndexed { index, title ->
                    headerRow.createCell(index).setCellValue(title)
                }

                val fileName = when (type) {
                    ImportType.COURSES    -> "Courses_Template.xlsx"
                    ImportType.LECTURERS  -> "Lecturers_Template.xlsx"
                    ImportType.CLASSROOMS -> "Classrooms_Template.xlsx"
                }
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
                    if (type == ImportType.CLASSROOMS) {
                        val name = row.getCell(0).safeString()
                        val cap  = row.getCell(1).safeString().toIntOrNull() ?: 0
                        val rawType = row.getCell(2).safeString().uppercase().trim()
                        val classroomType = when {
                            rawType.contains("LAB") && rawType.contains("COMPUTER") -> com.example.academicmanager.data.ClassroomType.COMPUTER_LAB
                            rawType.contains("LAB") -> com.example.academicmanager.data.ClassroomType.LAB
                            else -> com.example.academicmanager.data.ClassroomType.LECTURE
                        }
                        if (name.isNotBlank() && cap in 1..2000) {
                            items.add(com.example.academicmanager.data.Classroom(name = name, capacity = cap, classroomType = classroomType))
                        }
                    } else if (type == ImportType.COURSES) {
                        val code = row.getCell(0).safeString()
                        val name = row.getCell(1).safeString()
                        val dept = row.getCell(2).safeString()
                        if (code.isNotBlank()) {
                            items.add(Course(
                                courseCode = code,
                                courseName = name,
                                department = dept.ifBlank { "General" }
                            ))
                        }
                    } else {
                        val name = row.getCell(0).safeString()
                        val title = row.getCell(1).safeString()
                        val workingType = row.getCell(2).safeString()
                        val dept = row.getCell(3).safeString()
                        if (name.isNotBlank()) {
                            val baseUsername = CredentialUtils.generateUsername(name, title)
                            val usedUsernames = items.filterIsInstance<Lecturer>().map { it.username }.toSet()
                            var username = baseUsername
                            var suffix = 2
                            while (username in usedUsernames) {
                                username = "${baseUsername}_$suffix"
                                suffix++
                            }
                            val password = CredentialUtils.generatePassword()

                            items.add(Lecturer(
                                fullName = name,
                                title = title,
                                workingType = workingType,
                                username = username,
                                password = password,
                                department = dept.ifBlank { "General" },
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
                when (type) {
                    ImportType.COURSES -> {
                        val courses = items.filterIsInstance<com.example.academicmanager.data.Course>()
                        if (courses.isNotEmpty()) repository.addCourses(courses)
                    }
                    ImportType.CLASSROOMS -> {
                        val rooms = items.filterIsInstance<com.example.academicmanager.data.Classroom>()
                        rooms.forEach { repository.addClassroom(it) }
                    }
                    ImportType.LECTURERS -> {
                        val rawLecturers = items.filterIsInstance<Lecturer>()
                        if (rawLecturers.isNotEmpty()) {
                            val credentials   = rawLecturers.map { it.username to it.password }
                            val hashedLecturers = rawLecturers.map { it.copy(password = CredentialUtils.hashPassword(it.password)) }
                            repository.addLecturers(hashedLecturers)
                            _uiState.value = ImportState.CredentialSheet(credentials)
                            return@launch
                        }
                    }
                }
                _uiState.value = ImportState.Success("Veriler başarıyla kaydedildi.")
            } catch (e: Exception) {
                Log.e("FirestoreTest", "Error during commitToDb execution", e)
                _uiState.value = ImportState.Error("Database error: ${e.message}")
            }
        }
    }

    fun resetState() { _uiState.value = ImportState.Idle }

    fun downloadClassroomTemplate(context: Context) = downloadTemplate(context, ImportType.CLASSROOMS)

    private fun Cell?.safeString(): String {
        if (this == null) return ""
        return when (cellType) {
            CellType.STRING  -> stringCellValue?.trim() ?: ""
            CellType.NUMERIC -> numericCellValue.toLong().toString()
            CellType.BOOLEAN -> booleanCellValue.toString()
            CellType.FORMULA -> try { stringCellValue?.trim() ?: "" } catch (_: Exception) { numericCellValue.toLong().toString() }
            else             -> ""
        }
    }

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

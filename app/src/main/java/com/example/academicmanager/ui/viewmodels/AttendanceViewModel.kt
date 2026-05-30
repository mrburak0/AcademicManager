package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.AttendanceRecord
import com.example.academicmanager.data.SessionType
import com.example.academicmanager.data.UniversityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AttendanceViewModel(private val repository: UniversityRepository) : ViewModel() {

    val allRecords: StateFlow<List<AttendanceRecord>> = repository.getAttendanceRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveResult = MutableSharedFlow<Boolean>()
    val saveResult: SharedFlow<Boolean> = _saveResult

    fun getRecordsByLecturer(lecturerUsername: String): Flow<List<AttendanceRecord>> =
        repository.getAttendanceByLecturer(lecturerUsername)

    fun getRecordsByCourse(courseCode: String): Flow<List<AttendanceRecord>> =
        repository.getAttendanceByCourse(courseCode)

    fun saveAttendance(
        existingId: String,
        courseCode: String,
        courseName: String,
        department: String,
        lecturerUsername: String,
        sessionDate: String,
        dayOfWeek: String,
        timeSlot: String,
        sessionType: String,
        presentStudents: List<String>,
        absentStudents: List<String>
    ) {
        viewModelScope.launch {
            try {
                val record = AttendanceRecord(
                    id = existingId,
                    courseCode = courseCode,
                    courseName = courseName,
                    department = department,
                    lecturerUsername = lecturerUsername,
                    sessionDate = sessionDate,
                    dayOfWeek = dayOfWeek,
                    timeSlot = timeSlot,
                    sessionType = sessionType,
                    presentStudents = presentStudents,
                    absentStudents = absentStudents,
                    totalStudents = presentStudents.size + absentStudents.size,
                    timestamp = System.currentTimeMillis()
                )
                if (existingId.isBlank()) {
                    repository.saveAttendance(record)
                } else {
                    repository.updateAttendance(record)
                }
                _saveResult.emit(true)
            } catch (_: Exception) {
                _saveResult.emit(false)
            }
        }
    }

    // Returns attendance % for a specific student in a specific course
    fun attendancePercent(records: List<AttendanceRecord>, studentUsername: String): Float {
        if (records.isEmpty()) return 0f
        val attended = records.count { studentUsername in it.presentStudents }
        return (attended.toFloat() / records.size.toFloat()) * 100f
    }

    // For a student, compute attendance per course from a list of all records for their dept courses
    fun courseAttendanceMap(
        records: List<AttendanceRecord>,
        studentUsername: String,
        courseCodes: Set<String>
    ): Map<String, Pair<Int, Int>> { // courseCode -> (attended, total)
        return courseCodes.associateWith { code ->
            val courseRecords = records.filter { it.courseCode == code }
            val attended = courseRecords.count { studentUsername in it.presentStudents }
            Pair(attended, courseRecords.size)
        }
    }
}

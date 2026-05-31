package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.AttendanceRecord
import com.example.academicmanager.data.AttendanceSession
import com.example.academicmanager.data.SessionType
import com.example.academicmanager.data.UniversityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class AttendanceViewModel(private val repository: UniversityRepository) : ViewModel() {

    val allRecords: StateFlow<List<AttendanceRecord>> = repository.getAttendanceRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveResult = MutableSharedFlow<Boolean>()
    val saveResult: SharedFlow<Boolean> = _saveResult

    private val _sessionResult = MutableSharedFlow<AttendanceSession?>()
    val sessionResult: SharedFlow<AttendanceSession?> = _sessionResult

    private val _qrJoinResult = MutableSharedFlow<String>()
    val qrJoinResult: SharedFlow<String> = _qrJoinResult

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

    // ── QR Oturum Yönetimi ─────────────────────────────────────

    fun getActiveSession(courseCode: String): Flow<AttendanceSession?> =
        repository.getActiveSession(courseCode)

    fun createQrSession(
        courseCode: String, courseName: String, lecturerUsername: String,
        lecturerName: String, department: String, dayOfWeek: String,
        timeSlot: String, sessionType: String
    ) {
        viewModelScope.launch {
            try {
                val code = UUID.randomUUID().toString().take(8).uppercase()
                val now  = System.currentTimeMillis()
                val session = AttendanceSession(
                    sessionCode      = code,
                    courseCode       = courseCode,
                    courseName       = courseName,
                    lecturerUsername = lecturerUsername,
                    lecturerName     = lecturerName,
                    department       = department,
                    sessionDate      = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    dayOfWeek        = dayOfWeek,
                    timeSlot         = timeSlot,
                    sessionType      = sessionType,
                    createdAt        = now,
                    expiresAt        = now + 15 * 60 * 1000L,  // 15 dakika
                    isActive         = true
                )
                val saved = repository.createSession(session)
                _sessionResult.emit(saved)
            } catch (_: Exception) {
                _sessionResult.emit(null)
            }
        }
    }

    fun endSession(session: AttendanceSession) {
        viewModelScope.launch {
            try { repository.updateSession(session.copy(isActive = false)) } catch (_: Exception) {}
        }
    }

    // Öğrenci QR kodunu taradıktan sonra Firestore'dan oturumu doğrular
    fun joinSessionByCode(
        sessionCode: String,
        studentUsername: String,
        studentName: String,
        allStudents: List<com.example.academicmanager.data.Lecturer>
    ) {
        viewModelScope.launch {
            try {
                val session = repository.getSessionByCode(sessionCode)
                when {
                    session == null ->
                        _qrJoinResult.emit("INVALID")
                    studentUsername in session.presentStudents ->
                        _qrJoinResult.emit("ALREADY_MARKED")
                    else -> {
                        repository.addStudentToSession(session.id, studentUsername)
                        // Aynı zamanda attendance kaydını güncelle
                        val todayFmt = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        val absentStudents = allStudents
                            .filter { it.department == session.department }
                            .map { it.username }
                            .filter { it != studentUsername && it !in session.presentStudents }
                        repository.saveAttendance(
                            AttendanceRecord(
                                courseCode       = session.courseCode,
                                courseName       = session.courseName,
                                department       = session.department,
                                lecturerUsername = session.lecturerUsername,
                                sessionDate      = todayFmt,
                                dayOfWeek        = session.dayOfWeek,
                                timeSlot         = session.timeSlot,
                                sessionType      = session.sessionType,
                                presentStudents  = session.presentStudents + studentUsername,
                                absentStudents   = absentStudents,
                                totalStudents    = allStudents.count { it.department == session.department },
                                timestamp        = System.currentTimeMillis()
                            )
                        )
                        _qrJoinResult.emit("SUCCESS:${session.courseName}")
                    }
                }
            } catch (_: Exception) {
                _qrJoinResult.emit("ERROR")
            }
        }
    }

    // Returns attendance % for a specific student in a specific course
    fun attendancePercent(records: List<AttendanceRecord>, studentUsername: String): Float {
        if (records.isEmpty()) return 0f
        val attended = records.count { studentUsername in it.presentStudents }
        return (attended.toFloat() / records.size.toFloat()) * 100f
    }

    fun courseAttendanceMap(
        records: List<AttendanceRecord>,
        studentUsername: String,
        courseCodes: Set<String>
    ): Map<String, Pair<Int, Int>> {
        return courseCodes.associateWith { code ->
            val courseRecords = records.filter { it.courseCode == code }
            val attended = courseRecords.count { studentUsername in it.presentStudents }
            Pair(attended, courseRecords.size)
        }
    }
}

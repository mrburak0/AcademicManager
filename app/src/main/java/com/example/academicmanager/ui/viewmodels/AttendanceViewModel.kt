package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

// ── Katılım sonuç tipi ─────────────────────────────────────────

sealed class JoinResult {
    data class Success(val courseName: String, val method: String) : JoinResult()
    data class AlreadyMarked(val courseName: String) : JoinResult()
    object Expired : JoinResult()
    object InvalidQr : JoinResult()
    object Error : JoinResult()
}

// ─────────────────────────────────────────────────────────────
// ATTENDANCE VIEW MODEL
// ─────────────────────────────────────────────────────────────

class AttendanceViewModel(private val repository: UniversityRepository) : ViewModel() {

    // ── Tüm kayıtlar (öğrenci devam ekranı için) ──────────────

    val allRecords: StateFlow<List<AttendanceRecord>> =
        repository.getAttendanceRecords()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Sonuç akışları ────────────────────────────────────────

    private val _saveResult = MutableSharedFlow<Boolean>()
    val saveResult: SharedFlow<Boolean> = _saveResult

    private val _sessionStart = MutableSharedFlow<AttendanceSession?>()
    val sessionStart: SharedFlow<AttendanceSession?> = _sessionStart

    private val _joinResult = MutableSharedFlow<JoinResult>()
    val joinResult: SharedFlow<JoinResult> = _joinResult

    // ── Manuel kayıt ──────────────────────────────────────────

    fun getRecordsByLecturer(lecturerUsername: String): Flow<List<AttendanceRecord>> =
        repository.getAttendanceByLecturer(lecturerUsername)

    fun getRecordsByCourse(courseCode: String): Flow<List<AttendanceRecord>> =
        repository.getAttendanceByCourse(courseCode)

    fun saveAttendance(
        existingId      : String,
        courseCode      : String,
        courseName      : String,
        department      : String,
        lecturerUsername: String,
        sessionDate     : String,
        dayOfWeek       : String,
        timeSlot        : String,
        sessionType     : String,
        presentStudents : List<String>,
        absentStudents  : List<String>
    ) {
        viewModelScope.launch {
            try {
                val record = AttendanceRecord(
                    id               = existingId,
                    courseCode       = courseCode,
                    courseName       = courseName,
                    department       = department,
                    lecturerUsername = lecturerUsername,
                    sessionDate      = sessionDate,
                    dayOfWeek        = dayOfWeek,
                    timeSlot         = timeSlot,
                    sessionType      = sessionType,
                    presentStudents  = presentStudents,
                    absentStudents   = absentStudents,
                    totalStudents    = presentStudents.size + absentStudents.size,
                    timestamp        = System.currentTimeMillis()
                )
                if (existingId.isBlank()) repository.saveAttendance(record)
                else repository.updateAttendance(record)
                _saveResult.emit(true)
            } catch (_: Exception) {
                _saveResult.emit(false)
            }
        }
    }

    // ── QR/BLE Oturum yönetimi (hoca) ─────────────────────────

    fun getActiveSession(courseCode: String): Flow<AttendanceSession?> =
        repository.getActiveSession(courseCode)

    fun getActiveSessionsForDept(department: String): Flow<List<AttendanceSession>> =
        repository.getActiveSessionsByDepartment(department)

    /**
     * Yeni bir yoklama oturumu başlatır.
     * Ders programı kontrolü dışarıda yapılır (UI'da uyarı gösterilir),
     * ancak isScheduled false ise yine de oturum açılabilir.
     */
    fun startSession(
        entry           : ScheduleEntry,
        department      : String,
        lecturerUsername: String,
        lecturerName    : String,
        durationMinutes : Int
    ) {
        viewModelScope.launch {
            try {
                val now     = System.currentTimeMillis()
                val code    = UUID.randomUUID().toString().take(8).uppercase()
                val secret  = UUID.randomUUID().toString().replace("-", "")
                val session = AttendanceSession(
                    sessionCode      = code,
                    sessionSecret    = secret,
                    courseCode       = entry.courseCode,
                    courseName       = entry.courseName,
                    lecturerUsername = lecturerUsername,
                    lecturerName     = lecturerName,
                    department       = department,
                    sessionDate      = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    dayOfWeek        = entry.dayOfWeek,
                    timeSlot         = entry.timeSlot,
                    sessionType      = entry.sessionType,
                    createdAt        = now,
                    expiresAt        = now + durationMinutes * 60_000L,
                    durationMinutes  = durationMinutes,
                    isActive         = true
                )
                val saved = repository.createSession(session)
                _sessionStart.emit(saved)
            } catch (_: Exception) {
                _sessionStart.emit(null)
            }
        }
    }

    /** Oturumun bitiş süresini uzatır. */
    fun extendSession(session: AttendanceSession, additionalMinutes: Int) {
        viewModelScope.launch {
            try {
                repository.updateSession(
                    session.copy(expiresAt = session.expiresAt + additionalMinutes * 60_000L)
                )
            } catch (_: Exception) {}
        }
    }

    /** Oturumu kapatır ve bir AttendanceRecord oluşturur/günceller. */
    fun endSession(session: AttendanceSession, allDeptStudents: List<Lecturer> = emptyList()) {
        viewModelScope.launch {
            try {
                repository.updateSession(session.copy(isActive = false))
                if (allDeptStudents.isNotEmpty()) {
                    val absent = allDeptStudents.map { it.username }
                        .filter { it !in session.presentStudents }
                    repository.saveAttendance(
                        AttendanceRecord(
                            id                  = session.id,
                            courseCode          = session.courseCode,
                            courseName          = session.courseName,
                            department          = session.department,
                            lecturerUsername    = session.lecturerUsername,
                            sessionDate         = session.sessionDate,
                            dayOfWeek           = session.dayOfWeek,
                            timeSlot            = session.timeSlot,
                            sessionType         = session.sessionType,
                            presentStudents     = session.presentStudents,
                            absentStudents      = absent,
                            totalStudents       = allDeptStudents.size,
                            verificationMethods = session.verificationMethods,
                            timestamp           = System.currentTimeMillis()
                        )
                    )
                }
            } catch (_: Exception) {}
        }
    }

    // ── Öğrenci katılımı ──────────────────────────────────────

    /**
     * Öğrenciyi oturuma ekler.
     * [method]: "BLE" (Bluetooth ile doğrulandı) veya "QR" (QR ile doğrulandı)
     */
    fun markPresent(
        sessionId   : String,
        username    : String,
        fullName    : String,
        method      : String,
        allStudents : List<Lecturer>
    ) {
        viewModelScope.launch {
            try {
                val session = repository.getSessionById(sessionId)
                when {
                    session == null ->
                        _joinResult.emit(JoinResult.Error)
                    !session.isActive || System.currentTimeMillis() > session.expiresAt ->
                        _joinResult.emit(JoinResult.Expired)
                    username in session.presentStudents ->
                        _joinResult.emit(JoinResult.AlreadyMarked(session.courseName))
                    else -> {
                        repository.addStudentToSessionWithMethod(sessionId, username, method)
                        // Attendance kaydını güncelle (session ID = record ID → idempotent)
                        val updatedPresent = session.presentStudents + username
                        val deptStudents   = allStudents.filter { it.department == session.department }
                        val absent         = deptStudents.map { it.username }.filter { it !in updatedPresent }
                        repository.saveAttendance(
                            AttendanceRecord(
                                id                  = session.id,
                                courseCode          = session.courseCode,
                                courseName          = session.courseName,
                                department          = session.department,
                                lecturerUsername    = session.lecturerUsername,
                                sessionDate         = session.sessionDate,
                                dayOfWeek           = session.dayOfWeek,
                                timeSlot            = session.timeSlot,
                                sessionType         = session.sessionType,
                                presentStudents     = updatedPresent,
                                absentStudents      = absent,
                                totalStudents       = deptStudents.size,
                                verificationMethods = session.verificationMethods + mapOf(username to method),
                                timestamp           = System.currentTimeMillis()
                            )
                        )
                        _joinResult.emit(JoinResult.Success(session.courseName, method))
                    }
                }
            } catch (_: Exception) {
                _joinResult.emit(JoinResult.Error)
            }
        }
    }

    /**
     * QR içeriğini TOTP ile doğrular ve başarılıysa katılım kaydeder.
     * [session]: öğrencinin dinlediği gerçek zamanlı aktif oturum.
     */
    fun validateQrAndMark(
        qrContent   : String,
        session     : AttendanceSession,
        username    : String,
        fullName    : String,
        allStudents : List<Lecturer>
    ) {
        viewModelScope.launch {
            when {
                !AttendanceQrHelper.validate(qrContent, session) ->
                    _joinResult.emit(JoinResult.InvalidQr)
                else ->
                    markPresent(session.id, username, fullName, "QR", allStudents)
            }
        }
    }

    // ── Ders programı kontrolü ────────────────────────────────

    fun isCurrentlyScheduled(entry: ScheduleEntry): Boolean =
        AttendanceQrHelper.isCurrentlyScheduled(entry)

    // ── İstatistik yardımcıları ───────────────────────────────

    fun attendancePercent(records: List<AttendanceRecord>, studentUsername: String): Float {
        if (records.isEmpty()) return 0f
        val attended = records.count { studentUsername in it.presentStudents }
        return (attended.toFloat() / records.size.toFloat()) * 100f
    }

    fun courseAttendanceMap(
        records         : List<AttendanceRecord>,
        studentUsername : String,
        courseCodes     : Set<String>
    ): Map<String, Pair<Int, Int>> = courseCodes.associateWith { code ->
        val r = records.filter { it.courseCode == code }
        r.count { studentUsername in it.presentStudents } to r.size
    }

    // ── Geriye dönük uyumluluk ────────────────────────────────

    @Deprecated("Use startSession()")
    fun createQrSession(
        courseCode: String, courseName: String, lecturerUsername: String,
        lecturerName: String, department: String, dayOfWeek: String,
        timeSlot: String, sessionType: String
    ) {
        val fakeEntry = ScheduleEntry(
            courseCode = courseCode, courseName = courseName,
            dayOfWeek = dayOfWeek, timeSlot = timeSlot, sessionType = sessionType
        )
        startSession(fakeEntry, department, lecturerUsername, lecturerName, 15)
    }

    @Deprecated("Use markPresent()")
    fun joinSessionByCode(
        sessionCode   : String,
        studentUsername: String,
        studentName   : String,
        allStudents   : List<Lecturer>
    ) {
        viewModelScope.launch {
            try {
                val session = repository.getSessionByCode(sessionCode)
                if (session == null) { _joinResult.emit(JoinResult.Error); return@launch }
                markPresent(session.id, studentUsername, studentName, "QR", allStudents)
            } catch (_: Exception) { _joinResult.emit(JoinResult.Error) }
        }
    }

    // SharedFlow uyumluluğu — eski _qrJoinResult kodunu kırmamak için
    val qrJoinResult: SharedFlow<String> = joinResult.map { result ->
        when (result) {
            is JoinResult.Success      -> "SUCCESS:${result.courseName}"
            is JoinResult.AlreadyMarked -> "ALREADY_MARKED"
            is JoinResult.Expired       -> "INVALID"
            is JoinResult.InvalidQr     -> "INVALID"
            is JoinResult.Error         -> "ERROR"
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    // SessionResult uyumluluğu
    val sessionResult: SharedFlow<AttendanceSession?> = sessionStart
}

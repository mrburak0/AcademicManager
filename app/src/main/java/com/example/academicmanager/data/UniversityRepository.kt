package com.example.academicmanager.data

import kotlinx.coroutines.flow.Flow

interface UniversityRepository {
    // Bölümler
    fun getDepartments(): Flow<List<Department>>
    suspend fun addDepartment(department: Department)

    // Dersler
    fun getCourses(): Flow<List<Course>>
    suspend fun addCourses(courses: List<Course>)
    suspend fun deleteCourse(courseCode: String)

    // Öğretim görevlileri (LECTURER + STUDENT aynı koleksiyon)
    fun getLecturers(): Flow<List<Lecturer>>
    suspend fun addLecturers(lecturers: List<Lecturer>)
    suspend fun updateLecturer(lecturer: Lecturer)
    suspend fun getLecturerByUsername(username: String): Lecturer?

    // Öğrenciler (lecturers koleksiyonundan role=STUDENT filtreli)
    fun getStudents(): Flow<List<Lecturer>>
    suspend fun addStudent(student: Lecturer)
    fun getStudentsByDepartment(department: String): Flow<List<Lecturer>>

    // Sınıflar
    fun getClassrooms(): Flow<List<Classroom>>
    suspend fun addClassroom(classroom: Classroom)

    // Program girişleri
    fun getScheduleEntries(): Flow<List<ScheduleEntry>>
    suspend fun addScheduleEntry(entry: ScheduleEntry)
    suspend fun deleteScheduleEntry(entryId: String)
    fun getLecturerSchedule(lecturerName: String): Flow<List<ScheduleEntry>>

    // Duyurular
    fun getAnnouncements(): Flow<List<Announcement>>
    suspend fun addAnnouncement(announcement: Announcement)
    suspend fun deleteAnnouncement(id: String)

    // Kayıt onay sistemi
    fun getPendingRegistrations(): Flow<List<Lecturer>>

    // Silme işlemleri
    suspend fun deleteLecturer(username: String)
    suspend fun deleteClassroom(classroomId: String)

    // Seed temizleme (tüm koleksiyonu siler)
    suspend fun clearScheduleEntries()
    suspend fun clearClassrooms()

    // Ders talep akışı
    fun getScheduleRequests(): Flow<List<ScheduleRequest>>
    fun getLecturerRequests(lecturerUsername: String): Flow<List<ScheduleRequest>>
    suspend fun addScheduleRequest(request: ScheduleRequest)
    suspend fun updateScheduleRequest(request: ScheduleRequest)

    // Müsaitlik haritası
    fun getAvailabilities(): Flow<List<LecturerAvailability>>
    fun getLecturerAvailabilities(lecturerUsername: String): Flow<List<LecturerAvailability>>
    suspend fun addAvailability(availability: LecturerAvailability)
    suspend fun updateAvailability(availability: LecturerAvailability)

    // Sınav takvimi
    fun getExamEntries(): Flow<List<ExamEntry>>
    fun getExamEntriesByDepartment(department: String): Flow<List<ExamEntry>>
    suspend fun addExamEntry(entry: ExamEntry)
    suspend fun deleteExamEntry(id: String)

    // Yoklama takibi
    fun getAttendanceRecords(): Flow<List<AttendanceRecord>>
    fun getAttendanceByCourse(courseCode: String): Flow<List<AttendanceRecord>>
    fun getAttendanceByLecturer(lecturerUsername: String): Flow<List<AttendanceRecord>>
    suspend fun saveAttendance(record: AttendanceRecord)
    suspend fun updateAttendance(record: AttendanceRecord)

    // Ödev takibi
    fun getAssignments(): Flow<List<AssignmentEntry>>
    fun getAssignmentsByDepartment(department: String): Flow<List<AssignmentEntry>>
    fun getAssignmentsByLecturer(lecturerUsername: String): Flow<List<AssignmentEntry>>
    suspend fun addAssignment(assignment: AssignmentEntry)
    suspend fun deleteAssignment(id: String)
    fun getSubmissions(assignmentId: String): Flow<List<AssignmentSubmission>>
    fun getSubmissionsByStudent(studentUsername: String): Flow<List<AssignmentSubmission>>
    suspend fun submitAssignment(submission: AssignmentSubmission)

    // Akademik takvim
    fun getAcademicEvents(): Flow<List<AcademicEvent>>
    suspend fun addAcademicEvent(event: AcademicEvent)
    suspend fun deleteAcademicEvent(id: String)

    // Akademik takvim PDF
    suspend fun saveCalendarPdfUrl(url: String)
    suspend fun getCalendarPdfUrl(): String?

    // QR Yoklama oturumları
    fun getActiveSession(courseCode: String): Flow<AttendanceSession?>
    fun getActiveSessionsByDepartment(department: String): Flow<List<AttendanceSession>>
    suspend fun getSessionById(id: String): AttendanceSession?
    suspend fun createSession(session: AttendanceSession): AttendanceSession
    suspend fun updateSession(session: AttendanceSession)
    suspend fun getSessionByCode(sessionCode: String): AttendanceSession?
    suspend fun addStudentToSession(sessionId: String, studentUsername: String)
    suspend fun addStudentToSessionWithMethod(sessionId: String, username: String, method: String)

    // Telafi dersi sistemi
    fun getMakeupRequestsByDepartment(department: String): Flow<List<MakeupRequest>>
    fun getMakeupRequestsByLecturer(lecturerUsername: String): Flow<List<MakeupRequest>>
    suspend fun getMakeupRequestById(id: String): MakeupRequest?
    suspend fun saveMakeupRequest(request: MakeupRequest): MakeupRequest
    suspend fun updateMakeupRequest(request: MakeupRequest)
    suspend fun voteForMakeupSlot(requestId: String, studentUsername: String, slotId: String)

    // Akran eşleştirme
    fun getPeerMatchesByDepartment(department: String): Flow<List<PeerMatch>>
    fun getPeerMatchesByStudent(username: String): Flow<List<PeerMatch>>
    suspend fun savePeerMatch(match: PeerMatch): PeerMatch
    suspend fun updatePeerMatch(match: PeerMatch)
}

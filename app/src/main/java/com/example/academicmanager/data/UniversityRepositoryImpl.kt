package com.example.academicmanager.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UniversityRepositoryImpl(
    private val firestore: FirebaseFirestore
) : UniversityRepository {

    // ── Bölümler ──────────────────────────────────────────────

    override fun getDepartments(): Flow<List<Department>> {
        return firestore.collection("departments")
            .snapshots()
            .map { it.toObjects(Department::class.java) }
    }

    override suspend fun addDepartment(department: Department) {
        val docRef = if (department.id.isEmpty())
            firestore.collection("departments").document()
        else
            firestore.collection("departments").document(department.id)
        docRef.set(department.copy(id = docRef.id)).await()
    }

    // ── Dersler ───────────────────────────────────────────────

    override fun getCourses(): Flow<List<Course>> {
        return firestore.collection("courses")
            .snapshots()
            .map { it.toObjects(Course::class.java) }
    }

    override suspend fun addCourses(courses: List<Course>) {
        try {
            Log.d("Repo", "Batch write: ${courses.size} ders")
            val batch = firestore.batch()
            courses.forEach { course ->
                batch.set(firestore.collection("courses").document(course.courseCode), course)
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("Repo", "Ders batch yazımı başarısız", e)
            throw e
        }
    }

    override suspend fun deleteCourse(courseCode: String) {
        firestore.collection("courses").document(courseCode).delete().await()
    }

    // ── Öğretim Görevlileri ───────────────────────────────────

    override fun getLecturers(): Flow<List<Lecturer>> {
        return firestore.collection("lecturers")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Lecturer::class.java)
                    .filter { it.role != UserRole.STUDENT }
            }
    }

    override suspend fun addLecturers(lecturers: List<Lecturer>) {
        try {
            Log.d("Repo", "Batch write: ${lecturers.size} öğretim görevlisi")
            val batch = firestore.batch()
            lecturers.forEach { lecturer ->
                batch.set(firestore.collection("lecturers").document(lecturer.username), lecturer)
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("Repo", "Öğretim görevlisi batch yazımı başarısız", e)
            throw e
        }
    }

    override suspend fun updateLecturer(lecturer: Lecturer) {
        firestore.collection("lecturers").document(lecturer.username).set(lecturer).await()
    }

    override suspend fun getLecturerByUsername(username: String): Lecturer? {
        return firestore.collection("lecturers")
            .document(username)
            .get().await()
            .toObject(Lecturer::class.java)
    }

    // ── Öğrenciler ────────────────────────────────────────────

    override fun getStudents(): Flow<List<Lecturer>> {
        return firestore.collection("lecturers")
            .whereEqualTo("role", UserRole.STUDENT.name)
            .snapshots()
            .map { it.toObjects(Lecturer::class.java) }
    }

    override suspend fun addStudent(student: Lecturer) {
        val s = student.copy(role = UserRole.STUDENT)
        firestore.collection("lecturers").document(s.username).set(s).await()
    }

    override fun getStudentsByDepartment(department: String): Flow<List<Lecturer>> {
        return firestore.collection("lecturers")
            .whereEqualTo("role", UserRole.STUDENT.name)
            .whereEqualTo("department", department)
            .snapshots()
            .map { it.toObjects(Lecturer::class.java) }
    }

    // ── Sınıflar ──────────────────────────────────────────────

    override fun getClassrooms(): Flow<List<Classroom>> {
        return firestore.collection("classrooms")
            .snapshots()
            .map { it.toObjects(Classroom::class.java) }
    }

    override suspend fun addClassroom(classroom: Classroom) {
        val docRef = if (classroom.id.isEmpty())
            firestore.collection("classrooms").document()
        else
            firestore.collection("classrooms").document(classroom.id)
        docRef.set(classroom.copy(id = docRef.id)).await()
    }

    // ── Program Girişleri ─────────────────────────────────────

    override fun getScheduleEntries(): Flow<List<ScheduleEntry>> {
        return firestore.collection("schedule_entries")
            .snapshots()
            .map { it.toObjects(ScheduleEntry::class.java) }
    }

    override suspend fun addScheduleEntry(entry: ScheduleEntry) {
        val docRef = if (entry.id.isEmpty())
            firestore.collection("schedule_entries").document()
        else
            firestore.collection("schedule_entries").document(entry.id)
        docRef.set(entry.copy(id = docRef.id)).await()
    }

    override suspend fun deleteScheduleEntry(entryId: String) {
        firestore.collection("schedule_entries").document(entryId).delete().await()
    }

    override fun getLecturerSchedule(lecturerName: String): Flow<List<ScheduleEntry>> {
        return firestore.collection("schedule_entries")
            .whereEqualTo("lecturerName", lecturerName)
            .snapshots()
            .map { it.toObjects(ScheduleEntry::class.java) }
    }

    // ── Duyurular ─────────────────────────────────────────────

    override fun getAnnouncements(): Flow<List<Announcement>> {
        return firestore.collection("announcements")
            .snapshots()
            .map { it.toObjects(Announcement::class.java) }
    }

    override suspend fun addAnnouncement(announcement: Announcement) {
        val docRef = if (announcement.id.isEmpty())
            firestore.collection("announcements").document()
        else
            firestore.collection("announcements").document(announcement.id)
        docRef.set(announcement.copy(id = docRef.id)).await()
    }

    override suspend fun deleteAnnouncement(id: String) {
        firestore.collection("announcements").document(id).delete().await()
    }

    // ── Kayıt Onay Sistemi ────────────────────────────────────

    override fun getPendingRegistrations(): Flow<List<Lecturer>> {
        return firestore.collection("lecturers")
            .whereEqualTo("status", AccountStatus.PENDING)
            .snapshots()
            .map { it.toObjects(Lecturer::class.java) }
    }

    // ── Silme İşlemleri ───────────────────────────────────────

    override suspend fun deleteLecturer(username: String) {
        firestore.collection("lecturers").document(username).delete().await()
    }

    override suspend fun deleteClassroom(classroomId: String) {
        firestore.collection("classrooms").document(classroomId).delete().await()
    }

    // ── Seed Temizleme ────────────────────────────────────────

    override suspend fun clearScheduleEntries() {
        val docs = firestore.collection("schedule_entries").get().await()
        docs.documents.chunked(499).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    override suspend fun clearClassrooms() {
        val docs = firestore.collection("classrooms").get().await()
        docs.documents.chunked(499).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    // ── Ders Talep Akışı ─────────────────────────────────────

    override fun getScheduleRequests(): Flow<List<ScheduleRequest>> {
        return try {
            firestore.collection("schedule_requests")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(ScheduleRequest::class.java) }
        } catch (e: Exception) {
            firestore.collection("schedule_requests")
                .snapshots()
                .map { it.toObjects(ScheduleRequest::class.java) }
        }
    }

    override fun getLecturerRequests(lecturerUsername: String): Flow<List<ScheduleRequest>> {
        return firestore.collection("schedule_requests")
            .whereEqualTo("lecturerUsername", lecturerUsername)
            .snapshots()
            .map { it.toObjects(ScheduleRequest::class.java) }
    }

    override suspend fun addScheduleRequest(request: ScheduleRequest) {
        val docRef = if (request.id.isEmpty())
            firestore.collection("schedule_requests").document()
        else
            firestore.collection("schedule_requests").document(request.id)
        docRef.set(request.copy(id = docRef.id)).await()
    }

    override suspend fun updateScheduleRequest(request: ScheduleRequest) {
        firestore.collection("schedule_requests").document(request.id).set(request).await()
    }

    // ── Müsaitlik Haritası ────────────────────────────────────

    override fun getAvailabilities(): Flow<List<LecturerAvailability>> {
        return try {
            firestore.collection("lecturer_availability")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(LecturerAvailability::class.java) }
        } catch (_: Exception) {
            firestore.collection("lecturer_availability")
                .snapshots()
                .map { it.toObjects(LecturerAvailability::class.java) }
        }
    }

    override fun getLecturerAvailabilities(lecturerUsername: String): Flow<List<LecturerAvailability>> {
        return firestore.collection("lecturer_availability")
            .whereEqualTo("lecturerUsername", lecturerUsername)
            .snapshots()
            .map { it.toObjects(LecturerAvailability::class.java) }
    }

    override suspend fun addAvailability(availability: LecturerAvailability) {
        val docRef = if (availability.id.isEmpty())
            firestore.collection("lecturer_availability").document()
        else
            firestore.collection("lecturer_availability").document(availability.id)
        docRef.set(availability.copy(id = docRef.id)).await()
    }

    override suspend fun updateAvailability(availability: LecturerAvailability) {
        firestore.collection("lecturer_availability").document(availability.id).set(availability).await()
    }

    // ── Sınav Takvimi ─────────────────────────────────────────

    override fun getExamEntries(): Flow<List<ExamEntry>> {
        return try {
            firestore.collection("exam_entries")
                .orderBy("examDate")
                .snapshots()
                .map { it.toObjects(ExamEntry::class.java) }
        } catch (_: Exception) {
            firestore.collection("exam_entries")
                .snapshots()
                .map { it.toObjects(ExamEntry::class.java) }
        }
    }

    override fun getExamEntriesByDepartment(department: String): Flow<List<ExamEntry>> {
        return firestore.collection("exam_entries")
            .whereEqualTo("department", department)
            .snapshots()
            .map { it.toObjects(ExamEntry::class.java) }
    }

    override suspend fun addExamEntry(entry: ExamEntry) {
        val docRef = if (entry.id.isEmpty())
            firestore.collection("exam_entries").document()
        else
            firestore.collection("exam_entries").document(entry.id)
        docRef.set(entry.copy(id = docRef.id)).await()
    }

    override suspend fun deleteExamEntry(id: String) {
        firestore.collection("exam_entries").document(id).delete().await()
    }

    // ── Yoklama Takibi ────────────────────────────────────────

    override fun getAttendanceRecords(): Flow<List<AttendanceRecord>> {
        return try {
            firestore.collection("attendance")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(AttendanceRecord::class.java) }
        } catch (_: Exception) {
            firestore.collection("attendance")
                .snapshots()
                .map { it.toObjects(AttendanceRecord::class.java) }
        }
    }

    override fun getAttendanceByCourse(courseCode: String): Flow<List<AttendanceRecord>> {
        return firestore.collection("attendance")
            .whereEqualTo("courseCode", courseCode)
            .snapshots()
            .map { it.toObjects(AttendanceRecord::class.java) }
    }

    override fun getAttendanceByLecturer(lecturerUsername: String): Flow<List<AttendanceRecord>> {
        return firestore.collection("attendance")
            .whereEqualTo("lecturerUsername", lecturerUsername)
            .snapshots()
            .map { it.toObjects(AttendanceRecord::class.java) }
    }

    override suspend fun saveAttendance(record: AttendanceRecord) {
        val docRef = if (record.id.isEmpty())
            firestore.collection("attendance").document()
        else
            firestore.collection("attendance").document(record.id)
        docRef.set(record.copy(id = docRef.id)).await()
    }

    override suspend fun updateAttendance(record: AttendanceRecord) {
        firestore.collection("attendance").document(record.id).set(record).await()
    }

    // ── Ödev Takibi ───────────────────────────────────────────

    override fun getAssignments(): Flow<List<AssignmentEntry>> {
        return firestore.collection("assignments")
            .snapshots()
            .map { it.toObjects(AssignmentEntry::class.java) }
    }

    override fun getAssignmentsByDepartment(department: String): Flow<List<AssignmentEntry>> {
        return firestore.collection("assignments")
            .whereEqualTo("department", department)
            .snapshots()
            .map { it.toObjects(AssignmentEntry::class.java) }
    }

    override fun getAssignmentsByLecturer(lecturerUsername: String): Flow<List<AssignmentEntry>> {
        return firestore.collection("assignments")
            .whereEqualTo("lecturerUsername", lecturerUsername)
            .snapshots()
            .map { it.toObjects(AssignmentEntry::class.java) }
    }

    override suspend fun addAssignment(assignment: AssignmentEntry) {
        val docRef = if (assignment.id.isEmpty())
            firestore.collection("assignments").document()
        else
            firestore.collection("assignments").document(assignment.id)
        docRef.set(assignment.copy(id = docRef.id, timestamp = System.currentTimeMillis())).await()
    }

    override suspend fun deleteAssignment(id: String) {
        firestore.collection("assignments").document(id).delete().await()
    }

    override fun getSubmissions(assignmentId: String): Flow<List<AssignmentSubmission>> {
        return firestore.collection("assignment_submissions")
            .whereEqualTo("assignmentId", assignmentId)
            .snapshots()
            .map { it.toObjects(AssignmentSubmission::class.java) }
    }

    override fun getSubmissionsByStudent(studentUsername: String): Flow<List<AssignmentSubmission>> {
        return firestore.collection("assignment_submissions")
            .whereEqualTo("studentUsername", studentUsername)
            .snapshots()
            .map { it.toObjects(AssignmentSubmission::class.java) }
    }

    override suspend fun submitAssignment(submission: AssignmentSubmission) {
        val docRef = if (submission.id.isEmpty())
            firestore.collection("assignment_submissions").document()
        else
            firestore.collection("assignment_submissions").document(submission.id)
        docRef.set(submission.copy(id = docRef.id, submittedAt = System.currentTimeMillis())).await()
    }

    // ── Akademik Takvim ───────────────────────────────────────

    override fun getAcademicEvents(): Flow<List<AcademicEvent>> {
        return firestore.collection("academic_events")
            .snapshots()
            .map { it.toObjects(AcademicEvent::class.java) }
    }

    override suspend fun addAcademicEvent(event: AcademicEvent) {
        val docRef = if (event.id.isEmpty())
            firestore.collection("academic_events").document()
        else
            firestore.collection("academic_events").document(event.id)
        docRef.set(event.copy(id = docRef.id, timestamp = System.currentTimeMillis())).await()
    }

    override suspend fun deleteAcademicEvent(id: String) {
        firestore.collection("academic_events").document(id).delete().await()
    }

    // ── Akademik Takvim PDF ──────────────────────────────────

    override suspend fun saveCalendarPdfUrl(url: String) {
        firestore.collection("settings").document("calendar_pdf")
            .set(mapOf("url" to url, "updatedAt" to System.currentTimeMillis())).await()
    }

    override suspend fun getCalendarPdfUrl(): String? {
        return try {
            firestore.collection("settings").document("calendar_pdf")
                .get().await().getString("url")
        } catch (_: Exception) { null }
    }

    // ── QR Yoklama Oturumları ─────────────────────────────────

    override fun getActiveSession(courseCode: String): Flow<AttendanceSession?> {
        return firestore.collection("attendance_sessions")
            .whereEqualTo("courseCode", courseCode)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(AttendanceSession::class.java)
                    .firstOrNull { it.expiresAt > System.currentTimeMillis() }
            }
    }

    override suspend fun createSession(session: AttendanceSession): AttendanceSession {
        val docRef = firestore.collection("attendance_sessions").document()
        val saved = session.copy(id = docRef.id)
        docRef.set(saved).await()
        return saved
    }

    override suspend fun updateSession(session: AttendanceSession) {
        firestore.collection("attendance_sessions").document(session.id).set(session).await()
    }

    override suspend fun getSessionByCode(sessionCode: String): AttendanceSession? {
        return try {
            firestore.collection("attendance_sessions")
                .whereEqualTo("sessionCode", sessionCode)
                .whereEqualTo("isActive", true)
                .get().await()
                .toObjects(AttendanceSession::class.java)
                .firstOrNull { it.expiresAt > System.currentTimeMillis() }
        } catch (_: Exception) { null }
    }

    override suspend fun addStudentToSession(sessionId: String, studentUsername: String) {
        firestore.collection("attendance_sessions").document(sessionId)
            .update("presentStudents", com.google.firebase.firestore.FieldValue.arrayUnion(studentUsername))
            .await()
    }

    override fun getActiveSessionsByDepartment(department: String): Flow<List<AttendanceSession>> {
        return firestore.collection("attendance_sessions")
            .whereEqualTo("department", department)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(AttendanceSession::class.java)
                    .filter { it.expiresAt > System.currentTimeMillis() }
            }
    }

    override suspend fun getSessionById(id: String): AttendanceSession? {
        return try {
            firestore.collection("attendance_sessions")
                .document(id).get().await()
                .toObject(AttendanceSession::class.java)
        } catch (_: Exception) { null }
    }

    override suspend fun addStudentToSessionWithMethod(
        sessionId: String, username: String, method: String
    ) {
        firestore.collection("attendance_sessions").document(sessionId)
            .update(mapOf(
                "presentStudents" to com.google.firebase.firestore.FieldValue.arrayUnion(username),
                "verificationMethods.$username" to method
            )).await()
    }

    // ── Telafi Dersi ─────────────────────────────────────────

    override fun getMakeupRequestsByDepartment(department: String): Flow<List<MakeupRequest>> =
        firestore.collection("makeup_requests")
            .whereEqualTo("department", department)
            .snapshots()
            .map { it.toObjects(MakeupRequest::class.java) }

    override fun getMakeupRequestsByLecturer(lecturerUsername: String): Flow<List<MakeupRequest>> =
        firestore.collection("makeup_requests")
            .whereEqualTo("lecturerUsername", lecturerUsername)
            .snapshots()
            .map { it.toObjects(MakeupRequest::class.java) }

    override suspend fun getMakeupRequestById(id: String): MakeupRequest? = try {
        firestore.collection("makeup_requests").document(id).get().await()
            .toObject(MakeupRequest::class.java)
    } catch (_: Exception) { null }

    override suspend fun saveMakeupRequest(request: MakeupRequest): MakeupRequest {
        return if (request.id.isBlank()) {
            val ref = firestore.collection("makeup_requests").document()
            val saved = request.copy(id = ref.id)
            ref.set(saved).await()
            saved
        } else {
            firestore.collection("makeup_requests").document(request.id).set(request).await()
            request
        }
    }

    override suspend fun updateMakeupRequest(request: MakeupRequest) {
        firestore.collection("makeup_requests").document(request.id).set(request).await()
    }

    override suspend fun voteForMakeupSlot(requestId: String, studentUsername: String, slotId: String) {
        firestore.collection("makeup_requests").document(requestId)
            .update("votes.$studentUsername", slotId).await()
    }

    // ── Akran Eşleştirme ─────────────────────────────────────

    override fun getPeerMatchesByDepartment(department: String): Flow<List<PeerMatch>> =
        firestore.collection("peer_matches")
            .whereEqualTo("department", department)
            .snapshots()
            .map { it.toObjects(PeerMatch::class.java) }

    override fun getPeerMatchesByStudent(username: String): Flow<List<PeerMatch>> =
        firestore.collection("peer_matches")
            .whereArrayContains("participants", username)
            .snapshots()
            .map { it.toObjects(PeerMatch::class.java) }

    override suspend fun savePeerMatch(match: PeerMatch): PeerMatch {
        return if (match.id.isBlank()) {
            val ref = firestore.collection("peer_matches").document()
            val saved = match.copy(
                id = ref.id,
                participants = listOf(match.mentorUsername, match.menteeUsername)
            )
            ref.set(saved).await()
            saved
        } else {
            firestore.collection("peer_matches").document(match.id).set(match).await()
            match
        }
    }

    override suspend fun updatePeerMatch(match: PeerMatch) {
        firestore.collection("peer_matches").document(match.id).set(match).await()
    }
}

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
        val batch = firestore.batch()
        docs.documents.forEach { batch.delete(it.reference) }
        if (docs.documents.isNotEmpty()) batch.commit().await()
    }

    override suspend fun clearClassrooms() {
        val docs = firestore.collection("classrooms").get().await()
        val batch = firestore.batch()
        docs.documents.forEach { batch.delete(it.reference) }
        if (docs.documents.isNotEmpty()) batch.commit().await()
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
}

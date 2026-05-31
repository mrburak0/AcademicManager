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

    // Not yönetimi
    fun getGrades(): Flow<List<GradeRecord>>
    fun getGradesByCourse(courseCode: String): Flow<List<GradeRecord>>
    fun getGradesByStudent(studentUsername: String): Flow<List<GradeRecord>>
    fun getGradesByLecturer(lecturerUsername: String): Flow<List<GradeRecord>>
    suspend fun saveGrade(grade: GradeRecord)
    suspend fun deleteGrade(gradeId: String)

    // Yoklama takibi
    fun getAttendanceRecords(): Flow<List<AttendanceRecord>>
    fun getAttendanceByCourse(courseCode: String): Flow<List<AttendanceRecord>>
    fun getAttendanceByLecturer(lecturerUsername: String): Flow<List<AttendanceRecord>>
    suspend fun saveAttendance(record: AttendanceRecord)
    suspend fun updateAttendance(record: AttendanceRecord)
}

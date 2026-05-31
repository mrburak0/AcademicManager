package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.*
import com.example.academicmanager.util.CredentialUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AssignmentResult {
    object Success : AssignmentResult()
    data class LecturerClash(val existing: ScheduleEntry) : AssignmentResult()
    data class ClassroomClash(val existing: ScheduleEntry) : AssignmentResult()
    data class CapacityWarning(val message: String) : AssignmentResult()
    data class Error(val message: String) : AssignmentResult()
}

class AdminViewModel(private val repository: UniversityRepository) : ViewModel() {

    val lecturers: StateFlow<List<Lecturer>> = repository.getLecturers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val courses: StateFlow<List<Course>> = repository.getCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val classrooms: StateFlow<List<Classroom>> = repository.getClassrooms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduleEntries: StateFlow<List<ScheduleEntry>> = repository.getScheduleEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Öğrenciler
    val students: StateFlow<List<Lecturer>> = repository.getStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Atanmamış öğretim görevlileri (real-time panel)
    val unassignedLecturers: StateFlow<List<Lecturer>> = combine(lecturers, scheduleEntries) { lects, entries ->
        val assignedNames = entries.map { it.lecturerName }.toSet()
        lects.filter { it.fullName !in assignedNames }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Atanmamış dersler (real-time panel)
    val unassignedCourses: StateFlow<List<Course>> = combine(courses, scheduleEntries) { crses, entries ->
        val assignedCodes = entries.map { it.courseCode }.toSet()
        crses.filter { it.courseCode !in assignedCodes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Ders Talep Yönetimi ───────────────────────────────────

    val scheduleRequests: StateFlow<List<ScheduleRequest>> = repository.getScheduleRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRequests: StateFlow<List<ScheduleRequest>> = scheduleRequests
        .map { list -> list.filter { it.status == RequestStatus.PENDING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Onay bekleyen kayıtlar
    val pendingRegistrations: StateFlow<List<Lecturer>> = repository.getPendingRegistrations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approveRegistration(lecturer: Lecturer) {
        viewModelScope.launch {
            try {
                repository.updateLecturer(lecturer.copy(status = AccountStatus.APPROVED))
            } catch (_: Exception) { }
        }
    }

    fun rejectRegistration(lecturer: Lecturer) {
        viewModelScope.launch {
            try {
                repository.deleteLecturer(lecturer.username)
            } catch (_: Exception) { }
        }
    }

    private val _assignmentResult = MutableSharedFlow<AssignmentResult>()
    val assignmentResult: SharedFlow<AssignmentResult> = _assignmentResult

    // ── Silme İşlemleri ───────────────────────────────────────

    fun deleteLecturer(username: String) {
        viewModelScope.launch {
            try {
                repository.deleteLecturer(username)
            } catch (_: Exception) { }
        }
    }

    fun resetLecturerPassword(lecturer: Lecturer, newPassword: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateLecturer(
                    lecturer.copy(
                        password = CredentialUtils.hashPassword(newPassword),
                        mustChangePassword = true
                    )
                )
                onResult(true)
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun deleteClassroom(classroomId: String) {
        viewModelScope.launch {
            try {
                repository.deleteClassroom(classroomId)
            } catch (_: Exception) { }
        }
    }

    fun deleteCourse(courseCode: String) {
        viewModelScope.launch {
            try {
                repository.deleteCourse(courseCode)
            } catch (_: Exception) { }
        }
    }

    // ── Talep Onay/Red İşlemleri ──────────────────────────────

    fun approveScheduleRequest(request: ScheduleRequest, adminNote: String = "") {
        viewModelScope.launch {
            try {
                repository.addScheduleEntry(
                    ScheduleEntry(
                        courseCode    = request.courseCode,
                        courseName    = request.courseName,
                        lecturerName  = request.lecturerName,
                        classroomName = request.proposedClassroom,
                        dayOfWeek     = request.proposedDay,
                        timeSlot      = request.proposedTimeSlot,
                        sessionType   = SessionType.LECTURE
                    )
                )
                repository.updateScheduleRequest(
                    request.copy(status = RequestStatus.APPROVED, adminNote = adminNote)
                )
            } catch (_: Exception) { }
        }
    }

    fun rejectScheduleRequest(request: ScheduleRequest, adminNote: String) {
        viewModelScope.launch {
            try {
                repository.updateScheduleRequest(
                    request.copy(status = RequestStatus.REJECTED, adminNote = adminNote)
                )
            } catch (_: Exception) { }
        }
    }

    fun addScheduleRequest(request: ScheduleRequest) {
        viewModelScope.launch {
            try {
                repository.addScheduleRequest(request)
            } catch (_: Exception) { }
        }
    }

    // ── Sınıf Yönetimi ────────────────────────────────────────

    fun addClassroom(name: String, capacity: Int, classroomType: String = ClassroomType.LECTURE) {
        viewModelScope.launch {
            try {
                repository.addClassroom(Classroom(name = name, capacity = capacity, classroomType = classroomType))
            } catch (_: Exception) { }
        }
    }

    // ── Program Silme ─────────────────────────────────────────

    fun deleteScheduleEntry(entryId: String) {
        viewModelScope.launch {
            try {
                repository.deleteScheduleEntry(entryId)
            } catch (_: Exception) { }
        }
    }

    // ── Ders Atama (Çakışma + Akıllı Sınıf Doğrulaması) ─────

    fun assignCourse(
        course: Course,
        lecturer: Lecturer,
        classroom: Classroom,
        day: String,
        timeSlot: String,
        sessionType: String = SessionType.LECTURE
    ) {
        viewModelScope.launch {
            val currentEntries = scheduleEntries.value

            // ÇAKIŞMA KONTROLÜ 1: Aynı öğretmen, aynı gün, aynı saat
            val lecturerClash = currentEntries.find {
                it.lecturerName == lecturer.fullName &&
                it.dayOfWeek == day &&
                it.timeSlot == timeSlot
            }
            if (lecturerClash != null) {
                _assignmentResult.emit(AssignmentResult.LecturerClash(lecturerClash))
                return@launch
            }

            // ÇAKIŞMA KONTROLÜ 2: Aynı sınıf, aynı gün, aynı saat
            val classroomClash = currentEntries.find {
                it.classroomName == classroom.name &&
                it.dayOfWeek == day &&
                it.timeSlot == timeSlot
            }
            if (classroomClash != null) {
                _assignmentResult.emit(AssignmentResult.ClassroomClash(classroomClash))
                return@launch
            }

            // KAPASİTE UYARI KONTROLÜ: Beklenen öğrenci sayısı > sınıf kapasitesi
            if (course.expectedStudents > 0 && course.expectedStudents > classroom.capacity) {
                _assignmentResult.emit(
                    AssignmentResult.CapacityWarning(
                        "Uyarı: ${course.courseName} için beklenen öğrenci sayısı " +
                        "(${course.expectedStudents}) ${classroom.name} kapasitesini " +
                        "(${classroom.capacity}) aşıyor!"
                    )
                )
                // Uyarı verilir ama atama durdurulmaz
            }

            // SINIF TİPİ UYUM KONTROLÜ
            val typeWarning = when {
                sessionType == SessionType.LAB &&
                classroom.classroomType == ClassroomType.LECTURE ->
                    "Uyarı: Lab seansı için derslik seçildi. Laboratuvar önerilir."
                sessionType == SessionType.LECTURE &&
                classroom.classroomType == ClassroomType.LAB ->
                    "Uyarı: Teorik ders için laboratuvar seçildi. Derslik önerilir."
                else -> null
            }
            if (typeWarning != null) {
                _assignmentResult.emit(AssignmentResult.CapacityWarning(typeWarning))
            }

            try {
                repository.addScheduleEntry(
                    ScheduleEntry(
                        courseCode    = course.courseCode,
                        courseName    = course.courseName,
                        lecturerName  = lecturer.fullName,
                        classroomName = classroom.name,
                        dayOfWeek     = day,
                        timeSlot      = timeSlot,
                        sessionType   = sessionType
                    )
                )
                _assignmentResult.emit(AssignmentResult.Success)
            } catch (e: Exception) {
                _assignmentResult.emit(AssignmentResult.Error(e.message ?: "Bilinmeyen hata"))
            }
        }
    }

    // ── Öğrenci Ekleme ────────────────────────────────────────

    fun addStudent(
        fullName: String,
        username: String,
        password: String,
        department: String,
        studentYear: String,
        studentId: String
    ) {
        viewModelScope.launch {
            try {
                repository.addStudent(
                    Lecturer(
                        username           = username,
                        password           = CredentialUtils.hashPassword(password),
                        fullName           = fullName,
                        department         = department,
                        role               = UserRole.STUDENT,
                        mustChangePassword = true,
                        studentYear        = studentYear,
                        studentId          = studentId
                    )
                )
            } catch (_: Exception) { }
        }
    }

    // ── Müsaitlik Haritası ────────────────────────────────────

    val availabilities: StateFlow<List<LecturerAvailability>> = repository.getAvailabilities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingAvailabilities: StateFlow<List<LecturerAvailability>> = availabilities
        .map { list -> list.filter { it.status == AvailabilityStatus.PENDING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun submitAvailability(
        lecturerUsername: String,
        lecturerName: String,
        slots: Map<String, List<String>>
    ) {
        viewModelScope.launch {
            try {
                repository.addAvailability(
                    LecturerAvailability(
                        lecturerUsername = lecturerUsername,
                        lecturerName     = lecturerName,
                        monday           = slots["Monday"]    ?: emptyList(),
                        tuesday          = slots["Tuesday"]   ?: emptyList(),
                        wednesday        = slots["Wednesday"] ?: emptyList(),
                        thursday         = slots["Thursday"]  ?: emptyList(),
                        friday           = slots["Friday"]    ?: emptyList()
                    )
                )
            } catch (_: Exception) { }
        }
    }

    // Hocanın kendi haritasını admin onayı olmadan direkt APPROVED olarak günceller
    fun updateOwnAvailability(
        lecturerUsername: String,
        lecturerName: String,
        slots: Map<String, List<String>>,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val existing = availabilities.value
                    .filter { it.lecturerUsername == lecturerUsername && it.status == AvailabilityStatus.APPROVED }
                    .maxByOrNull { it.timestamp }

                val updated = LecturerAvailability(
                    id               = existing?.id ?: "",
                    lecturerUsername = lecturerUsername,
                    lecturerName     = lecturerName,
                    monday           = slots["Monday"]    ?: emptyList(),
                    tuesday          = slots["Tuesday"]   ?: emptyList(),
                    wednesday        = slots["Wednesday"] ?: emptyList(),
                    thursday         = slots["Thursday"]  ?: emptyList(),
                    friday           = slots["Friday"]    ?: emptyList(),
                    status           = AvailabilityStatus.APPROVED
                )
                if (existing != null) {
                    repository.updateAvailability(updated)
                } else {
                    repository.addAvailability(updated)
                }
                onComplete(true)
            } catch (_: Exception) {
                onComplete(false)
            }
        }
    }

    fun approveAvailability(availability: LecturerAvailability) {
        viewModelScope.launch {
            try {
                repository.updateAvailability(
                    availability.copy(status = AvailabilityStatus.APPROVED)
                )
            } catch (_: Exception) { }
        }
    }

    fun rejectAvailability(availability: LecturerAvailability, adminNote: String = "") {
        viewModelScope.launch {
            try {
                repository.updateAvailability(
                    availability.copy(status = AvailabilityStatus.REJECTED, adminNote = adminNote)
                )
            } catch (_: Exception) { }
        }
    }

    // ── Demo Verisi ───────────────────────────────────────────

    fun seedDemoData(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                // 0. Temizle
                repository.clearScheduleEntries()
                repository.clearClassrooms()

                // 1. Sınıflar
                listOf(
                    Classroom(id = "A-101",  name = "A-101",  capacity = 60, classroomType = ClassroomType.LECTURE),
                    Classroom(id = "B-202",  name = "B-202",  capacity = 40, classroomType = ClassroomType.LECTURE),
                    Classroom(id = "C-303",  name = "C-303",  capacity = 30, classroomType = ClassroomType.LECTURE),
                    Classroom(id = "D-104",  name = "D-104",  capacity = 50, classroomType = ClassroomType.LECTURE),
                    Classroom(id = "Lab-1",  name = "Lab-1",  capacity = 25, classroomType = ClassroomType.LAB),
                    Classroom(id = "Lab-2",  name = "Lab-2",  capacity = 20, classroomType = ClassroomType.LAB),
                    Classroom(id = "BL-101", name = "BL-101", capacity = 30, classroomType = ClassroomType.COMPUTER_LAB)
                ).forEach { repository.addClassroom(it) }

                // 2. Dersler (bazıları atanmış, bazıları Yapay Zeka demosu için atanmamış)
                repository.addCourses(listOf(
                    Course(courseCode = "CS101",   courseName = "Programlamaya Giriş",      department = "Computer Science",        hasLab = true,  expectedStudents = 40, weeklyHours = 2, labHours = 2),
                    Course(courseCode = "CS201",   courseName = "Veri Yapıları",            department = "Computer Science",        hasLab = false, expectedStudents = 35, weeklyHours = 3),
                    Course(courseCode = "CS301",   courseName = "Algoritmalar",             department = "Computer Science",        hasLab = false, expectedStudents = 30, weeklyHours = 3),
                    Course(courseCode = "CS401",   courseName = "Yapay Zeka",               department = "Computer Science",        hasLab = true,  expectedStudents = 28, weeklyHours = 2, labHours = 2),
                    Course(courseCode = "SW101",   courseName = "Web Geliştirme",           department = "Software Engineering",    hasLab = true,  expectedStudents = 30, weeklyHours = 2, labHours = 2),
                    Course(courseCode = "SW201",   courseName = "Yazılım Mühendisliği",     department = "Software Engineering",    hasLab = false, expectedStudents = 35, weeklyHours = 3),
                    Course(courseCode = "MATH101", courseName = "Matematik I",              department = "Mathematics",             hasLab = false, expectedStudents = 55, weeklyHours = 4),
                    Course(courseCode = "MATH201", courseName = "Lineer Cebir",             department = "Mathematics",             hasLab = false, expectedStudents = 40, weeklyHours = 3),
                    Course(courseCode = "EE101",   courseName = "Devre Analizi",            department = "Electrical Engineering",  hasLab = true,  expectedStudents = 20, weeklyHours = 2, labHours = 2),
                    Course(courseCode = "EE201",   courseName = "Elektronik",               department = "Electrical Engineering",  hasLab = false, expectedStudents = 25, weeklyHours = 3)
                ))

                // 3. Öğretim görevlileri (5 hoca — çeşitli bölüm ve unvanlar)
                repository.addLecturers(listOf(
                    Lecturer(username = "ahmet_yilmaz",  password = CredentialUtils.hashPassword("ahmet123"),   fullName = "Ahmet Yılmaz",   title = "Dr.",       workingType = "Full-time", department = "Computer Science",       mustChangePassword = false, role = UserRole.LECTURER, status = AccountStatus.APPROVED),
                    Lecturer(username = "ayse_kaya",     password = CredentialUtils.hashPassword("ayse123"),    fullName = "Ayşe Kaya",      title = "Prof. Dr.", workingType = "Full-time", department = "Mathematics",             mustChangePassword = false, role = UserRole.LECTURER, status = AccountStatus.APPROVED),
                    Lecturer(username = "mehmet_demir",  password = CredentialUtils.hashPassword("mehmet123"),  fullName = "Mehmet Demir",   title = "Dr.",       workingType = "Part-time", department = "Electrical Engineering",  mustChangePassword = false, role = UserRole.LECTURER, status = AccountStatus.APPROVED),
                    Lecturer(username = "zeynep_arslan", password = CredentialUtils.hashPassword("zeynep123"),  fullName = "Zeynep Arslan",  title = "Dr.",       workingType = "Full-time", department = "Computer Science",        mustChangePassword = false, role = UserRole.LECTURER, status = AccountStatus.APPROVED),
                    Lecturer(username = "ali_celik",     password = CredentialUtils.hashPassword("ali456"),     fullName = "Ali Çelik",      title = "Öğr. Gör.", workingType = "Full-time", department = "Software Engineering",    mustChangePassword = false, role = UserRole.LECTURER, status = AccountStatus.APPROVED)
                ))

                // 4. Öğrenciler (3 farklı bölüm)
                listOf(
                    Lecturer(username = "ogrenci_ali",    password = CredentialUtils.hashPassword("ali123"),    fullName = "Ali Vural",    department = "Computer Science",       role = UserRole.STUDENT, mustChangePassword = false, studentYear = "1. Sınıf", studentId = "20230001", status = AccountStatus.APPROVED),
                    Lecturer(username = "ogrenci_fatma",  password = CredentialUtils.hashPassword("fatma123"),  fullName = "Fatma Öz",     department = "Software Engineering",   role = UserRole.STUDENT, mustChangePassword = false, studentYear = "2. Sınıf", studentId = "20220042", status = AccountStatus.APPROVED),
                    Lecturer(username = "ogrenci_burak",  password = CredentialUtils.hashPassword("burak123"),  fullName = "Burak Yıldız", department = "Electrical Engineering", role = UserRole.STUDENT, mustChangePassword = false, studentYear = "3. Sınıf", studentId = "20210087", status = AccountStatus.APPROVED)
                ).forEach { repository.addStudent(it) }

                // 5. Müsaitlik haritaları (APPROVED — deterministik id, re-seed'de üzerine yazar)
                // Ahmet Yılmaz: Pzt/Çar/Per
                repository.addAvailability(LecturerAvailability(
                    id = "avail_ahmet_yilmaz", lecturerUsername = "ahmet_yilmaz", lecturerName = "Ahmet Yılmaz",
                    monday    = listOf("09:00-10:00", "10:00-11:00", "13:00-14:00"),
                    wednesday = listOf("09:00-10:00", "10:00-11:00", "13:00-14:00"),
                    thursday  = listOf("10:00-11:00", "13:00-14:00", "14:00-15:00"),
                    status = AvailabilityStatus.APPROVED
                ))
                // Ayşe Kaya: Sal/Per/Cum
                repository.addAvailability(LecturerAvailability(
                    id = "avail_ayse_kaya", lecturerUsername = "ayse_kaya", lecturerName = "Ayşe Kaya",
                    tuesday   = listOf("08:00-09:00", "10:00-11:00", "11:00-12:00"),
                    thursday  = listOf("11:00-12:00", "13:00-14:00"),
                    friday    = listOf("09:00-10:00", "10:00-11:00"),
                    status = AvailabilityStatus.APPROVED
                ))
                // Mehmet Demir: Pzt/Çar (part-time)
                repository.addAvailability(LecturerAvailability(
                    id = "avail_mehmet_demir", lecturerUsername = "mehmet_demir", lecturerName = "Mehmet Demir",
                    monday    = listOf("14:00-15:00", "15:00-16:00"),
                    wednesday = listOf("15:00-16:00", "16:00-17:00"),
                    status = AvailabilityStatus.APPROVED
                ))
                // Zeynep Arslan: Pzt/Sal/Cum
                repository.addAvailability(LecturerAvailability(
                    id = "avail_zeynep_arslan", lecturerUsername = "zeynep_arslan", lecturerName = "Zeynep Arslan",
                    monday    = listOf("09:00-10:00", "10:00-11:00"),
                    tuesday   = listOf("13:00-14:00", "14:00-15:00"),
                    friday    = listOf("09:00-10:00", "10:00-11:00", "11:00-12:00"),
                    status = AvailabilityStatus.APPROVED
                ))
                // Ali Çelik: Sal/Çar/Per
                repository.addAvailability(LecturerAvailability(
                    id = "avail_ali_celik", lecturerUsername = "ali_celik", lecturerName = "Ali Çelik",
                    tuesday   = listOf("09:00-10:00", "10:00-11:00"),
                    wednesday = listOf("10:00-11:00", "11:00-12:00", "13:00-14:00"),
                    thursday  = listOf("09:00-10:00", "14:00-15:00"),
                    status = AvailabilityStatus.APPROVED
                ))

                // 6. Program (yalnızca 4 ders atanmış — geri kalan 6 ders Yapay Zeka demosu için bırakıldı)
                listOf(
                    ScheduleEntry(id = "CS101_Mon_0900_LECTURE",   courseCode = "CS101",   courseName = "Programlamaya Giriş", lecturerName = "Ahmet Yılmaz", classroomName = "A-101",  dayOfWeek = "Monday",    timeSlot = "09:00-10:00", sessionType = SessionType.LECTURE),
                    ScheduleEntry(id = "CS101_Thu_1000_LAB",       courseCode = "CS101",   courseName = "Programlamaya Giriş Lab", lecturerName = "Ahmet Yılmaz", classroomName = "BL-101", dayOfWeek = "Thursday", timeSlot = "10:00-11:00", sessionType = SessionType.LAB),
                    ScheduleEntry(id = "MATH101_Tue_0800_LECTURE", courseCode = "MATH101", courseName = "Matematik I",        lecturerName = "Ayşe Kaya",    classroomName = "A-101",  dayOfWeek = "Tuesday",   timeSlot = "08:00-09:00", sessionType = SessionType.LECTURE),
                    ScheduleEntry(id = "EE101_Mon_1400_LECTURE",   courseCode = "EE101",   courseName = "Devre Analizi",      lecturerName = "Mehmet Demir", classroomName = "D-104",  dayOfWeek = "Monday",    timeSlot = "14:00-15:00", sessionType = SessionType.LECTURE),
                    ScheduleEntry(id = "EE101_Wed_1500_LAB",       courseCode = "EE101",   courseName = "Devre Analizi Lab",  lecturerName = "Mehmet Demir", classroomName = "Lab-1",  dayOfWeek = "Wednesday", timeSlot = "15:00-16:00", sessionType = SessionType.LAB)
                ).forEach { repository.addScheduleEntry(it) }

                // 7. Sınav Takvimi demo verileri
                listOf(
                    com.example.academicmanager.data.ExamEntry(id = "exam_cs101_midterm", courseCode = "CS101", courseName = "Programlamaya Giriş", department = "Computer Science", lecturerName = "Ahmet Yılmaz", examDate = "2026-06-10", startTime = "09:00", endTime = "11:00", classroom = "A-101", examType = "MIDTERM", notes = "Kitap kapalı, hesap makinesi yok"),
                    com.example.academicmanager.data.ExamEntry(id = "exam_math101_midterm", courseCode = "MATH101", courseName = "Matematik I", department = "Mathematics", lecturerName = "Ayşe Kaya", examDate = "2026-06-12", startTime = "13:00", endTime = "15:00", classroom = "B-202", examType = "MIDTERM", notes = "Formül kağıdı getirilebilir"),
                    com.example.academicmanager.data.ExamEntry(id = "exam_ee101_midterm", courseCode = "EE101", courseName = "Devre Analizi", department = "Electrical Engineering", lecturerName = "Mehmet Demir", examDate = "2026-06-14", startTime = "10:00", endTime = "12:00", classroom = "D-104", examType = "MIDTERM", notes = ""),
                    com.example.academicmanager.data.ExamEntry(id = "exam_cs101_final", courseCode = "CS101", courseName = "Programlamaya Giriş", department = "Computer Science", lecturerName = "Ahmet Yılmaz", examDate = "2026-07-08", startTime = "09:00", endTime = "12:00", classroom = "A-101", examType = "FINAL", notes = ""),
                    com.example.academicmanager.data.ExamEntry(id = "exam_math101_final", courseCode = "MATH101", courseName = "Matematik I", department = "Mathematics", lecturerName = "Ayşe Kaya", examDate = "2026-07-10", startTime = "13:00", endTime = "16:00", classroom = "B-202", examType = "FINAL", notes = "Tüm konular"),
                    com.example.academicmanager.data.ExamEntry(id = "exam_ee101_makeup", courseCode = "EE101", courseName = "Devre Analizi", department = "Electrical Engineering", lecturerName = "Mehmet Demir", examDate = "2026-07-22", startTime = "10:00", endTime = "12:00", classroom = "C-303", examType = "MAKEUP", notes = "Bütünleme")
                ).forEach { repository.addExamEntry(it) }

                // 8. Ödev demo verileri
                val now = System.currentTimeMillis()
                listOf(
                    com.example.academicmanager.data.AssignmentEntry(courseCode = "CS101", courseName = "Programlamaya Giriş", department = "Computer Science", lecturerUsername = "ahmet_yilmaz", lecturerName = "Ahmet Yılmaz", title = "Ödev 1: Temel Algoritmalar", description = "Sıralama ve arama algoritmalarını Python ile kodlayın. Bubble sort, selection sort ve binary search implement edin.", dueDate = "2026-06-15", dueTime = "23:59", maxPoints = 100, timestamp = now),
                    com.example.academicmanager.data.AssignmentEntry(courseCode = "CS101", courseName = "Programlamaya Giriş", department = "Computer Science", lecturerUsername = "ahmet_yilmaz", lecturerName = "Ahmet Yılmaz", title = "Ödev 2: Veri Yapıları", description = "Stack ve Queue veri yapılarını sıfırdan implement edin ve test edin.", dueDate = "2026-06-28", dueTime = "23:59", maxPoints = 100, timestamp = now),
                    com.example.academicmanager.data.AssignmentEntry(courseCode = "MATH101", courseName = "Matematik I", department = "Mathematics", lecturerUsername = "ayse_kaya", lecturerName = "Ayşe Kaya", title = "Problem Seti 1", description = "Sayfa 45-60 arası tüm alıştırmalar. Türev ve integral işlemleri.", dueDate = "2026-06-18", dueTime = "18:00", maxPoints = 50, timestamp = now),
                    com.example.academicmanager.data.AssignmentEntry(courseCode = "EE101", courseName = "Devre Analizi", department = "Electrical Engineering", lecturerUsername = "mehmet_demir", lecturerName = "Mehmet Demir", title = "Lab Raporu 1: RC Devreleri", description = "RC devresi deneyi raporu. Ölçüm grafikleri ve hesaplamalar dahil edilmeli.", dueDate = "2026-06-20", dueTime = "17:00", maxPoints = 80, timestamp = now)
                ).forEach { repository.addAssignment(it) }

                // ── Atanmamış bırakılan dersler (Yapay Zeka demo için) ──
                // CS201, CS301, CS401, SW101, SW201, MATH201, EE201
                // Zeynep Arslan ve Ali Çelik de atanmamış hoca olarak görünür

                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    // ── Otomatik Ders Atama ───────────────────────────────────

    data class AutoAssignProposal(
        val entry: ScheduleEntry,
        val course: Course,
        val included: Boolean = true
    )

    sealed class AutoAssignState {
        object Idle : AutoAssignState()
        object Computing : AutoAssignState()
        data class Ready(
            val proposals: List<AutoAssignProposal>,
            val unassigned: List<Course>,
            val warnings: List<String> = emptyList()
        ) : AutoAssignState()
        object Saving : AutoAssignState()
        data class Done(val saved: Int, val failed: Int) : AutoAssignState()
    }

    private val _autoAssignState = MutableStateFlow<AutoAssignState>(AutoAssignState.Idle)
    val autoAssignState: StateFlow<AutoAssignState> = _autoAssignState

    fun runAutoAssign() {
        viewModelScope.launch(Dispatchers.Default) {
            _autoAssignState.value = AutoAssignState.Computing

            val allCourses      = courses.value
            val allLecturers    = lecturers.value
            val allAvails       = availabilities.value.filter { it.status == AvailabilityStatus.APPROVED }
            val allClassrooms   = classrooms.value
            val existingEntries = scheduleEntries.value

            // Teorik seansı henüz atanmamış dersler
            val unassignedCourses = allCourses.filter { c ->
                existingEntries.none { it.courseCode == c.courseCode && it.sessionType == SessionType.LECTURE }
            }

            _autoAssignState.value = computeAutoAssign(
                unassignedCourses = unassignedCourses,
                allLecturers      = allLecturers,
                availabilities    = allAvails,
                classrooms        = allClassrooms,
                existingEntries   = existingEntries
            )
        }
    }

    fun toggleProposal(index: Int) {
        val current = _autoAssignState.value as? AutoAssignState.Ready ?: return
        val updated = current.proposals.toMutableList().apply {
            this[index] = this[index].copy(included = !this[index].included)
        }
        _autoAssignState.value = current.copy(proposals = updated)
    }

    fun selectAllProposals(select: Boolean) {
        val current = _autoAssignState.value as? AutoAssignState.Ready ?: return
        _autoAssignState.value = current.copy(
            proposals = current.proposals.map { it.copy(included = select) }
        )
    }

    fun confirmAutoAssign() {
        val current = _autoAssignState.value as? AutoAssignState.Ready ?: return
        val toSave = current.proposals.filter { it.included }
        viewModelScope.launch {
            _autoAssignState.value = AutoAssignState.Saving
            var saved = 0; var failed = 0
            toSave.forEach { proposal ->
                try { repository.addScheduleEntry(proposal.entry); saved++ }
                catch (_: Exception) { failed++ }
            }
            _autoAssignState.value = AutoAssignState.Done(saved, failed)
        }
    }

    fun resetAutoAssign() { _autoAssignState.value = AutoAssignState.Idle }

    private fun computeAutoAssign(
        unassignedCourses: List<Course>,
        allLecturers:      List<Lecturer>,
        availabilities:    List<LecturerAvailability>,
        classrooms:        List<Classroom>,
        existingEntries:   List<ScheduleEntry>
    ): AutoAssignState.Ready {

        val DAYS  = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        val SLOTS = listOf(
            "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00",
            "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00"
        )

        // Mevcut doluluk haritaları
        val lecturerOccupied  = mutableMapOf<String, MutableSet<String>>()
        val classroomOccupied = mutableMapOf<String, MutableSet<String>>()
        existingEntries.forEach { e ->
            lecturerOccupied .getOrPut(e.lecturerName)  { mutableSetOf() }.add("${e.dayOfWeek}|${e.timeSlot}")
            classroomOccupied.getOrPut(e.classroomName) { mutableSetOf() }.add("${e.dayOfWeek}|${e.timeSlot}")
        }

        // Müsaitlik haritası: username → liste<(gün, saat)>
        val availMap = mutableMapOf<String, List<Pair<String, String>>>()
        availabilities.forEach { avail ->
            val pairs = DAYS.flatMap { day ->
                // Saat sıralamasını SLOTS sırasına göre koru
                SLOTS.filter { slot -> slot in avail.slotsForDay(day) }.map { day to it }
            }
            if (pairs.isNotEmpty()) availMap[avail.lecturerUsername] = pairs
        }

        // Ders yükü takibi (yük dengeleme)
        val lecturerLoad = mutableMapOf<String, Int>()
        existingEntries.forEach { e ->
            lecturerLoad[e.lecturerName] = (lecturerLoad[e.lecturerName] ?: 0) + 1
        }

        val proposals  = mutableListOf<AutoAssignProposal>()
        val unassigned = mutableListOf<Course>()
        val warnings   = mutableListOf<String>()

        unassignedCourses.forEach { course ->
            // Aday sıralama: 1) aynı bölüm, 2) diğer bölüm — her iki grupta yük sıralı
            val sameDept = allLecturers
                .filter { it.department.equals(course.department, ignoreCase = true) && availMap.containsKey(it.username) }
                .sortedBy { lecturerLoad[it.fullName] ?: 0 }
            val otherDept = allLecturers
                .filter { !it.department.equals(course.department, ignoreCase = true) && availMap.containsKey(it.username) }
                .sortedBy { lecturerLoad[it.fullName] ?: 0 }
            val candidates = sameDept + otherDept

            var lectureAssigned = false

            outer@ for (lecturer in candidates) {
                val availSlots = availMap[lecturer.username] ?: continue

                for ((day, slot) in availSlots) {
                    val key = "$day|$slot"
                    if (lecturerOccupied[lecturer.fullName]?.contains(key) == true) continue

                    val lectureRoom = findBestClassroom(
                        classrooms, classroomOccupied, key,
                        expectedType = ClassroomType.LECTURE,
                        minCapacity  = course.expectedStudents
                    ) ?: continue

                    val entryId = "${course.courseCode}_${day}_${slot.replace(":", "")}_LECTURE"
                    proposals.add(AutoAssignProposal(
                        entry = ScheduleEntry(
                            id            = entryId,
                            courseCode    = course.courseCode,
                            courseName    = course.courseName,
                            lecturerName  = lecturer.fullName,
                            classroomName = lectureRoom.name,
                            dayOfWeek     = day,
                            timeSlot      = slot,
                            sessionType   = SessionType.LECTURE
                        ),
                        course = course
                    ))
                    lecturerOccupied .getOrPut(lecturer.fullName) { mutableSetOf() }.add(key)
                    classroomOccupied.getOrPut(lectureRoom.name)  { mutableSetOf() }.add(key)
                    lecturerLoad[lecturer.fullName] = (lecturerLoad[lecturer.fullName] ?: 0) + 1
                    lectureAssigned = true

                    // Lab seansı
                    if (course.hasLab) {
                        var labFound = false
                        for ((labDay, labSlot) in availSlots) {
                            val labKey = "$labDay|$labSlot"
                            if (labKey == key) continue
                            if (lecturerOccupied[lecturer.fullName]?.contains(labKey) == true) continue

                            val labRoom = findBestClassroom(classrooms, classroomOccupied, labKey, ClassroomType.LAB, course.expectedStudents)
                                ?: findBestClassroom(classrooms, classroomOccupied, labKey, ClassroomType.COMPUTER_LAB, course.expectedStudents)
                                ?: continue

                            val labId = "${course.courseCode}_${labDay}_${labSlot.replace(":", "")}_LAB"
                            proposals.add(AutoAssignProposal(
                                entry = ScheduleEntry(
                                    id            = labId,
                                    courseCode    = course.courseCode,
                                    courseName    = "${course.courseName} Lab",
                                    lecturerName  = lecturer.fullName,
                                    classroomName = labRoom.name,
                                    dayOfWeek     = labDay,
                                    timeSlot      = labSlot,
                                    sessionType   = SessionType.LAB
                                ),
                                course = course
                            ))
                            lecturerOccupied .getOrPut(lecturer.fullName) { mutableSetOf() }.add(labKey)
                            classroomOccupied.getOrPut(labRoom.name)       { mutableSetOf() }.add(labKey)
                            labFound = true
                            break
                        }
                        if (!labFound) warnings.add("${course.courseName}: uygun lab saati/sınıfı bulunamadı.")
                    }
                    break@outer
                }
            }

            if (!lectureAssigned) unassigned.add(course)
        }

        return AutoAssignState.Ready(proposals, unassigned, warnings)
    }

    private fun findBestClassroom(
        classrooms: List<Classroom>,
        occupied: MutableMap<String, MutableSet<String>>,
        key: String,
        expectedType: String,
        minCapacity: Int
    ): Classroom? {
        val free = classrooms.filter {
            it.classroomType == expectedType && occupied[it.name]?.contains(key) != true
        }
        val needed = minCapacity.coerceAtLeast(1)
        return free.filter { it.capacity >= needed }.minByOrNull { it.capacity }
            ?: free.maxByOrNull { it.capacity }  // en büyük mevcut oda (sığmasa da)
    }
}

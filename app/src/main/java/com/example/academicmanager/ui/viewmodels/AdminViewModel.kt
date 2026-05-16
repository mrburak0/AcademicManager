package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.*
import com.example.academicmanager.util.CredentialUtils
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
                // 0. Mevcut program ve sınıf kayıtlarını temizle (duplicate önleme)
                repository.clearScheduleEntries()
                repository.clearClassrooms()

                // 1. Sınıflar — id=name kullanılır, tekrar seed'de üzerine yazar
                listOf(
                    Classroom(id = "A-101",  name = "A-101",  capacity = 60,  classroomType = ClassroomType.LECTURE),
                    Classroom(id = "B-202",  name = "B-202",  capacity = 40,  classroomType = ClassroomType.LECTURE),
                    Classroom(id = "C-303",  name = "C-303",  capacity = 30,  classroomType = ClassroomType.LECTURE),
                    Classroom(id = "Lab-1",  name = "Lab-1",  capacity = 25,  classroomType = ClassroomType.LAB),
                    Classroom(id = "BL-101", name = "BL-101", capacity = 30,  classroomType = ClassroomType.COMPUTER_LAB)
                ).forEach { repository.addClassroom(it) }

                // 2. Dersler — hasLab ve expectedStudents eklendi
                repository.addCourses(listOf(
                    Course(courseCode = "CS101",   courseName = "Introduction to Programming", department = "Computer Science", hasLab = true,  expectedStudents = 40, weeklyHours = 2, labHours = 2),
                    Course(courseCode = "CS201",   courseName = "Data Structures",             department = "Computer Science", hasLab = false, expectedStudents = 35, weeklyHours = 3),
                    Course(courseCode = "CS301",   courseName = "Algorithms",                  department = "Computer Science", hasLab = false, expectedStudents = 30, weeklyHours = 3),
                    Course(courseCode = "MATH101", courseName = "Calculus I",                  department = "Mathematics",      hasLab = false, expectedStudents = 55, weeklyHours = 4),
                    Course(courseCode = "MATH201", courseName = "Linear Algebra",              department = "Mathematics",      hasLab = false, expectedStudents = 40, weeklyHours = 3),
                    Course(courseCode = "EE101",   courseName = "Circuit Analysis",            department = "Electrical Engineering", hasLab = true, expectedStudents = 20, weeklyHours = 2, labHours = 2)
                ))

                // 3. Öğretim görevlileri
                repository.addLecturers(listOf(
                    Lecturer(username = "ahmet_yilmaz", password = CredentialUtils.hashPassword("ahmet123"),  fullName = "Ahmet Yılmaz",  title = "Dr.",       workingType = "Full-time", department = "Computer Science",       mustChangePassword = false, role = UserRole.LECTURER),
                    Lecturer(username = "ayse_kaya",    password = CredentialUtils.hashPassword("ayse123"),   fullName = "Ayşe Kaya",    title = "Prof. Dr.", workingType = "Full-time", department = "Mathematics",            mustChangePassword = false, role = UserRole.LECTURER),
                    Lecturer(username = "mehmet_demir", password = CredentialUtils.hashPassword("mehmet123"), fullName = "Mehmet Demir", title = "Dr.",       workingType = "Part-time", department = "Electrical Engineering", mustChangePassword = false, role = UserRole.LECTURER)
                ))

                // 3b. Demo öğrenci
                repository.addStudent(
                    Lecturer(username = "ogrenci_ali", password = CredentialUtils.hashPassword("ali123"), fullName = "Ali Vural", department = "Computer Science", role = UserRole.STUDENT, mustChangePassword = false, studentYear = "1. Sınıf", studentId = "20230001")
                )

                // 4. Program girişleri — id deterministik: tekrar seed'de üzerine yazar
                listOf(
                    ScheduleEntry(id = "CS101_Monday_09:00-10:00_LECTURE",    courseCode = "CS101",   courseName = "Introduction to Programming", lecturerName = "Ahmet Yılmaz",  classroomName = "A-101",  dayOfWeek = "Monday",   timeSlot = "09:00-10:00", sessionType = SessionType.LECTURE),
                    ScheduleEntry(id = "CS201_Wednesday_10:00-11:00_LECTURE", courseCode = "CS201",   courseName = "Data Structures",             lecturerName = "Ahmet Yılmaz",  classroomName = "B-202",  dayOfWeek = "Wednesday", timeSlot = "10:00-11:00", sessionType = SessionType.LECTURE),
                    ScheduleEntry(id = "CS301_Friday_13:00-14:00_LECTURE",    courseCode = "CS301",   courseName = "Algorithms",                  lecturerName = "Ahmet Yılmaz",  classroomName = "A-101",  dayOfWeek = "Friday",    timeSlot = "13:00-14:00", sessionType = SessionType.LECTURE),
                    ScheduleEntry(id = "MATH101_Tuesday_08:00-09:00_LECTURE", courseCode = "MATH101", courseName = "Calculus I",                  lecturerName = "Ayşe Kaya",    classroomName = "C-303",  dayOfWeek = "Tuesday",   timeSlot = "08:00-09:00", sessionType = SessionType.LECTURE),
                    ScheduleEntry(id = "MATH201_Thursday_11:00-12:00_LECTURE",courseCode = "MATH201", courseName = "Linear Algebra",              lecturerName = "Ayşe Kaya",    classroomName = "B-202",  dayOfWeek = "Thursday",  timeSlot = "11:00-12:00", sessionType = SessionType.LECTURE),
                    ScheduleEntry(id = "EE101_Monday_14:00-15:00_LECTURE",    courseCode = "EE101",   courseName = "Circuit Analysis",            lecturerName = "Mehmet Demir", classroomName = "A-101",  dayOfWeek = "Monday",    timeSlot = "14:00-15:00", sessionType = SessionType.LECTURE),
                    ScheduleEntry(id = "EE101_Wednesday_15:00-16:00_LAB",     courseCode = "EE101",   courseName = "Circuit Analysis Lab",        lecturerName = "Mehmet Demir", classroomName = "Lab-1",  dayOfWeek = "Wednesday", timeSlot = "15:00-16:00", sessionType = SessionType.LAB),
                    ScheduleEntry(id = "CS101_Thursday_10:00-11:00_LAB",      courseCode = "CS101",   courseName = "Introduction to Prog. Lab",   lecturerName = "Ahmet Yılmaz",  classroomName = "BL-101", dayOfWeek = "Thursday",  timeSlot = "10:00-11:00", sessionType = SessionType.LAB)
                ).forEach { repository.addScheduleEntry(it) }

                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}

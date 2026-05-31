# AcademicManager — Project Report
**Course:** Mobile Programming  
**Date:** 16.05.2026  
**Developer:** Burak Aslan

---

## 1. Project Overview

AcademicManager is an Android application that enables lecturers, students, and administrators at university campuses to manage course schedules, announcements, and classroom assignments in real time.

---

## 2. Technologies Used

| Layer | Technology |
|-------|-----------|
| Language | **Kotlin** |
| UI Framework | **Jetpack Compose** + Material 3 |
| Database | **Firebase Firestore** (NoSQL, real-time) |
| Architecture | **MVVM** (Model-View-ViewModel) |
| Async | **Kotlin Coroutines** + **Flow** |
| Excel Processing | **Apache POI** |
| Dependency Injection | Manual (ViewModelFactory) |
| Navigation | **Jetpack Navigation Compose** |
| Image Loading | **Coil** |
| Build System | **Gradle KTS** |

---

## 3. APIs Used

### 3.1 Firebase Firestore SDK
```kotlin
// Real-time listener — converting to Flow
override fun getScheduleEntries(): Flow<List<ScheduleEntry>> {
    return firestore.collection("schedule_entries")
        .snapshots()                              // Firestore real-time stream
        .map { it.toObjects(ScheduleEntry::class.java) }
}
```
- `collection().snapshots()` → real-time sync across devices (~2 seconds)
- `batch().commit()` → atomic bulk write operations
- `whereEqualTo()` → role-based queries

### 3.2 Kotlin Coroutines & Flow
```kotlin
// UI state management with StateFlow
val scheduleEntries: StateFlow<List<ScheduleEntry>> =
    repository.getScheduleEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
```

### 3.3 Apache POI (Excel)
```kotlin
// Reading lecturer list from Excel file
val workbook = XSSFWorkbook(inputStream)
val sheet = workbook.getSheetAt(0)
for (i in 1..sheet.lastRowNum) {
    val row = sheet.getRow(i) ?: continue
    val name = row.getCell(0).safeString()
    val username = CredentialUtils.generateUsername(name, title)
    val password = CredentialUtils.generatePassword()
    items.add(Lecturer(username = username, password = password, ...))
}
```

### 3.4 MediaStore API (Downloads)
```kotlin
// Saving template Excel file to Downloads folder
val contentValues = ContentValues().apply {
    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
}
context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
```

### 3.5 ClipboardManager API
```kotlin
// Copying lecturer credentials to clipboard
val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
cm.setPrimaryClip(ClipData.newPlainText("credentials", text))
```

---

## 4. Architecture — MVVM

```
UI (Compose Screens)
    ↕  collectAsState()
ViewModel (StateFlow / SharedFlow)
    ↕  suspend fun / Flow
Repository Interface
    ↕
RepositoryImpl (Firestore)
```

### State Management with Sealed Classes
```kotlin
sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class PreviewReady(val items: List<Any>, val type: ImportType) : ImportState()
    data class Success(val message: String) : ImportState()
    data class Error(val message: String) : ImportState()
    data class CredentialSheet(val credentials: List<Pair<String, String>>) : ImportState()
}
```

### Role-Based Navigation
```kotlin
when (user.role) {
    UserRole.ADMIN    -> AdminNavGraph(navController, adminViewModel)
    UserRole.LECTURER -> LecturerNavGraph(navController, adminViewModel)
    UserRole.STUDENT  -> StudentNavGraph(navController, adminViewModel)
}
```

---

## 5. Highlighted Kotlin Features

### 5.1 Dynamic Password Strength Indicator
```kotlin
@Composable
fun PasswordStrengthRow(password: String) {
    if (password.isEmpty()) return
    val hasMinLength = password.length >= 6
    val hasUppercase = password.any { it.isUpperCase() }
    val hasLowercase = password.any { it.isLowerCase() }
    val hasDigit     = password.any { it.isDigit() }
    // Color-coded chip for each condition...
}
```

### 5.2 Clash-Detected Course Assignment
```kotlin
fun assignCourse(course: Course, lecturer: Lecturer, classroom: Classroom, day: String, timeSlot: String) {
    viewModelScope.launch {
        val currentEntries = scheduleEntries.value

        // Lecturer clash check
        val lecturerClash = currentEntries.find {
            it.lecturerName == lecturer.fullName &&
            it.dayOfWeek == day &&
            it.timeSlot == timeSlot
        }
        if (lecturerClash != null) {
            _assignmentResult.emit(AssignmentResult.LecturerClash(lecturerClash))
            return@launch
        }

        // Classroom clash check
        val classroomClash = currentEntries.find {
            it.classroomName == classroom.name &&
            it.dayOfWeek == day &&
            it.timeSlot == timeSlot
        }
        if (classroomClash != null) {
            _assignmentResult.emit(AssignmentResult.ClassroomClash(classroomClash))
            return@launch
        }

        repository.addScheduleEntry(ScheduleEntry(...))
        _assignmentResult.emit(AssignmentResult.Success)
    }
}
```

### 5.3 SHA-256 Password Hashing
```kotlin
fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
    return hashBytes.joinToString("") { "%02x".format(it) }
}
```

### 5.4 Automatic Username Generation
```kotlin
fun generateUsername(fullName: String, title: String): String {
    val normalized = fullName
        .lowercase()
        .replace('ğ', 'g').replace('ü', 'u').replace('ş', 's')
        .replace('ı', 'i').replace('ö', 'o').replace('ç', 'c')
    val parts = normalized.trim().split(" ")
    return parts.joinToString("_") { it.filter { c -> c.isLetterOrDigit() } }
}
// "Ahmet Yılmaz" → "ahmet_yilmaz"
```

### 5.5 Input Validation Filters
```kotlin
// Full name: letters and spaces only (blocks numbers)
onValueChange = { studentFullName = it.filter { c -> c.isLetter() || c.isWhitespace() || c == '-' } }

// Username: alphanumeric + underscore, auto-lowercase
onValueChange = { studentUsername = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase() }

// Capacity: digits only, validated 1-2000 on save
onValueChange = { capacityText = it.filter { c -> c.isDigit() } }
```

---

## 6. Firestore Data Model

```
academicmanagement-778f6 (Firebase Project)
├── lecturers/             {username} → Lecturer (role: ADMIN/LECTURER/STUDENT)
├── courses/               {courseCode} → Course
├── classrooms/            {name} → Classroom
├── schedule_entries/      {courseCode_day_time_type} → ScheduleEntry
├── schedule_requests/     {autoId} → ScheduleRequest (PENDING/APPROVED/REJECTED)
├── announcements/         {autoId} → Announcement
└── lecturer_availability/ {autoId} → LecturerAvailability
```

---

## 7. Test Results

| Test | Result |
|------|--------|
| Admin login & dashboard | ✅ |
| Lecturer login & calendar | ✅ |
| Student login & schedule | ✅ |
| Excel import & credential sheet | ✅ |
| Clash detection | ✅ |
| Password strength indicator | ✅ |
| Input validation | ✅ |
| Real-time Firestore sync | ✅ ~2 seconds |
| Turkish character support | ✅ |

---

## 8. Screenshots

| # | Screen |
|---|--------|
| 01 | Login Screen |
| 02 | Admin Dashboard |
| 03 | Admin — Classroom Management |
| 04 | Admin — Schedule Assignment |
| 05 | Lecturer Home |
| 06 | Lecturer Calendar |
| 07 | Announcements (Real-time sync) |
| 08 | Data Import |
| 09 | Student Home |
| 10 | Password Strength Indicator |

> Screenshots are located in the `ekranlar/` folder.

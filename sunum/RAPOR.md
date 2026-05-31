# AcademicManager — Proje Raporu
**Ders:** Mobile Programming  
**Tarih:** 16.05.2026  
**Geliştirici:** Burak Aslan

---

## 1. Proje Özeti

AcademicManager, üniversite kampüslerinde öğretim görevlisi, öğrenci ve yöneticilerin ders programlarını, duyuruları ve sınıf atamalarını gerçek zamanlı olarak yönetmesine olanak tanıyan bir Android uygulamasıdır.

---

## 2. Kullanılan Teknolojiler

| Katman | Teknoloji |
|--------|-----------|
| Dil | **Kotlin** |
| UI Framework | **Jetpack Compose** + Material 3 |
| Veritabanı | **Firebase Firestore** (NoSQL, real-time) |
| Mimari | **MVVM** (Model-View-ViewModel) |
| Async | **Kotlin Coroutines** + **Flow** |
| Excel İşleme | **Apache POI** |
| Dependency Injection | Manuel (ViewModelFactory) |
| Navigasyon | **Jetpack Navigation Compose** |
| Görsel Yükleme | **Coil** |
| Build | **Gradle KTS** |

---

## 3. Kullanılan API'ler

### 3.1 Firebase Firestore SDK
```kotlin
// Real-time listener — Flow'a dönüştürme
override fun getScheduleEntries(): Flow<List<ScheduleEntry>> {
    return firestore.collection("schedule_entries")
        .snapshots()                              // Firestore real-time stream
        .map { it.toObjects(ScheduleEntry::class.java) }
}
```
- `collection().snapshots()` → cihazlar arası anlık sync (~2 saniye)
- `batch().commit()` → atomik toplu yazma işlemleri
- `whereEqualTo()` → rol bazlı sorgular

### 3.2 Kotlin Coroutines & Flow
```kotlin
// StateFlow ile UI state yönetimi
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
// Excel dosyasından hoca listesi okuma
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
// Şablon Excel dosyasını İndirilenler klasörüne kaydetme
val contentValues = ContentValues().apply {
    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
}
context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
```

### 3.5 ClipboardManager API
```kotlin
// Hoca şifrelerini panoya kopyalama
val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
cm.setPrimaryClip(ClipData.newPlainText("credentials", text))
```

---

## 4. Mimari Yapı — MVVM

```
UI (Compose Screens)
    ↕  collectAsState()
ViewModel (StateFlow / SharedFlow)
    ↕  suspend fun / Flow
Repository Interface
    ↕
RepositoryImpl (Firestore)
```

### Sealed Class ile State Yönetimi
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

### Rol Bazlı Navigasyon
```kotlin
when (user.role) {
    UserRole.ADMIN    -> AdminNavGraph(navController, adminViewModel)
    UserRole.LECTURER -> LecturerNavGraph(navController, adminViewModel)
    UserRole.STUDENT  -> StudentNavGraph(navController, adminViewModel)
}
```

---

## 5. Öne Çıkan Kotlin Özellikleri

### 5.1 Dinamik Şifre Güç Göstergesi
```kotlin
@Composable
fun PasswordStrengthRow(password: String) {
    if (password.isEmpty()) return
    val hasMinLength = password.length >= 6
    val hasUppercase = password.any { it.isUpperCase() }
    val hasLowercase = password.any { it.isLowerCase() }
    val hasDigit     = password.any { it.isDigit() }
    // Her koşul için renkli chip gösterimi...
}
```

### 5.2 Çakışma Kontrollü Ders Atama
```kotlin
fun assignCourse(course: Course, lecturer: Lecturer, classroom: Classroom, day: String, timeSlot: String) {
    viewModelScope.launch {
        val currentEntries = scheduleEntries.value

        // Öğretmen çakışması kontrolü
        val lecturerClash = currentEntries.find {
            it.lecturerName == lecturer.fullName &&
            it.dayOfWeek == day &&
            it.timeSlot == timeSlot
        }
        if (lecturerClash != null) {
            _assignmentResult.emit(AssignmentResult.LecturerClash(lecturerClash))
            return@launch
        }

        // Sınıf çakışması kontrolü
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

### 5.3 SHA-256 Şifre Hashing
```kotlin
fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
    return hashBytes.joinToString("") { "%02x".format(it) }
}
```

### 5.4 Otomatik Kullanıcı Adı Üretimi
```kotlin
fun generateUsername(fullName: String, title: String): String {
    val normalized = fullName
        .lowercase()
        .replace('ğ', 'g').replace('ü', 'u').replace('ş', 's')
        .replace('ı', 'i').replace('ö', 'o').replace('ç', 'c')
    val parts = normalized.trim().split(" ")
    return parts.joinToString("_") { it.filter { c -> c.isLetterOrDigit() } }
}
```

### 5.5 Input Validasyon Filtreleri
```kotlin
// Sadece harf ve boşluk (ad-soyad için)
onValueChange = { studentFullName = it.filter { c -> c.isLetter() || c.isWhitespace() || c == '-' } }

// Sadece alfanumerik + alt çizgi (kullanıcı adı)
onValueChange = { studentUsername = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase() }

// Sadece rakam (kapasite, öğrenci no)
onValueChange = { capacityText = it.filter { c -> c.isDigit() } }
```

---

## 6. Firestore Veri Modeli

```
academicmanagement-778f6 (Firebase Project)
├── lecturers/          {username} → Lecturer (role: ADMIN/LECTURER/STUDENT)
├── courses/            {courseCode} → Course
├── classrooms/         {name} → Classroom
├── schedule_entries/   {courseCode_day_time_type} → ScheduleEntry
├── schedule_requests/  {autoId} → ScheduleRequest (PENDING/APPROVED/REJECTED)
├── announcements/      {autoId} → Announcement
└── lecturer_availability/ {autoId} → LecturerAvailability
```

---

## 7. Test Sonuçları

| Test | Sonuç |
|------|-------|
| Admin girişi & dashboard | ✅ |
| Hoca girişi & takvim | ✅ |
| Öğrenci girişi & program | ✅ |
| Excel import & credential sheet | ✅ |
| Çakışma kontrolü | ✅ |
| Şifre güç göstergesi | ✅ |
| Input validasyon | ✅ |
| Real-time Firestore sync | ✅ ~2 saniye |
| Türkçe karakter desteği | ✅ |

---

## 8. Ekran Görüntüleri

| # | Ekran |
|---|-------|
| 01 | Giriş Ekranı |
| 02 | Admin Dashboard |
| 03 | Admin — Sınıf Yönetimi |
| 04 | Admin — Program Atama |
| 05 | Hoca Ana Sayfa |
| 06 | Hoca Takvim |
| 07 | Duyurular (Real-time sync) |
| 08 | Veri İçe Aktarma |
| 09 | Öğrenci Ana Sayfa |
| 10 | Şifre Güç Göstergesi |

> Ekran görüntüleri `ekranlar/` klasöründe bulunmaktadır.

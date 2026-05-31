# AcademicManager — Presentation Notes
**Mobile Programming | Kotlin & Android**

---

## SLIDE 1 — Project Introduction

**AcademicManager**
> "Bringing university management to your pocket"

- Android application — Kotlin + Jetpack Compose
- 3 distinct user roles: Admin · Lecturer · Student
- Real-time Firebase Firestore backend
- Bulk data import via Excel

📱 *[Show: 01_giris_ekrani.png]*

---

## SLIDE 2 — Technology Stack

```
┌─────────────────────────────────────┐
│       JETPACK COMPOSE (UI)          │
│         Material 3 Design           │
├─────────────────────────────────────┤
│   MVVM Architecture │ StateFlow/Flow │
├─────────────────────────────────────┤
│     Kotlin Coroutines (async)        │
├─────────────────────────────────────┤
│   FIREBASE FIRESTORE (real-time)     │
├─────────────────────────────────────┤
│   Apache POI  │  MediaStore API      │
└─────────────────────────────────────┘
```

**Why this stack?**
- Compose → 100% Kotlin UI, no XML at all
- Firestore → Real-time sync without managing a server
- Coroutines → Clean async code, no callback hell

---

## SLIDE 3 — Architecture: MVVM + Repository

```kotlin
// ViewModel — business logic independent from UI
val scheduleEntries: StateFlow<List<ScheduleEntry>> =
    repository.getScheduleEntries()
        .stateIn(viewModelScope,
                 SharingStarted.WhileSubscribed(5000),
                 emptyList())

// Compose UI — observes state, auto-recomposes
val entries by viewModel.scheduleEntries.collectAsState()
```

**Benefit:** Screen rotation, backgrounding → no data loss

📱 *[Show: 02_admin_dashboard.png]*

---

## SLIDE 4 — Firebase Real-Time Sync

```kotlin
// One line — instant updates on all devices
firestore.collection("announcements")
    .snapshots()
    .map { it.toObjects(Announcement::class.java) }
```

**How it works:**
1. Admin adds an announcement → writes to Firestore
2. Firestore → pushes to all connected devices
3. Lecturer/Student screen → **updates in ~2 seconds**

📱 *[Show: 07_duyurular.png — the "SYNC TEST" announcement]*

---

## SLIDE 5 — Excel Import + Security

```kotlin
// SHA-256 password hashing — plaintext never stored
fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
    return hashBytes.joinToString("") { "%02x".format(it) }
}

// Automatic username generation
// "Ahmet Yılmaz" + "Dr." → "ahmet_yilmaz"
fun generateUsername(fullName: String, title: String): String {
    return fullName.lowercase()
        .replace('ğ','g').replace('ü','u').replace('ş','s')
        .replace('ı','i').replace('ö','o').replace('ç','c')
        .trim().split(" ").joinToString("_")
}
```

- Plain text passwords **never** reach the database
- Admin sees them only once → CredentialSheet screen

📱 *[Show: 08_admin_import.png]*

---

## SLIDE 6 — Sealed Classes for State Management

```kotlin
sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class PreviewReady(val items: List<Any>, val type: ImportType) : ImportState()
    data class Success(val message: String) : ImportState()
    data class Error(val message: String) : ImportState()
    data class CredentialSheet(val credentials: List<Pair<String, String>>) : ImportState()
}

// UI — exhaustive when, compile-time guarantee
when (val s = state) {
    is ImportState.Idle           -> ImportIdleScreen(...)
    is ImportState.Loading        -> CircularProgressIndicator()
    is ImportState.PreviewReady   -> ImportPreviewScreen(state = s, ...)
    is ImportState.CredentialSheet -> CredentialSheetScreen(credentials = s.credentials)
    // Compiler error if any branch is missing
}
```

**Kotlin power:** Exhaustive `when` → runtime crashes impossible

---

## SLIDE 7 — Clash Detection

```kotlin
// Same lecturer cannot teach two classes at the same time
val lecturerClash = currentEntries.find {
    it.lecturerName == lecturer.fullName &&
    it.dayOfWeek == day &&
    it.timeSlot == timeSlot
}
if (lecturerClash != null) {
    _assignmentResult.emit(AssignmentResult.LecturerClash(lecturerClash))
    return@launch   // Early exit — Kotlin idiom
}
```

- Lecturer clash → red warning dialog
- Classroom clash → yellow warning dialog
- Capacity exceeded → info message

📱 *[Show: 04_admin_program.png]*

---

## SLIDE 8 — Compose UI: Dynamic Password Check

```kotlin
@Composable
fun PasswordStrengthRow(password: String) {
    if (password.isEmpty()) return          // hide when empty
    Row {
        StrengthChip("6+ chars",     password.length >= 6)
        StrengthChip("Uppercase",    password.any { it.isUpperCase() })
    }
    Row {
        StrengthChip("Lowercase",    password.any { it.isLowerCase() })
        StrengthChip("Digit",        password.any { it.isDigit() })
    }
}
```

- Updates **in real-time** as the user types
- Green `CheckCircle` when met, gray when not

📱 *[Show: 10_sifre_guc_gostergesi.png]*

---

## SLIDE 9 — Input Validation

```kotlin
// Full name: letters and spaces only (digits blocked)
onValueChange = {
    studentFullName = it.filter { c -> c.isLetter() || c.isWhitespace() }
}

// Username: alphanumeric + underscore, auto-lowercase
onValueChange = {
    studentUsername = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase()
}

// Capacity: digits only, range 1-2000 enforced on save
onValueChange = { capacityText = it.filter { c -> c.isDigit() } }
if (parsedCapacity < 1 || parsedCapacity > 2000) → Toast warning
```

**Kotlin lambda filter:** Powerful one-line validation

---

## SLIDE 10 — Roles & Features

| Feature | Admin | Lecturer | Student |
|---------|:-----:|:--------:|:-------:|
| Dashboard & statistics | ✅ | ✅ | ✅ |
| View course schedule | ✅ | ✅ | ✅ |
| Classroom management | ✅ | — | — |
| Add lecturers/students | ✅ | — | — |
| Excel import | ✅ | — | — |
| Create schedule request | — | ✅ | — |
| Send availability map | — | ✅ | — |
| Approve/reject requests | ✅ | — | — |
| Create announcements | ✅ | — | — |
| PDF export | — | ✅ | — |

📱 *[Show: 05_hoca_anasayfa.png + 09_ogrenci_anasayfa.png]*

---

## SLIDE 11 — Live Demo

1. **Admin** → Dashboard → Add classroom → Assign course
2. **Lecturer** → Calendar → PDF export → Create schedule request
3. **Student** → View schedule
4. **Real-time:** Admin adds announcement → appears on Lecturer screen instantly

---

## SLIDE 12 — Summary

✅ **Kotlin** — Modern, safe, expressive syntax  
✅ **Jetpack Compose** — Fully declarative UI  
✅ **Firebase Firestore** — Serverless real-time database  
✅ **MVVM + Flow** — Testable, scalable architecture  
✅ **Apache POI** — Excel read/write  
✅ **SHA-256** — Secure password storage  

> "Every single screen is written in Kotlin — not one line of XML."

---

*Questions: mrburak.aslan@gmail.com*  
*GitHub: github.com/mrburak0/AcademicManager*

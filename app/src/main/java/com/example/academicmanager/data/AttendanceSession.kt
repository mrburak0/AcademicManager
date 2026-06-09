package com.example.academicmanager.data

data class AttendanceSession(
    val id                  : String  = "",
    val sessionCode         : String  = "",  // BLE beacon payload (8 chars)
    val sessionSecret       : String  = "",  // HMAC-SHA256 gizli anahtarı (dönen QR için)
    val courseCode          : String  = "",
    val courseName          : String  = "",
    val lecturerUsername    : String  = "",
    val lecturerName        : String  = "",
    val department          : String  = "",
    val sessionDate         : String  = "",  // yyyy-MM-dd
    val dayOfWeek           : String  = "",
    val timeSlot            : String  = "",
    val sessionType         : String  = "",
    val createdAt           : Long    = 0L,
    val expiresAt           : Long    = 0L,
    val durationMinutes     : Int     = 15,
    val isActive            : Boolean = true,
    val presentStudents     : List<String>           = emptyList(),
    val verificationMethods : Map<String, String>    = emptyMap()  // username → BLE | QR | MANUAL
)

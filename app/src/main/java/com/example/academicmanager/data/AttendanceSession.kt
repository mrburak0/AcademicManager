package com.example.academicmanager.data

data class AttendanceSession(
    val id               : String = "",
    val sessionCode      : String = "",   // QR içeriği ve BLE beacon verisi
    val courseCode       : String = "",
    val courseName       : String = "",
    val lecturerUsername : String = "",
    val lecturerName     : String = "",
    val department       : String = "",
    val sessionDate      : String = "",   // yyyy-MM-dd
    val dayOfWeek        : String = "",
    val timeSlot         : String = "",
    val sessionType      : String = "",
    val createdAt        : Long   = 0L,
    val expiresAt        : Long   = 0L,   // createdAt + 15 dakika
    val isActive         : Boolean = true,
    val presentStudents  : List<String> = emptyList()  // username listesi
)

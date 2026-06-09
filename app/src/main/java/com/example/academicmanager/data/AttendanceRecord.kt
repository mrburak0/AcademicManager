package com.example.academicmanager.data

data class AttendanceRecord(
    val id                  : String = "",
    val courseCode          : String = "",
    val courseName          : String = "",
    val department          : String = "",
    val lecturerUsername    : String = "",
    val sessionDate         : String = "",
    val dayOfWeek           : String = "",
    val timeSlot            : String = "",
    val sessionType         : String = SessionType.LECTURE,
    val presentStudents     : List<String>           = emptyList(),
    val absentStudents      : List<String>           = emptyList(),
    val totalStudents       : Int    = 0,
    val timestamp           : Long   = 0L,
    val verificationMethods : Map<String, String>    = emptyMap()  // username → BLE | QR | MANUAL
)

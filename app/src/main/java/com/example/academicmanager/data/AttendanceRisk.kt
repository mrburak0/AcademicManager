package com.example.academicmanager.data

data class AttendanceRisk(
    val studentUsername: String,
    val studentName: String,
    val studentId: String,
    val courseCode: String,
    val courseName: String,
    val lecturerUsername: String,
    val department: String,
    val attendedCount: Int,
    val totalSessions: Int,
    val percentage: Float,
    val level: RiskLevel,
    val consecutiveMissed: Int,  // missed streak at end of last N sessions
    val lastSeenDate: String     // yyyy-MM-dd or "—"
)

enum class RiskLevel { WARNING, CRITICAL }

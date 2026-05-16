package com.example.academicmanager.data

// Seans tipi sabitleri
object SessionType {
    const val LECTURE = "LECTURE"  // Teorik ders
    const val LAB     = "LAB"      // Laboratuvar seansı

    fun displayName(type: String): String = when (type) {
        LAB  -> "Lab"
        else -> "Teorik"
    }
}

data class ScheduleEntry(
    val id: String = "",
    val courseCode: String = "",
    val courseName: String = "",
    val lecturerName: String = "",
    val classroomName: String = "",
    val dayOfWeek: String = "",
    val timeSlot: String = "",
    val sessionType: String = SessionType.LECTURE
)

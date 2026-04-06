package com.example.academicmanager.data

data class ScheduleEntry(
    val id: String = "",
    val courseCode: String = "",
    val courseName: String = "",
    val lecturerName: String = "",
    val classroomName: String = "",
    val dayOfWeek: String = "",
    val timeSlot: String = ""
)

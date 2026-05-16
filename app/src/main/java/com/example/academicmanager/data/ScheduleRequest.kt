package com.example.academicmanager.data

object RequestStatus {
    const val PENDING  = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

data class ScheduleRequest(
    val id: String = "",
    val courseCode: String = "",
    val courseName: String = "",
    val lecturerUsername: String = "",
    val lecturerName: String = "",
    val proposedDay: String = "",
    val proposedTimeSlot: String = "",
    val proposedClassroom: String = "",
    val lecturerNote: String = "",
    val adminNote: String = "",
    val status: String = RequestStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)

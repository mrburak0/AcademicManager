package com.example.academicmanager.data

data class AssignmentEntry(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val courseCode: String = "",
    val courseName: String = "",
    val department: String = "",
    val lecturerUsername: String = "",
    val lecturerName: String = "",
    val dueDate: String = "",
    val dueTime: String = "23:59",
    val maxPoints: Int = 100,
    val timestamp: Long = 0L
)

data class AssignmentSubmission(
    val id: String = "",
    val assignmentId: String = "",
    val studentUsername: String = "",
    val studentName: String = "",
    val department: String = "",
    val note: String = "",
    val submittedAt: Long = 0L,
    val isLate: Boolean = false
)

package com.example.academicmanager.data

data class Lecturer(
    val username: String = "",
    val password: String = "",
    val fullName: String = "",
    val title: String = "",
    val workingType: String = "",
    val department: String = "General",
    val mustChangePassword: Boolean = true,
    val role: UserRole = UserRole.LECTURER,
    val profilePicturePath: String? = null
)

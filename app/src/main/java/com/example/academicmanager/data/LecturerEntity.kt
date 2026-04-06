package com.example.academicmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lecturers")
data class LecturerEntity(
    @PrimaryKey
    val fullName: String,
    val title: String,
    val workingType: String,
    val username: String = "",
    val password: String = "",
    val department: String = "General"
)

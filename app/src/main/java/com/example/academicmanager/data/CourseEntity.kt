package com.example.academicmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey
    val courseCode: String,
    val courseName: String,
    val lecturerName: String,
    val department: String,
    val dayOfWeek: String, // e.g., "Monday"
    val timeSlot: String   // e.g., "Morning" or "Afternoon"
)

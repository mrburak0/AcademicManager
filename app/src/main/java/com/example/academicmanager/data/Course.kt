package com.example.academicmanager.data

data class Course(
    val courseCode: String = "",
    val courseName: String = "",
    val department: String = "",
    // Lab + Teorik eşleştirmesi için alanlar
    val hasLab: Boolean = false,
    val weeklyHours: Int = 2,
    val labHours: Int = 0,
    // Akıllı sınıf seçimi için beklenen öğrenci sayısı
    val expectedStudents: Int = 0
)

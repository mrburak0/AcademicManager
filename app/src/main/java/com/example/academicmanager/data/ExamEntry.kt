package com.example.academicmanager.data

object ExamType {
    const val MIDTERM = "MIDTERM"
    const val FINAL   = "FINAL"
    const val MAKEUP  = "MAKEUP"

    fun displayName(type: String) = when (type) {
        MIDTERM -> "Vize"
        FINAL   -> "Final"
        MAKEUP  -> "Bütünleme"
        else    -> type
    }

    // ARGB long — matches GradeRecord.letterColor pattern
    fun color(type: String): Long = when (type) {
        MIDTERM -> 0xFF6366F1 // Indigo
        FINAL   -> 0xFFEF4444 // Red
        MAKEUP  -> 0xFFF59E0B // Amber
        else    -> 0xFF94A3B8 // Slate
    }

    val all = listOf(MIDTERM, FINAL, MAKEUP)
}

data class ExamEntry(
    val id: String = "",
    val courseCode: String = "",
    val courseName: String = "",
    val department: String = "",
    val lecturerName: String = "",
    val examDate: String = "",      // "2025-06-10"
    val startTime: String = "",     // "09:00"
    val endTime: String = "",       // "11:00"
    val classroom: String = "",
    val examType: String = ExamType.FINAL,
    val notes: String = "",
    val timestamp: Long = 0L
)

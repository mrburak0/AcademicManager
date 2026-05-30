package com.example.academicmanager.data

data class GradeRecord(
    val id: String = "",
    val studentUsername: String = "",
    val studentName: String = "",
    val courseCode: String = "",
    val courseName: String = "",
    val department: String = "",
    val lecturerUsername: String = "",
    val midterm: Float = -1f,
    val finalExam: Float = -1f,
    val assignment: Float = -1f,
    val lab: Float = -1f,
    val hasLab: Boolean = false,
    val letterGrade: String = "",
    val gpa: Float = -1f,
    val timestamp: Long = 0L
) {
    companion object {
        fun calculateAverage(midterm: Float, finalExam: Float, assignment: Float, lab: Float, hasLab: Boolean): Float {
            val m = midterm.coerceIn(0f, 100f)
            val f = finalExam.coerceIn(0f, 100f)
            val a = assignment.coerceIn(0f, 100f)
            return if (hasLab && lab >= 0) {
                val l = lab.coerceIn(0f, 100f)
                m * 0.25f + a * 0.10f + l * 0.15f + f * 0.50f
            } else {
                m * 0.30f + a * 0.10f + f * 0.60f
            }
        }

        fun calculateLetterGrade(average: Float): String = when {
            average >= 90 -> "AA"
            average >= 85 -> "BA"
            average >= 75 -> "BB"
            average >= 65 -> "CB"
            average >= 60 -> "CC"
            average >= 55 -> "DC"
            average >= 50 -> "DD"
            else          -> "FF"
        }

        fun letterToGpa(letter: String): Float = when (letter) {
            "AA" -> 4.0f
            "BA" -> 3.5f
            "BB" -> 3.0f
            "CB" -> 2.5f
            "CC" -> 2.0f
            "DC" -> 1.5f
            "DD" -> 1.0f
            else -> 0.0f
        }

        fun letterColor(letter: String): Long = when {
            letter == "AA" || letter == "BA" -> 0xFF10B981 // Green
            letter == "BB" || letter == "CB" -> 0xFF6366F1 // Indigo
            letter == "CC"                   -> 0xFFF59E0B // Amber
            letter == "DC" || letter == "DD" -> 0xFFEF8C44 // Orange
            else                             -> 0xFFEF4444 // Red (FF)
        }
    }
}

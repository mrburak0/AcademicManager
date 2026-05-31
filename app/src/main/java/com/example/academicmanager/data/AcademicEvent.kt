package com.example.academicmanager.data

object EventType {
    const val SEMESTER_START = "SEMESTER_START"
    const val SEMESTER_END   = "SEMESTER_END"
    const val HOLIDAY        = "HOLIDAY"
    const val EXAM_WEEK      = "EXAM_WEEK"
    const val REGISTRATION   = "REGISTRATION"
    const val OTHER          = "OTHER"

    fun displayName(type: String) = when (type) {
        SEMESTER_START -> "Dönem Başlangıcı"
        SEMESTER_END   -> "Dönem Sonu"
        HOLIDAY        -> "Tatil"
        EXAM_WEEK      -> "Sınav Haftası"
        REGISTRATION   -> "Kayıt Dönemi"
        else           -> "Diğer"
    }

    fun color(type: String): Long = when (type) {
        SEMESTER_START -> 0xFF10B981L
        SEMESTER_END   -> 0xFF6366F1L
        HOLIDAY        -> 0xFFF59E0BL
        EXAM_WEEK      -> 0xFFEF4444L
        REGISTRATION   -> 0xFF3B82F6L
        else           -> 0xFF94A3B8L
    }

    val all = listOf(SEMESTER_START, SEMESTER_END, HOLIDAY, EXAM_WEEK, REGISTRATION, OTHER)
}

data class AcademicEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val eventType: String = EventType.OTHER,
    val timestamp: Long = 0L
)

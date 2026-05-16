package com.example.academicmanager.data

// Duyuru tipi sabitleri
object AnnouncementType {
    const val INFO            = "INFO"
    const val WARNING         = "WARNING"
    const val CANCELLED       = "CANCELLED"
    const val SCHEDULE_CHANGE = "SCHEDULE_CHANGE"

    fun displayName(type: String): String = when (type) {
        INFO            -> "Bilgi"
        WARNING         -> "Uyarı"
        CANCELLED       -> "İptal"
        SCHEDULE_CHANGE -> "Program Değişikliği"
        else            -> type
    }

    val all = listOf(INFO, WARNING, CANCELLED, SCHEDULE_CHANGE)
}

data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = AnnouncementType.INFO,
    val timestamp: Long = 0L,
    val targetRole: String = "ALL",           // ALL, LECTURER, STUDENT
    val relatedCourseCode: String = "",
    val createdBy: String = "admin"
)

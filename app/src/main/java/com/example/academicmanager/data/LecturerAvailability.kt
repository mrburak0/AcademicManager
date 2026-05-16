package com.example.academicmanager.data

object AvailabilityStatus {
    const val PENDING  = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

data class LecturerAvailability(
    val id: String = "",
    val lecturerUsername: String = "",
    val lecturerName: String = "",
    val monday: List<String> = emptyList(),
    val tuesday: List<String> = emptyList(),
    val wednesday: List<String> = emptyList(),
    val thursday: List<String> = emptyList(),
    val friday: List<String> = emptyList(),
    val status: String = AvailabilityStatus.PENDING,
    val adminNote: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun slotsForDay(englishDay: String): List<String> = when (englishDay) {
        "Monday"    -> monday
        "Tuesday"   -> tuesday
        "Wednesday" -> wednesday
        "Thursday"  -> thursday
        "Friday"    -> friday
        else        -> emptyList()
    }

    val totalSlots: Int get() = monday.size + tuesday.size + wednesday.size + thursday.size + friday.size
}

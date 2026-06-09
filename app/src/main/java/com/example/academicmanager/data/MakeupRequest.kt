package com.example.academicmanager.data

data class MakeupRequest(
    val id: String = "",
    val originalCourseCode: String = "",
    val courseName: String = "",
    val department: String = "",
    val lecturerUsername: String = "",
    val lecturerName: String = "",
    val cancelledDayOfWeek: String = "",   // English: Monday…Friday
    val cancelledTimeSlot: String = "",    // HH:mm-HH:mm
    val cancelReason: String = "",
    val proposedSlots: List<MakeupSlot> = emptyList(),
    val votes: Map<String, String> = emptyMap(), // studentUsername → slotId
    val status: String = MakeupStatus.VOTING,
    val confirmedSlotId: String = "",
    val createdAt: Long = 0L,
    val voteDeadline: Long = 0L              // epoch ms — 48h from creation
)

data class MakeupSlot(
    val id: String = "",
    val date: String = "",           // yyyy-MM-dd
    val dayOfWeek: String = "",      // English
    val timeSlot: String = "",       // HH:mm-HH:mm
    val classroomName: String = "",
    val conflictCount: Int = 0       // # dept students with clashing class
)

object MakeupStatus {
    const val VOTING    = "VOTING"
    const val CONFIRMED = "CONFIRMED"
    const val CANCELLED = "CANCELLED"
}

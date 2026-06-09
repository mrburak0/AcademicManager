package com.example.academicmanager.data

data class PeerMatch(
    val id: String = "",
    val courseCode: String = "",
    val courseName: String = "",
    val department: String = "",
    val mentorUsername: String = "",
    val menteeUsername: String = "",
    // arrayContains query için — mentorUsername + menteeUsername
    val participants: List<String> = emptyList(),
    val status: String = PeerMatchStatus.PENDING,
    val mentorAccepted: Boolean = false,
    val menteeAccepted: Boolean = false,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L             // 14 gün sonra otomatik sona erer
)

object PeerMatchStatus {
    const val PENDING  = "PENDING"   // İkisi de henüz cevaplamadı
    const val ACTIVE   = "ACTIVE"    // İkisi de kabul etti
    const val DECLINED = "DECLINED"  // Biri reddetti
    const val EXPIRED  = "EXPIRED"
}

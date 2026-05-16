package com.example.academicmanager.data

// Hesap durum sabitleri
object AccountStatus {
    const val PENDING  = "PENDING"   // Admin onayı bekleniyor
    const val APPROVED = "APPROVED"  // Giriş yapabilir
}

// Admin kayıt kodu — bu kodu bilen kişi admin olarak kaydolup direkt giriş yapabilir
const val ADMIN_REGISTRATION_CODE = "CAMPUS2024"

data class Lecturer(
    val username: String = "",
    val password: String = "",
    val fullName: String = "",
    val title: String = "",
    val workingType: String = "",
    val department: String = "General",
    val mustChangePassword: Boolean = true,
    val role: UserRole = UserRole.LECTURER,
    val profilePicturePath: String? = null,
    val studentYear: String = "",
    val studentId: String = "",
    val status: String = AccountStatus.APPROVED  // Mevcut kullanıcılar default APPROVED
)

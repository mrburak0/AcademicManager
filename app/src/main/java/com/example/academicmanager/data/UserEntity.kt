package com.example.academicmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole { ADMIN, LECTURER }
enum class UserStatus { PENDING, APPROVED }

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val password: String,
    val fullName: String,
    val role: UserRole,
    val status: UserStatus = UserStatus.PENDING,
    val department: String = "",
    val profilePicturePath: String? = null
)

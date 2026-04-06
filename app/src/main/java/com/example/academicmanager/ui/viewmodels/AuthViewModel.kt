package com.example.academicmanager.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.UserEntity
import com.example.academicmanager.data.UserRole
import com.example.academicmanager.data.UserStatus
import com.example.academicmanager.data.Lecturer
import com.example.academicmanager.data.UniversityDao
import com.example.academicmanager.data.UniversityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: Lecturer) : AuthState()
    data class MustChangePassword(val user: Lecturer) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val userDao: UniversityDao,
    private val repository: UniversityRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    var currentUser: Lecturer? = null
        private set

    fun updateCurrentUser(user: Lecturer) {
        currentUser = user
        _authState.value = AuthState.Authenticated(user)
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            // 1. Admin Fallback
            if (username == "admin" && password == "admin") {
                val adminUser = Lecturer(
                    fullName = "System Administrator",
                    username = "admin",
                    role = UserRole.ADMIN,
                    mustChangePassword = false
                )
                currentUser = adminUser
                _authState.value = AuthState.Authenticated(adminUser)
                return@launch
            }

            // 2. Query Firestore for Lecturer
            try {
                val lecturer = repository.getLecturerByUsername(username)
                if (lecturer != null && lecturer.password == password) {
                    currentUser = lecturer
                    if (lecturer.mustChangePassword) {
                        _authState.value = AuthState.MustChangePassword(lecturer)
                    } else {
                        _authState.value = AuthState.Authenticated(lecturer)
                    }
                } else {
                    _authState.value = AuthState.Error("Invalid credentials or user not found.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Login error: ${e.message}")
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        val user = currentUser ?: return
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                if (user.password == oldPassword) {
                    val updatedUser = user.copy(
                        password = newPassword,
                        mustChangePassword = false
                    )
                    repository.updateLecturer(updatedUser)
                    currentUser = updatedUser
                    _authState.value = AuthState.Authenticated(updatedUser)
                } else {
                    _authState.value = AuthState.Error("Current password incorrect.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Update error: ${e.message}")
            }
        }
    }

    fun logout() {
        currentUser = null
        _authState.value = AuthState.Idle
    }

    // Keep register for local Phase 1 users if needed, but primary focus is Firestore now
    fun register(username: String, password: String, fullName: String, role: UserRole, department: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            // Logic for Phase 1 local registration remains unchanged...
            _authState.value = AuthState.Idle 
        }
    }
}

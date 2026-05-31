package com.example.academicmanager.ui.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.UserEntity
import com.example.academicmanager.data.UserRole
import com.example.academicmanager.data.UserStatus
import com.example.academicmanager.data.Lecturer
import com.example.academicmanager.data.SessionManager
import com.example.academicmanager.data.UniversityDao
import com.example.academicmanager.data.UniversityRepository
import com.example.academicmanager.util.CredentialUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    private val _verificationMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val verificationMessage: SharedFlow<String> = _verificationMessage

    var pendingVerificationEmail: String = ""
        private set

    var currentUser: Lecturer? = null
        private set

    fun updateCurrentUser(user: Lecturer) {
        currentUser = user
        _authState.value = AuthState.Authenticated(user)
    }

    fun login(username: String, password: String, context: Context? = null) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            // 1. Admin yerel hesabı (fallback)
            if (username == "admin" && password == "admin") {
                val adminUser = Lecturer(
                    fullName = "System Administrator",
                    username = "admin",
                    password = CredentialUtils.hashPassword("admin"),
                    role = UserRole.ADMIN,
                    mustChangePassword = false
                )
                currentUser = adminUser
                context?.let { SessionManager.saveSession(it, username) }
                _authState.value = AuthState.Authenticated(adminUser)
                return@launch
            }

            // 2. Query Firestore for Lecturer
            try {
                val lecturer = repository.getLecturerByUsername(username)
                if (lecturer != null) {
                    val hashedInput = CredentialUtils.hashPassword(password)
                    val passwordMatches = when {
                        lecturer.password == hashedInput -> true   // yeni format (SHA-256)
                        lecturer.password == password -> {         // eski format (plaintext) → migrate
                            repository.updateLecturer(lecturer.copy(password = hashedInput))
                            true
                        }
                        else -> false
                    }
                    if (passwordMatches) {
                        val activeLecturer = if (lecturer.password == password)
                            lecturer.copy(password = hashedInput) else lecturer
                        // Firebase Auth e-posta doğrulama kontrolü (sadece email'i olan kullanıcılar için)
                        if (activeLecturer.email.isNotBlank()) {
                            try {
                                val auth = FirebaseAuth.getInstance()
                                val result = auth.signInWithEmailAndPassword(activeLecturer.email, password).await()
                                val isVerified = result.user?.isEmailVerified ?: false
                                auth.signOut()
                                if (!isVerified) {
                                    pendingVerificationEmail = activeLecturer.email
                                    _authState.value = AuthState.Error("EMAIL_NOT_VERIFIED:${activeLecturer.email}")
                                    return@launch
                                }
                            } catch (_: Exception) {
                                // Firebase erişilemezse veya hesap yoksa Firestore auth'a devam et
                            }
                        }
                        // Hesap onay kontrolü
                        if (activeLecturer.status == com.example.academicmanager.data.AccountStatus.PENDING) {
                            _authState.value = AuthState.Error("Hesabınız henüz admin tarafından onaylanmadı.")
                            return@launch
                        }
                        currentUser = activeLecturer
                        context?.let { SessionManager.saveSession(it, username) }
                        if (activeLecturer.mustChangePassword) {
                            _authState.value = AuthState.MustChangePassword(activeLecturer)
                        } else {
                            _authState.value = AuthState.Authenticated(activeLecturer)
                        }
                    } else {
                        _authState.value = AuthState.Error("Kullanıcı adı veya şifre hatalı.")
                    }
                } else {
                    _authState.value = AuthState.Error("Kullanıcı adı veya şifre hatalı.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    if (e.message?.contains("Unable to resolve host") == true || e.message?.contains("network") == true)
                        "İnternet bağlantısı yok. Lütfen ağınızı kontrol edin."
                    else "Giriş hatası: ${e.message}"
                )
            }
        }
    }

    fun restoreSession(username: String) {
        viewModelScope.launch {
            if (username == "admin") {
                val adminUser = Lecturer(fullName = "System Administrator", username = "admin", password = CredentialUtils.hashPassword("admin"), role = UserRole.ADMIN, mustChangePassword = false)
                currentUser = adminUser
                _authState.value = AuthState.Authenticated(adminUser)
                return@launch
            }
            try {
                val lecturer = repository.getLecturerByUsername(username)
                if (lecturer != null && !lecturer.mustChangePassword) {
                    currentUser = lecturer
                    _authState.value = AuthState.Authenticated(lecturer)
                }
            } catch (_: Exception) { }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        val user = currentUser ?: return
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                if (user.password == CredentialUtils.hashPassword(oldPassword)) {
                    val updatedUser = user.copy(
                        password = CredentialUtils.hashPassword(newPassword),
                        mustChangePassword = false
                    )
                    repository.updateLecturer(updatedUser)
                    currentUser = updatedUser
                    _authState.value = AuthState.Authenticated(updatedUser)
                } else {
                    _authState.value = AuthState.Error("Mevcut şifre hatalı.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Şifre güncelleme hatası: ${e.message}")
            }
        }
    }

    fun logout(context: Context? = null) {
        context?.let { SessionManager.clearSession(it) }
        currentUser = null
        _authState.value = AuthState.Idle
    }

    fun register(
        username: String,
        password: String,
        fullName: String,
        role: UserRole,
        department: String,
        adminCode: String = "",
        email: String = "",
        city: String = "",
        university: String = ""
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val existing = repository.getLecturerByUsername(username)
                if (existing != null) {
                    _authState.value = AuthState.Error("Bu kullanıcı adı zaten alınmış.")
                    return@launch
                }

                val isAdminCode = adminCode.trim() == com.example.academicmanager.data.ADMIN_REGISTRATION_CODE
                val finalRole   = if (isAdminCode) UserRole.ADMIN else role
                val finalStatus = if (isAdminCode) com.example.academicmanager.data.AccountStatus.APPROVED
                                  else com.example.academicmanager.data.AccountStatus.PENDING
                val finalEmail  = email.trim().lowercase()

                // Firebase Auth hesabı oluştur ve doğrulama maili gönder (admin kodu dışı kayıtlar için)
                if (!isAdminCode && finalEmail.isNotBlank()) {
                    try {
                        val auth = FirebaseAuth.getInstance()
                        val result = auth.createUserWithEmailAndPassword(finalEmail, password).await()
                        result.user?.sendEmailVerification()?.await()
                        auth.signOut()
                    } catch (e: FirebaseAuthUserCollisionException) {
                        _authState.value = AuthState.Error("Bu e-posta adresi zaten kayıtlı.")
                        return@launch
                    } catch (e: Exception) {
                        _authState.value = AuthState.Error("Firebase hesabı oluşturulamadı: ${e.message}")
                        return@launch
                    }
                }

                val newLecturer = com.example.academicmanager.data.Lecturer(
                    username           = username.trim(),
                    password           = CredentialUtils.hashPassword(password),
                    fullName           = fullName.trim(),
                    role               = finalRole,
                    department         = department.trim(),
                    mustChangePassword = false,
                    status             = finalStatus,
                    email              = finalEmail,
                    city               = city.trim(),
                    university         = university.trim()
                )
                repository.updateLecturer(newLecturer)

                if (isAdminCode) {
                    currentUser = newLecturer
                    _authState.value = AuthState.Authenticated(newLecturer)
                } else {
                    _authState.value = AuthState.Error("REGISTRATION_PENDING")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Kayıt başarısız: ${e.message}")
            }
        }
    }

    fun resendVerificationEmail(rawPassword: String) {
        val email = pendingVerificationEmail.ifBlank { return }
        viewModelScope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val result = auth.signInWithEmailAndPassword(email, rawPassword).await()
                result.user?.sendEmailVerification()?.await()
                auth.signOut()
                _verificationMessage.emit("Doğrulama maili $email adresine gönderildi.")
            } catch (e: Exception) {
                _verificationMessage.emit("Mail gönderilemedi: ${e.message}")
            }
        }
    }
}

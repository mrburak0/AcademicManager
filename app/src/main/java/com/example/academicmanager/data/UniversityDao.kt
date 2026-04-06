package com.example.academicmanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UniversityDao {
    // Course Operations
    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Query("SELECT COUNT(*) FROM courses")
    fun getCourseCount(): Flow<Int>

    // Lecturer Operations
    @Query("SELECT * FROM lecturers")
    fun getAllLecturers(): Flow<List<LecturerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecturer(lecturer: LecturerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecturers(lecturers: List<LecturerEntity>)

    @Query("SELECT * FROM lecturers WHERE fullName = :fullName LIMIT 1")
    suspend fun getLecturerByName(fullName: String): LecturerEntity?

    @Delete
    suspend fun deleteLecturer(lecturer: LecturerEntity)

    @Query("SELECT COUNT(*) FROM lecturers")
    fun getLecturerCount(): Flow<Int>

    // User Operations
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUser(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE status = 'PENDING'")
    fun getPendingUsers(): Flow<List<UserEntity>>

    @Query("UPDATE users SET password = :newPassword WHERE id = :userId")
    suspend fun updatePassword(userId: Int, newPassword: String)
}

package com.example.academicmanager.data

import kotlinx.coroutines.flow.Flow

interface UniversityRepository {
    // Departments
    fun getDepartments(): Flow<List<Department>>
    suspend fun addDepartment(department: Department)

    // Courses
    fun getCourses(): Flow<List<Course>>
    suspend fun addCourses(courses: List<Course>)
    suspend fun deleteCourse(courseCode: String)

    // Lecturers
    fun getLecturers(): Flow<List<Lecturer>>
    suspend fun addLecturers(lecturers: List<Lecturer>)
    suspend fun updateLecturer(lecturer: Lecturer)
    suspend fun getLecturerByUsername(username: String): Lecturer?

    // Classrooms
    fun getClassrooms(): Flow<List<Classroom>>
    suspend fun addClassroom(classroom: Classroom)

    // Schedule Entries
    fun getScheduleEntries(): Flow<List<ScheduleEntry>>
    suspend fun addScheduleEntry(entry: ScheduleEntry)
    fun getLecturerSchedule(lecturerName: String): Flow<List<ScheduleEntry>>
}

package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.GradeRecord
import com.example.academicmanager.data.UniversityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GradeViewModel(private val repository: UniversityRepository) : ViewModel() {

    val allGrades: StateFlow<List<GradeRecord>> = repository.getGrades()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveResult = MutableSharedFlow<Boolean>()
    val saveResult: SharedFlow<Boolean> = _saveResult

    fun getGradesByStudent(studentUsername: String): Flow<List<GradeRecord>> =
        repository.getGradesByStudent(studentUsername)

    fun getGradesByLecturer(lecturerUsername: String): Flow<List<GradeRecord>> =
        repository.getGradesByLecturer(lecturerUsername)

    fun getGradesByCourse(courseCode: String): Flow<List<GradeRecord>> =
        repository.getGradesByCourse(courseCode)

    fun saveGrade(
        existingId: String,
        studentUsername: String,
        studentName: String,
        courseCode: String,
        courseName: String,
        department: String,
        lecturerUsername: String,
        midterm: Float,
        finalExam: Float,
        assignment: Float,
        lab: Float,
        hasLab: Boolean
    ) {
        viewModelScope.launch {
            try {
                val avg = GradeRecord.calculateAverage(midterm, finalExam, assignment, lab, hasLab)
                val letter = GradeRecord.calculateLetterGrade(avg)
                val gpa = GradeRecord.letterToGpa(letter)
                val record = GradeRecord(
                    id = existingId,
                    studentUsername = studentUsername,
                    studentName = studentName,
                    courseCode = courseCode,
                    courseName = courseName,
                    department = department,
                    lecturerUsername = lecturerUsername,
                    midterm = midterm,
                    finalExam = finalExam,
                    assignment = assignment,
                    lab = lab,
                    hasLab = hasLab,
                    letterGrade = letter,
                    gpa = gpa,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveGrade(record)
                _saveResult.emit(true)
            } catch (_: Exception) {
                _saveResult.emit(false)
            }
        }
    }

    fun deleteGrade(gradeId: String) {
        viewModelScope.launch {
            try { repository.deleteGrade(gradeId) } catch (_: Exception) { }
        }
    }

    fun calculateCumulativeGpa(grades: List<GradeRecord>): Float {
        val valid = grades.filter { it.gpa >= 0 }
        if (valid.isEmpty()) return 0f
        return valid.map { it.gpa }.average().toFloat()
    }
}

package com.example.academicmanager.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UniversityRepositoryImpl(
    private val firestore: FirebaseFirestore
) : UniversityRepository {

    override fun getDepartments(): Flow<List<Department>> {
        return firestore.collection("departments")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Department::class.java) }
    }

    override suspend fun addDepartment(department: Department) {
        val docRef = if (department.id.isEmpty()) {
            firestore.collection("departments").document()
        } else {
            firestore.collection("departments").document(department.id)
        }
        val finalDept = department.copy(id = docRef.id)
        docRef.set(finalDept).await()
    }

    override fun getCourses(): Flow<List<Course>> {
        return firestore.collection("courses")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Course::class.java) }
    }

    override suspend fun addCourses(courses: List<Course>) {
        try {
            Log.d("FirestoreTest", "Repository: Starting batch write for ${courses.size} courses")
            val batch = firestore.batch()
            courses.forEach { course ->
                val docRef = firestore.collection("courses").document(course.courseCode)
                batch.set(docRef, course)
            }
            batch.commit().await()
            Log.d("FirestoreTest", "Repository: Batch write for courses successful")
        } catch (e: Exception) {
            Log.e("FirestoreTest", "Repository: Batch write for courses FAILED", e)
            throw e
        }
    }

    override suspend fun deleteCourse(courseCode: String) {
        firestore.collection("courses").document(courseCode).delete().await()
    }

    override fun getLecturers(): Flow<List<Lecturer>> {
        return firestore.collection("lecturers")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Lecturer::class.java) }
    }

    override suspend fun addLecturers(lecturers: List<Lecturer>) {
        try {
            Log.d("FirestoreTest", "Repository: Starting batch write for ${lecturers.size} lecturers")
            val batch = firestore.batch()
            lecturers.forEach { lecturer ->
                val docRef = firestore.collection("lecturers").document(lecturer.username)
                batch.set(docRef, lecturer)
            }
            batch.commit().await()
            Log.d("FirestoreTest", "Repository: Batch write for lecturers successful")
        } catch (e: Exception) {
            Log.e("FirestoreTest", "Repository: Batch write for lecturers FAILED", e)
            throw e
        }
    }

    override suspend fun updateLecturer(lecturer: Lecturer) {
        firestore.collection("lecturers").document(lecturer.username).set(lecturer).await()
    }

    override suspend fun getLecturerByUsername(username: String): Lecturer? {
        return firestore.collection("lecturers").document(username).get().await().toObject(Lecturer::class.java)
    }

    override fun getClassrooms(): Flow<List<Classroom>> {
        return firestore.collection("classrooms")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Classroom::class.java) }
    }

    override suspend fun addClassroom(classroom: Classroom) {
        val docRef = if (classroom.id.isEmpty()) {
            firestore.collection("classrooms").document()
        } else {
            firestore.collection("classrooms").document(classroom.id)
        }
        val finalClassroom = classroom.copy(id = docRef.id)
        docRef.set(finalClassroom).await()
    }

    override fun getScheduleEntries(): Flow<List<ScheduleEntry>> {
        return firestore.collection("schedule_entries")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(ScheduleEntry::class.java) }
    }

    override suspend fun addScheduleEntry(entry: ScheduleEntry) {
        val docRef = if (entry.id.isEmpty()) {
            firestore.collection("schedule_entries").document()
        } else {
            firestore.collection("schedule_entries").document(entry.id)
        }
        val finalEntry = entry.copy(id = docRef.id)
        docRef.set(finalEntry).await()
    }

    override fun getLecturerSchedule(lecturerName: String): Flow<List<ScheduleEntry>> {
        return firestore.collection("schedule_entries")
            .whereEqualTo("lecturerName", lecturerName)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(ScheduleEntry::class.java) }
    }
}

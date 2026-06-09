package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.academicmanager.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AttendanceRiskViewModel(private val repository: UniversityRepository) : ViewModel() {

    // Reaktif akışlar — ViewModel dışından flow subscribe edilir

    fun risksForLecturer(
        lecturerUsername: String,
        students        : List<Lecturer>,
        minSessions     : Int = 3
    ): Flow<List<AttendanceRisk>> =
        repository.getAttendanceByLecturer(lecturerUsername).map { records ->
            computeRisks(
                records          = records,
                students         = students,
                filterLecturer   = lecturerUsername,
                minSessions      = minSessions
            )
        }

    fun risksForDepartment(
        department  : String,
        students    : List<Lecturer>,
        minSessions : Int = 3
    ): Flow<List<AttendanceRisk>> =
        repository.getAttendanceRecords().map { records ->
            computeRisks(
                records           = records,
                students          = students,
                filterDepartment  = department,
                minSessions       = minSessions
            )
        }

    // ── Hesaplama motoru ──────────────────────────────────────

    fun computeRisks(
        records          : List<AttendanceRecord>,
        students         : List<Lecturer>,
        filterLecturer   : String? = null,
        filterDepartment : String? = null,
        minSessions      : Int = 3
    ): List<AttendanceRisk> {
        val studentMap = students.associateBy { it.username }
        val risks      = mutableListOf<AttendanceRisk>()

        val byCourse = records.groupBy { it.courseCode }

        for ((courseCode, courseRecords) in byCourse) {
            if (courseRecords.size < minSessions) continue
            val rep = courseRecords.first()

            if (filterLecturer   != null && rep.lecturerUsername != filterLecturer) continue
            if (filterDepartment != null && rep.department       != filterDepartment) continue

            // Yalnızca bu dersin bölümündeki kayıtlı öğrencileri değerlendir
            val deptStudents = students.filter { it.department == rep.department }

            for (student in deptStudents) {
                val attended   = courseRecords.count { student.username in it.presentStudents }
                val total      = courseRecords.size
                val percentage = (attended.toFloat() / total) * 100f

                if (percentage >= 70f) continue   // risk yok

                val level = if (percentage < 50f) RiskLevel.CRITICAL else RiskLevel.WARNING

                // Ardışık devamsızlık zinciri (son N seansın sonundan geriye doğru say)
                val sorted = courseRecords.sortedBy { it.sessionDate }
                var consec  = 0
                for (record in sorted.reversed()) {
                    if (student.username !in record.presentStudents) consec++
                    else break
                }

                val lastSeen = courseRecords
                    .filter { student.username in it.presentStudents }
                    .maxByOrNull { it.sessionDate }?.sessionDate ?: "—"

                risks.add(
                    AttendanceRisk(
                        studentUsername  = student.username,
                        studentName      = student.fullName,
                        studentId        = student.studentId,
                        courseCode       = courseCode,
                        courseName       = rep.courseName,
                        lecturerUsername = rep.lecturerUsername,
                        department       = rep.department,
                        attendedCount    = attended,
                        totalSessions    = total,
                        percentage       = percentage,
                        level            = level,
                        consecutiveMissed = consec,
                        lastSeenDate     = lastSeen
                    )
                )
            }
        }

        // Kritik önce, sonra oran küçükten büyüğe
        return risks.sortedWith(
            compareByDescending<AttendanceRisk> { it.level.ordinal }
                .thenBy { it.percentage }
        )
    }

    // ── İstatistik yardımcıları ───────────────────────────────

    fun criticalCount(risks: List<AttendanceRisk>): Int =
        risks.count { it.level == RiskLevel.CRITICAL }

    fun warningCount(risks: List<AttendanceRisk>): Int =
        risks.count { it.level == RiskLevel.WARNING }
}

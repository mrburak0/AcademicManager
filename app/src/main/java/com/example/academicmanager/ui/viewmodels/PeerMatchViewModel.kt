package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PeerMatchViewModel(private val repository: UniversityRepository) : ViewModel() {

    private val _matchingResult = MutableSharedFlow<Int>() // oluşturulan yeni eşleşme sayısı
    val matchingResult: SharedFlow<Int> = _matchingResult

    // ── Sorgular ─────────────────────────────────────────────

    fun getMatchesByDepartment(department: String): Flow<List<PeerMatch>> =
        repository.getPeerMatchesByDepartment(department)

    fun getMatchesForStudent(username: String): Flow<List<PeerMatch>> =
        repository.getPeerMatchesByStudent(username)

    // ── Eşleştirme algoritması (admin tetikler) ───────────────

    /**
     * Her ders için:
     *   - Devam oranı ≥ 75% → mentor adayı
     *   - Devam oranı  < 65% → mentee adayı
     * Çiftler oluşturulurken:
     *   - Zaten aktif/bekleyen çift varsa atlanır
     *   - Her mentor en fazla 2 mentee alabilir
     */
    fun runMatching(
        department     : String,
        records        : List<AttendanceRecord>,
        students       : List<Lecturer>,
        existingMatches: List<PeerMatch>,
        minSessions    : Int = 3
    ) {
        viewModelScope.launch {
            try {
                val newMatches = buildMatches(department, records, students, existingMatches, minSessions)
                newMatches.forEach { repository.savePeerMatch(it) }
                _matchingResult.emit(newMatches.size)
            } catch (_: Exception) {
                _matchingResult.emit(0)
            }
        }
    }

    private fun buildMatches(
        department     : String,
        records        : List<AttendanceRecord>,
        students       : List<Lecturer>,
        existingMatches: List<PeerMatch>,
        minSessions    : Int
    ): List<PeerMatch> {
        val deptStudents = students.filter { it.department == department }
        val deptRecords  = records.filter  { it.department == department }
        val now          = System.currentTimeMillis()

        // Var olan aktif/pending çiftler — tekrar eşleştirmeyi önlemek için
        val activePairs = existingMatches
            .filter { it.status in listOf(PeerMatchStatus.PENDING, PeerMatchStatus.ACTIVE) }
            .map { it.mentorUsername to it.menteeUsername }
            .toSet()

        val result = mutableListOf<PeerMatch>()

        for ((courseCode, courseRecords) in deptRecords.groupBy { it.courseCode }) {
            if (courseRecords.size < minSessions) continue
            val rep = courseRecords.first()

            val mentors = mutableListOf<String>()
            val mentees = mutableListOf<String>()

            for (s in deptStudents) {
                val attended = courseRecords.count { s.username in it.presentStudents }
                val pct = (attended.toFloat() / courseRecords.size) * 100f
                when {
                    pct >= 75f -> mentors.add(s.username)
                    pct <  65f -> mentees.add(s.username)
                }
            }

            // Mentor başına mentee yükü sayacı
            val mentorLoad = mutableMapOf<String, Int>()

            for (mentee in mentees.shuffled()) {
                val mentor = mentors.firstOrNull { m ->
                    (mentorLoad[m] ?: 0) < 2 &&
                    (m to mentee) !in activePairs
                } ?: continue

                mentorLoad[mentor] = (mentorLoad[mentor] ?: 0) + 1

                result.add(
                    PeerMatch(
                        courseCode     = courseCode,
                        courseName     = rep.courseName,
                        department     = department,
                        mentorUsername = mentor,
                        menteeUsername = mentee,
                        participants   = listOf(mentor, mentee),
                        status         = PeerMatchStatus.PENDING,
                        createdAt      = now,
                        expiresAt      = now + 14 * 24 * 3_600_000L  // 2 hafta
                    )
                )
            }
        }
        return result
    }

    // ── Öğrenci yanıtı ────────────────────────────────────────

    fun respond(match: PeerMatch, username: String, accepted: Boolean) {
        viewModelScope.launch {
            try {
                val updated = when {
                    !accepted -> match.copy(status = PeerMatchStatus.DECLINED)
                    match.mentorUsername == username -> {
                        val m = match.copy(mentorAccepted = true)
                        if (m.menteeAccepted) m.copy(status = PeerMatchStatus.ACTIVE) else m
                    }
                    else -> {
                        val m = match.copy(menteeAccepted = true)
                        if (m.mentorAccepted) m.copy(status = PeerMatchStatus.ACTIVE) else m
                    }
                }
                repository.updatePeerMatch(updated)
            } catch (_: Exception) {}
        }
    }

    fun cancelMatch(match: PeerMatch) {
        viewModelScope.launch {
            try {
                repository.updatePeerMatch(match.copy(status = PeerMatchStatus.DECLINED))
            } catch (_: Exception) {}
        }
    }

    // ── Yardımcılar ───────────────────────────────────────────

    /** Eşleşmenin karşı tarafının kullanıcı adını döner. */
    fun partnerUsername(match: PeerMatch, myUsername: String): String =
        if (match.mentorUsername == myUsername) match.menteeUsername
        else match.mentorUsername

    /** Öğrencinin bu eşleşmedeki rolü. */
    fun myRole(match: PeerMatch, myUsername: String): String =
        if (match.mentorUsername == myUsername) "mentor" else "mentee"

    /** Süresi dolmuş bekleyen eşleşmeleri listeler. */
    fun expiredPending(matches: List<PeerMatch>): List<PeerMatch> {
        val now = System.currentTimeMillis()
        return matches.filter {
            it.status == PeerMatchStatus.PENDING && it.expiresAt > 0 && now > it.expiresAt
        }
    }
}

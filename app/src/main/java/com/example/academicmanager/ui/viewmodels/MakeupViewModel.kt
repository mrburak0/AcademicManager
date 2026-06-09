package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academicmanager.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

class MakeupViewModel(private val repository: UniversityRepository) : ViewModel() {

    private val _saveResult = MutableSharedFlow<Boolean>()
    val saveResult: SharedFlow<Boolean> = _saveResult

    // ── Firestore sorgular ────────────────────────────────────

    fun getRequestsByDepartment(department: String): Flow<List<MakeupRequest>> =
        repository.getMakeupRequestsByDepartment(department)

    fun getRequestsByLecturer(lecturerUsername: String): Flow<List<MakeupRequest>> =
        repository.getMakeupRequestsByLecturer(lecturerUsername)

    // ── Slot öneri algoritması (client-side) ──────────────────

    /**
     * Bölüm programı + hoca takvimi kesişiminden 3 aday slot üretir.
     * - Hocanın öğrettiği slotlar çıkarılır
     * - Öğrenci çakışma sayısı minimuma indirilir
     * - Hoca müsaitse bonus puan (düşük skor = daha iyi)
     */
    fun proposeSlots(
        cancelledDay     : String,
        cancelledTimeSlot: String,
        deptEntries      : List<ScheduleEntry>,    // bölüm ders programı
        lecturerEntries  : List<ScheduleEntry>,    // hocanın kendi girişleri
        lecturerAvail    : LecturerAvailability?
    ): List<MakeupSlot> {
        val timeSlots = listOf(
            "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00",
            "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00"
        )
        val today = LocalDate.now()
        val candidates = mutableListOf<Pair<MakeupSlot, Int>>()

        for (daysAhead in 1..14) {
            val date = today.plusDays(daysAhead.toLong())
            val dow  = date.dayOfWeek
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue

            val engDay = dow.toEnglish()
            val lecturerFreeSlots = lecturerAvail?.slotsForDay(engDay) ?: emptyList()

            for (slot in timeSlots) {
                // İptal edilen dersin aynı gün+saat kombinasyonunu atla
                if (engDay == cancelledDay && slot == cancelledTimeSlot) continue

                // Hoca bu slotta zaten ders veriyor → geç
                if (lecturerEntries.any { it.dayOfWeek == engDay && it.timeSlot == slot }) continue

                // Öğrenci çakışma sayısı (aynı gün+saatte ders olan öğrenciler)
                val conflicts = deptEntries.count { it.dayOfWeek == engDay && it.timeSlot == slot }

                // Hoca bu slotu müsait göstermediyse hafif ceza
                val lecturerPenalty = if (lecturerFreeSlots.contains(slot)) 0 else 30

                val score = conflicts * 100 + lecturerPenalty
                val slotId = "${date}_${slot.replace(":", "").replace("-", "")}"

                candidates.add(
                    MakeupSlot(
                        id            = slotId,
                        date          = date.toString(),
                        dayOfWeek     = engDay,
                        timeSlot      = slot,
                        conflictCount = conflicts
                    ) to score
                )
            }
        }

        // Aynı score'da farklı günlerin gelmemesi için shuffle sonra sort
        return candidates.shuffled().sortedBy { it.second }.take(3).map { it.first }
    }

    private fun DayOfWeek.toEnglish(): String = when (this) {
        DayOfWeek.MONDAY    -> "Monday"
        DayOfWeek.TUESDAY   -> "Tuesday"
        DayOfWeek.WEDNESDAY -> "Wednesday"
        DayOfWeek.THURSDAY  -> "Thursday"
        DayOfWeek.FRIDAY    -> "Friday"
        DayOfWeek.SATURDAY  -> "Saturday"
        DayOfWeek.SUNDAY    -> "Sunday"
    }

    // ── Talep oluştur ─────────────────────────────────────────

    fun createRequest(
        courseCode       : String,
        courseName       : String,
        department       : String,
        lecturerUsername : String,
        lecturerName     : String,
        cancelledDay     : String,
        cancelledTimeSlot: String,
        cancelReason     : String,
        proposedSlots    : List<MakeupSlot>
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val request = MakeupRequest(
                    originalCourseCode = courseCode,
                    courseName         = courseName,
                    department         = department,
                    lecturerUsername   = lecturerUsername,
                    lecturerName       = lecturerName,
                    cancelledDayOfWeek = cancelledDay,
                    cancelledTimeSlot  = cancelledTimeSlot,
                    cancelReason       = cancelReason,
                    proposedSlots      = proposedSlots,
                    status             = MakeupStatus.VOTING,
                    createdAt          = now,
                    voteDeadline       = now + 48 * 3_600_000L  // 48 saat
                )
                repository.saveMakeupRequest(request)
                _saveResult.emit(true)
            } catch (_: Exception) {
                _saveResult.emit(false)
            }
        }
    }

    // ── Öğrenci oyu ──────────────────────────────────────────

    fun vote(requestId: String, slotId: String, studentUsername: String) {
        viewModelScope.launch {
            try {
                repository.voteForMakeupSlot(requestId, studentUsername, slotId)
            } catch (_: Exception) {}
        }
    }

    // ── Hoca / Admin onayı ────────────────────────────────────

    fun confirmSlot(request: MakeupRequest, slotId: String) {
        viewModelScope.launch {
            try {
                repository.updateMakeupRequest(
                    request.copy(status = MakeupStatus.CONFIRMED, confirmedSlotId = slotId)
                )
            } catch (_: Exception) {}
        }
    }

    fun cancelRequest(request: MakeupRequest) {
        viewModelScope.launch {
            try {
                repository.updateMakeupRequest(request.copy(status = MakeupStatus.CANCELLED))
            } catch (_: Exception) {}
        }
    }

    // ── Yardımcılar ───────────────────────────────────────────

    /** Oylama sonucunda en çok oy alan slotId'yi döner. Eşitlikte ilk proposedSlot kazanır. */
    fun winningSlotId(request: MakeupRequest): String? {
        if (request.votes.isEmpty()) return null
        val tally = request.votes.values.groupingBy { it }.eachCount()
        return tally.maxByOrNull { it.value }?.key
    }

    /** Kalan oylama süresi (ms). Negatifse süre dolmuş. */
    fun voteTimeLeftMs(request: MakeupRequest): Long =
        request.voteDeadline - System.currentTimeMillis()

    /** Öğrencinin bu talep için oy kullanıp kullanmadığını döner. */
    fun hasVoted(request: MakeupRequest, username: String): Boolean =
        username in request.votes
}

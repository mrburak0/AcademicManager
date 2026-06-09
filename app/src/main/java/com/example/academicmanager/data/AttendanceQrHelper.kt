package com.example.academicmanager.data

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * TOTP-benzeri dönen QR yardımcısı.
 * QR içeriği her 30 saniyede bir değişir → ekran görüntüsü paylaşımını engeller.
 *
 * Format: "ACAD_ATT|{sessionId}|{timeWindow}|{hmac8}"
 *   timeWindow  = epochMillis / 30_000
 *   hmac8       = HmacSHA256(sessionId|timeWindow, sessionSecret)[0..7]
 *
 * Doğrulama: ±1 pencere toleransı (toplam ~60 sn) kabul edilir.
 */
object AttendanceQrHelper {

    const val QR_WINDOW_MS = 30_000L
    const val QR_PREFIX    = "ACAD_ATT"

    // ── QR üretimi ────────────────────────────────────────────

    fun currentQrContent(sessionId: String, secret: String): String {
        val window = currentWindow()
        return "$QR_PREFIX|$sessionId|$window|${hmac8(sessionId, window, secret)}"
    }

    // ── QR doğrulama ─────────────────────────────────────────

    fun validate(qrContent: String, session: AttendanceSession): Boolean {
        if (!session.isActive) return false
        if (System.currentTimeMillis() > session.expiresAt) return false
        val p = qrContent.split("|")
        if (p.size != 4 || p[0] != QR_PREFIX) return false
        if (p[1] != session.id) return false
        val qrWindow = p[2].toLongOrNull() ?: return false
        if (kotlin.math.abs(currentWindow() - qrWindow) > 1) return false
        return p[3] == hmac8(session.id, qrWindow, session.sessionSecret)
    }

    // ── Sonraki rotasyona kalan saniyeler ─────────────────────

    fun secsUntilRotation(): Long {
        val now = System.currentTimeMillis()
        return (QR_WINDOW_MS - now % QR_WINDOW_MS) / 1000L
    }

    // ── Ders saati kontrolü ───────────────────────────────────

    /**
     * Şu anki gün ve saatin ScheduleEntry'nin belirlediği zaman dilimine
     * (±5 dakika toleranslı) denk gelip gelmediğini döner.
     */
    fun isCurrentlyScheduled(entry: ScheduleEntry): Boolean {
        return try {
            val now   = java.time.LocalDateTime.now()
            val today = now.dayOfWeek
            val entryDay = parseTurkishDay(entry.dayOfWeek) ?: return false
            if (today != entryDay) return false
            val parts = entry.timeSlot.split("-")
            if (parts.size != 2) return false
            val start = java.time.LocalTime.parse(parts[0].trim())
            val end   = java.time.LocalTime.parse(parts[1].trim())
            val t = now.toLocalTime()
            t.isAfter(start.minusMinutes(5)) && t.isBefore(end.plusMinutes(5))
        } catch (_: Exception) { false }
    }

    // ── Özel yardımcılar ──────────────────────────────────────

    private fun currentWindow() = System.currentTimeMillis() / QR_WINDOW_MS

    private fun hmac8(sessionId: String, window: Long, secret: String): String = try {
        val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        mac.doFinal("$sessionId|$window".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(8)
    } catch (_: Exception) { "" }

    private fun parseTurkishDay(day: String): java.time.DayOfWeek? = when (
        day.lowercase().trim()
            .replace('ı', 'i').replace('İ', 'i').replace('ş', 's').replace('Ş', 's')
            .replace('ç', 'c').replace('Ç', 'c').replace('ö', 'o').replace('Ö', 'o')
            .replace('ü', 'u').replace('Ü', 'u')
    ) {
        "pazartesi", "monday"    -> java.time.DayOfWeek.MONDAY
        "sali", "tuesday"        -> java.time.DayOfWeek.TUESDAY
        "carsamba", "wednesday"  -> java.time.DayOfWeek.WEDNESDAY
        "persembe", "thursday"   -> java.time.DayOfWeek.THURSDAY
        "cuma", "friday"         -> java.time.DayOfWeek.FRIDAY
        "cumartesi", "saturday"  -> java.time.DayOfWeek.SATURDAY
        "pazar", "sunday"        -> java.time.DayOfWeek.SUNDAY
        else -> null
    }
}

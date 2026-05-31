# AcademicManager — Sunum Notları
**Mobile Programming | Kotlin & Android**

---

## SLAYT 1 — Proje Tanıtımı

**AcademicManager**
> "Üniversite yönetimini cebinize taşıyoruz"

- Android uygulaması — Kotlin + Jetpack Compose
- 3 farklı kullanıcı rolü: Admin · Hoca · Öğrenci
- Gerçek zamanlı Firebase Firestore backend
- Excel ile toplu veri aktarımı

📱 *[01_giris_ekrani.png göster]*

---

## SLAYT 2 — Kullanılan Teknolojiler

```
┌─────────────────────────────────────┐
│         JETPACK COMPOSE (UI)        │
│         Material 3 Design           │
├─────────────────────────────────────┤
│    MVVM Mimarisi  │  StateFlow/Flow  │
├─────────────────────────────────────┤
│      Kotlin Coroutines (async)       │
├─────────────────────────────────────┤
│    FIREBASE FIRESTORE (real-time)    │
├─────────────────────────────────────┤
│    Apache POI  │  MediaStore API     │
└─────────────────────────────────────┘
```

**Neden bu stack?**
- Compose → XML yazmadan tamamen Kotlin ile UI
- Firestore → Sunucu kurmadan anlık senkronizasyon
- Coroutines → Callback cehennemi yok, temiz async kod

---

## SLAYT 3 — Mimari: MVVM + Repository

```kotlin
// ViewModel — UI'dan bağımsız iş mantığı
val scheduleEntries: StateFlow<List<ScheduleEntry>> =
    repository.getScheduleEntries()
        .stateIn(viewModelScope,
                 SharingStarted.WhileSubscribed(5000),
                 emptyList())

// Compose UI — state'i izler, otomatik yenilenir
val entries by viewModel.scheduleEntries.collectAsState()
```

**Avantaj:** Ekran döndürme, arka plana atma → veri kaybolmaz

📱 *[02_admin_dashboard.png göster]*

---

## SLAYT 4 — Firebase Real-Time Sync

```kotlin
// Tek satır — tüm cihazlarda anlık güncelleme
firestore.collection("announcements")
    .snapshots()
    .map { it.toObjects(Announcement::class.java) }
```

**Nasıl çalışır?**
1. Admin duyuru ekler → Firestore'a yazar
2. Firestore → tüm bağlı cihazlara push
3. Hoca/Öğrenci ekranı → **~2 saniyede** güncellenir

📱 *[07_duyurular.png göster — "SYNC TEST" duyurusu]*

---

## SLAYT 5 — Excel İçe Aktarma + Güvenlik

```kotlin
// SHA-256 ile şifre hashing
fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
    return hashBytes.joinToString("") { "%02x".format(it) }
}

// Otomatik kullanıcı adı üretimi
// "Ahmet Yılmaz" → Dr. → "ahmet_yilmaz"
fun generateUsername(fullName: String, title: String): String {
    return fullName.lowercase()
        .replace('ğ','g').replace('ü','u').replace('ş','s')
        .replace('ı','i').replace('ö','o').replace('ç','c')
        .trim().split(" ").joinToString("_")
}
```

- Plain text şifre **asla** veritabanına gitmez
- Admin yalnızca bir kez görür → CredentialSheet

📱 *[08_admin_import.png göster]*

---

## SLAYT 6 — Sealed Class ile State Yönetimi

```kotlin
sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class PreviewReady(val items: List<Any>, val type: ImportType) : ImportState()
    data class Success(val message: String) : ImportState()
    data class Error(val message: String) : ImportState()
    data class CredentialSheet(val credentials: List<Pair<String, String>>) : ImportState()
}

// UI — when ile exhaustive kontrol, derleme garantisi
when (val s = state) {
    is ImportState.Idle          -> ImportIdleScreen(...)
    is ImportState.Loading       -> CircularProgressIndicator()
    is ImportState.PreviewReady  -> ImportPreviewScreen(state = s, ...)
    is ImportState.CredentialSheet -> CredentialSheetScreen(credentials = s.credentials)
    // ... (compiler hata verir eğer bir branch eksikse)
}
```

**Kotlin'in gücü:** `when` exhaustive → runtime crash imkânsız

---

## SLAYT 7 — Çakışma Kontrolü

```kotlin
// Aynı öğretmen, aynı gün, aynı saatte iki ders olamaz
val lecturerClash = currentEntries.find {
    it.lecturerName == lecturer.fullName &&
    it.dayOfWeek == day &&
    it.timeSlot == timeSlot
}
if (lecturerClash != null) {
    _assignmentResult.emit(AssignmentResult.LecturerClash(lecturerClash))
    return@launch   // Erken çıkış — Kotlin idiom
}
```

- Öğretmen çakışması → kırmızı uyarı
- Sınıf çakışması → sarı uyarı
- Kapasite aşımı → bilgi mesajı

📱 *[04_admin_program.png göster]*

---

## SLAYT 8 — Compose UI: Dinamik Şifre Kontrolü

```kotlin
@Composable
fun PasswordStrengthRow(password: String) {
    if (password.isEmpty()) return          // boşken gösterme
    Row {
        StrengthChip("6+ karakter", password.length >= 6)
        StrengthChip("Büyük harf",  password.any { it.isUpperCase() })
    }
    Row {
        StrengthChip("Küçük harf",  password.any { it.isLowerCase() })
        StrengthChip("Rakam",       password.any { it.isDigit() })
    }
}
```

- Kullanıcı yazarken **anlık** güncelleme
- Koşul karşılanınca yeşil `CheckCircle`, karşılanmayınca gri

📱 *[10_sifre_guc_gostergesi.png göster]*

---

## SLAYT 9 — Input Validasyon

```kotlin
// Ad-Soyad: sadece harf + boşluk (sayı engellenir)
onValueChange = {
    studentFullName = it.filter { c -> c.isLetter() || c.isWhitespace() }
}

// Kullanıcı adı: alfanumerik + alt çizgi, otomatik küçük harf
onValueChange = {
    studentUsername = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase()
}

// Kapasite: sadece rakam, 1-2000 arası
onValueChange = { capacityText = it.filter { c -> c.isDigit() } }
// kaydet butonunda:
if (parsedCapacity < 1 || parsedCapacity > 2000) → Toast uyarısı
```

**Kotlin lambda filter:** Tek satırda güçlü validasyon

---

## SLAYT 10 — Roller & Özellikler

| Özellik | Admin | Hoca | Öğrenci |
|---------|:-----:|:----:|:-------:|
| Dashboard & istatistik | ✅ | ✅ | ✅ |
| Ders programı görüntüle | ✅ | ✅ | ✅ |
| Sınıf yönetimi | ✅ | — | — |
| Hoca/Öğrenci ekleme | ✅ | — | — |
| Excel içe aktarma | ✅ | — | — |
| Ders talebi oluştur | — | ✅ | — |
| Müsaitlik bildirimi | — | ✅ | — |
| Talep onaylama | ✅ | — | — |
| Duyuru oluşturma | ✅ | — | — |
| PDF dışa aktarma | — | ✅ | — |

📱 *[05_hoca_anasayfa.png + 09_ogrenci_anasayfa.png göster]*

---

## SLAYT 11 — Demo

1. **Admin** → Dashboard → Sınıf ekle → Ders ata
2. **Hoca** → Takvim → PDF export → Talep oluştur
3. **Öğrenci** → Program görüntüle
4. **Real-time:** Admin duyuru ekle → Hoca ekranında anında görün

---

## SLAYT 12 — Özet

✅ **Kotlin** — Modern, güvenli, temiz syntax  
✅ **Jetpack Compose** — Tamamen deklaratif UI  
✅ **Firebase Firestore** — Sunucusuz real-time veritabanı  
✅ **MVVM + Flow** — Test edilebilir, ölçeklenebilir mimari  
✅ **Apache POI** — Excel okuma/yazma  
✅ **SHA-256** — Güvenli şifre saklama  

> "Tüm ekranlar Kotlin'de yazıldı — tek satır XML yok."

---

*Sorular için: mrburak.aslan@gmail.com*  
*GitHub: github.com/mrburak0/AcademicManager*

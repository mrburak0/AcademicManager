# AcademicManager — İlerleme Notu

> Bu dosyayı her görüşmenin başında ve sonunda güncelle.
> Yeni bir oturumda Claude'a "PROGRESS.md dosyasını oku ve kaldığımız yerden devam et" de.

---

## Proje Özeti

**Uygulama:** University Academic Management System  
**Stack:** Android · Jetpack Compose · Kotlin · MVVM · Firebase Firestore  
**Paket:** `com.example.academicmanager`

---

## Tamamlanan Aşamalar

### ✅ Phase 1 — Yerel Veritabanı & Temel UI
- Room veritabanı (CourseEntity, LecturerEntity, UserEntity)
- Excel import (Apache POI) — önizleme + kaydet akışı
- Dinamik alt navigasyon
- Minimalist Slate Gray / Emerald Green tasarım teması
- Temel takvim ekranı (Phase 1 versiyonu, hâlâ kodda mevcut)

### ✅ Phase 2 — Firebase & Çok Kullanıcılı Mimari
**Tüm adımlar tamamlandı. Son build: BAŞARILI ✓**

#### Veri Katmanı
| Dosya | Açıklama |
|-------|----------|
| `data/Lecturer.kt` | username, password, fullName, title, workingType, department, mustChangePassword, role |
| `data/Course.kt` | courseCode, courseName, department |
| `data/Classroom.kt` | id, name, capacity |
| `data/ScheduleEntry.kt` | id, courseCode, courseName, lecturerName, classroomName, dayOfWeek, timeSlot |
| `data/UniversityRepository.kt` | Interface — tüm CRUD + real-time flow tanımları |
| `data/UniversityRepositoryImpl.kt` | Firebase Firestore implementasyonu |

#### Kimlik Doğrulama
- `AuthViewModel` → login / changePassword / logout
- Admin fallback: `username=admin / password=admin`
- Hoca girişi: Firestore `lecturers` koleksiyonundan sorgu
- `mustChangePassword=true` → zorunlu şifre değiştirme ekranı

#### Admin Ekranları (`ui/AdminScreens.kt`)
- **AdminHomeScreen**: 3 panel (Unassigned Lecturers, Unassigned Courses, Total Classrooms) + QuickSetupCard (DB boşsa demo veri butonu)
- **ClassroomsScreen**: Sınıf ekle/listele, doluluk progress bar
- **AssignmentScreen**: Kurs+Hoca+Sınıf+Gün+Saat atama, çift rezervasyon önleme algoritması

#### Hoca Ekranları (`ui/LecturerScreens.kt`)
- **LecturerHomeScreen**: Karşılama kartı, istatistik kartları, günlük program özeti
- **LecturerCalendarScreen** *(yeniden tasarlandı)*:
  - Mon–Fri sekme butonları (bugün otomatik seçili, ders sayısı badge)
  - Dikey timeline görünümü (dolular → renkli kart, boşlar → ince çizgi)

#### Navigasyon & UI (`ui/MainScreen.kt`)
- `Screen` sealed class: Login, ChangePassword, AdminHome, Classrooms, Assignment, LecturerHome, LecturerCalendar, Data, Profile
- Rol bazlı yönlendirme (Admin → AdminHome, Hoca → LecturerHome)
- **LoginScreen** *(güzelleştirildi)*: Koyu gradient, kart form, şifre göster/gizle, demo hesap ipucu kartı
- Alt navigasyon: Labellar + emerald gösterge + MeetingRoom ikonu

#### Demo Veri (`ui/viewmodels/AdminViewModel.kt → seedDemoData()`)
Admin Dashboard'da "Load Demo Data" butonuna basınca Firestore'a yazılır:

| Tür | İçerik |
|-----|--------|
| Sınıflar | A-101 (60), B-202 (40), C-303 (30), Lab-1 (25) |
| Dersler | CS101, CS201, CS301, MATH101, MATH201, EE101 |
| Hocalar | ahmet_yilmaz, ayse_kaya, mehmet_demir |
| Program | 7 giriş — hocalara atanmış |

#### Demo Giriş Bilgileri
| Rol | Kullanıcı Adı | Şifre |
|-----|--------------|-------|
| Admin | `admin` | `admin` |
| Hoca 1 | `ahmet_yilmaz` | `ahmet123` |
| Hoca 2 | `ayse_kaya` | `ayse123` |
| Hoca 3 | `mehmet_demir` | `mehmet123` |

---

## Mevcut Dosya Yapısı

```
app/src/main/java/com/example/academicmanager/
├── data/
│   ├── Lecturer.kt
│   ├── Course.kt
│   ├── Classroom.kt
│   ├── ScheduleEntry.kt
│   ├── Department.kt
│   ├── UniversityRepository.kt          ← Interface
│   ├── UniversityRepositoryImpl.kt      ← Firebase impl
│   ├── UniversityDatabase.kt            ← Room (Phase 1, korunuyor)
│   ├── UniversityDao.kt                 ← Room DAO
│   ├── CourseEntity.kt / LecturerEntity.kt / UserEntity.kt
│   └── UserPreferences.kt
└── ui/
    ├── AdminScreens.kt                  ← Admin UI
    ├── LecturerScreens.kt               ← Hoca UI + Takvim
    ├── ImportScreen.kt                  ← Excel import
    ├── MainScreen.kt                    ← Navigasyon + Login + Profile
    ├── theme/                           ← Renkler, tipografi
    └── viewmodels/
        ├── AdminViewModel.kt            ← Admin logic + seedDemoData()
        ├── AuthViewModel.kt             ← Auth state
        ├── CourseViewModel.kt
        ├── DataImportViewModel.kt
        └── ViewModelFactory.kt
```

---

## Bilinen Eksiklikler / Yapılabilecekler

> Bunlar test edilmedi, henüz istenmedi — gelecek görüşmede öncelik ver.

- [ ] **Öğrenci modülü** — Phase 1/2 kapsamında öğrenci veri modeli yok. Gerekli mi?
- [ ] **Silme işlemleri** — Hoca/kurs silme UI'ı yok (sadece schedule entry silinebiliyor)
- [ ] **Import Screen** — Excel import (`ui/ImportScreen.kt`) Phase 2 Firestore'a bağlı mı kontrol et
- [ ] **ProfileScreen** — Hâlâ Room DAO kullanıyor bazı yerlerde (`pendingUsers`), Firestore'a taşınabilir
- [ ] **CalendarScreen (eski)** — `Screen.Calendar` legacy kodu hâlâ NavHost'ta, temizlenebilir
- [ ] **Hata yönetimi** — Network kesintisinde UI feedback yok
- [ ] **Arama/filtre** — Ders/hoca listelerinde arama yok

---

## Phase 3 — Henüz Belge Yok

`Phase_2_Course_Project.docx` dosyası var ama içeriği okunmadı (docx formatı).  
Bir sonraki görüşmede Phase 3 gereksinimleri varsa önce o dosyayı oku:
- Dosya: `Phase_2_Course_Project.docx` (proje kökünde)
- Okumak için: `! python -c "import docx; [print(p.text) for p in docx.Document('Phase_2_Course_Project.docx').paragraphs]"` komutunu dene

---

## Son Oturum Özeti (2026-04-13)

1. `seedDemoData()` fonksiyonu AdminViewModel'e eklendi
2. Admin Dashboard'a boş DB kontrolü + demo veri butonu eklendi
3. LecturerCalendarScreen tamamen yeniden tasarlandı (gün-sekme + timeline)
4. LoginScreen güzelleştirildi (gradient, kart, toggle, ipucu kartı)
5. Alt navigasyon labelları eklendi, ikon düzeltildi
6. `material-icons-extended` bağımlılığı eklendi
7. BUILD SUCCESSFUL ✓

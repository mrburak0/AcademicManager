package com.example.academicmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer

class UniversityApiViewModel : ViewModel() {

    private val _isLoading      = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _filtered       = MutableStateFlow<List<String>>(emptyList())
    val filtered: StateFlow<List<String>> = _filtered

    private val _departments    = MutableStateFlow<List<String>>(emptyList())
    val departments: StateFlow<List<String>> = _departments

    private val _isLoadingDepts = MutableStateFlow(false)
    val isLoadingDepts: StateFlow<Boolean> = _isLoadingDepts

    /** Artık kullanılmıyor — geriye dönük uyumluluk için boş bırakıldı. */
    fun loadAll() { _isLoading.value = false }

    /** Seçilen şehre göre üniversiteleri statik haritadan döndürür. */
    fun filterByCity(city: String) {
        if (city.isBlank()) { _filtered.value = emptyList(); return }
        _filtered.value = CITY_TO_UNIVERSITIES[city] ?: emptyList()
    }

    /**
     * 1) Üniversitenin Vikipedi sayfasından fakülte adlarını + fakülte sayfası linklerini çeker.
     * 2) Her fakülte sayfasını paralel olarak çekip içindeki bölümleri toplar.
     * Sonuç: fakülte adları + tüm bölüm adları (birleşik, tekrarsız, sıralı).
     */
    fun fetchDepartmentsForUniversity(universityName: String) {
        if (universityName.isBlank()) { _departments.value = emptyList(); return }
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingDepts.value = true
            _departments.value = emptyList()
            try {
                // facultyNames: üniversite sayfasından çekilen fakülte adları (bölüm değil)
                val facultyNames = mutableListOf<String>()
                val facultyLinks = mutableListOf<String>()

                // Adım 1: üniversite ana sayfası
                val uniWikitext = fetchWikitext(universityName)
                if (uniWikitext.isNotBlank()) {
                    parseUniversityPage(uniWikitext, universityName, facultyNames, facultyLinks)
                }

                // Adım 2: fakülte sayfalarını paralel çek, bölümleri topla
                val depts = coroutineScope {
                    facultyLinks.distinct().take(10).map { link ->
                        async(Dispatchers.IO) {
                            val wt = fetchWikitext(link)
                            if (wt.isNotBlank()) parseFacultyPage(wt) else emptyList()
                        }
                    }.awaitAll().flatten()
                }

                // Bölümler bulunduysa onları kullan; yoksa FALLBACK göster (fakülte adları bölüm gibi gösterilmez)
                _departments.value = when {
                    depts.size >= 3 -> depts.distinct().sorted()
                    else -> FALLBACK_DEPARTMENTS
                }
            } catch (_: Exception) {
                _departments.value = FALLBACK_DEPARTMENTS
            }
            _isLoadingDepts.value = false
        }
    }

    // ── Vikipedi wikitext çekici ───────────────────────────────────────────

    private fun fetchWikitext(pageTitle: String): String {
        return try {
            val enc  = java.net.URLEncoder.encode(pageTitle, "UTF-8")
            val url  = URL("https://tr.wikipedia.org/w/api.php?action=query&format=json" +
                           "&prop=revisions&rvprop=content&rvslots=main&redirects=1&titles=$enc")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout    = 6000
            conn.setRequestProperty("User-Agent", "AcademicManager/1.0 (Android)")

            if (conn.responseCode == 200) {
                val root   = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
                val pages  = root.optJSONObject("query")?.optJSONObject("pages")
                val pageId = pages?.keys()?.asSequence()?.firstOrNull() ?: return ""
                if (pageId == "-1") return ""
                pages.optJSONObject(pageId)
                    ?.optJSONArray("revisions")?.optJSONObject(0)
                    ?.optJSONObject("slots")?.optJSONObject("main")
                    ?.optString("*", "") ?: ""
            } else ""
        } catch (_: Exception) { "" }
    }

    // ── Üniversite ana sayfası: fakülte adları + fakülte wiki linkleri ────

    private fun parseUniversityPage(
        wikitext: String,
        universityName: String,
        result: MutableList<String>,
        facultyLinks: MutableList<String>
    ) {
        val sectionKw   = listOf("akademik", "fakülte", "enstitü", "yüksekokul", "birim", "okul", "konservatuvar")
        val headerRe    = Regex("^={2,}\\s*(.+?)\\s*={2,}\\s*$")
        val linkRe      = Regex("\\[\\[([^|\\]#]+?)(?:\\|([^\\]]+))?]]")
        val facultyKw   = listOf("fakülte", "enstitü", "yüksekokul", "okul", "konservatuvar", "merkez")

        var inSection = false
        for (raw in wikitext.lines()) {
            val line = raw.trim()
            val hm   = headerRe.find(line)
            if (hm != null) {
                inSection = sectionKw.any { hm.groupValues[1].lowercase().contains(it) }
                continue
            }
            if (!inSection || !line.startsWith("*")) continue

            val clean   = cleanLine(line)
            val content = clean.trimStart('*', ' ')
            if (content.isBlank()) continue

            val lm = linkRe.find(clean)
            if (lm != null) {
                val target  = lm.groupValues[1].trim()
                val display = lm.groupValues[2].ifBlank { target }
                    .substringBefore("{{")
                    .replace(universityName, "")
                    .replace(Regex("\\s*\\([^)]*\\)"), "")
                    .trim()

                // Fakülte/okul sayfalarını takip listesine al
                if (facultyKw.any { target.lowercase().contains(it) }) {
                    facultyLinks += target
                }
                if (isValid(display)) result += display
            } else {
                val name = content.replace(universityName, "").trim()
                if (isValid(name)) result += name
            }
        }
    }

    // ── Fakülte sayfası: sadece bölüm adlarını çıkar ─────────────────────

    private fun parseFacultyPage(wikitext: String): List<String> {
        val sectionKw = listOf("bölüm", "program", "akademik", "birim", "lisans", "anabilim", "dal", "programlar", "bölümleri")
        val headerRe  = Regex("^={2,}\\s*(.+?)\\s*={2,}\\s*$")
        val linkRe    = Regex("\\[\\[([^|\\]#]+?)(?:\\|([^\\]]+))?]]")
        val skipKw    = listOf("fakülte", "enstitü", "yüksekokul", "üniversite", "okul")

        val result    = mutableListOf<String>()
        var inSection = false
        for (raw in wikitext.lines()) {
            val line = raw.trim()
            val hm   = headerRe.find(line)
            if (hm != null) {
                inSection = sectionKw.any { hm.groupValues[1].lowercase().contains(it) }
                continue
            }
            // * ve # ile başlayan listeleri destekle
            if (!inSection || (!line.startsWith("*") && !line.startsWith("#"))) continue

            val clean   = cleanLine(line)
            val content = clean.trimStart('*', '#', ' ')
            if (content.isBlank()) continue

            val lm = linkRe.find(clean)
            val name = if (lm != null) {
                lm.groupValues[2].ifBlank { lm.groupValues[1] }
                    .substringBefore("{{")
                    .replace(Regex("\\s*\\([^)]*\\)"), "")
                    .trim()
            } else content.trim()

            val lower = name.lowercase()
            if (isValid(name) && skipKw.none { lower.contains(it) }) result += name
        }
        return result
    }

    // ── Yardımcılar ───────────────────────────────────────────────────────

    private fun cleanLine(line: String): String = line
        .substringBefore("{{")
        .replace(Regex("<ref[^>]*/?>.*?</ref>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<[^>]+>"), "")
        .trim()

    private fun isValid(name: String): Boolean =
        name.length > 3 &&
        !name.contains("{") && !name.contains("[") &&
        !name.contains("|") && !name.contains("http")

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace("[^\\p{ASCII}]".toRegex(), "")
            .replace("ş", "s").replace("ı", "i").replace("ğ", "g")
            .replace("ü", "u").replace("ö", "o").replace("ç", "c")
            .replace("i̇", "i")

    // ── Şehir → Üniversite statik haritası (tüm 81 il) ────────────────────

    private val CITY_TO_UNIVERSITIES: Map<String, List<String>> = mapOf(
        "Adana" to listOf("Çukurova Üniversitesi", "Adana Alparslan Türkeş Bilim ve Teknoloji Üniversitesi"),
        "Adıyaman" to listOf("Adıyaman Üniversitesi"),
        "Aksaray" to listOf("Aksaray Üniversitesi"),
        "Afyonkarahisar" to listOf("Afyon Kocatepe Üniversitesi"),
        "Ağrı" to listOf("Ağrı İbrahim Çeçen Üniversitesi"),
        "Amasya" to listOf("Amasya Üniversitesi"),
        "Ankara" to listOf(
            "Ankara Üniversitesi", "Orta Doğu Teknik Üniversitesi", "Hacettepe Üniversitesi",
            "Bilkent Üniversitesi", "Gazi Üniversitesi", "Yıldırım Beyazıt Üniversitesi",
            "TOBB Ekonomi ve Teknoloji Üniversitesi", "Başkent Üniversitesi", "Atılım Üniversitesi",
            "Çankaya Üniversitesi", "TED Üniversitesi", "Ostim Teknik Üniversitesi",
            "Ankara Sosyal Bilimler Üniversitesi", "Ankara Müzik ve Güzel Sanatlar Üniversitesi"
        ),
        "Antalya" to listOf("Akdeniz Üniversitesi", "Alanya Alaaddin Keykubat Üniversitesi", "Antalya Bilim Üniversitesi"),
        "Ardahan" to listOf("Ardahan Üniversitesi"),
        "Artvin" to listOf("Artvin Çoruh Üniversitesi"),
        "Aydın" to listOf("Aydın Adnan Menderes Üniversitesi"),
        "Balıkesir" to listOf("Balıkesir Üniversitesi", "Bandırma Onyedi Eylül Üniversitesi"),
        "Bartın" to listOf("Bartın Üniversitesi"),
        "Batman" to listOf("Batman Üniversitesi"),
        "Bayburt" to listOf("Bayburt Üniversitesi"),
        "Bilecik" to listOf("Bilecik Şeyh Edebali Üniversitesi"),
        "Bingöl" to listOf("Bingöl Üniversitesi"),
        "Bitlis" to listOf("Bitlis Eren Üniversitesi"),
        "Bolu" to listOf("Bolu Abant İzzet Baysal Üniversitesi"),
        "Burdur" to listOf("Burdur Mehmet Akif Ersoy Üniversitesi"),
        "Bursa" to listOf("Bursa Uludağ Üniversitesi", "Bursa Teknik Üniversitesi"),
        "Çanakkale" to listOf("Çanakkale Onsekiz Mart Üniversitesi"),
        "Çankırı" to listOf("Çankırı Karatekin Üniversitesi"),
        "Çorum" to listOf("Hitit Üniversitesi"),
        "Denizli" to listOf("Pamukkale Üniversitesi"),
        "Diyarbakır" to listOf("Dicle Üniversitesi"),
        "Düzce" to listOf("Düzce Üniversitesi"),
        "Edirne" to listOf("Trakya Üniversitesi"),
        "Elazığ" to listOf("Fırat Üniversitesi"),
        "Erzincan" to listOf("Erzincan Binali Yıldırım Üniversitesi"),
        "Erzurum" to listOf("Atatürk Üniversitesi"),
        "Eskişehir" to listOf("Anadolu Üniversitesi", "Eskişehir Osmangazi Üniversitesi", "Eskişehir Teknik Üniversitesi"),
        "Gaziantep" to listOf("Gaziantep Üniversitesi", "Gaziantep İslam Bilim ve Teknoloji Üniversitesi"),
        "Giresun" to listOf("Giresun Üniversitesi"),
        "Gümüşhane" to listOf("Gümüşhane Üniversitesi"),
        "Hakkari" to listOf("Hakkari Üniversitesi"),
        "Hatay" to listOf("Mustafa Kemal Üniversitesi", "İskenderun Teknik Üniversitesi"),
        "Iğdır" to listOf("Iğdır Üniversitesi"),
        "Isparta" to listOf("Süleyman Demirel Üniversitesi", "Isparta Uygulamalı Bilimler Üniversitesi"),
        "İçel (Mersin)" to listOf("Mersin Üniversitesi", "Tarsus Üniversitesi"),
        "İstanbul" to listOf(
            "İstanbul Üniversitesi", "İstanbul Teknik Üniversitesi", "Boğaziçi Üniversitesi",
            "Marmara Üniversitesi", "Yıldız Teknik Üniversitesi", "Galatasaray Üniversitesi",
            "Koç Üniversitesi", "Sabancı Üniversitesi", "Özyeğin Üniversitesi",
            "Yeditepe Üniversitesi", "Bahçeşehir Üniversitesi", "Kadir Has Üniversitesi",
            "İstanbul Bilgi Üniversitesi", "MEF Üniversitesi", "Altınbaş Üniversitesi",
            "Acıbadem Üniversitesi", "Medipol Üniversitesi", "İstanbul Medeniyet Üniversitesi",
            "Maltepe Üniversitesi", "Beykent Üniversitesi", "Beykoz Üniversitesi",
            "İstanbul Gelişim Üniversitesi", "Fatih Sultan Mehmet Vakıf Üniversitesi",
            "Nişantaşı Üniversitesi", "Atlas Üniversitesi", "Işık Üniversitesi",
            "İstanbul Esenyurt Üniversitesi", "İstinye Üniversitesi", "Halic Üniversitesi"
        ),
        "İzmir" to listOf(
            "Ege Üniversitesi", "Dokuz Eylül Üniversitesi", "İzmir Yüksek Teknoloji Enstitüsü",
            "Yaşar Üniversitesi", "İzmir Ekonomi Üniversitesi", "İzmir Kâtip Çelebi Üniversitesi",
            "İzmir Demokrasi Üniversitesi", "İzmir Bakırçay Üniversitesi"
        ),
        "Kahramanmaraş" to listOf("Kahramanmaraş Sütçü İmam Üniversitesi", "Kahramanmaraş İstiklal Üniversitesi"),
        "Karabük" to listOf("Karabük Üniversitesi"),
        "Karaman" to listOf("Karamanoğlu Mehmetbey Üniversitesi"),
        "Kars" to listOf("Kafkas Üniversitesi"),
        "Kastamonu" to listOf("Kastamonu Üniversitesi"),
        "Kayseri" to listOf("Erciyes Üniversitesi", "Abdullah Gül Üniversitesi", "Nuh Naci Yazgan Üniversitesi"),
        "Kırıkkale" to listOf("Kırıkkale Üniversitesi"),
        "Kırklareli" to listOf("Kırklareli Üniversitesi"),
        "Kırşehir" to listOf("Kırşehir Ahi Evran Üniversitesi"),
        "Kilis" to listOf("Kilis 7 Aralık Üniversitesi"),
        "Kocaeli" to listOf("Kocaeli Üniversitesi", "Gebze Teknik Üniversitesi"),
        "Konya" to listOf("Selçuk Üniversitesi", "Necmettin Erbakan Üniversitesi", "Konya Teknik Üniversitesi", "KTO Karatay Üniversitesi"),
        "Kütahya" to listOf("Kütahya Dumlupınar Üniversitesi", "Kütahya Sağlık Bilimleri Üniversitesi"),
        "Malatya" to listOf("İnönü Üniversitesi"),
        "Manisa" to listOf("Manisa Celal Bayar Üniversitesi"),
        "Mardin" to listOf("Mardin Artuklu Üniversitesi"),
        "Muğla" to listOf("Muğla Sıtkı Koçman Üniversitesi"),
        "Muş" to listOf("Muş Alparslan Üniversitesi"),
        "Nevşehir" to listOf("Nevşehir Hacı Bektaş Veli Üniversitesi"),
        "Niğde" to listOf("Niğde Ömer Halisdemir Üniversitesi"),
        "Ordu" to listOf("Ordu Üniversitesi"),
        "Osmaniye" to listOf("Osmaniye Korkut Ata Üniversitesi"),
        "Rize" to listOf("Recep Tayyip Erdoğan Üniversitesi"),
        "Sakarya" to listOf("Sakarya Üniversitesi", "Sakarya Uygulamalı Bilimler Üniversitesi"),
        "Samsun" to listOf("Ondokuz Mayıs Üniversitesi", "Samsun Üniversitesi"),
        "Siirt" to listOf("Siirt Üniversitesi"),
        "Sinop" to listOf("Sinop Üniversitesi"),
        "Sivas" to listOf("Sivas Cumhuriyet Üniversitesi", "Sivas Bilim ve Teknoloji Üniversitesi"),
        "Şanlıurfa" to listOf("Harran Üniversitesi"),
        "Şırnak" to listOf("Şırnak Üniversitesi"),
        "Tekirdağ" to listOf("Tekirdağ Namık Kemal Üniversitesi"),
        "Tokat" to listOf("Tokat Gaziosmanpaşa Üniversitesi"),
        "Trabzon" to listOf("Karadeniz Teknik Üniversitesi"),
        "Tunceli" to listOf("Munzur Üniversitesi"),
        "Uşak" to listOf("Uşak Üniversitesi"),
        "Van" to listOf("Van Yüzüncü Yıl Üniversitesi"),
        "Yalova" to listOf("Yalova Üniversitesi"),
        "Yozgat" to listOf("Yozgat Bozok Üniversitesi"),
        "Zonguldak" to listOf("Zonguldak Bülent Ecevit Üniversitesi")
    )

    // ── 81 İl listesi ──────────────────────────────────────────────────────

    companion object {
        val FALLBACK_DEPARTMENTS = listOf(
            "Bilgisayar Mühendisliği", "Elektrik-Elektronik Mühendisliği", "Makine Mühendisliği",
            "İnşaat Mühendisliği", "Endüstri Mühendisliği", "Kimya Mühendisliği",
            "Yazılım Mühendisliği", "Biyomedikal Mühendisliği", "Mimarlık",
            "İç Mimarlık", "Şehir ve Bölge Planlama",
            "Tıp", "Diş Hekimliği", "Eczacılık", "Hemşirelik", "Fizyoterapi",
            "Hukuk", "Siyaset Bilimi ve Kamu Yönetimi", "Uluslararası İlişkiler",
            "İşletme", "Ekonomi", "Finans ve Bankacılık", "Muhasebe",
            "Psikoloji", "Sosyoloji", "Tarih", "Felsefe", "Türk Dili ve Edebiyatı",
            "Matematik", "Fizik", "Kimya", "Biyoloji",
            "Eğitim Bilimleri", "Okul Öncesi Öğretmenliği", "Sınıf Öğretmenliği",
            "Güzel Sanatlar", "Müzik", "Sinema ve Televizyon",
            "Turizm İşletmeciliği", "Gastronomi", "Spor Bilimleri",
            "Çevre Mühendisliği", "Gıda Mühendisliği", "Harita Mühendisliği",
            "Medya ve İletişim", "Halkla İlişkiler", "Radyo, Televizyon ve Sinema",
            "Diğer"
        ).sorted()

        val TURKISH_CITIES = listOf(
            "Adana", "Adıyaman", "Afyonkarahisar", "Ağrı", "Amasya",
            "Ankara", "Antalya", "Artvin", "Aydın", "Balıkesir",
            "Bilecik", "Bingöl", "Bitlis", "Bolu", "Burdur",
            "Bursa", "Çanakkale", "Çankırı", "Çorum", "Denizli",
            "Diyarbakır", "Edirne", "Elazığ", "Erzincan", "Erzurum",
            "Eskişehir", "Gaziantep", "Giresun", "Gümüşhane", "Hakkari",
            "Hatay", "Isparta", "İçel (Mersin)", "İstanbul", "İzmir",
            "Kars", "Kastamonu", "Kayseri", "Kırklareli", "Kırşehir",
            "Kocaeli", "Konya", "Kütahya", "Malatya", "Manisa",
            "Kahramanmaraş", "Mardin", "Muğla", "Muş", "Nevşehir",
            "Niğde", "Ordu", "Rize", "Sakarya", "Samsun",
            "Siirt", "Sinop", "Sivas", "Tekirdağ", "Tokat",
            "Trabzon", "Tunceli", "Şanlıurfa", "Uşak", "Van",
            "Yozgat", "Zonguldak", "Aksaray", "Bayburt", "Karaman",
            "Kırıkkale", "Batman", "Şırnak", "Bartın", "Ardahan",
            "Iğdır", "Yalova", "Karabük", "Kilis", "Osmaniye", "Düzce"
        ).sorted()
    }
}

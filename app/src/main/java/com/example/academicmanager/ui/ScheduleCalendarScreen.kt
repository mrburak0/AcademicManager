package com.example.academicmanager.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.academicmanager.data.Lecturer
import com.example.academicmanager.data.ScheduleEntry
import com.example.academicmanager.data.SessionType
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.AdminViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CAL_DAYS  = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
private val CAL_DAYS_TR = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma")
private val CAL_DAYS_TR_SHORT = listOf("Pzt", "Sal", "Çar", "Per", "Cum")
private val CAL_SLOTS = listOf(
    "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00",
    "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00"
)

// Pastel renk paleti — her hoca farklı renk
private val PALETTE = listOf(
    Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444),
    Color(0xFF8B5CF6), Color(0xFF06B6D4), Color(0xFFF97316), Color(0xFF84CC16),
    Color(0xFFEC4899), Color(0xFF14B8A6)
)

private fun lecturerColor(name: String, all: List<String>): Color {
    val idx = all.indexOf(name).takeIf { it >= 0 } ?: (kotlin.math.abs(name.hashCode()) % PALETTE.size)
    return PALETTE[idx % PALETTE.size]
}

// ─────────────────────────────────────────────────────────────
// SCHEDULE CALENDAR SCREEN
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleCalendarScreen(adminVM: AdminViewModel, navController: NavController) {
    val allEntries  by adminVM.scheduleEntries.collectAsState()
    val lecturers   by adminVM.lecturers.collectAsState()
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()

    var selectedLecturer by remember { mutableStateOf<String?>(null) }
    var lecturerDropdown by remember { mutableStateOf(false) }
    var isPdfLoading     by remember { mutableStateOf(false) }

    val allLecturerNames = remember(allEntries) {
        allEntries.map { it.lecturerName }.distinct().sorted()
    }

    val filteredEntries = remember(allEntries, selectedLecturer) {
        if (selectedLecturer == null) allEntries else allEntries.filter { it.lecturerName == selectedLecturer }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Haftalık Takvim", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${filteredEntries.size} ders gösteriliyor", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = EmeraldGreen)
                    }
                },
                actions = {
                    // PDF export butonu
                    IconButton(
                        onClick = {
                            scope.launch {
                                isPdfLoading = true
                                val ok = withContext(Dispatchers.IO) {
                                    exportCalendarPdf(context, filteredEntries, allLecturerNames)
                                }
                                isPdfLoading = false
                                Toast.makeText(
                                    context,
                                    if (ok) "Takvim PDF olarak kaydedildi" else "PDF oluşturulamadı",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        enabled = !isPdfLoading
                    ) {
                        if (isPdfLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = EmeraldGreen, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF İndir", tint = EmeraldGreen)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = TextPrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Hoca filtresi ──────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = lecturerDropdown,
                onExpandedChange = { lecturerDropdown = !lecturerDropdown },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = selectedLecturer ?: "Tüm Hocalar",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Hoca Filtresi", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.FilterList, null, tint = EmeraldGreen, modifier = Modifier.size(18.dp)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lecturerDropdown) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen, unfocusedBorderColor = Slate700,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = lecturerDropdown,
                    onDismissRequest = { lecturerDropdown = false },
                    modifier = Modifier.background(Slate800)
                ) {
                    DropdownMenuItem(
                        text = { Text("Tüm Hocalar", color = EmeraldGreen, fontWeight = FontWeight.SemiBold) },
                        onClick = { selectedLecturer = null; lecturerDropdown = false }
                    )
                    allLecturerNames.forEach { name ->
                        val color = lecturerColor(name, allLecturerNames)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                                    Spacer(Modifier.width(8.dp))
                                    Text(name, color = TextPrimary)
                                }
                            },
                            onClick = { selectedLecturer = name; lecturerDropdown = false }
                        )
                    }
                }
            }

            // ── Renk legend ────────────────────────────────────────
            if (selectedLecturer == null && allLecturerNames.isNotEmpty()) {
                LecturerColorLegend(allLecturerNames)
            }

            // ── Takvim grid (yatay + dikey kaydırmalı) ────────────
            Box(modifier = Modifier.fillMaxSize()) {
                if (filteredEntries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.CalendarToday, null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                            Text("Gösterilecek ders yok", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                            Text("Demo veriyi yükleyin veya ders atayın.", color = TextSecondary.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    WeeklyCalendarGrid(filteredEntries, allLecturerNames)
                }
            }
        }
    }
}

@Composable
private fun LecturerColorLegend(names: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        names.forEach { name ->
            val color = lecturerColor(name, names)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                Text(name.substringAfterLast(" ").take(10), color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Kaydırılabilir haftalık grid ─────────────────────────────

private val TIME_COL_WIDTH: Dp = 62.dp
private val DAY_COL_WIDTH:  Dp = 160.dp
private val SLOT_HEIGHT:    Dp = 88.dp

@Composable
private fun WeeklyCalendarGrid(entries: List<ScheduleEntry>, allNames: List<String>) {
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    Column {
        // Sticky gün başlıkları (yatay kaydırılıyor ama dikey sabit)
        Row(modifier = Modifier.horizontalScroll(hScroll)) {
            Box(Modifier.width(TIME_COL_WIDTH)) // boş köşe
            CAL_DAYS.forEachIndexed { i, day ->
                Box(
                    modifier = Modifier
                        .width(DAY_COL_WIDTH)
                        .background(Slate800)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            CAL_DAYS_TR_SHORT[i],
                            color = PALETTE[i % PALETTE.size],
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            CAL_DAYS_TR[i],
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = TextSecondary.copy(alpha = 0.15f))

        // Zaman dilimleri + hücreler (her iki yönde kaydırılabilir)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(vScroll)
                .horizontalScroll(hScroll)
        ) {
            Row {
                // Saat etiketi sütunu
                Column {
                    CAL_SLOTS.forEach { slot ->
                        Box(
                            modifier = Modifier
                                .width(TIME_COL_WIDTH)
                                .height(SLOT_HEIGHT)
                                .background(Slate800.copy(alpha = 0.6f))
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                slot.take(5),
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Gün sütunları
                CAL_DAYS.forEach { day ->
                    Column(modifier = Modifier.width(DAY_COL_WIDTH)) {
                        CAL_SLOTS.forEach { slot ->
                            val dayEntries = entries.filter { it.dayOfWeek == day && it.timeSlot == slot }
                            CalendarCell(dayEntries, slot, allNames)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(entries: List<ScheduleEntry>, slot: String, allNames: List<String>) {
    Box(
        modifier = Modifier
            .width(DAY_COL_WIDTH)
            .height(SLOT_HEIGHT)
            .border(0.5.dp, TextSecondary.copy(alpha = 0.08f))
            .background(if (entries.isEmpty()) Color.Transparent else Color.Transparent)
            .padding(3.dp)
    ) {
        if (entries.isEmpty()) {
            // Boş hücre — hafif çizgi
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                entries.take(2).forEach { entry ->
                    val color = lecturerColor(entry.lecturerName, allNames)
                    val isLab = entry.sessionType == SessionType.LAB
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = if (isLab) 0.25f else 0.18f))
                            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Text(
                                entry.courseName,
                                color = color,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 10.sp
                            )
                            Text(
                                entry.lecturerName.split(" ").lastOrNull() ?: entry.lecturerName,
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    entry.classroomName,
                                    color = color.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isLab) {
                                    Box(
                                        Modifier.clip(RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.3f)).padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text("LAB", color = color, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                if (entries.size > 2) {
                    Text("+${entries.size - 2} daha", color = TextSecondary, fontSize = 8.sp, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PDF EXPORT
// ─────────────────────────────────────────────────────────────

private fun exportCalendarPdf(
    context: Context,
    entries: List<ScheduleEntry>,
    allNames: List<String>
): Boolean {
    return try {
        val pdfDoc = PdfDocument()
        // A4 landscape
        val pageWidth  = 842
        val pageHeight = 595
        val pageInfo   = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page       = pdfDoc.startPage(pageInfo)
        val canvas     = page.canvas

        val headerPaint = Paint().apply { isAntiAlias = true }
        val cellPaint   = Paint().apply { isAntiAlias = true }
        val textPaint   = Paint().apply { isAntiAlias = true; textSize = 7f }
        val boldPaint   = Paint().apply { isAntiAlias = true; textSize = 8f; typeface = Typeface.DEFAULT_BOLD }

        val marginTop  = 40f
        val marginLeft = 20f
        val timeColW   = 55f
        val dayColW    = (pageWidth - marginLeft - timeColW - 20f) / 5f
        val slotH      = (pageHeight - marginTop - 60f) / CAL_SLOTS.size
        val headerH    = 20f

        // Başlık
        boldPaint.textSize = 14f
        boldPaint.color = android.graphics.Color.parseColor("#10B981")
        canvas.drawText("Haftalık Ders Programı", marginLeft, marginTop - 12f, boldPaint)
        boldPaint.textSize = 8f

        // Gün başlıkları
        val headerY = marginTop + headerH
        headerPaint.color = android.graphics.Color.parseColor("#1E2535")
        canvas.drawRect(marginLeft, marginTop, pageWidth - 20f, headerY, headerPaint)

        CAL_DAYS_TR_SHORT.forEachIndexed { i, dayShort ->
            val x = marginLeft + timeColW + i * dayColW + dayColW / 2f
            boldPaint.color = android.graphics.Color.WHITE
            boldPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(dayShort, x, headerY - 6f, boldPaint)
        }

        // Grid çiz
        CAL_SLOTS.forEachIndexed { slotIdx, slot ->
            val y = headerY + slotIdx * slotH

            // Arka plan zebra
            cellPaint.color = if (slotIdx % 2 == 0) android.graphics.Color.parseColor("#111827") else android.graphics.Color.parseColor("#0F172A")
            canvas.drawRect(marginLeft, y, pageWidth - 20f, y + slotH, cellPaint)

            // Saat etiketi
            textPaint.color = android.graphics.Color.parseColor("#6B7280")
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(slot.take(5), marginLeft + 2f, y + slotH / 2f + 3f, textPaint)

            // Her gün için hücre içeriği
            CAL_DAYS.forEachIndexed { dayIdx, day ->
                val x = marginLeft + timeColW + dayIdx * dayColW
                val dayEntries = entries.filter { it.dayOfWeek == day && it.timeSlot == slot }

                // Hücre çerçevesi
                cellPaint.style = Paint.Style.STROKE
                cellPaint.strokeWidth = 0.5f
                cellPaint.color = android.graphics.Color.parseColor("#374151")
                canvas.drawRect(x, y, x + dayColW, y + slotH, cellPaint)
                cellPaint.style = Paint.Style.FILL

                if (dayEntries.isNotEmpty()) {
                    val entry = dayEntries.first()
                    val nameIdx = allNames.indexOf(entry.lecturerName).coerceAtLeast(0)
                    val hexColors = listOf("#6366F1", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#06B6D4", "#F97316", "#84CC16", "#EC4899", "#14B8A6")
                    val hex = hexColors[nameIdx % hexColors.size]

                    cellPaint.color = android.graphics.Color.parseColor(hex) and 0x00FFFFFF or 0x33000000
                    canvas.drawRect(x + 1f, y + 1f, x + dayColW - 1f, y + slotH - 1f, cellPaint)

                    boldPaint.color = android.graphics.Color.parseColor(hex)
                    boldPaint.textAlign = Paint.Align.LEFT
                    boldPaint.textSize = 7f
                    canvas.drawText(entry.courseName.take(20), x + 3f, y + 12f, boldPaint)

                    textPaint.color = android.graphics.Color.parseColor("#D1D5DB")
                    textPaint.textSize = 6f
                    canvas.drawText(entry.lecturerName.take(18), x + 3f, y + 22f, textPaint)
                    canvas.drawText(entry.classroomName, x + 3f, y + 31f, textPaint)
                    if (dayEntries.size > 1) {
                        textPaint.color = android.graphics.Color.parseColor("#9CA3AF")
                        canvas.drawText("+${dayEntries.size - 1} daha", x + 3f, y + 40f, textPaint)
                    }
                }
            }
        }

        pdfDoc.finishPage(page)

        // Downloads'a kaydet
        val fileName = "AcademicManager_Takvim_${System.currentTimeMillis()}.pdf"
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out -> pdfDoc.writeTo(out) }
        }
        pdfDoc.close()
        uri != null
    } catch (_: Exception) {
        false
    }
}

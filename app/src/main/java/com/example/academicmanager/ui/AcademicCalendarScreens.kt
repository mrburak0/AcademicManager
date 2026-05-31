package com.example.academicmanager.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.academicmanager.R
import com.example.academicmanager.data.*
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.AcademicCalendarViewModel
import com.example.academicmanager.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val CalendarBlue = Color(0xFF3B82F6)

// ─────────────────────────────────────────────────────────────
// ADMIN ACADEMIC CALENDAR SCREEN
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAcademicCalendarScreen(
    calendarViewModel: AcademicCalendarViewModel,
    navController: NavController
) {
    val events   by calendarViewModel.events.collectAsState()
    val snackbar  = remember { SnackbarHostState() }
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()

    var showAdd      by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<AcademicEvent?>(null) }
    var filterType   by remember { mutableStateOf<String?>(null) }
    var pdfUploading by remember { mutableStateOf(false) }

    // PDF yükle sonucu dinle
    LaunchedEffect(Unit) {
        calendarViewModel.pdfResult.collect { (ok, _) ->
            pdfUploading = false
            val msg = if (ok) "PDF başarıyla yüklendi" else "PDF yüklenemedi"
            snackbar.showSnackbar(msg)
        }
    }

    // PDF dosya seçici
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { calendarViewModel.uploadCalendarPdf(it, context); pdfUploading = true }
    }

    val today = LocalDate.now()
    val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val filtered = events
        .filter { filterType == null || it.eventType == filterType }
        .sortedWith(compareBy({ it.startDate }, { it.title }))
    val upcoming = filtered.filter { try { !LocalDate.parse(it.startDate, fmt).isBefore(today) } catch (_: Exception) { false } }
    val past     = filtered.filter { try { LocalDate.parse(it.startDate, fmt).isBefore(today) && (it.endDate.isBlank() || LocalDate.parse(it.endDate.ifBlank { it.startDate }, fmt).isBefore(today)) } catch (_: Exception) { false } }
    val ongoing  = filtered.filter { event ->
        try {
            val start = LocalDate.parse(event.startDate, fmt)
            val end   = if (event.endDate.isNotBlank()) LocalDate.parse(event.endDate, fmt) else start
            !today.isBefore(start) && !today.isAfter(end)
        } catch (_: Exception) { false }
    }

    Scaffold(
        containerColor = AppColorState.background,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(data, containerColor = CalendarBlue, contentColor = Color.White, shape = RoundedCornerShape(12.dp))
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // PDF Yükle butonu
                SmallFloatingActionButton(
                    onClick = { if (!pdfUploading) pdfPickerLauncher.launch("application/pdf") },
                    containerColor = AppColorState.surface2,
                    contentColor   = AppColorState.textPrimary,
                    shape          = RoundedCornerShape(12.dp)
                ) {
                    if (pdfUploading) CircularProgressIndicator(Modifier.size(18.dp), color = EmeraldGreen, strokeWidth = 2.dp)
                    else Icon(Icons.Default.UploadFile, contentDescription = "PDF Yükle")
                }
                // Etkinlik ekle butonu
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = CalendarBlue,
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(16.dp),
                    icon  = { Icon(Icons.Default.Add, null) },
                    text  = { Text(stringResource(R.string.add_event), fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(CalendarBlue.copy(alpha = 0.15f), Color.Transparent)))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppColorState.textPrimary)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.academic_calendar_title), color = AppColorState.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.academic_calendar_sub, events.size), color = AppColorState.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    if (ongoing.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = EmeraldGreen.copy(alpha = 0.15f)) {
                            Text("${ongoing.size} aktif", color = EmeraldGreen, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }

            // Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CalendarFilterChip(label = "Tümü", selected = filterType == null, color = CalendarBlue) { filterType = null }
                }
                items(EventType.all) { type ->
                    CalendarFilterChip(label = EventType.displayName(type), selected = filterType == type, color = Color(EventType.color(type))) { filterType = if (filterType == type) null else type }
                }
            }
            HorizontalDivider(color = AppColorState.surface2.copy(alpha = 0.4f))

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier = Modifier.size(96.dp).clip(CircleShape)
                                .background(Brush.radialGradient(listOf(CalendarBlue.copy(alpha = 0.15f), Color.Transparent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarMonth, null, tint = AppColorState.textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(46.dp))
                        }
                        Text(stringResource(R.string.no_events_yet), color = AppColorState.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.add_event_hint), color = AppColorState.textSecondary.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Ongoing section
                    if (ongoing.isNotEmpty()) {
                        item {
                            CalendarSectionHeader(stringResource(R.string.ongoing_events), EmeraldGreen, ongoing.size)
                        }
                        items(ongoing, key = { "ongoing_${it.id}" }) { event ->
                            AdminEventCard(event = event, isOngoing = true, onDelete = { deleteTarget = event })
                            Spacer(Modifier.height(8.dp))
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }

                    // Upcoming
                    if (upcoming.isNotEmpty()) {
                        item {
                            CalendarSectionHeader(stringResource(R.string.upcoming_events_cal), CalendarBlue, upcoming.size)
                        }
                        items(upcoming, key = { it.id }) { event ->
                            AdminEventCard(event = event, isOngoing = false, onDelete = { deleteTarget = event })
                            Spacer(Modifier.height(8.dp))
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }

                    // Past
                    if (past.isNotEmpty()) {
                        item {
                            CalendarSectionHeader(stringResource(R.string.past_events_cal), AppColorState.textSecondary, past.size, alpha = 0.5f)
                        }
                        items(past.reversed(), key = { "past_${it.id}" }) { event ->
                            AdminEventCard(event = event, isOngoing = false, isPast = true, onDelete = { deleteTarget = event })
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddAcademicEventDialog(
            onDismiss = { showAdd = false },
            onAdd     = { event ->
                calendarViewModel.addEvent(event)
                showAdd = false
            }
        )
    }

    deleteTarget?.let { event ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = AppColorState.surface,
            title = { Text(stringResource(R.string.delete_event_title), color = ErrorRed, fontWeight = FontWeight.Bold) },
            text  = { Text(event.title, color = AppColorState.textPrimary, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(onClick = { calendarViewModel.deleteEvent(event.id); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel), color = AppColorState.textSecondary) }
            }
        )
    }
}

@Composable
private fun CalendarFilterChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) color.copy(alpha = 0.18f) else AppColorState.surface,
        border = BorderStroke(1.dp, if (selected) color else AppColorState.surface2)
    ) {
        Text(label, color = if (selected) color else AppColorState.textSecondary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun CalendarSectionHeader(title: String, color: Color, count: Int, alpha: Float = 1f) {
    Row(modifier = Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color.copy(alpha = alpha)))
        Spacer(Modifier.width(10.dp))
        Text(title, color = AppColorState.textPrimary.copy(alpha = alpha), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(8.dp))
        Surface(shape = CircleShape, color = color.copy(alpha = alpha * 0.15f)) {
            Text(count.toString(), color = color.copy(alpha = alpha), modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminEventCard(
    event: AcademicEvent,
    isOngoing: Boolean,
    isPast: Boolean = false,
    onDelete: () -> Unit
) {
    val typeColor = Color(EventType.color(event.eventType))
    val alpha     = if (isPast) 0.45f else 1f
    val today     = LocalDate.now()
    val fmt       = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val daysAway  = try { ChronoUnit.DAYS.between(today, LocalDate.parse(event.startDate, fmt)) } catch (_: Exception) { null }

    val dateDisplay = buildString {
        append(formatEventDate(event.startDate))
        if (event.endDate.isNotBlank() && event.endDate != event.startDate) {
            append(" – ")
            append(formatEventDate(event.endDate))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = if (isPast) AppColorState.surface.copy(alpha = 0.6f) else AppColorState.surface),
        shape    = RoundedCornerShape(16.dp),
        border   = BorderStroke(1.dp, typeColor.copy(alpha = if (isPast) 0.1f else 0.28f))
    ) {
        Row {
            Box(modifier = Modifier.width(5.dp).fillMaxHeight()
                .background(Brush.verticalGradient(listOf(typeColor.copy(alpha = alpha), typeColor.copy(alpha = alpha * 0.3f))))
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = typeColor.copy(alpha = if (isPast) 0.08f else 0.15f)) {
                        Text(EventType.displayName(event.eventType), color = typeColor.copy(alpha = alpha), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    if (isOngoing) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = EmeraldGreen.copy(alpha = 0.15f)) {
                            Text(stringResource(R.string.ongoing_badge), color = EmeraldGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (!isPast && daysAway != null && daysAway > 0) {
                        Text("${daysAway}g", color = typeColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, null, tint = ErrorRed.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(event.title, color = AppColorState.textPrimary.copy(alpha = alpha), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CalendarMonth, null, tint = AppColorState.textSecondary.copy(alpha = alpha * 0.7f), modifier = Modifier.size(11.dp))
                    Text(dateDisplay, color = AppColorState.textSecondary.copy(alpha = alpha * 0.8f), style = MaterialTheme.typography.labelSmall)
                }
                if (event.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(event.description, color = AppColorState.textSecondary.copy(alpha = alpha * 0.7f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAcademicEventDialog(
    onDismiss: () -> Unit,
    onAdd: (AcademicEvent) -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate   by remember { mutableStateOf(today.format(fmt)) }
    var endDate     by remember { mutableStateOf("") }
    var eventType   by remember { mutableStateOf(EventType.SEMESTER_START) }
    var hasEndDate  by remember { mutableStateOf(false) }

    val parsedStart = remember(startDate) { try { LocalDate.parse(startDate, fmt) } catch (_: Exception) { today } }
    val parsedEnd   = remember(endDate) { try { LocalDate.parse(endDate, fmt) } catch (_: Exception) { parsedStart } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = AppColorState.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(CalendarBlue.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Event, null, tint = CalendarBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.add_event), color = AppColorState.textPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Title
                CalendarTextField(stringResource(R.string.event_title_field), title, { title = it }, Modifier.fillMaxWidth())

                // Event type chips
                Text(stringResource(R.string.event_type_label_cal), color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EventType.all.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { type ->
                                val tc  = Color(EventType.color(type))
                                val sel = eventType == type
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { eventType = type },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (sel) tc.copy(alpha = 0.18f) else AppColorState.surface2.copy(alpha = 0.5f),
                                    border = if (sel) BorderStroke(1.dp, tc) else BorderStroke(1.dp, Color.Transparent)
                                ) {
                                    Text(
                                        EventType.displayName(type),
                                        color = if (sel) tc else AppColorState.textSecondary,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Start date navigator
                Text(stringResource(R.string.event_start_date), color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                DateNavigator(startDate, fmt) { startDate = it; if (hasEndDate && parsedEnd.isBefore(parsedStart)) endDate = startDate }

                // End date toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = hasEndDate,
                        onCheckedChange = { hasEndDate = it; if (it && endDate.isBlank()) endDate = LocalDate.parse(startDate, fmt).plusDays(1).format(fmt) else if (!it) endDate = "" },
                        modifier = Modifier.size(36.dp),
                        colors = SwitchDefaults.colors(checkedThumbColor = CalendarBlue, checkedTrackColor = CalendarBlue.copy(alpha = 0.3f), uncheckedThumbColor = AppColorState.textSecondary, uncheckedTrackColor = AppColorState.surface2)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.event_has_end_date), color = AppColorState.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
                if (hasEndDate) {
                    Text(stringResource(R.string.event_end_date), color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                    DateNavigator(endDate.ifBlank { startDate }, fmt) { endDate = it }
                }

                // Description (optional)
                CalendarTextField(stringResource(R.string.event_description_opt), description, { description = it }, Modifier.fillMaxWidth(), maxLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    onAdd(AcademicEvent(title = title.trim(), description = description.trim(), startDate = startDate, endDate = if (hasEndDate) endDate else "", eventType = eventType))
                },
                colors = ButtonDefaults.buttonColors(containerColor = CalendarBlue),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add_event), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = AppColorState.textSecondary) }
        }
    )
}

@Composable
private fun DateNavigator(date: String, fmt: DateTimeFormatter, onDateChange: (String) -> Unit) {
    val parsed = remember(date) { try { LocalDate.parse(date, fmt) } catch (_: Exception) { LocalDate.now() } }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppColorState.surface2.copy(alpha = 0.6f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDateChange(parsed.minusDays(1).format(fmt)) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ChevronLeft, null, tint = AppColorState.textSecondary, modifier = Modifier.size(18.dp))
        }
        Text(date, color = AppColorState.textPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        IconButton(onClick = { onDateChange(parsed.plusDays(1).format(fmt)) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ChevronRight, null, tint = AppColorState.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CalendarTextField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier, maxLines: Int = 1, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier, shape = RoundedCornerShape(10.dp), maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CalendarBlue, unfocusedBorderColor = AppColorState.surface2,
            focusedLabelColor = CalendarBlue, focusedTextColor = AppColorState.textPrimary,
            unfocusedTextColor = AppColorState.textPrimary, cursorColor = CalendarBlue,
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
        )
    )
}

// ─────────────────────────────────────────────────────────────
// STUDENT/LECTURER ACADEMIC CALENDAR SCREEN (read-only)
// ─────────────────────────────────────────────────────────────

@Composable
fun AcademicCalendarScreen(
    authViewModel: AuthViewModel,
    calendarViewModel: AcademicCalendarViewModel,
    navController: NavController
) {
    val events   by calendarViewModel.events.collectAsState()
    val user      = authViewModel.currentUser
    val context   = LocalContext.current

    val today = LocalDate.now()
    val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    var filterType  by remember { mutableStateOf<String?>(null) }
    var showPast    by remember { mutableStateOf(false) }
    var pdfUrl      by remember { mutableStateOf<String?>(null) }
    var pdfLoading  by remember { mutableStateOf(false) }

    // PDF URL'ini yükle
    LaunchedEffect(Unit) {
        calendarViewModel.getCalendarPdfUrl { url -> pdfUrl = url }
    }

    val filtered = events.filter { filterType == null || it.eventType == filterType }
        .sortedWith(compareBy({ it.startDate }, { it.title }))

    val ongoing  = filtered.filter { ev ->
        try {
            val s = LocalDate.parse(ev.startDate, fmt)
            val e = if (ev.endDate.isNotBlank()) LocalDate.parse(ev.endDate, fmt) else s
            !today.isBefore(s) && !today.isAfter(e)
        } catch (_: Exception) { false }
    }
    val upcoming = filtered.filter { ev ->
        try { LocalDate.parse(ev.startDate, fmt).isAfter(today) } catch (_: Exception) { false }
    }
    val past = filtered.filter { ev ->
        try {
            val e = if (ev.endDate.isNotBlank()) LocalDate.parse(ev.endDate, fmt) else LocalDate.parse(ev.startDate, fmt)
            e.isBefore(today)
        } catch (_: Exception) { false }
    }

    val nextEvent   = upcoming.firstOrNull()
    val daysToNext  = nextEvent?.let { try { ChronoUnit.DAYS.between(today, LocalDate.parse(it.startDate, fmt)) } catch (_: Exception) { null } }

    Column(modifier = Modifier.fillMaxSize().background(AppColorState.background)) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(CalendarBlue.copy(alpha = 0.18f), Color.Transparent)))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppColorState.textPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.academic_calendar_title), color = AppColorState.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(user?.department ?: stringResource(R.string.all_departments), color = CalendarBlue.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                if (ongoing.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = EmeraldGreen.copy(alpha = 0.15f)) {
                        Text("${ongoing.size} aktif", color = EmeraldGreen, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                // PDF görüntüle butonu
                pdfUrl?.let { url ->
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "PDF açılamadı", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Görüntüle", tint = CalendarBlue, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(4.dp))
            }
        }

        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { CalendarFilterChip("Tümü", filterType == null, CalendarBlue) { filterType = null } }
            items(EventType.all) { type ->
                CalendarFilterChip(EventType.displayName(type), filterType == type, Color(EventType.color(type))) {
                    filterType = if (filterType == type) null else type
                }
            }
        }
        HorizontalDivider(color = AppColorState.surface2.copy(alpha = 0.4f))

        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.size(100.dp).clip(CircleShape)
                            .background(Brush.radialGradient(listOf(CalendarBlue.copy(alpha = 0.15f), Color.Transparent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, tint = AppColorState.textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(50.dp))
                    }
                    Text(stringResource(R.string.no_events_yet), color = AppColorState.textSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Ongoing banner
                if (ongoing.isNotEmpty()) {
                    item {
                        OngoingEventsBanner(ongoing)
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Next event hero
                if (nextEvent != null && daysToNext != null) {
                    item {
                        NextEventHeroCard(event = nextEvent, daysToNext = daysToNext)
                        Spacer(Modifier.height(18.dp))
                    }
                }

                // Upcoming
                if (upcoming.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = CalendarBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.upcoming_events_cal), color = AppColorState.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = CircleShape, color = CalendarBlue.copy(alpha = 0.15f)) {
                                Text(upcoming.size.toString(), color = CalendarBlue, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    items(upcoming, key = { it.id }) { event ->
                        StudentEventCard(event = event, today = today, fmt = fmt)
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // Past toggle
                if (past.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(AppColorState.surface.copy(alpha = 0.5f))
                                .clickable { showPast = !showPast }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, null, tint = AppColorState.textSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.past_events_cal, past.size), color = AppColorState.textSecondary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Icon(if (showPast) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = AppColorState.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (showPast) {
                        items(past.reversed(), key = { "past_${it.id}" }) { event ->
                            StudentEventCard(event = event, today = today, fmt = fmt, isPast = true)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun OngoingEventsBanner(ongoing: List<AcademicEvent>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.08f)),
        shape    = RoundedCornerShape(16.dp),
        border   = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldGreen))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ongoing_events), color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            ongoing.forEach { ev ->
                val typeColor = Color(EventType.color(ev.eventType))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(typeColor))
                    Text(ev.title, color = AppColorState.textPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Surface(shape = RoundedCornerShape(6.dp), color = typeColor.copy(alpha = 0.15f)) {
                        Text(EventType.displayName(ev.eventType), color = typeColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun NextEventHeroCard(event: AcademicEvent, daysToNext: Long) {
    val typeColor  = Color(EventType.color(event.eventType))
    val isToday    = daysToNext == 0L
    val gradBrush  = Brush.linearGradient(listOf(typeColor.copy(alpha = 0.25f), typeColor.copy(alpha = 0.08f), AppColorState.surface))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape    = RoundedCornerShape(24.dp),
        border   = BorderStroke(1.dp, typeColor.copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(gradBrush)) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = typeColor.copy(alpha = 0.18f)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = typeColor, modifier = Modifier.size(10.dp))
                            Text(stringResource(R.string.next_event_label), color = typeColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = typeColor.copy(alpha = 0.12f)) {
                        Text(EventType.displayName(event.eventType), color = typeColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(event.title, color = AppColorState.textPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        val dateDisplay = buildString {
                            append(formatEventDate(event.startDate))
                            if (event.endDate.isNotBlank() && event.endDate != event.startDate) {
                                append(" – ")
                                append(formatEventDate(event.endDate))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CalendarMonth, null, tint = AppColorState.textSecondary, modifier = Modifier.size(12.dp))
                            Text(dateDisplay, color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                        if (event.description.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(event.description, color = AppColorState.textSecondary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Surface(
                        shape  = RoundedCornerShape(16.dp),
                        color  = typeColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isToday) {
                                Text("BUGÜN", color = typeColor, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                            } else {
                                Text(daysToNext.toString(), color = typeColor, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                                Text(stringResource(R.string.days_remaining), color = typeColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentEventCard(
    event: AcademicEvent,
    today: LocalDate,
    fmt: DateTimeFormatter,
    isPast: Boolean = false
) {
    val typeColor = Color(EventType.color(event.eventType))
    val alpha     = if (isPast) 0.45f else 1f
    val daysLeft  = try { ChronoUnit.DAYS.between(today, LocalDate.parse(event.startDate, fmt)) } catch (_: Exception) { null }

    val dateDisplay = buildString {
        append(try { LocalDate.parse(event.startDate, fmt).format(DateTimeFormatter.ofPattern("d MMM, EEEE", java.util.Locale("tr","TR"))) } catch (_: Exception) { event.startDate })
        if (event.endDate.isNotBlank() && event.endDate != event.startDate) {
            append(" – ")
            append(try { LocalDate.parse(event.endDate, fmt).format(DateTimeFormatter.ofPattern("d MMM", java.util.Locale("tr","TR"))) } catch (_: Exception) { event.endDate })
        }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
            Box(modifier = Modifier.size(11.dp).clip(CircleShape).background(typeColor.copy(alpha = alpha)))
        }
        Spacer(Modifier.width(8.dp))
        Card(
            modifier = Modifier.weight(1f),
            colors   = CardDefaults.cardColors(containerColor = if (isPast) AppColorState.surface.copy(alpha = 0.5f) else AppColorState.surface),
            shape    = RoundedCornerShape(16.dp),
            border   = if (!isPast) BorderStroke(1.dp, typeColor.copy(alpha = 0.2f)) else null
        ) {
            Row {
                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(typeColor.copy(alpha = alpha)).clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(6.dp), color = typeColor.copy(alpha = if (isPast) 0.08f else 0.14f)) {
                            Text(EventType.displayName(event.eventType), color = typeColor.copy(alpha = alpha), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.weight(1f))
                        if (!isPast && daysLeft != null && daysLeft > 0) {
                            Surface(shape = RoundedCornerShape(6.dp), color = typeColor.copy(alpha = 0.10f)) {
                                Text("+${daysLeft}g", color = typeColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(event.title, color = AppColorState.textPrimary.copy(alpha = alpha), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CalendarMonth, null, tint = AppColorState.textSecondary.copy(alpha = alpha * 0.7f), modifier = Modifier.size(11.dp))
                        Text(dateDisplay, color = AppColorState.textSecondary.copy(alpha = alpha * 0.8f), style = MaterialTheme.typography.labelSmall)
                    }
                    if (event.description.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(event.description, color = AppColorState.textSecondary.copy(alpha = alpha * 0.7f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private fun formatEventDate(date: String): String {
    return try {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        LocalDate.parse(date, fmt).format(DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale("tr","TR")))
    } catch (_: Exception) { date }
}

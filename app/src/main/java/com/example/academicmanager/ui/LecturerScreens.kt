package com.example.academicmanager.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.academicmanager.R
import androidx.navigation.NavController
import com.example.academicmanager.data.AvailabilityStatus
import com.example.academicmanager.data.LecturerAvailability
import com.example.academicmanager.data.ScheduleEntry
import com.example.academicmanager.data.SessionType
import com.example.academicmanager.data.UserRole
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.AdminViewModel
import com.example.academicmanager.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Data lists — English strings for Firestore data operations, DO NOT localize
private val WEEK_DAYS_FULL = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
private val WEEK_DAYS_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
private val SCHEDULE_TIME_SLOTS = listOf(
    "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00",
    "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00"
)

// Display composable helpers for localized day names
@Composable
private fun weekDaysFull() = listOf(
    stringResource(R.string.day_monday), stringResource(R.string.day_tuesday),
    stringResource(R.string.day_wednesday), stringResource(R.string.day_thursday),
    stringResource(R.string.day_friday)
)

@Composable
private fun weekDaysShort() = listOf(
    stringResource(R.string.day_mon), stringResource(R.string.day_tue),
    stringResource(R.string.day_wed), stringResource(R.string.day_thu),
    stringResource(R.string.day_fri)
)

// Day accent colors for the calendar grid
private val DAY_COLORS = listOf(
    Color(0xFF6366F1), // Monday – Indigo
    Color(0xFF10B981), // Tuesday – Emerald
    Color(0xFFF59E0B), // Wednesday – Amber
    Color(0xFFEF4444), // Thursday – Red
    Color(0xFF8B5CF6)  // Friday – Violet
)

// ─────────────────────────────────────────────────────────────
// LECTURER HOME SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun LecturerHomeScreen(authViewModel: AuthViewModel, adminViewModel: AdminViewModel, navController: NavController) {
    val user = authViewModel.currentUser ?: return
    val allEntries by adminViewModel.scheduleEntries.collectAsState()
    val myEntries = allEntries.filter { it.lecturerName == user.fullName }

    // Group by day for the weekly summary (using English keys for data lookup)
    val coursesByDay = WEEK_DAYS_FULL.associateWith { day -> myEntries.filter { it.dayOfWeek == day } }
    val displayDaysFull = weekDaysFull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Welcome Header ───────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = EmeraldGreen.copy(alpha = 0.12f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreen.copy(alpha = 0.25f))
                        .border(2.dp, EmeraldGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user.fullName.firstOrNull()?.uppercase() ?: "?",
                        color = EmeraldGreen,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        buildString {
                            append(stringResource(R.string.welcome_prefix))
                            if (user.title.isNotBlank()) append("${user.title} ")
                            append(user.fullName)
                        },
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (user.department.isNotBlank() && user.department != "General") {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            user.department,
                            color = EmeraldGreen,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                // Duyurular bell ikonu
                IconButton(onClick = { navController.navigate("announcements") }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Duyurular", tint = EmeraldGreen, modifier = Modifier.size(22.dp))
                }
            }
        }

        // ── Stats Row ────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LecturerStatCard(
                label = stringResource(R.string.stat_total_courses),
                value = myEntries.size.toString(),
                icon = Icons.AutoMirrored.Filled.List,
                color = EmeraldGreen,
                modifier = Modifier.weight(1f)
            )
            LecturerStatCard(
                label = stringResource(R.string.stat_days_active),
                value = myEntries.map { it.dayOfWeek }.distinct().size.toString(),
                icon = Icons.Default.DateRange,
                color = IndigoAccent,
                modifier = Modifier.weight(1f)
            )
            LecturerStatCard(
                label = stringResource(R.string.stat_working_type),
                value = user.workingType.take(4).ifBlank { "N/A" },
                icon = Icons.Default.Person,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }

        // ── Weekly Schedule ──────────────────────────────────
        Text(
            stringResource(R.string.this_week),
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (myEntries.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.no_courses_assigned),
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.contact_admin),
                        color = TextSecondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Per-day sections
            WEEK_DAYS_FULL.forEachIndexed { dayIndex, day ->
                val dayEntries = coursesByDay[day] ?: emptyList()
                if (dayEntries.isNotEmpty()) {
                    DaySection(
                        day = displayDaysFull[dayIndex],
                        entries = dayEntries,
                        accentColor = DAY_COLORS[dayIndex],
                        entriesCountStr = if (dayEntries.size == 1)
                            stringResource(R.string.course_count_one, dayEntries.size)
                        else
                            stringResource(R.string.courses_count, dayEntries.size)
                    )
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun LecturerStatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun DaySection(day: String, entries: List<ScheduleEntry>, accentColor: Color, entriesCountStr: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                day,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.width(8.dp))
            Text(
                entriesCountStr,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
        entries.sortedBy { it.timeSlot }.forEach { entry ->
            LecturerCourseCard(entry, accentColor)
        }
    }
}

@Composable
private fun LecturerCourseCard(entry: ScheduleEntry, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.courseName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    entry.courseCode,
                    color = accentColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    entry.timeSlot,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    entry.classroomName,
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// LECTURER CALENDAR SCREEN  (Day-tab timeline design)
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturerCalendarScreen(authViewModel: AuthViewModel, adminViewModel: AdminViewModel) {
    val user = authViewModel.currentUser ?: return
    val allEntries      by adminViewModel.scheduleEntries.collectAsState()
    val allRequests     by adminViewModel.scheduleRequests.collectAsState()
    val courses         by adminViewModel.courses.collectAsState()
    val classrooms      by adminViewModel.classrooms.collectAsState()
    val myEntries  = allEntries.filter { it.lecturerName == user.fullName }
    val myRequests = allRequests.filter { it.lecturerUsername == user.username }

    val displayDaysFull  = weekDaysFull()
    val displayDaysShort = weekDaysShort()

    // Auto-select today (Mon=0 … Fri=4, weekend → Mon)
    val todayIndex = remember {
        val dow = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        when (dow) {
            java.util.Calendar.MONDAY    -> 0
            java.util.Calendar.TUESDAY   -> 1
            java.util.Calendar.WEDNESDAY -> 2
            java.util.Calendar.THURSDAY  -> 3
            java.util.Calendar.FRIDAY    -> 4
            else                         -> 0
        }
    }
    var selectedDay by remember { mutableIntStateOf(todayIndex) }

    // Use English data keys for filtering
    val dayEntries = myEntries
        .filter { it.dayOfWeek == WEEK_DAYS_FULL[selectedDay] }
        .sortedBy { SCHEDULE_TIME_SLOTS.indexOf(it.timeSlot) }

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ── Request FAB Dialog state ──────────────────────────────
    var showRequestDialog   by remember { mutableStateOf(false) }
    var reqSelectedCourse   by remember { mutableStateOf<com.example.academicmanager.data.Course?>(null) }
    var reqSelectedClass    by remember { mutableStateOf<com.example.academicmanager.data.Classroom?>(null) }
    var reqSelectedTime     by remember { mutableStateOf("") }
    var reqNote             by remember { mutableStateOf("") }
    var courseDropExpanded  by remember { mutableStateOf(false) }
    var classDropExpanded   by remember { mutableStateOf(false) }
    var timeDropExpanded    by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showRequestDialog = true },
                containerColor = EmeraldGreen,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.EventAvailable, contentDescription = stringResource(R.string.notify_availability))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Title + PDF Button ──────────────────────────────
            Row(
                modifier             = Modifier.fillMaxWidth(),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.my_schedule), style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.sessions_per_week, myEntries.size), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            exportSchedulePdf(context, myEntries, user.fullName)
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                    border = BorderStroke(1.dp, EmeraldGreen),
                    shape  = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.pdf_btn), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Day Selector Tabs ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WEEK_DAYS_FULL.forEachIndexed { idx, day ->
                    val isSelected = idx == selectedDay
                    val isToday    = idx == todayIndex
                    val count      = myEntries.count { it.dayOfWeek == day }
                    val tabColor   = DAY_COLORS[idx]

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when {
                                    isSelected -> tabColor
                                    isToday    -> tabColor.copy(alpha = 0.10f)
                                    else       -> Slate800
                                }
                            )
                            .then(
                                if (isToday && !isSelected)
                                    Modifier.border(1.dp, tabColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                else Modifier
                            )
                            .clickable { selectedDay = idx }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            displayDaysShort[idx],
                            color = if (isSelected) Color.White else if (isToday) tabColor else TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.height(4.dp))
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.25f)
                                        else tabColor.copy(alpha = 0.18f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    count.toString(),
                                    color = if (isSelected) Color.White else tabColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        } else {
                            Spacer(Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Day title ────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(DAY_COLORS[selectedDay])
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    displayDaysFull[selectedDay],
                    color = DAY_COLORS[selectedDay],
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (dayEntries.isEmpty()) stringResource(R.string.no_sessions)
                    else if (dayEntries.size == 1) stringResource(R.string.sessions_count_one, dayEntries.size)
                    else stringResource(R.string.sessions_count, dayEntries.size),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Timeline ─────────────────────────────────────────
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(SCHEDULE_TIME_SLOTS) { slot ->
                    val pendingReqForSlot = myRequests.find {
                        it.proposedDay == WEEK_DAYS_FULL[selectedDay] &&
                        it.proposedTimeSlot == slot &&
                        it.status == com.example.academicmanager.data.RequestStatus.PENDING
                    }
                    TimelineRow(
                        timeSlot    = slot,
                        entry       = dayEntries.find { it.timeSlot == slot },
                        accentColor = DAY_COLORS[selectedDay],
                        pendingRequest = pendingReqForSlot
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // ── Request Dialog ────────────────────────────────────────
    if (showRequestDialog) {
        AlertDialog(
            onDismissRequest = {
                showRequestDialog = false
                reqSelectedCourse = null; reqSelectedClass = null; reqSelectedTime = ""; reqNote = ""
            },
            containerColor = com.example.academicmanager.ui.theme.Slate800,
            title = { Text(stringResource(R.string.availability_dialog_title), color = EmeraldGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.availability_desc), color = com.example.academicmanager.ui.theme.TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.available_day_label, displayDaysFull[selectedDay]), color = EmeraldGreen, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    }

                    ExposedDropdownMenuBox(expanded = timeDropExpanded, onExpandedChange = { timeDropExpanded = !timeDropExpanded }) {
                        OutlinedTextField(
                            value = reqSelectedTime.ifEmpty { stringResource(R.string.available_time_hint) },
                            onValueChange = {}, readOnly = true,
                            label = { Text(stringResource(R.string.available_time_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeDropExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, focusedLabelColor = EmeraldGreen)
                        )
                        ExposedDropdownMenu(expanded = timeDropExpanded, onDismissRequest = { timeDropExpanded = false }, modifier = Modifier.background(com.example.academicmanager.ui.theme.Slate800)) {
                            SCHEDULE_TIME_SLOTS.forEach { slot ->
                                DropdownMenuItem(text = { Text(slot, color = com.example.academicmanager.ui.theme.TextPrimary) }, onClick = { reqSelectedTime = slot; timeDropExpanded = false })
                            }
                        }
                    }

                    ExposedDropdownMenuBox(expanded = courseDropExpanded, onExpandedChange = { courseDropExpanded = !courseDropExpanded }) {
                        OutlinedTextField(
                            value = reqSelectedCourse?.let { "${it.courseCode} – ${it.courseName}" } ?: stringResource(R.string.preferred_course_hint),
                            onValueChange = {}, readOnly = true,
                            label = { Text(stringResource(R.string.preferred_course_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, focusedLabelColor = EmeraldGreen)
                        )
                        ExposedDropdownMenu(expanded = courseDropExpanded, onDismissRequest = { courseDropExpanded = false }, modifier = Modifier.background(com.example.academicmanager.ui.theme.Slate800)) {
                            courses.forEach { c ->
                                DropdownMenuItem(text = { Text("${c.courseCode} – ${c.courseName}", color = com.example.academicmanager.ui.theme.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) }, onClick = { reqSelectedCourse = c; courseDropExpanded = false })
                            }
                        }
                    }

                    ExposedDropdownMenuBox(expanded = classDropExpanded, onExpandedChange = { classDropExpanded = !classDropExpanded }) {
                        OutlinedTextField(
                            value = reqSelectedClass?.name ?: stringResource(R.string.preferred_class_hint),
                            onValueChange = {}, readOnly = true,
                            label = { Text(stringResource(R.string.preferred_class_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, focusedLabelColor = EmeraldGreen)
                        )
                        ExposedDropdownMenu(expanded = classDropExpanded, onDismissRequest = { classDropExpanded = false }, modifier = Modifier.background(com.example.academicmanager.ui.theme.Slate800)) {
                            classrooms.forEach { cl ->
                                DropdownMenuItem(text = { Text("${cl.name} (kap: ${cl.capacity})", color = com.example.academicmanager.ui.theme.TextPrimary) }, onClick = { reqSelectedClass = cl; classDropExpanded = false })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reqNote, onValueChange = { reqNote = it },
                        label = { Text(stringResource(R.string.note_label)) },
                        placeholder = { Text(stringResource(R.string.note_hint), color = com.example.academicmanager.ui.theme.TextSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, unfocusedBorderColor = com.example.academicmanager.ui.theme.Slate700, focusedLabelColor = EmeraldGreen, unfocusedLabelColor = com.example.academicmanager.ui.theme.TextSecondary, focusedTextColor = com.example.academicmanager.ui.theme.TextPrimary, unfocusedTextColor = com.example.academicmanager.ui.theme.TextPrimary, cursorColor = EmeraldGreen, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val course = reqSelectedCourse
                        val cls = reqSelectedClass
                        if (course != null && cls != null && reqSelectedTime.isNotEmpty()) {
                            adminViewModel.addScheduleRequest(
                                com.example.academicmanager.data.ScheduleRequest(
                                    courseCode        = course.courseCode,
                                    courseName        = course.courseName,
                                    lecturerUsername  = user.username,
                                    lecturerName      = user.fullName,
                                    proposedDay       = WEEK_DAYS_FULL[selectedDay],
                                    proposedTimeSlot  = reqSelectedTime,
                                    proposedClassroom = cls.name,
                                    lecturerNote      = reqNote
                                )
                            )
                            showRequestDialog = false
                            reqSelectedCourse = null; reqSelectedClass = null; reqSelectedTime = ""; reqNote = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) { Text(stringResource(R.string.notify_btn)) }
            },
            dismissButton = {
                TextButton(onClick = { showRequestDialog = false; reqSelectedCourse = null; reqSelectedClass = null; reqSelectedTime = ""; reqNote = "" }) {
                    Text(stringResource(R.string.cancel), color = com.example.academicmanager.ui.theme.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun TimelineRow(
    timeSlot: String,
    entry: ScheduleEntry?,
    accentColor: Color,
    pendingRequest: com.example.academicmanager.data.ScheduleRequest? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (entry != null || pendingRequest != null) 10.dp else 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        // ── Time label ──────────────────────────────────────
        Text(
            timeSlot.take(5),
            color = if (entry != null) accentColor else TextSecondary.copy(alpha = 0.40f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (entry != null) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .width(54.dp)
                .padding(top = 7.dp),
            fontSize = 11.sp
        )

        // ── Dot + vertical line ──────────────────────────────
        val hasPending = pendingRequest != null && entry == null
        val pendingAmber = Color(0xFFF59E0B)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(if (entry != null || hasPending) 11.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            entry != null -> accentColor
                            hasPending    -> pendingAmber
                            else          -> TextSecondary.copy(alpha = 0.18f)
                        }
                    )
            )
            Box(
                modifier = Modifier
                    .width(if (entry != null || hasPending) 2.dp else 1.dp)
                    .height(if (entry != null || hasPending) 68.dp else 26.dp)
                    .background(
                        when {
                            entry != null -> accentColor.copy(alpha = 0.22f)
                            hasPending    -> pendingAmber.copy(alpha = 0.22f)
                            else          -> TextSecondary.copy(alpha = 0.07f)
                        }
                    )
            )
        }

        Spacer(Modifier.width(12.dp))

        // ── Content card or empty spacer ─────────────────────
        if (entry != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.09f)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            entry.courseName,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    entry.courseCode,
                                    color = accentColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.55f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                entry.classroomName,
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.13f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            entry.timeSlot.replace("-", "\n"),
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        } else if (hasPending && pendingRequest != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = pendingAmber.copy(alpha = 0.09f)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, pendingAmber.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = pendingAmber, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.available_awaiting), color = pendingAmber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(pendingRequest.courseName, color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.05f), thickness = 1.dp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PDF EXPORT
// ─────────────────────────────────────────────────────────────

private suspend fun exportSchedulePdf(
    context: Context,
    entries: List<ScheduleEntry>,
    ownerName: String
) = withContext(Dispatchers.IO) {
    try {
        val pdfDoc    = android.graphics.pdf.PdfDocument()
        val pageWidth = 842   // A4 landscape
        val pageHeight = 595
        val pageInfo  = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page      = pdfDoc.startPage(pageInfo)
        val canvas    = page.canvas

        val normalFace = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        val boldFace   = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

        val titlePaint = Paint().apply { color = android.graphics.Color.rgb(16, 185, 129); textSize = 18f; typeface = boldFace; isAntiAlias = true }
        val subPaint   = Paint().apply { color = android.graphics.Color.rgb(148, 163, 184); textSize = 10f; typeface = normalFace; isAntiAlias = true }
        val headerBgPt = Paint().apply { color = android.graphics.Color.rgb(30, 41, 59); style = Paint.Style.FILL }
        val whiteText  = Paint().apply { color = android.graphics.Color.WHITE; textSize = 8f; typeface = boldFace; isAntiAlias = true }
        val cellBg     = Paint().apply { color = android.graphics.Color.rgb(16, 185, 129); style = Paint.Style.FILL; alpha = 35 }
        val cellText   = Paint().apply { color = android.graphics.Color.rgb(30, 41, 59); textSize = 7f; typeface = boldFace; isAntiAlias = true }
        val cellSub    = Paint().apply { color = android.graphics.Color.rgb(100, 116, 139); textSize = 6f; typeface = normalFace; isAntiAlias = true }
        val gridPaint  = Paint().apply { color = android.graphics.Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 0.5f }
        val labBg      = Paint().apply { color = android.graphics.Color.rgb(245, 158, 11); style = Paint.Style.FILL; alpha = 40 }

        canvas.drawText(context.getString(R.string.weekly_schedule_pdf), 20f, 28f, titlePaint)
        canvas.drawText(ownerName, 20f, 44f, subPaint)
        canvas.drawText(java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date()), 20f, 56f, subPaint)

        val days      = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma")
        val dayKeys   = WEEK_DAYS_FULL
        val slots     = SCHEDULE_TIME_SLOTS
        val startX    = 20f
        val startY    = 68f
        val colW      = (pageWidth - startX * 2) / (days.size + 1)
        val rowH      = (pageHeight - startY - 20f) / (slots.size + 1)

        days.forEachIndexed { i, day ->
            val x = startX + (i + 1) * colW
            canvas.drawRect(x, startY, x + colW, startY + rowH, headerBgPt)
            canvas.drawText(day, x + 4f, startY + rowH / 2f + 3f, whiteText)
        }
        canvas.drawRect(startX, startY, startX + colW, startY + rowH, headerBgPt)
        canvas.drawText("Saat", startX + 4f, startY + rowH / 2f + 3f, whiteText)

        slots.forEachIndexed { row, slot ->
            val y = startY + (row + 1) * rowH

            canvas.drawRect(startX, y, startX + colW, y + rowH,
                Paint().apply { color = android.graphics.Color.rgb(51, 65, 85); style = Paint.Style.FILL })
            canvas.drawText(slot, startX + 2f, y + rowH / 2f + 3f,
                Paint().apply { color = android.graphics.Color.WHITE; textSize = 6.5f; typeface = normalFace; isAntiAlias = true })

            days.forEachIndexed { col, _ ->
                val x     = startX + (col + 1) * colW
                val entry = entries.find { it.dayOfWeek == dayKeys[col] && it.timeSlot == slot }
                if (entry != null) {
                    val bg = if (entry.sessionType == SessionType.LAB) labBg else cellBg
                    canvas.drawRect(x, y, x + colW, y + rowH, bg)
                    val name = if (entry.courseName.length > 16) entry.courseName.take(14) + "…" else entry.courseName
                    canvas.drawText(name, x + 2f, y + rowH * 0.35f + 3f, cellText)
                    canvas.drawText(entry.classroomName, x + 2f, y + rowH * 0.65f + 3f, cellSub)
                    if (entry.sessionType == SessionType.LAB) {
                        canvas.drawText("LAB", x + colW - 18f, y + 9f,
                            Paint().apply { color = android.graphics.Color.rgb(245, 158, 11); textSize = 6f; typeface = boldFace; isAntiAlias = true })
                    }
                }
                canvas.drawRect(x, y, x + colW, y + rowH, gridPaint)
            }
        }

        pdfDoc.finishPage(page)

        val fileName = "program_${System.currentTimeMillis()}.pdf"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cv = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { pdfDoc.writeTo(it) }
                cv.clear(); cv.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, cv, null, null)
            }
        } else {
            val dir  = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val file = File(dir, fileName)
            FileOutputStream(file).use { pdfDoc.writeTo(it) }
        }

        pdfDoc.close()

        withContext(Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.pdf_downloaded, fileName), Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.pdf_failed, e.message), Toast.LENGTH_LONG).show()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// LECTURER AVAILABILITY SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun LecturerAvailabilityScreen(authViewModel: AuthViewModel, adminViewModel: AdminViewModel) {
    val user = authViewModel.currentUser ?: return
    val allAvailabilities by adminViewModel.availabilities.collectAsState()
    val myAvailabilities  = allAvailabilities
        .filter { it.lecturerUsername == user.username }
        .sortedByDescending { it.timestamp }
    // Onaylı harita önce, yoksa en son gönderim
    val approvedMine = myAvailabilities.firstOrNull { it.status == AvailabilityStatus.APPROVED }
    val latestMine   = approvedMine ?: myAvailabilities.firstOrNull()

    val displayDays  = weekDaysShort()
    val slots        = SCHEDULE_TIME_SLOTS

    // İlk kez mi gönderiliyor? (daha önce hiç gönderilmemişse true)
    val isFirstTime = myAvailabilities.isEmpty()

    // selectedSlots[dayIdx] = set of slotIdx — mevcut onaylı haritadan başlat
    var selectedSlots        by remember { mutableStateOf<Map<Int, Set<Int>>>(emptyMap()) }
    var initialized          by remember { mutableStateOf(false) }
    var isSaving             by remember { mutableStateOf(false) }
    var showFirstTimeConfirm by remember { mutableStateOf(false) }
    val context       = LocalContext.current
    val totalSelected = selectedSlots.values.sumOf { it.size }

    // Onaylı harita gelince grid'e yükle (sadece bir kez — null iken initialized=true YAPMA)
    LaunchedEffect(approvedMine) {
        if (!initialized && approvedMine != null) {
            val loaded = mutableMapOf<Int, Set<Int>>()
            WEEK_DAYS_FULL.forEachIndexed { dayIdx, day ->
                val daySlots = approvedMine.slotsForDay(day)
                if (daySlots.isNotEmpty()) {
                    loaded[dayIdx] = daySlots
                        .mapNotNull { s -> slots.indexOf(s).takeIf { it >= 0 } }
                        .toSet()
                }
            }
            selectedSlots = loaded
            initialized = true
        }
    }

    fun toggleSlot(dayIdx: Int, slotIdx: Int) {
        selectedSlots = selectedSlots.toMutableMap().apply {
            val current = get(dayIdx) ?: emptySet()
            val updated = if (slotIdx in current) current - slotIdx else current + slotIdx
            if (updated.isEmpty()) remove(dayIdx) else put(dayIdx, updated)
        }
    }

    fun toggleDay(dayIdx: Int) {
        selectedSlots = selectedSlots.toMutableMap().apply {
            if ((get(dayIdx)?.size ?: 0) == slots.size) remove(dayIdx)
            else put(dayIdx, slots.indices.toSet())
        }
    }

    fun buildSlotsMap(): Map<String, List<String>> =
        WEEK_DAYS_FULL.indices.associate { dayIdx ->
            WEEK_DAYS_FULL[dayIdx] to (selectedSlots[dayIdx]
                ?.sorted()
                ?.map { slotIdx -> slots[slotIdx] }
                ?: emptyList())
        }.filter { it.value.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header ────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(stringResource(R.string.avail_screen_title), color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (isFirstTime)
                            "İlk gönderimde admin onayına gidecek. Sonraki güncellemeler anında uygulanır."
                        else
                            "Seçili slotları değiştirip 'Haritamı Güncelle' ile kaydet.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // ── Reddedildi notu (varsa) ───────────────────────────
        val rejected = myAvailabilities.firstOrNull { it.status == AvailabilityStatus.REJECTED }
        if (rejected != null && rejected.adminNote.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Admin Notu:", color = ErrorRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text("\"${rejected.adminNote}\"", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // ── Durum özeti ──────────────────────────────────────
        if (latestMine != null) {
            val statusColor = when (latestMine.status) {
                AvailabilityStatus.APPROVED -> EmeraldGreen
                AvailabilityStatus.REJECTED -> ErrorRed
                else                        -> Color(0xFFF59E0B)
            }
            val statusLabel = when (latestMine.status) {
                AvailabilityStatus.APPROVED -> "Haritanız aktif (${latestMine.totalSlots} slot)"
                AvailabilityStatus.REJECTED -> "Reddedildi — lütfen düzenleyin"
                else                        -> "Admin onayı bekleniyor"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusColor.copy(alpha = 0.09f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(10.dp))
                Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Haftalık Grid (Interactive) ────────────────────────
        Text(
            "Müsait olduğunuz saatleri seçin:",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                // Gün başlıkları + "Tümü" butonları
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.width(58.dp))
                    WEEK_DAYS_FULL.indices.forEach { dayIdx ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    displayDays[dayIdx],
                                    color = DAY_COLORS[dayIdx],
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(DAY_COLORS[dayIdx]))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))

                // Tüm Gün seç butonları (başta, grid üstünde)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.width(58.dp))
                    WEEK_DAYS_FULL.indices.forEach { dayIdx ->
                        val allSelected = (selectedSlots[dayIdx]?.size ?: 0) == slots.size
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (allSelected) DAY_COLORS[dayIdx].copy(alpha = 0.25f)
                                    else DAY_COLORS[dayIdx].copy(alpha = 0.07f)
                                )
                                .border(1.dp, DAY_COLORS[dayIdx].copy(alpha = if (allSelected) 0.7f else 0.2f), RoundedCornerShape(5.dp))
                                .clickable { toggleDay(dayIdx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (allSelected) "✓" else stringResource(R.string.avail_select_all_day),
                                color = DAY_COLORS[dayIdx],
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))

                // Saat dilimleri
                slots.forEachIndexed { slotIdx, slot ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            slot.take(5),
                            color = TextSecondary.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            modifier = Modifier.width(58.dp)
                        )
                        WEEK_DAYS_FULL.indices.forEach { dayIdx ->
                            val isSelected = selectedSlots[dayIdx]?.contains(slotIdx) == true
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) EmeraldGreen else Slate700)
                                    .border(1.dp, if (isSelected) EmeraldGreen else TextSecondary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .clickable { toggleSlot(dayIdx, slotIdx) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Seçim özeti + Temizle + Gönder/Güncelle ──────────
        if (totalSelected > 0) {
            Text(
                "$totalSelected slot seçili",
                color = EmeraldGreen,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (totalSelected > 0) {
                OutlinedButton(
                    onClick = { selectedSlots = emptyMap() },
                    modifier = Modifier.weight(0.35f),
                    border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            Button(
                onClick = {
                    if (totalSelected == 0) {
                        Toast.makeText(context, context.getString(R.string.avail_no_selection), Toast.LENGTH_SHORT).show()
                    } else if (isFirstTime) {
                        // İlk gönderim → onay dialogu göster
                        showFirstTimeConfirm = true
                    } else {
                        // Güncelleme → direkt APPROVED olarak kaydet
                        isSaving = true
                        adminViewModel.updateOwnAvailability(
                            lecturerUsername = user.username,
                            lecturerName     = user.fullName,
                            slots            = buildSlotsMap(),
                            onComplete       = { success ->
                                isSaving = false
                                val msg = if (success) "Müsaitlik haritanız güncellendi!" else "Güncelleme başarısız, tekrar deneyin."
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        totalSelected == 0         -> TextSecondary.copy(alpha = 0.3f)
                        isFirstTime                -> Color(0xFF6366F1)  // mor — ilk gönderim
                        else                       -> EmeraldGreen
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Kaydediliyor...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(
                        if (isFirstTime) Icons.AutoMirrored.Filled.Send else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isFirstTime)
                            if (totalSelected > 0) "Admin'e Gönder ($totalSelected slot)" else "Admin'e Gönder"
                        else
                            if (totalSelected > 0) "Haritamı Güncelle ($totalSelected slot)" else "Haritamı Güncelle",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    // ── İlk gönderim onay dialogu ─────────────────────────────
    if (showFirstTimeConfirm) {
        AlertDialog(
            onDismissRequest = { showFirstTimeConfirm = false },
            containerColor = Slate800,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Müsaitlik Gönder", color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Müsaitlik haritanız admin onayına gönderilecek.",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Onaylandıktan sonra ders atama sürecinde kullanılacak. Sonraki güncellemeleriniz anında geçerli olur.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF6366F1).copy(alpha = 0.1f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFF6366F1), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${totalSelected} slot seçili",
                            color = Color(0xFF6366F1),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFirstTimeConfirm = false
                        isSaving = true
                        adminViewModel.submitAvailability(
                            lecturerUsername = user.username,
                            lecturerName     = user.fullName,
                            slots            = buildSlotsMap()
                        )
                        isSaving = false
                        Toast.makeText(context, "Müsaitliğiniz admin onayına gönderildi!", Toast.LENGTH_LONG).show()
                        selectedSlots = emptyMap()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Evet, Gönder", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFirstTimeConfirm = false }) {
                    Text("İptal", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun AvailabilityGridReadOnly(
    avail: com.example.academicmanager.data.LecturerAvailability,
    displayDays: List<String>,
    accentColor: Color
) {
    val slots = SCHEDULE_TIME_SLOTS

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(48.dp))
            WEEK_DAYS_FULL.indices.forEach { dayIdx ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        displayDays[dayIdx],
                        color = DAY_COLORS[dayIdx],
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        slots.forEachIndexed { slotIdx, slot ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    slot.take(5),
                    color = TextSecondary.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    modifier = Modifier.width(48.dp)
                )
                WEEK_DAYS_FULL.forEachIndexed { dayIdx, day ->
                    val selected = slot in avail.slotsForDay(day)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .padding(horizontal = 1.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (selected) accentColor.copy(alpha = 0.8f) else Slate700),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

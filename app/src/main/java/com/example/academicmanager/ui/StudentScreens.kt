package com.example.academicmanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.academicmanager.R
import com.example.academicmanager.data.ScheduleEntry
import com.example.academicmanager.data.SessionType
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.AdminViewModel
import com.example.academicmanager.ui.viewmodels.AuthViewModel

// English data keys — used for Firestore data operations, DO NOT localize
private val STUDENT_WEEK_DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
private val STUDENT_TIME_SLOTS = listOf(
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

private val STUDENT_DAY_COLORS = listOf(
    Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFF8B5CF6)
)

// ─────────────────────────────────────────────────────────────
// STUDENT HOME SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun StudentHomeScreen(authViewModel: AuthViewModel, adminViewModel: AdminViewModel, navController: NavController) {
    val user        = authViewModel.currentUser ?: return
    val allEntries  by adminViewModel.scheduleEntries.collectAsState()
    val allCourses  by adminViewModel.courses.collectAsState()

    val deptCourses     = allCourses.filter { it.department == user.department }
    val deptCourseCodes = deptCourses.map { it.courseCode }.toSet()
    val myEntries       = allEntries.filter { it.courseCode in deptCourseCodes }

    val coursesByDay = STUDENT_WEEK_DAYS.associateWith { day ->
        myEntries.filter { it.dayOfWeek == day }
    }

    val displayDaysFull = weekDaysFull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Welcome Card ─────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = IndigoAccent.copy(alpha = 0.12f)),
            shape    = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(IndigoAccent.copy(alpha = 0.25f))
                        .border(2.dp, IndigoAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user.fullName.firstOrNull()?.uppercase(java.util.Locale("tr", "TR")) ?: "?",
                        color      = IndigoAccent,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.welcome_student), color = AppColorState.textSecondary, style = MaterialTheme.typography.bodySmall)
                    Text(user.fullName, color = AppColorState.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (user.department.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(user.department, color = IndigoAccent, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    if (user.studentYear.isNotBlank()) {
                        Text(user.studentYear, color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                // Duyurular bell ikonu
                IconButton(onClick = { navController.navigate("announcements") }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Duyurular", tint = IndigoAccent, modifier = Modifier.size(22.dp))
                }
            }
        }

        // ── Stat Cards ───────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StudentStatCard(stringResource(R.string.stat_weekly_courses), myEntries.size.toString(), Icons.Default.School, IndigoAccent, Modifier.weight(1f))
            StudentStatCard(stringResource(R.string.stat_active_days), myEntries.map { it.dayOfWeek }.distinct().size.toString(), Icons.Default.DateRange, EmeraldGreen, Modifier.weight(1f))
            StudentStatCard(stringResource(R.string.stat_lab_sessions), myEntries.count { it.sessionType == SessionType.LAB }.toString(), Icons.Default.Science, Color(0xFFF59E0B), Modifier.weight(1f))
        }

        // ── Quick Actions ─────────────────────────────────────
        Text(stringResource(R.string.quick_actions), color = AppColorState.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudentActionCard(
                    label = stringResource(R.string.my_grades_action),
                    icon = Icons.Default.Grade,
                    color = IndigoAccent,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("my_grades") }
                )
                StudentActionCard(
                    label = stringResource(R.string.my_attendance_action),
                    icon = Icons.Default.HowToReg,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("my_attendance") }
                )
                StudentActionCard(
                    label = "QR Yoklama",
                    icon = Icons.Default.QrCodeScanner,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("qr_scan") }
                )
                StudentActionCard(
                    label = stringResource(R.string.my_exam_schedule_action),
                    icon = Icons.AutoMirrored.Filled.EventNote,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("student_exam_schedule") }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudentActionCard(
                    label = stringResource(R.string.my_assignments_action),
                    icon = Icons.Default.Assignment,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("student_assignments") }
                )
                StudentActionCard(
                    label = stringResource(R.string.academic_calendar_action),
                    icon = Icons.Default.CalendarMonth,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("academic_calendar") }
                )
            }
        }

        // ── Weekly Schedule ──────────────────────────────────
        Text(stringResource(R.string.this_week), color = AppColorState.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (myEntries.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = AppColorState.surface),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier             = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment  = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = AppColorState.textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.no_dept_schedule), color = AppColorState.textSecondary, textAlign = TextAlign.Center)
                }
            }
        } else {
            STUDENT_WEEK_DAYS.forEachIndexed { i, day ->
                val dayEntries = coursesByDay[day] ?: emptyList()
                if (dayEntries.isNotEmpty()) {
                    StudentDaySection(
                        day = displayDaysFull[i],
                        entries = dayEntries,
                        accentColor = STUDENT_DAY_COLORS[i],
                        countStr = stringResource(R.string.courses_this_day, dayEntries.size)
                    )
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun StudentStatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = AppColorState.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(label, color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun StudentActionCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StudentDaySection(day: String, entries: List<ScheduleEntry>, accentColor: Color, countStr: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentColor))
            Spacer(Modifier.width(8.dp))
            Text(day, color = accentColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(8.dp))
            Text(countStr, color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
        }
        entries.sortedBy { it.timeSlot }.forEach { entry -> StudentCourseCard(entry, accentColor) }
    }
}

@Composable
private fun StudentCourseCard(entry: ScheduleEntry, accentColor: Color) {
    val isLab = entry.sessionType == SessionType.LAB
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = if (expanded) accentColor.copy(alpha = 0.12f) else AppColorState.surface),
        shape  = RoundedCornerShape(14.dp),
        border = if (expanded) BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)) else null
    ) {
        Column {
            // ── Collapsed row ─────────────────────────────────
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp)).background(accentColor))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.courseName, color = AppColorState.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (isLab) {
                            Spacer(Modifier.width(4.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFF59E0B).copy(alpha = 0.2f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text(stringResource(R.string.session_lab), color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(entry.courseCode, color = accentColor, style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(entry.timeSlot, color = AppColorState.textPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Text(entry.classroomName, color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // ── Expanded detail — receipt view ─────────────────
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    HorizontalDivider(color = accentColor.copy(alpha = 0.25f), thickness = 1.dp)
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReceiptRow(stringResource(R.string.receipt_course_code), entry.courseCode,    accentColor)
                        ReceiptRow(stringResource(R.string.receipt_lecturer),    entry.lecturerName,  accentColor)
                        ReceiptRow(stringResource(R.string.receipt_classroom),   entry.classroomName, accentColor)
                        ReceiptRow(stringResource(R.string.receipt_day),         weekDaysFull().getOrElse(STUDENT_WEEK_DAYS.indexOf(entry.dayOfWeek)) { entry.dayOfWeek },     accentColor)
                        ReceiptRow(stringResource(R.string.receipt_time),        entry.timeSlot,      accentColor)
                        ReceiptRow(
                            stringResource(R.string.receipt_type),
                            if (isLab) stringResource(R.string.type_lab_full) else stringResource(R.string.type_theory),
                            if (isLab) Color(0xFFF59E0B) else accentColor
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(30) { i ->
                            Box(modifier = Modifier.weight(1f).height(if (i % 3 == 0) 12.dp else 8.dp).background(accentColor.copy(alpha = if (i % 2 == 0) 0.5f else 0.2f)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, accentColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
        Text(value, color = AppColorState.textPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// STUDENT CALENDAR SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun StudentCalendarScreen(authViewModel: AuthViewModel, adminViewModel: AdminViewModel) {
    val user        = authViewModel.currentUser ?: return
    val allEntries  by adminViewModel.scheduleEntries.collectAsState()
    val allCourses  by adminViewModel.courses.collectAsState()

    val deptCourseCodes = allCourses.filter { it.department == user.department }.map { it.courseCode }.toSet()
    val myEntries       = allEntries.filter { it.courseCode in deptCourseCodes }

    val displayDaysFull  = weekDaysFull()
    val displayDaysShort = weekDaysShort()

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
        .filter { it.dayOfWeek == STUDENT_WEEK_DAYS[selectedDay] }
        .sortedBy { STUDENT_TIME_SLOTS.indexOf(it.timeSlot) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.student_schedule_title), style = MaterialTheme.typography.headlineSmall, color = AppColorState.textPrimary, fontWeight = FontWeight.Bold)
        Text("${user.department} · ${stringResource(R.string.courses_this_day, myEntries.size)}", color = AppColorState.textSecondary, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))

        // ── Day Selector Tabs ─────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            STUDENT_WEEK_DAYS.forEachIndexed { idx, day ->
                val isSelected = idx == selectedDay
                val isToday    = idx == todayIndex
                val count      = myEntries.count { it.dayOfWeek == day }
                val tabColor   = STUDENT_DAY_COLORS[idx]
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(when { isSelected -> tabColor; isToday -> tabColor.copy(alpha = 0.10f); else -> AppColorState.surface })
                        .then(if (isToday && !isSelected) Modifier.border(1.dp, tabColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp)) else Modifier)
                        .clickable { selectedDay = idx }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(displayDaysShort[idx], color = if (isSelected) Color.White else if (isToday) tabColor else AppColorState.textSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal)
                    Spacer(Modifier.height(4.dp))
                    if (count > 0) {
                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(if (isSelected) Color.White.copy(alpha = 0.25f) else tabColor.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                            Text(count.toString(), color = if (isSelected) Color.White else tabColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    } else { Spacer(Modifier.size(18.dp)) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(STUDENT_DAY_COLORS[selectedDay]))
            Spacer(Modifier.width(8.dp))
            Text(displayDaysFull[selectedDay], color = STUDENT_DAY_COLORS[selectedDay], fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Text(
                if (dayEntries.isEmpty()) stringResource(R.string.no_session_student)
                else stringResource(R.string.courses_this_day, dayEntries.size),
                color = AppColorState.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(12.dp))

        // ── Timeline ─────────────────────────────────────────
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(STUDENT_TIME_SLOTS) { slot ->
                StudentTimelineRow(slot, dayEntries.find { it.timeSlot == slot }, STUDENT_DAY_COLORS[selectedDay])
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun StudentTimelineRow(timeSlot: String, entry: ScheduleEntry?, accentColor: Color) {
    val isLab    = entry?.sessionType == SessionType.LAB
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier          = Modifier.fillMaxWidth().padding(bottom = if (entry != null) 10.dp else 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            timeSlot.take(5),
            color      = if (entry != null) accentColor else AppColorState.textSecondary.copy(alpha = 0.40f),
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = if (entry != null) FontWeight.Bold else FontWeight.Normal,
            modifier   = Modifier.width(54.dp).padding(top = 7.dp),
            fontSize   = 11.sp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp)) {
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.size(if (entry != null) 11.dp else 6.dp).clip(CircleShape).background(if (entry != null) accentColor else AppColorState.textSecondary.copy(alpha = 0.18f)))
            Box(modifier = Modifier.width(if (entry != null) 2.dp else 1.dp).height(if (entry != null && expanded) 120.dp else if (entry != null) 68.dp else 26.dp).background(if (entry != null) accentColor.copy(alpha = 0.22f) else AppColorState.textSecondary.copy(alpha = 0.07f)))
        }
        Spacer(Modifier.width(12.dp))
        if (entry != null) {
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded },
                colors   = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = if (expanded) 0.13f else 0.09f)),
                shape    = RoundedCornerShape(14.dp),
                border   = BorderStroke(1.dp, accentColor.copy(alpha = if (expanded) 0.45f else 0.28f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(entry.courseName, color = AppColorState.textPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (isLab) {
                                Spacer(Modifier.width(4.dp))
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFF59E0B).copy(alpha = 0.2f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                    Text(stringResource(R.string.session_lab), color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(accentColor.copy(alpha = 0.18f)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                                Text(entry.courseCode, color = accentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(entry.classroomName, color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp)
                    )
                }
                AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column {
                        HorizontalDivider(color = accentColor.copy(alpha = 0.25f))
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReceiptRow(stringResource(R.string.receipt_lecturer),  entry.lecturerName,  accentColor)
                            ReceiptRow(stringResource(R.string.receipt_day),       weekDaysFull().getOrElse(STUDENT_WEEK_DAYS.indexOf(entry.dayOfWeek)) { entry.dayOfWeek },     accentColor)
                            ReceiptRow(stringResource(R.string.receipt_time),      entry.timeSlot,      accentColor)
                            ReceiptRow(
                                stringResource(R.string.receipt_type),
                                if (isLab) stringResource(R.string.type_lab_full) else stringResource(R.string.type_theory),
                                if (isLab) Color(0xFFF59E0B) else accentColor
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(28) { i ->
                                Box(modifier = Modifier.weight(1f).height(if (i % 3 == 0) 10.dp else 6.dp).background(accentColor.copy(alpha = if (i % 2 == 0) 0.4f else 0.15f)))
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(26.dp), contentAlignment = Alignment.CenterStart) {
                HorizontalDivider(color = AppColorState.textSecondary.copy(alpha = 0.05f), thickness = 1.dp)
            }
        }
    }
}

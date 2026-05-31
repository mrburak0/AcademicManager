package com.example.academicmanager.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.academicmanager.R
import com.example.academicmanager.data.*
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.AdminViewModel
import com.example.academicmanager.ui.viewmodels.AuthViewModel
import com.example.academicmanager.ui.viewmodels.ExamViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ─────────────────────────────────────────────────────────────
// ADMIN EXAM SCHEDULE SCREEN
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExamScheduleScreen(
    adminViewModel: AdminViewModel,
    examViewModel: ExamViewModel,
    navController: NavController
) {
    val allExams   by examViewModel.allExams.collectAsState()
    val allCourses by adminViewModel.courses.collectAsState()
    val scope      = rememberCoroutineScope()
    val snackbar   = remember { SnackbarHostState() }

    var showAdd      by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ExamEntry?>(null) }

    // Sort & group by date
    val today = LocalDate.now()
    val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val grouped = allExams
        .sortedWith(compareBy({ it.examDate }, { it.startTime }))
        .groupBy { it.examDate }

    Scaffold(
        containerColor = Slate900,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(data, containerColor = EmeraldGreen, contentColor = Slate900, shape = RoundedCornerShape(12.dp))
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                containerColor = Color(0xFFEF4444),
                contentColor   = Color.White,
                shape          = RoundedCornerShape(16.dp),
                icon  = { Icon(Icons.Default.Add, null) },
                text  = { Text(stringResource(R.string.add_exam), fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFEF4444).copy(alpha = 0.18f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.exam_schedule_admin_title), color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.exam_schedule_admin_sub, allExams.size), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    // Type legend
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ExamType.all.forEach { t ->
                            Surface(shape = CircleShape, color = Color(ExamType.color(t)).copy(alpha = 0.2f)) {
                                Text(
                                    ExamType.displayName(t).take(3),
                                    color = Color(ExamType.color(t)),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
            HorizontalDivider(color = Slate700.copy(alpha = 0.5f))

            if (allExams.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier = Modifier.size(90.dp).clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFFEF4444).copy(alpha = 0.18f), Color.Transparent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EventBusy, null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(44.dp))
                        }
                        Text(stringResource(R.string.no_exams_yet), color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.add_exam_hint), color = TextSecondary.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    grouped.forEach { (date, exams) ->
                        val isPast   = try { LocalDate.parse(date, fmt).isBefore(today) } catch (_: Exception) { false }
                        val isToday  = try { LocalDate.parse(date, fmt) == today } catch (_: Exception) { false }
                        item(key = "header_$date") {
                            AdminDateHeader(date, isToday, isPast)
                        }
                        items(exams, key = { it.id }) { exam ->
                            AdminExamCard(exam, isPast, onDelete = { deleteTarget = exam })
                            Spacer(Modifier.height(8.dp))
                        }
                        item(key = "spacer_$date") { Spacer(Modifier.height(6.dp)) }
                    }
                }
            }
        }
    }

    // Add Exam Sheet
    if (showAdd) {
        AddExamDialog(
            courses  = allCourses,
            onDismiss = { showAdd = false },
            onAdd = { code, name, dept, lecturer, date, start, end, room, type, notes ->
                scope.launch {
                    examViewModel.addExam(code, name, dept, lecturer, date, start, end, room, type, notes)
                    showAdd = false
                    snackbar.showSnackbar("Sınav eklendi ✓")
                }
            }
        )
    }

    // Delete confirm
    deleteTarget?.let { exam ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = Slate800,
            title = { Text(stringResource(R.string.delete_exam_title), color = ErrorRed, fontWeight = FontWeight.Bold) },
            text  = { Text("${exam.courseName} — ${ExamType.displayName(exam.examType)} (${exam.examDate})", color = TextPrimary, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(onClick = { examViewModel.deleteExam(exam.id); deleteTarget = null }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel), color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun AdminDateHeader(date: String, isToday: Boolean, isPast: Boolean) {
    val fmt     = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val display = try {
        val ld = LocalDate.parse(date, fmt)
        val dayFmt = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", java.util.Locale("tr","TR"))
        ld.format(dayFmt)
    } catch (_: Exception) { date }

    Row(
        modifier = Modifier.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(6.dp).clip(CircleShape)
                .background(if (isToday) Color(0xFFEF4444) else if (isPast) TextSecondary.copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.6f))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            display,
            color = if (isToday) Color(0xFFEF4444) else if (isPast) TextSecondary.copy(alpha = 0.5f) else TextPrimary,
            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge
        )
        if (isToday) {
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFEF4444).copy(alpha = 0.18f)) {
                Text("BUGÜN", color = Color(0xFFEF4444), modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun AdminExamCard(exam: ExamEntry, isPast: Boolean, onDelete: () -> Unit) {
    val typeColor = Color(ExamType.color(exam.examType))
    val alpha     = if (isPast) 0.45f else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = if (isPast) 0.6f else 1f)),
        shape    = RoundedCornerShape(16.dp),
        border   = BorderStroke(1.dp, typeColor.copy(alpha = if (isPast) 0.12f else 0.3f))
    ) {
        Row(modifier = Modifier.padding(0.dp)) {
            // Left color bar
            Box(
                modifier = Modifier.width(5.dp).fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(typeColor.copy(alpha = alpha), typeColor.copy(alpha = alpha * 0.3f))))
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = typeColor.copy(alpha = if (isPast) 0.08f else 0.15f)) {
                        Text(
                            ExamType.displayName(exam.examType),
                            color = typeColor.copy(alpha = alpha),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(exam.courseCode, color = typeColor.copy(alpha = alpha * 0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, null, tint = ErrorRed.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(exam.courseName, color = TextPrimary.copy(alpha = alpha), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ExamDetailChip(Icons.Default.AccessTime, "${exam.startTime}–${exam.endTime}", alpha)
                    ExamDetailChip(Icons.Default.MeetingRoom, exam.classroom.ifBlank { "—" }, alpha)
                    if (exam.lecturerName.isNotBlank()) {
                        ExamDetailChip(Icons.Default.Person, exam.lecturerName, alpha)
                    }
                }
                if (exam.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(exam.notes, color = TextSecondary.copy(alpha = alpha * 0.7f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ExamDetailChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, alpha: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, tint = TextSecondary.copy(alpha = alpha * 0.7f), modifier = Modifier.size(11.dp))
        Text(text, color = TextSecondary.copy(alpha = alpha * 0.8f), style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExamDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String, String, String, String, String, String) -> Unit
) {
    var courseCode   by remember { mutableStateOf("") }
    var courseName   by remember { mutableStateOf("") }
    var department   by remember { mutableStateOf("") }
    var lecturer     by remember { mutableStateOf("") }
    var examDate     by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }
    var startTime    by remember { mutableStateOf("09:00") }
    var endTime      by remember { mutableStateOf("11:00") }
    var classroom    by remember { mutableStateOf("") }
    var examType     by remember { mutableStateOf(ExamType.FINAL) }
    var notes        by remember { mutableStateOf("") }
    var showCourse   by remember { mutableStateOf(false) }

    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val parsed = remember(examDate) { try { LocalDate.parse(examDate, fmt) } catch (_: Exception) { LocalDate.now() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Slate800,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFEF4444).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EditCalendar, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.add_exam), color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Course selector
                OutlinedButton(
                    onClick = { showCourse = !showCourse },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (courseName.isNotBlank()) EmeraldGreen.copy(alpha = 0.5f) else Slate700)
                ) {
                    Icon(Icons.Default.School, null, modifier = Modifier.size(14.dp), tint = if (courseName.isNotBlank()) EmeraldGreen else TextSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (courseName.isNotBlank()) courseName else stringResource(R.string.select_course_exam),
                        color = if (courseName.isNotBlank()) EmeraldGreen else TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(if (showCourse) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                AnimatedVisibility(visible = showCourse, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Slate700), shape = RoundedCornerShape(12.dp)) {
                        Column {
                            courses.take(10).forEach { c ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        courseCode = c.courseCode; courseName = c.courseName; department = c.department; showCourse = false
                                    }.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(c.courseName, color = TextPrimary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(c.courseCode, color = EmeraldGreen, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                if (c != courses.take(10).last()) HorizontalDivider(color = Slate800.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 8.dp))
                            }
                        }
                    }
                }

                // Exam type chips
                Text(stringResource(R.string.exam_type_label), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExamType.all.forEach { t ->
                        val tc = Color(ExamType.color(t))
                        val sel = examType == t
                        Surface(
                            modifier = Modifier.weight(1f).clickable { examType = t },
                            shape = RoundedCornerShape(10.dp),
                            color = if (sel) tc.copy(alpha = 0.18f) else Slate700.copy(alpha = 0.5f),
                            border = if (sel) BorderStroke(1.dp, tc) else BorderStroke(1.dp, Color.Transparent)
                        ) {
                            Text(
                                ExamType.displayName(t),
                                color = if (sel) tc else TextSecondary,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Date navigator
                Text(stringResource(R.string.exam_date_label), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Slate700.copy(alpha = 0.6f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { examDate = parsed.minusDays(1).format(fmt) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ChevronLeft, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    Text(examDate, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    IconButton(onClick = { examDate = parsed.plusDays(1).format(fmt) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // Time row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExamTextField(stringResource(R.string.exam_start_time), startTime, { startTime = it }, Modifier.weight(1f), KeyboardType.Number)
                    ExamTextField(stringResource(R.string.exam_end_time), endTime, { endTime = it }, Modifier.weight(1f), KeyboardType.Number)
                }

                // Classroom
                ExamTextField(stringResource(R.string.exam_classroom), classroom, { classroom = it }, Modifier.fillMaxWidth())

                // Lecturer (optional)
                ExamTextField(stringResource(R.string.exam_lecturer_optional), lecturer, { lecturer = it }, Modifier.fillMaxWidth())

                // Notes
                ExamTextField(stringResource(R.string.exam_notes_optional), notes, { notes = it }, Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (courseName.isBlank() || examDate.isBlank() || startTime.isBlank()) return@Button
                    onAdd(courseCode, courseName, department, lecturer, examDate, startTime, endTime, classroom, examType, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add_exam), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = TextSecondary) }
        }
    )
}

@Composable
private fun ExamTextField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier, shape = RoundedCornerShape(10.dp), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFEF4444), unfocusedBorderColor = Slate700,
            focusedLabelColor = Color(0xFFEF4444), focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary, cursorColor = Color(0xFFEF4444),
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
        )
    )
}

// ─────────────────────────────────────────────────────────────
// STUDENT EXAM SCHEDULE SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun StudentExamScheduleScreen(
    authViewModel: AuthViewModel,
    examViewModel: ExamViewModel,
    navController: NavController
) {
    val user = authViewModel.currentUser ?: return
    val examFlow = remember(user.department) { examViewModel.getExamsByDepartment(user.department) }
    val exams by examFlow.collectAsState(initial = emptyList())

    val today = LocalDate.now()
    val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val sorted   = exams.sortedWith(compareBy({ it.examDate }, { it.startTime }))
    val upcoming = sorted.filter { try { !LocalDate.parse(it.examDate, fmt).isBefore(today) } catch (_: Exception) { false } }
    val past     = sorted.filter { try { LocalDate.parse(it.examDate, fmt).isBefore(today) } catch (_: Exception) { false } }

    val nextExam = upcoming.firstOrNull()
    val daysToNext = nextExam?.let {
        try { ChronoUnit.DAYS.between(today, LocalDate.parse(it.examDate, fmt)) } catch (_: Exception) { null }
    }

    var showPast by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Slate900)) {
        // ── Hero Header ───────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFEF4444).copy(alpha = 0.22f), Color(0xFF6366F1).copy(alpha = 0.08f), Color.Transparent),
                        startY = 0f, endY = 400f
                    )
                )
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.exam_schedule_student_title), color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(user.department, color = Color(0xFFEF4444).copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
        }
        HorizontalDivider(color = Slate700.copy(alpha = 0.4f))

        if (exams.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.size(100.dp).clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color(0xFFEF4444).copy(alpha = 0.15f), Color.Transparent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EventAvailable, null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(50.dp))
                    }
                    Text(stringResource(R.string.no_exams_dept), color = TextSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Next Exam Hero Card
                if (nextExam != null && daysToNext != null) {
                    item {
                        NextExamHeroCard(exam = nextExam, daysToNext = daysToNext)
                        Spacer(Modifier.height(18.dp))
                    }
                }

                // Upcoming section header
                if (upcoming.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.upcoming_exams), color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = CircleShape, color = Color(0xFFEF4444).copy(alpha = 0.15f)) {
                                Text(upcoming.size.toString(), color = Color(0xFFEF4444), modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // Upcoming timeline
                items(upcoming, key = { it.id }) { exam ->
                    StudentExamTimelineCard(exam = exam, isNext = exam == nextExam, today = today, fmt = fmt)
                    Spacer(Modifier.height(10.dp))
                }

                // Past exams toggle
                if (past.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Slate800.copy(alpha = 0.5f))
                                .clickable { showPast = !showPast }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.past_exams, past.size), color = TextSecondary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Icon(if (showPast) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (showPast) {
                        items(past.reversed(), key = { "past_${it.id}" }) { exam ->
                            StudentExamTimelineCard(exam = exam, isNext = false, today = today, fmt = fmt, isPast = true)
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
private fun NextExamHeroCard(exam: ExamEntry, daysToNext: Long) {
    val typeColor = Color(ExamType.color(exam.examType))
    val isToday   = daysToNext == 0L

    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            typeColor.copy(alpha = 0.30f),
            typeColor.copy(alpha = 0.10f),
            Slate800
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape    = RoundedCornerShape(24.dp),
        border   = BorderStroke(1.dp, typeColor.copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(gradientBrush)) {
            Column(modifier = Modifier.padding(22.dp)) {
                // Badge row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = typeColor.copy(alpha = 0.2f)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Star, null, tint = typeColor, modifier = Modifier.size(10.dp))
                            Text(stringResource(R.string.next_exam_label), color = typeColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = typeColor.copy(alpha = 0.15f)) {
                        Text(ExamType.displayName(exam.examType), color = typeColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(14.dp))

                // Course name
                Text(exam.courseName, color = TextPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (exam.courseCode.isNotBlank()) {
                    Text(exam.courseCode, color = typeColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(16.dp))

                // Countdown + details
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            HeroDetail(Icons.Default.CalendarMonth, exam.examDate)
                            HeroDetail(Icons.Default.AccessTime, "${exam.startTime}–${exam.endTime}")
                            if (exam.classroom.isNotBlank()) HeroDetail(Icons.Default.MeetingRoom, exam.classroom)
                        }
                    }
                    // Countdown pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = typeColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isToday) {
                                Text("BUGÜN", color = typeColor, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                            } else {
                                Text(daysToNext.toString(), color = typeColor, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                                Text(stringResource(R.string.days_remaining), color = typeColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                if (exam.notes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Slate900.copy(alpha = 0.3f)).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(exam.notes, color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroDetail(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
        Text(text, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StudentExamTimelineCard(
    exam: ExamEntry,
    isNext: Boolean,
    today: LocalDate,
    fmt: DateTimeFormatter,
    isPast: Boolean = false
) {
    val typeColor = Color(ExamType.color(exam.examType))
    val alpha = if (isPast) 0.45f else 1f
    val daysLeft = try {
        val ld = LocalDate.parse(exam.examDate, fmt)
        ChronoUnit.DAYS.between(today, ld)
    } catch (_: Exception) { null }

    val dateDisplay = try {
        val ld = LocalDate.parse(exam.examDate, fmt)
        ld.format(DateTimeFormatter.ofPattern("d MMM, EEEE", java.util.Locale("tr","TR")))
    } catch (_: Exception) { exam.examDate }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline line
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
            Box(
                modifier = Modifier.size(if (isNext) 14.dp else 10.dp).clip(CircleShape)
                    .background(typeColor.copy(alpha = alpha)),
                contentAlignment = Alignment.Center
            ) {
                if (isNext) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Slate900))
                }
            }
        }
        Spacer(Modifier.width(8.dp))

        Card(
            modifier = Modifier.weight(1f),
            colors   = CardDefaults.cardColors(
                containerColor = if (isPast) Slate800.copy(alpha = 0.5f) else Slate800
            ),
            shape  = RoundedCornerShape(16.dp),
            border = if (!isPast) BorderStroke(1.dp, typeColor.copy(alpha = 0.2f)) else null
        ) {
            Row(modifier = Modifier.padding(0.dp)) {
                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(typeColor.copy(alpha = alpha)).clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(6.dp), color = typeColor.copy(alpha = if (isPast) 0.08f else 0.14f)) {
                            Text(ExamType.displayName(exam.examType), color = typeColor.copy(alpha = alpha), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.weight(1f))
                        // Days badge
                        if (!isPast && daysLeft != null) {
                            Surface(shape = RoundedCornerShape(6.dp), color = typeColor.copy(alpha = 0.10f)) {
                                Text(
                                    if (daysLeft == 0L) "Bugün!" else "+${daysLeft}g",
                                    color = typeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (isPast) {
                            Icon(Icons.Default.CheckCircle, null, tint = TextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(exam.courseName, color = TextPrimary.copy(alpha = alpha), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExamDetailChip(Icons.Default.CalendarMonth, dateDisplay, alpha)
                        ExamDetailChip(Icons.Default.AccessTime, "${exam.startTime}–${exam.endTime}", alpha)
                        if (exam.classroom.isNotBlank()) ExamDetailChip(Icons.Default.MeetingRoom, exam.classroom, alpha)
                    }
                    if (exam.notes.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(exam.notes, color = TextSecondary.copy(alpha = alpha * 0.7f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

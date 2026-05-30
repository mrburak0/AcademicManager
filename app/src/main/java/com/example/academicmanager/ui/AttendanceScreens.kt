package com.example.academicmanager.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.academicmanager.R
import com.example.academicmanager.data.*
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.AdminViewModel
import com.example.academicmanager.ui.viewmodels.AttendanceViewModel
import com.example.academicmanager.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val AttendanceAmber = Color(0xFFF59E0B)

// ─────────────────────────────────────────────────────────────
// LECTURER ATTENDANCE ENTRY SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun LecturerAttendanceScreen(
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    attendanceViewModel: AttendanceViewModel,
    navController: NavController
) {
    val user = authViewModel.currentUser ?: return
    val allEntries  by adminViewModel.scheduleEntries.collectAsState()
    val allCourses  by adminViewModel.courses.collectAsState()
    val allStudents by adminViewModel.students.collectAsState()
    val allRecordsFlow = remember(user.username) { attendanceViewModel.getRecordsByLecturer(user.username) }
    val allRecords by allRecordsFlow.collectAsState(initial = emptyList())

    val myCourseEntries = allEntries
        .filter { it.lecturerName == user.fullName }
        .distinctBy { it.courseCode }

    var selectedCourse by remember { mutableStateOf<ScheduleEntry?>(null) }

    AnimatedContent(
        targetState = selectedCourse,
        transitionSpec = {
            if (targetState != null)
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            else
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
        },
        label = "att_nav"
    ) { entry ->
        if (entry == null) {
            AttendanceCourseListScreen(
                myCourseEntries = myCourseEntries,
                allRecords = allRecords,
                onCourseSelected = { selectedCourse = it },
                onBack = { navController.popBackStack() }
            )
        } else {
            val courseInfo     = allCourses.find { it.courseCode == entry.courseCode }
            val studentsInDept = allStudents.filter { it.department == (courseInfo?.department ?: user.department) }
            val courseRecords  = allRecords.filter { it.courseCode == entry.courseCode }
            AttendanceSessionEntryScreen(
                entry = entry,
                courseInfo = courseInfo,
                students = studentsInDept,
                existingRecords = courseRecords,
                lecturerUsername = user.username,
                attendanceViewModel = attendanceViewModel,
                onBack = { selectedCourse = null }
            )
        }
    }
}

@Composable
private fun AttendanceCourseListScreen(
    myCourseEntries: List<ScheduleEntry>,
    allRecords: List<AttendanceRecord>,
    onCourseSelected: (ScheduleEntry) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Slate900)) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AttendanceAmber.copy(alpha = 0.15f), Color.Transparent)))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.attendance_entry_title), color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.attendance_entry_subtitle), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(AttendanceAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.HowToReg, null, tint = AttendanceAmber, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
            }
        }
        HorizontalDivider(color = Slate700.copy(alpha = 0.5f))

        if (myCourseEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Slate800), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.EventBusy, null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                    }
                    Text(stringResource(R.string.no_assigned_courses), color = TextSecondary, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(myCourseEntries) { entry ->
                    val sessionCount = allRecords.count { it.courseCode == entry.courseCode }
                    val lastSession  = allRecords.filter { it.courseCode == entry.courseCode }.maxByOrNull { it.timestamp }
                    AttendanceCourseCard(entry, sessionCount, lastSession?.sessionDate, onCourseSelected)
                }
            }
        }
    }
}

@Composable
private fun AttendanceCourseCard(
    entry: ScheduleEntry,
    sessionCount: Int,
    lastDate: String?,
    onClick: (ScheduleEntry) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(entry) },
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp))
                    .background(AttendanceAmber.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.HowToReg, null, tint = AttendanceAmber, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.courseName, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(entry.courseCode, color = AttendanceAmber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(20.dp), color = Slate700) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.AutoMirrored.Filled.EventNote, null, tint = TextSecondary, modifier = Modifier.size(10.dp))
                            Text(stringResource(R.string.sessions_recorded, sessionCount), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (lastDate != null) {
                        Text("Son: $lastDate", color = TextSecondary.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AttendanceSessionEntryScreen(
    entry: ScheduleEntry,
    courseInfo: Course?,
    students: List<Lecturer>,
    existingRecords: List<AttendanceRecord>,
    lecturerUsername: String,
    attendanceViewModel: AttendanceViewModel,
    onBack: () -> Unit
) {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var selectedDate by remember { mutableStateOf(today.format(formatter)) }
    var selectedSessionType by remember { mutableStateOf(SessionType.LECTURE) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val existingForDate = existingRecords.find { it.sessionDate == selectedDate && it.sessionType == selectedSessionType }

    val presentSet = remember(selectedDate, selectedSessionType, existingForDate) {
        mutableStateOf(existingForDate?.presentStudents?.toMutableSet() ?: mutableSetOf())
    }

    val presentCount = presentSet.value.size
    val totalCount   = students.size
    val pct = if (totalCount > 0) presentCount.toFloat() / totalCount else 0f

    Scaffold(
        containerColor = Slate900,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = AttendanceAmber, contentColor = Slate900, shape = RoundedCornerShape(12.dp))
            }
        },
        topBar = {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(AttendanceAmber.copy(alpha = 0.12f), Color.Transparent)))
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) }
                        Spacer(Modifier.width(4.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.courseName, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(entry.courseCode, color = AttendanceAmber, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                        // Mini attendance ring in header
                        Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(42.dp)) {
                                drawArc(Slate700, -90f, 360f, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                                if (pct > 0f) drawArc(AttendanceAmber, -90f, 360f * pct, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                            }
                            Text("${(pct * 100).toInt()}%", color = AttendanceAmber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
                HorizontalDivider(color = Slate700.copy(alpha = 0.5f))
            }
        },
        bottomBar = {
            Column(modifier = Modifier.background(Slate900).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Attendance summary bar
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate800)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.present_count, presentCount, totalCount),
                        color = AttendanceAmber,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { presentSet.value = students.map { it.username }.toMutableSet() }) {
                            Text(stringResource(R.string.select_all), color = EmeraldGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = { presentSet.value = mutableSetOf() }) {
                            Text(stringResource(R.string.deselect_all), color = ErrorRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Button(
                    onClick = {
                        scope.launch {
                            val presentList = presentSet.value.toList()
                            val absentList  = students.map { it.username }.filter { it !in presentSet.value }
                            attendanceViewModel.saveAttendance(
                                existingId = existingForDate?.id ?: "",
                                courseCode = entry.courseCode,
                                courseName = entry.courseName,
                                department = courseInfo?.department ?: "",
                                lecturerUsername = lecturerUsername,
                                sessionDate = selectedDate,
                                dayOfWeek = entry.dayOfWeek,
                                timeSlot = entry.timeSlot,
                                sessionType = selectedSessionType,
                                presentStudents = presentList,
                                absentStudents = absentList
                            )
                            snackbarHostState.showSnackbar("Yoklama kaydedildi ✓  $selectedDate")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AttendanceAmber),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp), tint = Slate900)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.attendance_save), fontWeight = FontWeight.Bold, color = Slate900, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date + Session type selector card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Date row
                        Text("Oturum Tarihi", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        DateNavigator(
                            selectedDate = selectedDate,
                            formatter = formatter,
                            today = today,
                            onDateChange = { selectedDate = it }
                        )
                        HorizontalDivider(color = Slate700.copy(alpha = 0.5f))
                        // Session type
                        Text("Oturum Tipi", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                SessionType.LECTURE to "Teorik",
                                SessionType.LAB      to "Lab"
                            ).forEach { (type, label) ->
                                val selected = selectedSessionType == type
                                Surface(
                                    modifier = Modifier.clickable { selectedSessionType = type }.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) AttendanceAmber.copy(alpha = 0.18f) else Slate700.copy(alpha = 0.6f),
                                    border = if (selected) BorderStroke(1.dp, AttendanceAmber) else BorderStroke(1.dp, Color.Transparent)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (type == SessionType.LECTURE) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.Science,
                                            null,
                                            tint = if (selected) AttendanceAmber else TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            label,
                                            color = if (selected) AttendanceAmber else TextSecondary,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        // Existing record indicator
                        if (existingForDate != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldGreen.copy(alpha = 0.10f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                Text(
                                    "Bu tarih için yoklama var — düzenleyebilirsiniz",
                                    color = EmeraldGreen,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            if (students.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PeopleOutline, null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.no_students_in_dept), color = TextSecondary, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(students, key = { it.username }) { student ->
                    val isPresent = student.username in presentSet.value
                    AttendanceStudentRow(
                        student = student,
                        isPresent = isPresent,
                        onToggle = {
                            val newSet = presentSet.value.toMutableSet()
                            if (isPresent) newSet.remove(student.username) else newSet.add(student.username)
                            presentSet.value = newSet
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateNavigator(
    selectedDate: String,
    formatter: DateTimeFormatter,
    today: LocalDate,
    onDateChange: (String) -> Unit
) {
    val parsed = remember(selectedDate) {
        try { LocalDate.parse(selectedDate, formatter) } catch (_: Exception) { today }
    }
    val isToday = parsed == today

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Slate700.copy(alpha = 0.6f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onDateChange(parsed.minusDays(1).format(formatter)) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.ChevronLeft, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                selectedDate,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            if (isToday) {
                Text(stringResource(R.string.today), color = AttendanceAmber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        if (!isToday) {
            TextButton(
                onClick = { onDateChange(today.format(formatter)) },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(stringResource(R.string.today), color = AttendanceAmber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Spacer(Modifier.width(36.dp))
        }
        IconButton(
            onClick = { onDateChange(parsed.plusDays(1).format(formatter)) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AttendanceStudentRow(student: Lecturer, isPresent: Boolean, onToggle: () -> Unit) {
    val accentColor by animateColorAsState(
        targetValue = if (isPresent) EmeraldGreen else Slate700,
        animationSpec = tween(200),
        label = "acc"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isPresent) EmeraldGreen.copy(alpha = 0.07f) else Slate800,
        animationSpec = tween(200),
        label = "bg"
    )

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(14.dp),
        border = if (isPresent) BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.5.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    student.fullName.firstOrNull()?.uppercase() ?: "?",
                    color = if (isPresent) EmeraldGreen else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(student.fullName, color = TextPrimary, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                if (student.studentId.isNotBlank()) {
                    Text(student.studentId, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            AnimatedContent(
                targetState = isPresent,
                transitionSpec = { scaleIn(tween(150)) + fadeIn() togetherWith scaleOut(tween(150)) + fadeOut() },
                label = "check"
            ) { present ->
                Icon(
                    if (present) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    null,
                    tint = if (present) EmeraldGreen else TextSecondary.copy(alpha = 0.35f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STUDENT MY ATTENDANCE SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun StudentAttendanceScreen(
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    attendanceViewModel: AttendanceViewModel,
    navController: NavController
) {
    val user       = authViewModel.currentUser ?: return
    val allCourses by adminViewModel.courses.collectAsState()
    val allRecords by attendanceViewModel.allRecords.collectAsState()

    val deptCourses     = allCourses.filter { it.department == user.department }
    val deptCourseCodes = deptCourses.map { it.courseCode }.toSet()

    val attendanceMap = attendanceViewModel.courseAttendanceMap(
        records = allRecords.filter { it.courseCode in deptCourseCodes },
        studentUsername = user.username,
        courseCodes = deptCourseCodes
    )

    val totalSessions = attendanceMap.values.sumOf { it.second }
    val totalAttended = attendanceMap.values.sumOf { it.first }
    val overallPct = if (totalSessions > 0) totalAttended.toFloat() / totalSessions * 100 else 0f

    Column(modifier = Modifier.fillMaxSize().background(Slate900)) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AttendanceAmber.copy(alpha = 0.16f), Color.Transparent)))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.my_attendance_title), color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(user.department, color = AttendanceAmber, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
        }
        HorizontalDivider(color = Slate700.copy(alpha = 0.5f))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AttendanceCircle(pct = overallPct / 100f)
                        Spacer(Modifier.height(12.dp))
                        if (totalSessions > 0) {
                            Text(
                                stringResource(R.string.attended_of, totalAttended, totalSessions),
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (overallPct < 70f) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                        .background(ErrorRed.copy(alpha = 0.12f))
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                                    Text(stringResource(R.string.attendance_warning), color = ErrorRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            Text("Henüz oturum kaydedilmedi", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Text("Derse Göre Devam", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
            }

            if (deptCourses.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_dept_courses), color = TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(deptCourses) { course ->
                    val (attended, total) = attendanceMap[course.courseCode] ?: Pair(0, 0)
                    val pct = if (total > 0) attended.toFloat() / total * 100 else 0f
                    StudentAttendanceCourseCard(course, attended, total, pct)
                }
            }
        }
    }
}

@Composable
private fun AttendanceCircle(pct: Float) {
    val color = when {
        pct >= 0.70f -> EmeraldGreen
        pct >= 0.50f -> AttendanceAmber
        pct >  0f    -> ErrorRed
        else         -> Slate700
    }
    Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(130.dp)) {
            drawArc(
                color = Slate700, startAngle = 135f, sweepAngle = 270f, useCenter = false,
                style = Stroke(12.dp.toPx(), cap = StrokeCap.Round),
                size = Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx()),
                topLeft = Offset(6.dp.toPx(), 6.dp.toPx())
            )
            if (pct > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.5f), color)),
                    startAngle = 135f, sweepAngle = 270f * pct, useCenter = false,
                    style = Stroke(12.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx()),
                    topLeft = Offset(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (pct > 0f) "%.0f%%".format(pct * 100) else "--",
                color = color,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(stringResource(R.string.overall_attendance), color = TextSecondary, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StudentAttendanceCourseCard(course: Course, attended: Int, total: Int, pct: Float) {
    val color = when {
        total == 0 -> TextSecondary
        pct >= 70f -> EmeraldGreen
        pct >= 50f -> AttendanceAmber
        else       -> ErrorRed
    }
    val animPct by animateFloatAsState(
        targetValue = if (total > 0) pct / 100f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "att_bar"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.HowToReg, null, tint = color, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(course.courseName, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(course.courseCode, color = color.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (total > 0) "%.0f%%".format(pct) else "--",
                        color = color,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (total > 0) {
                        Text("$attended/$total", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (total > 0) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(Slate700)
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight().fillMaxWidth(animPct)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.5f), color)))
                    )
                }
                if (pct < 70f) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(11.dp))
                        Text(stringResource(R.string.low_attendance_warning), color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.no_sessions_yet), color = TextSecondary.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

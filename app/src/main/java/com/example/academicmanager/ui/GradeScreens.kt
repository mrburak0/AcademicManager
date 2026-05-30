package com.example.academicmanager.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import com.example.academicmanager.ui.viewmodels.GradeViewModel
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
// LECTURER GRADE ENTRY SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun LecturerGradeEntryScreen(
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    gradeViewModel: GradeViewModel,
    navController: NavController
) {
    val user = authViewModel.currentUser ?: return
    val allEntries by adminViewModel.scheduleEntries.collectAsState()
    val allStudents by adminViewModel.students.collectAsState()
    val allCourses  by adminViewModel.courses.collectAsState()
    val allGrades   by gradeViewModel.allGrades.collectAsState()

    val myCourseEntries = allEntries
        .filter { it.lecturerName == user.fullName && it.sessionType == SessionType.LECTURE }
        .distinctBy { it.courseCode }

    var selectedEntry by remember { mutableStateOf<ScheduleEntry?>(null) }

    AnimatedContent(
        targetState = selectedEntry,
        transitionSpec = {
            if (targetState != null)
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            else
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
        },
        label = "grade_nav"
    ) { entry ->
        if (entry == null) {
            GradeCourseListScreen(
                user = user,
                myCourseEntries = myCourseEntries,
                allCourses = allCourses,
                allStudents = allStudents,
                allGrades = allGrades,
                onCourseSelected = { selectedEntry = it },
                onBack = { navController.popBackStack() }
            )
        } else {
            val courseInfo    = allCourses.find { it.courseCode == entry.courseCode }
            val studentsInDept = allStudents.filter { it.department == (courseInfo?.department ?: user.department) }
            GradeEntryForCourse(
                entry = entry,
                courseInfo = courseInfo,
                students = studentsInDept,
                existingGrades = allGrades.filter { it.courseCode == entry.courseCode },
                lecturerUsername = user.username,
                gradeViewModel = gradeViewModel,
                onBack = { selectedEntry = null }
            )
        }
    }
}

@Composable
private fun GradeCourseListScreen(
    user: Lecturer,
    myCourseEntries: List<ScheduleEntry>,
    allCourses: List<Course>,
    allStudents: List<Lecturer>,
    allGrades: List<GradeRecord>,
    onCourseSelected: (ScheduleEntry) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Slate900)) {
        // ── Gradient Header ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(EmeraldGreen.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.grade_entry_title),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.grade_entry_subtitle),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(EmeraldGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Grade, null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
            }
        }
        HorizontalDivider(color = Slate700.copy(alpha = 0.5f))

        if (myCourseEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape).background(Slate800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoStories, null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                    }
                    Text(stringResource(R.string.no_assigned_courses), color = TextSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(myCourseEntries) { entry ->
                    val courseInfo    = allCourses.find { it.courseCode == entry.courseCode }
                    val studentsInDept = allStudents.filter { it.department == courseInfo?.department || it.department == user.department }
                    val enteredCount  = allGrades.count { it.courseCode == entry.courseCode }
                    val pct = if (studentsInDept.isNotEmpty()) enteredCount.toFloat() / studentsInDept.size else 0f
                    GradeCourseCard(entry, studentsInDept.size, enteredCount, pct) { onCourseSelected(entry) }
                }
            }
        }
    }
}

@Composable
private fun GradeCourseCard(
    entry: ScheduleEntry,
    studentCount: Int,
    enteredCount: Int,
    pct: Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini circular progress
            Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(52.dp)) {
                    drawArc(
                        color = Slate700,
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    if (pct > 0f) {
                        drawArc(
                            color = EmeraldGreen,
                            startAngle = -90f, sweepAngle = 360f * pct, useCenter = false,
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                Icon(Icons.Default.Grade, null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.courseName, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(entry.courseCode, color = EmeraldGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(20.dp), color = EmeraldGreen.copy(alpha = 0.12f)) {
                        Text(
                            "$enteredCount / $studentCount ${stringResource(R.string.students_label)}",
                            color = EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (pct >= 1f) {
                        Surface(shape = RoundedCornerShape(20.dp), color = EmeraldGreen.copy(alpha = 0.18f)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(10.dp))
                                Text("Tamamlandı", color = EmeraldGreen, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun GradeEntryForCourse(
    entry: ScheduleEntry,
    courseInfo: Course?,
    students: List<Lecturer>,
    existingGrades: List<GradeRecord>,
    lecturerUsername: String,
    gradeViewModel: GradeViewModel,
    onBack: () -> Unit
) {
    val hasLab = courseInfo?.hasLab ?: false
    var expandedStudent by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Slate900,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = EmeraldGreen,
                    contentColor = Slate900,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(EmeraldGreen.copy(alpha = 0.12f), Color.Transparent)))
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                        }
                        Spacer(Modifier.width(4.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.courseName, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(entry.courseCode, color = EmeraldGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                if (hasLab) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF59E0B).copy(alpha = 0.15f)) {
                                        Text("Lab", color = Color(0xFFF59E0B), modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = Slate700) {
                            Text(
                                "${existingGrades.size}/${students.size}",
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
                HorizontalDivider(color = Slate700.copy(alpha = 0.5f))
            }
        }
    ) { padding ->
        if (students.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.PeopleOutline, null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
                    Text(stringResource(R.string.no_students_in_dept), color = TextSecondary, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 10.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(students, key = { it.username }) { student ->
                    val existing = existingGrades.find { it.studentUsername == student.username }
                    StudentGradeEntryCard(
                        student = student,
                        existing = existing,
                        hasLab = hasLab,
                        isExpanded = expandedStudent == student.username,
                        onToggle = { expandedStudent = if (expandedStudent == student.username) null else student.username },
                        onSave = { midterm, finalExam, assignment, lab ->
                            scope.launch {
                                gradeViewModel.saveGrade(
                                    existingId = existing?.id ?: "",
                                    studentUsername = student.username,
                                    studentName = student.fullName,
                                    courseCode = entry.courseCode,
                                    courseName = entry.courseName,
                                    department = courseInfo?.department ?: "",
                                    lecturerUsername = lecturerUsername,
                                    midterm = midterm, finalExam = finalExam,
                                    assignment = assignment, lab = lab,
                                    hasLab = hasLab
                                )
                                expandedStudent = null
                                snackbarHostState.showSnackbar("${student.fullName} — not kaydedildi ✓")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentGradeEntryCard(
    student: Lecturer,
    existing: GradeRecord?,
    hasLab: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSave: (Float, Float, Float, Float) -> Unit
) {
    var midterm    by remember(student.username, existing) { mutableStateOf(if (existing != null && existing.midterm >= 0)    existing.midterm.toString()    else "") }
    var finalExam  by remember(student.username, existing) { mutableStateOf(if (existing != null && existing.finalExam >= 0)  existing.finalExam.toString()  else "") }
    var assignment by remember(student.username, existing) { mutableStateOf(if (existing != null && existing.assignment >= 0) existing.assignment.toString() else "") }
    var lab        by remember(student.username, existing) { mutableStateOf(if (existing != null && hasLab && existing.lab >= 0) existing.lab.toString()     else "") }

    val previewAvg = remember(midterm, finalExam, assignment, lab) {
        val m = midterm.toFloatOrNull() ?: -1f
        val f = finalExam.toFloatOrNull() ?: -1f
        val a = assignment.toFloatOrNull() ?: -1f
        val l = lab.toFloatOrNull() ?: -1f
        if (m >= 0 && f >= 0 && a >= 0) GradeRecord.calculateAverage(m, f, a, l, hasLab) else -1f
    }
    val previewLetter = if (previewAvg >= 0) GradeRecord.calculateLetterGrade(previewAvg) else ""
    val letterColor   = if (previewLetter.isNotEmpty()) Color(GradeRecord.letterColor(previewLetter)) else TextSecondary

    val hasExisting = existing != null && existing.letterGrade.isNotEmpty()
    val existingColor = if (hasExisting) Color(GradeRecord.letterColor(existing!!.letterGrade)) else TextSecondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) Slate800 else Slate800
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isExpanded) CardDefaults.outlinedCardBorder().copy(width = 0.dp) else null
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape)
                        .background(
                            if (hasExisting) existingColor.copy(alpha = 0.12f)
                            else IndigoAccent.copy(alpha = 0.12f)
                        )
                        .border(1.5.dp,
                            if (hasExisting) existingColor.copy(alpha = 0.4f)
                            else IndigoAccent.copy(alpha = 0.2f),
                            CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        student.fullName.firstOrNull()?.uppercase() ?: "?",
                        color = if (hasExisting) existingColor else IndigoAccent,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(student.fullName, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    if (student.studentId.isNotBlank()) {
                        Text(student.studentId, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                // Existing grade badge
                if (hasExisting) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(10.dp), color = existingColor.copy(alpha = 0.14f)) {
                            Text(
                                existing!!.letterGrade,
                                color = existingColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text("%.1f".format(existing.gpa), color = existingColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = if (isExpanded) EmeraldGreen else TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = Slate700.copy(alpha = 0.6f))
                    Spacer(Modifier.height(2.dp))

                    // Weights info row
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Slate700.copy(alpha = 0.5f)).padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (hasLab) {
                            WeightChip("Vize", "25%")
                            WeightChip("Ödev", "10%")
                            WeightChip("Lab",  "15%")
                            WeightChip("Final","50%")
                        } else {
                            WeightChip("Vize", "30%")
                            WeightChip("Ödev", "10%")
                            WeightChip("Final","60%")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradeInputField(stringResource(R.string.grade_midterm), midterm, { midterm = it }, Modifier.weight(1f))
                        GradeInputField(stringResource(R.string.grade_assignment), assignment, { assignment = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (hasLab) GradeInputField(stringResource(R.string.grade_lab), lab, { lab = it }, Modifier.weight(1f))
                        GradeInputField(stringResource(R.string.grade_final), finalExam, { finalExam = it }, Modifier.weight(1f))
                    }

                    // Live preview
                    AnimatedVisibility(visible = previewLetter.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(letterColor.copy(alpha = 0.10f))
                                .border(1.dp, letterColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Ağırlıklı Ortalama", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                Text("%.1f / 100".format(previewAvg), color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(previewLetter, color = letterColor, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp))
                                Text("%.1f GPA".format(GradeRecord.letterToGpa(previewLetter)), color = letterColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val m = midterm.toFloatOrNull()    ?: return@Button
                            val f = finalExam.toFloatOrNull()  ?: return@Button
                            val a = assignment.toFloatOrNull() ?: return@Button
                            val l = if (hasLab) lab.toFloatOrNull() ?: 0f else -1f
                            onSave(m, f, a, l)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp), tint = Slate900)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.grade_save), fontWeight = FontWeight.Bold, color = Slate900)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightChip(label: String, weight: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(weight, color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun GradeInputField(
    label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 5) onValueChange(it) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        placeholder = { Text("0-100", color = TextSecondary.copy(alpha = 0.35f), style = MaterialTheme.typography.labelSmall) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldGreen, unfocusedBorderColor = Slate700,
            focusedLabelColor = EmeraldGreen, focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary, cursorColor = EmeraldGreen,
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
        ),
        singleLine = true
    )
}

// ─────────────────────────────────────────────────────────────
// STUDENT MY GRADES SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun StudentGradesScreen(
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    gradeViewModel: GradeViewModel,
    navController: NavController
) {
    val user = authViewModel.currentUser ?: return
    val allCourses by adminViewModel.courses.collectAsState()
    val myGradesFlow = remember(user.username) { gradeViewModel.getGradesByStudent(user.username) }
    val myGrades by myGradesFlow.collectAsState(initial = emptyList())

    val deptCourses = allCourses.filter { it.department == user.department }
    val cumulativeGpa = gradeViewModel.calculateCumulativeGpa(myGrades)
    val passedCount = myGrades.count { it.gpa >= 2.0f }
    val failedCount = myGrades.count { it.gpa in 0f..1.99f }
    val pendingCount = deptCourses.size - myGrades.size.coerceAtMost(deptCourses.size)

    Column(modifier = Modifier.fillMaxSize().background(Slate900)) {
        // ── Header ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(IndigoAccent.copy(alpha = 0.18f), Color.Transparent)))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.my_grades_title), color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(user.department, color = IndigoAccent, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
        }
        HorizontalDivider(color = Slate700.copy(alpha = 0.5f))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── GPA Hero Card ────────────────────────────────
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
                        // Circular GPA arc
                        GpaCircle(gpa = cumulativeGpa)
                        Spacer(Modifier.height(20.dp))
                        // Stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            GpaStatItem(passedCount.toString(), stringResource(R.string.grade_passed), EmeraldGreen)
                            Box(modifier = Modifier.width(1.dp).height(36.dp).background(Slate700))
                            GpaStatItem(failedCount.toString(), stringResource(R.string.grade_failed), ErrorRed)
                            Box(modifier = Modifier.width(1.dp).height(36.dp).background(Slate700))
                            GpaStatItem(pendingCount.toString(), "Bekleniyor", TextSecondary)
                        }
                    }
                }
            }

            item {
                Text(
                    "Ders Notları",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            if (deptCourses.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_dept_courses), color = TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(deptCourses) { course ->
                    val grade = myGrades.find { it.courseCode == course.courseCode }
                    StudentGradeCard(course = course, grade = grade)
                }
            }
        }
    }
}

@Composable
private fun GpaCircle(gpa: Float) {
    val pct = (gpa / 4.0f).coerceIn(0f, 1f)
    val color = when {
        gpa >= 3.0f -> EmeraldGreen
        gpa >= 2.0f -> IndigoAccent
        gpa > 0f    -> Color(0xFFF59E0B)
        else        -> Slate700
    }
    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(120.dp)) {
            drawArc(
                color = Slate700,
                startAngle = 135f, sweepAngle = 270f, useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
                size = Size(size.width - 10.dp.toPx(), size.height - 10.dp.toPx()),
                topLeft = Offset(5.dp.toPx(), 5.dp.toPx())
            )
            if (pct > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.5f), color)),
                    startAngle = 135f, sweepAngle = 270f * pct, useCenter = false,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(size.width - 10.dp.toPx(), size.height - 10.dp.toPx()),
                    topLeft = Offset(5.dp.toPx(), 5.dp.toPx())
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (gpa > 0f) "%.2f".format(gpa) else "--",
                color = color,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineMedium
            )
            Text("/ 4.00 GPA", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun GpaStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StudentGradeCard(course: Course, grade: GradeRecord?) {
    val hasGrade = grade != null && grade.letterGrade.isNotEmpty()
    val gradeColor = if (hasGrade) Color(GradeRecord.letterColor(grade!!.letterGrade)) else TextSecondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                    .background(gradeColor.copy(alpha = if (hasGrade) 0.12f else 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook, null,
                    tint = gradeColor.copy(alpha = if (hasGrade) 1f else 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(course.courseName, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(course.courseCode, color = if (hasGrade) gradeColor.copy(alpha = 0.7f) else TextSecondary, style = MaterialTheme.typography.labelSmall)
                if (hasGrade && grade!!.midterm >= 0) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (grade.midterm >= 0)    MiniTag("V: ${grade.midterm.toInt()}")
                        if (grade.assignment >= 0) MiniTag("Ö: ${grade.assignment.toInt()}")
                        if (grade.hasLab && grade.lab >= 0) MiniTag("L: ${grade.lab.toInt()}")
                        if (grade.finalExam >= 0)  MiniTag("F: ${grade.finalExam.toInt()}")
                    }
                }
            }
            if (hasGrade) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = RoundedCornerShape(10.dp), color = gradeColor.copy(alpha = 0.14f)) {
                        Text(
                            grade!!.letterGrade,
                            color = gradeColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text("%.1f".format(grade.gpa), color = gradeColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                }
            } else {
                Surface(shape = RoundedCornerShape(10.dp), color = Slate700) {
                    Text(
                        stringResource(R.string.grade_not_entered),
                        color = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniTag(text: String) {
    Surface(shape = RoundedCornerShape(6.dp), color = Slate700) {
        Text(text, color = TextSecondary, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}

// ─────────────────────────────────────────────────────────────
// ADMIN GRADES OVERVIEW SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun AdminGradesOverviewScreen(
    adminViewModel: AdminViewModel,
    gradeViewModel: GradeViewModel,
    navController: NavController
) {
    val allGrades  by gradeViewModel.allGrades.collectAsState()
    val allCourses by adminViewModel.courses.collectAsState()

    val totalEntered = allGrades.size
    val validGrades  = allGrades.filter { it.gpa >= 0 }
    val avgGpa   = if (validGrades.isNotEmpty()) validGrades.map { it.gpa }.average().toFloat() else 0f
    val passRate = if (allGrades.isNotEmpty()) allGrades.count { it.gpa >= 2.0f }.toFloat() / allGrades.size * 100 else 0f

    val gradeDistribution = listOf("AA","BA","BB","CB","CC","DC","DD","FF").map { letter ->
        letter to allGrades.count { it.letterGrade == letter }
    }
    val courseSummaries = allCourses.mapNotNull { course ->
        val cg = allGrades.filter { it.courseCode == course.courseCode }
        if (cg.isEmpty()) return@mapNotNull null
        val avg = cg.filter { it.gpa >= 0 }.map { it.gpa }.average().toFloat()
        Triple(course, cg.size, avg)
    }.sortedByDescending { it.third }

    Column(modifier = Modifier.fillMaxSize().background(Slate900)) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(EmeraldGreen.copy(alpha = 0.14f), Color.Transparent)))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.admin_grades_title), color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.admin_grades_subtitle), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        HorizontalDivider(color = Slate700.copy(alpha = 0.5f))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stat cards
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminGradeStatCard("%.2f".format(avgGpa), stringResource(R.string.avg_gpa), EmeraldGreen, Modifier.weight(1f))
                    AdminGradeStatCard(totalEntered.toString(), stringResource(R.string.total_entered), IndigoAccent, Modifier.weight(1f))
                    AdminGradeStatCard("%.0f%%".format(passRate), stringResource(R.string.pass_rate), Color(0xFFF59E0B), Modifier.weight(1f))
                }
            }

            // Grade distribution
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.grade_distribution), color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                        Spacer(Modifier.height(16.dp))
                        val maxCount = gradeDistribution.maxOfOrNull { it.second }.takeIf { it != null && it > 0 } ?: 1
                        gradeDistribution.forEach { (letter, count) ->
                            GradeDistributionBar(letter, count, maxCount, Color(GradeRecord.letterColor(letter)))
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Per-course summary
            if (courseSummaries.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.course_grade_summary), color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                }
                items(courseSummaries) { (course, count, avgGpaC) ->
                    val color = when {
                        avgGpaC >= 3.0f -> EmeraldGreen
                        avgGpaC >= 2.0f -> IndigoAccent
                        else            -> ErrorRed
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate800),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("%.1f".format(avgGpaC), color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(course.courseName, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(course.courseCode, color = color.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(stringResource(R.string.n_students, count), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminGradeStatCard(value: String, label: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(2.dp))
            Text(label, color = color.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun GradeDistributionBar(letter: String, count: Int, maxCount: Int, color: Color) {
    val fraction = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
    val animPct by animateFloatAsState(targetValue = fraction, animationSpec = tween(600), label = "bar")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(letter, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(28.dp))
        Box(
            modifier = Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(Slate700)
        ) {
            Box(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(animPct)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color)))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            count.toString(),
            color = if (count > 0) color else TextSecondary.copy(alpha = 0.4f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(22.dp),
            textAlign = TextAlign.End
        )
    }
}

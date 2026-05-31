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
import androidx.navigation.NavController
import com.example.academicmanager.R
import com.example.academicmanager.data.*
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.AdminViewModel
import com.example.academicmanager.ui.viewmodels.AssignmentViewModel
import com.example.academicmanager.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val AssignmentPurple = Color(0xFF8B5CF6)
private val AssignmentBlue   = Color(0xFF3B82F6)

// ─────────────────────────────────────────────────────────────
// LECTURER ASSIGNMENT SCREEN
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturerAssignmentScreen(
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    assignmentViewModel: AssignmentViewModel,
    navController: NavController
) {
    val user = authViewModel.currentUser ?: return
    val allCourses by adminViewModel.courses.collectAsState()
    val assignmentFlow = remember(user.username) {
        assignmentViewModel.getAssignmentsByLecturer(user.username)
    }
    val assignments by assignmentFlow.collectAsState(initial = emptyList())
    val scope    = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var showAdd      by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<AssignmentEntry?>(null) }
    var expandedId   by remember { mutableStateOf<String?>(null) }

    val today = LocalDate.now()
    val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val sorted = assignments.sortedWith(compareBy({ it.dueDate }, { it.title }))

    Scaffold(
        containerColor = Slate900,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(data, containerColor = AssignmentPurple, contentColor = Color.White, shape = RoundedCornerShape(12.dp))
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                containerColor = AssignmentPurple,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(16.dp),
                icon  = { Icon(Icons.Default.Add, null) },
                text  = { Text(stringResource(R.string.add_assignment), fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(AssignmentPurple.copy(alpha = 0.15f), Color.Transparent)))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.assignments_lecturer_title), color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.assignments_lecturer_sub, assignments.size), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    // Stats chips
                    val overdue  = sorted.count { try { LocalDate.parse(it.dueDate, fmt).isBefore(today) } catch (_: Exception) { false } }
                    val upcoming = sorted.count { try { !LocalDate.parse(it.dueDate, fmt).isBefore(today) } catch (_: Exception) { false } }
                    if (overdue > 0) AssignmentStatChip(overdue.toString(), "geçti", Color(0xFFEF4444))
                    Spacer(Modifier.width(4.dp))
                    if (upcoming > 0) AssignmentStatChip(upcoming.toString(), "aktif", AssignmentPurple)
                    Spacer(Modifier.width(8.dp))
                }
            }
            HorizontalDivider(color = Slate700.copy(alpha = 0.5f))

            if (sorted.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier = Modifier.size(96.dp).clip(CircleShape)
                                .background(Brush.radialGradient(listOf(AssignmentPurple.copy(alpha = 0.15f), Color.Transparent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Assignment, null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(46.dp))
                        }
                        Text(stringResource(R.string.no_assignments_yet), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.add_assignment_hint), color = TextSecondary.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sorted, key = { it.id }) { assignment ->
                        val isPast = try { LocalDate.parse(assignment.dueDate, fmt).isBefore(today) } catch (_: Exception) { false }
                        val subFlow = remember(assignment.id) { assignmentViewModel.getSubmissions(assignment.id) }
                        val submissions by subFlow.collectAsState(initial = emptyList())
                        LecturerAssignmentCard(
                            assignment  = assignment,
                            submissions = submissions,
                            isPast      = isPast,
                            isExpanded  = expandedId == assignment.id,
                            onExpand    = { expandedId = if (expandedId == assignment.id) null else assignment.id },
                            onDelete    = { deleteTarget = assignment }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddAssignmentDialog(
            courses   = allCourses,
            onDismiss = { showAdd = false },
            onAdd     = { entry ->
                assignmentViewModel.addAssignment(entry.copy(lecturerUsername = user.username, lecturerName = user.fullName))
                showAdd = false
                scope.launch { snackbar.showSnackbar("Ödev eklendi ✓") }
            }
        )
    }

    deleteTarget?.let { a ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = Slate800,
            title = { Text(stringResource(R.string.delete_assignment_title), color = ErrorRed, fontWeight = FontWeight.Bold) },
            text  = { Text(a.title, color = TextPrimary, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(onClick = { assignmentViewModel.deleteAssignment(a.id); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) {
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
private fun AssignmentStatChip(value: String, label: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            Text(label, color = color.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LecturerAssignmentCard(
    assignment: AssignmentEntry,
    submissions: List<AssignmentSubmission>,
    isPast: Boolean,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onDelete: () -> Unit
) {
    val today    = LocalDate.now()
    val fmt      = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val daysLeft = try { ChronoUnit.DAYS.between(today, LocalDate.parse(assignment.dueDate, fmt)) } catch (_: Exception) { null }
    val alpha    = if (isPast) 0.5f else 1f
    val barColor = when {
        isPast           -> TextSecondary.copy(alpha = 0.4f)
        daysLeft != null && daysLeft <= 2 -> Color(0xFFEF4444)
        daysLeft != null && daysLeft <= 5 -> Color(0xFFF59E0B)
        else             -> AssignmentPurple
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Slate800),
        shape    = RoundedCornerShape(16.dp),
        border   = BorderStroke(1.dp, barColor.copy(alpha = if (isPast) 0.1f else 0.25f))
    ) {
        Row(modifier = Modifier.clickable(onClick = onExpand)) {
            Box(modifier = Modifier.width(5.dp).fillMaxHeight()
                .background(Brush.verticalGradient(listOf(barColor, barColor.copy(alpha = 0.3f))))
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(assignment.title, color = TextPrimary.copy(alpha = alpha), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text(assignment.courseName, color = barColor.copy(alpha = alpha * 0.8f), style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.width(8.dp))
                    // Submission count badge
                    Surface(shape = RoundedCornerShape(8.dp), color = EmeraldGreen.copy(alpha = 0.12f)) {
                        Text("${submissions.size} teslim", color = EmeraldGreen.copy(alpha = alpha), modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, null, tint = ErrorRed.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssignmentInfoChip(Icons.Default.CalendarToday, assignment.dueDate, alpha)
                    AssignmentInfoChip(Icons.Default.AccessTime, assignment.dueTime, alpha)
                    if (daysLeft != null && !isPast) {
                        Surface(shape = RoundedCornerShape(6.dp), color = barColor.copy(alpha = 0.12f)) {
                            Text(if (daysLeft == 0L) "Bugün!" else "${daysLeft}g kaldı", color = barColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    } else if (isPast) {
                        Surface(shape = RoundedCornerShape(6.dp), color = TextSecondary.copy(alpha = 0.08f)) {
                            Text("Süresi doldu", color = TextSecondary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Expanded: submission list
                AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        HorizontalDivider(color = Slate700.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 8.dp))
                        if (assignment.description.isNotBlank()) {
                            Text(assignment.description, color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Text("Teslim Edenler (${submissions.size})", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        if (submissions.isEmpty()) {
                            Text(stringResource(R.string.no_submissions_yet), color = TextSecondary.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 4.dp))
                        } else {
                            submissions.forEach { sub ->
                                Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                    Text(sub.studentName.ifBlank { sub.studentUsername }, color = TextPrimary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                    if (sub.isLate) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF59E0B).copy(alpha = 0.15f)) {
                                            Text("Geç", color = Color(0xFFF59E0B), modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                if (sub.note.isNotBlank()) {
                                    Text("  ↳ ${sub.note}", color = TextSecondary.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 22.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAssignmentDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onAdd: (AssignmentEntry) -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var courseCode  by remember { mutableStateOf("") }
    var courseName  by remember { mutableStateOf("") }
    var department  by remember { mutableStateOf("") }
    var dueDate     by remember { mutableStateOf(LocalDate.now().plusDays(7).format(fmt)) }
    var dueTime     by remember { mutableStateOf("23:59") }
    var maxPoints   by remember { mutableStateOf("100") }
    var showCourse  by remember { mutableStateOf(false) }

    val parsed = remember(dueDate) { try { LocalDate.parse(dueDate, fmt) } catch (_: Exception) { LocalDate.now().plusDays(7) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Slate800,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(AssignmentPurple.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Assignment, null, tint = AssignmentPurple, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.add_assignment), color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Title
                AssignmentField(stringResource(R.string.assignment_title_field), title, { title = it }, Modifier.fillMaxWidth())

                // Course selector
                OutlinedButton(
                    onClick = { showCourse = !showCourse },
                    modifier = Modifier.fillMaxWidth(),
                    shape  = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (courseName.isNotBlank()) AssignmentPurple.copy(alpha = 0.5f) else Slate700)
                ) {
                    Icon(Icons.Default.School, null, modifier = Modifier.size(14.dp), tint = if (courseName.isNotBlank()) AssignmentPurple else TextSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text(courseName.ifBlank { stringResource(R.string.select_course_assignment) }, color = if (courseName.isNotBlank()) AssignmentPurple else TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(if (showCourse) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                AnimatedVisibility(visible = showCourse, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Slate700), shape = RoundedCornerShape(12.dp)) {
                        Column {
                            courses.take(12).forEach { c ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        courseCode = c.courseCode; courseName = c.courseName; department = c.department; showCourse = false
                                    }.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(c.courseName, color = TextPrimary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(c.courseCode, color = AssignmentPurple, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                if (c != courses.take(12).last()) HorizontalDivider(color = Slate800.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 8.dp))
                            }
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text(stringResource(R.string.assignment_description), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), maxLines = 3,
                    colors = assignmentFieldColors()
                )

                // Due date navigator
                Text(stringResource(R.string.assignment_due_date), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Slate700.copy(alpha = 0.6f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { dueDate = parsed.minusDays(1).format(fmt) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ChevronLeft, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    Text(dueDate, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    IconButton(onClick = { dueDate = parsed.plusDays(1).format(fmt) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // Time + Points row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssignmentField(stringResource(R.string.assignment_due_time), dueTime, { dueTime = it }, Modifier.weight(1f), KeyboardType.Number)
                    AssignmentField(stringResource(R.string.assignment_max_points), maxPoints, { maxPoints = it }, Modifier.weight(1f), KeyboardType.Number)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || courseName.isBlank()) return@Button
                    onAdd(AssignmentEntry(
                        title = title.trim(), description = description.trim(),
                        courseCode = courseCode, courseName = courseName, department = department,
                        dueDate = dueDate, dueTime = dueTime,
                        maxPoints = maxPoints.toIntOrNull() ?: 100
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = AssignmentPurple),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add_assignment), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = TextSecondary) }
        }
    )
}

@Composable
private fun assignmentFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AssignmentPurple, unfocusedBorderColor = Slate700,
    focusedLabelColor = AssignmentPurple, focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary, cursorColor = AssignmentPurple,
    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
)

@Composable
private fun AssignmentField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier, shape = RoundedCornerShape(10.dp), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = assignmentFieldColors()
    )
}

@Composable
private fun AssignmentInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, alpha: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, tint = TextSecondary.copy(alpha = alpha * 0.7f), modifier = Modifier.size(11.dp))
        Text(text, color = TextSecondary.copy(alpha = alpha * 0.8f), style = MaterialTheme.typography.labelSmall)
    }
}

// ─────────────────────────────────────────────────────────────
// STUDENT ASSIGNMENT SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun StudentAssignmentScreen(
    authViewModel: AuthViewModel,
    assignmentViewModel: AssignmentViewModel,
    navController: NavController
) {
    val user = authViewModel.currentUser ?: return
    val assignmentFlow = remember(user.department) { assignmentViewModel.getAssignmentsByDepartment(user.department) }
    val assignments    by assignmentFlow.collectAsState(initial = emptyList())
    val submissionFlow = remember(user.username) { assignmentViewModel.getSubmissionsByStudent(user.username) }
    val mySubmissions  by submissionFlow.collectAsState(initial = emptyList())

    val today = LocalDate.now()
    val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val sorted   = assignments.sortedWith(compareBy({ it.dueDate }, { it.title }))
    val upcoming = sorted.filter { try { !LocalDate.parse(it.dueDate, fmt).isBefore(today) } catch (_: Exception) { false } }
    val past     = sorted.filter { try { LocalDate.parse(it.dueDate, fmt).isBefore(today) } catch (_: Exception) { false } }

    val submittedIds = mySubmissions.map { it.assignmentId }.toSet()
    val nextAssignment = upcoming.firstOrNull()
    val daysToNext = nextAssignment?.let { try { ChronoUnit.DAYS.between(today, LocalDate.parse(it.dueDate, fmt)) } catch (_: Exception) { null } }

    var showPast     by remember { mutableStateOf(false) }
    var submitTarget by remember { mutableStateOf<AssignmentEntry?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Slate900)) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AssignmentPurple.copy(alpha = 0.18f), AssignmentBlue.copy(alpha = 0.06f), Color.Transparent)))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.assignments_student_title), color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(user.department, color = AssignmentPurple.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                // Submitted count
                val submittedUpcoming = upcoming.count { submittedIds.contains(it.id) }
                if (upcoming.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = EmeraldGreen.copy(alpha = 0.12f)) {
                        Text("$submittedUpcoming/${upcoming.size} teslim", color = EmeraldGreen, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
        }
        HorizontalDivider(color = Slate700.copy(alpha = 0.4f))

        if (assignments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.size(100.dp).clip(CircleShape)
                            .background(Brush.radialGradient(listOf(AssignmentPurple.copy(alpha = 0.15f), Color.Transparent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(50.dp))
                    }
                    Text(stringResource(R.string.no_assignments_dept), color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Next assignment hero
                if (nextAssignment != null && daysToNext != null) {
                    item {
                        NextAssignmentHeroCard(assignment = nextAssignment, daysToNext = daysToNext, isSubmitted = submittedIds.contains(nextAssignment.id))
                        Spacer(Modifier.height(18.dp))
                    }
                }

                // Upcoming header
                if (upcoming.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Pending, null, tint = AssignmentPurple, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.upcoming_assignments), color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = CircleShape, color = AssignmentPurple.copy(alpha = 0.15f)) {
                                Text(upcoming.size.toString(), color = AssignmentPurple, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                items(upcoming, key = { it.id }) { assignment ->
                    val isSubmitted = submittedIds.contains(assignment.id)
                    StudentAssignmentCard(
                        assignment  = assignment,
                        isSubmitted = isSubmitted,
                        today       = today,
                        fmt         = fmt,
                        onSubmit    = { submitTarget = assignment }
                    )
                    Spacer(Modifier.height(10.dp))
                }

                // Past toggle
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
                            Text(stringResource(R.string.past_assignments, past.size), color = TextSecondary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Icon(if (showPast) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (showPast) {
                        items(past.reversed(), key = { "past_${it.id}" }) { assignment ->
                            val isSubmitted = submittedIds.contains(assignment.id)
                            StudentAssignmentCard(assignment = assignment, isSubmitted = isSubmitted, today = today, fmt = fmt, isPast = true, onSubmit = { submitTarget = assignment })
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    submitTarget?.let { assignment ->
        SubmitAssignmentDialog(
            assignment  = assignment,
            today       = today,
            fmt         = fmt,
            onDismiss   = { submitTarget = null },
            onSubmit    = { note ->
                val isLate = try { LocalDate.parse(assignment.dueDate, fmt).isBefore(today) } catch (_: Exception) { false }
                assignmentViewModel.submitAssignment(AssignmentSubmission(
                    assignmentId    = assignment.id,
                    studentUsername = user.username,
                    studentName     = user.fullName,
                    department      = user.department,
                    note            = note,
                    isLate          = isLate
                ))
                submitTarget = null
            }
        )
    }
}

@Composable
private fun NextAssignmentHeroCard(assignment: AssignmentEntry, daysToNext: Long, isSubmitted: Boolean) {
    val isToday  = daysToNext == 0L
    val gradientBrush = Brush.linearGradient(
        colors = listOf(AssignmentPurple.copy(alpha = if (isSubmitted) 0.12f else 0.28f), AssignmentBlue.copy(alpha = 0.08f), Slate800)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape    = RoundedCornerShape(24.dp),
        border   = BorderStroke(1.dp, (if (isSubmitted) EmeraldGreen else AssignmentPurple).copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(gradientBrush)) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = (if (isSubmitted) EmeraldGreen else AssignmentPurple).copy(alpha = 0.18f)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (isSubmitted) Icons.Default.CheckCircle else Icons.Default.Star, null, tint = if (isSubmitted) EmeraldGreen else AssignmentPurple, modifier = Modifier.size(10.dp))
                            Text(if (isSubmitted) "Teslim Edildi" else stringResource(R.string.next_deadline_label), color = if (isSubmitted) EmeraldGreen else AssignmentPurple, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(assignment.title, color = TextPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(assignment.courseName, color = AssignmentPurple.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            AssignmentInfoChip(Icons.Default.CalendarMonth, assignment.dueDate, 1f)
                            AssignmentInfoChip(Icons.Default.AccessTime, assignment.dueTime, 1f)
                        }
                    }
                    if (!isSubmitted) {
                        Surface(
                            shape  = RoundedCornerShape(16.dp),
                            color  = AssignmentPurple.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, AssignmentPurple.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isToday) {
                                    Text("BUGÜN!", color = Color(0xFFEF4444), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                } else {
                                    Text(daysToNext.toString(), color = AssignmentPurple, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                                    Text(stringResource(R.string.days_remaining), color = AssignmentPurple.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentAssignmentCard(
    assignment: AssignmentEntry,
    isSubmitted: Boolean,
    today: LocalDate,
    fmt: DateTimeFormatter,
    isPast: Boolean = false,
    onSubmit: () -> Unit
) {
    val daysLeft = try { ChronoUnit.DAYS.between(today, LocalDate.parse(assignment.dueDate, fmt)) } catch (_: Exception) { null }
    val isOverdue = isPast && !isSubmitted
    val alpha     = if (isPast && !isSubmitted) 0.55f else 1f

    val statusColor = when {
        isSubmitted                       -> EmeraldGreen
        isOverdue                         -> Color(0xFFEF4444)
        daysLeft != null && daysLeft <= 2 -> Color(0xFFEF4444)
        daysLeft != null && daysLeft <= 5 -> Color(0xFFF59E0B)
        else                              -> AssignmentPurple
    }

    val dateDisplay = try {
        LocalDate.parse(assignment.dueDate, fmt).format(DateTimeFormatter.ofPattern("d MMM, EEEE", java.util.Locale("tr","TR")))
    } catch (_: Exception) { assignment.dueDate }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline dot
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(statusColor.copy(alpha = alpha)), contentAlignment = Alignment.Center) {
                if (isSubmitted) Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Slate900))
            }
        }
        Spacer(Modifier.width(8.dp))

        Card(
            modifier = Modifier.weight(1f),
            colors   = CardDefaults.cardColors(containerColor = if (isOverdue) Slate800.copy(alpha = 0.6f) else Slate800),
            shape    = RoundedCornerShape(16.dp),
            border   = if (!isPast || isSubmitted) BorderStroke(1.dp, statusColor.copy(alpha = 0.22f)) else null
        ) {
            Row {
                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(statusColor.copy(alpha = alpha)).clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Status badge
                        Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = if (isPast) 0.08f else 0.14f)) {
                            Text(
                                when {
                                    isSubmitted -> stringResource(R.string.submitted_badge)
                                    isOverdue   -> stringResource(R.string.overdue_badge)
                                    daysLeft == 0L -> "Bugün!"
                                    else        -> stringResource(R.string.pending_badge)
                                },
                                color = statusColor.copy(alpha = alpha),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (!isSubmitted && !isOverdue && daysLeft != null) {
                            Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.10f)) {
                                Text("+${daysLeft}g", color = statusColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        } else if (isSubmitted) {
                            Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(assignment.title, color = TextPrimary.copy(alpha = alpha), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(assignment.courseName, color = TextSecondary.copy(alpha = alpha * 0.8f), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AssignmentInfoChip(Icons.Default.CalendarMonth, dateDisplay, alpha)
                        AssignmentInfoChip(Icons.Default.AccessTime, assignment.dueTime, alpha)
                        Spacer(Modifier.weight(1f))
                        if (!isSubmitted) {
                            Button(
                                onClick = onSubmit,
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isOverdue) Color(0xFFF59E0B) else AssignmentPurple),
                                shape  = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isOverdue) stringResource(R.string.submit_late) else stringResource(R.string.submit_assignment), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (assignment.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(assignment.description, color = TextSecondary.copy(alpha = alpha * 0.7f), style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmitAssignmentDialog(
    assignment: AssignmentEntry,
    today: LocalDate,
    fmt: DateTimeFormatter,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val isLate = try { LocalDate.parse(assignment.dueDate, fmt).isBefore(today) } catch (_: Exception) { false }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Slate800,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background((if (isLate) Color(0xFFF59E0B) else AssignmentPurple).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AssignmentTurnedIn, null, tint = if (isLate) Color(0xFFF59E0B) else AssignmentPurple, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(stringResource(R.string.submit_assignment), color = TextPrimary, fontWeight = FontWeight.Bold)
                    if (isLate) Text("Geç teslim", color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(assignment.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text("${assignment.courseName} · Son teslim: ${assignment.dueDate} ${assignment.dueTime}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text(stringResource(R.string.submission_note), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), maxLines = 3,
                    colors = assignmentFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(note.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = if (isLate) Color(0xFFF59E0B) else AssignmentPurple),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isLate) "Geç Teslim Et" else stringResource(R.string.submit_assignment), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = TextSecondary) }
        }
    )
}

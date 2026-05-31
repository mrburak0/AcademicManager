package com.example.academicmanager.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.academicmanager.R
import com.example.academicmanager.data.*
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.*

// English keys must match Firestore values — do not localize
private val WEEK_DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

// Localized composable helpers — use these in UI, not hardcoded lists
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

private fun dayLocalizedName(englishDay: String, localizedList: List<String>) =
    localizedList.getOrElse(WEEK_DAYS.indexOf(englishDay)) { englishDay }

private fun dayLocalizedShort(englishDay: String, localizedList: List<String>) =
    localizedList.getOrElse(WEEK_DAYS.indexOf(englishDay)) { englishDay.take(3) }
private val TIME_SLOTS = listOf(
    "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00",
    "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00"
)

// ─────────────────────────────────────────────────────────────
// ADMIN HOME SCREEN
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(viewModel: AdminViewModel, navController: NavController) {
    val unassignedLecturers   by viewModel.unassignedLecturers.collectAsState()
    val unassignedCourses     by viewModel.unassignedCourses.collectAsState()
    val classrooms            by viewModel.classrooms.collectAsState()
    val scheduleEntries       by viewModel.scheduleEntries.collectAsState()
    val pendingRequests        by viewModel.pendingRequests.collectAsState()
    val pendingRegistrations   by viewModel.pendingRegistrations.collectAsState()
    val pendingAvailabilities  by viewModel.pendingAvailabilities.collectAsState()
    val students               by viewModel.students.collectAsState()
    val allAvailabilities      by viewModel.availabilities.collectAsState()
    val courses                by viewModel.courses.collectAsState()
    val context = LocalContext.current

    var lecturerExpanded  by remember { mutableStateOf(true) }
    var courseExpanded    by remember { mutableStateOf(true) }
    var scheduleExpanded  by remember { mutableStateOf(false) }
    var studentExpanded   by remember { mutableStateOf(false) }
    var requestExpanded   by remember { mutableStateOf(true) }
    var regExpanded       by remember { mutableStateOf(true) }
    var availExpanded     by remember { mutableStateOf(true) }

    // Reject dialog state (availability)
    var rejectAvailTarget  by remember { mutableStateOf<LecturerAvailability?>(null) }
    var rejectAvailNote    by remember { mutableStateOf("") }
    // View availability + schedule dialog
    var viewAvailTarget    by remember { mutableStateOf<LecturerAvailability?>(null) }

    // Student add dialog state
    var showAddStudentDialog  by remember { mutableStateOf(false) }
    var studentFullName       by remember { mutableStateOf("") }
    var studentUsername       by remember { mutableStateOf("") }
    var studentPassword       by remember { mutableStateOf("") }
    var studentDepartment     by remember { mutableStateOf("") }
    var studentYear           by remember { mutableStateOf("") }
    var studentId             by remember { mutableStateOf("") }
    var yearDropdownExpanded  by remember { mutableStateOf(false) }
    var deptDropdownExpanded  by remember { mutableStateOf(false) }

    val yearOptions = listOf(
        stringResource(R.string.year_1), stringResource(R.string.year_2),
        stringResource(R.string.year_3), stringResource(R.string.year_4),
        stringResource(R.string.masters_1), stringResource(R.string.masters_2),
        stringResource(R.string.phd_1), stringResource(R.string.phd_2),
        stringResource(R.string.phd_3), stringResource(R.string.phd_4)
    )
    val deptOptions = listOf(
        "Bilgisayar Mühendisliği", "Yazılım Mühendisliği", "Elektrik-Elektronik Mühendisliği",
        "Makine Mühendisliği", "İnşaat Mühendisliği", "Endüstri Mühendisliği",
        "Kimya Mühendisliği", "Biyomedikal Mühendisliği", "Çevre Mühendisliği",
        "Havacılık ve Uzay Mühendisliği", "Malzeme Bilimi ve Mühendisliği",
        "Gıda Mühendisliği", "Mekatronik Mühendisliği", "Yapay Zeka Mühendisliği",
        "Enerji Sistemleri Mühendisliği", "Kontrol ve Otomasyon Mühendisliği",
        "Matematik", "Fizik", "Kimya", "Biyoloji", "İstatistik",
        "Moleküler Biyoloji ve Genetik", "Coğrafya", "Jeofizik Mühendisliği",
        "Tıp", "Diş Hekimliği", "Eczacılık", "Hemşirelik", "Ebelik",
        "Fizyoterapi ve Rehabilitasyon", "Beslenme ve Diyetetik", "Veteriner Hekimliği",
        "Hukuk",
        "İktisat", "İşletme", "Maliye", "Bankacılık ve Finans",
        "Muhasebe ve Finans Yönetimi", "Kamu Yönetimi", "Siyaset Bilimi",
        "Uluslararası İlişkiler", "Uluslararası Ticaret", "Yönetim Bilişim Sistemleri",
        "Lojistik Yönetimi", "Çalışma Ekonomisi ve Endüstri İlişkileri",
        "Psikoloji", "Sosyoloji", "Tarih", "Arkeoloji", "Felsefe", "Antropoloji",
        "Türk Dili ve Edebiyatı", "İngiliz Dili ve Edebiyatı",
        "Alman Dili ve Edebiyatı", "Fransız Dili ve Edebiyatı",
        "Mimarlık", "İç Mimarlık", "Peyzaj Mimarlığı", "Şehir ve Bölge Planlama",
        "Endüstriyel Tasarım", "Grafik Tasarım", "Moda Tasarımı",
        "Eğitim Bilimleri", "Bilgisayar ve Öğretim Teknolojileri",
        "Okul Öncesi Öğretmenliği", "İngilizce Öğretmenliği",
        "Rehberlik ve Psikolojik Danışmanlık", "Özel Eğitim",
        "Beden Eğitimi ve Spor Öğretmenliği",
        "İletişim", "Gazetecilik", "Radyo, Televizyon ve Sinema",
        "Halkla İlişkiler ve Reklamcılık", "Dijital Medya",
        "Güzel Sanatlar", "Müzik", "Tiyatro", "Resim",
        "Turizm İşletmeciliği", "Gastronomi ve Mutfak Sanatları",
        "Spor Bilimleri", "Antrenörlük Eğitimi",
        "Diğer"
    )

    // Reject dialog state (schedule requests)
    var rejectTarget          by remember { mutableStateOf<com.example.academicmanager.data.ScheduleRequest?>(null) }
    var rejectNote            by remember { mutableStateOf("") }
    // Reject dialog state (registrations)
    var rejectRegTarget       by remember { mutableStateOf<com.example.academicmanager.data.Lecturer?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Quick Setup Banner ────────────────────────────────
        item {
            QuickSetupCard(
                isEmpty = unassignedLecturers.isEmpty() && unassignedCourses.isEmpty() && classrooms.isEmpty(),
                onSeed = {
                    viewModel.seedDemoData { success ->
                        Toast.makeText(
                            context,
                            if (success) context.getString(R.string.demo_loaded)
                            else context.getString(R.string.demo_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }

        // ── Header ──────────────────────────────────────────
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AcademicLogo(modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.admin_dashboard),
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColorState.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.admin_dashboard_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColorState.textSecondary
                    )
                }
                // Duyurular bell ikonu
                IconButton(onClick = { navController.navigate("announcements") }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Duyurular", tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                }
            }
        }

        // ── 3 Stat Cards ────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminStatCard(
                    title = stringResource(R.string.panel_unassigned_lecturers),
                    value = unassignedLecturers.size.toString(),
                    icon = Icons.Default.Person,
                    accentColor = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = stringResource(R.string.panel_unassigned_courses),
                    value = unassignedCourses.size.toString(),
                    icon = Icons.AutoMirrored.Filled.List,
                    accentColor = IndigoAccent,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = stringResource(R.string.panel_total_classrooms),
                    value = classrooms.size.toString(),
                    icon = Icons.Default.Home,
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Pending Availability Notifications ───────────────
        item {
            PanelCard(
                title        = stringResource(R.string.admin_avail_pending_section),
                count        = pendingAvailabilities.size,
                accentColor  = Color(0xFF10B981),
                expanded     = availExpanded,
                onToggle     = { availExpanded = !availExpanded }
            )
        }
        if (availExpanded) {
            if (pendingAvailabilities.isEmpty()) {
                item {
                    AllAssignedBanner(stringResource(R.string.admin_avail_no_pending))
                }
            } else {
                items(pendingAvailabilities) { avail ->
                    PendingAvailabilityCard(
                        avail     = avail,
                        onApprove = { viewModel.approveAvailability(avail) },
                        onReject  = { rejectAvailTarget = avail; rejectAvailNote = "" },
                        onView    = { viewAvailTarget = avail }
                    )
                }
            }
            item {
                OutlinedButton(
                    onClick = { navController.navigate("admin_availability") },
                    modifier    = Modifier.fillMaxWidth(),
                    border      = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                    colors      = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                    shape       = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.admin_avail_view_btn), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Pending Requests Panel ───────────────────────────
        if (pendingRequests.isNotEmpty()) {
            item {
                PanelCard(
                    title = stringResource(R.string.panel_availability),
                    count = pendingRequests.size,
                    accentColor = Color(0xFFF59E0B),
                    expanded = requestExpanded,
                    onToggle = { requestExpanded = !requestExpanded }
                )
            }
            if (requestExpanded) {
                items(pendingRequests) { request ->
                    PendingRequestCard(
                        request = request,
                        onApprove = { viewModel.approveScheduleRequest(request) },
                        onReject  = { rejectTarget = request; rejectNote = "" }
                    )
                }
            }
        }

        // ── Pending Registrations Panel ──────────────────────
        if (pendingRegistrations.isNotEmpty()) {
            item {
                PanelCard(
                    title = stringResource(R.string.panel_pending_reg),
                    count = pendingRegistrations.size,
                    accentColor = Color(0xFF6366F1),
                    expanded = regExpanded,
                    onToggle = { regExpanded = !regExpanded }
                )
            }
            if (regExpanded) {
                items(pendingRegistrations) { reg ->
                    PendingRegistrationCard(
                        lecturer  = reg,
                        onApprove = { viewModel.approveRegistration(reg) },
                        onReject  = { rejectRegTarget = reg }
                    )
                }
            }
        }

        // ── Unassigned Lecturers ─────────────────────────────
        item {
            PanelCard(
                title = stringResource(R.string.panel_unassigned_lecturers),
                count = unassignedLecturers.size,
                accentColor = EmeraldGreen,
                expanded = lecturerExpanded,
                onToggle = { lecturerExpanded = !lecturerExpanded }
            )
        }
        if (lecturerExpanded) {
            if (unassignedLecturers.isEmpty()) {
                item {
                    AllAssignedBanner(stringResource(R.string.all_lecturers_assigned))
                }
            } else {
                items(unassignedLecturers) { lecturer ->
                    val lecturerAvail = allAvailabilities
                        .filter { it.lecturerUsername == lecturer.username && it.status == AvailabilityStatus.APPROVED }
                        .maxByOrNull { it.timestamp }
                    LecturerInfoRow(
                        lecturer = lecturer,
                        onDelete = { viewModel.deleteLecturer(lecturer.username) },
                        onResetPassword = { newPwd, cb -> viewModel.resetLecturerPassword(lecturer, newPwd, cb) },
                        availability = lecturerAvail,
                        unassignedCourses = unassignedCourses,
                        classrooms = classrooms,
                        onAssignCourse = { course, classroom, day, time, session ->
                            viewModel.assignCourse(course, lecturer, classroom, day, time, session)
                        },
                        assignmentResult = viewModel.assignmentResult
                    )
                }
            }
        }

        // ── Unassigned Courses ───────────────────────────────
        item {
            PanelCard(
                title = stringResource(R.string.panel_unassigned_courses),
                count = unassignedCourses.size,
                accentColor = IndigoAccent,
                expanded = courseExpanded,
                onToggle = { courseExpanded = !courseExpanded }
            )
        }
        if (courseExpanded) {
            if (unassignedCourses.isEmpty()) {
                item {
                    AllAssignedBanner(stringResource(R.string.all_courses_scheduled))
                }
            } else {
                items(unassignedCourses) { course ->
                    CourseInfoRow(
                        course = course,
                        onDelete = { viewModel.deleteCourse(course.courseCode) }
                    )
                }
            }
        }

        // ── Recent Schedule Entries ──────────────────────────
        item {
            PanelCard(
                title = stringResource(R.string.panel_schedule),
                count = scheduleEntries.size,
                accentColor = Color(0xFFF59E0B),
                expanded = scheduleExpanded,
                onToggle = { scheduleExpanded = !scheduleExpanded }
            )
        }
        if (scheduleExpanded) {
            if (scheduleEntries.isEmpty()) {
                item {
                    AllAssignedBanner(stringResource(R.string.no_schedule_yet))
                }
            } else {
                items(scheduleEntries.takeLast(10).reversed()) { entry ->
                    ScheduleEntryInfoRow(entry)
                }
            }
        }

        // ── Student Management ─────────────────────────────────
        item {
            PanelCard(
                title = stringResource(R.string.panel_student_mgmt),
                count = students.size,
                accentColor = Color(0xFF8B5CF6),
                expanded = studentExpanded,
                onToggle = { studentExpanded = !studentExpanded }
            )
        }
        if (studentExpanded) {
            if (students.isEmpty()) {
                item {
                    AllAssignedBanner(stringResource(R.string.no_students_yet))
                }
            } else {
                items(students) { student ->
                    StudentInfoRow(
                        student = student,
                        onDelete = { viewModel.deleteLecturer(student.username) }
                    )
                }
            }
            item {
                Button(
                    onClick = { showAddStudentDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_student_title), fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Grades, Exams, Assignments, Calendar, Announcements ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { navController.navigate("admin_grades") },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Grade, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.panel_grades), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { navController.navigate("admin_exam_schedule") },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.exam_schedule_action), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { navController.navigate("lecturer_assignments") },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.nav_assignments), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { navController.navigate("admin_academic_cal") },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.nav_academic_calendar), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedButton(
                    onClick = { navController.navigate("announcements") },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.nav_announcements), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    // ── Availability Reject Dialog ───────────────────────────
    rejectAvailTarget?.let { avail ->
        AlertDialog(
            onDismissRequest = { rejectAvailTarget = null; rejectAvailNote = "" },
            containerColor = AppColorState.surface,
            title = { Text(stringResource(R.string.reject_notification_title), color = ErrorRed, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.reject_notification_msg, avail.lecturerName), color = AppColorState.textPrimary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectAvailNote, onValueChange = { rejectAvailNote = it },
                        label = { Text(stringResource(R.string.rejection_reason_label)) },
                        placeholder = { Text(stringResource(R.string.rejection_reason_hint), color = AppColorState.textSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ErrorRed, unfocusedBorderColor = AppColorState.surface2,
                            focusedLabelColor = ErrorRed, unfocusedLabelColor = AppColorState.textSecondary,
                            focusedTextColor = AppColorState.textPrimary, unfocusedTextColor = AppColorState.textPrimary,
                            cursorColor = ErrorRed, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.rejectAvailability(avail, rejectAvailNote); rejectAvailTarget = null; rejectAvailNote = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text(stringResource(R.string.reject)) }
            },
            dismissButton = {
                TextButton(onClick = { rejectAvailTarget = null; rejectAvailNote = "" }) { Text(stringResource(R.string.cancel), color = AppColorState.textSecondary) }
            }
        )
    }

    // ── Reject Note Dialog ───────────────────────────────────
    rejectTarget?.let { req ->
        AlertDialog(
            onDismissRequest = { rejectTarget = null; rejectNote = "" },
            containerColor = AppColorState.surface,
            title = { Text(stringResource(R.string.reject_notification_title), color = ErrorRed, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.reject_notification_msg, req.lecturerName), color = AppColorState.textPrimary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectNote,
                        onValueChange = { rejectNote = it },
                        label = { Text(stringResource(R.string.rejection_reason_label)) },
                        placeholder = { Text(stringResource(R.string.rejection_reason_hint), color = AppColorState.textSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor    = ErrorRed,
                            unfocusedBorderColor  = AppColorState.surface2,
                            focusedLabelColor     = ErrorRed,
                            unfocusedLabelColor   = AppColorState.textSecondary,
                            focusedTextColor      = AppColorState.textPrimary,
                            unfocusedTextColor    = AppColorState.textPrimary,
                            cursorColor           = ErrorRed,
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.rejectScheduleRequest(req, rejectNote); rejectTarget = null; rejectNote = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text(stringResource(R.string.reject)) }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null; rejectNote = "" }) { Text(stringResource(R.string.cancel), color = AppColorState.textSecondary) }
            }
        )
    }

    // ── Registration Reject Dialog ────────────────────────────
    rejectRegTarget?.let { reg ->
        AlertDialog(
            onDismissRequest = { rejectRegTarget = null },
            containerColor = AppColorState.surface,
            title = { Text(stringResource(R.string.reject_reg_title), color = ErrorRed, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.reject_reg_msg, reg.fullName, reg.username), color = AppColorState.textPrimary, style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                Button(onClick = { viewModel.rejectRegistration(reg); rejectRegTarget = null }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) { Text(stringResource(R.string.reject_and_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { rejectRegTarget = null }) { Text(stringResource(R.string.cancel), color = AppColorState.textSecondary) }
            }
        )
    }

    // ── Availability View Dialog (müsaitlik haritası + ders takvimi) ──
    viewAvailTarget?.let { avail ->
        val lecturerEntries = scheduleEntries.filter { it.lecturerName == avail.lecturerName }
        AlertDialog(
            onDismissRequest = { viewAvailTarget = null },
            containerColor = AppColorState.surface,
            modifier = Modifier.fillMaxWidth(),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(EmeraldGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(avail.lecturerName.firstOrNull()?.uppercase() ?: "?", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(avail.lecturerName, color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("${avail.totalSlots} müsait slot", color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Müsaitlik haritası
                    Text(stringResource(R.string.lecturer_availability_map), color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    AdminAvailabilityGrid(avail = avail, accentColor = EmeraldGreen)

                    if (lecturerEntries.isNotEmpty()) {
                        HorizontalDivider(color = AppColorState.textSecondary.copy(alpha = 0.12f))
                        Text("Mevcut Ders Takvimi (${lecturerEntries.size} ders)", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        lecturerEntries.sortedWith(compareBy({ WEEK_DAYS.indexOf(it.dayOfWeek) }, { it.timeSlot })).forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(dayLocalizedShort(entry.dayOfWeek, weekDaysShort()), color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.courseName, color = AppColorState.textPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${entry.timeSlot} · ${entry.classroomName}", color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(AppColorState.textSecondary.copy(alpha = 0.06f)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DateRange, null, tint = AppColorState.textSecondary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Henüz ders atanmamış", color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewAvailTarget = null }) {
                    Text(stringResource(R.string.close), color = EmeraldGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── Add Student Dialog ────────────────────────────────────
    if (showAddStudentDialog) {
        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false; studentFullName = ""; studentUsername = ""; studentPassword = ""; studentDepartment = ""; studentYear = ""; studentId = "" },
            containerColor = AppColorState.surface,
            title = { Text(stringResource(R.string.add_student_title), color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold) },
            text = {
                val violet = Color(0xFF8B5CF6)
                val sf = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor    = violet,
                    unfocusedBorderColor  = AppColorState.surface2,
                    focusedLabelColor     = violet,
                    unfocusedLabelColor   = AppColorState.textSecondary,
                    focusedTextColor      = AppColorState.textPrimary,
                    unfocusedTextColor    = AppColorState.textPrimary,
                    cursorColor           = violet,
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = studentFullName, onValueChange = { studentFullName = it.filter { c -> c.isLetter() || c.isWhitespace() || c == '-' } }, label = { Text(stringResource(R.string.student_name_label)) }, placeholder = { Text("Örn. Ali Vural", color = AppColorState.textSecondary.copy(alpha = 0.5f)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = sf)
                    OutlinedTextField(value = studentUsername, onValueChange = { studentUsername = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase() }, label = { Text(stringResource(R.string.student_username_label)) }, placeholder = { Text("Örn. ali_vural", color = AppColorState.textSecondary.copy(alpha = 0.5f)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = sf)
                    OutlinedTextField(value = studentPassword, onValueChange = { studentPassword = it }, label = { Text(stringResource(R.string.student_password_label)) }, placeholder = { Text(stringResource(R.string.new_password_hint), color = AppColorState.textSecondary.copy(alpha = 0.5f)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = sf, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    if (studentPassword.isNotEmpty()) {
                        PasswordStrengthRow(studentPassword)
                    }
                    // Department Dropdown
                    ExposedDropdownMenuBox(expanded = deptDropdownExpanded, onExpandedChange = { deptDropdownExpanded = !deptDropdownExpanded }) {
                        OutlinedTextField(
                            value = studentDepartment.ifEmpty { stringResource(R.string.select_department) },
                            onValueChange = {}, readOnly = true,
                            label = { Text(stringResource(R.string.student_dept_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp), colors = sf
                        )
                        ExposedDropdownMenu(
                            expanded = deptDropdownExpanded,
                            onDismissRequest = { deptDropdownExpanded = false },
                            modifier = Modifier.background(AppColorState.surface).heightIn(max = 280.dp)
                        ) {
                            deptOptions.forEach { dept ->
                                DropdownMenuItem(text = { Text(dept, color = AppColorState.textPrimary) }, onClick = { studentDepartment = dept; deptDropdownExpanded = false })
                            }
                        }
                    }
                    OutlinedTextField(value = studentId, onValueChange = { studentId = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.student_id_label)) }, placeholder = { Text(stringResource(R.string.student_id_hint), color = AppColorState.textSecondary.copy(alpha = 0.5f)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = sf, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    ExposedDropdownMenuBox(expanded = yearDropdownExpanded, onExpandedChange = { yearDropdownExpanded = !yearDropdownExpanded }) {
                        OutlinedTextField(
                            value = studentYear.ifEmpty { stringResource(R.string.class_year_label) }, onValueChange = {}, readOnly = true,
                            label = { Text(stringResource(R.string.class_year_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = sf
                        )
                        ExposedDropdownMenu(expanded = yearDropdownExpanded, onDismissRequest = { yearDropdownExpanded = false }, modifier = Modifier.background(AppColorState.surface)) {
                            yearOptions.forEach { year ->
                                DropdownMenuItem(text = { Text(year, color = AppColorState.textPrimary) }, onClick = { studentYear = year; yearDropdownExpanded = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (studentFullName.trim().length < 3) {
                            Toast.makeText(context, "Ad soyad en az 3 karakter olmalıdır", Toast.LENGTH_SHORT).show()
                        } else if (studentUsername.trim().length < 3) {
                            Toast.makeText(context, "Kullanıcı adı en az 3 karakter olmalıdır", Toast.LENGTH_SHORT).show()
                        } else if (studentPassword.length < 6) {
                            Toast.makeText(context, "Şifre en az 6 karakter olmalıdır", Toast.LENGTH_SHORT).show()
                        } else if (studentDepartment.isBlank()) {
                            Toast.makeText(context, "Bölüm seçiniz", Toast.LENGTH_SHORT).show()
                        } else if (studentYear.isBlank()) {
                            Toast.makeText(context, "Sınıf yılı seçiniz", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addStudent(studentFullName.trim(), studentUsername.trim(), studentPassword, studentDepartment, studentYear, studentId)
                            showAddStudentDialog = false
                            studentFullName = ""; studentUsername = ""; studentPassword = ""; studentDepartment = ""; studentYear = ""; studentId = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false; studentFullName = ""; studentUsername = ""; studentPassword = ""; studentDepartment = ""; studentYear = ""; studentId = "" }) {
                    Text(stringResource(R.string.cancel), color = AppColorState.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun QuickSetupCard(isEmpty: Boolean, onSeed: () -> Unit) {
    if (isEmpty) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.30f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.db_empty_title), color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.db_empty_desc), color = AppColorState.textSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onSeed, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.load_demo_data), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    } else {
        OutlinedButton(
            onClick = onSeed,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColorState.textSecondary),
            border = BorderStroke(1.dp, AppColorState.surface2)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.reload_demo_data), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppColorState.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                color = accentColor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                title,
                color = AppColorState.textSecondary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun PanelCard(
    title: String,
    count: Int,
    accentColor: Color,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = AppColorState.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(Modifier.width(10.dp))
                Text(title, color = AppColorState.textPrimary, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(containerColor = accentColor.copy(alpha = 0.2f)) {
                    Text(count.toString(), color = accentColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppColorState.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LecturerInfoRow(
    lecturer: Lecturer,
    onDelete: () -> Unit = {},
    onResetPassword: ((String, (Boolean) -> Unit) -> Unit)? = null,
    availability: LecturerAvailability? = null,
    unassignedCourses: List<Course> = emptyList(),
    classrooms: List<Classroom> = emptyList(),
    onAssignCourse: ((Course, Classroom, String, String, String) -> Unit)? = null,
    assignmentResult: kotlinx.coroutines.flow.SharedFlow<AssignmentResult>? = null
) {
    var showDeleteDialog   by remember { mutableStateOf(false) }
    var showDetailDialog   by remember { mutableStateOf(false) }  // now = availability+assign dialog
    var showResetPwdDialog by remember { mutableStateOf(false) }
    var newPwd             by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Assignment form state
    var selCourse    by remember { mutableStateOf<Course?>(null) }
    var selClassroom by remember { mutableStateOf<Classroom?>(null) }
    var selDay       by remember { mutableStateOf("") }
    var selTime      by remember { mutableStateOf("") }
    var selSession   by remember { mutableStateOf(SessionType.LECTURE) }
    var assignError  by remember { mutableStateOf<String?>(null) }

    // Collect assignment results for this dialog
    LaunchedEffect(showDetailDialog) {
        if (!showDetailDialog || assignmentResult == null) return@LaunchedEffect
        assignmentResult.collect { result ->
            when (result) {
                is AssignmentResult.Success -> { showDetailDialog = false }
                is AssignmentResult.LecturerClash   -> assignError = "Çakışma: ${result.existing.courseName} aynı saatte!"
                is AssignmentResult.ClassroomClash  -> assignError = "Sınıf meşgul: ${result.existing.courseName}"
                is AssignmentResult.CapacityWarning -> assignError = result.message
                is AssignmentResult.Error           -> assignError = result.message
            }
        }
    }

    // Müsaitlik bazlı gün/saat filtreleme
    val availDays = remember(availability) {
        if (availability == null) WEEK_DAYS
        else WEEK_DAYS.filter { day -> availability.slotsForDay(day).isNotEmpty() }
    }
    val availTimesForDay = remember(selDay, availability) {
        if (availability == null || selDay.isEmpty()) TIME_SLOTS
        else availability.slotsForDay(selDay).filter { it in TIME_SLOTS }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clickable { showDetailDialog = true },
        colors = CardDefaults.cardColors(containerColor = AppColorState.surface2),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    lecturer.fullName.firstOrNull()?.uppercase() ?: "?",
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(lecturer.fullName, color = AppColorState.textPrimary, fontWeight = FontWeight.Medium)
                Text(
                    buildString {
                        if (lecturer.title.isNotBlank()) append("${lecturer.title} · ")
                        append(lecturer.department)
                    },
                    color = AppColorState.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                if (lecturer.mustChangePassword) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Şifre değiştirilmedi", color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (onResetPassword != null) {
                IconButton(onClick = { showResetPwdDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = "Şifre sıfırla", tint = IndigoAccent.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = ErrorRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = AppColorState.surface,
            title = { Text(stringResource(R.string.delete_lecturer_title), color = ErrorRed) },
            text = { Text(stringResource(R.string.delete_lecturer_msg, lecturer.fullName), color = AppColorState.textPrimary) },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel), color = AppColorState.textSecondary) }
            }
        )
    }

    // ── Müsaitlik + Ders Atama Dialog ──────────────────────────
    if (showDetailDialog) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = false; assignError = null; selCourse = null; selClassroom = null; selDay = ""; selTime = "" },
            containerColor = AppColorState.surface,
            modifier = Modifier.fillMaxWidth(),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(EmeraldGreen.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text(lecturer.fullName.firstOrNull()?.uppercase() ?: "?", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(lecturer.fullName, color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(lecturer.department, color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Müsaitlik haritası
                    if (availability != null) {
                        Text(stringResource(R.string.lecturer_availability_map), color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        // Müsait = yeşil, müsait değil = kırmızı
                        AdminAvailabilityGrid(avail = availability, accentColor = EmeraldGreen, showUnavailableRed = true)
                    } else {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF59E0B).copy(alpha = 0.08f)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Onaylı müsaitlik haritası yok — tüm saatler gösterilir", color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (onAssignCourse != null) {
                        HorizontalDivider(color = AppColorState.textSecondary.copy(alpha = 0.12f))
                        Text("Ders Ata", color = IndigoAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)

                        // Hata mesajı
                        assignError?.let { err ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ErrorRed.copy(alpha = 0.1f)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(err, color = ErrorRed, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                IconButton(onClick = { assignError = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, null, tint = ErrorRed, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoAccent, unfocusedBorderColor = AppColorState.surface2,
                            focusedLabelColor = IndigoAccent, unfocusedLabelColor = AppColorState.textSecondary,
                            focusedTextColor = AppColorState.textPrimary, unfocusedTextColor = AppColorState.textPrimary,
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                        )

                        // Ders seç
                        var courseExp by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = courseExp, onExpandedChange = { courseExp = !courseExp }) {
                            OutlinedTextField(
                                value = selCourse?.let { "${it.courseCode} – ${it.courseName}" } ?: stringResource(R.string.select_course),
                                onValueChange = {}, readOnly = true,
                                label = { Text("Ders") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExp) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp), colors = fieldColors
                            )
                            ExposedDropdownMenu(expanded = courseExp, onDismissRequest = { courseExp = false }, modifier = Modifier.background(AppColorState.surface).heightIn(max = 200.dp)) {
                                unassignedCourses.forEach { c ->
                                    DropdownMenuItem(text = { Text("${c.courseCode} – ${c.courseName}", color = AppColorState.textPrimary, style = MaterialTheme.typography.bodySmall) }, onClick = { selCourse = c; courseExp = false })
                                }
                            }
                        }

                        // Gün seç (müsait günler)
                        var dayExp by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = dayExp, onExpandedChange = { dayExp = !dayExp }) {
                            OutlinedTextField(
                                value = if (selDay.isEmpty()) stringResource(R.string.select_day) else dayLocalizedName(selDay, weekDaysFull()),
                                onValueChange = {}, readOnly = true,
                                label = { Text(stringResource(R.string.label_day)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExp) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp), colors = fieldColors
                            )
                            ExposedDropdownMenu(expanded = dayExp, onDismissRequest = { dayExp = false }, modifier = Modifier.background(AppColorState.surface)) {
                                availDays.forEach { day ->
                                    DropdownMenuItem(text = { Text(dayLocalizedName(day, weekDaysFull()), color = AppColorState.textPrimary) }, onClick = { selDay = day; selTime = ""; dayExp = false })
                                }
                            }
                        }

                        // Saat seç (müsait saatler)
                        var timeExp by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = timeExp, onExpandedChange = { if (selDay.isNotEmpty()) timeExp = !timeExp }) {
                            OutlinedTextField(
                                value = selTime.ifEmpty { "Önce gün seçin" },
                                onValueChange = {}, readOnly = true,
                                label = { Text("Saat${if (availability != null) " (müsait saatler)" else ""}") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExp) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp), colors = fieldColors,
                                enabled = selDay.isNotEmpty()
                            )
                            ExposedDropdownMenu(expanded = timeExp, onDismissRequest = { timeExp = false }, modifier = Modifier.background(AppColorState.surface)) {
                                availTimesForDay.forEach { t ->
                                    DropdownMenuItem(text = { Text(t, color = AppColorState.textPrimary) }, onClick = { selTime = t; timeExp = false })
                                }
                            }
                        }

                        // Sınıf seç
                        var clsExp by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = clsExp, onExpandedChange = { clsExp = !clsExp }) {
                            OutlinedTextField(
                                value = selClassroom?.let { "${it.name} (${it.capacity})" } ?: stringResource(R.string.select_classroom),
                                onValueChange = {}, readOnly = true,
                                label = { Text("Sınıf") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clsExp) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp), colors = fieldColors
                            )
                            ExposedDropdownMenu(expanded = clsExp, onDismissRequest = { clsExp = false }, modifier = Modifier.background(AppColorState.surface).heightIn(max = 200.dp)) {
                                classrooms.forEach { cls ->
                                    DropdownMenuItem(text = { Text("${cls.name} · ${cls.capacity} kişi · ${ClassroomType.displayName(cls.classroomType)}", color = AppColorState.textPrimary, style = MaterialTheme.typography.bodySmall) }, onClick = { selClassroom = cls; clsExp = false })
                                }
                            }
                        }

                        // Seans tipi
                        var sessExp by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = sessExp, onExpandedChange = { sessExp = !sessExp }) {
                            OutlinedTextField(
                                value = SessionType.displayName(selSession),
                                onValueChange = {}, readOnly = true,
                                label = { Text("Seans Tipi") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sessExp) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp), colors = fieldColors
                            )
                            ExposedDropdownMenu(expanded = sessExp, onDismissRequest = { sessExp = false }, modifier = Modifier.background(AppColorState.surface)) {
                                DropdownMenuItem(text = { Text("Teorik", color = AppColorState.textPrimary) }, onClick = { selSession = SessionType.LECTURE; sessExp = false })
                                DropdownMenuItem(text = { Text("Lab", color = AppColorState.textPrimary) }, onClick = { selSession = SessionType.LAB; sessExp = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onAssignCourse != null) {
                        val canAssign = selCourse != null && selClassroom != null && selDay.isNotEmpty() && selTime.isNotEmpty()
                        Button(
                            onClick = {
                                if (canAssign) {
                                    assignError = null
                                    onAssignCourse(selCourse!!, selClassroom!!, selDay, selTime, selSession)
                                }
                            },
                            enabled = canAssign,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent, disabledContainerColor = AppColorState.surface2),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ata", fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { showDetailDialog = false; assignError = null }) {
                        Text(stringResource(R.string.close), color = AppColorState.textSecondary)
                    }
                }
            },
            dismissButton = null
        )
    }

    if (showResetPwdDialog && onResetPassword != null) {
        AlertDialog(
            onDismissRequest = { showResetPwdDialog = false; newPwd = "" },
            containerColor = AppColorState.surface,
            title = { Text("Şifre Sıfırla", color = IndigoAccent, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${lecturer.fullName} (@${lecturer.username}) için yeni şifre belirleyin.", color = AppColorState.textPrimary, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = newPwd,
                        onValueChange = { newPwd = it },
                        label = { Text("Yeni Şifre") },
                        placeholder = { Text("En az 6 karakter", color = AppColorState.textSecondary.copy(alpha = 0.5f)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoAccent, unfocusedBorderColor = AppColorState.surface2,
                            focusedLabelColor = IndigoAccent, unfocusedLabelColor = AppColorState.textSecondary,
                            focusedTextColor = AppColorState.textPrimary, unfocusedTextColor = AppColorState.textPrimary,
                            cursorColor = IndigoAccent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                        )
                    )
                    if (newPwd.isNotEmpty()) PasswordStrengthRow(newPwd)
                    Text("Hoca bir sonraki girişte şifresini değiştirmek zorunda kalacak.", color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPwd.length < 6) {
                            Toast.makeText(context, "Şifre en az 6 karakter olmalıdır", Toast.LENGTH_SHORT).show()
                        } else {
                            onResetPassword(newPwd) { success ->
                                if (success) {
                                    Toast.makeText(context, "${lecturer.fullName} şifresi sıfırlandı.", Toast.LENGTH_SHORT).show()
                                    showResetPwdDialog = false; newPwd = ""
                                } else {
                                    Toast.makeText(context, "Şifre sıfırlama başarısız.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
                ) { Text("Sıfırla") }
            },
            dismissButton = {
                TextButton(onClick = { showResetPwdDialog = false; newPwd = "" }) { Text(stringResource(R.string.cancel), color = AppColorState.textSecondary) }
            }
        )
    }
}

@Composable
private fun CourseInfoRow(course: Course, onDelete: () -> Unit = {}) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        colors = CardDefaults.cardColors(containerColor = AppColorState.surface2),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IndigoAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    course.courseCode.take(3),
                    color = IndigoAccent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(course.courseName, color = AppColorState.textPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(course.courseCode, color = IndigoAccent, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = ErrorRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = AppColorState.surface,
            title = { Text(stringResource(R.string.delete_course_title), color = ErrorRed) },
            text = { Text(stringResource(R.string.delete_course_msg, course.courseName), color = AppColorState.textPrimary) },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel), color = AppColorState.textSecondary) }
            }
        )
    }
}

@Composable
private fun StudentInfoRow(student: Lecturer, onDelete: () -> Unit = {}) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        colors = CardDefaults.cardColors(containerColor = AppColorState.surface2),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    student.fullName.firstOrNull()?.uppercase() ?: "?",
                    color = Color(0xFF8B5CF6),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(student.fullName, color = AppColorState.textPrimary, fontWeight = FontWeight.Medium)
                Text(
                    buildString {
                        if (student.studentYear.isNotBlank()) append("${student.studentYear} · ")
                        if (student.studentId.isNotBlank()) append(student.studentId)
                    },
                    color = AppColorState.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                if (student.mustChangePassword) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Şifre değiştirilmedi", color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = ErrorRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = AppColorState.surface,
            title = { Text(stringResource(R.string.delete_student_title), color = ErrorRed) },
            text = { Text(stringResource(R.string.delete_student_msg, student.fullName), color = AppColorState.textPrimary) },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel), color = AppColorState.textSecondary) }
            }
        )
    }
}

@Composable
private fun PendingRequestCard(
    request: com.example.academicmanager.data.ScheduleRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(request.lecturerName, color = AppColorState.textPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = AppColorState.textSecondary, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("${request.proposedDay} · ${request.proposedTimeSlot}", color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
            if (request.proposedClassroom.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MeetingRoom, null, tint = AppColorState.textSecondary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tercih: ${request.proposedClassroom}", color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (request.courseName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.List, null, tint = EmeraldGreen, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tercih: ${request.courseName}", color = EmeraldGreen, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (request.lecturerNote.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("\"${request.lecturerNote}\"", color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.schedule_btn), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.reject), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PendingRegistrationCard(
    lecturer: com.example.academicmanager.data.Lecturer,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val roleColor = when (lecturer.role) {
        com.example.academicmanager.data.UserRole.ADMIN    -> ErrorRed
        com.example.academicmanager.data.UserRole.STUDENT  -> Color(0xFF8B5CF6)
        else                                               -> Color(0xFF6366F1)
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1).copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(roleColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(lecturer.fullName.firstOrNull()?.uppercase() ?: "?", color = roleColor, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(lecturer.fullName, color = AppColorState.textPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text("@${lecturer.username} · ${lecturer.department}", color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(roleColor.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(lecturer.role.name, color = roleColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApprove, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.approve), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed), border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.reject), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScheduleEntryInfoRow(entry: ScheduleEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        colors = CardDefaults.cardColors(containerColor = AppColorState.surface2),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.courseName, color = AppColorState.textPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${entry.lecturerName} · ${entry.classroomName}", color = AppColorState.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(dayLocalizedShort(entry.dayOfWeek, weekDaysShort()), color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(entry.timeSlot, color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun AllAssignedBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(EmeraldGreen.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, color = EmeraldGreen, style = MaterialTheme.typography.bodySmall)
    }
}

// ─────────────────────────────────────────────────────────────
// PENDING AVAILABILITY CARD (admin home list item)
// ─────────────────────────────────────────────────────────────

@Composable
private fun PendingAvailabilityCard(
    avail: LecturerAvailability,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onView: () -> Unit = {}
) {
    val accentColor = Color(0xFF10B981)
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(avail.lecturerName.firstOrNull()?.uppercase() ?: "?", color = accentColor, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(avail.lecturerName, color = AppColorState.textPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.admin_avail_slots_count, avail.totalSlots),
                        color = AppColorState.textSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                // Görüntüle ikonu
                IconButton(onClick = onView, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Görüntüle", tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }
            // Day summary chips
            val days = listOf("Monday" to avail.monday, "Tuesday" to avail.tuesday, "Wednesday" to avail.wednesday, "Thursday" to avail.thursday, "Friday" to avail.friday)
            val dayNamesTR = listOf("Pzt", "Sal", "Çar", "Per", "Cum")
            val filled = days.mapIndexedNotNull { i, (_, slots) -> if (slots.isNotEmpty()) dayNamesTR[i] to slots.size else null }
            if (filled.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    filled.forEach { (dayShort, count) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("$dayShort·$count", color = accentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Üç buton: Görüntüle | Onayla | Reddet
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onView,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Görüntüle", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(stringResource(R.string.approve), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(stringResource(R.string.reject), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ADMIN AVAILABILITY SCREEN
// ─────────────────────────────────────────────────────────────

private val AVAIL_DAY_COLORS = listOf(
    Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6)
)
private val AVAIL_DAYS_TR_SHORT = listOf("Pzt", "Sal", "Çar", "Per", "Cum")
private val AVAIL_DAYS_EN       = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
private val AVAIL_SLOTS         = listOf(
    "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00",
    "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAvailabilityScreen(viewModel: AdminViewModel, navController: NavController) {
    val allAvailabilities by viewModel.availabilities.collectAsState()
    val lecturers         by viewModel.lecturers.collectAsState()

    var selectedUsername   by remember { mutableStateOf<String?>(null) }
    var lecturerDropdown   by remember { mutableStateOf(false) }

    val displayMap = when {
        selectedUsername == null -> {
            // Combined: find latest approved/pending per lecturer
            allAvailabilities
                .groupBy { it.lecturerUsername }
                .mapValues { (_, list) ->
                    list.filter { it.status == AvailabilityStatus.APPROVED }
                        .maxByOrNull { it.timestamp }
                        ?: list.filter { it.status == AvailabilityStatus.PENDING }
                            .maxByOrNull { it.timestamp }
                }
                .values.filterNotNull()
        }
        else -> {
            allAvailabilities
                .filter { it.lecturerUsername == selectedUsername }
                .sortedByDescending { it.timestamp }
        }
    }

    val selectedLecturerName = lecturers.find { it.username == selectedUsername }?.fullName
        ?: stringResource(R.string.admin_avail_all_lecturers)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.admin_avail_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.admin_avail_subtitle), color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = EmeraldGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = AppColorState.textPrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Hoca Filtresi ──────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = lecturerDropdown,
                onExpandedChange = { lecturerDropdown = !lecturerDropdown }
            ) {
                OutlinedTextField(
                    value        = selectedLecturerName,
                    onValueChange = {},
                    readOnly     = true,
                    label        = { Text(stringResource(R.string.admin_avail_select_lecturer), color = AppColorState.textSecondary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lecturerDropdown) },
                    modifier     = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                    shape        = RoundedCornerShape(12.dp),
                    colors       = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor    = EmeraldGreen,
                        unfocusedBorderColor  = AppColorState.surface2,
                        focusedLabelColor     = EmeraldGreen,
                        unfocusedLabelColor   = AppColorState.textSecondary,
                        focusedTextColor      = AppColorState.textPrimary,
                        unfocusedTextColor    = AppColorState.textPrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded         = lecturerDropdown,
                    onDismissRequest = { lecturerDropdown = false },
                    modifier         = Modifier.background(AppColorState.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.admin_avail_all_lecturers), color = EmeraldGreen, fontWeight = FontWeight.SemiBold) },
                        onClick = { selectedUsername = null; lecturerDropdown = false }
                    )
                    lecturers.forEach { lect ->
                        DropdownMenuItem(
                            text    = { Text(lect.fullName, color = AppColorState.textPrimary) },
                            onClick = { selectedUsername = lect.username; lecturerDropdown = false }
                        )
                    }
                }
            }

            // ── Harita / Grid ──────────────────────────────────
            if (displayMap.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = AppColorState.surface),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = AppColorState.textSecondary, modifier = Modifier.size(36.dp))
                            Text(stringResource(R.string.admin_avail_no_data), color = AppColorState.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else if (selectedUsername != null) {
                // Belirli hoca seçili — tüm gönderimlerini listele
                displayMap.forEach { avail ->
                    AdminAvailabilityDetailCard(avail = avail)
                }
            } else {
                // Tüm hocalar — onaylanan/bekleyen en son gönderimi göster
                AdminAllLecturersGrid(availabilities = displayMap)
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AdminAvailabilityDetailCard(avail: LecturerAvailability) {
    val (statusColor, statusLabel) = when (avail.status) {
        AvailabilityStatus.APPROVED -> EmeraldGreen to "Onaylandı"
        AvailabilityStatus.REJECTED -> ErrorRed     to "Reddedildi"
        else                        -> Color(0xFFF59E0B) to "Onay Bekliyor"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = AppColorState.surface),
        shape    = RoundedCornerShape(16.dp),
        border   = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(avail.lecturerName, color = AppColorState.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(avail.timestamp)),
                        color = AppColorState.textSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            // Read-only grid
            AdminAvailabilityGrid(avail = avail, accentColor = statusColor)
            if (avail.adminNote.isNotBlank()) {
                Text("Not: \"${avail.adminNote}\"", color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun AdminAllLecturersGrid(availabilities: List<LecturerAvailability>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = AppColorState.surface),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.admin_avail_all_maps),
                color      = AppColorState.textPrimary,
                fontWeight = FontWeight.Bold,
                style      = MaterialTheme.typography.titleSmall
            )
            Text(
                "${availabilities.size} hoca müsaitlik haritası",
                color = AppColorState.textSecondary,
                style = MaterialTheme.typography.labelSmall
            )
            HorizontalDivider(color = AppColorState.textSecondary.copy(alpha = 0.1f))

            // Column headers
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.width(60.dp))
                AVAIL_DAYS_TR_SHORT.forEachIndexed { i, day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day, color = AVAIL_DAY_COLORS[i], style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))

            // Grid rows
            AVAIL_SLOTS.forEach { slot ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(slot.take(5), color = AppColorState.textSecondary.copy(alpha = 0.55f), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(60.dp))
                    AVAIL_DAYS_EN.forEachIndexed { dayIdx, day ->
                        val lecturersAvailable = availabilities.filter { slot in it.slotsForDay(day) }
                        val count = lecturersAvailable.size
                        val maxCount = availabilities.size.coerceAtLeast(1)
                        val alpha = if (count == 0) 0.05f else (count.toFloat() / maxCount) * 0.75f + 0.15f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .padding(horizontal = 2.dp, vertical = 1.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (count > 0) AVAIL_DAY_COLORS[dayIdx].copy(alpha = alpha)
                                    else AppColorState.surface2
                                )
                                .border(1.dp, if (count > 0) AVAIL_DAY_COLORS[dayIdx].copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(5.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (count > 0) {
                                Text(
                                    count.toString(),
                                    color = if (count > 1) Color.White else AVAIL_DAY_COLORS[dayIdx],
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Legend
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = AppColorState.textSecondary.copy(alpha = 0.1f))
            Spacer(Modifier.height(2.dp))
            Text("Sayı = o saatte müsait hoca adedi", color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AdminAvailabilityGrid(
    avail: LecturerAvailability,
    accentColor: Color,
    showUnavailableRed: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        // Gün başlıkları
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(54.dp))
            AVAIL_DAYS_TR_SHORT.forEachIndexed { i, day ->
                val dayHasSlots = avail.slotsForDay(AVAIL_DAYS_EN[i]).isNotEmpty()
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        day,
                        color = if (showUnavailableRed && !dayHasSlots) ErrorRed else AVAIL_DAY_COLORS[i],
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        AVAIL_SLOTS.forEachIndexed { _, slot ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(slot.take(5), color = AppColorState.textSecondary.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, modifier = Modifier.width(54.dp))
                AVAIL_DAYS_EN.forEachIndexed { dayIdx, day ->
                    val selected = slot in avail.slotsForDay(day)
                    val unavailColor = if (showUnavailableRed) ErrorRed.copy(alpha = 0.18f) else AppColorState.surface2
                    val unavailBorder = if (showUnavailableRed) ErrorRed.copy(alpha = 0.25f) else Color.Transparent
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (selected) accentColor.copy(alpha = 0.75f) else unavailColor)
                            .border(1.dp, if (selected) accentColor.copy(alpha = 0.5f) else unavailBorder, RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.8f)))
                        } else if (showUnavailableRed) {
                            // Müsait olmayan hücrelerde ince kırmızı X işareti
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = ErrorRed.copy(alpha = 0.45f),
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CLASSROOMS SCREEN
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomsScreen(viewModel: AdminViewModel) {
    val classrooms    by viewModel.classrooms.collectAsState()
    val scheduleEntries by viewModel.scheduleEntries.collectAsState()

    var showAddDialog    by remember { mutableStateOf(false) }
    var name             by remember { mutableStateOf("") }
    var capacityText     by remember { mutableStateOf("") }
    var selectedType     by remember { mutableStateOf(ClassroomType.LECTURE) }
    var typeExpanded     by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldGreen,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_classroom_title))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            stringResource(R.string.classrooms_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = AppColorState.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (classrooms.size == 1)
                                stringResource(R.string.classrooms_subtitle_one, classrooms.size)
                            else
                                stringResource(R.string.classrooms_subtitle, classrooms.size),
                            color = AppColorState.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (classrooms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                tint = AppColorState.textSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.no_classrooms),
                                color = AppColorState.textSecondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                stringResource(R.string.tap_plus_add),
                                color = AppColorState.textSecondary.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                items(classrooms) { classroom ->
                    val bookingCount = scheduleEntries.count { it.classroomName == classroom.name }
                    ClassroomCard(
                        classroom = classroom,
                        bookingCount = bookingCount,
                        onDelete = { viewModel.deleteClassroom(classroom.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                name = ""; capacityText = ""
            },
            containerColor = AppColorState.surface,
            title = {
                Text(stringResource(R.string.add_classroom_title), color = EmeraldGreen, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.classroom_name_label)) },
                        placeholder = { Text(stringResource(R.string.classroom_name_hint), color = AppColorState.textSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, unfocusedBorderColor = AppColorState.surface2, focusedLabelColor = EmeraldGreen, unfocusedLabelColor = AppColorState.textSecondary, focusedTextColor = AppColorState.textPrimary, unfocusedTextColor = AppColorState.textPrimary, cursorColor = EmeraldGreen, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = { capacityText = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.capacity_label)) },
                        placeholder = { Text(stringResource(R.string.capacity_hint), color = AppColorState.textSecondary.copy(alpha = 0.5f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, unfocusedBorderColor = AppColorState.surface2, focusedLabelColor = EmeraldGreen, unfocusedLabelColor = AppColorState.textSecondary, focusedTextColor = AppColorState.textPrimary, unfocusedTextColor = AppColorState.textPrimary, cursorColor = EmeraldGreen, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = !typeExpanded }
                    ) {
                        OutlinedTextField(
                            value = ClassroomType.displayName(selectedType),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.classroom_type_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                focusedLabelColor  = EmeraldGreen
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false },
                            modifier = Modifier.background(AppColorState.surface)
                        ) {
                            ClassroomType.all.forEach { t ->
                                DropdownMenuItem(
                                    text    = { Text(ClassroomType.displayName(t), color = AppColorState.textPrimary) },
                                    onClick = { selectedType = t; typeExpanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedCapacity = capacityText.toIntOrNull() ?: 0
                        if (name.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.room_name_empty), Toast.LENGTH_SHORT).show()
                        } else if (parsedCapacity < 1 || parsedCapacity > 2000) {
                            Toast.makeText(context, "Kapasite 1 ile 2000 arasında olmalıdır", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addClassroom(
                                name          = name.trim(),
                                capacity      = parsedCapacity,
                                classroomType = selectedType
                            )
                            showAddDialog = false
                            name = ""; capacityText = ""; selectedType = ClassroomType.LECTURE
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; name = ""; capacityText = ""; selectedType = ClassroomType.LECTURE }) {
                    Text(stringResource(R.string.cancel), color = AppColorState.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun ClassroomCard(classroom: Classroom, bookingCount: Int, onDelete: () -> Unit = {}) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val totalSlots = 5 * 8 // 5 days x 8 time slots
    val occupancyPct = if (totalSlots > 0) (bookingCount.toFloat() / totalSlots).coerceIn(0f, 1f) else 0f
    val occupancyColor = when {
        occupancyPct < 0.4f -> EmeraldGreen
        occupancyPct < 0.75f -> Color(0xFFF59E0B)
        else -> ErrorRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColorState.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(classroom.name, color = AppColorState.textPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            ClassroomType.displayName(classroom.classroomType),
                            color = Color(0xFFF59E0B),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    stringResource(R.string.capacity_display, classroom.capacity),
                    color = AppColorState.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { occupancyPct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = occupancyColor,
                    trackColor = AppColorState.surface2
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$bookingCount",
                    color = occupancyColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(R.string.bookings_label),
                    color = AppColorState.textSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(4.dp))
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = ErrorRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = AppColorState.surface,
            title = { Text(stringResource(R.string.delete_course_title), color = ErrorRed) },
            text = { Text(stringResource(R.string.delete_course_msg, classroom.name), color = AppColorState.textPrimary) },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel), color = AppColorState.textSecondary) }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// ASSIGNMENT SCREEN
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentScreen(viewModel: AdminViewModel, navController: NavController) {
    val courses         by viewModel.courses.collectAsState()
    val lecturers       by viewModel.lecturers.collectAsState()
    val classrooms      by viewModel.classrooms.collectAsState()
    val scheduleEntries by viewModel.scheduleEntries.collectAsState()
    val context = LocalContext.current

    var selectedCourse    by remember { mutableStateOf<Course?>(null) }
    var selectedLecturer  by remember { mutableStateOf<Lecturer?>(null) }
    var selectedClassroom by remember { mutableStateOf<Classroom?>(null) }
    var selectedDay       by remember { mutableStateOf("") }
    var selectedTime      by remember { mutableStateOf("") }
    var selectedSession   by remember { mutableStateOf(SessionType.LECTURE) }
    var warningMessage    by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.assignmentResult.collect { result ->
            when (result) {
                is AssignmentResult.Success -> {
                    Toast.makeText(context, context.getString(R.string.assignment_saved), Toast.LENGTH_SHORT).show()
                    selectedCourse = null; selectedLecturer = null
                    selectedClassroom = null; selectedDay = ""; selectedTime = ""
                    selectedSession = SessionType.LECTURE; warningMessage = null
                }
                is AssignmentResult.LecturerClash -> {
                    val e = result.existing
                    Toast.makeText(
                        context,
                        "Çakışma! ${e.lecturerName} aynı saatte ${e.courseName} dersini veriyor\n(${e.dayOfWeek} ${e.timeSlot})",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is AssignmentResult.ClassroomClash -> {
                    val e = result.existing
                    Toast.makeText(
                        context,
                        "Çakışma! ${e.classroomName} sınıfı aynı saatte ${e.courseName} dersine atanmış\n(${e.dayOfWeek} ${e.timeSlot})",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is AssignmentResult.CapacityWarning -> {
                    warningMessage = result.message
                }
                is AssignmentResult.Error -> {
                    Toast.makeText(context, "Hata: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.course_assignment_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColorState.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.course_assignment_subtitle),
                    color = AppColorState.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = { navController.navigate("auto_assign") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Otomatik Ata", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Selector Cards ───────────────────────────────────
        SectionLabel(stringResource(R.string.label_course))
        DropdownSelector(
            placeholder = stringResource(R.string.select_course),
            selected = selectedCourse?.let { "${it.courseCode} – ${it.courseName}" } ?: "",
            options = courses.map { "${it.courseCode} – ${it.courseName}" },
            onSelect = { idx -> selectedCourse = courses.getOrNull(idx) }
        )

        SectionLabel(stringResource(R.string.label_lecturer))
        DropdownSelector(
            placeholder = stringResource(R.string.select_lecturer),
            selected = selectedLecturer?.let { "${it.title} ${it.fullName}".trim() } ?: "",
            options = lecturers.map { "${it.title} ${it.fullName}".trim() },
            onSelect = { idx -> selectedLecturer = lecturers.getOrNull(idx) }
        )

        SectionLabel(stringResource(R.string.label_classroom))
        DropdownSelector(
            placeholder = stringResource(R.string.select_classroom),
            selected = selectedClassroom?.let { "${it.name}  (cap: ${it.capacity})" } ?: "",
            options = classrooms.map { "${it.name}  (cap: ${it.capacity})" },
            onSelect = { idx -> selectedClassroom = classrooms.getOrNull(idx) }
        )

        SectionLabel(stringResource(R.string.label_day))
        DropdownSelector(
            placeholder = stringResource(R.string.select_day),
            selected = dayLocalizedName(selectedDay, weekDaysFull()),
            options = weekDaysFull(),
            onSelect = { idx -> selectedDay = WEEK_DAYS[idx] }
        )

        SectionLabel(stringResource(R.string.label_time_slot))
        DropdownSelector(
            placeholder = stringResource(R.string.select_time),
            selected = selectedTime,
            options = TIME_SLOTS,
            onSelect = { idx -> selectedTime = TIME_SLOTS[idx] }
        )

        SectionLabel(stringResource(R.string.label_session_type))
        DropdownSelector(
            placeholder = stringResource(R.string.select_session),
            selected = SessionType.displayName(selectedSession),
            options = listOf(SessionType.displayName(SessionType.LECTURE), SessionType.displayName(SessionType.LAB)),
            onSelect = { idx -> selectedSession = if (idx == 0) SessionType.LECTURE else SessionType.LAB }
        )

        AnimatedVisibility(visible = warningMessage != null) {
            warningMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(msg, color = Color(0xFFF59E0B), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { warningMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        val allFilled = selectedCourse != null && selectedLecturer != null &&
            selectedClassroom != null && selectedDay.isNotEmpty() && selectedTime.isNotEmpty()

        Button(
            onClick = {
                if (allFilled) {
                    viewModel.assignCourse(
                        selectedCourse!!, selectedLecturer!!,
                        selectedClassroom!!, selectedDay, selectedTime, selectedSession
                    )
                }
            },
            enabled = allFilled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldGreen,
                disabledContainerColor = AppColorState.surface2
            )
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.assign_btn), fontWeight = FontWeight.Bold)
        }

        // ── Current Schedule ─────────────────────────────────
        if (scheduleEntries.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = AppColorState.textSecondary.copy(alpha = 0.1f)
            )
            Text(
                stringResource(R.string.current_schedule, scheduleEntries.size),
                style = MaterialTheme.typography.titleSmall,
                color = AppColorState.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))

            scheduleEntries
                .sortedWith(compareBy({ WEEK_DAYS.indexOf(it.dayOfWeek) }, { it.timeSlot }))
                .forEach { entry ->
                    ScheduleEntryCard(entry, onDelete = { viewModel.deleteScheduleEntry(entry.id) })
                }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = AppColorState.textSecondary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    placeholder: String,
    selected: String,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.ifEmpty { placeholder },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (selected.isEmpty()) AppColorState.surface2 else EmeraldGreen.copy(alpha = 0.5f),
                focusedBorderColor = EmeraldGreen,
                unfocusedTextColor = if (selected.isEmpty()) AppColorState.textSecondary else AppColorState.textPrimary,
                focusedTextColor = AppColorState.textPrimary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppColorState.surface)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = AppColorState.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// AUTO-ASSIGN SCREEN
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoAssignScreen(viewModel: AdminViewModel) {
    val state       by viewModel.autoAssignState.collectAsState()
    val unassignedLecturers by viewModel.unassignedLecturers.collectAsState()
    val unassignedCourses   by viewModel.unassignedCourses.collectAsState()
    val availabilities      by viewModel.availabilities.collectAsState()
    val classrooms          by viewModel.classrooms.collectAsState()
    val context = LocalContext.current

    val accentColor = Color(0xFF8B5CF6)

    LaunchedEffect(state) {
        if (state is AdminViewModel.AutoAssignState.Done) {
            val s = state as AdminViewModel.AutoAssignState.Done
            Toast.makeText(
                context,
                "${s.saved} ders atandı${if (s.failed > 0) ", ${s.failed} hata" else ""}",
                Toast.LENGTH_LONG
            ).show()
            kotlinx.coroutines.delay(1500)
            viewModel.resetAutoAssign()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Otomatik Ders Atama", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Müsaitlik haritasına göre dersler otomatik atanır", color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = AppColorState.textPrimary)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val s = state) {

                // ── IDLE ─────────────────────────────────────────
                is AdminViewModel.AutoAssignState.Idle -> {
                    item { AutoAssignIdleSection(viewModel, accentColor, unassignedCourses, availabilities, classrooms) }
                }

                // ── COMPUTING ─────────────────────────────────────
                is AdminViewModel.AutoAssignState.Computing -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                CircularProgressIndicator(color = accentColor, strokeWidth = 3.dp, modifier = Modifier.size(56.dp))
                                Text("Hesaplanıyor...", color = AppColorState.textSecondary, fontWeight = FontWeight.SemiBold)
                                Text("Müsaitlikler ve sınıflar kontrol ediliyor", color = AppColorState.textSecondary.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // ── READY ─────────────────────────────────────────
                is AdminViewModel.AutoAssignState.Ready -> {
                    // Summary row
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoAssignStatChip("${s.proposals.count { it.included }} seçili", accentColor, Modifier.weight(1f))
                            AutoAssignStatChip("${s.unassigned.size} atanamadı", ErrorRed, Modifier.weight(1f))
                            if (s.warnings.isNotEmpty())
                                AutoAssignStatChip("${s.warnings.size} uyarı", Color(0xFFF59E0B), Modifier.weight(1f))
                        }
                    }

                    // Select all / none row
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.selectAllProposals(true) },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) { Text("Tümünü Seç", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = { viewModel.selectAllProposals(false) },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, AppColorState.textSecondary.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColorState.textSecondary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) { Text("Tümünü Kaldır", style = MaterialTheme.typography.labelSmall) }
                            OutlinedButton(
                                onClick = { viewModel.runAutoAssign() },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, AppColorState.textSecondary.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColorState.textSecondary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Yenile", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Proposals
                    if (s.proposals.isEmpty()) {
                        item {
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppColorState.surface), shape = RoundedCornerShape(14.dp)) {
                                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.SearchOff, null, tint = AppColorState.textSecondary, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Atanacak ders bulunamadı", color = AppColorState.textSecondary)
                                    Text("Hocaların müsaitlik haritası girilmiş olmalı", color = AppColorState.textSecondary.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                "Önerilen Atamalar (${s.proposals.size})",
                                color = accentColor, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        itemsIndexed(s.proposals) { index, proposal ->
                            AutoAssignProposalCard(
                                proposal = proposal,
                                accentColor = accentColor,
                                onToggle = { viewModel.toggleProposal(index) }
                            )
                        }
                    }

                    // Unassigned courses
                    if (s.unassigned.isNotEmpty()) {
                        item {
                            Text(
                                "Atanamayan Dersler (${s.unassigned.size})",
                                color = ErrorRed, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(s.unassigned) { course ->
                            Card(
                                Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.07f)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.25f))
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(ErrorRed.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(course.courseCode.take(3), color = ErrorRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(course.courseName, color = AppColorState.textPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Uygun hoca/saat/sınıf bulunamadı", color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    // Warnings
                    if (s.warnings.isNotEmpty()) {
                        item {
                            Text("Uyarılar", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                        }
                        items(s.warnings) { warning ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF59E0B).copy(alpha = 0.08f)).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(warning, color = Color(0xFFF59E0B), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Save button
                    item {
                        val selectedCount = s.proposals.count { it.included }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.confirmAutoAssign() },
                            enabled = selectedCount > 0,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                disabledContainerColor = AppColorState.surface2
                            )
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (selectedCount > 0) "$selectedCount Atamayı Kaydet" else "Atama Seçilmedi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(Modifier.height(80.dp))
                    }
                }

                // ── SAVING ───────────────────────────────────────
                is AdminViewModel.AutoAssignState.Saving -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                CircularProgressIndicator(color = EmeraldGreen, strokeWidth = 3.dp, modifier = Modifier.size(56.dp))
                                Text("Firebase'e kaydediliyor...", color = AppColorState.textSecondary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // ── DONE (LaunchedEffect handles nav) ────────────
                is AdminViewModel.AutoAssignState.Done -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(64.dp))
                                Text("Tamamlandı!", color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoAssignIdleSection(
    viewModel: AdminViewModel,
    accentColor: Color,
    unassignedCourses: List<Course>,
    availabilities: List<com.example.academicmanager.data.LecturerAvailability>,
    classrooms: List<Classroom>
) {
    val approvedAvailCount = availabilities.count { it.status == AvailabilityStatus.APPROVED }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Info card
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = accentColor, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Nasıl Çalışır?", color = accentColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                listOf(
                    "Hocaların müsaitlik haritaları incelenir",
                    "Her ders için uygun hoca, saat ve sınıf bulunur",
                    "Çakışmalar otomatik engellenir",
                    "Lab dersleri için ayrı lab seansı eklenir",
                    "Yük dengeleme ile adil dağılım sağlanır"
                ).forEachIndexed { i, text ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(20.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) { Text("${i+1}", color = accentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(10.dp))
                        Text(text, color = AppColorState.textPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Stats
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AutoAssignStatChip("${unassignedCourses.size} ders bekliyor", accentColor, Modifier.weight(1f))
            AutoAssignStatChip("$approvedAvailCount hoca müsait", EmeraldGreen, Modifier.weight(1f))
            AutoAssignStatChip("${classrooms.size} sınıf var", Color(0xFFF59E0B), Modifier.weight(1f))
        }

        // Requirements check
        val hasIssues = unassignedCourses.isEmpty() || approvedAvailCount == 0 || classrooms.isEmpty()
        if (hasIssues) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ön Koşullar", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                    RequirementRow("Atanacak ders var", unassignedCourses.isNotEmpty())
                    RequirementRow("Onaylı müsaitlik var", approvedAvailCount > 0)
                    RequirementRow("Sınıf tanımlı", classrooms.isNotEmpty())
                }
            }
        }

        Button(
            onClick = { viewModel.runAutoAssign() },
            enabled = !hasIssues,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, disabledContainerColor = AppColorState.surface2)
        ) {
            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Otomatik Atamayı Başlat", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun RequirementRow(label: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (met) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            null, tint = if (met) EmeraldGreen else ErrorRed, modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (met) AppColorState.textPrimary else ErrorRed, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AutoAssignStatChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AutoAssignProposalCard(
    proposal: AdminViewModel.AutoAssignProposal,
    accentColor: Color,
    onToggle: () -> Unit
) {
    val isLab = proposal.entry.sessionType == SessionType.LAB
    val cardColor  = if (proposal.included) (if (isLab) Color(0xFFF59E0B) else accentColor) else AppColorState.textSecondary
    val bgAlpha    = if (proposal.included) 0.08f else 0.04f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = bgAlpha)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, cardColor.copy(alpha = if (proposal.included) 0.35f else 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gün badge
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(cardColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dayLocalizedShort(proposal.entry.dayOfWeek, weekDaysShort()), color = cardColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Text(proposal.entry.timeSlot.take(5), color = cardColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        proposal.entry.courseName,
                        color = if (proposal.included) AppColorState.textPrimary else AppColorState.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isLab) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFF59E0B).copy(alpha = 0.2f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                            Text("LAB", color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                    }
                }
                Text(proposal.entry.lecturerName, color = cardColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Text("${proposal.entry.classroomName} · ${proposal.entry.timeSlot}", color = AppColorState.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
            // Toggle switch
            Switch(
                checked = proposal.included,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = cardColor,
                    uncheckedTrackColor = AppColorState.surface2
                )
            )
        }
    }
}

@Composable
private fun ScheduleEntryCard(entry: ScheduleEntry, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = AppColorState.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EmeraldGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dayLocalizedShort(entry.dayOfWeek, weekDaysShort()).uppercase(),
                    color = EmeraldGreen,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.courseName,
                    color = AppColorState.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${entry.lecturerName} · ${entry.classroomName}",
                    color = AppColorState.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    entry.timeSlot,
                    color = EmeraldGreen,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = ErrorRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = AppColorState.surface,
            title = { Text(stringResource(R.string.remove_assignment_title), color = ErrorRed) },
            text = {
                Text(
                    stringResource(R.string.remove_assignment_msg, entry.courseName, entry.dayOfWeek, entry.timeSlot),
                    color = AppColorState.textPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = AppColorState.textSecondary)
                }
            }
        )
    }
}

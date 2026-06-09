package com.example.academicmanager.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.academicmanager.R
import com.example.academicmanager.data.*
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.*

private val RiskAmber    = Color(0xFFF59E0B)
private val RiskCritical = Color(0xFFEF4444)

// ─────────────────────────────────────────────────────────────
// HOCA: Risk Dashboard
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturerRiskScreen(
    authViewModel    : AuthViewModel,
    adminViewModel   : AdminViewModel,
    riskViewModel    : AttendanceRiskViewModel,
    navController    : NavController
) {
    val user     = authViewModel.currentUser ?: return
    val students by adminViewModel.students.collectAsState()
    val colors   = LocalAppColors.current

    val risks by remember(user.username, students) {
        riskViewModel.risksForLecturer(user.username, students)
    }.collectAsState(initial = emptyList())

    RiskDashboard(
        title         = stringResource(R.string.risk_screen_title),
        subtitle      = stringResource(R.string.risk_screen_subtitle),
        risks         = risks,
        riskViewModel = riskViewModel,
        navController = navController,
        colors        = colors
    )
}

// ─────────────────────────────────────────────────────────────
// ADMİN: Risk Dashboard (tüm bölüm)
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRiskScreen(
    adminViewModel: AdminViewModel,
    riskViewModel : AttendanceRiskViewModel,
    navController : NavController
) {
    val students    by adminViewModel.students.collectAsState()
    val courses     by adminViewModel.courses.collectAsState()
    val colors      = LocalAppColors.current

    val departments = remember(courses) {
        courses.map { it.department }.distinct().sorted()
    }
    var selectedDept by remember(departments) {
        mutableStateOf(departments.firstOrNull() ?: "")
    }

    val risks by remember(selectedDept, students) {
        riskViewModel.risksForDepartment(selectedDept, students)
    }.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.risk_admin_title), color = colors.textPrimary, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        if (selectedDept.isNotBlank())
                            Text(selectedDept, color = colors.textSecondary,
                                style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = EmeraldGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { pad ->
        Column(modifier = Modifier.padding(pad).fillMaxSize()) {
            if (departments.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = departments.indexOf(selectedDept).coerceAtLeast(0),
                    containerColor   = colors.surface,
                    contentColor     = EmeraldGreen,
                    edgePadding      = 12.dp
                ) {
                    departments.forEach { dept ->
                        Tab(
                            selected = dept == selectedDept,
                            onClick  = { selectedDept = dept },
                            text = {
                                Text(dept.take(20),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (dept == selectedDept) FontWeight.Bold else FontWeight.Normal)
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                RiskDashboard(
                    title         = stringResource(R.string.risk_admin_title),
                    subtitle      = stringResource(R.string.risk_admin_subtitle, selectedDept),
                    risks         = risks,
                    riskViewModel = riskViewModel,
                    navController = navController,
                    colors        = colors,
                    showTopBar    = false
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ORTAK: Risk Listesi UI
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RiskDashboard(
    title        : String,
    subtitle     : String,
    risks        : List<AttendanceRisk>,
    riskViewModel: AttendanceRiskViewModel,
    navController: NavController,
    colors       : AppColors,
    showTopBar   : Boolean = true
) {
    val criticalCount = riskViewModel.criticalCount(risks)
    val warningCount  = riskViewModel.warningCount(risks)

    var filter by remember { mutableStateOf("ALL") }

    val displayed = when (filter) {
        "CRITICAL" -> risks.filter { it.level == RiskLevel.CRITICAL }
        "WARNING"  -> risks.filter { it.level == RiskLevel.WARNING  }
        else       -> risks
    }

    Scaffold(
        containerColor = colors.background,
        topBar = if (showTopBar) ({
            TopAppBar(
                title = {
                    Column {
                        Text(title, color = colors.textPrimary, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Text(subtitle, color = colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = EmeraldGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }) else ({})
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(pad).fillMaxSize()
        ) {
            // ── Özet ──────────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RiskSummaryCard(stringResource(R.string.risk_summary_critical), criticalCount, RiskCritical,
                        Icons.Default.Error, modifier = Modifier.weight(1f))
                    RiskSummaryCard(stringResource(R.string.risk_summary_warning), warningCount, RiskAmber,
                        Icons.Default.Warning, modifier = Modifier.weight(1f))
                    RiskSummaryCard(stringResource(R.string.risk_summary_total), risks.size, colors.textSecondary,
                        Icons.Default.People, modifier = Modifier.weight(1f))
                }
            }

            // ── Filtre ─────────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to stringResource(R.string.risk_filter_all), "CRITICAL" to stringResource(R.string.risk_filter_critical), "WARNING" to stringResource(R.string.risk_filter_warning))
                        .forEach { (key, label) ->
                            val isSelected = filter == key
                            val chipColor  = when (key) {
                                "CRITICAL" -> RiskCritical
                                "WARNING"  -> RiskAmber
                                else       -> EmeraldGreen
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick  = { filter = key },
                                label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor    = chipColor.copy(0.15f),
                                    selectedLabelColor        = chipColor,
                                    selectedLeadingIconColor  = chipColor
                                )
                            )
                        }
                }
            }

            if (displayed.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = EmeraldGreen, modifier = Modifier.size(48.dp))
                            Text(stringResource(R.string.risk_empty_title),
                                color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.risk_empty_desc),
                                color = colors.textSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                // Ders bazında grupla
                val byCourse = displayed.groupBy { it.courseCode }
                byCourse.forEach { (courseCode, courseRisks) ->
                    item(key = "header_$courseCode") {
                        val rep = courseRisks.first()
                        CourseRiskHeader(
                            courseName = rep.courseName,
                            courseCode = courseCode,
                            count      = courseRisks.size,
                            colors     = colors
                        )
                    }
                    items(courseRisks, key = { "${it.courseCode}_${it.studentUsername}" }) { risk ->
                        RiskStudentCard(risk = risk, colors = colors)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Yardımcı Composable'lar
// ─────────────────────────────────────────────────────────────

@Composable
private fun RiskSummaryCard(
    label   : String,
    count   : Int,
    color   : Color,
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    val colors = LocalAppColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(0.1f)),
        shape  = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(count.toString(), color = color, fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge)
            Text(label, color = color.copy(0.8f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CourseRiskHeader(
    courseName: String, courseCode: String, count: Int, colors: AppColors
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(Modifier.weight(1f), color = colors.border)
        Surface(shape = RoundedCornerShape(20.dp), color = colors.surface) {
            Text(stringResource(R.string.risk_course_header, courseName, courseCode, count),
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
        }
        HorizontalDivider(Modifier.weight(1f), color = colors.border)
    }
}

@Composable
private fun RiskStudentCard(risk: AttendanceRisk, colors: AppColors) {
    val levelColor = if (risk.level == RiskLevel.CRITICAL) RiskCritical else RiskAmber
    val levelIcon  = if (risk.level == RiskLevel.CRITICAL) Icons.Default.Error else Icons.Default.Warning
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        colors  = CardDefaults.cardColors(containerColor = colors.surface),
        shape   = RoundedCornerShape(14.dp),
        border  = BorderStroke(1.dp, levelColor.copy(0.25f)),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Avatar
                Box(Modifier.size(40.dp).clip(CircleShape).background(levelColor.copy(0.13f)),
                    contentAlignment = Alignment.Center) {
                    Text(risk.studentName.firstOrNull()?.uppercase() ?: "?",
                        color = levelColor, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(risk.studentName, color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    if (risk.studentId.isNotBlank())
                        Text(risk.studentId, color = colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall)
                }
                // Seviye chip
                Surface(shape = RoundedCornerShape(20.dp), color = levelColor.copy(0.13f)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(levelIcon, null, tint = levelColor, modifier = Modifier.size(11.dp))
                        Text(
                            "%.0f%%".format(risk.percentage),
                            color = levelColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Devam çubuğu
            AttendanceBar(
                attended  = risk.attendedCount,
                total     = risk.totalSessions,
                pct       = risk.percentage,
                levelColor = levelColor
            )

            // Genişletilmiş detay
            if (expanded) {
                HorizontalDivider(color = colors.border)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailStat(stringResource(R.string.risk_detail_attended), "${risk.attendedCount}/${risk.totalSessions}",
                        EmeraldGreen, colors)
                    if (risk.consecutiveMissed > 0)
                        DetailStat(stringResource(R.string.risk_detail_consec), stringResource(R.string.risk_detail_consec_val, risk.consecutiveMissed),
                            levelColor, colors)
                    DetailStat(stringResource(R.string.risk_detail_last_seen), risk.lastSeenDate, colors.textSecondary, colors)
                }
            }
        }
    }
}

@Composable
private fun AttendanceBar(attended: Int, total: Int, pct: Float, levelColor: Color) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.risk_sessions_fraction, attended, total), color = colors.textSecondary,
                style = MaterialTheme.typography.labelSmall)
            Text("%.0f%%".format(pct), color = levelColor,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            .background(colors.surface2)) {
            Box(
                Modifier.fillMaxWidth((pct / 100f).coerceIn(0f, 1f)).fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp)).background(levelColor)
            )
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String, color: Color, colors: AppColors) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
        Text(value, color = color, fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall)
    }
}

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

private val PeerPurple = Color(0xFF8B5CF6)
private val PeerTeal   = Color(0xFF14B8A6)

// ─────────────────────────────────────────────────────────────
// ADMİN: Eşleştirme Paneli
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPeerMatchScreen(
    adminViewModel   : AdminViewModel,
    peerMatchViewModel: PeerMatchViewModel,
    navController    : NavController
) {
    val courses  by adminViewModel.courses.collectAsState()
    val students by adminViewModel.students.collectAsState()
    val records  by adminViewModel.attendanceRecords.collectAsState()
    val colors   = LocalAppColors.current

    val departments = remember(courses) { courses.map { it.department }.distinct().sorted() }
    var selectedDept by remember { mutableStateOf(departments.firstOrNull() ?: "") }
    var isRunning    by remember { mutableStateOf(false) }
    var lastResult   by remember { mutableStateOf<Int?>(null) }

    val existingMatches by remember(selectedDept) {
        peerMatchViewModel.getMatchesByDepartment(selectedDept)
    }.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        peerMatchViewModel.matchingResult.collect { count ->
            isRunning  = false
            lastResult = count
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.peer_admin_title), color = colors.textPrimary, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.peer_admin_subtitle), color = colors.textSecondary,
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
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(pad)
        ) {
            // Bölüm seçici
            item {
                Card(colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.peer_select_dept), color = colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall)
                        departments.forEach { dept ->
                            val isSelected = dept == selectedDept
                            Surface(
                                onClick = { selectedDept = dept },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) PeerPurple.copy(0.13f) else colors.surface2,
                                border = if (isSelected) BorderStroke(1.dp, PeerPurple) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.School, null,
                                        tint = if (isSelected) PeerPurple else colors.textSecondary,
                                        modifier = Modifier.size(16.dp))
                                    Text(dept,
                                        color = if (isSelected) PeerPurple else colors.textPrimary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.weight(1f))
                                    if (isSelected)
                                        Icon(Icons.Default.CheckCircle, null,
                                            tint = PeerPurple, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Eşleştirme çalıştır
            item {
                Card(colors = CardDefaults.cardColors(containerColor = PeerPurple.copy(0.08f)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, PeerPurple.copy(0.2f)),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.AutoAwesome, null,
                                tint = PeerPurple, modifier = Modifier.size(22.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.peer_run_auto), color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.peer_criteria),
                                    color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        lastResult?.let { cnt ->
                            Surface(shape = RoundedCornerShape(8.dp),
                                color = if (cnt > 0) EmeraldGreen.copy(0.1f) else colors.surface2) {
                                Text(
                                    if (cnt > 0) stringResource(R.string.peer_result_found, cnt)
                                    else stringResource(R.string.peer_result_none),
                                    color = if (cnt > 0) EmeraldGreen else colors.textSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                isRunning  = true
                                lastResult = null
                                peerMatchViewModel.runMatching(
                                    department      = selectedDept,
                                    records         = records,
                                    students        = students,
                                    existingMatches = existingMatches
                                )
                            },
                            enabled  = selectedDept.isNotBlank() && !isRunning,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = PeerPurple),
                            shape    = RoundedCornerShape(12.dp)
                        ) {
                            if (isRunning) {
                                CircularProgressIndicator(Modifier.size(18.dp),
                                    color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.peer_running))
                            } else {
                                Icon(Icons.Default.AutoAwesome, null,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.peer_run_btn), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Mevcut eşleşmeler
            val activeMatches = existingMatches.filter {
                it.status in listOf(PeerMatchStatus.PENDING, PeerMatchStatus.ACTIVE)
            }
            val pastMatches = existingMatches.filter {
                it.status in listOf(PeerMatchStatus.DECLINED, PeerMatchStatus.EXPIRED)
            }

            item {
                Text(stringResource(R.string.peer_active_matches, activeMatches.size),
                    color = colors.textPrimary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)
            }

            if (activeMatches.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.People, null,
                                tint = colors.textSecondary.copy(0.4f), modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.peer_no_matches), color = colors.textSecondary,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                items(activeMatches, key = { it.id }) { match ->
                    AdminMatchCard(match = match, students = students, viewModel = peerMatchViewModel, colors = colors)
                }
            }

            if (pastMatches.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.peer_past_matches, pastMatches.size), color = colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall)
                }
                items(pastMatches, key = { "past_${it.id}" }) { match ->
                    AdminMatchCard(match = match, students = students, viewModel = peerMatchViewModel, colors = colors)
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ÖĞRENCİ: Akran Eşleştirmelerim
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentPeerMatchScreen(
    authViewModel     : AuthViewModel,
    adminViewModel    : AdminViewModel,
    peerMatchViewModel: PeerMatchViewModel,
    navController     : NavController
) {
    val user    = authViewModel.currentUser ?: return
    val students by adminViewModel.students.collectAsState()
    val colors  = LocalAppColors.current

    val allMatches by remember(user.username) {
        peerMatchViewModel.getMatchesForStudent(user.username)
    }.collectAsState(initial = emptyList())

    // Süresi dolmuş pending eşleşmeleri filtrele
    val now        = System.currentTimeMillis()
    val active     = allMatches.filter {
        it.status in listOf(PeerMatchStatus.PENDING, PeerMatchStatus.ACTIVE) &&
        (it.expiresAt == 0L || it.expiresAt > now)
    }
    val past       = allMatches.filter {
        it.status in listOf(PeerMatchStatus.DECLINED, PeerMatchStatus.EXPIRED) ||
        (it.status == PeerMatchStatus.PENDING && it.expiresAt > 0L && it.expiresAt <= now)
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.peer_student_title), color = colors.textPrimary, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.peer_student_subtitle), color = colors.textSecondary,
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
        if (allMatches.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(80.dp).clip(CircleShape).background(colors.surface),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.People, null,
                            tint = colors.textSecondary.copy(0.4f), modifier = Modifier.size(36.dp))
                    }
                    Text(stringResource(R.string.peer_no_student_matches), color = colors.textSecondary,
                        fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.peer_no_student_hint),
                        color = colors.textSecondary.copy(0.6f),
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(pad)
            ) {
                if (active.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.peer_active_section), color = colors.textPrimary,
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                    items(active, key = { it.id }) { match ->
                        StudentMatchCard(
                            match     = match,
                            myUsername = user.username,
                            students  = students,
                            viewModel = peerMatchViewModel,
                            colors    = colors
                        )
                    }
                }
                if (past.isNotEmpty()) {
                    item {
                        HorizontalDivider(color = colors.border)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.peer_past_section), color = colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    items(past, key = { "p_${it.id}" }) { match ->
                        StudentMatchCard(match, user.username, students, peerMatchViewModel, colors)
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ORTAK: Kart Composable'lar
// ─────────────────────────────────────────────────────────────

@Composable
private fun AdminMatchCard(
    match    : PeerMatch,
    students : List<Lecturer>,
    viewModel: PeerMatchViewModel,
    colors   : AppColors
) {
    val mentorStudent = students.find { it.username == match.mentorUsername }
    val menteeStudent = students.find { it.username == match.menteeUsername }
    val statusColor   = matchStatusColor(match.status)
    val activeStr   = stringResource(R.string.peer_status_active)
    val declinedStr = stringResource(R.string.peer_status_declined)
    val expiredStr  = stringResource(R.string.peer_status_expired)
    val pendingStr  = stringResource(R.string.peer_status_pending)
    val statusLabel = when (match.status) {
        PeerMatchStatus.ACTIVE   -> activeStr
        PeerMatchStatus.DECLINED -> declinedStr
        PeerMatchStatus.EXPIRED  -> expiredStr
        else                     -> pendingStr
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape  = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, statusColor.copy(0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PeopleAlt, null, tint = PeerPurple, modifier = Modifier.size(18.dp))
                Text(match.courseName, color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f))
                StatusPill(statusLabel, statusColor)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoleAvatarCard(
                    name  = mentorStudent?.fullName ?: match.mentorUsername,
                    id    = mentorStudent?.studentId ?: "",
                    role  = "Mentor",
                    color = PeerTeal,
                    accepted = match.mentorAccepted,
                    modifier = Modifier.weight(1f)
                )
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.CompareArrows, null,
                        tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
                RoleAvatarCard(
                    name  = menteeStudent?.fullName ?: match.menteeUsername,
                    id    = menteeStudent?.studentId ?: "",
                    role  = "Mentee",
                    color = PeerPurple,
                    accepted = match.menteeAccepted,
                    modifier = Modifier.weight(1f)
                )
            }
            if (match.status in listOf(PeerMatchStatus.PENDING, PeerMatchStatus.ACTIVE)) {
                OutlinedButton(
                    onClick = { viewModel.cancelMatch(match) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = BorderStroke(1.dp, ErrorRed.copy(0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text(stringResource(R.string.peer_cancel_match), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun StudentMatchCard(
    match      : PeerMatch,
    myUsername : String,
    students   : List<Lecturer>,
    viewModel  : PeerMatchViewModel,
    colors     : AppColors
) {
    val myRole         = viewModel.myRole(match, myUsername)
    val isMentor       = myRole == "mentor"
    val roleColor      = if (isMentor) PeerTeal else PeerPurple
    val statusColor    = matchStatusColor(match.status)
    val activeStr2   = stringResource(R.string.peer_status_active)
    val declinedStr2 = stringResource(R.string.peer_status_declined)
    val expiredStr2  = stringResource(R.string.peer_status_expired)
    val pendingStr2  = stringResource(R.string.peer_status_pending)
    val statusLabel = when (match.status) {
        PeerMatchStatus.ACTIVE   -> activeStr2
        PeerMatchStatus.DECLINED -> declinedStr2
        PeerMatchStatus.EXPIRED  -> expiredStr2
        else                     -> pendingStr2
    }
    val isActive       = match.status == PeerMatchStatus.ACTIVE
    val isPending      = match.status == PeerMatchStatus.PENDING
    val myAccepted     = if (isMentor) match.mentorAccepted else match.menteeAccepted

    // İsim yalnızca ACTIVE durumda göster (gizlilik)
    val partnerUsername = viewModel.partnerUsername(match, myUsername)
    val partnerStudent  = if (isActive) students.find { it.username == partnerUsername } else null
    val partnerDisplay  = partnerStudent?.fullName ?: if (isActive) partnerUsername else "—"

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape  = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, statusColor.copy(0.25f)),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Başlık
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(roleColor.copy(0.15f)),
                    contentAlignment = Alignment.Center) {
                    Icon(
                        if (isMentor) Icons.Default.Star else Icons.Default.School,
                        null, tint = roleColor, modifier = Modifier.size(22.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(match.courseName, color = colors.textPrimary,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (isMentor) stringResource(R.string.peer_you_are_mentor)
                        else stringResource(R.string.peer_mentor_assigned),
                        color = roleColor, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                StatusPill(statusLabel, statusColor)
            }

            // Karşı taraf bilgisi
            Surface(shape = RoundedCornerShape(10.dp),
                color = if (isActive) roleColor.copy(0.08f) else colors.surface2,
                border = if (isActive) BorderStroke(1.dp, roleColor.copy(0.2f)) else null) {
                Row(Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (isActive) Icons.Default.Person else Icons.Default.Lock,
                        null,
                        tint = if (isActive) roleColor else colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            if (isActive) partnerDisplay
                            else if (isMentor) stringResource(R.string.peer_mentee_hidden)
                            else stringResource(R.string.peer_mentor_hidden),
                            color = if (isActive) colors.textPrimary else colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isActive && partnerStudent != null && partnerStudent.studentId.isNotBlank())
                            Text(partnerStudent.studentId, color = roleColor.copy(0.7f),
                                style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Kabul/Ret butonları
            if (isPending && !myAccepted) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.respond(match, myUsername, true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.peer_match_accept), fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { viewModel.respond(match, myUsername, false) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = BorderStroke(1.dp, ErrorRed.copy(0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.peer_match_decline), fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (isPending && myAccepted) {
                Surface(shape = RoundedCornerShape(8.dp), color = EmeraldGreen.copy(0.08f)) {
                    Row(Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                        Text(stringResource(R.string.peer_you_accepted),
                            color = EmeraldGreen, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Yardımcı Composable'lar
// ─────────────────────────────────────────────────────────────

@Composable
private fun RoleAvatarCard(
    name    : String,
    id      : String,
    role    : String,
    color   : Color,
    accepted: Boolean,
    modifier: Modifier
) {
    val colors = LocalAppColors.current
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(0.08f), modifier = modifier) {
        Column(Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = color.copy(0.15f)) {
                Text("  $role  ", color = color,
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp))
            }
            Text(name, color = colors.textPrimary, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                maxLines = 2)
            if (id.isNotBlank())
                Text(id, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
            if (accepted)
                Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
            else
                Icon(Icons.Default.HourglassEmpty, null, tint = colors.textSecondary.copy(0.4f), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(0.12f)) {
        Text("  $label  ", color = color,
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp))
    }
}

private fun matchStatusColor(status: String): Color = when (status) {
    PeerMatchStatus.ACTIVE   -> EmeraldGreen
    PeerMatchStatus.DECLINED,
    PeerMatchStatus.EXPIRED  -> ErrorRed
    else                     -> Color(0xFFF59E0B)
}


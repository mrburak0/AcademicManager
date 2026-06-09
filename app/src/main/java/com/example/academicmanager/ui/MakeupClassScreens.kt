package com.example.academicmanager.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.academicmanager.R
import com.example.academicmanager.data.*
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.*
import java.time.format.DateTimeFormatter
import java.time.LocalDate

private val MakeupOrange = Color(0xFFF97316)
private val MakeupBlue   = Color(0xFF3B82F6)

// ─────────────────────────────────────────────────────────────
// HOCA: Telafi Dersi Ekranı
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturerMakeupScreen(
    authViewModel  : AuthViewModel,
    adminViewModel : AdminViewModel,
    makeupViewModel: MakeupViewModel,
    navController  : NavController
) {
    val user       = authViewModel.currentUser ?: return
    val allEntries by adminViewModel.scheduleEntries.collectAsState()
    val allCourses by adminViewModel.courses.collectAsState()
    val allAvails  by adminViewModel.availabilities.collectAsState()
    val colors     = LocalAppColors.current

    val myCourses = remember(allEntries, user.fullName) {
        allEntries.filter { it.lecturerName == user.fullName }.distinctBy { it.courseCode }
    }
    val myAvailability = remember(allAvails, user.username) {
        allAvails.filter { it.lecturerUsername == user.username }.maxByOrNull { it.timestamp }
    }

    val existingRequests by remember(user.username) {
        makeupViewModel.getRequestsByLecturer(user.username)
    }.collectAsState(initial = emptyList())

    // ── Ekran durumu ─────────────────────────────────────────
    var selectedEntry   by remember { mutableStateOf<ScheduleEntry?>(null) }
    var cancelReason    by remember { mutableStateOf("") }
    var proposedSlots   by remember { mutableStateOf<List<MakeupSlot>>(emptyList()) }
    var phase           by remember { mutableStateOf(0) } // 0=select,1=reason,2=slots
    var isSaving        by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        makeupViewModel.saveResult.collect { ok ->
            isSaving = false
            if (ok) { phase = 0; selectedEntry = null; cancelReason = ""; proposedSlots = emptyList() }
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.makeup_planner_title), color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (phase > 0) phase-- else navController.popBackStack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = EmeraldGreen) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(pad).fillMaxSize()
        ) {
            // ── Aktif talepler ──────────────────────────────
            val active = existingRequests.filter { it.status == MakeupStatus.VOTING || it.status == MakeupStatus.CONFIRMED }
            if (active.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.makeup_active_section), color = colors.textPrimary,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                items(active, key = { it.id }) { req ->
                    MakeupRequestCard(
                        request     = req,
                        viewModel   = makeupViewModel,
                        myUsername  = user.username,
                        isLecturer  = true
                    )
                }
                item { HorizontalDivider(color = colors.border) }
            }

            // ── Faz 0: Ders seçimi ──────────────────────────
            if (phase == 0) {
                item {
                    Text(stringResource(R.string.makeup_which_course),
                        color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                if (myCourses.isEmpty()) {
                    item {
                        EmptyState(stringResource(R.string.makeup_no_courses), Icons.Default.Book, colors)
                    }
                } else {
                    items(myCourses) { entry ->
                        CourseSelectCard(
                            entry      = entry,
                            isSelected = selectedEntry?.courseCode == entry.courseCode,
                            onClick    = { selectedEntry = entry }
                        )
                    }
                }
                item {
                    Button(
                        onClick = { phase = 1 },
                        enabled = selectedEntry != null,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MakeupOrange),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.makeup_enter_reason), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Faz 1: İptal gerekçesi ──────────────────────
            if (phase == 1) {
                item {
                    selectedEntry?.let { e ->
                        InfoBanner("${e.courseName} — ${e.dayOfWeek} ${e.timeSlot}", MakeupOrange, colors)
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.makeup_reason_label), color = colors.textSecondary,
                                style = MaterialTheme.typography.labelSmall)
                            OutlinedTextField(
                                value = cancelReason,
                                onValueChange = { cancelReason = it },
                                placeholder = { Text(stringResource(R.string.makeup_reason_hint)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MakeupOrange,
                                    unfocusedBorderColor = colors.border
                                )
                            )
                        }
                    }
                }
                item {
                    Button(
                        onClick = {
                            val entry = selectedEntry ?: return@Button
                            val dept = allCourses.find { it.courseCode == entry.courseCode }?.department ?: ""
                            val deptEntries = allEntries.filter { e ->
                                allCourses.any { c -> c.courseCode == e.courseCode && c.department == dept }
                            }
                            val lecturerEntries = allEntries.filter { it.lecturerName == user.fullName }
                            proposedSlots = makeupViewModel.proposeSlots(
                                cancelledDay      = entry.dayOfWeek,
                                cancelledTimeSlot = entry.timeSlot,
                                deptEntries       = deptEntries,
                                lecturerEntries   = lecturerEntries,
                                lecturerAvail     = myAvailability
                            )
                            phase = 2
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MakeupOrange),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.makeup_propose_btn), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Faz 2: Önerilen slotlar + oylama başlat ─────
            if (phase == 2) {
                item {
                    Text(stringResource(R.string.makeup_propose_desc),
                        color = colors.textPrimary, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.makeup_selected_slots),
                        color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
                if (proposedSlots.isEmpty()) {
                    item { EmptyState(stringResource(R.string.makeup_no_slots_found), Icons.Default.Warning, colors) }
                } else {
                    items(proposedSlots, key = { it.id }) { slot ->
                        ProposedSlotCard(slot = slot, colors = colors)
                    }
                }
                item {
                    Button(
                        onClick = {
                            val entry = selectedEntry ?: return@Button
                            val dept  = allCourses.find { it.courseCode == entry.courseCode }?.department ?: user.department
                            isSaving = true
                            makeupViewModel.createRequest(
                                courseCode        = entry.courseCode,
                                courseName        = entry.courseName,
                                department        = dept,
                                lecturerUsername  = user.username,
                                lecturerName      = user.fullName,
                                cancelledDay      = entry.dayOfWeek,
                                cancelledTimeSlot = entry.timeSlot,
                                cancelReason      = cancelReason,
                                proposedSlots     = proposedSlots
                            )
                        },
                        enabled = proposedSlots.isNotEmpty() && !isSaving,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.makeup_saving))
                        } else {
                            Icon(Icons.Default.HowToVote, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.makeup_start_voting_btn), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ÖĞRENCİ: Telafi Oylaması Ekranı
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMakeupVotingScreen(
    authViewModel  : AuthViewModel,
    adminViewModel : AdminViewModel,
    makeupViewModel: MakeupViewModel,
    navController  : NavController
) {
    val user   = authViewModel.currentUser ?: return
    val colors = LocalAppColors.current

    val requests by remember(user.department) {
        makeupViewModel.getRequestsByDepartment(user.department)
    }.collectAsState(initial = emptyList())

    val active = requests.filter { it.status == MakeupStatus.VOTING }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.makeup_voting_title), color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = EmeraldGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { pad ->
        if (active.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(80.dp).clip(CircleShape).background(colors.surface),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.HowToVote, null,
                            tint = colors.textSecondary.copy(0.4f), modifier = Modifier.size(36.dp))
                    }
                    Text(stringResource(R.string.makeup_no_active), color = colors.textSecondary,
                        fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.makeup_no_active_hint),
                        color = colors.textSecondary.copy(0.6f),
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(pad)
            ) {
                item {
                    Text(stringResource(R.string.makeup_active_count, active.size),
                        color = colors.textPrimary, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.makeup_join_time_hint),
                        color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
                items(active, key = { it.id }) { req ->
                    MakeupRequestCard(
                        request    = req,
                        viewModel  = makeupViewModel,
                        myUsername = user.username,
                        isLecturer = false
                    )
                }

                // Onaylanan / geçmiş talepler
                val past = requests.filter { it.status != MakeupStatus.VOTING }
                if (past.isNotEmpty()) {
                    item {
                        HorizontalDivider(color = colors.border)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.makeup_past_requests), color = colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    items(past, key = { "p_${it.id}" }) { req ->
                        MakeupRequestCard(req, makeupViewModel, user.username, false)
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ORTAK: Talep Kartı (hoca + öğrenci)
// ─────────────────────────────────────────────────────────────

@Composable
private fun MakeupRequestCard(
    request   : MakeupRequest,
    viewModel : MakeupViewModel,
    myUsername: String,
    isLecturer: Boolean
) {
    val colors    = LocalAppColors.current
    val hasVoted  = viewModel.hasVoted(request, myUsername)
    val winner    = viewModel.winningSlotId(request)
    val timeLeft  = viewModel.voteTimeLeftMs(request)
    val hoursLeft = (timeLeft / 3_600_000L).coerceAtLeast(0)

    var showVoteDialog    by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val statusColor = when (request.status) {
        MakeupStatus.CONFIRMED -> EmeraldGreen
        MakeupStatus.CANCELLED -> ErrorRed
        else                   -> MakeupOrange
    }
    val confirmedLabel = stringResource(R.string.makeup_status_confirmed)
    val cancelledLabel = stringResource(R.string.makeup_status_cancelled)
    val votingLabel    = stringResource(R.string.makeup_status_voting)
    val statusLabel = when (request.status) {
        MakeupStatus.CONFIRMED -> confirmedLabel
        MakeupStatus.CANCELLED -> cancelledLabel
        else                   -> votingLabel
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, statusColor.copy(0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Başlık
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(11.dp))
                    .background(MakeupOrange.copy(0.13f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EventRepeat, null,
                        tint = MakeupOrange, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(request.courseName, color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.makeup_lecturer_cancelled, request.lecturerName, request.cancelledDayOfWeek, request.cancelledTimeSlot),
                        color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
                StatusChip(statusLabel, statusColor)
            }

            if (request.cancelReason.isNotBlank()) {
                Surface(shape = RoundedCornerShape(8.dp), color = colors.surface2) {
                    Text("\"${request.cancelReason}\"",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }

            // Onaylanmış slot
            if (request.status == MakeupStatus.CONFIRMED) {
                val confirmedSlot = request.proposedSlots.find { it.id == request.confirmedSlotId }
                confirmedSlot?.let { slot ->
                    Surface(shape = RoundedCornerShape(10.dp), color = EmeraldGreen.copy(0.1f),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(0.3f))) {
                        Row(Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                            Column {
                                Text(stringResource(R.string.makeup_date_time, slot.date, slot.timeSlot),
                                    color = EmeraldGreen, fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall)
                                if (slot.classroomName.isNotBlank())
                                    Text(slot.classroomName, color = EmeraldGreen.copy(0.7f),
                                        style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Oylama devam ediyor
            if (request.status == MakeupStatus.VOTING) {
                // Oy dağılımı özeti
                val voteCount = request.votes.size
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HowToVote, null,
                        tint = MakeupOrange, modifier = Modifier.size(14.dp))
                    Text(stringResource(R.string.makeup_votes_left, voteCount, hoursLeft),
                        color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                }

                // Slot oy çubukları
                request.proposedSlots.forEach { slot ->
                    val slotVotes = request.votes.values.count { it == slot.id }
                    val fraction  = if (voteCount > 0) slotVotes.toFloat() / voteCount else 0f
                    val isWinner  = slot.id == winner && voteCount > 0
                    SlotVoteRow(slot = slot, votes = slotVotes, fraction = fraction,
                        isWinner = isWinner, colors = colors)
                }

                // Butonlar
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isLecturer && !hasVoted) {
                        Button(
                            onClick = { showVoteDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MakeupOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.HowToVote, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.makeup_vote_btn), fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (!isLecturer && hasVoted) {
                        Surface(shape = RoundedCornerShape(10.dp),
                            color = EmeraldGreen.copy(0.1f)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.CheckCircle, null,
                                    tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                Text(stringResource(R.string.makeup_voted_badge), color = EmeraldGreen,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (isLecturer) {
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.makeup_approve_btn), fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = { viewModel.cancelRequest(request) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            border = BorderStroke(1.dp, ErrorRed.copy(0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(stringResource(R.string.makeup_cancel_request_btn), fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }

    // Oy verme dialog
    if (showVoteDialog) {
        AlertDialog(
            onDismissRequest = { showVoteDialog = false },
            containerColor = colors.surface,
            title = { Text(stringResource(R.string.makeup_which_time_dialog), color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    request.proposedSlots.forEach { slot ->
                        val slotVotes = request.votes.values.count { it == slot.id }
                        OutlinedButton(
                            onClick = {
                                viewModel.vote(request.id, slot.id, myUsername)
                                showVoteDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MakeupOrange),
                            border = BorderStroke(1.dp, MakeupOrange.copy(0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                Text(formatSlotDate(slot.date), fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall)
                                Text(stringResource(R.string.makeup_vote_detail, slot.dayOfWeek, slot.timeSlot, slotVotes),
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showVoteDialog = false }) {
                    Text(stringResource(R.string.cancel), color = colors.textSecondary)
                }
            }
        )
    }

    // Onaylama dialog (hoca)
    if (showConfirmDialog) {
        val winSlot = request.proposedSlots.find { it.id == (viewModel.winningSlotId(request) ?: "") }
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = colors.surface,
            title = { Text(stringResource(R.string.makeup_confirm_slot_title), color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.makeup_best_slot_label), color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall)
                    winSlot?.let { slot ->
                        Surface(shape = RoundedCornerShape(10.dp), color = EmeraldGreen.copy(0.1f)) {
                            Text("${slot.date} ${slot.timeSlot} (${slot.dayOfWeek})",
                                color = EmeraldGreen, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp))
                        }
                    } ?: Text(stringResource(R.string.makeup_no_votes_yet), color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(color = colors.border)
                    Text(stringResource(R.string.makeup_all_slots_label), color = colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall)
                    request.proposedSlots.forEach { slot ->
                        val cnt = request.votes.values.count { it == slot.id }
                        TextButton(
                            onClick = {
                                viewModel.confirmSlot(request, slot.id)
                                showConfirmDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.makeup_slot_detail, slot.date, slot.timeSlot, cnt),
                                color = colors.textPrimary,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                if (winSlot != null) {
                    Button(
                        onClick = {
                            viewModel.confirmSlot(request, winSlot.id)
                            showConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(stringResource(R.string.makeup_confirm_top)) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel), color = colors.textSecondary)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Yardımcı Composable'lar
// ─────────────────────────────────────────────────────────────

@Composable
private fun CourseSelectCard(entry: ScheduleEntry, isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MakeupOrange.copy(0.1f) else colors.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) BorderStroke(1.dp, MakeupOrange) else null
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(MakeupOrange.copy(0.13f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Book, null, tint = MakeupOrange, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(entry.courseName, color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text("${entry.courseCode} · ${entry.dayOfWeek} · ${entry.timeSlot}",
                    color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
            if (isSelected) Icon(Icons.Default.CheckCircle, null,
                tint = MakeupOrange, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ProposedSlotCard(slot: MakeupSlot, colors: AppColors) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MakeupBlue.copy(0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(MakeupBlue.copy(0.1f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CalendarToday, null, tint = MakeupBlue, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(formatSlotDate(slot.date), color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text("${slot.dayOfWeek} · ${slot.timeSlot}", color = colors.textSecondary,
                    style = MaterialTheme.typography.labelSmall)
            }
            if (slot.conflictCount == 0) {
                StatusChip(stringResource(R.string.makeup_no_conflict_label), EmeraldGreen)
            } else {
                StatusChip(stringResource(R.string.makeup_conflict_count, slot.conflictCount), ErrorRed)
            }
        }
    }
}

@Composable
private fun SlotVoteRow(
    slot    : MakeupSlot,
    votes   : Int,
    fraction: Float,
    isWinner: Boolean,
    colors  : AppColors
) {
    val barColor = if (isWinner) EmeraldGreen else MakeupBlue
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isWinner) Icon(Icons.Default.EmojiEvents, null,
                tint = EmeraldGreen, modifier = Modifier.size(12.dp))
            Text("${slot.date} ${slot.timeSlot}",
                color = if (isWinner) EmeraldGreen else colors.textPrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isWinner) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f))
            Text(stringResource(R.string.makeup_vote_count, votes), color = barColor,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
            .background(colors.surface2)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight()
                .clip(RoundedCornerShape(2.dp)).background(barColor))
        }
    }
}

@Composable
private fun InfoBanner(text: String, color: Color, colors: AppColors) {
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(0.1f),
        border = BorderStroke(1.dp, color.copy(0.25f))) {
        Row(Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Info, null, tint = color, modifier = Modifier.size(14.dp))
            Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(0.12f)) {
        Text("  $label  ", color = color,
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp))
    }
}

@Composable
private fun EmptyState(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, colors: AppColors) {
    Card(colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = colors.textSecondary.copy(0.4f), modifier = Modifier.size(20.dp))
            Text(text, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatSlotDate(dateStr: String): String = try {
    val date = LocalDate.parse(dateStr)
    date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("tr")))
} catch (_: Exception) { dateStr }

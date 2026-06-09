package com.example.academicmanager.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.ParcelUuid
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.academicmanager.R
import com.example.academicmanager.data.*
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.AdminViewModel
import com.example.academicmanager.ui.viewmodels.AttendanceViewModel
import com.example.academicmanager.ui.viewmodels.AuthViewModel
import com.example.academicmanager.ui.viewmodels.JoinResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.EnumMap
import java.util.UUID

// ── Sabitler ─────────────────────────────────────────────────
private val APP_BLE_UUID = ParcelUuid.fromString("0000ACAD-0000-1000-8000-00805f9b34fb")
private val QrAmber  = Color(0xFFF59E0B)
private val QrPurple = Color(0xFF8B5CF6)
private const val BLE_SCAN_TIMEOUT_MS  = 15_000L
private const val BLE_MIN_RSSI         = -85          // dBm — ~10 metre eşik

// ─────────────────────────────────────────────────────────────
// HOCA: Gelişmiş QR + BLE Yoklama Oturumu Ekranı
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturerQrSessionScreen(
    authViewModel      : AuthViewModel,
    adminViewModel     : AdminViewModel,
    attendanceViewModel: AttendanceViewModel,
    navController      : NavController
) {
    val user       = authViewModel.currentUser ?: return
    val allEntries by adminViewModel.scheduleEntries.collectAsState()
    val allCourses by adminViewModel.courses.collectAsState()
    val allStudents by adminViewModel.students.collectAsState()
    val colors     = LocalAppColors.current
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()

    val myCourses = remember(allEntries, user.fullName) {
        allEntries.filter { it.lecturerName == user.fullName }.distinctBy { it.courseCode }
    }

    // ── Ekran durumu ─────────────────────────────────────────
    var selectedEntry   by remember { mutableStateOf<ScheduleEntry?>(null) }
    var durationMinutes by remember { mutableStateOf(15) }
    var activeSession   by remember { mutableStateOf<AttendanceSession?>(null) }
    var qrBitmap        by remember { mutableStateOf<Bitmap?>(null) }
    var qrSecsLeft      by remember { mutableStateOf(0L) }
    var remainingSecs   by remember { mutableStateOf(0L) }
    var bleAdvertising  by remember { mutableStateOf(false) }
    var bleError        by remember { mutableStateOf<String?>(null) }
    var isCreating      by remember { mutableStateOf(false) }
    var showExtendSheet by remember { mutableStateOf(false) }
    var bleAdvertiser   by remember { mutableStateOf<BluetoothLeAdvertiser?>(null) }
    var bleCallback     by remember { mutableStateOf<AdvertiseCallback?>(null) }

    // Gerçek zamanlı oturum (katılan öğrenci sayısı için Firestore'dan dinle)
    val liveSession by remember(activeSession?.courseCode) {
        activeSession?.let { attendanceViewModel.getActiveSession(it.courseCode) }
            ?: kotlinx.coroutines.flow.flowOf(null)
    }.collectAsState(initial = null)

    // Oturum başlatma sonucu
    LaunchedEffect(Unit) {
        attendanceViewModel.sessionStart.collect { session ->
            isCreating = false
            if (session != null) {
                activeSession = session
                remainingSecs = (session.expiresAt - System.currentTimeMillis()) / 1000
            } else {
                Toast.makeText(context, context.getString(R.string.qr_session_create_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dönen QR: her 30 saniyede bir yenilenir
    LaunchedEffect(activeSession?.id) {
        val s = activeSession ?: return@LaunchedEffect
        while (true) {
            val content = AttendanceQrHelper.currentQrContent(s.id, s.sessionSecret)
            qrBitmap = withContext(Dispatchers.Default) { generateQrBitmap(content) }
            qrSecsLeft = AttendanceQrHelper.secsUntilRotation()
            // Sonraki pencere başına kadar bekle
            delay(qrSecsLeft * 1000L)
        }
    }

    // QR saat sayacı (saniye hassasiyeti)
    LaunchedEffect(activeSession?.id) {
        if (activeSession == null) return@LaunchedEffect
        while (true) {
            delay(1000)
            qrSecsLeft = AttendanceQrHelper.secsUntilRotation()
        }
    }

    // Geri sayım + otomatik oturum kapatma
    LaunchedEffect(activeSession?.id) {
        val s = activeSession ?: return@LaunchedEffect
        while (remainingSecs > 0) {
            delay(1000)
            remainingSecs--
        }
        // Süre doldu
        val deptStudents = allStudents.filter { it.department == s.department }
        attendanceViewModel.endSession(liveSession ?: s, deptStudents)
        stopBleAdvertising(bleAdvertiser, bleCallback)
        bleAdvertising = false
        activeSession = null
        qrBitmap = null
        Toast.makeText(context, context.getString(R.string.qr_session_ended_toast), Toast.LENGTH_SHORT).show()
    }

    DisposableEffect(Unit) {
        onDispose {
            activeSession?.let {
                val deptStudents = allStudents.filter { st -> st.department == it.department }
                attendanceViewModel.endSession(liveSession ?: it, deptStudents)
            }
            stopBleAdvertising(bleAdvertiser, bleCallback)
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.qr_title_lecturer), color = colors.textPrimary,
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (activeSession != null)
                            Text(activeSession!!.courseName, color = QrAmber,
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
        AnimatedContent(
            targetState = activeSession,
            transitionSpec = {
                if (targetState != null)
                    slideInVertically { it } + fadeIn() togetherWith
                    slideOutVertically { -it } + fadeOut()
                else
                    slideInVertically { -it } + fadeIn() togetherWith
                    slideOutVertically { it } + fadeOut()
            },
            label = "session_phase",
            modifier = Modifier.padding(pad)
        ) { session ->
            if (session == null) {
                SessionSetupPane(
                    myCourses        = myCourses,
                    scheduleChecker  = { attendanceViewModel.isCurrentlyScheduled(it) },
                    selectedEntry    = selectedEntry,
                    onSelectEntry    = { selectedEntry = it },
                    durationMinutes  = durationMinutes,
                    onDurationChange = { durationMinutes = it },
                    isCreating       = isCreating,
                    onStart          = {
                        val entry = selectedEntry ?: return@SessionSetupPane
                        isCreating = true
                        val dept = allCourses.find { it.courseCode == entry.courseCode }
                            ?.department ?: user.department
                        attendanceViewModel.startSession(
                            entry = entry, department = dept,
                            lecturerUsername = user.username, lecturerName = user.fullName,
                            durationMinutes = durationMinutes
                        )
                    }
                )
            } else {
                ActiveSessionPane(
                    session        = session,
                    liveSession    = liveSession,
                    allStudents    = allStudents.filter { it.department == session.department },
                    remainingSecs  = remainingSecs,
                    qrBitmap       = qrBitmap,
                    qrSecsLeft     = qrSecsLeft,
                    bleAdvertising = bleAdvertising,
                    bleError       = bleError,
                    onBleToggle    = {
                        if (bleAdvertising) {
                            stopBleAdvertising(bleAdvertiser, bleCallback)
                            bleAdvertising = false
                        } else {
                            val err = startBleAdvertising(context, session.sessionCode) { adv, cb ->
                                bleAdvertiser = adv; bleCallback = cb
                            }
                            if (err == null) { bleAdvertising = true; bleError = null }
                            else bleError = err
                        }
                    },
                    onExtend       = { mins ->
                        scope.launch {
                            attendanceViewModel.extendSession(liveSession ?: session, mins)
                            remainingSecs += mins * 60L
                        }
                    },
                    onEnd          = {
                        val deptStudents = allStudents.filter { it.department == session.department }
                        attendanceViewModel.endSession(liveSession ?: session, deptStudents)
                        stopBleAdvertising(bleAdvertiser, bleCallback)
                        bleAdvertising = false
                        activeSession = null
                        qrBitmap = null
                    }
                )
            }
        }

        if (showExtendSheet) {
            ExtendSessionSheet(
                onDismiss = { showExtendSheet = false },
                onExtend  = { mins ->
                    showExtendSheet = false
                    val s = liveSession ?: activeSession ?: return@ExtendSessionSheet
                    attendanceViewModel.extendSession(s, mins)
                    remainingSecs += mins * 60L
                }
            )
        }
    }
}

// ── Oturum kurulum ekranı ─────────────────────────────────────

@Composable
private fun SessionSetupPane(
    myCourses       : List<ScheduleEntry>,
    scheduleChecker : (ScheduleEntry) -> Boolean,
    selectedEntry   : ScheduleEntry?,
    onSelectEntry   : (ScheduleEntry) -> Unit,
    durationMinutes : Int,
    onDurationChange: (Int) -> Unit,
    isCreating      : Boolean,
    onStart         : () -> Unit
) {
    val colors = LocalAppColors.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(stringResource(R.string.qr_select_course_hint),
                color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }

        if (myCourses.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.qr_no_courses), color = colors.textSecondary,
                        modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            items(myCourses) { entry ->
                val isSelected  = selectedEntry?.courseCode == entry.courseCode
                val isScheduled = scheduleChecker(entry)
                CoursePickCard(
                    entry = entry, isSelected = isSelected,
                    isScheduled = isScheduled,
                    onClick = { onSelectEntry(entry) }
                )
            }
        }

        item {
            DurationSelector(selected = durationMinutes, onSelect = onDurationChange)
        }

        item {
            selectedEntry?.let { entry ->
                val isScheduled = scheduleChecker(entry)
                if (!isScheduled) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(QrAmber.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, null,
                            tint = QrAmber, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.qr_course_not_scheduled),
                            color = QrAmber, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            Button(
                onClick = onStart,
                enabled = selectedEntry != null && !isCreating,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(Modifier.size(20.dp),
                        color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.qr_creating_session), fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.qr_start_attendance, durationMinutes), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CoursePickCard(
    entry      : ScheduleEntry,
    isSelected : Boolean,
    isScheduled: Boolean,
    onClick    : () -> Unit
) {
    val colors = LocalAppColors.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EmeraldGreen.copy(alpha = 0.13f) else colors.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) BorderStroke(1.dp, EmeraldGreen) else null
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp))
                    .background(if (isScheduled) EmeraldGreen.copy(0.15f) else colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isScheduled) Icons.Default.PlayCircle else Icons.Default.Book,
                    null,
                    tint = if (isScheduled) EmeraldGreen else colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.courseName, color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text("${entry.courseCode} · ${entry.dayOfWeek} · ${entry.timeSlot}",
                    color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                if (isScheduled) {
                    Spacer(Modifier.height(4.dp))
                    Surface(shape = RoundedCornerShape(20.dp),
                        color = EmeraldGreen.copy(alpha = 0.12f)) {
                        Text(stringResource(R.string.qr_active_slot_badge),
                            color = EmeraldGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
            if (isSelected)
                Icon(Icons.Default.CheckCircle, null,
                    tint = EmeraldGreen, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DurationSelector(selected: Int, onSelect: (Int) -> Unit) {
    val colors = LocalAppColors.current
    Card(colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.qr_duration_label), color = colors.textSecondary,
                style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 15, 20, 30).forEach { min ->
                    val isSel = selected == min
                    Surface(
                        onClick = { onSelect(min) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) EmeraldGreen.copy(0.15f) else colors.surface2,
                        border = if (isSel) BorderStroke(1.dp, EmeraldGreen) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text("$min",
                                color = if (isSel) EmeraldGreen else colors.textPrimary,
                                fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Normal,
                                style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.qr_mins_unit),
                                color = if (isSel) EmeraldGreen else colors.textSecondary,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// ── Aktif oturum ekranı ───────────────────────────────────────

@Composable
private fun ActiveSessionPane(
    session       : AttendanceSession,
    liveSession   : AttendanceSession?,
    allStudents   : List<Lecturer>,
    remainingSecs : Long,
    qrBitmap      : Bitmap?,
    qrSecsLeft    : Long,
    bleAdvertising: Boolean,
    bleError      : String?,
    onBleToggle   : () -> Unit,
    onExtend      : (Int) -> Unit,
    onEnd         : () -> Unit
) {
    val colors        = LocalAppColors.current
    val effectiveSession = liveSession ?: session
    val presentList   = effectiveSession.presentStudents
    val mins          = remainingSecs / 60
    val secs          = remainingSecs % 60
    val isLow         = remainingSecs < 120
    val timerColor    = if (isLow) ErrorRed else EmeraldGreen

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Sayaç ──────────────────────────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = timerColor.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                            .background(timerColor.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Timer, null, tint = timerColor,
                            modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.qr_time_left, mins, secs),
                            color = timerColor, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp
                        )
                        if (isLow)
                            Text(stringResource(R.string.qr_time_warning), color = timerColor,
                                style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        listOf(5 to stringResource(R.string.qr_extend_quick_5), 10 to stringResource(R.string.qr_extend_quick_10)).forEach { (m, label) ->
                            TextButton(
                                onClick = { onExtend(m) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(label, color = EmeraldGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // ── Dönen QR ──────────────────────────────────────────
        item {
            QrDisplayCard(qrBitmap = qrBitmap, secsLeft = qrSecsLeft)
        }

        // ── BLE kontrol ────────────────────────────────────────
        item {
            BleToggleCard(
                bleAdvertising = bleAdvertising,
                bleError       = bleError,
                sessionCode    = session.sessionCode,
                onToggle       = onBleToggle
            )
        }

        // ── Katılanlar listesi ─────────────────────────────────
        item {
            Text(stringResource(R.string.qr_students_attending, presentList.size),
                color = colors.textPrimary, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall)
        }

        if (presentList.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.HourglassEmpty, null,
                            tint = colors.textSecondary.copy(0.4f), modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.qr_no_students_yet),
                            color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(presentList, key = { it }) { username ->
                val method = effectiveSession.verificationMethods[username] ?: "?"
                val student = allStudents.find { it.username == username }
                PresentStudentRow(
                    displayName = student?.fullName ?: username,
                    studentId   = student?.studentId ?: "",
                    method      = method
                )
            }
        }

        // ── Bitir butonu ───────────────────────────────────────
        item {
            Button(
                onClick = onEnd,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed.copy(alpha = 0.13f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp), tint = ErrorRed)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.qr_end_session), color = ErrorRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QrDisplayCard(qrBitmap: Bitmap?, secsLeft: Long) {
    val colors = LocalAppColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.QrCode, null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.qr_code_label),
                    color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(12.dp))
            if (qrBitmap != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Kodu",
                        modifier = Modifier.size(260.dp).padding(10.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                // Rotasyon geri sayım çubuğu
                val fraction by animateFloatAsState(
                    targetValue = (secsLeft.coerceAtLeast(0) / 30f).coerceIn(0f, 1f),
                    animationSpec = tween(800),
                    label = "qr_bar"
                )
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.weight(1f).height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.surface2)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(EmeraldGreen.copy(0.5f), EmeraldGreen))
                                )
                        )
                    }
                    Text("${secsLeft}s",
                        color = if (secsLeft <= 5) ErrorRed else EmeraldGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold)
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp), color = EmeraldGreen, strokeWidth = 3.dp
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.qr_preparing), color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BleToggleCard(
    bleAdvertising: Boolean,
    bleError      : String?,
    sessionCode   : String,
    onToggle      : () -> Unit
) {
    val colors = LocalAppColors.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (bleAdvertising) QrPurple.copy(0.1f) else colors.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (bleAdvertising) BorderStroke(1.dp, QrPurple.copy(0.4f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (bleAdvertising) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                null,
                tint = if (bleAdvertising) QrPurple else colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (bleAdvertising) stringResource(R.string.ble_active) else stringResource(R.string.ble_inactive),
                    color = if (bleAdvertising) QrPurple else colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    if (bleAdvertising)
                        stringResource(R.string.ble_hint_active, sessionCode)
                    else
                        stringResource(R.string.ble_hint_inactive),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2
                )
                bleError?.let {
                    Text(it, color = ErrorRed,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
            Switch(
                checked = bleAdvertising,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = QrPurple
                )
            )
        }
    }
}

@Composable
private fun PresentStudentRow(displayName: String, studentId: String, method: String) {
    val colors = LocalAppColors.current
    val methodColor = when (method) {
        "BLE"    -> QrPurple
        "QR"     -> EmeraldGreen
        "MANUAL" -> QrAmber
        else     -> colors.textSecondary
    }
    val methodIcon = when (method) {
        "BLE"    -> Icons.Default.Bluetooth
        "QR"     -> Icons.Default.QrCode
        "MANUAL" -> Icons.Default.Edit
        else     -> Icons.Default.HelpOutline
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(EmeraldGreen.copy(0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    displayName.firstOrNull()?.uppercase() ?: "?",
                    color = EmeraldGreen, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(displayName, color = colors.textPrimary,
                    fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                if (studentId.isNotBlank())
                    Text(studentId, color = colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall)
            }
            Surface(shape = RoundedCornerShape(20.dp),
                color = methodColor.copy(alpha = 0.13f)) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(methodIcon, null, tint = methodColor, modifier = Modifier.size(11.dp))
                    Text(method, color = methodColor,
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ExtendSessionSheet(onDismiss: () -> Unit, onExtend: (Int) -> Unit) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = colors.surface,
        title = {
            Text(stringResource(R.string.qr_extend_title), color = colors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5 to stringResource(R.string.qr_extend_5), 10 to stringResource(R.string.qr_extend_10), 15 to stringResource(R.string.qr_extend_15)).forEach { (m, label) ->
                    OutlinedButton(
                        onClick = { onExtend(m) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(label, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.textSecondary) }
        }
    )
}

// ─────────────────────────────────────────────────────────────
// ÖĞRENCİ: Akıllı Yoklama Katılım Ekranı
// ─────────────────────────────────────────────────────────────

private enum class StudentPhase { SESSIONS, BLE_CHECK, QR_FALLBACK, RESULT }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StudentQrScanScreen(
    authViewModel      : AuthViewModel,
    adminViewModel     : AdminViewModel,
    attendanceViewModel: AttendanceViewModel,
    navController      : NavController
) {
    val user        = authViewModel.currentUser ?: return
    val allStudents by adminViewModel.students.collectAsState()
    val colors      = LocalAppColors.current
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()

    // Bölüm için aktif oturumları gerçek zamanlı dinle
    val activeSessions by remember(user.department) {
        attendanceViewModel.getActiveSessionsForDept(user.department)
    }.collectAsState(initial = emptyList())

    // ── Ekran durumu ─────────────────────────────────────────
    var phase          by remember { mutableStateOf(StudentPhase.SESSIONS) }
    var targetSession  by remember { mutableStateOf<AttendanceSession?>(null) }
    var joinResult     by remember { mutableStateOf<JoinResult?>(null) }
    var bleScanActive  by remember { mutableStateOf(false) }
    var bleScanSecs    by remember { mutableStateOf(0L) }

    // Katılım sonucu
    LaunchedEffect(Unit) {
        attendanceViewModel.joinResult.collect { result ->
            joinResult = result
            phase = StudentPhase.RESULT
            bleScanActive = false
        }
    }

    // BLE tarama mantığı — targetSession ve bleScanActive değişince çalışır
    LaunchedEffect(bleScanActive) {
        if (!bleScanActive) return@LaunchedEffect
        val session = targetSession ?: return@LaunchedEffect
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val hasBle = btAdapter != null &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        val blePermOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED

        if (hasBle && btAdapter!!.isEnabled && blePermOk) {
            bleScanSecs = BLE_SCAN_TIMEOUT_MS / 1000
            val found = scanBleForSession(btAdapter, session.sessionCode)
            if (found) {
                attendanceViewModel.markPresent(
                    session.id, user.username, user.fullName, "BLE", allStudents
                )
            } else {
                bleScanActive = false
                phase = StudentPhase.QR_FALLBACK
            }
        } else {
            // BLE desteklenmiyor/kapalı → direkt QR'ye geç
            bleScanActive = false
            phase = StudentPhase.QR_FALLBACK
        }
    }

    // BLE geri sayım (UI için)
    LaunchedEffect(bleScanActive) {
        if (!bleScanActive) { bleScanSecs = 0; return@LaunchedEffect }
        bleScanSecs = BLE_SCAN_TIMEOUT_MS / 1000
        while (bleScanSecs > 0 && bleScanActive) {
            delay(1000)
            bleScanSecs--
        }
    }

    // QR scanner launcher
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val content = result.contents
        if (content != null) {
            val session = targetSession ?: return@rememberLauncherForActivityResult
            attendanceViewModel.validateQrAndMark(
                qrContent = content, session = session,
                username = user.username, fullName = user.fullName,
                allStudents = allStudents
            )
        }
    }

    val permissions = rememberMultiplePermissionsState(
        buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    )

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.qr_title_student), color = colors.textPrimary, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (phase != StudentPhase.SESSIONS) {
                            bleScanActive = false
                            phase = StudentPhase.SESSIONS
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = EmeraldGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { pad ->
        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                slideOutHorizontally { -it } + fadeOut()
            },
            label = "student_phase",
            modifier = Modifier.padding(pad).fillMaxSize()
        ) { p ->
            when (p) {
                StudentPhase.SESSIONS ->
                    ActiveSessionsPane(
                        sessions     = activeSessions,
                        studentUsername = user.username,
                        onJoin       = { session ->
                            targetSession = session
                            if (!permissions.allPermissionsGranted) {
                                permissions.launchMultiplePermissionRequest()
                            } else {
                                bleScanActive = true
                                phase = StudentPhase.BLE_CHECK
                            }
                        }
                    )
                StudentPhase.BLE_CHECK ->
                    BleCheckPane(
                        session    = targetSession,
                        secsLeft   = bleScanSecs,
                        onFallback = { bleScanActive = false; phase = StudentPhase.QR_FALLBACK }
                    )
                StudentPhase.QR_FALLBACK ->
                    QrFallbackPane(
                        session = targetSession,
                        onScan  = {
                            val opts = ScanOptions().apply {
                                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                setPrompt(context.getString(R.string.qr_scan_prompt))
                                setBeepEnabled(true)
                                setOrientationLocked(false)
                            }
                            scanLauncher.launch(opts)
                        }
                    )
                StudentPhase.RESULT ->
                    JoinResultPane(
                        result  = joinResult,
                        onReset = { joinResult = null; phase = StudentPhase.SESSIONS }
                    )
            }
        }
    }
}

@Composable
private fun ActiveSessionsPane(
    sessions       : List<AttendanceSession>,
    studentUsername: String,
    onJoin         : (AttendanceSession) -> Unit
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.qr_active_sessions_title),
            color = colors.textPrimary, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.qr_active_sessions_desc),
            color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                            .background(colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.HourglassEmpty, null,
                            tint = colors.textSecondary.copy(0.4f), modifier = Modifier.size(36.dp))
                    }
                    Text(stringResource(R.string.qr_no_active_sessions),
                        color = colors.textSecondary, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.qr_no_active_hint),
                        color = colors.textSecondary.copy(0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sessions, key = { it.id }) { session ->
                    val alreadyJoined = studentUsername in session.presentStudents
                    SessionNotificationCard(
                        session       = session,
                        alreadyJoined = alreadyJoined,
                        onJoin        = { onJoin(session) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionNotificationCard(
    session      : AttendanceSession,
    alreadyJoined: Boolean,
    onJoin       : () -> Unit
) {
    val colors = LocalAppColors.current
    val minsLeft = ((session.expiresAt - System.currentTimeMillis()) / 60_000L)
        .coerceAtLeast(0)
    val isUrgent = minsLeft <= 3

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (alreadyJoined) EmeraldGreen.copy(0.08f) else colors.surface
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (alreadyJoined) EmeraldGreen.copy(0.3f)
            else if (isUrgent) ErrorRed.copy(0.3f)
            else QrAmber.copy(0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(QrAmber.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.HowToReg, null,
                        tint = QrAmber, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(session.courseName, color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                    Text("${session.courseCode} · ${session.timeSlot}",
                        color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(20.dp),
                    color = if (isUrgent) ErrorRed.copy(0.12f) else QrAmber.copy(0.12f)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null,
                            tint = if (isUrgent) ErrorRed else QrAmber,
                            modifier = Modifier.size(12.dp))
                        Text(stringResource(R.string.qr_time_remaining, minsLeft),
                            color = if (isUrgent) ErrorRed else QrAmber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
                Surface(shape = RoundedCornerShape(20.dp),
                    color = colors.surface2) {
                    Text(stringResource(R.string.qr_students_count, session.presentStudents.size),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                }
                Spacer(Modifier.weight(1f))
                if (alreadyJoined) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.qr_saved_badge), color = EmeraldGreen,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Button(
                        onClick = onJoin,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.qr_join_attendance), fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun BleCheckPane(
    session  : AttendanceSession?,
    secsLeft : Long,
    onFallback: () -> Unit
) {
    val colors = LocalAppColors.current
    val pulse by rememberInfiniteTransition(label = "ble_pulse").animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp * pulse).clip(CircleShape)
                .background(QrPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape)
                    .background(QrPurple.copy(0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bluetooth, null,
                    tint = QrPurple, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.qr_ble_verifying),
            color = colors.textPrimary, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.qr_ble_searching, secsLeft),
            color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center)
        session?.let {
            Spacer(Modifier.height(8.dp))
            Text(it.courseName, color = QrPurple,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(32.dp))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
            color = QrPurple, trackColor = QrPurple.copy(0.15f)
        )
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onFallback) {
            Text(stringResource(R.string.qr_ble_fallback),
                color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun QrFallbackPane(session: AttendanceSession?, onScan: () -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(90.dp).clip(CircleShape)
                .background(EmeraldGreen.copy(0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.QrCodeScanner, null,
                tint = EmeraldGreen, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.qr_scan_title),
            color = colors.textPrimary, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.qr_scan_subtitle),
            color = colors.textSecondary, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium)
        session?.let {
            Spacer(Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = EmeraldGreen.copy(0.1f)) {
                Text("  ${it.courseName}  ",
                    color = EmeraldGreen, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onScan,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.qr_scan_prompt), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun JoinResultPane(result: JoinResult?, onReset: () -> Unit) {
    val colors = LocalAppColors.current
    val isSuccess = result is JoinResult.Success || result is JoinResult.AlreadyMarked

    val icon  = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error
    val color = if (isSuccess) EmeraldGreen else ErrorRed
    val successStr      = stringResource(R.string.qr_result_success)
    val alreadyStr      = stringResource(R.string.qr_result_already)
    val expiredStr      = stringResource(R.string.qr_result_expired)
    val invalidStr      = stringResource(R.string.qr_result_invalid)
    val errorStr        = stringResource(R.string.qr_result_error)
    val detailSuccessFmt = stringResource(R.string.qr_detail_success)
    val detailAlreadyFmt = stringResource(R.string.qr_detail_already)
    val detailExpired   = stringResource(R.string.qr_detail_expired)
    val detailInvalid   = stringResource(R.string.qr_detail_invalid)
    val detailError     = stringResource(R.string.qr_detail_error)

    val title = when (result) {
        is JoinResult.Success       -> successStr
        is JoinResult.AlreadyMarked -> alreadyStr
        is JoinResult.Expired       -> expiredStr
        is JoinResult.InvalidQr     -> invalidStr
        is JoinResult.Error, null   -> errorStr
    }
    val subtitle = when (result) {
        is JoinResult.Success ->
            detailSuccessFmt.format(result.courseName, result.method)
        is JoinResult.AlreadyMarked ->
            detailAlreadyFmt.format(result.courseName)
        is JoinResult.Expired   -> detailExpired
        is JoinResult.InvalidQr -> detailInvalid
        else -> detailError
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(90.dp).clip(CircleShape)
                .background(color.copy(0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, color = colors.textPrimary, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.qr_ok_btn), fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// BLE YARDIMCI FONKSİYONLAR
// ─────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
private fun startBleAdvertising(
    context     : android.content.Context,
    sessionCode : String,
    onReady     : (BluetoothLeAdvertiser, AdvertiseCallback) -> Unit
): String? {
    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return context.getString(R.string.ble_not_supported)
    if (!adapter.isEnabled) return context.getString(R.string.ble_disabled)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)
        != PackageManager.PERMISSION_GRANTED) return context.getString(R.string.ble_advertise_perm)

    val advertiser = adapter.bluetoothLeAdvertiser ?: return context.getString(R.string.ble_not_supported)

    val settings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .setConnectable(false)
        .setTimeout(0)
        .build()

    val data = AdvertiseData.Builder()
        .addServiceUuid(APP_BLE_UUID)
        .addServiceData(APP_BLE_UUID, sessionCode.take(8).padEnd(8, '0').toByteArray())
        .setIncludeDeviceName(false)
        .build()

    val callback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {}
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {}
    }
    return try {
        advertiser.startAdvertising(settings, data, callback)
        onReady(advertiser, callback)
        null
    } catch (e: Exception) { context.getString(R.string.ble_start_failed, e.message ?: "") }
}

@SuppressLint("MissingPermission")
private fun stopBleAdvertising(advertiser: BluetoothLeAdvertiser?, callback: AdvertiseCallback?) {
    try { advertiser?.stopAdvertising(callback) } catch (_: Exception) {}
}

@SuppressLint("MissingPermission")
private suspend fun scanBleForSession(btAdapter: BluetoothAdapter, sessionCode: String): Boolean {
    var found = false
    val scanner = btAdapter.bluetoothLeScanner ?: return false
    val cb = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val data = result.scanRecord?.getServiceData(APP_BLE_UUID) ?: return
            if (String(data).startsWith(sessionCode.take(8)) && result.rssi > BLE_MIN_RSSI)
                found = true
        }
    }
    val filters  = listOf(ScanFilter.Builder().setServiceUuid(APP_BLE_UUID).build())
    val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    return try {
        scanner.startScan(filters, settings, cb)
        delay(BLE_SCAN_TIMEOUT_MS)
        scanner.stopScan(cb)
        found
    } catch (_: Exception) { false }
}

// ─────────────────────────────────────────────────────────────
// QR BİTMAP ÜRETİCİ
// ─────────────────────────────────────────────────────────────

private fun generateQrBitmap(content: String, size: Int = 640): Bitmap {
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
    hints[EncodeHintType.MARGIN] = 1
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size)
        for (y in 0 until size)
            bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK
                                else android.graphics.Color.WHITE)
    return bmp
}

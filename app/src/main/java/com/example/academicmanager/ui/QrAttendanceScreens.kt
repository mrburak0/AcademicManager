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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.academicmanager.data.AttendanceSession
import com.example.academicmanager.data.ScheduleEntry
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.AdminViewModel
import com.example.academicmanager.ui.viewmodels.AttendanceViewModel
import com.example.academicmanager.ui.viewmodels.AuthViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.EnumMap
import java.util.UUID

// ─────────────────────────────────────────────────────────────
// SABIT: Bu uygulama için BLE servis UUID'si
// ─────────────────────────────────────────────────────────────
private val APP_BLE_UUID = ParcelUuid.fromString("0000ACAD-0000-1000-8000-00805f9b34fb")

// ─────────────────────────────────────────────────────────────
// YARDIMCI: QR bitmap üret
// ─────────────────────────────────────────────────────────────
private fun generateQrBitmap(content: String, size: Int = 600): Bitmap {
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
    hints[EncodeHintType.MARGIN] = 1
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size)
        bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    return bmp
}

// ─────────────────────────────────────────────────────────────
// HOCA: QR Yoklama Oturumu Ekranı
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturerQrSessionScreen(
    authViewModel    : AuthViewModel,
    adminViewModel   : AdminViewModel,
    attendanceViewModel: AttendanceViewModel,
    navController    : NavController
) {
    val user       = authViewModel.currentUser ?: return
    val allEntries by adminViewModel.scheduleEntries.collectAsState()
    val allCourses by adminViewModel.courses.collectAsState()
    val colors     = LocalAppColors.current
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()

    // Hocanın dersleri
    val myCourses = remember(allEntries, user.fullName) {
        allEntries.filter { it.lecturerName == user.fullName }.distinctBy { it.courseCode }
    }

    var selectedEntry    by remember { mutableStateOf<ScheduleEntry?>(null) }
    var activeSession    by remember { mutableStateOf<AttendanceSession?>(null) }
    var qrBitmap         by remember { mutableStateOf<Bitmap?>(null) }
    var remainingSecs    by remember { mutableStateOf(0L) }
    var bleAdvertising   by remember { mutableStateOf(false) }
    var bleError         by remember { mutableStateOf<String?>(null) }
    var isCreating       by remember { mutableStateOf(false) }

    // BLE advertiser referansı
    var bleAdvertiser by remember { mutableStateOf<BluetoothLeAdvertiser?>(null) }
    var bleCallback   by remember { mutableStateOf<AdvertiseCallback?>(null) }

    // Oturum oluşturulunca
    LaunchedEffect(Unit) {
        attendanceViewModel.sessionResult.collect { session ->
            if (session != null) {
                activeSession = session
                qrBitmap = generateQrBitmap(session.sessionCode)
                remainingSecs = (session.expiresAt - System.currentTimeMillis()) / 1000
                isCreating = false
            } else {
                isCreating = false
                Toast.makeText(context, "Oturum oluşturulamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Geri sayım
    LaunchedEffect(activeSession) {
        if (activeSession != null) {
            while (remainingSecs > 0) {
                delay(1000)
                remainingSecs--
            }
            // Süre doldu — oturumu kapat
            activeSession?.let {
                attendanceViewModel.endSession(it)
                stopBleAdvertising(bleAdvertiser, bleCallback)
                bleAdvertising = false
                activeSession = null
                qrBitmap = null
                Toast.makeText(context, "Yoklama oturumu sona erdi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Ekrandan çıkınca oturumu kapat
    DisposableEffect(Unit) {
        onDispose {
            activeSession?.let { attendanceViewModel.endSession(it) }
            stopBleAdvertising(bleAdvertiser, bleCallback)
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("QR Yoklama", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = EmeraldGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (activeSession == null) {
                // ── Ders Seçimi ─────────────────────────────────
                Text("Yoklama almak istediğiniz dersi seçin:",
                    color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                if (myCourses.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = colors.surface), modifier = Modifier.fillMaxWidth()) {
                        Text("Atanmış ders bulunamadı.", color = colors.textSecondary,
                            modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    myCourses.forEach { entry ->
                        val isSelected = selectedEntry?.courseCode == entry.courseCode
                        Card(
                            onClick = { selectedEntry = entry },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) EmeraldGreen.copy(alpha = 0.15f) else colors.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen) else null
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Book, null, tint = if (isSelected) EmeraldGreen else colors.textSecondary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(entry.courseName, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text("${entry.courseCode} · ${entry.dayOfWeek} · ${entry.timeSlot}", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                                if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        val entry = selectedEntry ?: return@Button
                        isCreating = true
                        attendanceViewModel.createQrSession(
                            courseCode       = entry.courseCode,
                            courseName       = entry.courseName,
                            lecturerUsername = user.username,
                            lecturerName     = user.fullName,
                            department       = allCourses.find { it.courseCode == entry.courseCode }?.department ?: user.department,
                            dayOfWeek        = entry.dayOfWeek,
                            timeSlot         = entry.timeSlot,
                            sessionType      = entry.sessionType
                        )
                    },
                    enabled = selectedEntry != null && !isCreating,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isCreating) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Default.QrCode, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("QR Oturum Başlat", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // ── Aktif Oturum — QR Göster ──────────────────
                val mins = remainingSecs / 60
                val secs = remainingSecs % 60

                Text(activeSession!!.courseName,
                    color = EmeraldGreen, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text("Kod: ${activeSession!!.sessionCode}",
                    color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))

                // Geri sayım
                Card(colors = CardDefaults.cardColors(containerColor = if (remainingSecs < 60) ErrorRed.copy(0.15f) else EmeraldGreen.copy(0.1f)),
                    shape = RoundedCornerShape(12.dp)) {
                    Text("${mins}:${secs.toString().padStart(2,'0')} kaldı",
                        color = if (remainingSecs < 60) ErrorRed else EmeraldGreen,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }
                Spacer(Modifier.height(16.dp))

                // QR
                qrBitmap?.let { bmp ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                        Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR Kodu",
                            modifier = Modifier.size(280.dp).padding(12.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Katılan öğrenci sayısı
                val presentCount = activeSession!!.presentStudents.size
                Card(colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("$presentCount öğrenci katıldı", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // BLE buton
                OutlinedButton(
                    onClick = {
                        if (bleAdvertising) {
                            stopBleAdvertising(bleAdvertiser, bleCallback)
                            bleAdvertising = false
                        } else {
                            val code = activeSession!!.sessionCode
                            val result = startBleAdvertising(context, code) { advertiser, cb ->
                                bleAdvertiser = advertiser; bleCallback = cb
                            }
                            if (result == null) {
                                bleAdvertising = true
                                bleError = null
                            } else {
                                bleError = result
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (bleAdvertising) EmeraldGreen else colors.textSecondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (bleAdvertising) EmeraldGreen else colors.border)
                ) {
                    Icon(Icons.Default.Bluetooth, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (bleAdvertising) "Bluetooth Aktif (Kapat)" else "Bluetooth Aktif Et")
                }
                bleError?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp)) }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        activeSession?.let { attendanceViewModel.endSession(it) }
                        stopBleAdvertising(bleAdvertiser, bleCallback)
                        bleAdvertising = false
                        activeSession = null
                        qrBitmap = null
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yoklamayı Bitir", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ÖĞRENCİ: QR Tarama Ekranı
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StudentQrScanScreen(
    authViewModel      : AuthViewModel,
    adminViewModel     : AdminViewModel,
    attendanceViewModel: AttendanceViewModel,
    navController      : NavController
) {
    val user      = authViewModel.currentUser ?: return
    val allStudents by adminViewModel.students.collectAsState()
    val colors    = LocalAppColors.current
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()

    var status         by remember { mutableStateOf<String?>(null) }
    var isSuccess      by remember { mutableStateOf(false) }
    var isChecking     by remember { mutableStateOf(false) }
    var bleCheckActive by remember { mutableStateOf(false) }
    var pendingCode    by remember { mutableStateOf<String?>(null) }

    // QR tarama sonucu
    LaunchedEffect(Unit) {
        attendanceViewModel.qrJoinResult.collect { result ->
            isChecking = false
            bleCheckActive = false
            pendingCode = null
            when {
                result.startsWith("SUCCESS:") -> {
                    isSuccess = true
                    status = "✓ Yoklamanız alındı\n${result.removePrefix("SUCCESS:")}"
                }
                result == "INVALID"       -> { isSuccess = false; status = "Geçersiz veya süresi dolmuş QR kodu." }
                result == "ALREADY_MARKED"-> { isSuccess = true;  status = "Yoklamanız zaten kaydedilmiş." }
                else                      -> { isSuccess = false; status = "Bir hata oluştu. Tekrar deneyin." }
            }
        }
    }

    // ZXing QR scanner launcher
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val code = result.contents
        if (code != null) {
            pendingCode = code
            bleCheckActive = true
        }
    }

    // BLE yakınlık doğrulaması — izin kontrolü yapıldıktan sonra çağrılır
    LaunchedEffect(bleCheckActive, pendingCode) {
        if (bleCheckActive && pendingCode != null) {
            val code = pendingCode!!
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            val hasBle = btAdapter != null && context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            val blePermOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED

            if (hasBle && btAdapter!!.isEnabled && blePermOk) {
                val foundNearby = scanBleForSession(btAdapter, code)
                if (foundNearby) {
                    isChecking = true
                    attendanceViewModel.joinSessionByCode(code, user.username, user.fullName, allStudents)
                } else {
                    bleCheckActive = false
                    pendingCode = null
                    status = "Sınıf dışındasınız. Bluetooth ile hocanın cihazı bulunamadı."
                    isSuccess = false
                }
            } else {
                // BLE desteklenmiyor veya kapalı — sadece QR doğrulama yap
                isChecking = true
                attendanceViewModel.joinSessionByCode(code, user.username, user.fullName, allStudents)
            }
        }
    }

    // Kamera ve bluetooth izin kontrolü
    val cameraPermissionState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.CAMERA)
    )

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("QR ile Yoklama", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = EmeraldGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.QrCodeScanner, null, tint = EmeraldGreen, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text("QR ile Yoklama", color = colors.textPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Hocanın tahtada gösterdiği QR kodu tarayın.\nBluetooth açıksa sınıf içinde olduğunuz da doğrulanır.",
                color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))

            status?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccess) EmeraldGreen.copy(0.12f) else ErrorRed.copy(0.12f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            null, tint = if (isSuccess) EmeraldGreen else ErrorRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(msg, color = if (isSuccess) EmeraldGreen else ErrorRed,
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            when {
                bleCheckActive -> {
                    CircularProgressIndicator(color = EmeraldGreen)
                    Spacer(Modifier.height(8.dp))
                    Text("Bluetooth ile konum doğrulanıyor...", color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall)
                }
                isChecking -> {
                    CircularProgressIndicator(color = EmeraldGreen)
                    Spacer(Modifier.height(8.dp))
                    Text("Yoklama kaydediliyor...", color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    Button(
                        onClick = {
                            if (cameraPermissionState.allPermissionsGranted) {
                                status = null
                                val opts = ScanOptions().apply {
                                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    setPrompt("QR kodu çerçeveye getirin")
                                    setBeepEnabled(true)
                                    setOrientationLocked(false)
                                }
                                scanLauncher.launch(opts)
                            } else {
                                cameraPermissionState.launchMultiplePermissionRequest()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("QR Kodu Tara", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// YARDIMCILAR: BLE Advertise Başlat/Durdur
// ─────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
private fun startBleAdvertising(
    context    : android.content.Context,
    sessionCode: String,
    onReady    : (BluetoothLeAdvertiser, AdvertiseCallback) -> Unit
): String? {
    val btAdapter = BluetoothAdapter.getDefaultAdapter() ?: return "Cihaz Bluetooth desteklemiyor"
    if (!btAdapter.isEnabled) return "Bluetooth kapalı. Lütfen açın."

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
        return "Bluetooth Advertise izni gerekli"
    }

    val advertiser = btAdapter.bluetoothLeAdvertiser ?: return "Bu cihaz BLE yayını desteklemiyor"

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
        override fun onStartFailure(errorCode: Int) { }
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) { }
    }

    return try {
        advertiser.startAdvertising(settings, data, callback)
        onReady(advertiser, callback)
        null
    } catch (e: Exception) {
        "BLE başlatılamadı: ${e.message}"
    }
}

@SuppressLint("MissingPermission")
private fun stopBleAdvertising(advertiser: BluetoothLeAdvertiser?, callback: AdvertiseCallback?) {
    try { advertiser?.stopAdvertising(callback) } catch (_: Exception) {}
}

@SuppressLint("MissingPermission")
private suspend fun scanBleForSession(btAdapter: BluetoothAdapter, sessionCode: String): Boolean {
    var foundNearby = false
    val scanner = btAdapter.bluetoothLeScanner ?: return false
    val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val data = result.scanRecord?.getServiceData(APP_BLE_UUID)
            if (data != null && String(data).startsWith(sessionCode.take(8))) {
                if (result.rssi > -85) foundNearby = true
            }
        }
    }
    val filters  = listOf(ScanFilter.Builder().setServiceUuid(APP_BLE_UUID).build())
    val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    try {
        scanner.startScan(filters, settings, scanCallback)
        delay(5000)
        scanner.stopScan(scanCallback)
    } catch (_: Exception) {}
    return foundNearby
}

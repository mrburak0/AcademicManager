package com.example.academicmanager.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import com.example.academicmanager.R
import com.example.academicmanager.data.*
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.AdminViewModel
import com.example.academicmanager.ui.viewmodels.DataImportViewModel
import com.example.academicmanager.ui.viewmodels.ImportState
import com.example.academicmanager.ui.viewmodels.ImportType

// ─────────────────────────────────────────────────────────────
// IMPORT IDLE SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun ImportIdleScreen(
    onDownloadCourses: () -> Unit,
    onImportCourses: () -> Unit,
    onDownloadLecturers: () -> Unit,
    onImportLecturers: () -> Unit,
    errorMessage: String? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Page header ──────────────────────────────────────
        item {
            Column {
                Text(
                    stringResource(R.string.import_data_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColorState.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.import_data_subtitle),
                    color = AppColorState.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // ── Workflow guide ───────────────────────────────────
        item { WorkflowStepsCard() }

        // ── Error banner (if any) ────────────────────────────
        if (errorMessage != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        errorMessage,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // ── Courses import card ──────────────────────────────
        item {
            ImportTypeCard(
                title = stringResource(R.string.tab_courses_name),
                subtitle = stringResource(R.string.tab_courses_sub),
                accentColor = IndigoAccent,
                expectedColumns = listOf("Course Code", "Course Name", "Department"),
                note = null,
                icon = Icons.AutoMirrored.Filled.List,
                onDownload = onDownloadCourses,
                onImport = onImportCourses
            )
        }

        // ── Lecturers import card ────────────────────────────
        item {
            ImportTypeCard(
                title = stringResource(R.string.tab_lecturers_name),
                subtitle = stringResource(R.string.tab_lecturers_sub),
                accentColor = EmeraldGreen,
                expectedColumns = listOf("Name", "Title", "Working Type", "Department"),
                note = stringResource(R.string.lecturer_creds_auto),
                icon = Icons.Default.Person,
                onDownload = onDownloadLecturers,
                onImport = onImportLecturers
            )
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun WorkflowStepsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColorState.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.how_it_works),
                color = AppColorState.textSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepBubble(number = "1", label = stringResource(R.string.step_download), color = EmeraldGreen)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(AppColorState.textSecondary.copy(alpha = 0.2f)))
                StepBubble(number = "2", label = stringResource(R.string.step_fill), color = IndigoAccent)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(AppColorState.textSecondary.copy(alpha = 0.2f)))
                StepBubble(number = "3", label = stringResource(R.string.step_import), color = Color(0xFFF59E0B))
                Box(modifier = Modifier.weight(1f).height(1.dp).background(AppColorState.textSecondary.copy(alpha = 0.2f)))
                StepBubble(number = "4", label = stringResource(R.string.step_save), color = Color(0xFF8B5CF6))
            }
        }
    }
}

@Composable
private fun StepBubble(number: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                color = color,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = AppColorState.textSecondary,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun ImportTypeCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    expectedColumns: List<String>,
    note: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.AutoMirrored.Filled.List,
    onDownload: () -> Unit,
    onImport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColorState.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Title row ────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        color = AppColorState.textPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        subtitle,
                        color = AppColorState.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = AppColorState.textSecondary.copy(alpha = 0.08f))
            Spacer(Modifier.height(14.dp))

            // ── Expected columns ──────────────────────────────
            Text(
                stringResource(R.string.excel_columns_label),
                color = AppColorState.textSecondary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                expectedColumns.forEachIndexed { i, col ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${i + 1}",
                            color = accentColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            col,
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Optional note ─────────────────────────────────
            if (note != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.06f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp).padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        note,
                        color = AppColorState.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Action buttons ────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.btn_template),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = onImport,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.btn_import_xlsx),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// LOADING SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun ImportLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                color = EmeraldGreen,
                strokeWidth = 3.dp,
                modifier = Modifier.size(52.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.importing_excel),
                    color = AppColorState.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.please_wait),
                    color = AppColorState.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PREVIEW SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun ImportPreviewScreen(
    state: ImportState.PreviewReady,
    onDiscard: () -> Unit,
    onSave: () -> Unit
) {
    val isLecturers = state.type == ImportType.LECTURERS
    val accentColor = if (isLecturers) EmeraldGreen else IndigoAccent
    val typeLabel = if (isLecturers) stringResource(R.string.tab_lecturers_name) else stringResource(R.string.tab_courses_name)

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.preview_verify, typeLabel),
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColorState.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(10.dp))
                Badge(containerColor = accentColor.copy(alpha = 0.2f)) {
                    Text(
                        stringResource(R.string.preview_rows, state.items.size),
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.preview_review_msg),
                color = AppColorState.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        HorizontalDivider(color = AppColorState.textSecondary.copy(alpha = 0.08f))

        // ── Item list ─────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(state.items) { index, item ->
                when (item) {
                    is Lecturer    -> LecturerPreviewCard(index + 1, item, accentColor)
                    is Course      -> CoursePreviewCard(index + 1, item, accentColor)
                    is LecturerEntity -> SimplePreviewRow(index + 1, item.fullName, accentColor)
                    is CourseEntity   -> SimplePreviewRow(index + 1, "${item.courseCode} – ${item.courseName}", accentColor)
                    else -> Unit
                }
            }
        }

        // ── Bottom actions ────────────────────────────────────
        HorizontalDivider(color = AppColorState.textSecondary.copy(alpha = 0.08f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.btn_discard), fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.btn_save_count, state.items.size, typeLabel),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LecturerPreviewCard(index: Int, lecturer: Lecturer, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColorState.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Row number
            Text(
                "$index",
                color = AppColorState.textSecondary.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(20.dp)
            )
            // Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    lecturer.fullName.firstOrNull()?.uppercase() ?: "?",
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Full name + title
                Text(
                    buildString {
                        if (lecturer.title.isNotBlank()) append("${lecturer.title} ")
                        append(lecturer.fullName)
                    },
                    color = AppColorState.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (lecturer.workingType.isNotBlank()) {
                    Text(
                        lecturer.workingType,
                        color = AppColorState.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(6.dp))
                // Credential chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CredentialChip(
                        prefix = "@",
                        value = lecturer.username,
                        chipColor = IndigoAccent
                    )
                    CredentialChip(
                        prefix = "pw ",
                        value = lecturer.password,
                        chipColor = Color(0xFFF59E0B),
                        monospace = true
                    )
                }
            }
        }
    }
}

@Composable
private fun CredentialChip(
    prefix: String,
    value: String,
    chipColor: Color,
    monospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(chipColor.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            prefix,
            color = chipColor.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            color = chipColor,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (monospace) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CoursePreviewCard(index: Int, course: Course, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColorState.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Row number
            Text(
                "$index",
                color = AppColorState.textSecondary.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(20.dp)
            )
            // Code badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    course.courseCode,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    course.courseName,
                    color = AppColorState.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (course.department.isNotBlank() && course.department != "General") {
                    Text(
                        course.department,
                        color = accentColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SimplePreviewRow(index: Int, text: String, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColorState.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$index",
                color = AppColorState.textSecondary.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(24.dp)
            )
            Text(text, color = AppColorState.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CREDENTIAL SHEET SCREEN — import sonrası şifre listesi
// ─────────────────────────────────────────────────────────────

@Composable
fun CredentialSheetScreen(
    credentials: List<Pair<String, String>>,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    fun copyAll() {
        val text = credentials.joinToString("\n") { (u, p) -> "$u  →  $p" }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("credentials", text))
        Toast.makeText(context, context.getString(R.string.copied_all), Toast.LENGTH_SHORT).show()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EmeraldGreen.copy(alpha = 0.08f))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        stringResource(R.string.cred_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.cred_subtitle, credentials.size),
                        color = AppColorState.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.cred_warning),
                    color = Color(0xFFF59E0B),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        HorizontalDivider(color = AppColorState.textSecondary.copy(alpha = 0.08f))

        // ── Credential List ───────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(credentials) { index, (username, password) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColorState.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sıra numarası
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                color = EmeraldGreen,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            // Kullanıcı adı
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    username,
                                    color = IndigoAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            // Şifre
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    password,
                                    color = Color(0xFFF59E0B),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // Tek satır kopyala
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("cred", "$username  →  $password"))
                                Toast.makeText(context, context.getString(R.string.copied_one, username), Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy), tint = AppColorState.textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // ── Bottom actions ─────────────────────────────────────
        HorizontalDivider(color = AppColorState.textSecondary.copy(alpha = 0.08f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = ::copyAll,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_copy_all), fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_got_it), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB COMPOSABLES — called from DataScreen in MainScreen.kt
// ─────────────────────────────────────────────────────────────

@Composable
fun CourseImportTab(
    importVM: DataImportViewModel,
    context: Context,
    onLaunch: (ImportType) -> Unit,
    errorMsg: String?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(stringResource(R.string.course_import_title), style = MaterialTheme.typography.titleMedium, color = AppColorState.textPrimary, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.course_import_subtitle), color = AppColorState.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (errorMsg != null) {
            item { ErrorBanner(errorMsg) }
        }
        item { WorkflowStepsCard() }
        item {
            ImportTypeCard(
                title = stringResource(R.string.tab_courses_name),
                subtitle = stringResource(R.string.tab_courses_sub),
                accentColor = IndigoAccent,
                expectedColumns = listOf("Course Code", "Course Name", "Department"),
                note = null,
                icon = Icons.AutoMirrored.Filled.List,
                onDownload = { importVM.downloadTemplate(context, ImportType.COURSES) },
                onImport   = { onLaunch(ImportType.COURSES) }
            )
        }
    }
}

@Composable
fun LecturerImportTab(
    importVM: DataImportViewModel,
    context: Context,
    onLaunch: (ImportType) -> Unit,
    errorMsg: String?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(stringResource(R.string.lecturer_import_title), style = MaterialTheme.typography.titleMedium, color = AppColorState.textPrimary, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.lecturer_import_subtitle), color = AppColorState.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (errorMsg != null) {
            item { ErrorBanner(errorMsg) }
        }
        item { WorkflowStepsCard() }
        item {
            ImportTypeCard(
                title = stringResource(R.string.tab_lecturers_name),
                subtitle = stringResource(R.string.tab_lecturers_sub),
                accentColor = EmeraldGreen,
                expectedColumns = listOf("Name", "Title", "Working Type", "Department"),
                note = stringResource(R.string.lecturer_creds_auto),
                icon = Icons.Default.Person,
                onDownload = { importVM.downloadTemplate(context, ImportType.LECTURERS) },
                onImport   = { onLaunch(ImportType.LECTURERS) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomDataTab(
    importVM: DataImportViewModel,
    adminVM: AdminViewModel,
    context: Context,
    onLaunch: (ImportType) -> Unit,
    classrooms: List<Classroom>,
    scheduleEntries: List<ScheduleEntry>
) {
    val accentColor = Color(0xFFF59E0B)

    // Manuel giriş state
    var roomName     by remember { mutableStateOf("") }
    var capacityText by remember { mutableStateOf("") }
    var roomType     by remember { mutableStateOf(ClassroomType.LECTURE) }
    var typeExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Başlık
        item {
            Column {
                Text(stringResource(R.string.classroom_mgmt_title), style = MaterialTheme.typography.titleMedium, color = AppColorState.textPrimary, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.classroom_mgmt_subtitle), color = AppColorState.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Manuel giriş formu
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = AppColorState.surface),
                shape    = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Add, null, tint = accentColor, modifier = Modifier.size(18.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.manual_classroom_add_btn), color = AppColorState.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }

                    // Sınıf adı
                    OutlinedTextField(
                        value = roomName,
                        onValueChange = { roomName = it },
                        label = { Text(stringResource(R.string.classroom_name_ex)) },
                        placeholder = { Text(stringResource(R.string.enter_text_hint), color = AppColorState.textSecondary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor, unfocusedBorderColor = AppColorState.surface2,
                            focusedLabelColor = accentColor, unfocusedLabelColor = AppColorState.textSecondary,
                            focusedTextColor = AppColorState.textPrimary, unfocusedTextColor = AppColorState.textPrimary,
                            cursorColor = accentColor, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                        )
                    )

                    // Kapasite (yalnızca sayı)
                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = { capacityText = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text(stringResource(R.string.capacity_ex)) },
                        placeholder = { Text(stringResource(R.string.enter_number_hint), color = AppColorState.textSecondary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor, unfocusedBorderColor = AppColorState.surface2,
                            focusedLabelColor = accentColor, unfocusedLabelColor = AppColorState.textSecondary,
                            focusedTextColor = AppColorState.textPrimary, unfocusedTextColor = AppColorState.textPrimary,
                            cursorColor = accentColor, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                        )
                    )

                    // Tip dropdown
                    ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                        OutlinedTextField(
                            value = ClassroomType.displayName(roomType),
                            onValueChange = {}, readOnly = true,
                            label = { Text(stringResource(R.string.classroom_type_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor, unfocusedBorderColor = AppColorState.surface2,
                                focusedLabelColor = accentColor, unfocusedLabelColor = AppColorState.textSecondary,
                                focusedTextColor = AppColorState.textPrimary, unfocusedTextColor = AppColorState.textPrimary,
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                            )
                        )
                        ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }, modifier = Modifier.background(AppColorState.surface)) {
                            ClassroomType.all.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(ClassroomType.displayName(t), color = AppColorState.textPrimary) },
                                    onClick = { roomType = t; typeExpanded = false }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val cap = capacityText.toIntOrNull() ?: 0
                            when {
                                roomName.isBlank() -> Toast.makeText(context, context.getString(R.string.room_name_empty), Toast.LENGTH_SHORT).show()
                                cap < 1 || cap > 2000 -> Toast.makeText(context, context.getString(R.string.capacity_range_error), Toast.LENGTH_SHORT).show()
                                else -> {
                                    adminVM.addClassroom(roomName.trim(), cap, roomType)
                                    Toast.makeText(context, context.getString(R.string.classroom_added_toast), Toast.LENGTH_SHORT).show()
                                    roomName = ""; capacityText = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_save_room), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Excel import kartı
        item {
            ImportTypeCard(
                title = stringResource(R.string.classrooms_excel_title),
                subtitle = stringResource(R.string.classrooms_excel_subtitle),
                accentColor = accentColor,
                expectedColumns = listOf("Name", "Capacity", "Type"),
                note = stringResource(R.string.classroom_type_note),
                icon = Icons.Default.MeetingRoom,
                onDownload = { importVM.downloadClassroomTemplate(context) },
                onImport   = { onLaunch(ImportType.CLASSROOMS) }
            )
        }

        // Mevcut sınıf listesi
        if (classrooms.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.registered_classrooms, classrooms.size),
                    color = accentColor, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(classrooms) { classroom ->
                val bookingCount = scheduleEntries.count { it.classroomName == classroom.name }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColorState.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MeetingRoom, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(classroom.name, color = AppColorState.textPrimary, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.clip(RoundedCornerShape(5.dp)).background(accentColor.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(ClassroomType.displayName(classroom.classroomType), color = accentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(stringResource(R.string.capacity_bookings, classroom.capacity, bookingCount), color = AppColorState.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        var showDel by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDel = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, null, tint = ErrorRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        }
                        if (showDel) {
                            AlertDialog(
                                onDismissRequest = { showDel = false },
                                containerColor = AppColorState.surface,
                                title = { Text(stringResource(R.string.delete_classroom_title), color = ErrorRed) },
                                text  = { Text(stringResource(R.string.delete_classroom_msg, classroom.name), color = AppColorState.textPrimary) },
                                confirmButton = {
                                    Button(onClick = { adminVM.deleteClassroom(classroom.id); showDel = false }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) { Text(stringResource(R.string.delete)) }
                                },
                                dismissButton = { TextButton(onClick = { showDel = false }) { Text(stringResource(R.string.cancel), color = AppColorState.textSecondary) } }
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(top = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.MeetingRoom, null, tint = AppColorState.textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                        Text(stringResource(R.string.no_classrooms), color = AppColorState.textSecondary)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ErrorRed.copy(alpha = 0.1f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Close, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
    }
}

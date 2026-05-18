package com.example.academicmanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import com.example.academicmanager.data.Announcement
import com.example.academicmanager.data.AnnouncementType
import com.example.academicmanager.data.UserRole
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.AnnouncementsViewModel
import com.example.academicmanager.ui.viewmodels.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Announcement type color mapping
private fun announcementColor(type: String): Color = when (type) {
    AnnouncementType.CANCELLED       -> Color(0xFFEF4444)
    AnnouncementType.WARNING         -> Color(0xFFF59E0B)
    AnnouncementType.SCHEDULE_CHANGE -> Color(0xFF6366F1)
    else                             -> Color(0xFF10B981)
}

private fun announcementIcon(type: String) = when (type) {
    AnnouncementType.CANCELLED       -> Icons.Default.Cancel
    AnnouncementType.WARNING         -> Icons.Default.Warning
    AnnouncementType.SCHEDULE_CHANGE -> Icons.Default.Edit
    else                             -> Icons.Default.Info
}

// ─────────────────────────────────────────────────────────────
// ANNOUNCEMENTS SCREEN
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    announcementsViewModel: AnnouncementsViewModel,
    authViewModel: AuthViewModel,
    navController: NavController? = null
) {
    val announcements by announcementsViewModel.announcements.collectAsState()
    val currentUser   = authViewModel.currentUser
    val isAdmin       = currentUser?.role == UserRole.ADMIN

    var showAddDialog by remember { mutableStateOf(false) }
    var dialogTitle   by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var dialogType    by remember { mutableStateOf(AnnouncementType.INFO) }
    var typeExpanded  by remember { mutableStateOf(false) }

    val sorted = announcements.sortedByDescending { it.timestamp }

    // Localized display name helper (composable context)
    val typeDisplayName: @Composable (String) -> String = { type ->
        when (type) {
            AnnouncementType.INFO            -> stringResource(R.string.ann_type_info)
            AnnouncementType.WARNING         -> stringResource(R.string.ann_type_warning)
            AnnouncementType.CANCELLED       -> stringResource(R.string.ann_type_cancelled)
            AnnouncementType.SCHEDULE_CHANGE -> stringResource(R.string.ann_type_schedule_change)
            else                             -> stringResource(R.string.ann_type_info)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.announcements_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            if (sorted.size == 1)
                                stringResource(R.string.announcements_count_one, sorted.size)
                            else
                                stringResource(R.string.announcements_count, sorted.size),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    if (navController != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = EmeraldGreen
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick   = { showAddDialog = true },
                    containerColor = EmeraldGreen,
                    contentColor   = Color.White,
                    shape          = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_announcement_title))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Başlık artık TopAppBar'da — boş bırak
                Spacer(Modifier.height(4.dp))
            }

            if (sorted.isEmpty()) {
                item {
                    Box(
                        modifier          = Modifier.fillMaxWidth().padding(top = 80.dp),
                        contentAlignment  = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint     = TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.no_announcements), color = TextSecondary)
                        }
                    }
                }
            } else {
                items(sorted, key = { it.id }) { announcement ->
                    AnnouncementCard(
                        announcement    = announcement,
                        isAdmin         = isAdmin,
                        typeDisplayName = typeDisplayName(announcement.type),
                        onDelete        = { announcementsViewModel.deleteAnnouncement(announcement.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // ── Add Announcement Dialog (Admin) ───────────────────────
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog  = false
                dialogTitle    = ""; dialogMessage = ""
                dialogType     = AnnouncementType.INFO
            },
            containerColor = Slate800,
            title = { Text(stringResource(R.string.add_announcement_title), color = EmeraldGreen, fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value         = dialogTitle,
                        onValueChange = { dialogTitle = it },
                        label         = { Text(stringResource(R.string.ann_title_field)) },
                        placeholder   = { Text(stringResource(R.string.ann_title_hint), color = TextSecondary.copy(alpha = 0.5f)) },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor    = EmeraldGreen, unfocusedBorderColor = Slate800,
                            focusedLabelColor     = EmeraldGreen, unfocusedLabelColor  = TextSecondary,
                            focusedTextColor      = TextPrimary,  unfocusedTextColor   = TextPrimary,
                            cursorColor           = EmeraldGreen,
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                        )
                    )
                    OutlinedTextField(
                        value         = dialogMessage,
                        onValueChange = { dialogMessage = it },
                        label         = { Text(stringResource(R.string.ann_message_field)) },
                        placeholder   = { Text(stringResource(R.string.ann_message_hint), color = TextSecondary.copy(alpha = 0.5f)) },
                        modifier      = Modifier.fillMaxWidth().height(120.dp),
                        maxLines      = 5,
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor    = EmeraldGreen, unfocusedBorderColor = Slate800,
                            focusedLabelColor     = EmeraldGreen, unfocusedLabelColor  = TextSecondary,
                            focusedTextColor      = TextPrimary,  unfocusedTextColor   = TextPrimary,
                            cursorColor           = EmeraldGreen,
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                        )
                    )
                    // Announcement type selector
                    ExposedDropdownMenuBox(
                        expanded        = typeExpanded,
                        onExpandedChange = { typeExpanded = !typeExpanded }
                    ) {
                        OutlinedTextField(
                            value         = typeDisplayName(dialogType),
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text(stringResource(R.string.ann_type_field)) },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier      = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                focusedLabelColor  = EmeraldGreen
                            )
                        )
                        ExposedDropdownMenu(
                            expanded        = typeExpanded,
                            onDismissRequest = { typeExpanded = false },
                            modifier        = Modifier.background(Slate800)
                        ) {
                            AnnouncementType.all.forEach { t ->
                                DropdownMenuItem(
                                    text    = { Text(typeDisplayName(t), color = TextPrimary) },
                                    onClick = { dialogType = t; typeExpanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dialogTitle.isNotBlank() && dialogMessage.isNotBlank()) {
                            announcementsViewModel.addAnnouncement(
                                title       = dialogTitle,
                                message     = dialogMessage,
                                type        = dialogType,
                                createdBy   = currentUser?.username ?: "admin"
                            )
                            showAddDialog = false
                            dialogTitle   = ""; dialogMessage = ""
                            dialogType    = AnnouncementType.INFO
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) { Text(stringResource(R.string.publish_btn)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    dialogTitle   = ""; dialogMessage = ""
                }) { Text(stringResource(R.string.cancel), color = TextSecondary) }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// ANNOUNCEMENT CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun AnnouncementCard(
    announcement: Announcement,
    isAdmin: Boolean,
    typeDisplayName: String,
    onDelete: () -> Unit
) {
    val color             = announcementColor(announcement.type)
    val icon              = announcementIcon(announcement.type)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateStr = if (announcement.timestamp > 0L)
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            .format(Date(announcement.timestamp))
    else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Slate800),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment          = Alignment.CenterVertically,
                    horizontalArrangement      = Arrangement.SpaceBetween,
                    modifier                   = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            typeDisplayName,
                            color      = color,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (dateStr.isNotEmpty()) {
                        Text(dateStr, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(announcement.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(announcement.message, color = TextSecondary, style = MaterialTheme.typography.bodySmall)

                if (announcement.relatedCourseCode.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.ann_course_label, announcement.relatedCourseCode),
                        color = EmeraldGreen,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Delete button — admin only
            if (isAdmin) {
                IconButton(
                    onClick  = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint     = ErrorRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor   = Slate800,
            title            = { Text(stringResource(R.string.delete_ann_title), color = ErrorRed) },
            text             = { Text(stringResource(R.string.delete_ann_msg, announcement.title), color = TextPrimary) },
            confirmButton    = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        )
    }
}

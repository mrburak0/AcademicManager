package com.example.academicmanager.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.example.academicmanager.R
import com.example.academicmanager.data.*
import com.example.academicmanager.ui.theme.*
import com.example.academicmanager.ui.viewmodels.*
import com.example.academicmanager.data.AppSettings
import com.example.academicmanager.data.SessionManager
import com.example.academicmanager.util.CredentialUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun AcademicLogo(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(EmeraldGreen.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    radius = size.minDimension / 2
                )
                
                val path = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.2f)
                    lineTo(size.width * 0.8f, size.height * 0.35f)
                    lineTo(size.width * 0.8f, size.height * 0.7f)
                    quadraticTo(size.width * 0.8f, size.height * 0.85f, size.width * 0.5f, size.height * 0.9f)
                    quadraticTo(size.width * 0.2f, size.height * 0.85f, size.width * 0.2f, size.height * 0.7f)
                    lineTo(size.width * 0.2f, size.height * 0.35f)
                    close()
                }
                drawPath(path, color = EmeraldGreen)
                
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = center.copy(y = size.height * 0.4f, x = size.width * 0.45f),
                    end = center.copy(y = size.height * 0.75f, x = size.width * 0.45f),
                    strokeWidth = 4f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = center.copy(y = size.height * 0.4f, x = size.width * 0.55f),
                    end = center.copy(y = size.height * 0.75f, x = size.width * 0.55f),
                    strokeWidth = 4f
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "ACADEMIC",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp
        )
        Text(
            text = "MANAGER",
            style = MaterialTheme.typography.labelSmall,
            color = EmeraldGreen,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp
        )
    }
}

sealed class Screen(val route: String, @androidx.annotation.StringRes val labelRes: Int, val icon: ImageVector) {
    object Login             : Screen("login",             R.string.nav_home,          Icons.Default.Lock)
    object Register          : Screen("register",          R.string.nav_home,          Icons.Default.Person)
    object ChangePassword    : Screen("change_password",   R.string.nav_settings,      Icons.Default.Lock)
    object Home              : Screen("home",              R.string.nav_home,          Icons.Default.Home)
    object Calendar          : Screen("calendar",          R.string.nav_calendar,      Icons.Default.DateRange)
    object AdminHome         : Screen("admin_home",        R.string.nav_home,          Icons.Default.Home)
    object Classrooms        : Screen("classrooms",        R.string.nav_classrooms,    Icons.Default.MeetingRoom)
    object Assignment        : Screen("assignment",        R.string.nav_schedule,      Icons.Default.Edit)
    object ScheduleCalendar  : Screen("schedule_calendar", R.string.nav_calendar,      Icons.Default.CalendarMonth)
    object LecturerHome         : Screen("lecturer_home",         R.string.nav_home,          Icons.Default.Home)
    object LecturerCalendar     : Screen("lecturer_calendar",     R.string.nav_calendar,      Icons.Default.DateRange)
    object LecturerAvailability : Screen("lecturer_availability", R.string.nav_availability,  Icons.Default.EventAvailable)
    object StudentHome          : Screen("student_home",          R.string.nav_home,          Icons.Default.School)
    object StudentCalendar      : Screen("student_calendar",      R.string.nav_calendar,      Icons.Default.DateRange)
    object Data                 : Screen("data",                  R.string.nav_import,        Icons.AutoMirrored.Filled.List)
    object Profile              : Screen("profile",               R.string.nav_settings,      Icons.Default.Settings)
    object Announcements        : Screen("announcements",         R.string.nav_announcements, Icons.Default.Notifications)
    object AdminAvailability    : Screen("admin_availability",    R.string.nav_availability,  Icons.Default.CalendarMonth)
    object AutoAssign           : Screen("auto_assign",           R.string.nav_schedule,      Icons.Default.AutoAwesome)
}

@Composable
fun MainScreen(
    appSettings: com.example.academicmanager.data.AppSettings? = null,
    currentTheme: String = com.example.academicmanager.data.AppSettings.THEME_SYSTEM,
    onThemeChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { UniversityDatabase.getDatabase(context) }
    val dao = database.universityDao()

    val repository: UniversityRepository = remember {
        UniversityRepositoryImpl(FirebaseFirestore.getInstance())
    }

    val authViewModel: AuthViewModel = viewModel(factory = ViewModelFactory(dao, repository))
    val adminViewModel: AdminViewModel = viewModel(factory = ViewModelFactory(dao, repository))
    val announcementsViewModel: AnnouncementsViewModel = viewModel(factory = ViewModelFactory(dao, repository))
    val authState by authViewModel.authState.collectAsState()

    // Restore session on app startup
    LaunchedEffect(Unit) {
        if (authState is AuthState.Idle) {
            val savedUsername = SessionManager.getUsername(context)
            if (savedUsername != null) {
                authViewModel.restoreSession(savedUsername)
            }
        }
    }

    val navController = rememberNavController()

    val currentRole = (authState as? AuthState.Authenticated)?.user?.role

    val bottomNavItems = remember(currentRole) {
        when (currentRole) {
            UserRole.ADMIN   -> listOf(Screen.AdminHome, Screen.Assignment, Screen.ScheduleCalendar, Screen.Data, Screen.Profile)
            UserRole.STUDENT -> listOf(Screen.StudentHome, Screen.StudentCalendar, Screen.Profile)
            else             -> listOf(Screen.LecturerHome, Screen.LecturerCalendar, Screen.LecturerAvailability, Screen.Profile)
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                val dest = when ((authState as AuthState.Authenticated).user.role) {
                    UserRole.ADMIN   -> Screen.AdminHome.route
                    UserRole.STUDENT -> Screen.StudentHome.route
                    else             -> Screen.LecturerHome.route
                }
                navController.navigate(dest) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.MustChangePassword -> {
                navController.navigate(Screen.ChangePassword.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.Idle -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> {}
        }
    }

    if (authState is AuthState.Authenticated) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Slate800,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(72.dp)
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (selected) EmeraldGreen else TextSecondary.copy(alpha = 0.5f)
                                )
                            },
                            label = {
                                Text(
                                    stringResource(screen.labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) EmeraldGreen else TextSecondary.copy(alpha = 0.5f),
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = EmeraldGreen.copy(alpha = 0.12f),
                                selectedIconColor = EmeraldGreen,
                                unselectedIconColor = TextSecondary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController, startDestination = Screen.Login.route, modifier = Modifier.padding(innerPadding)) {
                // ── Auth screens ──────────────────────────────────
                composable(Screen.Login.route) { LoginScreen(authViewModel, navController) }
                composable(Screen.Register.route) { RegisterScreen(authViewModel, navController) }
                // ── Admin ekranları ───────────────────────────────
                composable(Screen.AdminHome.route)          { AdminHomeScreen(adminViewModel, navController) }
                composable(Screen.Classrooms.route)         { ClassroomsScreen(adminViewModel) }
                composable(Screen.Assignment.route)         { AssignmentScreen(adminViewModel, navController) }
                composable(Screen.ScheduleCalendar.route)   { ScheduleCalendarScreen(adminViewModel, navController) }
                composable(Screen.AutoAssign.route)         { AutoAssignScreen(adminViewModel, navController) }
                composable(Screen.AdminAvailability.route)  { AdminAvailabilityScreen(adminViewModel, navController) }
                // ── Öğretim görevlisi ekranları ───────────────────
                composable(Screen.LecturerHome.route)           { LecturerHomeScreen(authViewModel, adminViewModel, navController) }
                composable(Screen.LecturerCalendar.route)       { LecturerCalendarScreen(authViewModel, adminViewModel) }
                composable(Screen.LecturerAvailability.route)   { LecturerAvailabilityScreen(authViewModel, adminViewModel) }
                // ── Öğrenci ekranları ─────────────────────────────
                composable(Screen.StudentHome.route)     { StudentHomeScreen(authViewModel, adminViewModel, navController) }
                composable(Screen.StudentCalendar.route) { StudentCalendarScreen(authViewModel, adminViewModel) }
                // ── Ortak ekranlar ────────────────────────────────
                composable(Screen.Data.route)          { DataScreen(viewModel(factory = ViewModelFactory(dao, repository)), adminViewModel) }
                composable(Screen.Announcements.route) { AnnouncementsScreen(announcementsViewModel, authViewModel, navController) }
                composable(Screen.Profile.route)       {
                    ProfileScreen(
                        authViewModel  = authViewModel,
                        dao            = dao,
                        repository     = repository,
                        currentTheme   = currentTheme,
                        onThemeChange  = onThemeChange,
                        appSettings    = appSettings
                    )
                }
                // ── Phase 1 geriye dönük uyumluluk ────────────────
                composable(Screen.Home.route)     { HomeScreen(dao) }
                composable(Screen.Calendar.route) { CalendarScreen(authViewModel, dao) }
            }
        }
    } else if (authState is AuthState.MustChangePassword) {
        NavHost(navController, startDestination = Screen.ChangePassword.route) {
            composable(Screen.ChangePassword.route) { ChangePasswordScreen(authViewModel) }
        }
    } else {
        NavHost(navController, startDestination = Screen.Login.route) {
            composable(Screen.Login.route) { LoginScreen(authViewModel, navController) }
            composable(Screen.Register.route) { RegisterScreen(authViewModel, navController) }
        }
    }
}

@Composable
fun ChangePasswordScreen(viewModel: AuthViewModel) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.first_time_title), style = MaterialTheme.typography.headlineSmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.first_time_subtitle), style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = oldPassword,
            onValueChange = { oldPassword = it },
            label = { Text(stringResource(R.string.old_password)) },
            placeholder = { Text(stringResource(R.string.old_password_hint), color = TextSecondary.copy(alpha = 0.5f)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, focusedLabelColor = EmeraldGreen, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = EmeraldGreen, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text(stringResource(R.string.new_password)) },
            placeholder = { Text(stringResource(R.string.new_password_hint), color = TextSecondary.copy(alpha = 0.5f)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, focusedLabelColor = EmeraldGreen, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = EmeraldGreen, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
        )
        if (newPassword.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            PasswordStrengthRow(newPassword)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(R.string.confirm_password)) },
            placeholder = { Text(stringResource(R.string.confirm_password_hint), color = TextSecondary.copy(alpha = 0.5f)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, focusedLabelColor = EmeraldGreen, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = EmeraldGreen, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
        )

        if (authState is AuthState.Error) {
            Text((authState as AuthState.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(32.dp))
        val isLoading = authState is AuthState.Loading
        Button(
            onClick = {
                if (newPassword != confirmPassword) {
                    Toast.makeText(context, context.getString(R.string.passwords_no_match), Toast.LENGTH_SHORT).show()
                } else if (newPassword.length < 6) {
                    Toast.makeText(context, context.getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.changePassword(oldPassword, newPassword)
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.update_password), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoginScreen(viewModel: AuthViewModel, navController: NavController) {
    var username        by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A1628), Color(0xFF0F172A), Color(0xFF1A2744))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(56.dp))

            AcademicLogo()

            Spacer(Modifier.height(40.dp))

            // ── Login Card ───────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(28.dp)) {
                    Text(
                        stringResource(R.string.sign_in),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.sign_in_subtitle),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.username)) },
                        placeholder = { Text(stringResource(R.string.username_hint), color = TextSecondary.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = if (username.isNotBlank()) EmeraldGreen else TextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor    = EmeraldGreen,
                            unfocusedBorderColor  = Slate700,
                            focusedLabelColor     = EmeraldGreen,
                            unfocusedLabelColor   = TextSecondary,
                            focusedTextColor      = TextPrimary,
                            unfocusedTextColor    = TextPrimary,
                            cursorColor           = EmeraldGreen,
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        placeholder = { Text(stringResource(R.string.password_hint), color = TextSecondary.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (password.isNotBlank()) EmeraldGreen else TextSecondary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor    = EmeraldGreen,
                            unfocusedBorderColor  = Slate700,
                            focusedLabelColor     = EmeraldGreen,
                            unfocusedLabelColor   = TextSecondary,
                            focusedTextColor      = TextPrimary,
                            unfocusedTextColor    = TextPrimary,
                            cursorColor           = EmeraldGreen,
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    // Error banner
                    if (authState is AuthState.Error) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.10f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                (authState as AuthState.Error).message,
                                color = Color(0xFFEF4444),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    val isLoading = authState is AuthState.Loading
                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.login(username.trim(), password, context)
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.signing_in), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.sign_in), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Demo credentials hint ─────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.07f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.demo_accounts),
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    CredentialHintRow("Admin",      "admin",         "admin")
                    CredentialHintRow("Lecturer 1", "ahmet_yilmaz",  "ahmet123")
                    CredentialHintRow("Lecturer 2", "ayse_kaya",     "ayse123")
                    CredentialHintRow("Lecturer 3", "mehmet_demir",  "mehmet123")
                    CredentialHintRow("Student",    "ogrenci_ali",   "ali123")
                }
            }

            // ── Kayıt Ol Linki ────────────────────────────────
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { navController.navigate(Screen.Register.route) }) {
                Text("Hesabın yok mu? ", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Text("Kayıt Ol", color = EmeraldGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PASSWORD STRENGTH INDICATOR
// ─────────────────────────────────────────────────────────────

@Composable
fun PasswordStrengthRow(password: String) {
    if (password.isEmpty()) return
    val hasMinLength = password.length >= 6
    val hasUppercase = password.any { it.isUpperCase() }
    val hasLowercase = password.any { it.isLowerCase() }
    val hasDigit     = password.any { it.isDigit() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E2535))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StrengthChip("6+ karakter", hasMinLength, Modifier.weight(1f))
            StrengthChip("Büyük harf",  hasUppercase, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StrengthChip("Küçük harf", hasLowercase, Modifier.weight(1f))
            StrengthChip("Rakam",      hasDigit,     Modifier.weight(1f))
        }
    }
}

@Composable
private fun StrengthChip(label: String, met: Boolean, modifier: Modifier = Modifier) {
    val chipColor = if (met) EmeraldGreen else Color(0xFF6B7280)
    val bgColor   = if (met) EmeraldGreen.copy(alpha = 0.12f) else Color.Transparent
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (met) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = chipColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            color = chipColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (met) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun CredentialHintRow(role: String, username: String, password: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            role,
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(76.dp)
        )
        Text(
            username,
            color = TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            "  /  $password",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private val DEPARTMENTS = listOf(
    // Mühendislik Fakültesi
    "Bilgisayar Mühendisliği",
    "Yazılım Mühendisliği",
    "Elektrik-Elektronik Mühendisliği",
    "Makine Mühendisliği",
    "İnşaat Mühendisliği",
    "Endüstri Mühendisliği",
    "Kimya Mühendisliği",
    "Biyomedikal Mühendisliği",
    "Çevre Mühendisliği",
    "Havacılık ve Uzay Mühendisliği",
    "Malzeme Bilimi ve Mühendisliği",
    "Petrol ve Doğal Gaz Mühendisliği",
    "Maden Mühendisliği",
    "Metalurji ve Malzeme Mühendisliği",
    "Gıda Mühendisliği",
    "Biyoloji Mühendisliği",
    "Enerji Sistemleri Mühendisliği",
    "Mekatronik Mühendisliği",
    "Yapay Zeka Mühendisliği",
    "Kontrol ve Otomasyon Mühendisliği",
    // Fen Edebiyat Fakültesi
    "Matematik",
    "Fizik",
    "Kimya",
    "Biyoloji",
    "İstatistik",
    "Moleküler Biyoloji ve Genetik",
    "Astronomi ve Uzay Bilimleri",
    "Jeofizik Mühendisliği",
    "Coğrafya",
    "Türk Dili ve Edebiyatı",
    "İngiliz Dili ve Edebiyatı",
    "Alman Dili ve Edebiyatı",
    "Fransız Dili ve Edebiyatı",
    "Tarih",
    "Arkeoloji",
    "Sanat Tarihi",
    "Felsefe",
    "Sosyoloji",
    "Psikoloji",
    "Antropoloji",
    // Tıp ve Sağlık Bilimleri
    "Tıp",
    "Diş Hekimliği",
    "Eczacılık",
    "Hemşirelik",
    "Ebelik",
    "Fizyoterapi ve Rehabilitasyon",
    "Beslenme ve Diyetetik",
    "Sağlık Yönetimi",
    "Biyokimya",
    "Veteriner Hekimliği",
    "Tıbbi Görüntüleme Teknikleri",
    "Anestezi",
    "Tıbbi Laboratuvar Teknikleri",
    // Sosyal Bilimler ve İdari Bilimler
    "İktisat",
    "İşletme",
    "Muhasebe ve Finans Yönetimi",
    "Maliye",
    "Bankacılık ve Finans",
    "Kamu Yönetimi",
    "Siyaset Bilimi",
    "Uluslararası İlişkiler",
    "Uluslararası Ticaret",
    "Çalışma Ekonomisi ve Endüstri İlişkileri",
    "Yönetim Bilişim Sistemleri",
    "Lojistik Yönetimi",
    // Hukuk
    "Hukuk",
    // Eğitim Fakültesi
    "Eğitim Bilimleri",
    "Bilgisayar ve Öğretim Teknolojileri",
    "Türkçe Öğretmenliği",
    "Matematik Öğretmenliği",
    "Fen Bilgisi Öğretmenliği",
    "İngilizce Öğretmenliği",
    "Okul Öncesi Öğretmenliği",
    "Rehberlik ve Psikolojik Danışmanlık",
    "Özel Eğitim",
    "Beden Eğitimi ve Spor Öğretmenliği",
    "Müzik Öğretmenliği",
    // Mimarlık ve Tasarım
    "Mimarlık",
    "İç Mimarlık",
    "Peyzaj Mimarlığı",
    "Şehir ve Bölge Planlama",
    "Endüstriyel Tasarım",
    "Grafik Tasarım",
    "Moda Tasarımı",
    // İletişim ve Medya
    "İletişim",
    "Gazetecilik",
    "Radyo, Televizyon ve Sinema",
    "Halkla İlişkiler ve Reklamcılık",
    "Dijital Medya",
    // Turizm ve Otelcilik
    "Turizm İşletmeciliği",
    "Otelcilik Yönetimi",
    "Gastronomi ve Mutfak Sanatları",
    "Turizm Rehberliği",
    // Güzel Sanatlar
    "Güzel Sanatlar",
    "Müzik",
    "Tiyatro",
    "Fotoğraf ve Video",
    "Resim",
    "Heykel",
    // Spor Bilimleri
    "Spor Bilimleri",
    "Antrenörlük Eğitimi",
    "Rekreasyon",
    // Diğer
    "Diğer"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(viewModel: AuthViewModel, navController: NavController) {
    var username      by remember { mutableStateOf("") }
    var password      by remember { mutableStateOf("") }
    var confirmPass   by remember { mutableStateOf("") }
    var fullName      by remember { mutableStateOf("") }
    var department    by remember { mutableStateOf("") }
    var adminCode     by remember { mutableStateOf("") }
    var role          by remember { mutableStateOf(UserRole.LECTURER) }
    var roleExpanded  by remember { mutableStateOf(false) }
    var deptExpanded  by remember { mutableStateOf(false) }
    var passVisible   by remember { mutableStateOf(false) }
    var showPending   by remember { mutableStateOf(false) }
    val authState     by viewModel.authState.collectAsState()
    val context       = LocalContext.current
    val isAdminRole   = role == UserRole.ADMIN

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor    = EmeraldGreen,
        unfocusedBorderColor  = Slate700,
        focusedLabelColor     = EmeraldGreen,
        unfocusedLabelColor   = TextSecondary,
        focusedTextColor      = TextPrimary,
        unfocusedTextColor    = TextPrimary,
        cursorColor           = EmeraldGreen,
        focusedContainerColor   = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )

    // Admin kodu ile kayıt olduysa otomatik yönlendir
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            navController.navigate(Screen.AdminHome.route) {
                popUpTo(0) { inclusive = true }
            }
        }
        if (authState is AuthState.Error &&
            (authState as AuthState.Error).message == "REGISTRATION_PENDING") {
            showPending = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1628), Color(0xFF0F172A), Color(0xFF1A2744))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Geri butonu
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = TextSecondary)
                }
            }

            Text(
                if (isAdminRole) "Yönetici Hesabı Oluştur" else "Hesap Oluştur",
                style = MaterialTheme.typography.headlineMedium,
                color = if (isAdminRole) Color(0xFFF59E0B) else TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isAdminRole) "Admin kodu gerekli — yetkili kullanıcılar için."
                else "Kaydınız admin onayından sonra aktif olur.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(24.dp))

            if (showPending) {
                // ── Onay Bekleniyor Ekranı ─────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Kayıt Alındı!", color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Hesabınız oluşturuldu. Admin tarafından onaylandıktan sonra giriş yapabilirsiniz.",
                            color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Giriş Ekranına Dön", fontWeight = FontWeight.Bold) }
                    }
                }
            } else {
                // ── Kayıt Formu ────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        // Ad Soyad
                        OutlinedTextField(
                            value = fullName, onValueChange = { fullName = it },
                            label = { Text("Ad Soyad") },
                            placeholder = { Text("Örn. Ali Vural", color = TextSecondary.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = if (fullName.isNotBlank()) EmeraldGreen else TextSecondary) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = fieldColors
                        )

                        // Kullanıcı adı
                        OutlinedTextField(
                            value = username, onValueChange = { username = it },
                            label = { Text("Kullanıcı Adı") },
                            placeholder = { Text("Örn. ali_vural (boşluksuz)", color = TextSecondary.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.AccountCircle, null, tint = if (username.isNotBlank()) EmeraldGreen else TextSecondary) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = fieldColors
                        )

                        // Bölüm Dropdown
                        ExposedDropdownMenuBox(expanded = deptExpanded, onExpandedChange = { deptExpanded = !deptExpanded }) {
                            OutlinedTextField(
                                value = department.ifEmpty { "Bölüm Seçin" },
                                onValueChange = {}, readOnly = true,
                                label = { Text("Bölüm") },
                                leadingIcon = { Icon(Icons.Default.School, null, tint = if (department.isNotBlank()) EmeraldGreen else TextSecondary) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors
                            )
                            ExposedDropdownMenu(
                                expanded = deptExpanded,
                                onDismissRequest = { deptExpanded = false },
                                modifier = Modifier.background(Slate800).heightIn(max = 300.dp)
                            ) {
                                DEPARTMENTS.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept, color = TextPrimary) },
                                        onClick = { department = dept; deptExpanded = false }
                                    )
                                }
                            }
                        }

                        // Şifre
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            label = { Text("Şifre") },
                            placeholder = { Text("En az 6 karakter", color = TextSecondary.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = if (password.isNotBlank()) EmeraldGreen else TextSecondary) },
                            trailingIcon = {
                                IconButton(onClick = { passVisible = !passVisible }) {
                                    Icon(if (passVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = TextSecondary)
                                }
                            },
                            visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = fieldColors
                        )
                        if (password.isNotEmpty()) {
                            PasswordStrengthRow(password)
                        }

                        // Şifre tekrar
                        OutlinedTextField(
                            value = confirmPass, onValueChange = { confirmPass = it },
                            label = { Text("Şifre Tekrar") },
                            placeholder = { Text("Şifreyi tekrar girin", color = TextSecondary.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = if (confirmPass.isNotBlank()) EmeraldGreen else TextSecondary) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = fieldColors
                        )

                        // Rol Seçici — 3 seçenek (Öğrenci / Hoca / Admin)
                        ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = !roleExpanded }) {
                            OutlinedTextField(
                                value = when (role) {
                                    UserRole.ADMIN    -> "Yönetici (Admin)"
                                    UserRole.STUDENT  -> "Öğrenci"
                                    else              -> "Öğretim Görevlisi"
                                },
                                onValueChange = {}, readOnly = true,
                                label = { Text("Hesap Türü") },
                                leadingIcon = {
                                    Icon(
                                        when (role) {
                                            UserRole.ADMIN   -> Icons.Default.AdminPanelSettings
                                            UserRole.STUDENT -> Icons.Default.School
                                            else             -> Icons.Default.Person
                                        },
                                        null,
                                        tint = when (role) {
                                            UserRole.ADMIN   -> Color(0xFFF59E0B)
                                            UserRole.STUDENT -> Color(0xFF8B5CF6)
                                            else             -> EmeraldGreen
                                        }
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor    = if (isAdminRole) Color(0xFFF59E0B) else EmeraldGreen,
                                    unfocusedBorderColor  = if (isAdminRole) Color(0xFFF59E0B).copy(alpha = 0.5f) else Slate700,
                                    focusedLabelColor     = if (isAdminRole) Color(0xFFF59E0B) else EmeraldGreen,
                                    unfocusedLabelColor   = TextSecondary,
                                    focusedTextColor      = TextPrimary,
                                    unfocusedTextColor    = TextPrimary,
                                    cursorColor           = EmeraldGreen,
                                    focusedContainerColor   = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }, modifier = Modifier.background(Slate800)) {
                                DropdownMenuItem(
                                    text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Person, null, tint = EmeraldGreen, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Öğretim Görevlisi", color = TextPrimary) } },
                                    onClick = { role = UserRole.LECTURER; adminCode = ""; roleExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.School, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Öğrenci", color = TextPrimary) } },
                                    onClick = { role = UserRole.STUDENT; adminCode = ""; roleExpanded = false }
                                )
                                HorizontalDivider(color = Slate700)
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AdminPanelSettings, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text("Yönetici (Admin)", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                                Text("Yetkilendirme kodu gerektirir", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    },
                                    onClick = { role = UserRole.ADMIN; roleExpanded = false }
                                )
                            }
                        }

                        // Admin kodu — yalnızca Admin seçiliyse görünür
                        if (isAdminRole) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.07f)),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Yönetici Yetkilendirmesi", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = adminCode, onValueChange = { adminCode = it },
                                        label = { Text("Admin Kodu") },
                                        leadingIcon = { Icon(Icons.Default.Key, null, tint = Color(0xFFF59E0B)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFF59E0B), unfocusedBorderColor = Color(0xFFF59E0B).copy(alpha = 0.4f),
                                            focusedLabelColor = Color(0xFFF59E0B), unfocusedLabelColor = Color(0xFFF59E0B).copy(alpha = 0.6f),
                                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                            cursorColor = Color(0xFFF59E0B),
                                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }

                        // Hata mesajı
                        if (authState is AuthState.Error && (authState as AuthState.Error).message != "REGISTRATION_PENDING") {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ErrorRed.copy(alpha = 0.10f)).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text((authState as AuthState.Error).message, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        val isLoading = authState is AuthState.Loading
                        Button(
                            onClick = {
                                when {
                                    fullName.isBlank() || username.isBlank() || password.isBlank() || department.isBlank() ->
                                        Toast.makeText(context, "Tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                                    password != confirmPass ->
                                        Toast.makeText(context, "Şifreler eşleşmiyor", Toast.LENGTH_SHORT).show()
                                    password.length < 6 ->
                                        Toast.makeText(context, "Şifre en az 6 karakter olmalı", Toast.LENGTH_SHORT).show()
                                    isAdminRole && adminCode.isBlank() ->
                                        Toast.makeText(context, "Yönetici kaydı için admin kodu gerekli", Toast.LENGTH_SHORT).show()
                                    else -> viewModel.register(username.trim(), password, fullName.trim(), role, department, adminCode.trim())
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAdminRole) Color(0xFFF59E0B) else EmeraldGreen
                            )
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text(if (isAdminRole) "Yönetici Olarak Kayıt Ol" else "Kayıt Ol", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
fun HomeScreen(dao: UniversityDao) {
    val courseCount by dao.getCourseCount().collectAsState(initial = 0)
    val lecturerCount by dao.getLecturerCount().collectAsState(initial = 0)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AcademicLogo(modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Text("Overview", style = MaterialTheme.typography.headlineMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Courses", courseCount.toString(), Icons.AutoMirrored.Filled.List, Modifier.weight(1f))
            StatCard("Faculty", lecturerCount.toString(), Icons.Default.Person, Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(label: String, count: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Slate800), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(count, color = EmeraldGreen, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(importVM: DataImportViewModel, adminVM: AdminViewModel) {
    val importState by importVM.uiState.collectAsState()
    val classrooms  by adminVM.classrooms.collectAsState()
    val schedEntries by adminVM.scheduleEntries.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }
    var pickType    by remember { mutableStateOf(ImportType.COURSES) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importVM.parseExcel(context, it, pickType) }
    }
    val launch = { type: ImportType ->
        pickType = type
        filePicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    }

    // Non-idle import states take over the full screen
    when (val s = importState) {
        is ImportState.Loading      -> { ImportLoadingScreen(); return }
        is ImportState.PreviewReady -> {
            ImportPreviewScreen(s, { importVM.resetState() }, { importVM.commitToDb(s.items, s.type) })
            return
        }
        is ImportState.CredentialSheet -> {
            CredentialSheetScreen(s.credentials) { importVM.resetState() }
            return
        }
        is ImportState.Success -> {
            LaunchedEffect(Unit) { Toast.makeText(context, s.message, Toast.LENGTH_LONG).show(); importVM.resetState() }
            return
        }
        else -> {}
    }

    val errorMsg = (importState as? ImportState.Error)?.message

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Tab bar ──────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = Slate800,
            contentColor     = EmeraldGreen,
            indicator        = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier  = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color     = EmeraldGreen
                )
            }
        ) {
            listOf("Dersler", "Öğretmenler", "Sınıflar").forEachIndexed { i, label ->
                Tab(
                    selected = selectedTab == i,
                    onClick  = { selectedTab = i; if (errorMsg != null) importVM.resetState() },
                    text     = {
                        Text(
                            label,
                            fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal,
                            color      = if (selectedTab == i) EmeraldGreen else TextSecondary
                        )
                    }
                )
            }
        }

        // ── Tab content ──────────────────────────────────────────
        when (selectedTab) {
            0 -> CourseImportTab(importVM, context, launch, errorMsg)
            1 -> LecturerImportTab(importVM, context, launch, errorMsg)
            2 -> ClassroomDataTab(importVM, adminVM, context, launch, classrooms, schedEntries)
        }
    }
}

@Composable
fun CalendarScreen(authViewModel: AuthViewModel, dao: UniversityDao) {
    val user = authViewModel.currentUser ?: return
    val courses by dao.getAllCourses().collectAsState(initial = emptyList())
    val weekDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    val timeSlots = listOf("Morning", "Afternoon")
    val filteredCourses = if (user.role == UserRole.LECTURER) courses.filter { it.lecturerName == user.fullName && it.department == user.department } else courses.filter { it.department == user.department }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Schedule", style = MaterialTheme.typography.headlineMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
            weekDays.forEach { day ->
                Column(modifier = Modifier.width(220.dp).padding(end = 12.dp)) {
                    Text(day, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                    timeSlots.forEach { slot ->
                        val course = filteredCourses.find { it.dayOfWeek == day && it.timeSlot == slot }
                        ScheduleCard(slot, course)
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleCard(slot: String, course: CourseEntity?) {
    Card(modifier = Modifier.fillMaxWidth().height(120.dp).padding(bottom = 12.dp).then(if (course != null) Modifier.border(1.dp, EmeraldGreen, RoundedCornerShape(16.dp)) else Modifier), colors = CardDefaults.cardColors(containerColor = if (course != null) EmeraldGreen.copy(0.1f) else Slate800), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(slot, style = MaterialTheme.typography.labelSmall, color = if (course != null) EmeraldGreen else TextSecondary)
            if (course != null) {
                Column {
                    Text(course.courseName, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(course.courseCode, color = EmeraldGreen, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text("Empty", color = TextSecondary.copy(alpha = 0.3f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    dao: UniversityDao,
    repository: UniversityRepository,
    currentTheme: String = AppSettings.THEME_SYSTEM,
    onThemeChange: (String) -> Unit = {},
    appSettings: AppSettings? = null
) {
    val user = authViewModel.currentUser ?: return
    val pendingUsers by dao.getPendingUsers().collectAsState(initial = emptyList())
    val scope   = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    var showInspectDialog by remember { mutableStateOf<UserEntity?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val fileName = "profile_${user.username}_${System.currentTimeMillis()}.jpg"
                    val file = File(context.filesDir, fileName)
                    val outputStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.flush()
                    outputStream.close()
                    val updatedUser = user.copy(profilePicturePath = file.absolutePath)
                    
                    // Update in Firestore
                    repository.updateLecturer(updatedUser)
                    
                    withContext(Dispatchers.Main) {
                        authViewModel.updateCurrentUser(updatedUser)
                    }
                } catch (e: Exception) { }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            val imageModifier = Modifier.size(120.dp).border(2.dp, EmeraldGreen, CircleShape).padding(4.dp).clip(CircleShape)
            if (user.profilePicturePath != null) {
                AsyncImage(model = user.profilePicturePath, contentDescription = null, modifier = imageModifier, contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = imageModifier, tint = TextSecondary)
            }
            FloatingActionButton(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.size(40.dp), containerColor = EmeraldGreen, shape = CircleShape) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.full_name_display, user.fullName), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.username_display, user.username), color = TextSecondary)
        Text(stringResource(R.string.role_display, user.role.name), color = EmeraldGreen, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { showPasswordDialog = true }, 
            modifier = Modifier.fillMaxWidth(), 
            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.change_password_btn))
        }
        
        if (user.role == UserRole.ADMIN) {
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.pending_approvals), style = MaterialTheme.typography.titleMedium, color = EmeraldGreen)
            pendingUsers.forEach { pendingUser ->
                Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = Slate800), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(pendingUser.fullName, fontWeight = FontWeight.Bold)
                            Text(pendingUser.username, style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            IconButton(onClick = { showInspectDialog = pendingUser }) {
                                Icon(Icons.Default.Info, contentDescription = "Inspect", tint = TextSecondary)
                            }
                            IconButton(onClick = { scope.launch { dao.updateUser(pendingUser.copy(status = UserStatus.APPROVED)) } }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Approve", tint = EmeraldGreen)
                            }
                        }
                    }
                }
            }
        }

        // ── Appearance Settings ───────────────────────────────
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.appearance_section), style = MaterialTheme.typography.titleMedium, color = EmeraldGreen)
        Spacer(Modifier.height(12.dp))

        // Tema seçici
        val themeOptions = listOf(
            AppSettings.THEME_SYSTEM to stringResource(R.string.theme_system),
            AppSettings.THEME_LIGHT  to stringResource(R.string.theme_light),
            AppSettings.THEME_DARK   to stringResource(R.string.theme_dark)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            themeOptions.forEach { (mode, label) ->
                val isSelected = currentTheme == mode
                OutlinedButton(
                    onClick = { onThemeChange(mode) },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) EmeraldGreen.copy(alpha = 0.15f) else Color.Transparent,
                        contentColor   = if (isSelected) EmeraldGreen else TextSecondary
                    ),
                    border   = androidx.compose.foundation.BorderStroke(
                        1.dp, if (isSelected) EmeraldGreen else Slate700
                    )
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        // Dil seçici
        Spacer(Modifier.height(12.dp))
        val langOptions = listOf(AppSettings.LANG_TR to stringResource(R.string.lang_turkish), AppSettings.LANG_EN to stringResource(R.string.lang_english))
        val currentLang = appSettings?.language ?: AppSettings.LANG_TR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            langOptions.forEach { (code, label) ->
                val isSelected = currentLang == code
                OutlinedButton(
                    onClick = {
                        appSettings?.language = code
                        Toast.makeText(context, context.getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
                        activity?.recreate()
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) IndigoAccent.copy(alpha = 0.15f) else Color.Transparent,
                        contentColor   = if (isSelected) IndigoAccent else TextSecondary
                    ),
                    border   = androidx.compose.foundation.BorderStroke(
                        1.dp, if (isSelected) IndigoAccent else Slate700
                    )
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(Modifier.height(48.dp))
        Button(
            onClick = { authViewModel.logout(context) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.logout_btn), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
        }
    }

    if (showInspectDialog != null) {
        val u = showInspectDialog!!
        AlertDialog(
            onDismissRequest = { showInspectDialog = null },
            title = { Text(stringResource(R.string.inspect_user_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.inspect_user_full_name, u.fullName), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.inspect_user_username, u.username))
                    Text(stringResource(R.string.inspect_user_dept, u.department))
                    Text(stringResource(R.string.inspect_user_role, u.role.name))
                    Text(stringResource(R.string.inspect_user_status, u.status.name))
                }
            },
            confirmButton = { TextButton(onClick = { showInspectDialog = null }) { Text(stringResource(R.string.close)) } }
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPasswordDialog = false
                oldPassword = ""; newPassword = ""; confirmPassword = ""
            },
            title = { Text(stringResource(R.string.update_password_title), color = EmeraldGreen, fontWeight = FontWeight.Bold) },
            text = {
                val pf = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor    = EmeraldGreen,
                    unfocusedBorderColor  = Slate700,
                    focusedLabelColor     = EmeraldGreen,
                    unfocusedLabelColor   = TextSecondary,
                    focusedTextColor      = TextPrimary,
                    unfocusedTextColor    = TextPrimary,
                    cursorColor           = EmeraldGreen,
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text(stringResource(R.string.old_password)) },
                        placeholder = { Text(stringResource(R.string.old_password_hint), color = TextSecondary.copy(alpha = 0.5f)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = pf
                    )
                    HorizontalDivider(thickness = 1.dp, color = TextSecondary.copy(alpha = 0.1f))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(R.string.new_password)) },
                        placeholder = { Text(stringResource(R.string.new_password_hint), color = TextSecondary.copy(alpha = 0.5f)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = pf
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.confirm_password)) },
                        placeholder = { Text(stringResource(R.string.confirm_password_hint), color = TextSecondary.copy(alpha = 0.5f)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = pf
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (user.username == "admin") {
                            Toast.makeText(context, "Admin şifresi sistem tarafından yönetilir, değiştirilemez.", Toast.LENGTH_SHORT).show()
                        } else if (CredentialUtils.hashPassword(oldPassword) != user.password) {
                            Toast.makeText(context, context.getString(R.string.old_password_incorrect), Toast.LENGTH_SHORT).show()
                        } else if (newPassword != confirmPassword) {
                            Toast.makeText(context, context.getString(R.string.passwords_no_match), Toast.LENGTH_SHORT).show()
                        } else if (newPassword.length < 6) {
                            Toast.makeText(context, context.getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                                val updatedUser = user.copy(password = CredentialUtils.hashPassword(newPassword))
                                repository.updateLecturer(updatedUser)
                                withContext(Dispatchers.Main) {
                                    authViewModel.updateCurrentUser(updatedUser)
                                    showPasswordDialog = false
                                    oldPassword = ""; newPassword = ""; confirmPassword = ""
                                    Toast.makeText(context, context.getString(R.string.password_updated), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) { Text(stringResource(R.string.confirm_change)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    oldPassword = ""; newPassword = ""; confirmPassword = ""
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

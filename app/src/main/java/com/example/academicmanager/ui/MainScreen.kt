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
import coil.compose.AsyncImage
import com.example.academicmanager.data.*
import com.example.academicmanager.ui.theme.EmeraldGreen
import com.example.academicmanager.ui.theme.Slate800
import com.example.academicmanager.ui.theme.TextPrimary
import com.example.academicmanager.ui.theme.TextSecondary
import com.example.academicmanager.ui.viewmodels.*
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

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Register : Screen("register", "Register", Icons.Default.Person)
    object ChangePassword : Screen("change_password", "Security", Icons.Default.Lock)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Calendar : Screen("calendar", "Schedule", Icons.Default.DateRange)
    object Data : Screen("data", "Courses", Icons.AutoMirrored.Filled.List)
    object Profile : Screen("profile", "Settings", Icons.Default.Settings)
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val database = remember { UniversityDatabase.getDatabase(context) }
    val dao = database.universityDao()
    
    val repository: UniversityRepository = remember { 
        UniversityRepositoryImpl(FirebaseFirestore.getInstance()) 
    }
    
    val authViewModel: AuthViewModel = viewModel(factory = ViewModelFactory(dao, repository))
    val authState by authViewModel.authState.collectAsState()
    
    val navController = rememberNavController()
    val courseCount by dao.getCourseCount().collectAsState(initial = 0)

    val bottomNavItems = remember(courseCount) {
        val list = mutableListOf(Screen.Home, Screen.Data, Screen.Profile)
        if (courseCount > 0) {
            list.add(1, Screen.Calendar)
        }
        list
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate(Screen.Home.route) {
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
                    containerColor = Color.Transparent, 
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(70.dp).padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { 
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = screen.icon, 
                                        contentDescription = null,
                                        modifier = Modifier.size(if (selected) 24.dp else 20.dp),
                                        tint = if (selected) EmeraldGreen else TextSecondary.copy(alpha = 0.5f)
                                    )
                                    if (selected) {
                                        Spacer(Modifier.height(4.dp))
                                        Box(Modifier.size(4.dp).background(EmeraldGreen, CircleShape))
                                    }
                                }
                            },
                            label = null,
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController, startDestination = Screen.Login.route, modifier = Modifier.padding(innerPadding)) {
                composable(Screen.Login.route) { LoginScreen(authViewModel, navController) }
                composable(Screen.Register.route) { RegisterScreen(authViewModel, navController) }
                composable(Screen.Home.route) { HomeScreen(dao) }
                composable(Screen.Calendar.route) { CalendarScreen(authViewModel, dao) }
                composable(Screen.Data.route) { DataScreen(viewModel(factory = ViewModelFactory(dao, repository))) }
                composable(Screen.Profile.route) { ProfileScreen(authViewModel, dao, repository) }
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
        Text("First-Time Security Update", style = MaterialTheme.typography.headlineSmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("You must change your default password to continue.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = oldPassword, 
            onValueChange = { oldPassword = it }, 
            label = { Text("Old Password") }, 
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = newPassword, 
            onValueChange = { newPassword = it }, 
            label = { Text("New Password") }, 
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = confirmPassword, 
            onValueChange = { confirmPassword = it }, 
            label = { Text("Confirm New Password") }, 
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (authState is AuthState.Error) {
            Text((authState as AuthState.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (newPassword != confirmPassword) {
                    Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                } else if (newPassword.length < 6) {
                    Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.changePassword(oldPassword, newPassword)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Update Password", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoginScreen(viewModel: AuthViewModel, navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AcademicLogo(modifier = Modifier.padding(bottom = 48.dp))
        
        OutlinedTextField(
            value = username, 
            onValueChange = { username = it }, 
            label = { Text("Username") }, 
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, focusedLabelColor = EmeraldGreen)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it }, 
            label = { Text("Password") }, 
            visualTransformation = PasswordVisualTransformation(), 
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, focusedLabelColor = EmeraldGreen)
        )
        
        if (authState is AuthState.Error) {
            Text((authState as AuthState.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.login(username, password)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Login", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = { navController.navigate(Screen.Register.route) }) {
            Text("Don't have an account? Register", color = TextSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(viewModel: AuthViewModel, navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.LECTURER) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState())) {
        Text("Create Account", style = MaterialTheme.typography.headlineMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(12.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(value = role.name, onValueChange = {}, readOnly = true, label = { Text("Role") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                UserRole.entries.forEach { roleOption ->
                    DropdownMenuItem(text = { Text(roleOption.name) }, onClick = { role = roleOption; expanded = false })
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (username.isBlank() || password.isBlank() || fullName.isBlank() || department.isBlank()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.register(username, password, fullName, role, department)
                    navController.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
        ) {
            Text("Register", fontWeight = FontWeight.Bold)
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

@Composable
fun DataScreen(viewModel: DataImportViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(ImportType.COURSES) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.parseExcel(context, it, selectedType) }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (val s = state) {
            is ImportState.Idle -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Data Management", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(32.dp))
                    ImportActionCard("Courses", Icons.AutoMirrored.Filled.List, { viewModel.downloadTemplate(context, ImportType.COURSES) }, { selectedType = ImportType.COURSES; filePicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") })
                    Spacer(Modifier.height(16.dp))
                    ImportActionCard("Lecturers", Icons.Default.Person, { viewModel.downloadTemplate(context, ImportType.LECTURERS) }, { selectedType = ImportType.LECTURERS; filePicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") })
                }
            }
            is ImportState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = EmeraldGreen)
            is ImportState.PreviewReady -> {
                Column(Modifier.fillMaxSize()) {
                    Text("Verify Data (${s.items.size} rows)", color = EmeraldGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    LazyColumn(Modifier.weight(1f).padding(vertical = 16.dp)) {
                        items(s.items) { item ->
                            val text = when (item) {
                                is CourseEntity -> "${item.courseCode} - ${item.courseName}"
                                is LecturerEntity -> item.fullName
                                is Course -> "${item.courseCode} - ${item.courseName}"
                                is Lecturer -> "${item.fullName} (@${item.username})"
                                else -> "Unknown Item"
                            }
                            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Slate800)) {
                                Text(text = text, modifier = Modifier.padding(16.dp), color = TextPrimary)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { viewModel.resetState() }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Slate800)) { Text("Discard") }
                        Button(onClick = { viewModel.commitToDb(s.items, s.type) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)) { Text("Save to DB") }
                    }
                }
            }
            is ImportState.Error -> {
                AlertDialog(onDismissRequest = { viewModel.resetState() }, title = { Text("Import Error") }, text = { Text(s.message) }, confirmButton = { Button(onClick = { viewModel.resetState() }) { Text("OK") } })
            }
            is ImportState.Success -> {
                LaunchedEffect(Unit) { Toast.makeText(context, s.message, Toast.LENGTH_LONG).show(); viewModel.resetState() }
            }
        }
    }
}

@Composable
fun ImportActionCard(title: String, icon: ImageVector, onDownload: () -> Unit, onImport: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Slate800), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = EmeraldGreen)
                Spacer(Modifier.width(16.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(onClick = onDownload) { Icon(Icons.Default.Refresh, contentDescription = "Template", tint = TextSecondary) }
                IconButton(onClick = onImport) { Icon(Icons.Default.Add, contentDescription = "Import", tint = EmeraldGreen) }
            }
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
fun ProfileScreen(authViewModel: AuthViewModel, dao: UniversityDao, repository: UniversityRepository) {
    val user = authViewModel.currentUser ?: return
    val pendingUsers by dao.getPendingUsers().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
        Text("Profile Settings", style = MaterialTheme.typography.headlineMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
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
        Text("Full Name: ${user.fullName}", style = MaterialTheme.typography.titleLarge)
        Text("Username: @${user.username}", color = TextSecondary)
        Text("Role: ${user.role}", color = EmeraldGreen, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { showPasswordDialog = true }, 
            modifier = Modifier.fillMaxWidth(), 
            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Change Password")
        }
        
        if (user.role == UserRole.ADMIN) {
            Spacer(Modifier.height(32.dp))
            Text("Pending Approvals", style = MaterialTheme.typography.titleMedium, color = EmeraldGreen)
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

        Spacer(Modifier.height(48.dp))
        Button(
            onClick = { authViewModel.logout() }, 
            modifier = Modifier.fillMaxWidth(), 
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Logout", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
        }
    }

    if (showInspectDialog != null) {
        val u = showInspectDialog!!
        AlertDialog(
            onDismissRequest = { showInspectDialog = null },
            title = { Text("User Details") },
            text = {
                Column {
                    Text("Full Name: ${u.fullName}", fontWeight = FontWeight.Bold)
                    Text("Username: ${u.username}")
                    Text("Department: ${u.department}")
                    Text("Applied Role: ${u.role}")
                    Text("Current Status: ${u.status}")
                }
            },
            confirmButton = { TextButton(onClick = { showInspectDialog = null }) { Text("Close") } }
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPasswordDialog = false
                oldPassword = ""; newPassword = ""; confirmPassword = ""
            },
            title = { Text("Update Password", color = EmeraldGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = oldPassword, 
                        onValueChange = { oldPassword = it }, 
                        label = { Text("Verify Old Password") }, 
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(thickness = 1.dp, color = TextSecondary.copy(alpha = 0.1f))
                    OutlinedTextField(
                        value = newPassword, 
                        onValueChange = { newPassword = it }, 
                        label = { Text("New Password") }, 
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassword, 
                        onValueChange = { confirmPassword = it }, 
                        label = { Text("Confirm New Password") }, 
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldPassword != user.password) {
                            Toast.makeText(context, "Old password incorrect", Toast.LENGTH_SHORT).show()
                        } else if (newPassword != confirmPassword) {
                            Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                        } else if (newPassword.length < 4) {
                            Toast.makeText(context, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                                val updatedUser = user.copy(password = newPassword)
                                repository.updateLecturer(updatedUser)
                                withContext(Dispatchers.Main) {
                                    authViewModel.updateCurrentUser(updatedUser)
                                    showPasswordDialog = false
                                    oldPassword = ""; newPassword = ""; confirmPassword = ""
                                    Toast.makeText(context, "Password updated!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) { Text("Confirm Change") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPasswordDialog = false
                    oldPassword = ""; newPassword = ""; confirmPassword = ""
                }) { Text("Cancel") }
            }
        )
    }
}

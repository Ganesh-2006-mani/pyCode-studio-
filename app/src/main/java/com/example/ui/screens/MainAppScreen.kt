package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.compiler.ConsoleOutput
import com.example.compiler.InterpreterState
import com.example.data.database.ProjectEntity
import com.example.ui.editor.PythonSyntaxVisualTransformation
import com.example.ui.theme.*
import com.example.ui.viewmodel.PyCodeViewModel
import com.example.tutorials.PythonLessons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen(
    viewModel: PyCodeViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Observe and show custom toast/snack bars
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearSnackbar()
            }
        }
    }

    MyApplicationTheme(darkTheme = isDark) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background futuristic vector decorations
                FuturisticBackgroundGrid(isDark = isDark)

                // Master Router
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        "splash" -> SplashScreen(viewModel)
                        "onboarding" -> OnboardingScreen(viewModel)
                        "home" -> HomeScreen(viewModel)
                        "editor" -> EditorScreen(viewModel)
                        "tutorials" -> TutorialsScreen(viewModel)
                        "settings" -> SettingsScreen(viewModel)
                        "profile" -> ProfileScreen(viewModel)
                    }
                }

                // Standard floating HUD snackbar host
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                )

                // Navigation drawer at the bottom of standard pages
                val showBottomNav = currentScreen != "splash" && currentScreen != "onboarding"
                if (showBottomNav) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        GlassyBottomNavigationBar(
                            selectedScreen = currentScreen,
                            onTabSelected = { viewModel.currentScreen.value = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FuturisticBackgroundGrid(isDark: Boolean) {
    val gridColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.2f) else Color(0xFFCBD5E1).copy(alpha = 0.3f)
    val circleColor = if (isDark) CosmicAccentCyan.copy(alpha = 0.05f) else CosmicNeonBlue.copy(alpha = 0.04f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Canvas grids
                val step = 40.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                    y += step
                }
                var x = 0f
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                    x += step
                }

                // Radiant neon circle halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(circleColor, Color.Transparent),
                        center = Offset(size.width * 0.82f, size.height * 0.15f),
                        radius = size.width * 0.45f
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.15f),
                    radius = size.width * 0.45f
                )
            }
    )
}

// ================= SPLASH SCREEN =================
@Composable
fun SplashScreen(viewModel: PyCodeViewModel) {
    var progressText by remember { mutableStateOf("Initializing PyCode modules...") }
    var bootProgress by remember { mutableStateOf(0.1f) }

    LaunchedEffect(Unit) {
        delay(600)
        progressText = "Pre-populating offline compiler library..."
        bootProgress = 0.4f
        delay(700)
        progressText = "Binding static token parser rules..."
        bootProgress = 0.7f
        delay(600)
        progressText = "Offline Sandbox Safe Grid Online."
        bootProgress = 1.0f
        delay(500)
        viewModel.currentScreen.value = "onboarding"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.pycode_logo_1779884999881),
            contentDescription = "PyCode Studio Logo Logo",
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(32.dp))
                .border(2.dp, CosmicAccentCyan, RoundedCornerShape(32.dp))
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "PyCode Studio",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = CosmicAccentCyan
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Modern Offline Python IDE",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = CosmicGray,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(60.dp))

        // Progress loader indicator
        LinearProgressIndicator(
            progress = { bootProgress },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(6.dp)
                .clip(CircleShape),
            color = CosmicAccentCyan,
            trackColor = CosmicCardBg
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = progressText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = CosmicNeonBlue
            )
        )
    }
}

// ================= ONBOARDING SCREEN =================
@Composable
fun OnboardingScreen(viewModel: PyCodeViewModel) {
    var activeStep by remember { mutableStateOf(0) }
    
    val steps = listOf(
        OnboardingItem(
            title = "100% Offline Compiler",
            description = "No API keys, server subscriptions, or cloud latency. Compile, test, and run your script instantly on your device with complete security.",
            icon = Icons.Default.Dns
        ),
        OnboardingItem(
            title = "Professional Editor & Console",
            description = "Write Python with real-time semantic highlighting, line counters, voice-typing, smart suggestions, and interactive terminal input support.",
            icon = Icons.Default.Terminal
        ),
        OnboardingItem(
            title = "Interactive Practice Guides",
            description = "Learn while typing! Solve real offline algorithm exercises, accumulate scholar XP, and benchmark your progress entirely off the grid.",
            icon = Icons.Default.School
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip Button top right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { viewModel.currentScreen.value = "home" },
                modifier = Modifier.testTag("onboarding_skip")
            ) {
                Text("Skip", color = CosmicGray)
            }
        }

        // Active Slider Card
        val activeItem = steps[activeStep]
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicCardBg.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, CosmicNeonBlue.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(CosmicAccentCyan.copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, CosmicAccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = activeItem.icon,
                        contentDescription = null,
                        tint = CosmicAccentCyan,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = activeItem.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = CosmicWhite
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = activeItem.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = CosmicGray,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // Indicator index dots & Navigation
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                steps.forEachIndexed { idx, _ ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (idx == activeStep) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (idx == activeStep) CosmicAccentCyan else CosmicCardBg)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // CTA Button
            Button(
                onClick = {
                    if (activeStep < steps.size - 1) {
                        activeStep++
                    } else {
                        viewModel.currentScreen.value = "home"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_continue"),
                colors = ButtonDefaults.buttonColors(containerColor = CosmicNeonBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (activeStep == steps.size - 1) "Launch Engine" else "Next Step",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

data class OnboardingItem(val title: String, val description: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

// ================= GLASSY BOTTOM BAR =================
@Composable
fun GlassyBottomNavigationBar(
    selectedScreen: String,
    onTabSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicCardBg.copy(alpha = 0.82f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTab(
                screenId = "home",
                icon = Icons.Default.Dashboard,
                label = "Dashboard",
                active = selectedScreen == "home",
                onClick = onTabSelected
            )
            BottomNavTab(
                screenId = "editor",
                icon = Icons.Default.Code,
                label = "Editor",
                active = selectedScreen == "editor",
                onClick = onTabSelected
            )
            BottomNavTab(
                screenId = "tutorials",
                icon = Icons.Outlined.Book,
                label = "Tutorials",
                active = selectedScreen == "tutorials",
                onClick = onTabSelected
            )
            BottomNavTab(
                screenId = "profile",
                icon = Icons.Outlined.Person,
                label = "Profile",
                active = selectedScreen == "profile",
                onClick = onTabSelected
            )
            BottomNavTab(
                screenId = "settings",
                icon = Icons.Outlined.Settings,
                label = "Settings",
                active = selectedScreen == "settings",
                onClick = onTabSelected
            )
        }
    }
}

@Composable
fun BottomNavTab(
    screenId: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: (String) -> Unit
) {
    val duration = 250
    val tintColor by animateColorAsState(
        targetValue = if (active) CosmicAccentCyan else CosmicGray,
        animationSpec = tween(duration),
        label = "navColor"
    )

    Column(
        modifier = Modifier
            .width(64.dp)
            .height(56.dp)
            .clickable(onClick = { onClick(screenId) })
            .testTag("nav_tab_$screenId"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = tintColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ================= HOME SCREEN =================
@Composable
fun HomeScreen(viewModel: PyCodeViewModel) {
    val projects by viewModel.allProjects.collectAsState()
    val favorited by viewModel.favoriteProjects.collectAsState()
    val recent by viewModel.recentProjects.collectAsState()
    val scholarXp by viewModel.totalXp.collectAsState()
    
    val searchVal by viewModel.searchQuery.collectAsState()
    val categoryVal by viewModel.selectedCategory.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjName by remember { mutableStateOf("") }
    var newProjCategory by remember { mutableStateOf("Basics") }

    val filteredProjects = projects.filter {
        (categoryVal == "All" || it.category == categoryVal) &&
        (searchVal.isEmpty() || it.name.contains(searchVal, ignoreCase = true))
    }

    val context = LocalContext.current
    // File Importer
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val inputStr = context.contentResolver.openInputStream(uri)
                val displayName = "imported_" + (System.currentTimeMillis() % 1000) + ".py"
                if (inputStr != null) {
                    viewModel.importCodeFromUri(inputStr, displayName)
                }
            } catch (e: Exception) {
                viewModel.showSnackbar("Failed to import: ${e.message}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Welcome and Scholar Stats
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicCardBg.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CosmicAccentCyan.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OFFLINE SECURE GRID",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = CosmicAccentCyan,
                                letterSpacing = 2.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scholar Workspace",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = CosmicWhite
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(CosmicPurple.copy(alpha = 0.2f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Level ${(scholarXp / 500) + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = CosmicAccentCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$scholarXp XP Scholar credits",
                                style = MaterialTheme.typography.bodySmall.copy(color = CosmicGray)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(CosmicAccentCyan.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = null,
                            tint = CosmicAccentCyan,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }

        // Action Quick Access Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clickable { showCreateDialog = true }
                        .testTag("action_create_project"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicCardBg.copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, CosmicNeonBlue.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = CosmicAccentCyan)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("New File", style = MaterialTheme.typography.labelLarge.copy(color = CosmicWhite))
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clickable { fileImportLauncher.launch(arrayOf("text/*", "application/octet-stream")) }
                        .testTag("action_import_file"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicCardBg.copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, CosmicNeonBlue.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, tint = CosmicNeonBlue)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Import Script", style = MaterialTheme.typography.labelLarge.copy(color = CosmicWhite))
                    }
                }
            }
        }

        // Recent Sandbox Files Selection
        if (recent.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Projects",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CosmicAccentCyan
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    recent.take(5).forEach { proj ->
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable {
                                    viewModel.setProjectActive(proj)
                                    viewModel.currentScreen.value = "editor"
                                }
                                .testTag("recent_item_${proj.id}"),
                            colors = CardDefaults.cardColors(containerColor = CosmicCardBg.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CosmicGray.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = CosmicNeonBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    if (proj.isFavorite) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = CosmicAccentCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = proj.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = CosmicWhite
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = proj.category,
                                    style = MaterialTheme.typography.labelSmall.copy(color = CosmicGray)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Explorer Header & Filter Row
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "Offline Project Manager",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CosmicAccentCyan
                    )
                )
                
                Spacer(modifier = Modifier.height(10.dp))

                // Search Box
                OutlinedTextField(
                    value = searchVal,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search files...", color = CosmicGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_projects_input"),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CosmicGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CosmicWhite,
                        unfocusedTextColor = CosmicWhite,
                        focusedBorderColor = CosmicAccentCyan,
                        unfocusedBorderColor = CosmicGray.copy(alpha = 0.3f),
                        unfocusedContainerColor = CosmicCardBg.copy(alpha = 0.4f),
                        focusedContainerColor = CosmicCardBg.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val categories = listOf("All", "Basics", "Algorithms", "Games")
                    categories.forEach { cat ->
                        val isSelected = cat == categoryVal
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) CosmicAccentCyan else CosmicCardBg)
                                .clickable { viewModel.selectedCategory.value = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("category_chip_$cat")
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) CosmicDeepNavy else CosmicGray,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }

        // Folder files dynamic list
        if (filteredProjects.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = CosmicGray.copy(alpha = 0.4f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No offline files detected",
                        style = MaterialTheme.typography.bodyMedium.copy(color = CosmicGray)
                    )
                }
            }
        } else {
            items(filteredProjects) { project ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            viewModel.setProjectActive(project)
                            viewModel.currentScreen.value = "editor"
                        }
                        .testTag("project_row_${project.id}"),
                    colors = CardDefaults.cardColors(containerColor = CosmicCardBg.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CosmicNeonBlue.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (project.isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = if (project.isFavorite) CosmicAccentCyan else CosmicGray,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        viewModel.activeProjectId.value = project.id
                                        viewModel.toggleFavoriteActiveProject()
                                    }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = CosmicWhite,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Category: ${project.category} offline file",
                                    style = MaterialTheme.typography.bodySmall.copy(color = CosmicGray)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.deleteProjectById(project.id) },
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete File", tint = CosmicGray)
                        }
                    }
                }
            }
        }
    }

    // Modal dialog to create new project offline
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Generate Python Source File", color = CosmicWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProjName,
                        onValueChange = { newProjName = it },
                        label = { Text("Filename (e.g., myscript.py)", color = CosmicGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CosmicWhite,
                            unfocusedTextColor = CosmicWhite,
                            focusedBorderColor = CosmicAccentCyan,
                            unfocusedBorderColor = CosmicGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_new_filename"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Choose Category tag:", color = CosmicGray, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tags = listOf("Basics", "Algorithms", "Games")
                        tags.forEach { tag ->
                            val isSelected = newProjCategory == tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) CosmicNeonBlue else CosmicCardBg)
                                    .clickable { newProjCategory = tag }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("dialog_tag_chip_$tag")
                            ) {
                                Text(
                                    text = tag,
                                    color = if (isSelected) Color.White else CosmicGray,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjName.isNotEmpty()) {
                            viewModel.saveAsNewProject(newProjName, newProjCategory)
                            showCreateDialog = false
                            viewModel.currentScreen.value = "editor"
                        } else {
                            viewModel.showSnackbar("Please specify a project name")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicAccentCyan),
                    modifier = Modifier.testTag("dialog_confirm_create")
                ) {
                    Text("Create", color = CosmicDeepNavy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = CosmicGray)
                }
            },
            containerColor = CosmicCardBg
        )
    }
}

// ================= EDITOR SCREEN =================
@Composable
fun EditorScreen(viewModel: PyCodeViewModel) {
    val editorText by viewModel.editorContent.collectAsState()
    val activeName by viewModel.activeProjectName.collectAsState()
    val activeId by viewModel.activeProjectId.collectAsState()
    val isFavorite by viewModel.isFavoriteState.collectAsState()

    val consoleOuts by viewModel.consoleOutputs.collectAsState()
    val isRunningState by viewModel.interpreterState.collectAsState()
    val zoomVal by viewModel.zoomLevel.collectAsState()
    val isSplit by viewModel.isSplitScreen.collectAsState()

    val suggestions by viewModel.autocompleteSuggestions.collectAsState()

    val isSearchVisible by viewModel.isFindReplaceVisible.collectAsState()
    val findVal by viewModel.findText.collectAsState()
    val replaceVal by viewModel.replaceText.collectAsState()

    var consoleInputVal by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Share result launcher
    val shareIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}

    // File Exporter Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            try {
                val outStr = context.contentResolver.openOutputStream(uri)
                if (outStr != null) {
                    viewModel.exportCodeToUri(outStr)
                }
            } catch (e: Exception) {
                viewModel.showSnackbar("Export failed: ${e.message}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // TOP HEADER ACTION BAR
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicHeaderBg.copy(alpha = 0.95f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back option
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.currentScreen.value = "home" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CosmicAccentCyan)
                    }
                    Column(modifier = Modifier.padding(start = 2.dp)) {
                        Text(
                            text = activeName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CosmicWhite
                             ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                        Text(
                            text = if (activeId != null) "Saved Offline" else "Unsaved Buff",
                            style = MaterialTheme.typography.bodySmall.copy(color = CosmicGray, fontSize = 10.sp)
                        )
                    }
                }

                Row {
                    // Quick Bookmarker Toggle
                    IconButton(onClick = { viewModel.toggleFavoriteActiveProject() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) CosmicAccentCyan else CosmicGray
                        )
                    }

                    // Format trigger
                    IconButton(onClick = { viewModel.formatActiveCode() }) {
                        Icon(Icons.Default.FormatAlignLeft, contentDescription = "Format Code", tint = CosmicAccentCyan)
                    }

                    // Run Python compiler
                    IconButton(
                        onClick = { viewModel.runPythonCode() },
                        modifier = Modifier.testTag("run_python_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run compiler", tint = CosmicEmerald)
                    }

                    // Additional menu Actions
                    var showDropdownMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showDropdownMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Extra Menu", tint = CosmicGray)
                    }

                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false },
                        containerColor = CosmicCardBg
                    ) {
                        DropdownMenuItem(
                            text = { Text("Search & Replace", color = CosmicWhite) },
                            onClick = {
                                viewModel.isFindReplaceVisible.value = !isSearchVisible
                                showDropdownMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.FindInPage, contentDescription = null, tint = CosmicAccentCyan) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to File", color = CosmicWhite) },
                            onClick = {
                                exportLauncher.launch(activeName)
                                showDropdownMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = null, tint = CosmicNeonBlue) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Script", color = CosmicWhite) },
                            onClick = {
                                val text = viewModel.getShareableText()
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                val chooser = Intent.createChooser(shareIntent, "Share Python Script via")
                                shareIntentLauncher.launch(chooser)
                                showDropdownMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = CosmicAccentCyan) }
                        )
                    }
                }
            }
        }

        // TOOLBAR: FIND AND REPLACE PANEL
        if (isSearchVisible) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = findVal,
                            onValueChange = { viewModel.findText.value = it },
                            placeholder = { Text("Find text", color = CosmicGray, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CosmicWhite,
                                unfocusedTextColor = CosmicWhite,
                                focusedBorderColor = CosmicAccentCyan,
                                unfocusedBorderColor = CosmicGray
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = replaceVal,
                            onValueChange = { viewModel.replaceText.value = it },
                            placeholder = { Text("Replace with", color = CosmicGray, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CosmicWhite,
                                unfocusedTextColor = CosmicWhite,
                                focusedBorderColor = CosmicNeonBlue,
                                unfocusedBorderColor = CosmicGray
                            ),
                            singleLine = true
                        )
                    }
                    Column(
                        modifier = Modifier.width(90.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                if (findVal.isNotEmpty()) {
                                    val regex = findVal.toRegex()
                                    val count = regex.findAll(editorText).count()
                                    if (count > 0) {
                                        val replaced = editorText.replace(findVal, replaceVal)
                                        viewModel.updateEditorContent(replaced)
                                        viewModel.showSnackbar("Replaced $count occurrences!")
                                    } else {
                                        viewModel.showSnackbar("No matches discovered")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicAccentCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ALL", fontSize = 10.sp, color = CosmicDeepNavy)
                        }
                        TextButton(
                            onClick = { viewModel.isFindReplaceVisible.value = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close", fontSize = 11.sp, color = CosmicGray)
                        }
                    }
                }
            }
        }

        // CONTROL UTILITIES BAR: UNDO / REDO / ZOOM
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.performUndo() },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCardBg),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = CosmicAccentCyan, modifier = Modifier.size(18.dp))
                }

                Button(
                    onClick = { viewModel.performRedo() },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCardBg),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo", tint = CosmicAccentCyan, modifier = Modifier.size(18.dp))
                }
            }

            // Zoom level
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.zoomLevel.value = (zoomVal - 1).coerceAtLeast(10f) }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Font small", tint = CosmicGray)
                }
                Text(text = "${zoomVal.toInt()}", color = CosmicWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                IconButton(onClick = { viewModel.zoomLevel.value = (zoomVal + 1).coerceAtMost(28f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Font big", tint = CosmicGray)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Toggle Split-Screen Layout (Editor / Console)
                IconButton(onClick = { viewModel.isSplitScreen.value = !isSplit }) {
                    Icon(
                        imageVector = if (isSplit) Icons.Default.FullscreenExit else Icons.Default.ViewAgenda,
                        contentDescription = "Split view",
                        tint = if (isSplit) CosmicAccentCyan else CosmicGray
                    )
                }
            }
        }

        // OFFLINE CONTINUOUS AI AUTOCOMPLETE drawer toolbar inside the editor!
        if (suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(CosmicCardBg.copy(alpha = 0.92f))
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Offline Autocomplete icon",
                    tint = CosmicAccentCyan,
                    modifier = Modifier.size(14.dp)
                )
                suggestions.forEach { sug ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicNeonBlue.copy(alpha = 0.3f))
                            .clickable { viewModel.applyAutocomplete(sug) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("suggestion_insert_$sug")
                    ) {
                        Text(
                            text = sug,
                            color = CosmicAccentCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // SPLIT SCREEN CONTENT PANEL OR MAIN COMPILER CANVAS
        val weights = if (isSplit) Pair(0.52f, 0.48f) else Pair(1f, 0f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(weights.first)
                .background(CosmicDeepNavy)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Line Number Indicators Left Panel
                LineNumbersPane(allText = editorText, fontSize = zoomVal)

                // Native Kotlin-Compose Python source code write Canvas
                BasicTextField(
                    value = editorText,
                    onValueChange = { viewModel.updateEditorContent(it) },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = zoomVal.sp,
                        color = SyntaxText
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("code_editor_field"),
                    cursorBrush = SolidColor(CosmicAccentCyan),
                    visualTransformation = PythonSyntaxVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default
                    )
                )
            }
            
            // KEYBOARD QUICK INSERT PANEL
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                QuickInsertToolbar { symbol ->
                    val cursorInsertion = if (symbol == "INDENT") "    " else symbol
                    viewModel.updateEditorContent(editorText + cursorInsertion)
                }
            }
        }

        // DETACHABLE OR FLOATING SPLIT SCREEN SYSTEM - REPRESENTS THE TERMINAL / STACK
        if (isSplit || isRunningState != InterpreterState.Idle) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isSplit) weights.second else if (isRunningState != InterpreterState.Idle) 0.35f else 0.001f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(CosmicConsoleBg)
                ) {
                    // Header console actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmicHeaderBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = CosmicAccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Python 3 Compiler Terminal",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicWhite
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isRunningState is InterpreterState.Running || isRunningState is InterpreterState.WaitingForInput) {
                                TextButton(onClick = { viewModel.stopPythonExecution() }) {
                                    Text("STOP", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(
                                onClick = { viewModel.consoleOutputs.value = emptyList() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Terminal", tint = CosmicGray, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { viewModel.isSplitScreen.value = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Collapse Console", tint = CosmicGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Console output feeds
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(8.dp),
                        contentPadding = PaddingValues(bottom = 50.dp)
                    ) {
                        items(consoleOuts) { output ->
                            when (output) {
                                is ConsoleOutput.Standard -> {
                                    Text(
                                        text = output.text,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = SyntaxText,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                                is ConsoleOutput.InputPrompt -> {
                                    Text(
                                        text = output.text,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = CosmicAccentCyan,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                                is ConsoleOutput.Error -> {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0x33FF0033)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = output.text,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color(0xFFFF4D4D),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                                is ConsoleOutput.System -> {
                                    Text(
                                        text = output.text,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color(0xFF66C2FF),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Input Waiting element
                        if (isRunningState is InterpreterState.WaitingForInput) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = CosmicAccentCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    OutlinedTextField(
                                        value = consoleInputVal,
                                        onValueChange = { consoleInputVal = it },
                                        placeholder = { Text("Enter response value", fontSize = 11.sp, color = CosmicGray) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("console_terminal_input"),
                                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CosmicWhite),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CosmicAccentCyan,
                                            unfocusedBorderColor = CosmicGray
                                        ),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions(onSend = {
                                            viewModel.sendConsoleInput(consoleInputVal)
                                            consoleInputVal = ""
                                        })
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            viewModel.sendConsoleInput(consoleInputVal)
                                            consoleInputVal = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CosmicAccentCyan),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Submit", fontSize = 10.sp, color = CosmicDeepNavy, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LineNumbersPane(allText: String, fontSize: Float) {
    val len = allText.split("\n").size
    Column(
        modifier = Modifier
            .width(36.dp)
            .fillMaxHeight()
            .background(Color(0xFF0A0A0C))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (i in 1..len) {
            Text(
                text = "$i",
                color = CosmicGray.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
fun QuickInsertToolbar(onInsert: (String) -> Unit) {
    val items = listOf("INDENT", ":", "(", ")", "[", "]", "'", "\"", "=", "+", "-", "*")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(Color(0xFF121214))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { sym ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                    .clickable { onInsert(sym) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("quick_insert_$sym")
            ) {
                Text(
                    text = sym,
                    color = CosmicWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ================= PRACTICAL TUTORIALS =================
@Composable
fun TutorialsScreen(viewModel: PyCodeViewModel) {
    val completedProgress by viewModel.allProgress.collectAsState()
    val playList = PythonLessons.playlist

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Scholar tutorial headers
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "Interactive Lessons Playground",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = CosmicAccentCyan
                    )
                )
                Text(
                    text = "Learn Python offline through active compilations during coding assignments.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = CosmicGray)
                )
            }
        }

        items(playList) { lesson ->
            val hasCompleted = completedProgress.any { it.lessonId == lesson.id && it.isCompleted }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("lesson_card_${lesson.id}"),
                colors = CardDefaults.cardColors(containerColor = CosmicCardBg.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (hasCompleted) Color(0xFF00FFB3).copy(alpha = 0.4f) else CosmicNeonBlue.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (hasCompleted) Color(0xFF00FFB3).copy(alpha = 0.12f) else CosmicPurple.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = lesson.difficulty,
                                color = if (hasCompleted) Color(0xFF00FFB3) else CosmicAccentCyan,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        if (hasCompleted) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00FFB3), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("COMPLETED", fontSize = 11.sp, color = Color(0xFF00FFB3), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "+${lesson.xpReward} XP",
                                color = CosmicNeonBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CosmicWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = lesson.story,
                        style = MaterialTheme.typography.bodySmall.copy(color = CosmicGray)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.selectLesson(lesson) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("start_challenge_btn_${lesson.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasCompleted) CosmicCardBg else CosmicNeonBlue
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = if (hasCompleted) BorderStroke(1.dp, CosmicGray.copy(alpha = 0.3f)) else null
                    ) {
                        Text(
                            text = if (hasCompleted) "Replay Playground" else "Start Exercise",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ================= LOCAL PROFILE =================
@Composable
fun ProfileScreen(viewModel: PyCodeViewModel) {
    val totalProgressXp by viewModel.totalXp.collectAsState()
    val progressList by viewModel.allProgress.collectAsState()
    val allProjectsList by viewModel.allProjects.collectAsState()

    val levelValue = (totalProgressXp / 500) + 1
    val completedExercisesCount = progressList.filter { it.isCompleted }.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(CosmicAccentCyan.copy(alpha = 0.12f), CircleShape)
                        .border(2.dp, CosmicAccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PY",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicAccentCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Offline Scholar: dev#grid",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CosmicWhite
                    )
                )

                Text(
                    text = "Sub-grid Offline compilation terminal",
                    style = MaterialTheme.typography.bodySmall.copy(color = CosmicGray)
                )
            }
        }

        // Leaderboard Statistics Board
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicCardBg.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CosmicNeonBlue.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Scholar Milestones",
                        fontWeight = FontWeight.Bold,
                        color = CosmicAccentCyan,
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatItem(
                            modifier = Modifier.weight(1f),
                            metric = "$totalProgressXp",
                            label = "Accumulated XP"
                        )
                        ProfileStatItem(
                            modifier = Modifier.weight(1f),
                            metric = "Level $levelValue",
                            label = "Scholar Rank"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatItem(
                            modifier = Modifier.weight(1f),
                            metric = "$completedExercisesCount / 5",
                            label = "Tutorials Solved"
                        )
                        ProfileStatItem(
                            modifier = Modifier.weight(1f),
                            metric = "${allProjectsList.size}",
                            label = "Locally Saves"
                        )
                    }
                }
            }
        }

        // Offline milestones/achievements
        item {
            Column(
                modifier = Modifier
                    .fillParentMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Unlocked Scholar Badges",
                    fontWeight = FontWeight.Bold,
                    color = CosmicWhite,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                AchievementBadgeRow(
                    label = "Hello Compiler",
                    desc = "Printed 'Welcome to Python' offline successfully.",
                    unlocked = completedExercisesCount >= 1
                )
                AchievementBadgeRow(
                    label = "Decision Maker",
                    desc = "Solved nested conditional logic branch evaluation.",
                    unlocked = completedExercisesCount >= 3
                )
                AchievementBadgeRow(
                    label = "Master Loop",
                    desc = "Analyzed star structures loops without stack failures.",
                    unlocked = completedExercisesCount >= 5
                )
            }
        }
    }
}

@Composable
fun ProfileStatItem(
    modifier: Modifier = Modifier,
    metric: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F111E)),
        border = BorderStroke(0.5.dp, CosmicGray.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = metric,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CosmicAccentCyan
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = CosmicGray
            )
        }
    }
}

@Composable
fun AchievementBadgeRow(
    label: String,
    desc: String,
    unlocked: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) CosmicCardBg else CosmicCardBg.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (unlocked) CosmicAccentCyan.copy(alpha = 0.3f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (unlocked) CosmicAccentCyan.copy(alpha = 0.15f) else Color(0xFF1E2430),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = if (unlocked) CosmicAccentCyan else CosmicGray.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    color = if (unlocked) CosmicWhite else CosmicGray.copy(alpha = 0.4f)
                )
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = CosmicGray
                )
            }
        }
    }
}

// ================= SETTINGS SCREEN =================
@Composable
fun SettingsScreen(viewModel: PyCodeViewModel) {
    val isDark by viewModel.isDarkMode.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "Configuration Panel",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = CosmicAccentCyan
                    )
                )
                Text(
                    text = "Preferences and diagnostic instructions of PyCode Studio IDE.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = CosmicGray)
                )
            }
        }

        // Appearance section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicCardBg.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CosmicNeonBlue.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Appearance Theme Options",
                        fontWeight = FontWeight.Bold,
                        color = CosmicAccentCyan,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Futuristic Dark Mode", color = CosmicWhite)
                            Text(
                                text = "Enables ocular protection cosmic themes.",
                                style = MaterialTheme.typography.bodySmall.copy(color = CosmicGray)
                            )
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { viewModel.isDarkMode.value = it },
                            modifier = Modifier.testTag("toggle_dark_theme")
                        )
                    }
                }
            }
        }

        // Guide instructions section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicCardBg.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CosmicNeonBlue.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Keyboard Short-cuts Guide",
                        fontWeight = FontWeight.Bold,
                        color = CosmicAccentCyan,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShortcutTextRow(keys = "Ctrl + Enter", purpose = "Run Python script")
                    ShortcutTextRow(keys = "Ctrl + Z", purpose = "Undo last text sequence")
                    ShortcutTextRow(keys = "Ctrl + Y", purpose = "Redo last text sequence")
                    ShortcutTextRow(keys = "Tab Drawer", purpose = "Insert 4-spaces indentation")
                    ShortcutTextRow(keys = "Voice Button", purpose = "Trigger speech compilation")
                }
            }
        }

        // Diagnostics
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicCardBg.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Danger Diagnostics",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This erases all user templates, deletes local sandbox files, and resets XP logs.",
                        style = MaterialTheme.typography.bodySmall.copy(color = CosmicGray)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_user_data_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reset Storage & Cache", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Erase Storage & Sandbox?", color = CosmicWhite, fontWeight = FontWeight.Bold) },
            text = { Text("Are you absolutely certain? This deleted everything and cannot be undone.", color = CosmicGray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllUserData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    modifier = Modifier.testTag("confirm_reset_btn")
                ) {
                    Text("Confirm Erase", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = CosmicGray)
                }
            },
            containerColor = CosmicCardBg
        )
    }
}

@Composable
fun ShortcutTextRow(keys: String, purpose: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = purpose, color = CosmicWhite, fontSize = 13.sp)
        Box(
            modifier = Modifier
                .background(Color(0xFF1E2430), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = keys,
                color = CosmicAccentCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

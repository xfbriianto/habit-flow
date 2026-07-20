package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Habit
import com.example.data.HabitLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: HabitViewModel) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val lastSyncCode by viewModel.lastSyncCode.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Habits, 1 = Stats, 2 = Cloud Sync
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedHabitForDetail by remember { mutableStateOf<Habit?>(null) }

    // Request notification permissions for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Reminders enabled! Notification permission recommended for alerts.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { /* Menu */ },
                        modifier = Modifier.testTag("menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                title = {
                    Text(
                        text = "Habit Flow",
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    // Export CSV Button
                    IconButton(
                        onClick = { viewModel.exportStatsAndShare(context) },
                        modifier = Modifier.testTag("export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Statistics",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Theme Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = Color(0xFF49454F).copy(alpha = 0.5f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(if (activeTab == 0) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, contentDescription = "Today's Habits") },
                    label = { Text("Habits") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF21005D),
                        selectedTextColor = Color(0xFFE6E1E5),
                        unselectedIconColor = Color(0xFFCAC4D0),
                        unselectedTextColor = Color(0xFFCAC4D0),
                        indicatorColor = Color(0xFFEADDFF)
                    ),
                    modifier = Modifier.testTag("tab_today")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(if (activeTab == 1) Icons.Filled.BarChart else Icons.Outlined.BarChart, contentDescription = "Progress Statistics") },
                    label = { Text("Insights") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF21005D),
                        selectedTextColor = Color(0xFFE6E1E5),
                        unselectedIconColor = Color(0xFFCAC4D0),
                        unselectedTextColor = Color(0xFFCAC4D0),
                        indicatorColor = Color(0xFFEADDFF)
                    ),
                    modifier = Modifier.testTag("tab_stats")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(if (activeTab == 2) Icons.Filled.CloudSync else Icons.Outlined.CloudSync, contentDescription = "Cloud Sync") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF21005D),
                        selectedTextColor = Color(0xFFE6E1E5),
                        unselectedIconColor = Color(0xFFCAC4D0),
                        unselectedTextColor = Color(0xFFCAC4D0),
                        indicatorColor = Color(0xFFEADDFF)
                    ),
                    modifier = Modifier.testTag("tab_sync")
                )
            }
        },
        floatingActionButton = {
            if (activeTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Create Habit") },
                    text = { Text("New Habit") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .testTag("add_habit_fab")
                        .navigationBarsPadding()
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                0 -> HabitsScreen(
                    viewModel = viewModel,
                    habits = habits,
                    logs = logs,
                    selectedDate = selectedDate,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    onHabitClick = { selectedHabitForDetail = it }
                )
                1 -> StatisticsScreen(
                    viewModel = viewModel,
                    habits = habits,
                    logs = logs
                )
                2 -> SyncScreen(
                    viewModel = viewModel,
                    syncing = syncing,
                    lastSyncCode = lastSyncCode,
                    syncMessage = syncMessage
                )
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, cat, freq, time ->
                viewModel.addHabit(title, desc, cat, freq, time)
                showAddDialog = false
            }
        )
    }

    if (selectedHabitForDetail != null) {
        HabitDetailDialog(
            habit = selectedHabitForDetail!!,
            logs = logs.filter { it.habitId == selectedHabitForDetail!!.id },
            onDismiss = { selectedHabitForDetail = null },
            onDelete = {
                viewModel.deleteHabit(selectedHabitForDetail!!)
                selectedHabitForDetail = null
            }
        )
    }
}

// Today's Habits List Screen
@Composable
fun HabitsScreen(
    viewModel: HabitViewModel,
    habits: List<Habit>,
    logs: List<HabitLog>,
    selectedDate: String,
    searchQuery: String,
    selectedCategory: String,
    onHabitClick: (Habit) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Week Date Selector Slider
        WeekCalendarSlider(
            selectedDate = selectedDate,
            onDateSelected = { viewModel.setSelectedDate(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search & Filtering Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search habits...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("search_bar"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Buttons Row
        CategoryFiltersRow(
            selectedCategory = selectedCategory,
            onCategorySelected = { viewModel.setSelectedCategory(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Process Habits filtering
        val filteredHabits = habits.filter { habit ->
            val matchesSearch = habit.title.contains(searchQuery, ignoreCase = true) ||
                    habit.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || habit.category == selectedCategory
            matchesSearch && matchesCategory
        }

        // Daily Progress Bar
        val habitsForToday = filteredHabits
        val completionsTodayCount = habitsForToday.count { h ->
            logs.any { log -> log.habitId == h.id && log.completedDate == selectedDate }
        }
        val progressRatio = if (habitsForToday.isNotEmpty()) {
            completionsTodayCount.toFloat() / habitsForToday.size
        } else {
            0f
        }

        DailyProgressHeader(
            completedCount = completionsTodayCount,
            totalCount = habitsForToday.size,
            progress = progressRatio
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Empty state check
        if (filteredHabits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DoneAll,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedCategory != "All") "No habits match filters" else "No habits created yet",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Tap 'New Habit' below to kickstart your routine!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredHabits, key = { it.id }) { habit ->
                    val isCompleted = logs.any { it.habitId == habit.id && it.completedDate == selectedDate }
                    HabitCard(
                        habit = habit,
                        isCompleted = isCompleted,
                        onToggle = { viewModel.toggleHabitCompletion(habit.id, selectedDate) },
                        onClick = { onHabitClick(habit) }
                    )
                }
            }
        }
    }
}

// Week Slider component
@Composable
fun WeekCalendarSlider(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayFormat = SimpleDateFormat("EEE", Locale.US)
    val numFormat = SimpleDateFormat("d", Locale.US)

    // Calculate the last 7 days including today
    val dates = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        // Start 5 days ago to 2 days ahead for convenient navigation
        cal.add(Calendar.DAY_OF_YEAR, -5)
        for (i in 0 until 8) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (date in dates) {
            val dateStr = sdf.format(date)
            val isSelected = dateStr == selectedDate
            val isToday = sdf.format(Date()) == dateStr

            val containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                else -> MaterialTheme.colorScheme.surface
            }

            val contentColor = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isToday -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurface
            }

            val borderColor = if (isToday && !isSelected) {
                MaterialTheme.colorScheme.secondary
            } else {
                Color.Transparent
            }

            Card(
                onClick = { onDateSelected(dateStr) },
                colors = CardDefaults.cardColors(containerColor = containerColor),
                modifier = Modifier
                    .width(58.dp)
                    .height(72.dp)
                    .testTag("date_card_$dateStr"),
                shape = RoundedCornerShape(16.dp),
                border = if (borderColor != Color.Transparent) BorderStroke(1.5.dp, borderColor) else null
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayFormat.format(date).uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = numFormat.format(date),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

// Filters row
@Composable
fun CategoryFiltersRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("All", "Health", "Productivity", "Mind", "Fitness")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (cat in categories) {
            val isSelected = selectedCategory == cat
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(cat) },
                label = { Text(cat) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("filter_chip_$cat")
            )
        }
    }
}

// Progress Bar Header
@Composable
fun DailyProgressHeader(
    completedCount: Int,
    totalCount: Int,
    progress: Float
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("progress_header")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .drawBehind {
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.15f),
                            style = Stroke(width = 6.dp.toPx())
                        )
                        drawArc(
                            color = if (progress >= 1f) Color(0xFF00F09F) else Color(0xFF6366F1),
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = if (progress >= 1f) "Splendid! All set today! 🎉" else "Routine Progress",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "$completedCount of $totalCount habits completed",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (progress >= 1f) Color(0xFF00F09F) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            }
        }
    }
}

// Habit Card
@Composable
fun HabitCard(
    habit: Habit,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val categoryColor = when (habit.category) {
        "Health" -> Color(0xFF10B981)
        "Productivity" -> Color(0xFF3B82F6)
        "Mind" -> Color(0xFF8B5CF6)
        "Fitness" -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B)
    }

    val categoryIcon = when (habit.category) {
        "Health" -> Icons.Default.Favorite
        "Productivity" -> Icons.Default.Work
        "Mind" -> Icons.Default.SelfImprovement
        "Fitness" -> Icons.Default.FitnessCenter
        else -> Icons.Default.Star
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = habit.category,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (habit.description.isNotEmpty()) {
                    Text(
                        text = habit.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Flame Streak Icon
                    if (habit.streakCount > 0) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak Count",
                            tint = Color(0xFFFF9F1C),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${habit.streakCount} day streak",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9F1C),
                            modifier = Modifier.padding(start = 2.dp, end = 8.dp)
                        )
                    }

                    // Reminder Badge
                    if (habit.reminderTime != null) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notification Reminder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = habit.reminderTime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            // Interactive Checkbox with Ripple feedback and size 48dp+ accessibility
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("toggle_habit_${habit.id}")
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Toggle completion",
                    tint = if (isCompleted) Color(0xFF00F09F) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// Featured Streak Card matching Immersive UI Design Section
@Composable
fun FeaturedStreakCard(
    currentStreak: Int,
    bestStreak: Int
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF381E72), Color(0xFF1C1B1F))
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFD0BCFF).copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(300f, -50f),
                        radius = 400f
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "CURRENT STREAK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFFD0BCFF),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = currentStreak.toString(),
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 58.sp
                    )
                    Text(
                        text = "days",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCCC48E),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFFCAC4D0),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Personal best: $bestStreak days",
                        fontSize = 12.sp,
                        color = Color(0xFFCAC4D0),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Statistics Tab Screen
@Composable
fun StatisticsScreen(
    viewModel: HabitViewModel,
    habits: List<Habit>,
    logs: List<HabitLog>
) {
    val context = LocalContext.current
    val activeStreak = habits.maxOfOrNull { it.streakCount } ?: 0
    val bestStreak = habits.maxOfOrNull { it.maxStreak } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Progress Analytics",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Sticking to daily resolutions",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        FeaturedStreakCard(currentStreak = activeStreak, bestStreak = bestStreak)

        Spacer(modifier = Modifier.height(12.dp))

        // Grid Stats summary
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatSummaryCard(
                title = "Total Habits",
                value = habits.size.toString(),
                icon = Icons.Default.ListAlt,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatSummaryCard(
                title = "Best Streak",
                value = "$bestStreak days",
                icon = Icons.Default.LocalFireDepartment,
                color = Color(0xFFFF9F1C),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val totalCompletions = logs.size
            StatSummaryCard(
                title = "All Logs",
                value = totalCompletions.toString(),
                icon = Icons.Default.DoneAll,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            val avgStreak = if (habits.isNotEmpty()) "%.1f".format(habits.map { it.streakCount }.average()) else "0"
            StatSummaryCard(
                title = "Avg Streak",
                value = "$avgStreak days",
                icon = Icons.Default.TrendingUp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Custom Canvas 7-Day Completion Rates Chart
        Text(
            text = "7-Day Completion Rates",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Last7DaysChart(logs = logs, habitsCount = habits.size)

        Spacer(modifier = Modifier.height(20.dp))

        // Share & Export Callout Card with Immersive UI Styling
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Show off your consistency!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Export fully formatted CSV analytics to share with family and friends and keep yourself accountable.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.exportStatsAndShare(context) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEADDFF),
                        contentColor = Color(0xFF21005D)
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Export Stats", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// 7-day completion rates custom chart
@Composable
fun Last7DaysChart(logs: List<HabitLog>, habitsCount: Int) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFormat = SimpleDateFormat("E", Locale.US)
    
    // Get past 7 dates
    val past7Dates = remember(logs) {
        val list = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0 until 7) {
            list.add(Pair(sdf.format(cal.time), labelFormat.format(cal.time)))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .testTag("7_day_chart")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            for (datePair in past7Dates) {
                val compCount = logs.count { it.completedDate == datePair.first }
                val ratio = if (habitsCount > 0) compCount.toFloat() / habitsCount else 0f
                val animatedRatio by animateFloatAsState(
                    targetValue = ratio,
                    animationSpec = tween(durationMillis = 500)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${(ratio * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ratio >= 1f) Color(0xFF00F09F) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Draw vertical bar on canvas
                    Canvas(
                        modifier = Modifier
                            .width(16.dp)
                            .height(110.dp)
                    ) {
                        val barHeight = size.height * animatedRatio
                        drawRoundRect(
                            color = Color.LightGray.copy(alpha = 0.15f),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                        if (barHeight > 0) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = if (ratio >= 1f) {
                                        listOf(Color(0xFF00F09F), Color(0xFF05B075))
                                    } else {
                                        listOf(Color(0xFF818CF8), Color(0xFF4F46E5))
                                    }
                                ),
                                topLeft = Offset(0f, size.height - barHeight),
                                size = Size(size.width, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = datePair.second.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// Statistical Summary Card Component
@Composable
fun StatSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.testTag("stat_card_${title.lowercase().replace(" ", "_")}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Cloud Sync Tab Screen
@Composable
fun SyncScreen(
    viewModel: HabitViewModel,
    syncing: Boolean,
    lastSyncCode: String,
    syncMessage: String?
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var inputSyncCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Cross-Device Sync",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Synchronize habits flawlessly across phones or tablets.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Sync Messages Callout
        AnimatedVisibility(visible = syncMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = syncMessage ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearSyncMessage() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Action 1: Upload Backup
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Upload Backup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Generates a unique secure code. Keep your habits safe on cloud.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterHorizontally),
                        strokeWidth = 2.dp
                    )
                } else {
                    Button(
                        onClick = {
                            viewModel.generateCloudSync { code ->
                                Toast.makeText(context, "Backup uploaded! Code copied.", Toast.LENGTH_SHORT).show()
                                clipboardManager.setText(AnnotatedString(code))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_backup_btn")
                    ) {
                        Text("Backup Now")
                    }
                }

                if (lastSyncCode.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Active Sync Code", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(lastSyncCode, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(lastSyncCode))
                                Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Copy Code", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action 2: Download / Sync Code
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Restore / Import Cloud Data",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Type an 8-digit code from your other device to pull and restore habits.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = inputSyncCode,
                    onValueChange = { inputSyncCode = it },
                    placeholder = { Text("E.g. ABCD-EFGH", fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restore_code_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (inputSyncCode.trim().isNotEmpty()) {
                            viewModel.syncFromCloud(inputSyncCode) { success ->
                                if (success) {
                                    Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_LONG).show()
                                    inputSyncCode = ""
                                } else {
                                    Toast.makeText(context, "Failed to restore data. Check code.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Enter a valid code.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restore_backup_btn"),
                    enabled = !syncing
                ) {
                    Text("Sync & Restore")
                }
            }
        }
    }
}

// Add Habit Dialog Component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Health") }
    var frequency by remember { mutableStateOf("Daily") }
    
    var enableReminder by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(30) }

    val categories = listOf("Health", "Productivity", "Mind", "Fitness", "General")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_habit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Track a New Habit",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Habit Title") },
                    placeholder = { Text("E.g. Drink water") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_habit_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("E.g. 8 glasses a day") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_habit_desc_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Category selector buttons
                Text("Select Category", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (cat in categories) {
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_dialog_cat_$cat")
                        )
                    }
                }

                // Daily Reminders Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Daily Reminder Alert", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Triggers a system alarm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = enableReminder,
                        onCheckedChange = { enableReminder = it },
                        modifier = Modifier.testTag("add_habit_reminder_switch")
                    )
                }

                if (enableReminder) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Trigger at: ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Minute / hour spinner fallback
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedCard(onClick = { hour = (hour + 1) % 24 }, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = String.format("%02d", hour),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Text(" : ", fontSize = 18.sp, fontWeight = FontWeight.Black)
                            OutlinedCard(onClick = { minute = (minute + 5) % 60 }, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = String.format("%02d", minute),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("add_dialog_cancel")) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.trim().isNotEmpty()) {
                                val reminderStr = if (enableReminder) String.format("%02d:%02d", hour, minute) else null
                                onConfirm(title, desc, selectedCategory, frequency, reminderStr)
                            }
                        },
                        enabled = title.trim().isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_dialog_save")
                    ) {
                        Text("Save Habit")
                    }
                }
            }
        }
    }
}

// Habit Detail & Streak visual calendar dialog
@Composable
fun HabitDetailDialog(
    habit: Habit,
    logs: List<HabitLog>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

    // Calculate completions in a grid representation for last 30 days
    val dates30Days = remember(logs) {
        val list = mutableListOf<Pair<String, Boolean>>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -29)
        for (i in 0 until 30) {
            val dateStr = sdf.format(cal.time)
            val completed = logs.any { it.completedDate == dateStr }
            list.add(Pair(dateStr, completed))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("habit_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = habit.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Category: ${habit.category}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_habit_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Habit", tint = Color.Red)
                    }
                }

                if (habit.description.isNotEmpty()) {
                    Text(
                        text = habit.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Streaks Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9F1C).copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF9F1C))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Current Streak", fontSize = 10.sp, color = Color(0xFFFF9F1C))
                            Text("${habit.streakCount} days", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9F1C))
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Longest Streak", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            Text("${habit.maxStreak} days", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Visual Calendar Grid
                Text(
                    text = "Consistency Map (Last 30 Days)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Grid mapping out the completions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        text = monthFormat.format(Date()),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 5x6 or simple wrapping grid for 30 days
                    val columnsCount = 6
                    val rowsCount = 5

                    for (row in 0 until rowsCount) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until columnsCount) {
                                val index = row * columnsCount + col
                                if (index < dates30Days.size) {
                                    val (dateStr, completed) = dates30Days[index]
                                    val dayNum = dateStr.split("-").last().toInt().toString()

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (completed) Color(0xFF00F09F) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (completed) Color(0xFF0E0F14) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(34.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_dialog_close")
                ) {
                    Text("Close Details")
                }
            }
        }
    }
}

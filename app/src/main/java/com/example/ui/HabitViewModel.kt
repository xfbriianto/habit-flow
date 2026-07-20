package com.example.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val database = HabitDatabase.getDatabase(application)
    private val repository = HabitRepository(database.habitDao())

    // All habits and completion logs
    val habits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<HabitLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings / UI state
    private val sharedPrefs = application.getSharedPreferences("habit_tracker_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("dark_mode", true)) // Default to dark mode for elegant aesthetic
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Sync State
    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _lastSyncCode = MutableStateFlow(sharedPrefs.getString("last_sync_code", "") ?: "")
    val lastSyncCode: StateFlow<String> = _lastSyncCode.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        sharedPrefs.edit().putBoolean("dark_mode", newValue).apply()
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    // CRUD operations
    fun addHabit(title: String, description: String, category: String, frequency: String, reminderTime: String?) {
        viewModelScope.launch {
            val newHabit = Habit(
                title = title.trim(),
                description = description.trim(),
                category = category,
                frequency = frequency,
                reminderTime = reminderTime
            )
            val generatedId = repository.insertHabit(newHabit)
            val habitWithId = newHabit.copy(id = generatedId.toInt())
            if (reminderTime != null) {
                scheduleAlarmForHabit(habitWithId)
            }
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            if (habit.reminderTime != null) {
                scheduleAlarmForHabit(habit)
            } else {
                cancelAlarmForHabit(habit)
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            cancelAlarmForHabit(habit)
            repository.deleteHabit(habit)
        }
    }

    fun toggleHabitCompletion(habitId: Int, date: String) {
        viewModelScope.launch {
            repository.toggleHabitCompletion(habitId, date)
        }
    }

    // Alarm scheduler
    private fun scheduleAlarmForHabit(habit: Habit) {
        val reminder = habit.reminderTime ?: return
        val parts = reminder.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("habit_title", habit.title)
            putExtra("habit_desc", habit.description)
            putExtra("habit_id", habit.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarmForHabit(habit: Habit) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    // Cloud Sync logic via kvdb.io
    fun generateCloudSync(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            _syncing.value = true
            _syncMessage.value = "Preparing backup..."
            try {
                val currentHabits = habits.value
                val currentLogs = logs.value

                val json = getBackupJson(currentHabits, currentLogs)
                val randomCode = generateRandomSyncCode()

                val success = uploadBackup(randomCode, json)
                if (success) {
                    _lastSyncCode.value = randomCode
                    sharedPrefs.edit().putString("last_sync_code", randomCode).apply()
                    _syncMessage.value = "Backup uploaded successfully! Code: $randomCode"
                    onComplete(randomCode)
                } else {
                    _syncMessage.value = "Failed to upload backup to cloud."
                }
            } catch (e: Exception) {
                _syncMessage.value = "Error during backup upload: ${e.localizedMessage}"
            } finally {
                _syncing.value = false
            }
        }
    }

    fun syncFromCloud(code: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _syncing.value = true
            _syncMessage.value = "Fetching backup for code $code..."
            try {
                val json = downloadBackup(code.trim().uppercase())
                if (json != null) {
                    val (importedHabits, importedLogs) = parseBackupJson(json)
                    repository.importBackup(importedHabits, importedLogs)
                    
                    // Reschedule alarms for any imported habits
                    for (habit in importedHabits) {
                        if (habit.reminderTime != null) {
                            scheduleAlarmForHabit(habit)
                        }
                    }

                    _lastSyncCode.value = code.trim().uppercase()
                    sharedPrefs.edit().putString("last_sync_code", code.trim().uppercase()).apply()
                    _syncMessage.value = "Cloud sync successful! Restored ${importedHabits.size} habits."
                    onComplete(true)
                } else {
                    _syncMessage.value = "No backup found or invalid sync code."
                    onComplete(false)
                }
            } catch (e: Exception) {
                _syncMessage.value = "Error restoring cloud backup: ${e.localizedMessage}"
                onComplete(false)
            } finally {
                _syncing.value = false
            }
        }
    }

    private fun generateRandomSyncCode(): String {
        val chars = "ABCDEFGHJKLMNOPQRSTUVWXYZ23456789" // Exclude confusing letters/numbers like I, 1, O, 0
        val random = Random()
        val sb = StringBuilder()
        for (i in 0 until 8) {
            sb.append(chars[random.nextInt(chars.length)])
            if (i == 3) sb.append("-")
        }
        return sb.toString()
    }

    private suspend fun uploadBackup(code: String, json: String): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://kvdb.io/habittracker_sync_yzwvpx/$code")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
            }
            val responseCode = connection.responseCode
            responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun downloadBackup(code: String): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://kvdb.io/habittracker_sync_yzwvpx/$code")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun getBackupJson(habitsList: List<Habit>, logsList: List<HabitLog>): String {
        val root = JSONObject()
        val habitsArray = JSONArray()
        for (h in habitsList) {
            val hObj = JSONObject()
            hObj.put("id", h.id)
            hObj.put("title", h.title)
            hObj.put("description", h.description)
            hObj.put("category", h.category)
            hObj.put("frequency", h.frequency)
            hObj.put("reminderTime", h.reminderTime ?: JSONObject.NULL)
            hObj.put("createdAt", h.createdAt)
            hObj.put("streakCount", h.streakCount)
            hObj.put("maxStreak", h.maxStreak)
            hObj.put("lastCompletedDate", h.lastCompletedDate ?: JSONObject.NULL)
            hObj.put("targetValue", h.targetValue)
            hObj.put("isArchived", h.isArchived)
            habitsArray.put(hObj)
        }

        val logsArray = JSONArray()
        for (l in logsList) {
            val lObj = JSONObject()
            lObj.put("id", l.id)
            lObj.put("habitId", l.habitId)
            lObj.put("completedDate", l.completedDate)
            lObj.put("completedTimestamp", l.completedTimestamp)
            lObj.put("value", l.value)
            logsArray.put(lObj)
        }

        root.put("habits", habitsArray)
        root.put("logs", logsArray)
        return root.toString()
    }

    private fun parseBackupJson(jsonStr: String): Pair<List<Habit>, List<HabitLog>> {
        val root = JSONObject(jsonStr)
        val habitsArray = root.getJSONArray("habits")
        val logsArray = root.getJSONArray("logs")

        val habitsList = mutableListOf<Habit>()
        for (i in 0 until habitsArray.length()) {
            val hObj = habitsArray.getJSONObject(i)
            val h = Habit(
                id = hObj.optInt("id", 0),
                title = hObj.getString("title"),
                description = hObj.optString("description", ""),
                category = hObj.optString("category", "General"),
                frequency = hObj.optString("frequency", "Daily"),
                reminderTime = if (hObj.isNull("reminderTime")) null else hObj.getString("reminderTime"),
                createdAt = hObj.optLong("createdAt", System.currentTimeMillis()),
                streakCount = hObj.optInt("streakCount", 0),
                maxStreak = hObj.optInt("maxStreak", 0),
                lastCompletedDate = if (hObj.isNull("lastCompletedDate")) null else hObj.getString("lastCompletedDate"),
                targetValue = hObj.optInt("targetValue", 1),
                isArchived = hObj.optBoolean("isArchived", false)
            )
            habitsList.add(h)
        }

        val logsList = mutableListOf<HabitLog>()
        for (i in 0 until logsArray.length()) {
            val lObj = logsArray.getJSONObject(i)
            val l = HabitLog(
                id = lObj.optInt("id", 0),
                habitId = lObj.getInt("habitId"),
                completedDate = lObj.getString("completedDate"),
                completedTimestamp = lObj.optLong("completedTimestamp", System.currentTimeMillis()),
                value = lObj.optInt("value", 1)
            )
            logsList.add(l)
        }

        return Pair(habitsList, logsList)
    }

    // Export statistics to share with friends as a CSV
    fun exportStatsAndShare(context: Context) {
        viewModelScope.launch {
            try {
                val currentHabits = habits.value
                val currentLogs = logs.value

                val csvContent = withContext(Dispatchers.Default) {
                    val sb = StringBuilder()
                    // Headers
                    sb.append("Habit Title,Category,Frequency,Current Streak,Longest Streak,Last Completed,Created Date,Total Completions\n")
                    
                    val dateSdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                    for (habit in currentHabits) {
                        val totalCompletions = currentLogs.count { it.habitId == habit.id }
                        val createdDate = dateSdf.format(Date(habit.createdAt))
                        val lastComp = habit.lastCompletedDate ?: "N/A"
                        
                        // Clean up strings to avoid CSV escaping errors
                        val cleanTitle = habit.title.replace(",", " ")
                        val cleanCategory = habit.category.replace(",", " ")
                        
                        sb.append("$cleanTitle,$cleanCategory,${habit.frequency},${habit.streakCount},${habit.maxStreak},$lastComp,$createdDate,$totalCompletions\n")
                    }
                    sb.toString()
                }

                // Write file to cache directory
                val cacheDir = context.cacheDir
                val exportFile = File(cacheDir, "habit_stats_export.csv")
                FileWriter(exportFile).use { writer ->
                    writer.write(csvContent)
                }

                // Share intent
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    exportFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "My Habit Statistics")
                    putExtra(Intent.EXTRA_TEXT, "Hey! Check out my habit progress stats. Sticking to my streaks! 🚀")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Share Stats with Friends").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

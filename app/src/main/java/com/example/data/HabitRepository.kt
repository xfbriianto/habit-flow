package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class HabitRepository(private val habitDao: HabitDao) {

    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allLogs: Flow<List<HabitLog>> = habitDao.getAllLogs()

    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>> {
        return habitDao.getLogsForHabit(habitId)
    }

    suspend fun insertHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteLogsForHabit(habit.id)
        habitDao.deleteHabit(habit)
    }

    suspend fun toggleHabitCompletion(habitId: Int, date: String): Boolean {
        val existingLog = habitDao.getLogByDate(habitId, date)
        val habit = habitDao.getHabitById(habitId) ?: return false

        if (existingLog != null) {
            // Delete the completion log
            habitDao.deleteLogByDate(habitId, date)
        } else {
            // Insert completion log
            habitDao.insertLog(HabitLog(habitId = habitId, completedDate = date))
        }

        // Recalculate streak!
        val logs = habitDao.getLogsForHabitDirect(habitId)
        val completedDates = logs.map { it.completedDate }
        val currentStreak = calculateStreak(completedDates, date)

        // Find the maximum streak
        val maxStreak = maxOf(habit.maxStreak, currentStreak)

        // Find the last completed date
        val lastCompleted = completedDates.maxOrNull()

        // Update habit record
        val updatedHabit = habit.copy(
            streakCount = currentStreak,
            maxStreak = maxStreak,
            lastCompletedDate = lastCompleted
        )
        habitDao.updateHabit(updatedHabit)
        return true
    }

    suspend fun clearAll() {
        habitDao.clearAllData()
    }

    suspend fun importBackup(habits: List<Habit>, logs: List<HabitLog>) {
        habitDao.clearAllData()
        for (habit in habits) {
            // Insert with existing properties (excluding generated id, or preserving it if needed)
            habitDao.insertHabit(habit)
        }
        for (log in logs) {
            habitDao.insertLog(log)
        }
    }

    fun calculateStreak(completedDates: List<String>, todayStr: String): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsedDates = completedDates.mapNotNull {
            try { sdf.parse(it) } catch (e: Exception) { null }
        }.distinct().sortedDescending()

        if (parsedDates.isEmpty()) return 0

        val todayDate = try { sdf.parse(todayStr) } catch (e: Exception) { Date() } ?: Date()

        // check if first completion is today or yesterday
        val firstCompleted = parsedDates.first()
        
        val cal1 = Calendar.getInstance().apply { time = firstCompleted }
        val cal2 = Calendar.getInstance().apply { time = todayDate }
        
        fun Calendar.clearTime() {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal1.clearTime()
        cal2.clearTime()
        
        val diffMillis = cal2.timeInMillis - cal1.timeInMillis
        val diffDays = diffMillis / (1000 * 60 * 60 * 24)
        
        if (diffDays > 1) {
            return 0 // Streak broken, last completion was before yesterday
        }
        
        var streak = 0
        val currentCal = Calendar.getInstance().apply { time = firstCompleted }
        currentCal.clearTime()
        
        for (date in parsedDates) {
            val dateCal = Calendar.getInstance().apply { time = date }
            dateCal.clearTime()
            
            if (dateCal.timeInMillis == currentCal.timeInMillis) {
                streak++
                currentCal.add(Calendar.DAY_OF_YEAR, -1)
            } else if (dateCal.timeInMillis < currentCal.timeInMillis) {
                break // Broken sequence
            }
        }
        
        return streak
    }
}

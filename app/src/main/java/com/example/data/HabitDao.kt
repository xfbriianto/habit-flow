package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Int): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY completedDate DESC")
    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY completedDate DESC")
    suspend fun getLogsForHabitDirect(habitId: Int): List<HabitLog>

    @Query("SELECT * FROM habit_logs ORDER BY completedDate DESC")
    fun getAllLogs(): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE completedDate = :date")
    suspend fun getLogsForDate(date: String): List<HabitLog>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND completedDate = :date LIMIT 1")
    suspend fun getLogByDate(habitId: Int, date: String): HabitLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLog): Long

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND completedDate = :date")
    suspend fun deleteLogByDate(habitId: Int, date: String)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsForHabit(habitId: Int)

    @Transaction
    suspend fun clearAllData() {
        // Handy for clean sync/reset
        queryClearAllLogs()
        queryClearAllHabits()
    }

    @Query("DELETE FROM habit_logs")
    suspend fun queryClearAllLogs()

    @Query("DELETE FROM habits")
    suspend fun queryClearAllHabits()
}

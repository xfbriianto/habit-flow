package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: String, // Health, Productivity, Mind, Fitness, Custom
    val frequency: String = "Daily", // Daily, Weekly
    val reminderTime: String? = null, // "08:30"
    val createdAt: Long = System.currentTimeMillis(),
    val streakCount: Int = 0,
    val maxStreak: Int = 0,
    val lastCompletedDate: String? = null, // "yyyy-MM-dd"
    val targetValue: Int = 1,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId"]), Index(value = ["completedDate"])]
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val completedDate: String, // "yyyy-MM-dd"
    val completedTimestamp: Long = System.currentTimeMillis(),
    val value: Int = 1
)

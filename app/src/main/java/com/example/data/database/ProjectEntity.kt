package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val content: String,
    val category: String = "All",
    val isFavorite: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val isRecent: Boolean = false,
    val isDefaultTemplate: Boolean = false
)

@Entity(tableName = "lesson_progress")
data class ProgressEntity(
    @PrimaryKey val lessonId: String,
    val isCompleted: Boolean = false,
    val userCodeSubmitted: String = "",
    val scoreAwarded: Int = 0,
    val completedAt: Long = System.currentTimeMillis()
)

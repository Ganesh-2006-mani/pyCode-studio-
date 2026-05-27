package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isFavorite = 1 ORDER BY lastModified DESC")
    fun getFavoriteProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isRecent = 1 ORDER BY lastModified DESC LIMIT 10")
    fun getRecentProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Int): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE isDefaultTemplate = 0")
    suspend fun deleteAllUserProjects()

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM lesson_progress")
    fun getAllProgress(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun getProgressForLesson(lessonId: String): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProgress(progress: ProgressEntity)

    @Query("SELECT SUM(scoreAwarded) FROM lesson_progress")
    fun getTotalXpFlow(): Flow<Int?>

    @Query("DELETE FROM lesson_progress")
    suspend fun clearProgress()
}

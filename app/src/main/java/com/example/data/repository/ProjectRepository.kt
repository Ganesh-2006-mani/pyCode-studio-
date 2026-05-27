package com.example.data.repository

import com.example.data.database.ProjectDao
import com.example.data.database.ProgressDao
import com.example.data.database.ProjectEntity
import com.example.data.database.ProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val progressDao: ProgressDao
) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val favoriteProjects: Flow<List<ProjectEntity>> = projectDao.getFavoriteProjects()
    val recentProjects: Flow<List<ProjectEntity>> = projectDao.getRecentProjects()
    val allProgress: Flow<List<ProgressEntity>> = progressDao.getAllProgress()
    val totalXp: Flow<Int?> = progressDao.getTotalXpFlow()

    suspend fun getProjectById(id: Int): ProjectEntity? = withContext(Dispatchers.IO) {
        projectDao.getProjectById(id)
    }

    suspend fun saveProject(project: ProjectEntity): Long = withContext(Dispatchers.IO) {
        projectDao.insertProject(project)
    }

    suspend fun updateProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        projectDao.deleteProject(project)
    }

    suspend fun getProgressForLesson(lessonId: String): ProgressEntity? = withContext(Dispatchers.IO) {
        progressDao.getProgressForLesson(lessonId)
    }

    suspend fun updateLessonProgress(progress: ProgressEntity) = withContext(Dispatchers.IO) {
        progressDao.updateProgress(progress)
    }

    suspend fun checkAndPrepopulate() = withContext(Dispatchers.IO) {
        val count = projectDao.getProjectCount()
        if (count == 0) {
            val defaultProjects = listOf(
                ProjectEntity(
                    name = "hello_world.py",
                    content = """# Welcome to PyCode Studio!
# Run this default project to see interactive mobile code execution.

print("Hello! Welcome to PyCode Studio - The Offline IDE.")
username = input("Please enter your name: ")
print(f"Awesome, welcome to coding {username}!")
print("PyCode Studio executes your script completely offline.")
""",
                    category = "Basics",
                    isFavorite = true,
                    isRecent = true,
                    isDefaultTemplate = true
                ),
                ProjectEntity(
                    name = "fibonacci.py",
                    content = """# Fibonacci Sequence Generator
# Computes fibonacci sequence up to N terms offline.

print("===== FIBONACCI SERIES =====")
limit_input = input("Enter number of terms (default 10): ")
limit = int(limit_input) if limit_input else 10

print(f"Generating first {limit} terms:")
a, b = 0, 1
count = 0

while count < limit:
    print(f"Term {count + 1}: {a}")
    nth = a + b
    # Update values
    a = b
    b = nth
    count += 1

print("Successful execution!")
""",
                    category = "Algorithms",
                    isFavorite = true,
                    isRecent = true,
                    isDefaultTemplate = true
                ),
                ProjectEntity(
                    name = "number_guesser.py",
                    content = """# Interactive Number Guessing Game
# Uses custom offline console console input!

print("===== NUMBER GUESSING GAME =====")
secret_number = 42
guesses_taken = 0

print("I have chosen a secret number between 1 and 100.")
print("Can you guess it within 5 attempts?")

while guesses_taken < 5:
    guess_str = input("Enter your guess [1-100]: ")
    if not guess_str:
        print("Please enter a valid number!")
        continue
    
    guess = int(guess_str)
    guesses_taken += 1
    
    if guess < secret_number:
        print("Too low! Try again.")
    elif guess > secret_number:
        print("Too high! Try again.")
    else:
        print(f"BINGO! You guessed the secret number in {guesses_taken} attempts!")
        break
else:
    print(f"Game Over! The mystery number was: {secret_number}.")
""",
                    category = "Games",
                    isFavorite = false,
                    isRecent = false,
                    isDefaultTemplate = true
                ),
                ProjectEntity(
                    name = "star_patterns.py",
                    content = """# Beautiful Visual Pattern Printer
# Uses nested loops and string multiplication

print("===== TRIANGLE STAR PATTERN =====")
size = 7

# Upper triangle
for i in range(1, size + 1):
    spaces = " " * (size - i)
    stars = "*" * (2 * i - 1)
    print(spaces + stars)

# Mirror inverted pattern
for i in range(size - 1, 0, -1):
    spaces = " " * (size - i)
    stars = "*" * (2 * i - 1)
    print(spaces + stars)

print("\nPyCode Studio rendered star shapes perfectly!")
""",
                    category = "Basics",
                    isFavorite = false,
                    isRecent = false,
                    isDefaultTemplate = true
                )
            )

            for (project in defaultProjects) {
                projectDao.insertProject(project)
            }
        }
    }
}

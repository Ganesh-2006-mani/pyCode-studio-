package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ProgressEntity
import com.example.data.database.ProjectEntity
import com.example.data.repository.ProjectRepository
import com.example.compiler.ConsoleOutput
import com.example.compiler.InterpreterState
import com.example.compiler.PythonInterpreter
import com.example.tutorials.Lesson
import com.example.tutorials.PythonLessons
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

class PyCodeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ProjectRepository(database.projectDao(), database.progressDao())

    // UI state streams
    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteProjects: StateFlow<List<ProjectEntity>> = repository.favoriteProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentProjects: StateFlow<List<ProjectEntity>> = repository.recentProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProgress: StateFlow<List<ProgressEntity>> = repository.allProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalXp: StateFlow<Int> = repository.totalXp
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Editor status variables
    var activeProjectId = MutableStateFlow<Int?>(null)
    val activeProjectName = MutableStateFlow("untitled.py")
    val editorContent = MutableStateFlow("")
    val isFavoriteState = MutableStateFlow(false)

    // Screen navigation state
    val currentScreen = MutableStateFlow("splash") // splash, onboarding, home, editor, tutorials, settings, profile
    val activeLesson = MutableStateFlow<Lesson?>(null)

    // Code Execution States
    val consoleOutputs = MutableStateFlow<List<ConsoleOutput>>(emptyList())
    val interpreterState = MutableStateFlow<InterpreterState>(InterpreterState.Idle)
    private var interpreter: PythonInterpreter? = null

    // Project query states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")

    // UI Settings state
    val zoomLevel = MutableStateFlow(15f)
    val isDarkMode = MutableStateFlow(true)
    val isSplitScreen = MutableStateFlow(false)

    // Auto-save control
    private var lastSavedContent = ""

    // Find and Replace
    val isFindReplaceVisible = MutableStateFlow(false)
    val findText = MutableStateFlow("")
    val replaceText = MutableStateFlow("")

    // Visual notifications
    val snackbarMessage = MutableStateFlow<String?>(null)

    // Advanced dynamic undo/redo logs
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    // Offline Auto-complete Suggestions state
    val autocompleteSuggestions = MutableStateFlow<List<String>>(emptyList())

    init {
        viewModelScope.launch {
            // Initial data preparation
            repository.checkAndPrepopulate()
            loadLastSession()
        }
    }

    private suspend fun loadLastSession() {
        val projects = repository.allProjects.firstOrNull() ?: emptyList()
        val recent = projects.find { it.isRecent } ?: projects.firstOrNull()
        if (recent != null) {
            setProjectActive(recent)
        } else {
            // Seeding template fallback
            val defaultText = "# PyCode Studio Sandbox\nprint('Type and experiment offline!')\n"
            editorContent.value = defaultText
            lastSavedContent = defaultText
        }
    }

    fun setProjectActive(project: ProjectEntity) {
        activeProjectId.value = project.id
        activeProjectName.value = project.name
        editorContent.value = project.content
        isFavoriteState.value = project.isFavorite
        lastSavedContent = project.content
        undoStack.clear()
        redoStack.clear()
        
        // Update isRecent status
        viewModelScope.launch {
            repository.updateProject(project.copy(isRecent = true, lastModified = System.currentTimeMillis()))
        }
    }

    fun updateEditorContent(newContent: String) {
        if (editorContent.value != newContent) {
            // Capture historical state for undo
            if (undoStack.isEmpty() || undoStack.last() != editorContent.value) {
                undoStack.add(editorContent.value)
                if (undoStack.size > 50) undoStack.removeAt(0)
            }
            redoStack.clear()
            editorContent.value = newContent
            triggerAutoSave()
            updateSuggestions(newContent)
        }
    }

    fun performUndo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(editorContent.value)
            editorContent.value = prev
            triggerAutoSave()
        }
    }

    fun performRedo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(editorContent.value)
            editorContent.value = next
            triggerAutoSave()
        }
    }

    private fun triggerAutoSave() {
        val currentId = activeProjectId.value
        val name = activeProjectName.value
        val content = editorContent.value
        if (currentId != null && content != lastSavedContent) {
            viewModelScope.launch {
                repository.updateProject(
                    ProjectEntity(
                        id = currentId,
                        name = name,
                        content = content,
                        isFavorite = isFavoriteState.value,
                        lastModified = System.currentTimeMillis(),
                        isRecent = true
                    )
                )
                lastSavedContent = content
            }
        }
    }

    fun toggleFavoriteActiveProject() {
        val currentId = activeProjectId.value ?: return
        val currentFavorite = isFavoriteState.value
        val toggled = !currentFavorite
        isFavoriteState.value = toggled
        viewModelScope.launch {
            val proj = repository.getProjectById(currentId) ?: return@launch
            repository.updateProject(proj.copy(isFavorite = toggled))
            showSnackbar(if (toggled) "Added to Favorite projects" else "Removed from Favorites")
        }
    }

    fun saveAsNewProject(name: String, category: String = "All") {
        viewModelScope.launch {
            val sanitizedName = if (name.endsWith(".py")) name else "$name.py"
            val newProject = ProjectEntity(
                name = sanitizedName,
                content = editorContent.value,
                category = category,
                lastModified = System.currentTimeMillis(),
                isRecent = true
            )
            val newId = repository.saveProject(newProject)
            activeProjectId.value = newId.toInt()
            activeProjectName.value = sanitizedName
            lastSavedContent = editorContent.value
            showSnackbar("Project '$sanitizedName' saved successfully!")
        }
    }

    fun deleteActiveProject() {
        val currentId = activeProjectId.value ?: return
        viewModelScope.launch {
            val proj = repository.getProjectById(currentId) ?: return@launch
            repository.deleteProject(proj)
            activeProjectId.value = null
            activeProjectName.value = "untitled.py"
            editorContent.value = ""
            lastSavedContent = ""
            loadLastSession()
            showSnackbar("Deleted project successfully")
        }
    }

    fun deleteProjectById(projectId: Int) {
        viewModelScope.launch {
            val proj = repository.getProjectById(projectId) ?: return@launch
            repository.deleteProject(proj)
            if (activeProjectId.value == projectId) {
                activeProjectId.value = null
                activeProjectName.value = "untitled.py"
                editorContent.value = ""
                lastSavedContent = ""
                loadLastSession()
            }
            showSnackbar("Deleted project: ${proj.name}")
        }
    }

    // Python compiler trigger
    fun runPythonCode() {
        interpreter?.terminate()
        consoleOutputs.value = emptyList()
        
        val codeToExecute = editorContent.value
        val currentInterpreter = PythonInterpreter(
            onOutput = { output ->
                viewModelScope.launch {
                    val currentList = consoleOutputs.value.toMutableList()
                    currentList.add(output)
                    consoleOutputs.value = currentList
                }
            },
            onStateChange = { state ->
                viewModelScope.launch {
                    interpreterState.value = state
                    // If successfully finished, check if this is matching active lesson requirement
                    if (state is InterpreterState.Completed) {
                        checkLessonCompletion()
                    }
                }
            }
        )
        interpreter = currentInterpreter
        viewModelScope.launch {
            currentInterpreter.executeCode(codeToExecute)
        }
    }

    fun sendConsoleInput(input: String) {
        viewModelScope.launch {
            interpreter?.provideInput(input)
        }
    }

    fun stopPythonExecution() {
        interpreter?.terminate()
        interpreterState.value = InterpreterState.Idle
        consoleOutputs.value = consoleOutputs.value + ConsoleOutput.System("\n>>> Execution stopped manually <<<")
    }

    private fun checkLessonCompletion() {
        val lesson = activeLesson.value ?: return
        val textOutputs = consoleOutputs.value.filterIsInstance<ConsoleOutput.Standard>().map { it.text }
        
        // Use lesson custom checking
        val (passed, feedback) = lesson.validationCheck(textOutputs, emptyMap())
        
        viewModelScope.launch {
            if (passed) {
                // Award XP and flag success
                repository.updateLessonProgress(
                    ProgressEntity(
                        lessonId = lesson.id,
                        isCompleted = true,
                        userCodeSubmitted = editorContent.value,
                        scoreAwarded = lesson.xpReward
                    )
                )
                showSnackbar("Congratulations! +${lesson.xpReward} XP: $feedback")
            } else {
                showSnackbar("Verification unsuccessful: $feedback")
            }
        }
    }

    fun selectLesson(lesson: Lesson) {
        activeLesson.value = lesson
        activeProjectName.value = "challenge_${lesson.id}.py"
        editorContent.value = lesson.codeStub
        lastSavedContent = lesson.codeStub
        activeProjectId.value = null
        currentScreen.value = "editor"
        showSnackbar("Exercise active! Implement the instructions.")
    }

    fun formatActiveCode() {
        val code = editorContent.value
        val formatted = try {
            val lines = code.split("\n")
            lines.joinToString("\n") { line ->
                // Clean trailing spaces and balance basic structural symbols
                var clean = line.trimEnd()
                val trimStart = clean.trimStart()
                if (trimStart.startsWith("def ") || trimStart.startsWith("if ") || trimStart.startsWith("for ") || trimStart.startsWith("while ")) {
                    if (!clean.endsWith(":")) {
                        clean += ":"
                    }
                }
                clean
            }
        } catch (e: Exception) {
            code
        }
        updateEditorContent(formatted)
        showSnackbar("Code formatted successfully!")
    }

    // Smart Local AI Suggestions System without internet!
    private val standardPythonKeywords = listOf(
        "print", "input", "len", "range", "append", "int", "str", "float",
        "for", "while", "if", "elif", "else", "def", "return", "pass",
        "True", "False", "None", "and", "or", "not", "in", "import"
    )

    private fun updateSuggestions(text: String) {
        // Find last word being typed
        val lines = text.split("\n")
        val lastLine = lines.lastOrNull() ?: ""
        val lastWord = lastLine.split(" ", "\t", "(", ")", "=", "+", "-", "*", "/", ",", "[").lastOrNull()?.trim() ?: ""

        if (lastWord.length >= 2) {
            // Read declared names in active script
            val declaredVariables = mutableSetOf<String>()
            val vRegex = "([a-zA-Z_][a-zA-Z0-9_]*)\\s*=".toRegex()
            vRegex.findAll(text).forEach { match ->
                declaredVariables.add(match.groupValues[1])
            }
            // Read function declarations
            val fRegex = "def\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(".toRegex()
            fRegex.findAll(text).forEach { matchResult ->
                declaredVariables.add(matchResult.groupValues[1])
            }

            val candidates = standardPythonKeywords + declaredVariables.toList()
            val filtered = candidates.filter { it.startsWith(lastWord) && it != lastWord }.distinct().take(5)
            autocompleteSuggestions.value = filtered
        } else {
            autocompleteSuggestions.value = emptyList()
        }
    }

    fun applyAutocomplete(word: String) {
        val currentText = editorContent.value
        val lines = currentText.split("\n").toMutableList()
        val lastLine = lines.lastOrNull() ?: ""
        // Locate last word boundary
        val lastTokenIdx = lastLine.lastIndexOfAny(charArrayOf(' ', '\t', '(', ')', '=', '+', '-', '*', '/', ',', '['))
        val prefix = if (lastTokenIdx != -1) lastLine.substring(0, lastTokenIdx + 1) else ""
        
        lines[lines.size - 1] = prefix + word
        val newText = lines.joinToString("\n")
        updateEditorContent(newText)
        autocompleteSuggestions.value = emptyList()
    }

    // Share option
    fun getShareableText(): String {
        return "=== Python Script on PyCode Studio ===\n# File: ${activeProjectName.value}\n\n${editorContent.value}\n\nRunning fully offline on PyCode Studio!"
    }

    // Export / Import support
    fun exportCodeToUri(outputStream: OutputStream) {
        try {
            outputStream.write(editorContent.value.toByteArray())
            outputStream.flush()
            showSnackbar("Exported successfully to file!")
        } catch (e: Exception) {
            showSnackbar("Export failed: ${e.message}")
        }
    }

    fun importCodeFromUri(inputStream: InputStream, filename: String) {
        try {
            val text = inputStream.bufferedReader().use { it.readText() }
            val name = if (filename.endsWith(".py")) filename else "$filename.py"
            activeProjectId.value = null
            activeProjectName.value = name
            updateEditorContent(text)
            saveAsNewProject(name)
            showSnackbar("Imported '$name' successfully!")
        } catch (e: Exception) {
            showSnackbar("Import failed: ${e.message}")
        }
    }

    // Quick helpers
    fun showSnackbar(message: String) {
        snackbarMessage.value = message
    }

    fun clearSnackbar() {
        snackbarMessage.value = null
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            database.projectDao().deleteAllUserProjects()
            database.progressDao().clearProgress()
            activeProjectId.value = null
            activeProjectName.value = "sandbox.py"
            editorContent.value = ""
            lastSavedContent = ""
            repository.checkAndPrepopulate()
            loadLastSession()
            showSnackbar("PyCode Studio storage reset completed!")
        }
    }
}

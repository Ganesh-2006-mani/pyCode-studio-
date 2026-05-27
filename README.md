# PyCode Studio — Offline Python IDE & Mobile Compiler

PyCode Studio is a modern, high-intensity, completely offline Python scripting compiler and development IDE designed for Android. Built entirely without external cloud APIs, internet dependencies, or paid service integrations, it supports continuous interactive parsing, syntax parsing, and learning challenges entirely off the grid.

---

## 🚀 Key Feature Sets

- **Self-Contained Compiler Engine**: Native asynchronous Python 3 subset parser managing calculations, conditions, variables, custom lists, functions (`def`), and iterative loops.
- **Interactive `input()` Console**: Fully suspendable execution cycles that pause and request user responses directly inside a terminal-styled keyboard drawer.
- **Offline Code Suggestions**: Active dynamic autocomplete providing semantic recommendations matching local declared variables, methods, and built-in commands as you type.
- **Local SQLite File Workspace**: Comprehensive file explorer supporting file creation, sorting, searching, import-to-sandbox, and bookmarked favorites.
- **Syntax Visual Transformer**: Jetpack Compose real-time visual parser applying distinctive color-coding highlighting to comments, strings, and system variables in the editor canvas.
- **Scholar Tutorial Challenges**: Integrated algorithm learning series with automated syntax evaluations awarding XP levels.

---

## 🛠️ App Directory Structure

```text
pycode-studio/
│
├── app/
│   ├── build.gradle.kts           # AGP and Library module mappings (Room & Icons)
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml # Entry definitions and adaptive icon link
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt # Entry ComponentActivity
│   │   │   │   │
│   │   │   │   ├── compiler/
│   │   │   │   │   └── PythonInterpreter.kt # Pure Kotlin Python 3 interpreter
│   │   │   │   │
│   │   │   │   ├── data/
│   │   │   │   │   ├── database/
│   │   │   │   │   │   ├── AppDatabase.kt   # Persistent Room SQLite constructor
│   │   │   │   │   │   ├── ProjectDao.kt    # Project & lesson data access definitions
│   │   │   │   │   │   └── ProjectEntity.kt # Saved file & progress schema definitions
│   │   │   │   │   └── repository/
│   │   │   │   │       └── ProjectRepository.kt # Data coordinator & prepopulator
│   │   │   │   │
│   │   │   │   ├── tutorials/
│   │   │   │   │   └── PythonLessons.kt # Offline lesson playlists & checkers
│   │   │   │   │
│   │   │   │   └── ui/
│   │   │   │       ├── editor/
│   │   │   │       │   └── PythonSyntaxVisualTransformation.kt # Real-time highlights
│   │   │   │       ├── screens/
│   │   │   │       │   └── MainAppScreen.kt # Master reactive Compose user pages
│   │   │   │       ├── theme/
│   │   │   │       │   ├── Color.kt      # Neon cyber programming color variables
│   │   │   │       │   └── Theme.kt      # System Dark/Light material templates
│   │   │   │
│   │   │   └── res/
│   │   │       ├── drawable/
│   │   │       │   ├── pycode_logo_1779884999881.png   # Custom generated launcher logo
│   │   │       │   ├── ic_launcher_background.xml # Cyber launcher background
│   │   │       │   └── ic_launcher_foreground.xml # Center-safe launcher foreground
```

---

## 📝 Step-by-Step Installation & Build Guidelines

To build and compile PyCode Studio from source:

### 1. Requirements

- Android Studio **Koala** or newer.
- Java Development Kit (**JDK 17** or newer).
- Android SDK **Platform 36** (Target SDK).

### 2. Dependency Resolution

All packages and version constraints are managed via `gradle/libs.versions.toml` containing:
- **Jetpack Compose BoM**: `2024.09.00`
- **Room SQLite**: `2.7.0` (with Kotlin Symbol Processing **KSP** compiler mapping)
- **Material Icons Extended**: Responsive IDE visual controls support.

### 3. Compilation Commands

From the project root workspace directory, call:
```bash
# Assemble debug apk
gradle assembleDebug

# Complete static unit and Robolectric tests
gradle test
```
The resulting build APK will be generated under:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 Interactive User Environment

1. **The Sandbox Workspace**: Create new `.py` files, customize their category flags, zoom typography dynamically, format active scopes, and share completed work instantly.
2. **The Output Panel**: Run computations in a separate terminal thread. Input variables in real time when prompted.
3. **Scholar Progression**: Learn offline syntax structures, complete checks, and test achievements.

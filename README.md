# Android Task Manager

A native Android task manager app inspired by Windows Task Manager, built with
Kotlin and Jetpack Compose in a Google Material You (Material 3) style.

Targets **Android 12+ (API 31)** with a pure-English UI.

![Platform](https://img.shields.io/badge/Android-12%2B-green)
![Language](https://img.shields.io/badge/Kotlin-2.0-blue)
![UI](https://img.shields.io/badge/UI-Compose%20%2F%20Material%203-purple)

## Features

Four tabs via bottom navigation:

| Tab | What it shows |
|-----|---------------|
| **Processes** | Running processes visible to the app (icon, name, PID, memory, state) with a stop button, plus recently used apps from `UsageStatsManager` (requires usage access). |
| **Performance** | Live charts (1 s refresh): CPU (or load average), memory, storage and network RX/TX curves, drawn with a custom Canvas. |
| **Storage** | Shared storage usage bar with used / free / total. |
| **Settings** | Usage access permission guide and app info. |

- Material You dynamic color scheme (falls back to a brand palette)
- MVVM architecture with `ViewModel` + `StateFlow`
- All sampling runs on background dispatchers; the UI thread is never blocked
- Honest about platform limits: unavailable data is labeled instead of faked

## Platform limitations (read this)

Modern Android deliberately hides system internals from non-root apps.
This project reports exactly what the platform exposes:

| Data | Availability on Android 12+ (non-root) | Source used |
|------|----------------------------------------|-------------|
| System CPU % | **Not available** — `/proc/stat` returns `EACCES` (SELinux). No public API exists. | `/proc/loadavg` fallback shows the 1-minute load average instead |
| Memory | Available | `ActivityManager.MemoryInfo` + `/proc/meminfo` |
| Storage | Available | `StatFs` |
| Network rate | Available with **usage access** | `NetworkStatsManager.querySummaryForDevice` (falls back to `TrafficStats`) |
| Full process list | **Not available** — `getRunningAppProcesses()` returns only your own process | Visible processes + `UsageStatsManager` recent-apps list |
| Killing other apps | **Not available** — `killBackgroundProcesses` only stops permitted apps | Shows "Insufficient permissions" otherwise |

Root access (or an ADB-assisted data source) would be required to lift these
restrictions. They are OS privacy policies, not bugs in this app.

## Tech stack

- **Kotlin 2.0** / **Jetpack Compose (BOM 2024.09)** / **Material 3**
- **Gradle 8.9** with Kotlin DSL and a version catalog (`gradle/libs.versions.toml`)
- **AGP 8.5.2**, compile/target SDK 35, min SDK 31
- Architecture: MVVM — `ViewModel` + `StateFlow` + repository classes
- Charts drawn with Compose `Canvas` (no chart library dependency)

## Project structure

```
.
├── build.gradle.kts              # Root build script (plugins only)
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat         # Gradle wrapper
├── gradle/
│   ├── wrapper/                  # Wrapper jar + properties
│   └── libs.versions.toml        # Version catalog
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/android/tskmgr/
        │   ├── MainActivity.kt               # Activity + bottom navigation scaffold
        │   ├── data/
        │   │   ├── SystemMetrics.kt          # CPU / memory / storage / network readers
        │   │   ├── ProcessRepository.kt      # Running processes + recent apps
        │   │   ├── ProcessInfo.kt            # Models
        │   │   └── PermissionsHelper.kt      # Usage access helpers
        │   └── ui/
        │       ├── PerformanceViewModel.kt   # 1 s sampling loop (background)
        │       ├── ProcessViewModel.kt
        │       ├── StorageViewModel.kt
        │       ├── SettingsViewModel.kt
        │       ├── theme/                    # Material You theme + palette
        │       ├── components/Charts.kt      # Sparkline chart + percent bar
        │       └── screens/                  # Processes / Performance / Storage / Settings
        └── res/                              # Strings, themes, adaptive icon
```

## Requirements

- JDK 17+ (21 recommended)
- Android SDK: `platform-tools`, `platforms;android-35`, `build-tools;35.0.0`
- A device or emulator running Android 12+ (API 31+)

Set `ANDROID_HOME` (or create `local.properties` with `sdk.dir`) before building.

## Build

```bash
# Debug APK
./gradlew :app:assembleDebug

# Output
# app/build/outputs/apk/debug/app-debug.apk

# Install on a connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Usage notes

1. Open the app, go to the **Settings** tab.
2. Tap **Usage access** and enable it for Task Manager.
3. Granting it unlocks: the recently used apps list (Processes tab) and the
   live network charts (Performance tab).
4. The stop button on a process may report "Insufficient permissions" — that
   is the OS refusing, not an app bug.

## License

This project is provided as-is for educational purposes.

# PureCam

English | [简体中文](README.zh-CN.md)

A pure, distraction-free camera app for Android, built with Jetpack Compose and CameraX.

> **Status:** early prototype (v0.1.0).

## Features

- 3:4 viewfinder that matches the final photo, with tap-to-focus and pinch-to-zoom
- Shutter button with capture feedback
- Flash modes: OFF / AUTO / ON
- Front and back camera switching
- Photos saved to `Pictures/PureCam`, with the latest shot shown as a thumbnail
- Portrait-locked UI, with photos still oriented correctly however the phone is held
- English and Simplified Chinese UI

## Tech stack

- **Language:** Kotlin 2.4
- **UI:** Jetpack Compose, Material 3
- **Camera:** CameraX 1.6 (`LifecycleCameraController`)
- **Build:** Android Gradle Plugin 9.3 (built-in Kotlin), Gradle 9.6
- **SDK:** minSdk 29 (Android 10), compileSdk 37

## Getting started

You need the latest stable [Android Studio](https://developer.android.com/studio) and a physical device running Android 10 or later. Emulator cameras don't reflect real-device behavior.

1. Clone the repository and open the project folder in Android Studio.
2. Wait for Gradle sync to finish. If Android Studio asks for SDK Platform 37, install it.
3. Enable USB debugging on your phone, connect it, and click **Run**.

To build from the command line (requires JDK 17+ and the Android SDK):

```bash
./gradlew assembleDebug      # macOS / Linux
gradlew.bat assembleDebug    # Windows
```

## Project structure

```text
app/src/main/java/com/purecam/app/
├── MainActivity.kt          # Entry point and edge-to-edge setup
├── camera/
│   ├── CameraScreen.kt      # Viewfinder and capture controls
│   ├── PermissionScreen.kt  # Camera permission flow
│   └── PhotoStorage.kt      # Saving to the gallery and loading thumbnails
└── ui/theme/Theme.kt        # Dark Material 3 theme
```

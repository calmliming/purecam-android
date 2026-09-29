# PureCam

English | [简体中文](README.zh-CN.md)

A pure, distraction-free camera app for Android, built with Jetpack Compose and CameraX.

> **Status:** early prototype (v0.1.0).

## Features

- 3:4 viewfinder that matches the final photo
- Tap-to-focus with a focus ring, and pinch-to-zoom with a zoom ratio label (tap it to go back to 1x)
- Shutter button with capture feedback
- Flash modes: OFF / AUTO / ON
- Front and back camera switching
- Flash mode and camera choice are remembered across launches
- Photos saved to `Pictures/PureCam`; the thumbnail shows the latest one as soon as the app opens
- Portrait-locked UI, with photos still oriented correctly however the phone is held
- English and Simplified Chinese UI

## Tech stack

- **Language:** Kotlin 2.4
- **UI:** Jetpack Compose, Material 3
- **Camera:** CameraX 1.6 (`LifecycleCameraController`)
- **Settings:** Jetpack DataStore (Preferences)
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

Run the unit tests with `testDebugUnitTest` in place of `assembleDebug`. They run on the JVM, no device needed.

Release builds are signed with the key described in `keystore.properties` in the project root. The file holds passwords, so it is git-ignored and you create it yourself:

```properties
storeFile=D:/keys/purecam-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Use forward slashes in the path, since backslashes are escape characters in this file. Then run `assembleRelease`. Without the file, the release APK is still built, just unsigned.

## Project structure

```text
app/src/main/java/com/purecam/app/
├── MainActivity.kt             # Entry point and edge-to-edge setup
├── camera/
│   ├── CameraScreen.kt         # Permission check and viewfinder page
│   ├── CameraControls.kt       # Top and bottom bar controls
│   ├── ViewfinderOverlays.kt   # Focus ring and zoom ratio label
│   ├── PermissionScreen.kt     # Camera permission flow
│   └── MediaStorage.kt         # Saving to the gallery, finding the latest photo, thumbnails
├── settings/CameraSettings.kt  # Remembered settings (DataStore)
└── ui/theme/Theme.kt           # Dark Material 3 theme
```

Unit tests live in `app/src/test/`.

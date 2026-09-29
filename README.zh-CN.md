# PureCam

[English](README.md) | 简体中文

一款纯粹、无干扰的 Android 相机应用，基于 Jetpack Compose 和 CameraX 开发。

> **状态：** 早期原型（v0.1.0）。

## 功能

- 3:4 取景框，与最终成片一致
- 点按对焦并显示对焦框；双指缩放并显示变焦倍数，点击倍数回到 1x
- 带拍摄反馈的快门按钮
- 闪光灯：关闭 / 自动 / 开启
- 前后镜头切换
- 记住闪光灯模式和前后镜头的选择，重新打开应用后保持不变
- 照片保存到系统相册 `Pictures/PureCam`，打开应用就显示最近一张的缩略图
- 界面锁定竖屏，无论横拿竖拿，拍出的照片方向都正确
- 支持英文和简体中文界面

## 技术栈

- **语言：** Kotlin 2.4
- **界面：** Jetpack Compose、Material 3
- **相机：** CameraX 1.6（`LifecycleCameraController`）
- **设置存储：** Jetpack DataStore（Preferences）
- **构建：** Android Gradle Plugin 9.3（内置 Kotlin 支持）、Gradle 9.6
- **SDK：** minSdk 29（Android 10）、compileSdk 37

## 开始使用

需要最新稳定版 [Android Studio](https://developer.android.com/studio)，以及一台 Android 10 及以上的真机。模拟器的相机无法反映真机效果。

1. 克隆仓库，用 Android Studio 打开项目文件夹。
2. 等待 Gradle 同步完成。如果提示缺少 SDK Platform 37，按提示安装。
3. 手机开启 USB 调试，连接电脑后点击 **Run**。

命令行构建（需要 JDK 17+ 和 Android SDK）：

```bash
./gradlew assembleDebug      # macOS / Linux
gradlew.bat assembleDebug    # Windows
```

把 `assembleDebug` 换成 `testDebugUnitTest` 可以运行单元测试。测试在电脑的 JVM 上运行，不需要连手机。

release 包用项目根目录 `keystore.properties` 里写的密钥签名。这个文件里有密码，已被 git 忽略，需要自己创建：

```properties
storeFile=D:/keys/purecam-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

路径要用正斜杠 `/`，因为反斜杠在这个文件里是转义符。然后运行 `assembleRelease`。没有这个文件时，release 包照常构建，只是不签名。

## 项目结构

```text
app/src/main/java/com/purecam/app/
├── MainActivity.kt             # 应用入口，沉浸式（edge-to-edge）设置
├── camera/
│   ├── CameraScreen.kt         # 权限判断和取景页
│   ├── CameraControls.kt       # 顶栏、底栏的按钮
│   ├── ViewfinderOverlays.kt   # 对焦框、变焦倍数
│   ├── PermissionScreen.kt     # 相机权限流程
│   └── MediaStorage.kt         # 保存到相册、查询最近一张、读取缩略图
├── settings/CameraSettings.kt  # 记住的设置（DataStore）
└── ui/theme/Theme.kt           # 深色 Material 3 主题
```

单元测试在 `app/src/test/` 目录。

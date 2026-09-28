# PureCam

[English](README.md) | 简体中文

一款纯粹、无干扰的 Android 相机应用，基于 Jetpack Compose 和 CameraX 开发。

> **状态：** 早期原型（v0.1.0）。

## 功能

- 3:4 取景框，与最终成片一致；支持点按对焦、双指缩放
- 带拍摄反馈的快门按钮
- 闪光灯：关闭 / 自动 / 开启
- 前后镜头切换
- 照片保存到系统相册 `Pictures/PureCam`，并显示最近一张的缩略图
- 界面锁定竖屏，无论横拿竖拿，拍出的照片方向都正确
- 支持英文和简体中文界面

## 技术栈

- **语言：** Kotlin 2.4
- **界面：** Jetpack Compose、Material 3
- **相机：** CameraX 1.6（`LifecycleCameraController`）
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

## 项目结构

```text
app/src/main/java/com/purecam/app/
├── MainActivity.kt          # 应用入口，沉浸式（edge-to-edge）设置
├── camera/
│   ├── CameraScreen.kt      # 取景器和拍摄控件
│   ├── PermissionScreen.kt  # 相机权限流程
│   └── PhotoStorage.kt      # 保存到相册、读取缩略图
└── ui/theme/Theme.kt        # 深色 Material 3 主题
```

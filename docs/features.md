# PureCam 功能清单

> 对应版本：v0.1.0（versionCode 1）· 整理日期：2026-09-29
>
> 按代码现状整理已实现的功能和目前还不支持的部分。技术栈、构建和运行方式见 [README](../README.zh-CN.md)。

## 总览

| 模块 | 已实现 |
| --- | --- |
| 相机权限 | 进入即申请；永久拒绝后引导去系统设置开启 |
| 取景 | 3:4 取景框（与成片一致）、点按对焦、双指缩放 |
| 拍照 | 快门按钮，按下时取景画面闪黑作为反馈 |
| 照片保存 | JPEG 存入系统相册 `Pictures/PureCam`，不需要存储权限 |
| 闪光灯 | OFF → AUTO → ON 循环切换，仅后置镜头 |
| 镜头切换 | 前置 / 后置 |
| 最近照片 | 缩略图显示刚拍的一张，点击用系统相册打开 |
| 界面外观 | 竖屏锁定（照片方向仍然正确）、黑底沉浸式、深色主题 |
| 多语言 | 英文（默认）、简体中文 |

## 界面布局

应用只有两个界面：没有相机权限时显示授权页，有权限后显示取景页。取景页从上到下分三块：

| 区域 | 内容 |
| --- | --- |
| 顶栏 | 左侧是闪光灯按钮，切到前置镜头时隐藏 |
| 中部 | 3:4 取景框，在剩余空间内居中 |
| 底栏 | 从左到右：最近照片缩略图、快门、切换镜头 |

## 功能详情

### 相机权限

- 没有相机权限时显示授权页，并自动弹出系统授权框。
- 用户拒绝后，点「允许使用相机」可以再次申请。
- 用户永久拒绝后（Android 10 上选了不再询问，Android 11 起拒绝两次），系统不会再弹框。这时按钮变成「去设置开启」，点击跳到本应用的系统设置页。
- 每次回到前台都会重新检查权限：在系统设置里开启权限后返回应用，会直接进入取景页。

代码：[PermissionScreen.kt][PermissionScreen]；权限判断和回到前台时的重新检查在 [CameraScreen.kt][CameraScreen] 的 `CameraScreen()`。

### 取景

- 取景框固定为 3:4，与 CameraX 默认的拍照比例一致，看到的画面就是拍到的画面。
- 点按画面对焦，双指缩放调节变焦。这两项由 CameraX 的 `LifecycleCameraController` 提供。
- 相机跟随界面生命周期：应用切到后台时释放相机，回到前台时自动恢复。

代码：[CameraScreen.kt][CameraScreen] 的 `CameraContent()`。

### 拍照

- 快门按钮是白色圆环加实心圆，按下时实心圆缩小。
- 按下快门的瞬间取景画面闪黑，200 毫秒内淡出，作为拍摄反馈。
- 相机还没初始化完成时（刚打开应用的一瞬间），按快门没有反应。
- 界面锁定竖屏，但 CameraX 会按手机的实际朝向处理照片方向，横着拍的照片也是正的。
- 保存失败时弹出「照片保存失败」提示，并在日志中记录错误（Tag 为 `PureCam`）。

代码：[CameraScreen.kt][CameraScreen] 的 `ShutterButton()`，以及 `CameraContent()` 里的快门回调。

### 照片保存

- 保存为 JPEG，位置是系统相册的 `Pictures/PureCam` 目录。
- 文件名格式为 `PureCam_日期_时间_毫秒.jpg`，例如 `PureCam_20260929_104512_123.jpg`。
- 通过 MediaStore 写入，Android 10 及以上不需要存储权限。

代码：[PhotoStorage.kt][PhotoStorage] 的 `takePhotoToGallery()`。

### 闪光灯

- 位于顶栏左侧，显示闪电图标和当前模式。点击按 OFF → AUTO → ON 循环切换，默认 OFF。
- 三种模式颜色不同：OFF 为半透明白色，AUTO 为白色，ON 为琥珀色。
- 只在后置镜头下显示。切到前置镜头时隐藏，切回后置时恢复之前选的模式。

代码：[CameraScreen.kt][CameraScreen] 的 `FlashButton()`、`nextFlashMode()`。

### 前后镜头切换

- 位于底栏右侧，点击在前置、后置镜头之间切换，切换时图标转半圈。默认使用后置镜头。
- 切换前会确认设备有目标镜头。设备没有该镜头，或者相机还没初始化完成时，点击没有反应。

代码：[CameraScreen.kt][CameraScreen] 的 `SwitchCameraButton()`，以及 `CameraContent()` 里的切换回调。

### 最近照片

- 位于底栏左侧的圆形缩略图，拍照成功后显示刚拍的那一张。拍第一张之前是空的圆形占位，点击没有反应。
- 缩略图由系统生成，已经按照片方向转正。读取失败时显示空白占位，但仍然可以点击打开照片。
- 点击缩略图，用系统相册或其他看图应用打开这张照片。设备上没有能打开图片的应用时，点击没有反应。

代码：[CameraScreen.kt][CameraScreen] 的 `Thumbnail()`；[PhotoStorage.kt][PhotoStorage] 的 `loadThumbnail()`、`openInGallery()`。

### 界面外观

- 界面锁定竖屏。
- 沉浸式显示：状态栏和导航栏透明、使用浅色图标，界面内容避开系统栏和挖孔区域。
- 深色 Material 3 主题，黑底白字。琥珀色（`#FFB300`）只用在闪光灯 ON、授权页按钮等少数需要强调的地方。
- 启动时窗口背景就是黑色，不会闪白。
- 自适应启动图标：深色底，前景是极简的镜头图形（白色圆环、中心圆点、琥珀色指示灯）。图标带单色图层，支持 Android 13 起的主题图标。

代码：[MainActivity.kt][MainActivity]、[Theme.kt][Theme]、[AndroidManifest.xml][Manifest]、[themes.xml][themes]、[ic_launcher_foreground.xml][launcher-fg]。

### 多语言与无障碍

- 界面支持英文（默认）和简体中文，跟随系统语言。
- 授权页文案、保存失败提示和读屏描述都有中文翻译。应用名 PureCam 和闪光灯模式 OFF / AUTO / ON 在中文界面下也保持英文。
- 闪光灯、快门、切换镜头和最近照片都设置了读屏描述，TalkBack 能读出它们的用途。

代码：[values/strings.xml][strings-en]、[values-zh/strings.xml][strings-zh]。

## 暂不支持

下面是目前还没有的能力。计划新增的功能见 [新增功能规划](roadmap.md)。

- 只能拍照，不能录像。
- 照片比例固定为 3:4，不能切换到 16:9、1:1 等比例。
- 点按对焦和双指缩放没有界面提示，比如对焦框和变焦倍数。
- 拍照没有快门声，只有画面闪黑。
- 闪光灯只在拍照瞬间触发，没有常亮补光；前置镜头没有补光。
- 最近照片只保存在内存里。界面重建（比如切换系统语言）或重新打开应用后，缩略图会清空，也不会读取相册里已有的照片。
- 闪光灯模式和前后镜头的选择在界面重建后会保留，但没有持久化。重新打开应用后恢复默认：闪光灯 OFF，后置镜头。
- 没有设置页，也没有网格线、定时拍摄、音量键拍照、手动 ISO / 快门等进阶功能。代码注释提到：要做手动参数时，再把 `LifecycleCameraController` 换成 `ProcessCameraProvider` 加 Camera2 互操作。

## 工程与配置

| 项目 | 现状 |
| --- | --- |
| 最低系统 | Android 10（minSdk 29） |
| targetSdk / compileSdk | 36 / 37 |
| 权限 | 只申请 `CAMERA` |
| 硬件要求 | 清单声明必须有相机（`android.hardware.camera.any`） |
| 系统备份 | 关闭（`allowBackup="false"`） |
| Release 构建 | 开启 R8 代码压缩和资源压缩 |
| 自动化测试 | 暂无 |

详见 [app/build.gradle.kts][app-gradle] 和 [AndroidManifest.xml][Manifest]。

[MainActivity]: ../app/src/main/java/com/purecam/app/MainActivity.kt
[CameraScreen]: ../app/src/main/java/com/purecam/app/camera/CameraScreen.kt
[PermissionScreen]: ../app/src/main/java/com/purecam/app/camera/PermissionScreen.kt
[PhotoStorage]: ../app/src/main/java/com/purecam/app/camera/PhotoStorage.kt
[Theme]: ../app/src/main/java/com/purecam/app/ui/theme/Theme.kt
[Manifest]: ../app/src/main/AndroidManifest.xml
[themes]: ../app/src/main/res/values/themes.xml
[launcher-fg]: ../app/src/main/res/drawable/ic_launcher_foreground.xml
[strings-en]: ../app/src/main/res/values/strings.xml
[strings-zh]: ../app/src/main/res/values-zh/strings.xml
[app-gradle]: ../app/build.gradle.kts

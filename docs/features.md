# PureCam 功能清单

> 对应版本：v1.0.0（versionCode 1），包含[规划](roadmap.md)的第一期 · 更新日期：2026-09-29
>
> 按代码现状整理已实现的功能和目前还不支持的部分。技术栈、构建和运行方式见 [README](../README.zh-CN.md)。

## 总览

| 模块 | 已实现 |
| --- | --- |
| 相机权限 | 进入即申请；永久拒绝后引导去系统设置开启 |
| 取景 | 3:4 取景框（与成片一致）、点按对焦（带对焦框）、双指缩放（显示倍数） |
| 拍照 | 快门按钮，按下时取景画面闪黑作为反馈 |
| 照片保存 | JPEG 存入系统相册 `Pictures/PureCam`，不需要存储权限 |
| 闪光灯 | OFF → AUTO → ON 循环切换，仅后置镜头 |
| 镜头切换 | 前置 / 后置 |
| 最近照片 | 缩略图显示 `Pictures/PureCam` 里最新的一张，打开应用就有；点击用系统相册打开 |
| 记住设置 | 闪光灯模式和前后镜头，重新打开应用后保持上次的选择 |
| 界面外观 | 竖屏锁定（照片方向仍然正确）、黑底沉浸式、深色主题 |
| 多语言 | 英文（默认）、简体中文 |

## 界面布局

应用只有两个界面：没有相机权限时显示授权页，有权限后显示取景页。取景页从上到下分三块：

| 区域 | 内容 |
| --- | --- |
| 顶栏 | 左侧是闪光灯按钮，切到前置镜头时隐藏 |
| 中部 | 3:4 取景框，在剩余空间内居中；上面叠加对焦框，底部居中是变焦倍数 |
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

代码：[CameraScreen.kt][CameraScreen] 的 `ViewfinderPage()`。

### 对焦框

- 点按取景画面，点按位置出现一个白色圆框，从略大缩到正常大小。圆框外衬一圈半透明黑边，亮背景上也看得清。
- 对焦成功后圆框停留约 1 秒再淡出；对焦失败时圆框先变暗再淡出。
- 切换镜头时清掉对焦框。
- 点按位置和对焦结果来自 CameraX 1.5 起提供的 `getTapToFocusInfoState()`。应用不拦截触摸事件，点按对焦和双指缩放照常工作。

代码：[ViewfinderOverlays.kt][ViewfinderOverlays] 的 `FocusRing()`。

### 变焦倍数

- 双指缩放时，取景框底部居中显示当前倍数，比如 `2.4x`。保留一位小数，整数倍不带小数，比如 `2x`。
- 倍数不是 1x 时标签一直显示，点击标签回到 1x；回到 1x 后标签停留约 1.5 秒再淡出。刚打开相机时不显示。
- 部分机型可以缩小到 1x 以下（超广角），比如 `0.6x`。

代码：[ViewfinderOverlays.kt][ViewfinderOverlays] 的 `ZoomRatioLabel()`、`formatZoomRatio()`。

### 拍照

- 快门按钮是白色圆环加实心圆，按下时实心圆缩小。
- 按下快门的瞬间取景画面闪黑，200 毫秒内淡出，作为拍摄反馈。
- 相机还没初始化完成时（刚打开应用的一瞬间），按快门没有反应。
- 界面锁定竖屏，但 CameraX 会按手机的实际朝向处理照片方向，横着拍的照片也是正的。
- 保存失败时弹出「照片保存失败」提示，并在日志中记录错误（Tag 为 `PureCam`）。

代码：[CameraControls.kt][CameraControls] 的 `ShutterButton()`，以及 [CameraScreen.kt][CameraScreen] 的 `ViewfinderPage()` 里的快门回调。

### 照片保存

- 保存为 JPEG，位置是系统相册的 `Pictures/PureCam` 目录。
- 文件名格式为 `PureCam_日期_时间_毫秒.jpg`，例如 `PureCam_20260929_104512_123.jpg`。
- 通过 MediaStore 写入，Android 10 及以上不需要存储权限。

代码：[MediaStorage.kt][MediaStorage] 的 `takePhotoToGallery()`。

### 闪光灯

- 位于顶栏左侧，显示闪电图标和当前模式。点击按 OFF → AUTO → ON 循环切换，默认 OFF。
- 三种模式颜色不同：OFF 为半透明白色，AUTO 为白色，ON 为琥珀色。
- 只在后置镜头下显示。切到前置镜头时隐藏，切回后置时恢复之前选的模式。
- 选择会保存下来，重新打开应用后保持不变，见[记住设置](#记住设置)。

代码：[CameraControls.kt][CameraControls] 的 `FlashButton()`、`FlashMode`。

### 前后镜头切换

- 位于底栏右侧，点击在前置、后置镜头之间切换，切换时图标转半圈。默认使用后置镜头。
- 切换前会确认设备有目标镜头。设备没有该镜头，或者相机还没初始化完成时，点击没有反应。
- 选择会保存下来，重新打开应用后直接打开上次用的镜头，见[记住设置](#记住设置)。

代码：[CameraControls.kt][CameraControls] 的 `SwitchCameraButton()`，以及 [CameraScreen.kt][CameraScreen] 的 `ViewfinderPage()` 里的切换回调。

### 最近照片

- 位于底栏左侧的圆形缩略图，显示 `Pictures/PureCam` 里最新的一张照片。打开应用时就会显示，拍照成功后换成刚拍的那一张。
- 每次回到前台都重新查询一次：在相册里删掉这一张后回到应用，缩略图换成上一张；全部删完就显示空的圆形占位，点击没有反应。查询出错时保留当前的缩略图。
- 不申请读取相册的权限，所以只能看到本应用保存的照片。卸载重装或清除应用数据后，以前拍的照片不再算本应用的，缩略图要等拍下一张才会出现。
- 缩略图由系统生成，已经按照片方向转正。读取失败时显示空白占位，但仍然可以点击打开照片。
- 点击缩略图，用系统相册或其他看图应用打开这张照片。设备上没有能打开图片的应用时，点击没有反应。

代码：[CameraControls.kt][CameraControls] 的 `Thumbnail()`；[MediaStorage.kt][MediaStorage] 的 `queryLatestPhoto()`、`loadThumbnail()`、`openInGallery()`；回到前台时的查询在 [CameraScreen.kt][CameraScreen] 的 `ViewfinderPage()`。

### 记住设置

- 闪光灯模式和前后镜头的选择一改就保存。重新打开应用、切换系统语言后，都保持上次的选择。
- 先读出设置再打开相机，不会先打开后置镜头再切到前置。读取只要几毫秒，这期间是黑屏。
- 设置文件损坏或读取失败时，按默认设置：后置镜头、闪光灯 OFF。
- 设置里是前置镜头、但这台设备没有前置镜头时（比如换机时迁移过来的设置），改用后置镜头。
- 保存在 DataStore 的 `camera_settings` 文件里。

代码：[CameraSettings.kt][CameraSettings]；读取设置在 [CameraScreen.kt][CameraScreen] 的 `CameraContent()`。

### 界面外观

- 界面锁定竖屏。
- 沉浸式显示：状态栏和导航栏透明、使用浅色图标，界面内容避开系统栏和挖孔区域。
- 深色 Material 3 主题，黑底白字。琥珀色（`#FFB300`）只用在闪光灯 ON、授权页按钮等少数需要强调的地方。
- 启动时窗口背景就是黑色，不会闪白。
- 自适应启动图标：深色底，前景是极简的镜头图形（白色圆环、中心圆点、琥珀色指示灯）。图标带单色图层，支持 Android 13 起的主题图标。

代码：[MainActivity.kt][MainActivity]、[Theme.kt][Theme]、[AndroidManifest.xml][Manifest]、[themes.xml][themes]、[ic_launcher_foreground.xml][launcher-fg]。

### 多语言与无障碍

- 界面支持英文（默认）和简体中文，跟随系统语言。
- 授权页文案、保存失败提示和读屏描述都有中文翻译。应用名 PureCam 和闪光灯模式 OFF / AUTO / ON 在中文界面下也保持英文，变焦倍数也统一写成 `2.4x` 的格式。
- 闪光灯、快门、切换镜头和最近照片都设置了读屏描述，TalkBack 能读出它们的用途。变焦倍数标签会读出倍数，并提示点击可以「恢复到 1x」。

代码：[values/strings.xml][strings-en]、[values-zh/strings.xml][strings-zh]。

## 暂不支持

下面是目前还没有的能力。计划新增的功能见 [新增功能规划](roadmap.md)。

- 只能拍照，不能录像。
- 照片比例固定为 3:4，不能切换到 16:9、1:1 等比例。
- 拍照没有快门声，只有画面闪黑。
- 闪光灯只在拍照瞬间触发，没有常亮补光；前置镜头没有补光。
- 没有设置页，也没有网格线、定时拍摄、音量键拍照、手动 ISO / 快门等进阶功能。代码注释提到：要做手动参数时，再把 `LifecycleCameraController` 换成 `ProcessCameraProvider` 加 Camera2 互操作。

## 工程与配置

| 项目 | 现状 |
| --- | --- |
| 最低系统 | Android 10（minSdk 29） |
| targetSdk / compileSdk | 36 / 37 |
| 权限 | 只申请 `CAMERA` |
| 硬件要求 | 清单声明必须有相机（`android.hardware.camera.any`） |
| 系统备份 | 关闭云备份（`allowBackup="false"`）。Android 12 起，部分厂商设备上的换机迁移不受这个开关控制，应用数据（包括设置）仍会迁到新手机 |
| Release 构建 | 开启 R8 代码压缩和资源压缩；用发布密钥签名，签名信息在项目根目录的 `keystore.properties`（不提交到 git），没有这个文件时不签名 |
| 自动化测试 | JVM 单元测试，覆盖闪光灯档位循环、变焦倍数格式、设置读写。运行 `gradlew testDebugUnitTest`，代码在 [app/src/test][tests] |

详见 [app/build.gradle.kts][app-gradle] 和 [AndroidManifest.xml][Manifest]。

[MainActivity]: ../app/src/main/java/com/purecam/app/MainActivity.kt
[CameraScreen]: ../app/src/main/java/com/purecam/app/camera/CameraScreen.kt
[CameraControls]: ../app/src/main/java/com/purecam/app/camera/CameraControls.kt
[ViewfinderOverlays]: ../app/src/main/java/com/purecam/app/camera/ViewfinderOverlays.kt
[PermissionScreen]: ../app/src/main/java/com/purecam/app/camera/PermissionScreen.kt
[MediaStorage]: ../app/src/main/java/com/purecam/app/camera/MediaStorage.kt
[CameraSettings]: ../app/src/main/java/com/purecam/app/settings/CameraSettings.kt
[Theme]: ../app/src/main/java/com/purecam/app/ui/theme/Theme.kt
[Manifest]: ../app/src/main/AndroidManifest.xml
[themes]: ../app/src/main/res/values/themes.xml
[launcher-fg]: ../app/src/main/res/drawable/ic_launcher_foreground.xml
[strings-en]: ../app/src/main/res/values/strings.xml
[strings-zh]: ../app/src/main/res/values-zh/strings.xml
[app-gradle]: ../app/build.gradle.kts
[tests]: ../app/src/test/java/com/purecam/app

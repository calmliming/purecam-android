package com.purecam.app.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.purecam.app.R
import com.purecam.app.settings.CameraSettings
import com.purecam.app.settings.CameraSettingsStore
import kotlinx.coroutines.launch

private const val TAG = "PureCam"

/**
 * 相机主界面：没有相机权限时显示授权页，有权限后显示取景器和拍摄控件。
 */
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }

    // 用户可能去系统设置里改了权限，每次回到前台都重新检查
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = context.hasCameraPermission()
    }

    if (hasPermission) {
        CameraContent()
    } else {
        PermissionScreen(onGranted = { hasPermission = true })
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

/**
 * 先读出上次的设置再打开相机，免得上次用的是前置、这次却先打开后置再切过去。
 * 读取只要几毫秒，这期间保持黑屏。
 */
@Composable
private fun CameraContent() {
    val context = LocalContext.current
    val settingsStore = remember { CameraSettingsStore(context) }
    val savedSettings by produceState<CameraSettings?>(initialValue = null) {
        val saved = settingsStore.load()
        // 设置可能是换机时从别的手机迁移过来的：这台设备没有前置镜头就改用后置，否则相机打不开
        val hasFrontCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        value = if (saved.useFrontCamera && !hasFrontCamera) saved.copy(useFrontCamera = false) else saved
    }
    savedSettings?.let { ViewfinderPage(initialSettings = it, settingsStore = settingsStore) }
}

@Composable
private fun ViewfinderPage(initialSettings: CameraSettings, settingsStore: CameraSettingsStore) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var settings by remember { mutableStateOf(initialSettings) }

    // CameraController 已经处理好预览、拍照、点按对焦、双指缩放和照片方向。
    // 以后要做手动 ISO / 快门这类精细控制时，再换成 ProcessCameraProvider + Camera2 互操作。
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            // 一开始就用上次的镜头和闪光灯，不用先打开默认镜头再切换
            cameraSelector = cameraSelectorFor(initialSettings.useFrontCamera)
            imageCaptureFlashMode = initialSettings.flashMode.imageCaptureMode
        }
    }
    DisposableEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose { cameraController.unbind() }
    }
    // 相机初始化是异步的，完成前调用 takePicture 会抛异常（刚打开应用就按快门会闪退）
    var cameraInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(cameraController) {
        val initialization = cameraController.initializationFuture
        initialization.addListener(
            { cameraInitialized = runCatching { initialization.get() }.isSuccess },
            ContextCompat.getMainExecutor(context),
        )
    }

    var lastPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }
    val shutterBlink = remember { Animatable(0f) }
    val tapToFocusInfo by cameraController.tapToFocusInfoState.observeAsState()
    val zoomState by cameraController.zoomState.observeAsState()

    LaunchedEffect(settings.useFrontCamera) {
        cameraController.cameraSelector = cameraSelectorFor(settings.useFrontCamera)
    }
    LaunchedEffect(settings.flashMode) {
        cameraController.imageCaptureFlashMode = settings.flashMode.imageCaptureMode
    }
    // 设置一改就保存，下次打开应用时恢复
    LaunchedEffect(settings) {
        settingsStore.save(settings)
    }

    // 每次回到前台都重新查一次最近的照片：用户可能在相册里删掉了它
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val shown = lastPhoto
        scope.launch {
            // 查询出错时保留当前的缩略图
            val latest = queryLatestPhoto(context)
                .onFailure { e -> Log.w(TAG, "Failed to query the latest photo", e) }
                .getOrElse { return@launch }
            if (latest == shown?.uri) return@launch
            val photo = latest?.let { CapturedPhoto(it, loadThumbnail(context, it)) }
            // 查询期间刚拍了新照片的话，以新照片为准
            if (lastPhoto === shown) lastPhoto = photo
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding(),
    ) {
        TopBar(
            flashMode = settings.flashMode,
            showFlash = !settings.useFrontCamera,
            onFlashClick = { settings = settings.copy(flashMode = settings.flashMode.next()) },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // 取景框固定 3:4，和 CameraX 默认的拍照比例一致：看到的就是拍到的
            Box(modifier = Modifier.aspectRatio(3f / 4f)) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx).apply { controller = cameraController } },
                    modifier = Modifier.fillMaxSize(),
                )
                // 切换镜头时清掉上一个镜头的对焦框
                key(settings.useFrontCamera) {
                    FocusRing(info = tapToFocusInfo, modifier = Modifier.fillMaxSize())
                }
                ZoomRatioLabel(
                    zoomRatio = zoomState?.zoomRatio,
                    onClick = { cameraController.setZoomRatio(1f) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                )
                // 按下快门时取景画面闪黑一下，作为拍摄反馈
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = shutterBlink.value }
                        .background(Color.Black),
                )
            }
        }
        BottomBar(
            lastPhoto = lastPhoto,
            useFrontCamera = settings.useFrontCamera,
            onThumbnailClick = { photo -> openInGallery(context, photo.uri) },
            onShutterClick = {
                if (!cameraInitialized) return@BottomBar
                scope.launch {
                    shutterBlink.snapTo(1f)
                    shutterBlink.animateTo(0f, tween(durationMillis = 200))
                }
                cameraController.takePhotoToGallery(
                    context = context,
                    onSuccess = { uri ->
                        scope.launch { lastPhoto = CapturedPhoto(uri, loadThumbnail(context, uri)) }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Photo capture failed", e)
                        Toast.makeText(context, R.string.photo_save_failed, Toast.LENGTH_SHORT).show()
                    },
                )
            },
            onSwitchClick = {
                val useFront = !settings.useFrontCamera
                // 相机还没初始化完时 hasCamera 会抛异常；设备没有对应镜头时返回 false
                if (runCatching { cameraController.hasCamera(cameraSelectorFor(useFront)) }.getOrDefault(false)) {
                    settings = settings.copy(useFrontCamera = useFront)
                }
            },
        )
    }
}

private fun cameraSelectorFor(useFrontCamera: Boolean): CameraSelector =
    if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

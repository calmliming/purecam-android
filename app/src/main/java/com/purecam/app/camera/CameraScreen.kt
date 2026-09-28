package com.purecam.app.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.purecam.app.R
import com.purecam.app.ui.theme.PureCamTheme
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

@Composable
private fun CameraContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // CameraController 已经处理好预览、拍照、点按对焦、双指缩放和照片方向。
    // 以后要做手动 ISO / 快门这类精细控制时，再换成 ProcessCameraProvider + Camera2 互操作。
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
    DisposableEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose { cameraController.unbind() }
    }

    var useFrontCamera by rememberSaveable { mutableStateOf(false) }
    var flashMode by rememberSaveable { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var lastPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }
    val shutterBlink = remember { Animatable(0f) }

    LaunchedEffect(useFrontCamera) {
        cameraController.cameraSelector =
            if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    }
    LaunchedEffect(flashMode) {
        cameraController.imageCaptureFlashMode = flashMode
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding(),
    ) {
        TopBar(
            flashMode = flashMode,
            showFlash = !useFrontCamera,
            onFlashClick = { flashMode = nextFlashMode(flashMode) },
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
            useFrontCamera = useFrontCamera,
            onThumbnailClick = { photo -> openInGallery(context, photo.uri) },
            onShutterClick = {
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
                val target =
                    if (useFrontCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                // 相机还没初始化完时 hasCamera 会抛异常；设备没有对应镜头时返回 false
                if (runCatching { cameraController.hasCamera(target) }.getOrDefault(false)) {
                    useFrontCamera = !useFrontCamera
                }
            },
        )
    }
}

@Composable
private fun TopBar(flashMode: Int, showFlash: Boolean, onFlashClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (showFlash) {
            FlashButton(flashMode = flashMode, onClick = onFlashClick)
        }
    }
}

@Composable
private fun FlashButton(flashMode: Int, onClick: () -> Unit) {
    val label = when (flashMode) {
        ImageCapture.FLASH_MODE_AUTO -> R.string.flash_auto
        ImageCapture.FLASH_MODE_ON -> R.string.flash_on
        else -> R.string.flash_off
    }
    val tint = when (flashMode) {
        ImageCapture.FLASH_MODE_ON -> MaterialTheme.colorScheme.primary
        ImageCapture.FLASH_MODE_AUTO -> Color.White
        else -> Color.White.copy(alpha = 0.6f)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_flash),
            contentDescription = stringResource(R.string.cd_flash),
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(label),
            color = tint,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun BottomBar(
    lastPhoto: CapturedPhoto?,
    useFrontCamera: Boolean,
    onThumbnailClick: (CapturedPhoto) -> Unit,
    onShutterClick: () -> Unit,
    onSwitchClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 36.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(photo = lastPhoto, onClick = onThumbnailClick)
        ShutterButton(onClick = onShutterClick)
        SwitchCameraButton(useFrontCamera = useFrontCamera, onClick = onSwitchClick)
    }
}

@Composable
private fun Thumbnail(photo: CapturedPhoto?, onClick: (CapturedPhoto) -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
            .then(if (photo != null) Modifier.clickable { onClick(photo) } else Modifier),
    ) {
        photo?.thumbnail?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.cd_last_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.88f else 1f, label = "shutterScale")
    val description = stringResource(R.string.cd_shutter)
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description }
            .border(4.dp, Color.White, CircleShape)
            .padding(9.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(Color.White, CircleShape),
    )
}

@Composable
private fun SwitchCameraButton(useFrontCamera: Boolean, onClick: () -> Unit) {
    // 每次切换镜头，图标转半圈
    val rotation by animateFloatAsState(targetValue = if (useFrontCamera) 180f else 0f, label = "switchRotation")
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_switch_camera),
            contentDescription = stringResource(R.string.cd_switch_camera),
            tint = Color.White,
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
        )
    }
}

private fun nextFlashMode(current: Int): Int = when (current) {
    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
    else -> ImageCapture.FLASH_MODE_OFF
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ControlsPreview() {
    PureCamTheme {
        Column {
            TopBar(flashMode = ImageCapture.FLASH_MODE_AUTO, showFlash = true, onFlashClick = {})
            BottomBar(
                lastPhoto = null,
                useFrontCamera = false,
                onThumbnailClick = {},
                onShutterClick = {},
                onSwitchClick = {},
            )
        }
    }
}

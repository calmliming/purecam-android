package com.purecam.app.camera

import androidx.camera.core.ImageCapture
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purecam.app.R
import com.purecam.app.ui.theme.PureCamTheme

/** 闪光灯按钮的档位，点击按 OFF → AUTO → ON 循环 */
enum class FlashMode(val imageCaptureMode: Int) {
    OFF(ImageCapture.FLASH_MODE_OFF),
    AUTO(ImageCapture.FLASH_MODE_AUTO),
    ON(ImageCapture.FLASH_MODE_ON),
    ;

    fun next(): FlashMode = when (this) {
        OFF -> AUTO
        AUTO -> ON
        ON -> OFF
    }
}

@Composable
fun TopBar(flashMode: FlashMode, showFlash: Boolean, onFlashClick: () -> Unit) {
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
private fun FlashButton(flashMode: FlashMode, onClick: () -> Unit) {
    val label = when (flashMode) {
        FlashMode.OFF -> R.string.flash_off
        FlashMode.AUTO -> R.string.flash_auto
        FlashMode.ON -> R.string.flash_on
    }
    val tint = when (flashMode) {
        FlashMode.OFF -> Color.White.copy(alpha = 0.6f)
        FlashMode.AUTO -> Color.White
        FlashMode.ON -> MaterialTheme.colorScheme.primary
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
fun BottomBar(
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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ControlsPreview() {
    PureCamTheme {
        Column {
            TopBar(flashMode = FlashMode.AUTO, showFlash = true, onFlashClick = {})
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

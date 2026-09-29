package com.purecam.app.camera

import androidx.camera.view.CameraController
import androidx.camera.view.TapToFocusInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purecam.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 点按对焦的圆框：点按处出现，从略大缩到正常大小；对焦成功后停留约 1 秒再淡出，失败时先变暗再淡出。
 * [info] 来自 [CameraController.getTapToFocusInfoState]，点按位置是 PreviewView 里的像素坐标，
 * 所以这一层要和 PreviewView 一样大。
 */
@Composable
fun FocusRing(info: TapToFocusInfo?, modifier: Modifier = Modifier) {
    // 刚进入界面（包括切换镜头后重建）时拿到的是之前那次点按的状态，不画
    val initialInfo = remember { info }
    var center by remember { mutableStateOf<Offset?>(null) }
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(info) {
        if (info == null || info === initialInfo) return@LaunchedEffect
        val point = info.tapPoint?.let { Offset(it.x, it.y) }
        // 对焦结果只作用于当前这个框：位置对不上的，是切换镜头之前那次点按的结果
        val isCurrentTap = point != null && point == center
        when (info.focusState) {
            CameraController.TAP_TO_FOCUS_STARTED -> {
                center = point ?: return@LaunchedEffect
                alpha.snapTo(1f)
                scale.snapTo(1.3f)
                scale.animateTo(1f, tween(durationMillis = 200))
            }
            CameraController.TAP_TO_FOCUS_FOCUSED -> if (isCurrentTap) {
                launch { scale.animateTo(1f) }
                delay(1000)
                alpha.animateTo(0f, tween(durationMillis = 300))
            }
            CameraController.TAP_TO_FOCUS_NOT_FOCUSED, CameraController.TAP_TO_FOCUS_FAILED -> if (isCurrentTap) {
                launch { scale.animateTo(1f) }
                alpha.animateTo(0.4f, tween(durationMillis = 150))
                delay(400)
                alpha.animateTo(0f, tween(durationMillis = 300))
            }
            else -> alpha.snapTo(0f)
        }
    }

    Canvas(modifier = modifier) {
        val ringCenter = center ?: return@Canvas
        if (alpha.value == 0f) return@Canvas
        val radius = 32.dp.toPx() * scale.value
        val strokeWidth = 1.5.dp.toPx()
        // 外面衬一圈半透明黑边，亮的背景上也看得清
        drawCircle(
            color = Color.Black,
            radius = radius,
            center = ringCenter,
            alpha = alpha.value * 0.25f,
            style = Stroke(width = strokeWidth * 3),
        )
        drawCircle(
            color = Color.White,
            radius = radius,
            center = ringCenter,
            alpha = alpha.value,
            style = Stroke(width = strokeWidth),
        )
    }
}

/**
 * 变焦倍数标签，比如 `2.4x`。缩放时显示；倍数不是 1x 时一直显示，点击回到 1x；回到 1x 后稍等一会儿淡出。
 * [zoomRatio] 为 null 表示相机还没打开。
 */
@Composable
fun ZoomRatioLabel(zoomRatio: Float?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val text = zoomRatio?.let(::formatZoomRatio)
    var previousText by remember { mutableStateOf(text) }
    var recentlyChanged by remember { mutableStateOf(false) }
    LaunchedEffect(text) {
        // 相机刚打开时拿到的第一个倍数不算缩放
        val changed = previousText != null && text != null && text != previousText
        previousText = text
        recentlyChanged = changed
        if (changed) {
            delay(1500)
            recentlyChanged = false
        }
    }

    AnimatedVisibility(
        visible = zoomRatio != null && (recentlyChanged || !isDefaultZoom(zoomRatio)),
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Text(
            text = text.orEmpty(),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    onClickLabel = stringResource(R.string.cd_reset_zoom),
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** 变焦倍数的显示文字：保留一位小数，整数倍不带小数，比如 `0.6x`、`1x`、`2.4x`、`10x` */
fun formatZoomRatio(ratio: Float): String {
    val tenths = (ratio * 10).roundToInt()
    return if (tenths % 10 == 0) "${tenths / 10}x" else "${tenths / 10}.${tenths % 10}x"
}

/** 按显示精度判断是不是 1x，免得 1.02 这类误差让标签一直不消失 */
fun isDefaultZoom(ratio: Float): Boolean = (ratio * 10).roundToInt() == 10

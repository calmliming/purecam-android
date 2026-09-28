package com.purecam.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 强调色，只用在闪光灯开启、主按钮等少数需要突出的地方 */
val Amber = Color(0xFFFFB300)

private val PureCamColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
)

@Composable
fun PureCamTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PureCamColorScheme, content = content)
}

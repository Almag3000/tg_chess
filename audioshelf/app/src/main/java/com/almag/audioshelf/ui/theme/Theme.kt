package com.almag.audioshelf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.almag.audioshelf.R
import com.almag.audioshelf.core.Status

/** Palette of the web version ("Книжная полка"). */
object Shelf {
    val bg = Color(0xFF141210)
    val bg2 = Color(0xFF1E1B18)
    val bg3 = Color(0xFF272320)
    val bg4 = Color(0xFF312D29)
    val line = Color(0xFF3A3530)
    val muted = Color(0xFF6B6560)
    val sub = Color(0xFF9A948E)
    val text = Color(0xFFEDE8E3)
    val white = Color(0xFFFAF8F5)

    val amber = Color(0xFFE8A83E)
    val amberDark = Color(0xFFC48A28)
    val amberBg = Color(0x1FE8A83E)

    val green = Color(0xFF6DBF8A)
    val greenBg = Color(0x1F6DBF8A)
    val blue = Color(0xFF6BA8D4)
    val blueBg = Color(0x1F6BA8D4)
    val red = Color(0xFFD47070)
    val redBg = Color(0x1FD47070)

    val board = Color(0xFF5A3F27)
    val boardDark = Color(0xFF3B2A1A)

    fun color(status: Status) = when (status) {
        Status.LISTENED -> green
        Status.LISTENING -> amber
        Status.DROPPED -> red
        Status.WANT -> blue
    }

    fun background(status: Status) = when (status) {
        Status.LISTENED -> greenBg
        Status.LISTENING -> amberBg
        Status.DROPPED -> redBg
        Status.WANT -> blueBg
    }
}

val Inter = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold),
    Font(R.font.inter_800, FontWeight.ExtraBold),
)

val Playfair = FontFamily(
    Font(R.font.playfair_600, FontWeight.SemiBold),
    Font(R.font.playfair_700, FontWeight.Bold),
    Font(R.font.playfair_500_italic, FontWeight.Medium, FontStyle.Italic),
)

private val base = TextStyle(fontFamily = Inter, color = Shelf.text)

private val typography = Typography(
    displayLarge = base.copy(fontFamily = Playfair, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 56.sp),
    headlineMedium = base.copy(fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = base.copy(fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = base.copy(fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = base.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall = base.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = base.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = base.copy(fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = base.copy(fontSize = 12.sp, lineHeight = 16.sp, color = Shelf.sub),
    labelLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Shelf.muted),
)

private val colors = darkColorScheme(
    primary = Shelf.amber,
    onPrimary = Color(0xFF1A1206),
    primaryContainer = Shelf.amberBg,
    onPrimaryContainer = Shelf.amber,
    secondary = Shelf.blue,
    onSecondary = Color(0xFF0B1620),
    tertiary = Shelf.green,
    background = Shelf.bg,
    onBackground = Shelf.text,
    surface = Shelf.bg,
    onSurface = Shelf.text,
    surfaceVariant = Shelf.bg3,
    onSurfaceVariant = Shelf.sub,
    surfaceContainerLowest = Shelf.bg,
    surfaceContainerLow = Shelf.bg2,
    surfaceContainer = Shelf.bg2,
    surfaceContainerHigh = Shelf.bg2,
    surfaceContainerHighest = Shelf.bg3,
    outline = Shelf.line,
    outlineVariant = Shelf.line,
    error = Shelf.red,
    onError = Color(0xFF200A0A),
    inverseSurface = Shelf.text,
    inverseOnSurface = Shelf.bg,
    scrim = Color(0xCC000000),
)

@Composable
fun ShelfTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}

package uz.carlcare.salesassistant.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object BrandColors {
    val Tecno = Color(0xFF00A8E8)
    val Infinix = Color(0xFF22C55E)
    val DarkBackground = Color(0xFF0B0F14)
    val DarkSurface = Color(0xFF131A23)
    val LightBackground = Color(0xFFF4F6F9)
    val LightSurface = Color(0xFFFFFFFF)
}

private val DarkScheme = darkColorScheme(
    primary = BrandColors.Tecno,
    onPrimary = Color(0xFF00131C),
    secondary = BrandColors.Infinix,
    onSecondary = Color(0xFF02130A),
    background = BrandColors.DarkBackground,
    onBackground = Color(0xFFE6EDF5),
    surface = BrandColors.DarkSurface,
    onSurface = Color(0xFFE6EDF5),
    surfaceVariant = Color(0xFF1B2430),
    onSurfaceVariant = Color(0xFF9FB0C2),
    outline = Color(0xFF2C3A49),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0077B6),
    onPrimary = Color.White,
    secondary = Color(0xFF0E9F6E),
    onSecondary = Color.White,
    background = BrandColors.LightBackground,
    onBackground = Color(0xFF101820),
    surface = BrandColors.LightSurface,
    onSurface = Color(0xFF101820),
    surfaceVariant = Color(0xFFE8EDF3),
    onSurfaceVariant = Color(0xFF4A5A6A),
    outline = Color(0xFFCBD5E1),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun SalesAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content,
    )
}

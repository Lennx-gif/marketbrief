package app.marketbrief.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    primaryContainer = Color(0xFF0060A0),
    secondary = Color(0xFF80DEEA),
    secondaryContainer = Color(0xFF006D77),
    tertiary = Color(0xFFA5D6A7),
    tertiaryContainer = Color(0xFF2E7D32),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF1E1E1E),
    background = Color(0xFF000000),
    error = Color(0xFFCF6679),
    onPrimary = Color(0xFF000000),
    onPrimaryContainer = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    onSecondaryContainer = Color(0xFFFFFFFF),
    onTertiary = Color(0xFF000000),
    onTertiaryContainer = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFB0B0B0),
    onBackground = Color(0xFFFFFFFF),
    onError = Color(0xFF000000),
    outline = Color(0xFF707070),
    outlineVariant = Color(0xFF404040),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0060A0),
    primaryContainer = Color(0xFFD6E4F0),
    secondary = Color(0xFF006D77),
    secondaryContainer = Color(0xFFD6F0F0),
    tertiary = Color(0xFF2E7D32),
    tertiaryContainer = Color(0xFFD6E8D6),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F0F0),
    background = Color(0xFFFAFAFA),
    error = Color(0xFFB00020),
    onPrimary = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onSecondaryContainer = Color(0xFFFFFFFF),
    onTertiary = Color(0xFF000000),
    onTertiaryContainer = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF404040),
    onBackground = Color(0xFF000000),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF707070),
    outlineVariant = Color(0xFFC0C0C0),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

@Composable
fun MarketBriefTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
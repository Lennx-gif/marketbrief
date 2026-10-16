package app.marketbrief.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Apple Liquid Glass Palette
private val AppleBlueLight = Color(0xFF0062E0) // 5.5:1 contrast on white
private val AppleBlueDark = Color(0xFF0A84FF)

private val LightColors = lightColorScheme(
    primary = AppleBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FC),
    onPrimaryContainer = AppleBlueLight,
    secondary = Color(0xFF5856D6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFEBFC),
    onSecondaryContainer = Color(0xFF5856D6),
    tertiary = Color(0xFF34C759),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F8ED),
    onTertiaryContainer = Color(0xFF1F8F5F),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6C6C70),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFC6C6C8), // 0.5dp hairline divider
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E5),
    onErrorContainer = Color(0xFFD70015)
)

private val DarkColors = darkColorScheme(
    primary = AppleBlueDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1C3A5E),
    onPrimaryContainer = AppleBlueDark,
    secondary = Color(0xFF5E5CE6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF282550),
    onSecondaryContainer = Color(0xFF9896FF),
    tertiary = Color(0xFF30D158),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF133820),
    onTertiaryContainer = Color(0xFF30D158),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF98989D),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF38383A), // 0.5dp hairline divider dark
    error = Color(0xFFFF453A),
    onError = Color.Black,
    errorContainer = Color(0xFF491212),
    onErrorContainer = Color(0xFFFF6961)
)

@Immutable
data class MarketBriefSemanticColors(
    val bullish: Color = Color(0xFF1F8F5F),
    val bearish: Color = Color(0xFFE0612F),
    val neutral: Color = Color(0xFF8E8E93),
    val hairline: Color,
    val cardSurface: Color,
    val subtlePill: Color
)

val LocalMarketBriefColors = staticCompositionLocalOf {
    MarketBriefSemanticColors(
        hairline = Color(0xFFC6C6C8),
        cardSurface = Color(0xFFFFFFFF),
        subtlePill = Color(0xFFF2F2F7)
    )
}

// Apple iOS Typography Scale with Negative Tracking on Headings
val AppleTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = (-0.8).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.6).sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.4).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)

object MarketBriefTheme {
    val colors: MarketBriefSemanticColors
        @Composable
        get() = LocalMarketBriefColors.current
}

@Composable
fun MarketBriefTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val semanticColors = if (darkTheme) {
        MarketBriefSemanticColors(
            bullish = Color(0xFF30D158),
            bearish = Color(0xFFFF453A),
            neutral = Color(0xFF8E8E93),
            hairline = Color(0xFF38383A),
            cardSurface = Color(0xFF1C1C1E),
            subtlePill = Color(0xFF2C2C2E)
        )
    } else {
        MarketBriefSemanticColors(
            bullish = Color(0xFF1F8F5F),
            bearish = Color(0xFFE0612F),
            neutral = Color(0xFF8E8E93),
            hairline = Color(0xFFC6C6C8),
            cardSurface = Color(0xFFFFFFFF),
            subtlePill = Color(0xFFF2F2F7)
        )
    }

    CompositionLocalProvider(LocalMarketBriefColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppleTypography,
            content = content
        )
    }
}
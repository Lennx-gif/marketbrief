package app.marketbrief.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized Design System Tokens for MarketBrief.
 * Ensures strict visual consistency, predictable sizing, and production-quality glassmorphism.
 */
object MarketBriefDesignTokens {
    // Brand Colors
    val TurquoiseAccent = Color(0xFF00D1C1)
    val TurquoiseDark = Color(0xFF009688)
    val TurquoiseActiveHighlight = Color(0xFF00D1C1).copy(alpha = 0.22f)
    val NearBlackBackground = Color(0xFF0D0E11)
    val DarkSurface = Color(0xFF16181D)
    val GlassSurface = Color(0xF4141519)
    val GlassBorder = Color.White.copy(alpha = 0.12f)
    val HairlineBorder = Color.White.copy(alpha = 0.10f)
    val InactiveMuted = Color(0xFF8E8E93)
    val InactiveMutedText = Color(0xFFA0A0A5)
    val CardSurface = Color(0xFF1B1D22)
    val ErrorRed = Color(0xFFFF453A)
    val BullishGreen = Color(0xFF30D158)
    val BearishOrange = Color(0xFFFF6961)
}

/**
 * Standardized icon action button with consistent touch target and border treatment.
 */
@Composable
fun MarketBriefIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp,
    tint: Color = MarketBriefDesignTokens.InactiveMuted,
    backgroundColor: Color = Color.White.copy(alpha = 0.06f),
    borderColor: Color = MarketBriefDesignTokens.HairlineBorder
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(0.5.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Standardized publisher / source badge with consistent padding and typography.
 */
@Composable
fun MarketBriefPublisherBadge(
    name: String,
    modifier: Modifier = Modifier,
    timeAgo: String? = null
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(0.5.dp, MarketBriefDesignTokens.HairlineBorder),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            val text = if (timeAgo != null) "$name · $timeAgo" else name
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MarketBriefDesignTokens.InactiveMutedText,
                maxLines = 1
            )
        }
    }
}

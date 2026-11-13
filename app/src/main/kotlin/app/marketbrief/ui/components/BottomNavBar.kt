package app.marketbrief.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNavigateToCalculator: (String) -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    val screens = listOf(
        ScreenItem("news", "Feed", Icons.AutoMirrored.Filled.Article),
        ScreenItem("analysis", "Analysis", Icons.AutoMirrored.Filled.TrendingUp),
        ScreenItem("calculator", "Calculator", Icons.Default.Calculate),
        ScreenItem("watchlist", "Watchlist", Icons.Default.Star)
    )

    // Frosted dark translucent glassmorphism surface with subtle border
    val glassBackgroundColor = Color(0xCC12171A)
    val glassBorderColor = Color(0x26FFFFFF)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.45f),
                    ambientColor = Color.Black.copy(alpha = 0.25f)
                )
                .border(
                    width = 1.dp,
                    color = glassBorderColor,
                    shape = CircleShape
                ),
            shape = CircleShape,
            color = glassBackgroundColor,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEach { screen ->
                    val isSelected = when (screen.route) {
                        "news" -> currentRoute == "news" || currentRoute.startsWith("news/")
                        "analysis" -> currentRoute.startsWith("analysis")
                        "calculator" -> currentRoute.startsWith("calculator")
                        "watchlist" -> currentRoute.startsWith("watchlist")
                        else -> currentRoute.startsWith(screen.route)
                    }

                    val activePillColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF1F4E52) else Color.Transparent,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "activePillColor"
                    )

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF5CE1E6) else Color(0xFF8E8E93),
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "iconTint"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Smooth animated cyan/teal pill capsule for active destination
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(38.dp)
                                .clip(CircleShape)
                                .background(activePillColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label,
                                tint = iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ScreenItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

package app.marketbrief.feature.news.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marketbrief.core.storage.UserPrefsRepository
import kotlinx.coroutines.launch

data class OnboardingCategoryItem(
    val id: String,
    val title: String,
    val descriptor: String,
    val icon: ImageVector
)

private val ONBOARDING_CATEGORIES = listOf(
    OnboardingCategoryItem("Forex", "Forex & Rates", "Currencies, FX & central bank rates", Icons.Default.AttachMoney),
    OnboardingCategoryItem("Crypto", "Crypto & DeFi", "Bitcoin, Ethereum & digital assets", Icons.Default.CurrencyBitcoin),
    OnboardingCategoryItem("Stocks", "Equities & Macro", "Indices, earnings & market trends", Icons.AutoMirrored.Filled.TrendingUp),
    OnboardingCategoryItem("Tech", "Tech & AI", "Semiconductors, AI & Big Tech", Icons.Default.Memory),
    OnboardingCategoryItem("Commodities", "Energy & Commodities", "Crude oil, gold & metals", Icons.Default.LocalGasStation),
    OnboardingCategoryItem("Trade", "Global Trade", "Supply chains, tariffs & shipping", Icons.Default.Public),
    OnboardingCategoryItem("Politics", "Politics & Policy", "Elections, regulations & fiscal law", Icons.Default.AccountBalance),
    OnboardingCategoryItem("Finance", "Banking & Finance", "Yields, inflation & debt markets", Icons.Default.Payments)
)

private val ObsidianBg = Color(0xFF0B0F12)
private val CardDarkSurface = Color(0xFF161B1E)
private val CyanAccent = Color(0xFF5CE1E6)
private val TealSelectedFill = Color(0xFF1F4E52)
private val SubtleBorder = Color(0x26FFFFFF)

@Composable
fun OnboardingScreen(
    userPrefsRepository: UserPrefsRepository,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCategories by remember {
        mutableStateOf(setOf("Forex", "Crypto", "Stocks", "Tech", "Commodities"))
    }

    fun finishOnboarding(categoriesToSave: Set<String>) {
        coroutineScope.launch {
            userPrefsRepository.setPreferredCategories(categoriesToSave)
            userPrefsRepository.completeOnboarding()
            onComplete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MarketBrief",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                TextButton(
                    onClick = { finishOnboarding(emptySet()) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Skip",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanAccent
                    )
                }
            }

            // Headline & Subtext
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Tailor your market brief",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Choose the sectors and topics you want prioritized in your feed. No account required.",
                    fontSize = 14.sp,
                    color = Color(0xFFA0A0A5),
                    lineHeight = 20.sp
                )
            }

            // Toggle Bar: Select All / Clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedCategories.size} of ${ONBOARDING_CATEGORIES.size} selected",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8E8E93)
                )

                TextButton(
                    onClick = {
                        selectedCategories = if (selectedCategories.size == ONBOARDING_CATEGORIES.size) {
                            emptySet()
                        } else {
                            ONBOARDING_CATEGORIES.map { it.id }.toSet()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (selectedCategories.size == ONBOARDING_CATEGORIES.size) "Clear all" else "Select all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanAccent
                    )
                }
            }

            // Interactive Category Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ONBOARDING_CATEGORIES, key = { it.id }) { item ->
                    val isSelected = item.id in selectedCategories

                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) TealSelectedFill else CardDarkSurface,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "cardBgColor"
                    )

                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) CyanAccent else SubtleBorder,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "cardBorderColor"
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = backgroundColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectedCategories = if (isSelected) {
                                    selectedCategories - item.id
                                } else {
                                    selectedCategories + item.id
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) CyanAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) CyanAccent else Color(0xFFC4C4C8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Text(
                                text = item.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                text = item.descriptor,
                                fontSize = 11.sp,
                                color = if (isSelected) Color(0xFFD4F8F9) else Color(0xFF8E8E93),
                                lineHeight = 15.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // Sticky Bottom CTA Pill
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent
        ) {
            val isEnabled = selectedCategories.isNotEmpty()

            Button(
                onClick = { finishOnboarding(selectedCategories) },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanAccent,
                    contentColor = Color(0xFF071316),
                    disabledContainerColor = CardDarkSurface,
                    disabledContentColor = Color(0xFF555A60)
                )
            ) {
                Text(
                    text = if (isEnabled) "Continue (${selectedCategories.size} selected)" else "Select at least 1 category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

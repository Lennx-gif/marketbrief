package app.marketbrief.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.marketbrief.BuildConfig
import app.marketbrief.core.entitlements.Plan
import app.marketbrief.core.entitlements.PlanSource
import app.marketbrief.feature.analysis.AnalysisScreen
import app.marketbrief.feature.calculator.CalculatorScreen
import app.marketbrief.feature.journal.JournalScreen
import app.marketbrief.feature.news.NewsScreen
import app.marketbrief.feature.news.ui.ArticleDetailScreen
import app.marketbrief.feature.news.ui.OnboardingScreen
import app.marketbrief.feature.settings.SettingsScreen
import app.marketbrief.feature.watchlist.WatchlistScreen
import app.marketbrief.ui.components.BottomNavBar
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@OptIn(ExperimentalMaterial3Api::class)
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppNavHostEntryPoint {
    fun newsRepository(): app.marketbrief.core.data.NewsRepository
    fun userPrefsRepository(): app.marketbrief.core.storage.UserPrefsRepository
}

private val TOP_LEVEL_TABS = listOf("news", "analysis", "calculator", "watchlist")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openArticleId: String? = null,
    openCalculatorSymbol: String? = null
) {
    val context = LocalContext.current
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppNavHostEntryPoint::class.java
        )
    }

    val userPrefsRepository = entryPoint.userPrefsRepository()
    val hasCompletedOnboarding by userPrefsRepository.hasCompletedOnboarding.collectAsStateWithLifecycle(initialValue = null)

    // Cold-launch splash prevention: show dark background until onboarding status is read
    if (hasCompletedOnboarding == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F12))
        )
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route.orEmpty()
    val isTopLevelRoute = currentRoute.substringBefore("?") in TOP_LEVEL_TABS

    var isBottomBarVisible by remember { mutableStateOf(true) }
    var activeCategoryFilter by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(currentRoute) {
        isBottomBarVisible = true
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(openArticleId) {
        if (!openArticleId.isNullOrBlank()) navController.navigate("article/$openArticleId")
    }
    LaunchedEffect(openCalculatorSymbol) {
        if (!openCalculatorSymbol.isNullOrBlank()) navController.navigate("calculator?symbol=$openCalculatorSymbol")
    }

    val startDestination = if (hasCompletedOnboarding == true) "news" else "onboarding"

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0B0F12))) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (isTopLevelRoute) {
                    GlobalTopAppBar(
                        onSavedClick = {
                            activeCategoryFilter = "Saved"
                            if (currentRoute.substringBefore("?") != "news") {
                                navController.navigate("news?filter=Saved") {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onSettingsClick = {
                            navController.navigate("settings")
                        }
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = if (isTopLevelRoute && isBottomBarVisible) Modifier.padding(bottom = 80.dp) else Modifier
                )
            },
            containerColor = Color(0xFF0B0F12)
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Zero-Auth Onboarding Route
                composable(
                    route = "onboarding",
                    enterTransition = { fadeIn(tween(200)) },
                    exitTransition = {
                        fadeOut(tween(220))
                    }
                ) {
                    OnboardingScreen(
                        userPrefsRepository = userPrefsRepository,
                        onComplete = {
                            navController.navigate("news") {
                                popUpTo("onboarding") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // Feed / News Tab
                composable(
                    route = "news",
                    enterTransition = {
                        val initial = initialState.destination.route?.substringBefore("?").orEmpty()
                        if (initial in TOP_LEVEL_TABS) {
                            val initialIdx = TOP_LEVEL_TABS.indexOf(initial)
                            val targetIdx = TOP_LEVEL_TABS.indexOf("news")
                            fadeIn(tween(220, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) {
                                if (targetIdx >= initialIdx) (it * 0.05f).toInt() else -(it * 0.05f).toInt()
                            }
                        } else {
                            fadeIn(tween(260, easing = FastOutSlowInEasing))
                        }
                    },
                    exitTransition = { fadeOut(tween(180)) }
                ) {
                    NewsScreen(
                        onOpenArticle = { articleId ->
                            navController.navigate("article/$articleId")
                        },
                        onOpenSettings = { navController.navigate("settings") },
                        onOpenCalculator = { symbol ->
                            navController.navigate("calculator?symbol=$symbol")
                        },
                        onOpenBookmarks = {
                            activeCategoryFilter = "Saved"
                        },
                        onBottomBarVisibilityChanged = { visible ->
                            isBottomBarVisible = visible
                        },
                        initialCategory = activeCategoryFilter
                    )
                }

                // Feed with filter argument
                composable(
                    route = "news?filter={filter}",
                    arguments = listOf(navArgument("filter") { type = NavType.StringType; defaultValue = "Trending" }),
                    enterTransition = { fadeIn(tween(220, easing = FastOutSlowInEasing)) },
                    exitTransition = { fadeOut(tween(180)) }
                ) { backStackEntry ->
                    val filterArg = backStackEntry.arguments?.getString("filter") ?: activeCategoryFilter
                    NewsScreen(
                        onOpenArticle = { articleId ->
                            navController.navigate("article/$articleId")
                        },
                        onOpenSettings = { navController.navigate("settings") },
                        onOpenCalculator = { symbol ->
                            navController.navigate("calculator?symbol=$symbol")
                        },
                        onOpenBookmarks = {
                            activeCategoryFilter = "Saved"
                        },
                        onBottomBarVisibilityChanged = { visible ->
                            isBottomBarVisible = visible
                        },
                        initialCategory = filterArg
                    )
                }

                // Article Detail / Twitter-Style Browser
                composable(
                    route = "article/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    enterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260, easing = FastOutSlowInEasing)) + fadeIn(tween(260))
                    },
                    exitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260, easing = FastOutSlowInEasing)) + fadeOut(tween(260))
                    },
                    popEnterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260, easing = FastOutSlowInEasing)) + fadeIn(tween(260))
                    },
                    popExitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260, easing = FastOutSlowInEasing)) + fadeOut(tween(260))
                    }
                ) { backStackEntry ->
                    val articleId = backStackEntry.arguments?.getString("id") ?: ""
                    val inAppFallbackEnabled by userPrefsRepository.inAppArticleFallback
                        .collectAsStateWithLifecycle(initialValue = true)
                    ArticleDetailScreen(
                        articleId = articleId,
                        newsRepository = entryPoint.newsRepository(),
                        onBack = { navController.popBackStack() },
                        onSizeTrade = { symbol ->
                            navController.navigate("calculator?symbol=$symbol")
                        },
                        inAppFallbackEnabled = inAppFallbackEnabled
                    )
                }

                // Settings Screen
                composable(
                    route = "settings",
                    enterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260, easing = FastOutSlowInEasing)) + fadeIn(tween(260))
                    },
                    exitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260, easing = FastOutSlowInEasing)) + fadeOut(tween(260))
                    },
                    popEnterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260, easing = FastOutSlowInEasing)) + fadeIn(tween(260))
                    },
                    popExitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260, easing = FastOutSlowInEasing)) + fadeOut(tween(260))
                    }
                ) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        versionName = BuildConfig.VERSION_NAME,
                        versionCode = BuildConfig.VERSION_CODE,
                        flavorName = BuildConfig.FLAVOR,
                        twelveDataKeyConfigured = BuildConfig.TWELVE_DATA_API_KEY.isNotBlank(),
                        onNavigateToOnboarding = {
                            navController.navigate("onboarding")
                        }
                    )
                }

                // Analysis Tab
                composable(
                    route = "analysis",
                    enterTransition = {
                        val initial = initialState.destination.route?.substringBefore("?").orEmpty()
                        if (initial in TOP_LEVEL_TABS) {
                            val initialIdx = TOP_LEVEL_TABS.indexOf(initial)
                            val targetIdx = TOP_LEVEL_TABS.indexOf("analysis")
                            fadeIn(tween(220, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) {
                                if (targetIdx >= initialIdx) (it * 0.05f).toInt() else -(it * 0.05f).toInt()
                            }
                        } else {
                            fadeIn(tween(260, easing = FastOutSlowInEasing))
                        }
                    },
                    exitTransition = { fadeOut(tween(180)) }
                ) {
                    AnalysisScreen()
                }

                // Calculator Tab
                composable(
                    route = "calculator",
                    enterTransition = {
                        val initial = initialState.destination.route?.substringBefore("?").orEmpty()
                        if (initial in TOP_LEVEL_TABS) {
                            val initialIdx = TOP_LEVEL_TABS.indexOf(initial)
                            val targetIdx = TOP_LEVEL_TABS.indexOf("calculator")
                            fadeIn(tween(220, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) {
                                if (targetIdx >= initialIdx) (it * 0.05f).toInt() else -(it * 0.05f).toInt()
                            }
                        } else {
                            fadeIn(tween(260, easing = FastOutSlowInEasing))
                        }
                    },
                    exitTransition = { fadeOut(tween(180)) }
                ) {
                    CalculatorScreen(initialSymbol = "", onOpenJournal = { navController.navigate("journal") })
                }

                composable(
                    route = "calculator?symbol={symbol}",
                    arguments = listOf(
                        navArgument("symbol") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        }
                    ),
                    enterTransition = { fadeIn(tween(220, easing = FastOutSlowInEasing)) },
                    exitTransition = { fadeOut(tween(180)) }
                ) { backStackEntry ->
                    val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
                    CalculatorScreen(initialSymbol = symbol, onOpenJournal = { navController.navigate("journal") })
                }

                composable("journal") {
                    JournalScreen(onBack = { navController.popBackStack() })
                }

                // Watchlist Tab
                composable(
                    route = "watchlist",
                    enterTransition = {
                        val initial = initialState.destination.route?.substringBefore("?").orEmpty()
                        if (initial in TOP_LEVEL_TABS) {
                            val initialIdx = TOP_LEVEL_TABS.indexOf(initial)
                            val targetIdx = TOP_LEVEL_TABS.indexOf("watchlist")
                            fadeIn(tween(220, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) {
                                if (targetIdx >= initialIdx) (it * 0.05f).toInt() else -(it * 0.05f).toInt()
                            }
                        } else {
                            fadeIn(tween(260, easing = FastOutSlowInEasing))
                        }
                    },
                    exitTransition = { fadeOut(tween(180)) }
                ) {
                    WatchlistScreen(
                        onNavigateToCalculator = { symbol ->
                            navController.navigate("calculator?symbol=$symbol")
                        }
                    )
                }
            }
        }

        // Floating Icon-Only Glassmorphic Bottom Navigation Pill
        if (isTopLevelRoute) {
            val density = LocalDensity.current
            val hideOffsetPx = with(density) { 120.dp.toPx() }
            val bottomBarAlpha by animateFloatAsState(
                targetValue = if (isBottomBarVisible) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                label = "bottomBarAlpha"
            )
            val bottomBarOffsetY by animateFloatAsState(
                targetValue = if (isBottomBarVisible) 0f else hideOffsetPx,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                label = "bottomBarOffsetY"
            )

            BottomNavBar(
                navController = navController,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        alpha = bottomBarAlpha
                        translationY = bottomBarOffsetY
                    }
            ) { symbol ->
                navController.navigate("calculator?symbol=$symbol")
            }
        }
    }
}

/**
 * Unified Global Top App Bar for MarketBrief.
 * Displays "MarketBrief" bold brand title on left with Saved and Settings action buttons on right.
 */
@Composable
private fun GlobalTopAppBar(
    onSavedClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xF40B0F12),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "MarketBrief",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Saved / Bookmarks Button (48dp touch target, 24dp icon)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSavedClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Saved stories",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Settings Button (48dp touch target, 24dp icon)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

class PlanSourceImpl : PlanSource {
    override val plan: Plan
        get() = when (BuildConfig.PLAN) {
            "PERSONAL" -> Plan.PERSONAL
            "STORE" -> Plan.PRO
            else -> Plan.FREE
        }
}

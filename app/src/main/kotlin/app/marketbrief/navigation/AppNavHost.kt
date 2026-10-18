package app.marketbrief.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.marketbrief.BuildConfig
import app.marketbrief.core.entitlements.Plan
import app.marketbrief.core.entitlements.PlanSource
import app.marketbrief.feature.analysis.AnalysisScreen
import app.marketbrief.feature.calculator.CalculatorScreen
import app.marketbrief.feature.news.NewsScreen
import app.marketbrief.feature.watchlist.WatchlistScreen
import app.marketbrief.ui.components.BottomNavBar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import app.marketbrief.core.storage.db.ArticleDao
import app.marketbrief.core.storage.db.BookmarkDao
import app.marketbrief.core.storage.db.DownloadDao
import app.marketbrief.feature.news.ui.ArticleDetailScreen
import app.marketbrief.feature.settings.SettingsScreen
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppNavHostEntryPoint {
    fun articleDao(): ArticleDao
    fun bookmarkDao(): BookmarkDao
    fun downloadDao(): DownloadDao
    fun newsRepository(): app.marketbrief.core.data.NewsRepository
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val planSource = PlanSourceImpl()
    val plan = planSource.plan

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val showBottomBar = currentRoute in listOf(
        "news",
        "analysis",
        "calculator",
        "calculator?symbol={symbol}",
        "watchlist"
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    navController = navController,
                    onNavigateToCalculator = { symbol ->
                        navController.navigate("calculator?symbol=$symbol")
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "news",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("news") {
                NewsScreen(
                    onNavigateToCalculator = { symbol ->
                        navController.navigate("calculator?symbol=$symbol")
                    },
                    onOpenArticle = { articleId ->
                        navController.navigate("article/$articleId")
                    },
                    onOpenSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            composable(
                route = "article/{id}",
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val articleId = backStackEntry.arguments?.getString("id") ?: ""
                val context = LocalContext.current
                val entryPoint = remember(context) {
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        AppNavHostEntryPoint::class.java
                    )
                }
                ArticleDetailScreen(
                    articleId = articleId,
                    articleDao = entryPoint.articleDao(),
                    bookmarkDao = entryPoint.bookmarkDao(),
                    downloadDao = entryPoint.downloadDao(),
                    newsRepository = entryPoint.newsRepository(),
                    onBack = { navController.popBackStack() },
                    onSizeTrade = { symbol ->
                        navController.navigate("calculator?symbol=$symbol")
                    }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "calculator?symbol={symbol}",
                arguments = listOf(
                    navArgument("symbol") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
                CalculatorScreen(initialSymbol = symbol)
            }
            composable("analysis") {
                AnalysisScreen()
            }
            composable("watchlist") {
                WatchlistScreen(
                    onNavigateToCalculator = { symbol ->
                        navController.navigate("calculator?symbol=$symbol")
                    }
                )
            }
        }
    }
}

class PlanSourceImpl : PlanSource {
    override val plan: Plan
        get() = when (BuildConfig.PLAN) {
            "PERSONAL" -> Plan.PERSONAL
            "STORE" -> Plan.PRO // Will be replaced with billing-backed source
            else -> Plan.FREE
        }
}
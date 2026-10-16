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

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val planSource = PlanSourceImpl()
    val plan = planSource.plan

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomNavBar(
                navController = navController,
                onNavigateToCalculator = { symbol ->
                    navController.navigate("calculator?symbol=$symbol")
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "news",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("news") {
                NewsScreen(onNavigateToCalculator = { symbol ->
                    navController.navigate("calculator?symbol=$symbol")
                })
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
                WatchlistScreen()
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
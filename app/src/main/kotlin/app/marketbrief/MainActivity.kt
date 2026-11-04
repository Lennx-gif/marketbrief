package app.marketbrief

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.core.ui.theme.MarketBriefTheme
import app.marketbrief.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var openArticleId by mutableStateOf<String?>(null)
    private var openCalculatorSymbol by mutableStateOf<String?>(null)

    @Inject
    lateinit var userPrefsRepository: UserPrefsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openArticleId = intent.getStringExtra(EXTRA_OPEN_ARTICLE_ID)
        openCalculatorSymbol = intent.getStringExtra(EXTRA_OPEN_CALCULATOR_SYMBOL)
        setContent {
            val themeMode by userPrefsRepository.themeMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")
            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemDark
            }

            MarketBriefTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(openArticleId = openArticleId, openCalculatorSymbol = openCalculatorSymbol)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openArticleId = intent.getStringExtra(EXTRA_OPEN_ARTICLE_ID)
        openCalculatorSymbol = intent.getStringExtra(EXTRA_OPEN_CALCULATOR_SYMBOL)
    }

    companion object {
        const val EXTRA_OPEN_ARTICLE_ID = "open_article_id"
        const val EXTRA_OPEN_CALCULATOR_SYMBOL = "open_calculator_symbol"
    }
}

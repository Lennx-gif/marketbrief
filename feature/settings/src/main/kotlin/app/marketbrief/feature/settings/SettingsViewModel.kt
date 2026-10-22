package app.marketbrief.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.data.positioning.PositioningRepository
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.core.storage.db.ArticleDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val homeCountry: String = "Kenya",
    val themeMode: String = "SYSTEM",
    val analysisView: String = "SENTIMENT",
    val wifiOnly: Boolean = true,
    val headlinesNotif: Boolean = true,
    val budgetUsed: Int = 0,
    val cachedArticleCount: Int = 0,
    val myfxbookConnected: Boolean = false,
    val myfxbookBusy: Boolean = false,
    val myfxbookMessage: String? = null
)

private data class GeneralPrefs(val country: String, val theme: String, val analysis: String)
private data class OperationalPrefs(val wifi: Boolean, val notif: Boolean, val budget: Int, val count: Int)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val articleDao: ArticleDao,
    private val positioning: PositioningRepository
) : ViewModel() {

    private val myfxbookBusy = MutableStateFlow(false)
    private val myfxbookMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { positioning.loadConnectionState() }
    }

    private val generalPrefsFlow = combine(
        prefs.homeCountry,
        prefs.themeMode,
        prefs.analysisView
    ) { country, theme, analysis ->
        GeneralPrefs(country, theme, analysis)
    }

    private val operationalPrefsFlow = combine(
        prefs.wifiOnlyDownload,
        prefs.headlinesNotification,
        prefs.dailyBudgetUsed,
        articleDao.observeCount()
    ) { wifi, notif, budget, count ->
        OperationalPrefs(wifi, notif, budget, count)
    }

    private val myfxbookFlow = combine(
        positioning.connected,
        myfxbookBusy,
        myfxbookMessage
    ) { connected, busy, message -> Triple(connected, busy, message) }

    val uiState: StateFlow<SettingsUiState> = combine(
        generalPrefsFlow,
        operationalPrefsFlow,
        myfxbookFlow
    ) { general, op, fx ->
        SettingsUiState(
            homeCountry = general.country,
            themeMode = general.theme,
            analysisView = general.analysis,
            wifiOnly = op.wifi,
            headlinesNotif = op.notif,
            budgetUsed = op.budget,
            cachedArticleCount = op.count,
            myfxbookConnected = fx.first,
            myfxbookBusy = fx.second,
            myfxbookMessage = fx.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setHomeCountry(country: String) {
        viewModelScope.launch { prefs.setHomeCountry(country) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setAnalysisView(view: String) {
        viewModelScope.launch { prefs.setAnalysisView(view) }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { prefs.setWifiOnlyDownload(enabled) }
    }

    fun setHeadlinesNotif(enabled: Boolean) {
        viewModelScope.launch { prefs.setHeadlinesNotification(enabled) }
    }

    fun clearCachedNews() {
        viewModelScope.launch {
            articleDao.clearUnsaved()
        }
    }

    fun connectMyfxbook(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            myfxbookMessage.value = "Enter your Myfxbook email and password."
            return
        }
        viewModelScope.launch {
            myfxbookBusy.value = true
            myfxbookMessage.value = null
            val result = positioning.connect(email, password)
            myfxbookMessage.value = result.exceptionOrNull()?.message
            myfxbookBusy.value = false
        }
    }

    fun disconnectMyfxbook() {
        viewModelScope.launch {
            positioning.disconnect()
            myfxbookMessage.value = null
        }
    }
}

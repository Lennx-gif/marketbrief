package app.marketbrief.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.data.positioning.PositioningRepository
import app.marketbrief.core.data.DailyDigestScheduler
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.core.storage.db.ArticleDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val homeCountry: String = "Kenya",
    val accountCurrency: String = "USD",
    val themeMode: String = "SYSTEM",
    val analysisView: String = "SENTIMENT",
    val wifiOnly: Boolean = true,
    val headlinesNotif: Boolean = true,
    val digestTime: String = "08:00",
    val homeOnlyDigest: Boolean = false,
    val kenyaTradeLens: Boolean = false,
    val quietHours: String = "22:00-07:00",
    val sources: List<String> = emptyList(),
    val sourcePreferences: Map<String, String> = emptyMap(),
    val budgetUsed: Int = 0,
    val twelveDataRemaining: Int = 800,
    val myfxbookRemaining: Int = 100,
    val cachedArticleCount: Int = 0,
    val myfxbookConnected: Boolean = false,
    val myfxbookBusy: Boolean = false,
    val myfxbookMessage: String? = null
)

private data class GeneralPrefs(val country: String, val theme: String, val analysis: String, val digestTime: String, val homeOnly: Boolean, val quietHours: String, val currency: String)
private data class OperationalPrefs(val wifi: Boolean, val notif: Boolean, val budget: Int, val count: Int, val kenyaLens: Boolean, val twelveRemaining: Int, val myfxbookRemaining: Int)
private data class BaseOperationalPrefs(val wifi: Boolean, val notif: Boolean, val budget: Int, val count: Int)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val articleDao: ArticleDao,
    private val positioning: PositioningRepository,
    private val digestScheduler: DailyDigestScheduler,
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val myfxbookBusy = MutableStateFlow(false)
    private val myfxbookMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { positioning.loadConnectionState() }
    }

    private val digestPrefsFlow = combine(prefs.digestTime, prefs.homeCountryOnlyDigest, prefs.quietHours) { time, homeOnly, quiet -> Triple(time, homeOnly, quiet) }

    private val generalPrefsFlow = combine(
        prefs.homeCountry,
        prefs.themeMode,
        prefs.analysisView,
        digestPrefsFlow,
        prefs.accountCurrency
    ) { country, theme, analysis, digest, currency ->
        GeneralPrefs(country, theme, analysis, digest.first, digest.second, digest.third, currency)
    }

    private val baseOperationalPrefsFlow = combine(
        prefs.wifiOnlyDownload,
        prefs.headlinesNotification,
        prefs.dailyBudgetUsed,
        articleDao.observeCount()
    ) { wifi, notif, budget, count -> BaseOperationalPrefs(wifi, notif, budget, count) }

    private val providerBudgetsFlow = combine(
        prefs.providerBudgetRemaining("twelve_data", 800),
        prefs.providerBudgetRemaining("myfxbook", 100)
    ) { twelve, myfx -> twelve to myfx }

    private val operationalPrefsFlow = combine(
        baseOperationalPrefsFlow,
        prefs.kenyaTradeLens,
        providerBudgetsFlow
    ) { base, kenya, provider ->
        OperationalPrefs(base.wifi, base.notif, base.budget, base.count, kenya, provider.first, provider.second)
    }

    private val myfxbookFlow = combine(
        positioning.connected,
        myfxbookBusy,
        myfxbookMessage
    ) { connected, busy, message -> Triple(connected, busy, message) }

    private val sourcePrefsFlow = combine(newsRepository.observeAvailableSources(), newsRepository.observeSourcePreferences()) { names, weights -> names to weights }

    val uiState: StateFlow<SettingsUiState> = combine(
        generalPrefsFlow,
        operationalPrefsFlow,
        myfxbookFlow,
        sourcePrefsFlow
    ) { general, op, fx, sources ->
        SettingsUiState(
            homeCountry = general.country,
            accountCurrency = general.currency,
            themeMode = general.theme,
            analysisView = general.analysis,
            wifiOnly = op.wifi,
            headlinesNotif = op.notif,
            digestTime = general.digestTime,
            homeOnlyDigest = general.homeOnly,
            kenyaTradeLens = op.kenyaLens,
            quietHours = general.quietHours,
            sources = sources.first,
            sourcePreferences = sources.second,
            budgetUsed = op.budget,
            twelveDataRemaining = op.twelveRemaining,
            myfxbookRemaining = op.myfxbookRemaining,
            cachedArticleCount = op.count,
            myfxbookConnected = fx.first,
            myfxbookBusy = fx.second,
            myfxbookMessage = fx.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setHomeCountry(country: String) {
        viewModelScope.launch { prefs.setHomeCountry(country) }
    }
    fun setAccountCurrency(currency: String) { viewModelScope.launch { prefs.setAccountCurrency(currency) } }

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
        viewModelScope.launch {
            prefs.setHeadlinesNotification(enabled)
            digestScheduler.reschedule(enabled, prefs.digestTime.first())
        }
    }

    fun setDigestTime(value: String) {
        viewModelScope.launch {
            prefs.setDigestTime(value)
            digestScheduler.reschedule(prefs.headlinesNotification.first(), value)
        }
    }

    fun setHomeOnlyDigest(enabled: Boolean) { viewModelScope.launch { prefs.setHomeCountryOnlyDigest(enabled) } }
    fun setQuietHours(value: String) { viewModelScope.launch { prefs.setQuietHours(value) } }
    fun setKenyaTradeLens(enabled: Boolean) { viewModelScope.launch { prefs.setKenyaTradeLens(enabled) } }
    fun setSourcePreference(source: String, weight: String) { viewModelScope.launch { newsRepository.setSourcePreference(source, weight) } }

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

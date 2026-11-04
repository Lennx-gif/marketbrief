package app.marketbrief.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPrefsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val homeCountry: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_HOME_COUNTRY] ?: "Kenya"
    }

    val themeMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val analysisView: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_ANALYSIS_VIEW] ?: "SENTIMENT"
    }

    val wifiOnlyDownload: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_WIFI_ONLY] ?: false
    }

    val headlinesNotification: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIF_HEADLINES] ?: true
    }

    val digestTime: Flow<String> = dataStore.data.map { it[KEY_DIGEST_TIME] ?: "08:00" }
    val homeCountryOnlyDigest: Flow<Boolean> = dataStore.data.map { it[KEY_HOME_ONLY_DIGEST] ?: false }
    val quietHours: Flow<String> = dataStore.data.map { it[KEY_QUIET_HOURS] ?: "22:00-07:00" }
    val kenyaTradeLens: Flow<Boolean> = dataStore.data.map { it[KEY_KENYA_LENS] ?: false }

    val dailyBudgetUsed: Flow<Int> = dataStore.data.map { prefs ->
        val currentDay = currentEpochDay()
        val storedDay = prefs[KEY_BUDGET_EPOCH_DAY] ?: 0L
        if (storedDay != currentDay) 0 else prefs[KEY_BUDGET_COUNT] ?: 0
    }

    fun providerBudgetRemaining(provider: String, limit: Int): Flow<Int> = dataStore.data.map { prefs ->
        val dayKey = providerDayKey(provider)
        val countKey = providerCountKey(provider)
        val used = if ((prefs[dayKey] ?: 0L) == currentEpochDay()) prefs[countKey] ?: 0 else 0
        (limit - used).coerceAtLeast(0)
    }

    suspend fun consumeProviderRequest(provider: String, cost: Int, limit: Int): Boolean {
        var allowed = false
        dataStore.edit { prefs ->
            val day = currentEpochDay()
            val oldDay = prefs[providerDayKey(provider)] ?: 0L
            val used = if (oldDay == day) prefs[providerCountKey(provider)] ?: 0 else 0
            if (used + cost <= limit) {
                prefs[providerDayKey(provider)] = day
                prefs[providerCountKey(provider)] = used + cost
                allowed = true
            }
        }
        return allowed
    }

    /** Last-used calculator inputs, so the screen opens where the user left off. */
    val calculatorDefaults: Flow<CalculatorDefaults> = dataStore.data.map { prefs ->
        CalculatorDefaults(
            instrumentSymbol = prefs[KEY_CALC_INSTRUMENT] ?: "EURUSD",
            accountCurrency = prefs[KEY_CALC_ACCOUNT_CURRENCY] ?: "USD",
            balanceText = prefs[KEY_CALC_BALANCE] ?: "10000",
            riskIsPercent = prefs[KEY_CALC_RISK_IS_PERCENT] ?: true,
            riskValueText = prefs[KEY_CALC_RISK_VALUE] ?: "1"
        )
    }
    val accountCurrency: Flow<String> = dataStore.data.map { it[KEY_CALC_ACCOUNT_CURRENCY] ?: "USD" }

    suspend fun setHomeCountry(country: String) {
        dataStore.edit { it[KEY_HOME_COUNTRY] = country }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setAnalysisView(view: String) {
        dataStore.edit { it[KEY_ANALYSIS_VIEW] = view }
    }

    suspend fun setWifiOnlyDownload(enabled: Boolean) {
        dataStore.edit { it[KEY_WIFI_ONLY] = enabled }
    }

    suspend fun setHeadlinesNotification(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIF_HEADLINES] = enabled }
    }

    suspend fun setDigestTime(value: String) { dataStore.edit { it[KEY_DIGEST_TIME] = value } }
    suspend fun setHomeCountryOnlyDigest(enabled: Boolean) { dataStore.edit { it[KEY_HOME_ONLY_DIGEST] = enabled } }
    suspend fun setQuietHours(value: String) { dataStore.edit { it[KEY_QUIET_HOURS] = value } }
    suspend fun setAccountCurrency(value: String) { dataStore.edit { it[KEY_CALC_ACCOUNT_CURRENCY] = value.uppercase() } }
    suspend fun setKenyaTradeLens(enabled: Boolean) { dataStore.edit { it[KEY_KENYA_LENS] = enabled } }

    /**
     * Checks if a request can be executed within the 100/day developer budget.
     * Automatically rolls over count at midnight UTC.
     */
    suspend fun canMakeRequest(count: Int = 1): Boolean {
        val currentDay = currentEpochDay()
        val prefs = dataStore.data.first()
        val storedDay = prefs[KEY_BUDGET_EPOCH_DAY] ?: 0L
        val currentCount = if (storedDay != currentDay) 0 else prefs[KEY_BUDGET_COUNT] ?: 0
        return (currentCount + count) <= DAILY_BUDGET_LIMIT
    }

    suspend fun recordRequest(count: Int = 1) {
        val currentDay = currentEpochDay()
        dataStore.edit { prefs ->
            val storedDay = prefs[KEY_BUDGET_EPOCH_DAY] ?: 0L
            val currentCount = if (storedDay != currentDay) 0 else prefs[KEY_BUDGET_COUNT] ?: 0
            prefs[KEY_BUDGET_EPOCH_DAY] = currentDay
            prefs[KEY_BUDGET_COUNT] = currentCount + count
        }
    }

    /** Saves only the fields that are provided; used to persist defaults as the user edits them. */
    suspend fun saveCalculatorDefaults(
        instrumentSymbol: String? = null,
        accountCurrency: String? = null,
        balanceText: String? = null,
        riskIsPercent: Boolean? = null,
        riskValueText: String? = null
    ) {
        dataStore.edit { prefs ->
            instrumentSymbol?.let { prefs[KEY_CALC_INSTRUMENT] = it }
            accountCurrency?.let { prefs[KEY_CALC_ACCOUNT_CURRENCY] = it }
            balanceText?.let { prefs[KEY_CALC_BALANCE] = it }
            riskIsPercent?.let { prefs[KEY_CALC_RISK_IS_PERCENT] = it }
            riskValueText?.let { prefs[KEY_CALC_RISK_VALUE] = it }
        }
    }

    companion object {
        const val DAILY_BUDGET_LIMIT = 100

        val KEY_HOME_COUNTRY = stringPreferencesKey("home_country")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ANALYSIS_VIEW = stringPreferencesKey("analysis_view")
        val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val KEY_NOTIF_HEADLINES = booleanPreferencesKey("notif_headlines")
        val KEY_DIGEST_TIME = stringPreferencesKey("digest_time")
        val KEY_HOME_ONLY_DIGEST = booleanPreferencesKey("home_only_digest")
        val KEY_QUIET_HOURS = stringPreferencesKey("quiet_hours")
        val KEY_KENYA_LENS = booleanPreferencesKey("kenya_trade_lens")

        private fun providerCountKey(provider: String) = intPreferencesKey("${provider.lowercase()}_budget_count")
        private fun providerDayKey(provider: String) = longPreferencesKey("${provider.lowercase()}_budget_day")
        val KEY_BUDGET_COUNT = intPreferencesKey("budget_count")
        val KEY_BUDGET_EPOCH_DAY = longPreferencesKey("budget_epoch_day")
        val KEY_CALC_INSTRUMENT = stringPreferencesKey("calc_last_instrument")
        val KEY_CALC_ACCOUNT_CURRENCY = stringPreferencesKey("calc_account_currency")
        val KEY_CALC_BALANCE = stringPreferencesKey("calc_balance")
        val KEY_CALC_RISK_IS_PERCENT = booleanPreferencesKey("calc_risk_is_percent")
        val KEY_CALC_RISK_VALUE = stringPreferencesKey("calc_risk_value")

        private fun currentEpochDay(): Long = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
    }
}

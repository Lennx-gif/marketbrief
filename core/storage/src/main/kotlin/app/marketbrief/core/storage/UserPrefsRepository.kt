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
        prefs[KEY_WIFI_ONLY] ?: true
    }

    val headlinesNotification: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIF_HEADLINES] ?: true
    }

    val dailyBudgetUsed: Flow<Int> = dataStore.data.map { prefs ->
        val currentDay = currentEpochDay()
        val storedDay = prefs[KEY_BUDGET_EPOCH_DAY] ?: 0L
        if (storedDay != currentDay) 0 else prefs[KEY_BUDGET_COUNT] ?: 0
    }

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

    companion object {
        const val DAILY_BUDGET_LIMIT = 100

        val KEY_HOME_COUNTRY = stringPreferencesKey("home_country")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ANALYSIS_VIEW = stringPreferencesKey("analysis_view")
        val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val KEY_NOTIF_HEADLINES = booleanPreferencesKey("notif_headlines")
        val KEY_BUDGET_COUNT = intPreferencesKey("budget_count")
        val KEY_BUDGET_EPOCH_DAY = longPreferencesKey("budget_epoch_day")

        private fun currentEpochDay(): Long = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
    }
}

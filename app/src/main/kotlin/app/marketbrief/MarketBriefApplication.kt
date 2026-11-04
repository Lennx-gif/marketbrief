package app.marketbrief

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.work.BackgroundWork
import app.marketbrief.work.DigestWork
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MarketBriefApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var prefs: UserPrefsRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        BackgroundWork.schedule(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (prefs.headlinesNotification.first()) DigestWork.scheduleNext(this@MarketBriefApplication, prefs.digestTime.first())
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val headlinesChannel = NotificationChannel(
                CHANNEL_HEADLINES_ID,
                "Breaking Headlines",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Breaking financial and macroeconomic market alerts"
            }

            val digestChannel = NotificationChannel(
                CHANNEL_DIGEST_ID,
                "Daily Digest",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Daily market summary and macroeconomic briefs"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Price Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Hourly market price level alerts" }

            notificationManager?.createNotificationChannels(listOf(headlinesChannel, digestChannel, alertsChannel))
        }
    }

    companion object {
        const val CHANNEL_HEADLINES_ID = "marketbrief_headlines"
        const val CHANNEL_DIGEST_ID = "marketbrief_digest"
        const val CHANNEL_ALERTS_ID = "marketbrief_alerts"
    }
}

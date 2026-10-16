package app.marketbrief

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MarketBriefApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
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

            notificationManager?.createNotificationChannels(listOf(headlinesChannel, digestChannel))
        }
    }

    companion object {
        const val CHANNEL_HEADLINES_ID = "marketbrief_headlines"
        const val CHANNEL_DIGEST_ID = "marketbrief_digest"
    }
}
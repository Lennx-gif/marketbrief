package app.marketbrief.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.marketbrief.MainActivity
import app.marketbrief.MarketBriefApplication
import app.marketbrief.core.data.NewsRefreshScheduler
import app.marketbrief.core.data.DailyDigestScheduler
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.data.RefreshResult
import app.marketbrief.core.data.TopArticleSelector
import app.marketbrief.core.model.CountryFilter
import app.marketbrief.core.model.FeedQuery
import app.marketbrief.core.storage.UserPrefsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@HiltWorker
class PrefetchWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val news: NewsRepository,
    private val prefs: UserPrefsRepository
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val articles = news.observeFeed(FeedQuery(country = CountryFilter.Home)).first()
        val fresh = articles.filter { article ->
            val body = news.getArticleBody(article.id)
            body?.kind == "FULL" && System.currentTimeMillis() - body.extractedAt < 24 * 60 * 60 * 1000L
        }.map { it.id }.toSet()
        val selected = TopArticleSelector.select(articles, fresh)
        selected.forEachIndexed { index, article ->
            if (isStopped) return Result.success()
            val outcome = news.extractArticleBody(article.id)
            if (outcome.isFailure && outcome.exceptionOrNull()?.message?.contains("offline", true) == true) return Result.retry()
            if (index < selected.lastIndex) delay(1_500)
        }
        return Result.success()
    }
}

@HiltWorker
class FeedRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val news: NewsRepository,
    private val prefs: UserPrefsRepository
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        // Keep 40 daily requests available for manual refreshes. Kenya lens adds a second query.
        val cost = if (prefs.kenyaTradeLens.first()) 2 else 1
        if (prefs.dailyBudgetUsed.first() + cost > 60 || !prefs.canMakeRequest(cost)) return Result.success()
        return when (news.refresh(FeedQuery(country = CountryFilter.Home))) {
            is RefreshResult.Success -> Result.success()
            RefreshResult.BudgetExhausted -> Result.success()
            is RefreshResult.Error -> Result.retry()
        }
    }
}

@HiltWorker
class DigestWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val news: NewsRepository,
    private val prefs: UserPrefsRepository
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        try {
            if (!prefs.headlinesNotification.first()) return Result.success()
            if (isWithinQuietHours(prefs.quietHours.first())) return Result.success()
            if (BuildNotificationPermission.required && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
            val countryOnly = prefs.homeCountryOnlyDigest.first()
            val country = prefs.homeCountry.first()
            val ranked = news.observeFeed(FeedQuery(country = CountryFilter.Home)).first()
            val feed = if (countryOnly) ranked.filter { it.region.equals(country, true) || it.title.contains(country, true) } else ranked
            val top = feed.take(3)
            if (top.isEmpty()) return Result.success()
            val article = top.first()
            val intent = Intent(applicationContext, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_ARTICLE_ID, article.id)
            val pending = PendingIntent.getActivity(applicationContext, article.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val lines = top.joinToString("\n") { "• ${it.title}" }
            val notification = NotificationCompat.Builder(applicationContext, MarketBriefApplication.CHANNEL_DIGEST_ID)
                .setSmallIcon(app.marketbrief.R.drawable.ic_launcher_foreground)
                .setContentTitle("Your MarketBrief daily digest")
                .setContentText(article.title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(lines))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(DIGEST_ID, notification)
            return Result.success()
        } finally {
            DigestWork.scheduleNext(applicationContext, prefs.digestTime.first())
        }
    }

    companion object { private const val DIGEST_ID = 81001 }
}

private fun isWithinQuietHours(value: String): Boolean = runCatching {
    val parts = value.split('-')
    if (parts.size != 2) return false
    val now = java.time.LocalTime.now(ZoneId.of("Africa/Nairobi"))
    val start = LocalTime.parse(parts[0])
    val end = LocalTime.parse(parts[1])
    if (start <= end) now >= start && now < end else now >= start || now < end
}.getOrDefault(false)

class WorkNewsRefreshScheduler(private val context: Context, private val prefs: UserPrefsRepository) : NewsRefreshScheduler {
    override suspend fun schedulePrefetch() {
        val wifiOnly = prefs.wifiOnlyDownload.first()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "prefetch-top3", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<PrefetchWorker>().setConstraints(constraints).build()
        )
    }
}

class WorkDailyDigestScheduler(private val context: Context) : DailyDigestScheduler {
    override suspend fun reschedule(enabled: Boolean, time: String) {
        if (enabled) DigestWork.scheduleNext(context, time)
        else WorkManager.getInstance(context).cancelUniqueWork("daily-digest")
    }
}

object DigestWork {
    fun scheduleNext(context: Context, time: String) {
        val localTime = runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.of(8, 0))
        val zone = ZoneId.of("Africa/Nairobi")
        val now = ZonedDateTime.now(zone)
        var next = now.toLocalDate().atTime(localTime).atZone(zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<DigestWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork("daily-digest", ExistingWorkPolicy.REPLACE, request)
    }
}

object BackgroundWork {
    fun schedule(context: Context) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()
        val request = PeriodicWorkRequestBuilder<FeedRefreshWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("feed-refresh", ExistingPeriodicWorkPolicy.KEEP, request)
        AlertWork.schedule(context)
    }
}

private object BuildNotificationPermission { val required get() = android.os.Build.VERSION.SDK_INT >= 33 }

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
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import app.marketbrief.MainActivity
import app.marketbrief.MarketBriefApplication
import app.marketbrief.core.data.AlertDirection
import app.marketbrief.core.data.AlertEvaluator
import app.marketbrief.core.data.AlertSnapshot
import app.marketbrief.core.data.MarketDataRepository
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.storage.db.PriceAlertDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@HiltWorker
class AlertCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val marketData: MarketDataRepository,
    private val alerts: PriceAlertDao
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val active = alerts.active()
        val bySymbol = active.groupBy { it.symbol }.entries.take(8)
        if (bySymbol.isEmpty()) return Result.success()
        val ids = bySymbol.map { (symbol, items) -> AssetId(symbol, runCatching { AssetClass.valueOf(items.first().assetClass) }.getOrDefault(AssetClass.FOREX)) }
        marketData.refreshQuotes(ids)
        val quotes = marketData.quotes.first()
        bySymbol.forEach { (symbol, items) ->
            val assetId = ids.first { it.symbol == symbol }
            val price = quotes[assetId]?.mid ?: return@forEach
            items.forEach itemLoop@{ stored ->
                val target = stored.targetPrice.toDoubleOrNull() ?: return@itemLoop
                val snapshot = AlertSnapshot(stored.id, target, AlertDirection.valueOf(stored.direction), stored.lastCheckedPrice?.toDoubleOrNull(), stored.triggeredAt != null)
                val evaluated = AlertEvaluator.evaluate(snapshot, price)
                alerts.upsert(stored.copy(lastCheckedPrice = BigDecimal.valueOf(price).stripTrailingZeros().toPlainString(), triggeredAt = if (evaluated.crossed) System.currentTimeMillis() else stored.triggeredAt))
                if (evaluated.crossed && (android.os.Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)) {
                    val intent = Intent(applicationContext, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_CALCULATOR_SYMBOL, symbol)
                    val pending = PendingIntent.getActivity(applicationContext, stored.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val notification = NotificationCompat.Builder(applicationContext, MarketBriefApplication.CHANNEL_ALERTS_ID)
                        .setSmallIcon(app.marketbrief.R.drawable.ic_launcher_foreground)
                        .setContentTitle("Price level reached: $symbol")
                        .setContentText("Price crossed ${stored.targetPrice}")
                        .setContentIntent(pending).setAutoCancel(true).build()
                    NotificationManagerCompat.from(applicationContext).notify(stored.id.hashCode(), notification)
                }
            }
        }
        return Result.success()
    }
}

object AlertWork {
    fun schedule(context: Context) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()
        val request = PeriodicWorkRequestBuilder<AlertCheckWorker>(1, TimeUnit.HOURS).setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("price-alert-check", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

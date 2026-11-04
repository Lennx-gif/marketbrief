package app.marketbrief.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.marketbrief.MainActivity
import app.marketbrief.MarketBriefApplication
import app.marketbrief.R
import app.marketbrief.core.data.PriceAlertNotifier

class AndroidPriceAlertNotifier(private val context: Context) : PriceAlertNotifier {
    override fun notify(alertId: String, symbol: String, target: String) {
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_CALCULATOR_SYMBOL, symbol)
        val pending = PendingIntent.getActivity(context, alertId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, MarketBriefApplication.CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Price level reached: $symbol")
            .setContentText("Price crossed $target")
            .setContentIntent(pending).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify(alertId.hashCode(), notification)
    }
}

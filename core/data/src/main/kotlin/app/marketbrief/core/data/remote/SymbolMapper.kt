package app.marketbrief.core.data.remote

import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Timeframe

/**
 * Translates between the app's symbols (EURUSD) and Twelve Data's (EUR/USD).
 */
object SymbolMapper {

    /** Index tickers differ per provider, and index access on the free plan is unconfirmed. */
    private val indexMap = mapOf(
        "SPX500" to "SPX",
        "US500" to "SPX",
        "US30" to "DJI",
        "NAS100" to "NDX"
    )

    private val cryptoQuotes = listOf("USDT", "USDC", "USD", "EUR", "BTC")

    fun toTwelveData(id: AssetId): String {
        val s = id.symbol.trim().uppercase().replace("/", "")
        return when (id.assetClass) {
            AssetClass.FOREX, AssetClass.COMMODITY ->
                if (s.length == 6) "${s.take(3)}/${s.drop(3)}" else s
            AssetClass.CRYPTO -> {
                val quote = cryptoQuotes.firstOrNull { s.endsWith(it) && s.length > it.length }
                if (quote != null) "${s.removeSuffix(quote)}/$quote" else s
            }
            AssetClass.INDEX -> indexMap[s] ?: s
            AssetClass.STOCK, AssetClass.EQUITY -> s
        }
    }

    fun fromTwelveData(symbol: String, assetClass: AssetClass): AssetId =
        AssetId(symbol.replace("/", "").uppercase(), assetClass)

    fun interval(timeframe: Timeframe): String = when (timeframe) {
        Timeframe.M1 -> "1min"
        Timeframe.M5 -> "5min"
        Timeframe.M15 -> "15min"
        Timeframe.H1 -> "1h"
        Timeframe.H4 -> "4h"
        Timeframe.D1 -> "1day"
        Timeframe.W1 -> "1week"
        Timeframe.MN1 -> "1month"
    }
}

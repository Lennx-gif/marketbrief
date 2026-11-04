package app.marketbrief.core.risk

/** Minimal USD crosses needed to convert a trade's quote currency into its account currency. */
object RateNeeds {
    private val USD_CROSSES = mapOf(
        "EUR" to "EURUSD", "GBP" to "GBPUSD", "JPY" to "USDJPY",
        "CHF" to "USDCHF", "CAD" to "USDCAD", "AUD" to "AUDUSD", "NZD" to "NZDUSD"
    )

    fun forTrade(instrument: Instrument, accountCurrency: CurrencyCode): Set<String> {
        val quoteCurrency = instrument.quoteCurrency
        if (quoteCurrency == accountCurrency) return emptySet()
        return listOf(quoteCurrency, accountCurrency)
            .filter { it.code != "USD" }
            .map { currency -> USD_CROSSES[currency.code] ?: "${currency.code}USD" }
            .toSet()
            .take(2)
            .toSet()
    }
}

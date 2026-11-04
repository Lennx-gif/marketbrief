package app.marketbrief.core.storage

/** Snapshot of the calculator inputs persisted in DataStore between sessions. */
data class CalculatorDefaults(
    val instrumentSymbol: String,
    val accountCurrency: String,
    val balanceText: String,
    val riskIsPercent: Boolean,
    val riskValueText: String
)

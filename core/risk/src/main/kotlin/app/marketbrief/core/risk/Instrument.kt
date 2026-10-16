package app.marketbrief.core.risk

import java.math.BigDecimal

/** Currency code normalised to upper case, e.g. "USD", "JPY", "XAU", "USDT". */
@JvmInline
value class CurrencyCode(val code: String) {
    init {
        require(code.length in 3..6 && code.all { it in 'A'..'Z' }) { "Invalid currency code: '$code'" }
    }

    override fun toString(): String = code
}

enum class AssetClass { FOREX, METAL, INDEX, CRYPTO, STOCK }

/**
 * Everything the sizer needs to know about a tradable instrument.
 *
 * The sizing maths only uses [contractSize], [quoteCurrency] and the lot constraints. [pipSize] is used
 * to read stop and take-profit distances in pips, and for display. That is what keeps the calculator
 * asset-agnostic: a new asset class is a new [Instrument] value, not new maths.
 *
 * @property baseCurrency what one unit of the instrument is denominated in, if it is a currency
 *   (EUR in EURUSD, XAU in XAUUSD). Null for indices and stocks.
 * @property pipSize the price increment treated as one pip or point.
 * @property contractSize units of the instrument in 1.0 lot (100,000 for standard forex).
 */
data class Instrument(
    val symbol: String,
    val assetClass: AssetClass,
    val quoteCurrency: CurrencyCode,
    val baseCurrency: CurrencyCode? = null,
    val pipSize: BigDecimal,
    val contractSize: BigDecimal,
    val lotStep: BigDecimal = BigDecimal("0.01"),
    val minLot: BigDecimal = BigDecimal("0.01"),
    val maxLot: BigDecimal = BigDecimal("100"),
) {
    init {
        require(symbol.isNotBlank()) { "Symbol must not be blank" }
        require(pipSize.signum() > 0) { "pipSize must be greater than zero" }
        require(contractSize.signum() > 0) { "contractSize must be greater than zero" }
        require(lotStep.signum() > 0) { "lotStep must be greater than zero" }
        require(minLot.signum() > 0) { "minLot must be greater than zero" }
        require(maxLot >= minLot) { "maxLot must be at least minLot" }
    }

    companion object {
        /**
         * Standard forex pair from a six-letter symbol such as "EURUSD" or "EUR/USD".
         * Pip size is 0.01 for JPY-quoted pairs and 0.0001 otherwise. Contract size is 100,000.
         * Override the lot constraints to match your broker.
         */
        fun forex(
            symbol: String,
            lotStep: BigDecimal = BigDecimal("0.01"),
            minLot: BigDecimal = BigDecimal("0.01"),
            maxLot: BigDecimal = BigDecimal("100"),
        ): Instrument {
            val letters = symbol.filter { it.isLetter() }.uppercase()
            require(letters.length == 6) { "Forex symbol must be six letters like EURUSD or EUR/USD: '$symbol'" }
            val base = CurrencyCode(letters.take(3))
            val quote = CurrencyCode(letters.drop(3))
            val pip = if (quote.code == "JPY") BigDecimal("0.01") else BigDecimal("0.0001")
            return Instrument(
                symbol = letters,
                assetClass = AssetClass.FOREX,
                quoteCurrency = quote,
                baseCurrency = base,
                pipSize = pip,
                contractSize = BigDecimal("100000"),
                lotStep = lotStep,
                minLot = minLot,
                maxLot = maxLot,
            )
        }
    }
}

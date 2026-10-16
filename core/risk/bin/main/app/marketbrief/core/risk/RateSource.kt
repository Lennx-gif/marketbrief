package app.marketbrief.core.risk

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** 34 significant digits. Intermediate results are never rounded below this. */
internal val CALC = MathContext(34, RoundingMode.HALF_EVEN)

/**
 * Supplies exchange rates for converting a trade's profit and loss into the account currency.
 *
 * The interface is synchronous on purpose. The data layer fetches live prices asynchronously, builds a
 * [PairRateTable] from them, and passes that in. The core module never touches the network.
 */
interface RateSource {
    /** Units of [to] per one unit of [from], or null if this source cannot convert between them. */
    fun rate(from: CurrencyCode, to: CurrencyCode): BigDecimal?
}

/**
 * Rates derived from a table of pair prices, where each price is quote currency per one base currency
 * (EURUSD = 1.1000 means 1 EUR = 1.1000 USD).
 *
 * Conversion tries the pair directly, then its inverse, then one hop through a pivot currency
 * (USD first, so JPY to EUR works from just USDJPY and EURUSD).
 */
class PairRateTable(pairs: Map<String, BigDecimal>) : RateSource {

    private val prices: Map<Pair<CurrencyCode, CurrencyCode>, BigDecimal> =
        pairs.entries.associate { (key, price) ->
            val letters = key.filter { it.isLetter() }.uppercase()
            require(letters.length == 6) { "Pair key must look like EURUSD or EUR/USD: '$key'" }
            require(price.signum() > 0) { "Price for $key must be greater than zero" }
            (CurrencyCode(letters.take(3)) to CurrencyCode(letters.drop(3))) to price
        }

    private val pivots: List<CurrencyCode> =
        prices.keys
            .flatMap { listOf(it.first, it.second) }
            .distinct()
            .sortedBy { if (it.code == "USD") "" else it.code }

    override fun rate(from: CurrencyCode, to: CurrencyCode): BigDecimal? {
        if (from == to) return BigDecimal.ONE
        oneStep(from, to)?.let { return it }
        for (pivot in pivots) {
            if (pivot == from || pivot == to) continue
            val first = oneStep(from, pivot) ?: continue
            val second = oneStep(pivot, to) ?: continue
            return first.multiply(second, CALC)
        }
        return null
    }

    private fun oneStep(from: CurrencyCode, to: CurrencyCode): BigDecimal? =
        prices[from to to] ?: prices[to to from]?.let { BigDecimal.ONE.divide(it, CALC) }
}

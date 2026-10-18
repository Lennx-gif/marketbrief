package app.marketbrief.core.risk

import java.math.BigDecimal

/**
 * Immutable lookup of instruments by symbol.
 *
 * Unknown symbols return null on purpose. A six-letter symbol like BTCUSD must never be silently
 * treated as a forex pair with a 100,000 contract. Register anything that is not standard forex
 * explicitly with [withInstrument].
 */
class InstrumentCatalog private constructor(private val bySymbol: Map<String, Instrument>) {

    fun find(symbol: String): Instrument? = bySymbol[normalise(symbol)]

    fun all(): List<Instrument> = bySymbol.values.sortedBy { it.symbol }

    fun withInstrument(instrument: Instrument): InstrumentCatalog =
        InstrumentCatalog(bySymbol + (normalise(instrument.symbol) to instrument))

    companion object {
        private fun normalise(symbol: String): String = symbol.filter { it.isLetterOrDigit() }.uppercase()

        /** The 28 standard majors and crosses, in standard market quoting order. */
        private val FOREX_SYMBOLS = listOf(
            "EURUSD", "GBPUSD", "AUDUSD", "NZDUSD", "USDJPY", "USDCHF", "USDCAD",
            "EURGBP", "EURJPY", "EURCHF", "EURAUD", "EURCAD", "EURNZD",
            "GBPJPY", "GBPCHF", "GBPAUD", "GBPCAD", "GBPNZD",
            "AUDJPY", "AUDCHF", "AUDCAD", "AUDNZD",
            "NZDJPY", "NZDCHF", "NZDCAD",
            "CADJPY", "CADCHF", "CHFJPY",
        )

        /**
         * Example of a non-forex instrument. Contract size and pip size vary by broker, so check
         * these against your broker's specification before trusting the pip display.
         */
        private val GOLD = Instrument(
            symbol = "XAUUSD",
            assetClass = AssetClass.METAL,
            quoteCurrency = CurrencyCode("USD"),
            baseCurrency = CurrencyCode("XAU"),
            pipSize = BigDecimal("0.1"),
            contractSize = BigDecimal("100"),
        )

        private val STOCKS = listOf(
            Instrument.stock("AAPL"),
            Instrument.stock("MSFT"),
            Instrument.stock("NVDA"),
            Instrument.stock("TSLA"),
            Instrument.stock("AMZN"),
            Instrument.stock("GOOGL"),
        )

        private val INDICES = listOf(
            Instrument.index("SPX500"),
            Instrument.index("US30"),
            Instrument.index("NAS100"),
        )

        private val CRYPTO = listOf(
            Instrument.crypto("BTCUSD", baseCurrency = "BTC"),
            Instrument.crypto("ETHUSD", baseCurrency = "ETH"),
        )

        fun default(): InstrumentCatalog =
            InstrumentCatalog(
                (FOREX_SYMBOLS.map { Instrument.forex(it) } + GOLD + STOCKS + INDICES + CRYPTO)
                    .associateBy { it.symbol }
            )

        fun empty(): InstrumentCatalog = InstrumentCatalog(emptyMap())
    }
}

package app.marketbrief.core.analysis.sentiment

/**
 * Extracts currency codes and asset symbols from headline and summary text.
 */
class AssetTagger {

    fun tag(text: String): Set<String> {
        val lower = text.lowercase()
        val tags = mutableSetOf<String>()

        KEYWORD_MAP.forEach { (keyword, symbol) ->
            if (lower.contains(keyword)) {
                tags.add(symbol)
            }
        }
        return tags
    }

    companion object {
        val KEYWORD_MAP = mapOf(
            // Currencies & Central Banks
            "dollar" to "USD", "fed " to "USD", "federal reserve" to "USD", "powell" to "USD", "u.s. dollar" to "USD",
            "euro" to "EUR", "ecb" to "EUR", "lagarde" to "EUR", "eurozone" to "EUR",
            "pound" to "GBP", "sterling" to "GBP", "bank of england" to "GBP", "boe" to "GBP",
            "yen" to "JPY", "bank of japan" to "JPY", "boj" to "JPY",
            "yuan" to "CNY", "renminbi" to "CNY",
            "franc" to "CHF", "snb" to "CHF",
            "loonie" to "CAD", "bank of canada" to "CAD",
            "aussie" to "AUD", "rba" to "AUD",
            "kiwi" to "NZD", "rbnz" to "NZD",
            "shilling" to "KES", "cbk" to "KES", "kenya" to "KES",

            // Metals & Commodities
            "gold" to "XAU", "bullion" to "XAU", "xau" to "XAU",
            "silver" to "XAG", "xag" to "XAG",
            "oil" to "BRENT", "crude" to "WTI", "opec" to "OIL",

            // Crypto
            "bitcoin" to "BTC", "btc" to "BTC",
            "ethereum" to "ETH", "eth" to "ETH",

            // Equities & Indices
            "sp500" to "SPX500", "s&p" to "SPX500", "wall street" to "US30", "nasdaq" to "NAS100",
            "apple" to "AAPL", "aapl" to "AAPL",
            "microsoft" to "MSFT", "msft" to "MSFT",
            "nvidia" to "NVDA", "nvda" to "NVDA",
            "tesla" to "TSLA", "tsla" to "TSLA",
            "amazon" to "AMZN", "amzn" to "AMZN",
            "google" to "GOOGL", "alphabet" to "GOOGL", "googl" to "GOOGL"
        )
    }
}

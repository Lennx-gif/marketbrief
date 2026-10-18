package app.marketbrief.core.analysis.sentiment

import app.marketbrief.core.model.Category

/**
 * Automatically classifies news headlines and summaries into appropriate categories.
 */
class CategoryClassifier {

    fun classify(title: String, summary: String = ""): List<Category> {
        val text = "$title $summary".lowercase()
        val categories = mutableSetOf<Category>()

        if (matches(text, TRADE_KEYWORDS)) categories.add(Category.Trade)
        if (matches(text, FINANCE_KEYWORDS)) categories.add(Category.Finance)
        if (matches(text, FOREX_KEYWORDS)) categories.add(Category.Forex)
        if (matches(text, TECH_KEYWORDS)) categories.add(Category.Tech)
        if (matches(text, POLITICS_KEYWORDS)) categories.add(Category.Politics)
        if (matches(text, STOCKS_KEYWORDS)) categories.add(Category.Stocks)
        if (matches(text, CRYPTO_KEYWORDS)) categories.add(Category.Crypto)
        if (matches(text, COMMODITIES_KEYWORDS)) categories.add(Category.Commodities)

        // Ensure at least one financial category is assigned
        if (categories.isEmpty()) {
            categories.add(Category.Finance)
        }

        return categories.toList()
    }

    private fun matches(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    companion object {
        private val TRADE_KEYWORDS = listOf("trade", "export", "import", "tariff", "deficit", "cargo", "shipment", "treaty", "port", "customs", "wto")
        private val FINANCE_KEYWORDS = listOf("market", "bank", "treasury", "bond", "yield", "rate", "fed", "inflation", "cpi", "debt", "recession", "gdp", "economy", "fiscal", "loan")
        private val FOREX_KEYWORDS = listOf("forex", "fx", "currency", "dollar", "euro", "yen", "sterling", "pound", "shilling", "exchange rate", "devaluation", "central bank")
        private val TECH_KEYWORDS = listOf("ai", "artificial intelligence", "tech", "semiconductor", "chip", "software", "cloud", "apple", "microsoft", "nvidia", "google", "meta", "cyber")
        private val POLITICS_KEYWORDS = listOf("election", "president", "congress", "parliament", "government", "policy", "law", "sanction", "minister", "senate", "white house", "geopolitic")
        private val STOCKS_KEYWORDS = listOf("stock", "share", "equity", "nasdaq", "s&p", "dow", "earnings", "dividend", "ipo", "wall street", "nyse", "rally", "selloff")
        private val CRYPTO_KEYWORDS = listOf("bitcoin", "crypto", "ethereum", "btc", "eth", "blockchain", "token", "binance", "coinbase", "solana")
        private val COMMODITIES_KEYWORDS = listOf("gold", "oil", "crude", "brent", "silver", "commodity", "grain", "wheat", "copper", "energy", "opec")
    }
}

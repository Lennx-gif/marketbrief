package app.marketbrief.core.analysis.sentiment

import app.marketbrief.core.model.Article
import app.marketbrief.core.model.Sentiment

/**
 * Aggregates sentiment across articles per asset and computes currency pair sentiment differentials.
 */
class SentimentAggregator(
    private val scorer: SentimentScorer = SentimentScorer(),
    private val tagger: AssetTagger = AssetTagger()
) {

    /**
     * Aggregates article sentiments by asset and pair.
     */
    fun aggregate(articles: List<Article>): Map<String, Sentiment> {
        val scoresByAsset = mutableMapOf<String, MutableList<Float>>()

        for (article in articles) {
            val text = "${article.title} ${article.summary}"
            val score = article.sentiment ?: scorer.score(text)
            val tags = tagger.tag(text)

            for (tag in tags) {
                scoresByAsset.getOrPut(tag) { mutableListOf() }.add(score)
            }
        }

        val assetSentiments = scoresByAsset.mapValues { (_, scores) ->
            val avg = scores.average().toFloat()
            Sentiment(
                score = avg,
                label = labelFor(avg),
                headlineCount = scores.size
            )
        }.toMutableMap()

        // Compute major currency pair scores: pairScore = baseScore - quoteScore
        val pairs = listOf(
            "EURUSD" to ("EUR" to "USD"),
            "GBPUSD" to ("GBP" to "USD"),
            "USDJPY" to ("USD" to "JPY"),
            "USDCHF" to ("USD" to "CHF"),
            "USDCAD" to ("USD" to "CAD"),
            "AUDUSD" to ("AUD" to "USD"),
            "NZDUSD" to ("NZD" to "USD"),
            "EURGBP" to ("EUR" to "GBP"),
            "EURJPY" to ("EUR" to "JPY"),
            "GBPJPY" to ("GBP" to "JPY")
        )

        for ((pair, components) in pairs) {
            val base = assetSentiments[components.first]
            val quote = assetSentiments[components.second]
            if (base != null || quote != null) {
                val baseScore = base?.score ?: 0f
                val quoteScore = quote?.score ?: 0f
                val pairScore = (baseScore - quoteScore).coerceIn(-1f, 1f)
                val count = (base?.headlineCount ?: 0) + (quote?.headlineCount ?: 0)
                assetSentiments[pair] = Sentiment(
                    score = pairScore,
                    label = labelFor(pairScore),
                    headlineCount = count
                )
            }
        }

        return assetSentiments
    }

    private fun labelFor(score: Float): String = when {
        score >= 0.15f -> "Bullish"
        score <= -0.15f -> "Bearish"
        else -> "Neutral"
    }
}

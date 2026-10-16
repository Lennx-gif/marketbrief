package app.marketbrief.core.analysis

import app.marketbrief.core.analysis.sentiment.AssetTagger
import app.marketbrief.core.analysis.sentiment.SentimentAggregator
import app.marketbrief.core.analysis.sentiment.SentimentScorer
import app.marketbrief.core.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SentimentTest {

    private val scorer = SentimentScorer()
    private val tagger = AssetTagger()
    private val aggregator = SentimentAggregator(scorer, tagger)

    @Test
    fun testBullishHeadlineScoring() {
        val score = scorer.score("Gold surges to record high as central banks boost purchases")
        assertTrue("Expected score > 0 but was $score", score > 0.3f)
    }

    @Test
    fun testBearishHeadlineScoring() {
        val score = scorer.score("Stock markets slump into correction amid recession fear and tariff warnings")
        assertTrue("Expected score < 0 but was $score", score < -0.3f)
    }

    @Test
    fun testNegationHandling() {
        val scoreNormal = scorer.score("Company reports growth")
        val scoreNegated = scorer.score("Company reports no growth")

        assertTrue("Normal score should be positive", scoreNormal > 0f)
        assertTrue("Negated score should be negative", scoreNegated < 0f)
    }

    @Test
    fun testAssetTagging() {
        val tags = tagger.tag("Federal Reserve signals dollar stability while ECB considers euro rate cut")
        assertTrue("Expected USD in tags", "USD" in tags)
        assertTrue("Expected EUR in tags", "EUR" in tags)
    }

    @Test
    fun testSentimentAggregationPairScore() {
        val articles = listOf(
            Article(
                id = "1",
                title = "Dollar surges amid strong job gains and rising yields",
                summary = "U.S. economy shows resilience as dollar climbs against peers",
                url = "https://example.com/1",
                sourceId = "reuters",
                sourceName = "Reuters",
                publishedAt = System.currentTimeMillis()
            ),
            Article(
                id = "2",
                title = "Eurozone economy slumps as inflation and recession fears mount",
                summary = "Euro drops following weak factory data",
                url = "https://example.com/2",
                sourceId = "bloomberg",
                sourceName = "Bloomberg",
                publishedAt = System.currentTimeMillis()
            )
        )

        val sentiments = aggregator.aggregate(articles)

        val usd = sentiments["USD"]
        val eur = sentiments["EUR"]
        val eurusd = sentiments["EURUSD"]

        assertTrue("USD should be bullish", (usd?.score ?: 0f) > 0f)
        assertTrue("EUR should be bearish", (eur?.score ?: 0f) < 0f)
        assertTrue("EURUSD pair score should be bearish", (eurusd?.score ?: 0f) < 0f)
        assertEquals("Bearish", eurusd?.label)
    }
}

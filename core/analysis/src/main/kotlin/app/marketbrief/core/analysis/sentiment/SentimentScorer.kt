package app.marketbrief.core.analysis.sentiment

/**
 * On-device NLP sentiment scorer using a financial lexicon with negation support.
 * Returns normalized score between -1.0 (strongly bearish) and +1.0 (strongly bullish).
 */
class SentimentScorer(
    private val lexicon: Map<String, Float> = DEFAULT_LEXICON
) {
    fun score(text: String): Float {
        val tokens = text.lowercase().split(Regex("[^a-z']+")).filter { it.isNotBlank() }
        var sum = 0f
        var hits = 0

        tokens.forEachIndexed { i, token ->
            val weight = lexicon[token] ?: return@forEachIndexed
            val negated = i > 0 && tokens[i - 1] in NEGATORS
            sum += if (negated) -weight else weight
            hits++
        }

        return if (hits == 0) 0f else (sum / hits).coerceIn(-1f, 1f)
    }

    companion object {
        val NEGATORS = setOf("not", "no", "never", "without", "fails", "fail", "fell")

        val DEFAULT_LEXICON: Map<String, Float> = mapOf(
            // Bullish / Positive financial terms
            "surge" to 0.8f, "surges" to 0.8f, "surged" to 0.8f, "surging" to 0.8f,
            "gain" to 0.6f, "gains" to 0.6f, "gained" to 0.6f, "gaining" to 0.6f,
            "rally" to 0.8f, "rallies" to 0.8f, "rallied" to 0.8f, "rallying" to 0.8f,
            "bullish" to 0.9f, "boom" to 0.8f, "booming" to 0.8f, "record" to 0.6f,
            "grow" to 0.5f, "growth" to 0.6f, "growing" to 0.5f,
            "soar" to 0.8f, "soars" to 0.8f, "soared" to 0.8f, "soaring" to 0.8f,
            "rebound" to 0.6f, "rebounds" to 0.6f, "rebounded" to 0.6f,
            "profit" to 0.6f, "profits" to 0.6f, "profitable" to 0.7f,
            "high" to 0.4f, "higher" to 0.5f, "highest" to 0.6f,
            "climb" to 0.5f, "climbs" to 0.5f, "climbed" to 0.5f,
            "jump" to 0.6f, "jumps" to 0.6f, "jumped" to 0.6f,
            "optimism" to 0.7f, "optimistic" to 0.7f, "positive" to 0.6f,
            "recovery" to 0.6f, "recovering" to 0.6f,
            "beat" to 0.6f, "beats" to 0.6f, "beating" to 0.6f,
            "upgrade" to 0.7f, "upgrades" to 0.7f, "upgraded" to 0.7f,
            "stimulus" to 0.5f, "easing" to 0.4f, "inflow" to 0.6f, "inflows" to 0.6f,

            // Bearish / Negative financial terms
            "drop" to -0.6f, "drops" to -0.6f, "dropped" to -0.6f, "dropping" to -0.6f,
            "plunge" to -0.8f, "plunges" to -0.8f, "plunged" to -0.8f, "plunging" to -0.8f,
            "slump" to -0.7f, "slumps" to -0.7f, "slumped" to -0.7f, "slumping" to -0.7f,
            "fall" to -0.5f, "falls" to -0.5f, "falling" to -0.5f,
            "crash" to -0.9f, "crashes" to -0.9f, "crashed" to -0.9f, "crashing" to -0.9f,
            "bearish" to -0.9f, "loss" to -0.6f, "losses" to -0.6f,
            "recession" to -0.8f, "crisis" to -0.8f, "turmoil" to -0.7f,
            "low" to -0.4f, "lower" to -0.5f, "lowest" to -0.6f,
            "decline" to -0.5f, "declines" to -0.5f, "declined" to -0.5f, "declining" to -0.5f,
            "selloff" to -0.8f, "sell-off" to -0.8f, "warning" to -0.6f, "warns" to -0.6f,
            "fear" to -0.7f, "fears" to -0.7f, "pessimism" to -0.7f,
            "miss" to -0.6f, "misses" to -0.6f, "missed" to -0.6f,
            "downgrade" to -0.7f, "downgrades" to -0.7f, "downgraded" to -0.7f,
            "tariff" to -0.4f, "tariffs" to -0.4f, "debt" to -0.4f, "default" to -0.9f,
            "outflow" to -0.6f, "outflows" to -0.6f, "inflation" to -0.4f
        )
    }
}

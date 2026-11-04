package app.marketbrief.core.analysis

enum class DivergenceLabel {
    CROWD_LONG_BULLISH_NEWS, CROWD_SHORT_BULLISH_NEWS,
    CROWD_LONG_BEARISH_NEWS, CROWD_SHORT_BEARISH_NEWS
}

data class DivergenceInput(val sentimentScore: Double, val headlineCount: Int, val crowdLongPercent: Double)

object DivergenceDetector {
    fun detect(input: DivergenceInput): DivergenceLabel? {
        if (input.headlineCount < MIN_HEADLINES) return null
        val bullish = input.sentimentScore >= SENTIMENT_BULLISH
        val bearish = input.sentimentScore <= SENTIMENT_BEARISH
        val crowdLong = input.crowdLongPercent >= CROWD_LONG
        val crowdShort = input.crowdLongPercent <= CROWD_SHORT
        return when {
            bullish && crowdLong -> DivergenceLabel.CROWD_LONG_BULLISH_NEWS
            bullish && crowdShort -> DivergenceLabel.CROWD_SHORT_BULLISH_NEWS
            bearish && crowdLong -> DivergenceLabel.CROWD_LONG_BEARISH_NEWS
            bearish && crowdShort -> DivergenceLabel.CROWD_SHORT_BEARISH_NEWS
            else -> null
        }
    }
    const val MIN_HEADLINES = 5
    const val SENTIMENT_BULLISH = 0.15
    const val SENTIMENT_BEARISH = -0.15
    const val CROWD_LONG = 65.0
    const val CROWD_SHORT = 35.0
}

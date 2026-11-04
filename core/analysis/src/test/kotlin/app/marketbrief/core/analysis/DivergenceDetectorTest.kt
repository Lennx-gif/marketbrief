package app.marketbrief.core.analysis

import org.junit.Assert.*
import org.junit.Test

class DivergenceDetectorTest {
    @Test fun headlineGateSuppressesSmallSamples() {
        assertNull(DivergenceDetector.detect(DivergenceInput(.7, 3, 80.0)))
    }

    @Test fun reportsFourSentimentAndCrowdCombinations() {
        assertEquals(DivergenceLabel.CROWD_LONG_BULLISH_NEWS, DivergenceDetector.detect(DivergenceInput(.2, 5, 65.0)))
        assertEquals(DivergenceLabel.CROWD_SHORT_BULLISH_NEWS, DivergenceDetector.detect(DivergenceInput(.2, 5, 35.0)))
        assertEquals(DivergenceLabel.CROWD_LONG_BEARISH_NEWS, DivergenceDetector.detect(DivergenceInput(-.2, 5, 65.0)))
        assertEquals(DivergenceLabel.CROWD_SHORT_BEARISH_NEWS, DivergenceDetector.detect(DivergenceInput(-.2, 5, 35.0)))
    }
}

package app.marketbrief.core.data

import org.junit.Assert.*
import org.junit.Test

class MarketAwarenessTest {
    @Test fun alertRequiresCrossingAndFiresOnlyOnce() {
        val alert = AlertSnapshot("a", 10.0, AlertDirection.ABOVE, 9.0)
        val hit = AlertEvaluator.evaluate(alert, 10.0)
        assertTrue(hit.crossed)
        assertFalse(AlertEvaluator.evaluate(hit.alert, 11.0).crossed)
        assertFalse(AlertEvaluator.evaluate(alert.copy(lastPrice = 11.0), 12.0).crossed)
    }

    @Test fun ftsQuerySanitizerDropsOperatorsAndQuotes() {
        assertEquals("market* AND news*", FtsQuerySanitizer.prefixQuery("market OR \"news\""))
        assertNull(FtsQuerySanitizer.prefixQuery("\" * OR "))
    }
}

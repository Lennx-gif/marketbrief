package app.marketbrief.feature.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugInfoBuilderTest {
    @Test
    fun `version formatter includes version code and flavor`() {
        assertTrue(DebugInfoBuilder.formatVersion("2.4.0", 27, "Personal") == "2.4.0 (27) · Personal")
    }

    @Test
    fun `debug info contains operational metadata but no credentials or article content`() {
        val text = DebugInfoBuilder.build(
            DebugInfoInput(
                versionName = "1.0.0",
                versionCode = 1,
                flavor = "Personal",
                newsRequestsRemaining = 90,
                twelveDataCreditsRemaining = 700,
                myfxbookRequestsRemaining = 100,
                lastMarketRefreshEpochMillis = 1_700_000_000_000L,
                cachedArticleCount = 12,
                twelveDataKeyConfigured = true,
                myfxbookConnected = false
            )
        )

        assertTrue(text.contains("Twelve Data credits remaining: 700"))
        assertTrue(text.contains("Articles saved on this device: 12"))
        assertFalse(text.contains("password"))
        assertFalse(text.contains("api_key"))
        assertFalse(text.contains("article body"))
    }
}

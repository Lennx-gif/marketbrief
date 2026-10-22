package app.marketbrief.core.data.remote

import app.marketbrief.core.data.positioning.PositioningError
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyfxbookClientTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    @Test
    fun `outlook maps symbols and percentages`() {
        val dto = json.decodeFromString<MyfxbookOutlookDto>(
            """{"error":false,"message":"","symbols":[
               {"name":"EURUSD","shortPercentage":38,"longPercentage":62,"shortVolume":1200.5,"longVolume":2100.25,"avgShortPrice":1.09}]}"""
        )
        val list = MyfxbookClient.mapOutlook(dto)
        assertEquals(1, list.size)
        assertEquals("EURUSD", list[0].symbol)
        assertEquals(62.0, list[0].longPercent, 0.0)
        assertEquals(38.0, list[0].shortPercent, 0.0)
    }

    @Test
    fun `error messages classify`() {
        assertTrue(MyfxbookClient.classify("Invalid session") is PositioningError.SessionExpired)
        assertTrue(MyfxbookClient.classify("Request limit exceeded") is PositioningError.RateLimited)
        assertTrue(MyfxbookClient.classify("Something odd") is PositioningError.Server)
    }
}

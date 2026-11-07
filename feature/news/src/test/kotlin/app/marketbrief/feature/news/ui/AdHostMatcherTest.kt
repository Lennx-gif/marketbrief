package app.marketbrief.feature.news.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdHostMatcherTest {
    private val matcher = AdHostMatcher.fromLines(
        sequenceOf("doubleclick.net", "taboola.com", "# ignored")
    )

    @Test
    fun `matches exact ad host and its subdomains`() {
        assertTrue(matcher.matches("doubleclick.net"))
        assertTrue(matcher.matches("secure.doubleclick.net"))
    }

    @Test
    fun `does not match lookalike hosts`() {
        assertFalse(matcher.matches("notdoubleclick.net"))
        assertFalse(matcher.matches("doubleclick.net.attacker.example"))
    }

    @Test
    fun `same registrable domain allows publisher subdomains but not lookalikes`() {
        assertTrue(AdHostMatcher.sameRegistrableDomain("https://news.publisher.co.uk/a", "https://www.publisher.co.uk/b"))
        assertFalse(AdHostMatcher.sameRegistrableDomain("https://publisher.example", "https://publisher.example.attacker.test"))
    }
}

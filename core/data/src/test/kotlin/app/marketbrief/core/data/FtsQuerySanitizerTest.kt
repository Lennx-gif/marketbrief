package app.marketbrief.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQuerySanitizerTest {

    @Test
    fun plainWordsBecomePrefixTerms() {
        assertEquals("gold*", FtsQuerySanitizer.prefixQuery("gold"))
    }

    @Test
    fun hyphensAreStrippedSoMatchStaysWellFormed() {
        // "-kes" would parse as the FTS NOT operator and throw in MATCH.
        assertEquals("usd* AND kes*", FtsQuerySanitizer.prefixQuery("usd -kes"))
        assertEquals("usdkes*", FtsQuerySanitizer.prefixQuery("usd-kes"))
    }

    @Test
    fun ftsOperatorsAreDropped() {
        assertEquals("gold*", FtsQuerySanitizer.prefixQuery("gold OR"))
        assertNull(FtsQuerySanitizer.prefixQuery("NOT"))
    }

    @Test
    fun punctuationOnlyInputYieldsNull() {
        assertNull(FtsQuerySanitizer.prefixQuery(" * ( ) "))
        assertNull(FtsQuerySanitizer.prefixQuery("   "))
    }
}

package app.marketbrief.core.entitlements

import org.junit.Test
import org.junit.Assert.*

class EntitlementsTest {

    @Test
    fun testFreePlanFeatures() {
        val config = Plans.default[Plan.FREE]!!
        
        assertTrue(config.hasFeature(Feature.NEWS_FEED))
        assertTrue(config.hasFeature(Feature.NEWS_SAVED))
        assertTrue(config.hasFeature(Feature.NEWS_SEARCH))
        assertTrue(config.hasFeature(Feature.CALCULATOR_BASIC))
        assertTrue(config.hasFeature(Feature.ANALYSIS_BASIC))
        
        assertFalse(config.hasFeature(Feature.CALCULATOR_PRESETS))
        assertFalse(config.hasFeature(Feature.CALCULATOR_LOG))
        assertFalse(config.hasFeature(Feature.CALCULATOR_ADVANCED))
        assertFalse(config.hasFeature(Feature.ANALYSIS_ADVANCED_INDICATORS))
        assertFalse(config.hasFeature(Feature.ANALYSIS_SCREENER))
        assertFalse(config.hasFeature(Feature.WATCHLIST))
        assertFalse(config.hasFeature(Feature.ECONOMIC_CALENDAR))
        assertFalse(config.hasFeature(Feature.ALERTS))
        assertFalse(config.hasFeature(Feature.EXPORT_IMPORT))
    }

    @Test
    fun testProPlanFeatures() {
        val config = Plans.default[Plan.PRO]!!
        
        Feature.values().forEach { feature ->
            assertTrue("PRO should have $feature", config.hasFeature(feature))
        }
    }

    @Test
    fun testPersonalPlanFeatures() {
        val config = Plans.default[Plan.PERSONAL]!!
        
        Feature.values().forEach { feature ->
            assertTrue("PERSONAL should have $feature", config.hasFeature(feature))
        }
    }

    @Test
    fun testFreePlanLimits() {
        val config = Plans.default[Plan.FREE]!!
        
        assertEquals(5, config.getLimit(Limit.WATCHLIST_ITEMS))
        assertEquals(50, config.getLimit(Limit.SAVED_ARTICLES))
        assertEquals(3, config.getLimit(Limit.CALCULATOR_PRESETS))
        assertEquals(100, config.getLimit(Limit.CALCULATOR_LOG_ENTRIES))
        assertEquals(0, config.getLimit(Limit.ALERTS_ACTIVE))
    }

    @Test
    fun testProPlanLimits() {
        val config = Plans.default[Plan.PRO]!!
        
        Limit.values().forEach { limit ->
            assertEquals(Int.MAX_VALUE, config.getLimit(limit))
        }
    }

    @Test
    fun testEntitlementCheckerGateOpen() {
        val checker = EntitlementChecker(Plan.PERSONAL)
        
        val gate = checker.gate(Feature.CALCULATOR_ADVANCED)
        assertTrue(gate is Gate.Open)
    }

    @Test
    fun testEntitlementCheckerGateLocked() {
        val checker = EntitlementChecker(Plan.FREE)
        
        val gate = checker.gate(Feature.CALCULATOR_ADVANCED)
        assertTrue(gate is Gate.Locked)
        val locked = gate as Gate.Locked
        assertEquals(Feature.CALCULATOR_ADVANCED, locked.feature)
        assertEquals(Plan.FREE, locked.plan)
        assertTrue(locked.upgradeMessage.isNotEmpty())
    }

    @Test
    fun testEntitlementCheckerCanAddWithinLimit() {
        val checker = EntitlementChecker(Plan.FREE)
        
        val gate = checker.canAdd(Limit.WATCHLIST_ITEMS, 3)
        assertTrue(gate is Gate.Open)
    }

    @Test
    fun testEntitlementCheckerCanAddExceedsLimit() {
        val checker = EntitlementChecker(Plan.FREE)
        
        val gate = checker.canAdd(Limit.WATCHLIST_ITEMS, 5)
        assertTrue(gate is Gate.LimitExceeded)
        val exceeded = gate as Gate.LimitExceeded
        assertEquals(Limit.WATCHLIST_ITEMS, exceeded.limit)
        assertEquals(5, exceeded.current)
        assertEquals(5, exceeded.max)
    }
}
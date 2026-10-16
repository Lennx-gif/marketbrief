package app.marketbrief.core.entitlements

/**
 * Features that can be gated by plan.
 */
enum class Feature {
    NEWS_FEED,
    NEWS_SAVED,
    NEWS_SEARCH,
    CALCULATOR_BASIC,
    CALCULATOR_PRESETS,
    CALCULATOR_LOG,
    CALCULATOR_ADVANCED,
    ANALYSIS_BASIC,
    ANALYSIS_ADVANCED_INDICATORS,
    ANALYSIS_SCREENER,
    WATCHLIST,
    ECONOMIC_CALENDAR,
    ALERTS,
    EXPORT_IMPORT
}

/**
 * Limits that can be enforced by plan.
 */
enum class Limit {
    WATCHLIST_ITEMS,
    SAVED_ARTICLES,
    CALCULATOR_PRESETS,
    CALCULATOR_LOG_ENTRIES,
    ALERTS_ACTIVE
}

/**
 * Subscription plan.
 */
enum class Plan {
    FREE,
    PRO,
    PERSONAL
}

/**
 * Feature and limit configuration for a plan.
 */
data class PlanConfig(
    val enabledFeatures: Set<Feature>,
    val limits: Map<Limit, Int>
) {
    fun hasFeature(feature: Feature): Boolean = feature in enabledFeatures
    fun getLimit(limit: Limit): Int = limits[limit] ?: Int.MAX_VALUE
}

/**
 * Default plan configurations.
 */
object Plans {
    val default: Map<Plan, PlanConfig> = mapOf(
        Plan.FREE to PlanConfig(
            enabledFeatures = setOf(
                Feature.NEWS_FEED,
                Feature.NEWS_SAVED,
                Feature.NEWS_SEARCH,
                Feature.CALCULATOR_BASIC,
                Feature.ANALYSIS_BASIC,
            ),
            limits = mapOf(
                Limit.WATCHLIST_ITEMS to 5,
                Limit.SAVED_ARTICLES to 50,
                Limit.CALCULATOR_PRESETS to 3,
                Limit.CALCULATOR_LOG_ENTRIES to 100,
                Limit.ALERTS_ACTIVE to 0,
            )
        ),
        Plan.PRO to PlanConfig(
            enabledFeatures = Feature.values().toSet(),
            limits = mapOf(
                Limit.WATCHLIST_ITEMS to Int.MAX_VALUE,
                Limit.SAVED_ARTICLES to Int.MAX_VALUE,
                Limit.CALCULATOR_PRESETS to Int.MAX_VALUE,
                Limit.CALCULATOR_LOG_ENTRIES to Int.MAX_VALUE,
                Limit.ALERTS_ACTIVE to Int.MAX_VALUE,
            )
        ),
        Plan.PERSONAL to PlanConfig(
            enabledFeatures = Feature.values().toSet(),
            limits = mapOf(
                Limit.WATCHLIST_ITEMS to Int.MAX_VALUE,
                Limit.SAVED_ARTICLES to Int.MAX_VALUE,
                Limit.CALCULATOR_PRESETS to Int.MAX_VALUE,
                Limit.CALCULATOR_LOG_ENTRIES to Int.MAX_VALUE,
                Limit.ALERTS_ACTIVE to Int.MAX_VALUE,
            )
        )
    )
}

/**
 * Result of an entitlement check.
 */
sealed interface Gate {
    data class Open(val feature: Feature) : Gate
    data class Locked(val feature: Feature, val plan: Plan, val upgradeMessage: String) : Gate
    data class LimitExceeded(val limit: Limit, val current: Int, val max: Int) : Gate
}

/**
 * Checks entitlements for the current plan.
 */
class EntitlementChecker(private val plan: Plan) {

    private val config = Plans.default[plan]!!

    fun gate(feature: Feature): Gate {
        return if (config.hasFeature(feature)) {
            Gate.Open(feature)
        } else {
            val upgradeMessage = when (plan) {
                Plan.FREE -> "Upgrade to PRO to unlock $feature"
                Plan.PRO -> "Contact support for $feature access"
                Plan.PERSONAL -> "This feature is available in PERSONAL plan"
            }
            Gate.Locked(feature, plan, upgradeMessage)
        }
    }

    fun canAdd(limit: Limit, currentCount: Int): Gate {
        val max = config.getLimit(limit)
        return if (currentCount < max) {
            Gate.Open(Feature.values().firstOrNull { it.name.contains(limit.name) } ?: Feature.NEWS_FEED)
        } else {
            Gate.LimitExceeded(limit, currentCount, max)
        }
    }

    fun getLimit(limit: Limit): Int = config.getLimit(limit)
}

/**
 * Source of the current plan. Implementations can read from DataStore, backend, etc.
 */
interface PlanSource {
    val plan: Plan
}
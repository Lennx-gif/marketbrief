package app.marketbrief.core.data.remote

import app.marketbrief.core.model.Category

/**
 * Optimized query builders for NewsAPI /v2/everything.
 */
object NewsQueries {
    private const val TRADE =
        "(trade OR exports OR imports OR tariff OR tariffs OR \"trade balance\" OR currency OR forex)"

    fun homeTrade(country: String): String = "\"$country\" AND $TRADE"

    fun forCategory(c: Category): String = when (c) {
        Category.Finance -> "(markets OR inflation OR \"interest rates\" OR \"central bank\")"
        Category.Forex -> "(forex OR currency OR dollar OR euro OR yen OR sterling)"
        Category.Tech -> "(technology OR AI OR semiconductor OR software)"
        Category.Politics -> "(election OR parliament OR government OR policy)"
        Category.Stocks -> "(stocks OR equities OR earnings OR \"stock market\")"
        Category.Crypto -> "(bitcoin OR ethereum OR crypto)"
        Category.Commodities -> "(gold OR oil OR commodities)"
        Category.Trade, Category.All, Category.Saved -> TRADE
    }
}

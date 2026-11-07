package app.marketbrief.core.storage.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Version 2 contained only news data. Version 3 added calculator presets and its trade log. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS presets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, instrumentSymbol TEXT NOT NULL, accountCurrency TEXT NOT NULL, balanceText TEXT NOT NULL, riskIsPercent INTEGER NOT NULL, riskValueText TEXT NOT NULL, leverageText TEXT NOT NULL, createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS trade_log (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, symbol TEXT NOT NULL, direction TEXT NOT NULL, entryText TEXT NOT NULL, stopText TEXT NOT NULL, stopIsPips INTEGER NOT NULL, takeProfitText TEXT NOT NULL, lots TEXT NOT NULL, riskAmount TEXT NOT NULL, accountCurrency TEXT NOT NULL, createdAt INTEGER NOT NULL)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS journal_entries (id TEXT NOT NULL PRIMARY KEY, createdAt INTEGER NOT NULL, symbol TEXT NOT NULL, assetClass TEXT NOT NULL, direction TEXT NOT NULL, entry TEXT NOT NULL, stop TEXT NOT NULL, stopIsPips INTEGER NOT NULL, takeProfit TEXT, lots TEXT NOT NULL, riskAmount TEXT NOT NULL, riskPercent TEXT NOT NULL, accountCurrency TEXT NOT NULL, status TEXT NOT NULL, openedAt INTEGER, closedAt INTEGER, exitPrice TEXT, pnl TEXT, rMultiple TEXT, notes TEXT NOT NULL, tags TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS journal_headlines (entryId TEXT NOT NULL, articleId TEXT NOT NULL, title TEXT NOT NULL, url TEXT NOT NULL, PRIMARY KEY(entryId, articleId))")
        db.execSQL("CREATE TABLE IF NOT EXISTS account_settings (id INTEGER NOT NULL PRIMARY KEY, startingBalance TEXT NOT NULL, currency TEXT NOT NULL, maxRiskPerTradePercent TEXT NOT NULL, dailyLossLimitPercent TEXT NOT NULL, maxOpenRiskPercent TEXT NOT NULL, maxDrawdownPercent TEXT NOT NULL, drawdownMode TEXT NOT NULL, resetTimezone TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS calendar_events (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, currency TEXT NOT NULL, instant INTEGER NOT NULL, impact TEXT NOT NULL, source TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS price_alerts (id TEXT NOT NULL PRIMARY KEY, symbol TEXT NOT NULL, assetClass TEXT NOT NULL, targetPrice TEXT NOT NULL, direction TEXT NOT NULL, createdAt INTEGER NOT NULL, triggeredAt INTEGER, lastCheckedPrice TEXT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_price_alerts_symbol ON price_alerts(symbol)")
        db.execSQL("CREATE TABLE IF NOT EXISTS article_tags (articleId TEXT NOT NULL, tag TEXT NOT NULL, PRIMARY KEY(articleId, tag))")
        db.execSQL("CREATE TABLE IF NOT EXISTS source_preferences (sourceName TEXT NOT NULL PRIMARY KEY, weight TEXT NOT NULL)")
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS article_fts USING FTS4(articleId, title, summary)")
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS article_body_fts USING FTS4(articleId, markdown)")
        db.execSQL("INSERT INTO article_fts(articleId, title, summary) SELECT id, title, summary FROM articles")
        db.execSQL("INSERT INTO article_body_fts(articleId, markdown) SELECT articleId, markdown FROM article_body")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS rate_cache (currencyPair TEXT NOT NULL PRIMARY KEY, rate TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS watchlist_items (" +
                    "symbol TEXT NOT NULL PRIMARY KEY, " +
                    "assetClass TEXT NOT NULL, " +
                    "position INTEGER NOT NULL, " +
                    "addedAt INTEGER NOT NULL)"
            )
        }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS journal_accounts (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "type TEXT NOT NULL, " +
                        "currency TEXT NOT NULL, " +
                        "startingBalance TEXT NOT NULL, " +
                        "maxRiskPerTradePercent TEXT NOT NULL, " +
                        "dailyLossLimitPercent TEXT NOT NULL, " +
                        "maxOpenRiskPercent TEXT NOT NULL, " +
                        "maxDrawdownPercent TEXT NOT NULL, " +
                        "drawdownMode TEXT NOT NULL, " +
                        "resetTimezone TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL, " +
                        "archived INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO journal_accounts " +
                        "(id, name, type, currency, startingBalance, maxRiskPerTradePercent, dailyLossLimitPercent, " +
                        "maxOpenRiskPercent, maxDrawdownPercent, drawdownMode, resetTimezone, createdAt, archived) " +
                        "SELECT 1, 'Account 1', 'LIVE', currency, startingBalance, maxRiskPerTradePercent, " +
                        "dailyLossLimitPercent, maxOpenRiskPercent, maxDrawdownPercent, drawdownMode, resetTimezone, " +
                        "CAST(strftime('%s','now') AS INTEGER) * 1000, 0 FROM account_settings WHERE id = 1"
                )
                db.execSQL(
                    "INSERT INTO journal_accounts " +
                        "(id, name, type, currency, startingBalance, maxRiskPerTradePercent, dailyLossLimitPercent, " +
                        "maxOpenRiskPercent, maxDrawdownPercent, drawdownMode, resetTimezone, createdAt, archived) " +
                        "SELECT 1, 'Account 1', 'LIVE', 'USD', '10000', '1', '3', '5', '10', 'STATIC', " +
                        "'Africa/Nairobi', CAST(strftime('%s','now') AS INTEGER) * 1000, 0 " +
                        "WHERE NOT EXISTS (SELECT 1 FROM journal_accounts WHERE id = 1)"
                )

                db.execSQL("ALTER TABLE journal_entries RENAME TO journal_entries_v6")
                db.execSQL(
                    "CREATE TABLE journal_entries (" +
                        "id TEXT NOT NULL PRIMARY KEY, createdAt INTEGER NOT NULL, symbol TEXT NOT NULL, " +
                        "assetClass TEXT NOT NULL, direction TEXT NOT NULL, entry TEXT NOT NULL, stop TEXT NOT NULL, " +
                        "stopIsPips INTEGER NOT NULL, takeProfit TEXT, lots TEXT NOT NULL, riskAmount TEXT NOT NULL, " +
                        "riskPercent TEXT NOT NULL, accountCurrency TEXT NOT NULL, status TEXT NOT NULL, openedAt INTEGER, " +
                        "closedAt INTEGER, exitPrice TEXT, pnl TEXT, rMultiple TEXT, notes TEXT NOT NULL, tags TEXT NOT NULL, " +
                        "accountId INTEGER NOT NULL, setupId TEXT, checklistResult TEXT NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO journal_entries " +
                        "(id, createdAt, symbol, assetClass, direction, entry, stop, stopIsPips, takeProfit, lots, " +
                        "riskAmount, riskPercent, accountCurrency, status, openedAt, closedAt, exitPrice, pnl, rMultiple, " +
                        "notes, tags, accountId, setupId, checklistResult) " +
                        "SELECT id, createdAt, symbol, assetClass, direction, entry, stop, stopIsPips, takeProfit, lots, " +
                        "riskAmount, riskPercent, accountCurrency, status, openedAt, closedAt, exitPrice, pnl, rMultiple, " +
                        "notes, tags, 1, NULL, '' FROM journal_entries_v6"
                )
                db.execSQL("DROP TABLE journal_entries_v6")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_journal_entries_accountId ON journal_entries(accountId)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS trade_setups (" +
                        "id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL, tags TEXT NOT NULL, " +
                        "checklist TEXT NOT NULL, color TEXT NOT NULL, archived INTEGER NOT NULL, createdAt INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS daily_entries (" +
                        "id TEXT NOT NULL PRIMARY KEY, accountId INTEGER, date TEXT NOT NULL, planFollowed INTEGER, " +
                        "mood TEXT NOT NULL, energy INTEGER, notes TEXT NOT NULL, lessons TEXT NOT NULL, tags TEXT NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_daily_entries_date ON daily_entries(date)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS journal_attachments (" +
                        "id TEXT NOT NULL PRIMARY KEY, ownerType TEXT NOT NULL, ownerId TEXT NOT NULL, " +
                        "filePath TEXT NOT NULL, createdAt INTEGER NOT NULL)"
                )
            }
        }

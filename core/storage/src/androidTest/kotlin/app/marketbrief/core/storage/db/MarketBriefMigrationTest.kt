package app.marketbrief.core.storage.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarketBriefMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MarketBriefDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrateFromVersion5PreservesTradesAndCreatesPersistentTables() {
        val databaseName = "marketbrief-migration-5-7"
        helper.createDatabase(databaseName, 5).apply {
            execSQL(
                "INSERT INTO account_settings " +
                    "(id, startingBalance, currency, maxRiskPerTradePercent, dailyLossLimitPercent, " +
                    "maxOpenRiskPercent, maxDrawdownPercent, drawdownMode, resetTimezone) " +
                    "VALUES (1, '25000', 'KES', '2', '4', '8', '15', 'TRAILING', 'Africa/Nairobi')"
            )
            execSQL(
                "INSERT INTO journal_entries " +
                    "(id, createdAt, symbol, assetClass, direction, entry, stop, stopIsPips, takeProfit, lots, " +
                    "riskAmount, riskPercent, accountCurrency, status, openedAt, closedAt, exitPrice, pnl, rMultiple, notes, tags) " +
                    "VALUES ('trade-1', 123, 'EURUSD', 'FOREX', 'LONG', '1.1', '1.0', 0, NULL, '0.1', " +
                    "'100', '1', 'KES', 'CLOSED', 124, 125, '1.2', '50', '0.5', 'preserve notes', 'one,two')"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            7,
            true,
            MIGRATION_5_6,
            MIGRATION_6_7
        )
        migrated.query("SELECT name, currency, startingBalance FROM journal_accounts WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Account 1", cursor.getString(0))
            assertEquals("KES", cursor.getString(1))
            assertEquals("25000", cursor.getString(2))
        }
        migrated.query("SELECT id, accountId, notes, setupId FROM journal_entries WHERE id = 'trade-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("trade-1", cursor.getString(0))
            assertEquals(1L, cursor.getLong(1))
            assertEquals("preserve notes", cursor.getString(2))
            assertTrue(cursor.isNull(3))
        }
        migrated.query("SELECT COUNT(*) FROM watchlist_items").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }
}

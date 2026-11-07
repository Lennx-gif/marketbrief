package app.marketbrief.core.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ArticleEntity::class,
        BookmarkEntity::class,
        DownloadEntity::class,
        ArticleBodyEntity::class,
        PresetEntity::class,
        TradeLogEntity::class,
        JournalEntryEntity::class,
        JournalHeadlineEntity::class,
        AccountSettingsEntity::class,
        CalendarEventEntity::class,
        PriceAlertEntity::class,
        ArticleTagEntity::class,
        SourcePrefEntity::class,
        ArticleFtsEntity::class,
        ArticleBodyFtsEntity::class,
        RateCacheEntity::class,
        WatchlistItemEntity::class,
        JournalAccountEntity::class,
        TradeSetupEntity::class,
        DailyEntryEntity::class,
        JournalAttachmentEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class MarketBriefDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun downloadDao(): DownloadDao
    abstract fun articleBodyDao(): ArticleBodyDao
    abstract fun presetDao(): PresetDao
    abstract fun tradeLogDao(): TradeLogDao
    abstract fun journalDao(): JournalDao
    abstract fun calendarDao(): CalendarDao
    abstract fun priceAlertDao(): PriceAlertDao
    abstract fun articleSearchDao(): ArticleSearchDao
    abstract fun sourcePrefDao(): SourcePrefDao
    abstract fun articleTagDao(): ArticleTagDao
    abstract fun rateCacheDao(): RateCacheDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun journalAccountDao(): JournalAccountDao
    abstract fun tradeSetupDao(): TradeSetupDao
    abstract fun dailyEntryDao(): DailyEntryDao
    abstract fun journalAttachmentDao(): JournalAttachmentDao
}

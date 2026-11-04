package app.marketbrief.core.storage.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import app.marketbrief.core.data.NewsProvider
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.storage.NewsRepositoryImpl
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.core.storage.JournalSetupWriter
import app.marketbrief.core.storage.RoomJournalSetupWriter
import app.marketbrief.core.storage.RoomEconomicCalendarProvider
import app.marketbrief.core.data.EconomicCalendarProvider
import app.marketbrief.core.storage.db.ArticleDao
import app.marketbrief.core.storage.db.BookmarkDao
import app.marketbrief.core.storage.db.DownloadDao
import app.marketbrief.core.storage.db.MarketBriefDatabase
import app.marketbrief.core.storage.db.MIGRATION_2_3
import app.marketbrief.core.storage.db.MIGRATION_3_4
import app.marketbrief.core.storage.db.MIGRATION_4_5
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.storageDataStore: DataStore<Preferences> by preferencesDataStore(name = "marketbrief_user_prefs")

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides @Singleton
    fun provideJournalSetupWriter(dao: app.marketbrief.core.storage.db.JournalDao, articles: ArticleDao): JournalSetupWriter =
        RoomJournalSetupWriter(dao, articles)

    @Provides @Singleton
    fun provideEconomicCalendarProvider(dao: app.marketbrief.core.storage.db.CalendarDao): EconomicCalendarProvider = RoomEconomicCalendarProvider(dao)

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.storageDataStore

    @Provides
    @Singleton
    fun provideMarketBriefDatabase(@ApplicationContext context: Context): MarketBriefDatabase {
        return Room.databaseBuilder(
            context,
            MarketBriefDatabase::class.java,
            "marketbrief.db"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    @Provides
    fun provideArticleDao(database: MarketBriefDatabase): ArticleDao = database.articleDao()

    @Provides
    fun provideBookmarkDao(database: MarketBriefDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideDownloadDao(database: MarketBriefDatabase): DownloadDao = database.downloadDao()

    @Provides
    fun provideArticleBodyDao(database: MarketBriefDatabase): app.marketbrief.core.storage.db.ArticleBodyDao =
        database.articleBodyDao()

    @Provides
    fun providePresetDao(database: MarketBriefDatabase): app.marketbrief.core.storage.db.PresetDao =
        database.presetDao()

    @Provides
    fun provideTradeLogDao(database: MarketBriefDatabase): app.marketbrief.core.storage.db.TradeLogDao =
        database.tradeLogDao()

    @Provides fun provideJournalDao(database: MarketBriefDatabase): app.marketbrief.core.storage.db.JournalDao = database.journalDao()
    @Provides fun provideCalendarDao(database: MarketBriefDatabase): app.marketbrief.core.storage.db.CalendarDao = database.calendarDao()
    @Provides fun providePriceAlertDao(database: MarketBriefDatabase): app.marketbrief.core.storage.db.PriceAlertDao = database.priceAlertDao()
    @Provides fun provideArticleSearchDao(database: MarketBriefDatabase): app.marketbrief.core.storage.db.ArticleSearchDao = database.articleSearchDao()
    @Provides fun provideSourcePrefDao(database: MarketBriefDatabase): app.marketbrief.core.storage.db.SourcePrefDao = database.sourcePrefDao()
    @Provides fun provideRateCacheDao(database: MarketBriefDatabase): app.marketbrief.core.storage.db.RateCacheDao = database.rateCacheDao()
    @Provides fun provideArticleTagDao(database: MarketBriefDatabase): app.marketbrief.core.storage.db.ArticleTagDao = database.articleTagDao()

    @Provides
    @Singleton
    fun provideNewsRepository(
        articleDao: ArticleDao,
        bookmarkDao: BookmarkDao,
        downloadDao: DownloadDao,
        articleBodyDao: app.marketbrief.core.storage.db.ArticleBodyDao,
        provider: NewsProvider,
        prefs: UserPrefsRepository,
        searchDao: app.marketbrief.core.storage.db.ArticleSearchDao,
        sourcePrefDao: app.marketbrief.core.storage.db.SourcePrefDao,
        articleTagDao: app.marketbrief.core.storage.db.ArticleTagDao,
        refreshScheduler: app.marketbrief.core.data.NewsRefreshScheduler
    ): NewsRepository {
        return NewsRepositoryImpl(
            articleDao = articleDao,
            bookmarkDao = bookmarkDao,
            downloadDao = downloadDao,
            articleBodyDao = articleBodyDao,
            provider = provider,
            prefs = prefs,
            articleSearchDao = searchDao,
            sourcePrefDao = sourcePrefDao,
            articleTagDao = articleTagDao,
            refreshScheduler = refreshScheduler
        )
    }
}

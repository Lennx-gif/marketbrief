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
import app.marketbrief.core.storage.db.ArticleDao
import app.marketbrief.core.storage.db.BookmarkDao
import app.marketbrief.core.storage.db.DownloadDao
import app.marketbrief.core.storage.db.MarketBriefDatabase
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
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideArticleDao(database: MarketBriefDatabase): ArticleDao = database.articleDao()

    @Provides
    fun provideBookmarkDao(database: MarketBriefDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideDownloadDao(database: MarketBriefDatabase): DownloadDao = database.downloadDao()

    @Provides
    @Singleton
    fun provideNewsRepository(
        articleDao: ArticleDao,
        bookmarkDao: BookmarkDao,
        downloadDao: DownloadDao,
        provider: NewsProvider,
        prefs: UserPrefsRepository
    ): NewsRepository {
        return NewsRepositoryImpl(
            articleDao = articleDao,
            bookmarkDao = bookmarkDao,
            downloadDao = downloadDao,
            provider = provider,
            prefs = prefs
        )
    }
}

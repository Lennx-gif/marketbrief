package app.marketbrief.di

import app.marketbrief.BuildConfig
import android.content.Context
import app.marketbrief.core.data.MarketDataRepository
import app.marketbrief.core.data.NewsProvider
import app.marketbrief.core.data.positioning.CredentialStore
import app.marketbrief.core.data.positioning.MyfxbookPositioningRepository
import app.marketbrief.core.data.positioning.PositioningRepository
import app.marketbrief.core.data.remote.CachedMarketDataRepository
import app.marketbrief.core.data.remote.MyfxbookClient
import app.marketbrief.core.data.remote.TwelveDataSource
import app.marketbrief.core.storage.KeystoreCredentialStore
import app.marketbrief.core.data.remote.NewsApiProvider
import app.marketbrief.core.entitlements.PlanSource
import app.marketbrief.navigation.PlanSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNewsProvider(): NewsProvider {
        return NewsApiProvider(apiKey = BuildConfig.NEWSAPI_KEY)
    }

    @Provides
    @Singleton
    fun providePlanSource(): PlanSource = PlanSourceImpl()

    @Provides
    @Singleton
    fun provideMarketDataRepository(): MarketDataRepository =
        CachedMarketDataRepository(TwelveDataSource(apiKey = BuildConfig.TWELVE_DATA_API_KEY))

    @Provides
    @Singleton
    fun provideCredentialStore(@ApplicationContext context: Context): CredentialStore =
        KeystoreCredentialStore(context)

    @Provides
    @Singleton
    fun providePositioningRepository(store: CredentialStore): PositioningRepository =
        MyfxbookPositioningRepository(api = MyfxbookClient(), store = store)
}

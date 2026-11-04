package app.marketbrief.di

import app.marketbrief.BuildConfig
import android.content.Context
import app.marketbrief.core.data.MarketDataRepository
import app.marketbrief.core.data.NewsProvider
import app.marketbrief.core.data.positioning.CredentialStore
import app.marketbrief.core.data.positioning.MyfxbookPositioningRepository
import app.marketbrief.core.data.positioning.PositioningRepository
import app.marketbrief.core.data.positioning.CrowdPosition
import app.marketbrief.core.data.positioning.MyfxbookCredentials
import app.marketbrief.core.data.positioning.PositioningError
import app.marketbrief.core.data.remote.CachedMarketDataRepository
import app.marketbrief.core.data.remote.MyfxbookClient
import app.marketbrief.core.data.remote.TwelveDataSource
import app.marketbrief.core.data.remote.RequestBudget
import app.marketbrief.core.storage.KeystoreCredentialStore
import app.marketbrief.core.data.remote.NewsApiProvider
import app.marketbrief.core.entitlements.PlanSource
import app.marketbrief.navigation.PlanSourceImpl
import dagger.Module
import dagger.Provides
import app.marketbrief.core.data.NewsRefreshScheduler
import app.marketbrief.core.data.DailyDigestScheduler
import app.marketbrief.core.data.PriceAlertNotifier
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.core.storage.PersistentRateRepository
import app.marketbrief.core.storage.db.RateCacheDao
import app.marketbrief.work.WorkNewsRefreshScheduler
import app.marketbrief.work.WorkDailyDigestScheduler
import app.marketbrief.work.AndroidPriceAlertNotifier
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNewsRefreshScheduler(@ApplicationContext context: Context, prefs: UserPrefsRepository): NewsRefreshScheduler =
        WorkNewsRefreshScheduler(context, prefs)

    @Provides @Singleton
    fun provideDailyDigestScheduler(@ApplicationContext context: Context): DailyDigestScheduler = WorkDailyDigestScheduler(context)

    @Provides @Singleton
    fun providePriceAlertNotifier(@ApplicationContext context: Context): PriceAlertNotifier = AndroidPriceAlertNotifier(context)

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
    fun provideTwelveDataSource(prefs: UserPrefsRepository): TwelveDataSource =
            TwelveDataSource(apiKey = if (BuildConfig.PLAN == "PERSONAL") BuildConfig.TWELVE_DATA_API_KEY else "", budget = RequestBudget(
            perMinute = 8, perDay = 800,
            dailyAcquirer = { cost -> prefs.consumeProviderRequest("twelve_data", cost, 800) },
            dailyRemaining = { prefs.providerBudgetRemaining("twelve_data", 800).first() }
        ))

    @Provides
    @Singleton
    fun provideMarketDataRepository(source: TwelveDataSource): MarketDataRepository =
        CachedMarketDataRepository(source)

    @Provides
    @Singleton
    fun provideRateRepository(marketData: MarketDataRepository, cache: RateCacheDao): app.marketbrief.core.data.RateRepository =
        PersistentRateRepository(marketData, cache)

    @Provides
    @Singleton
    fun provideCredentialStore(@ApplicationContext context: Context): CredentialStore =
        KeystoreCredentialStore(context)

    @Provides
    @Singleton
    fun providePositioningRepository(store: CredentialStore, prefs: UserPrefsRepository): PositioningRepository = if (BuildConfig.PLAN == "PERSONAL") {
        MyfxbookPositioningRepository(api = MyfxbookClient(), store = store, budget = RequestBudget(
            perMinute = 10, perDay = 100,
            dailyAcquirer = { cost -> prefs.consumeProviderRequest("myfxbook", cost, 100) },
            dailyRemaining = { prefs.providerBudgetRemaining("myfxbook", 100).first() }
        ))
    } else StorePositioningRepository
}

private object StorePositioningRepository : PositioningRepository {
    override val connected: Flow<Boolean> = kotlinx.coroutines.flow.flowOf(false)
    override val positions: Flow<List<CrowdPosition>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override val lastUpdated: Flow<Long?> = kotlinx.coroutines.flow.flowOf(null)
    override suspend fun loadConnectionState() = Unit
    override suspend fun connect(email: String, password: String): Result<Unit> = Result.failure(PositioningError.NotConnected())
    override suspend fun disconnect() = Unit
    override suspend fun refresh(force: Boolean): Result<Unit> = Result.failure(PositioningError.NotConnected())
}

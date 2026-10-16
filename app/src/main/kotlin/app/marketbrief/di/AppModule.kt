package app.marketbrief.di

import app.marketbrief.BuildConfig
import app.marketbrief.core.data.NewsProvider
import app.marketbrief.core.data.remote.NewsApiProvider
import app.marketbrief.core.entitlements.PlanSource
import app.marketbrief.navigation.PlanSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}
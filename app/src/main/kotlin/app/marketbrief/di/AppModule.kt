package app.marketbrief.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.marketbrief.core.data.InMemoryNewsRepository
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.entitlements.PlanSource
import app.marketbrief.navigation.PlanSourceImpl
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.marketBriefDataStore by preferencesDataStore(name = "marketbrief_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.marketBriefDataStore

    @Provides
    @Singleton
    fun provideNewsRepository(): NewsRepository = InMemoryNewsRepository()

    @Provides
    @Singleton
    fun providePlanSource(): PlanSource = PlanSourceImpl()
}
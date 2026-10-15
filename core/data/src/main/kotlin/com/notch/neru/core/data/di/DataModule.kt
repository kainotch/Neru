package com.notch.neru.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.notch.neru.core.data.repository.SettingsRepository
import com.notch.neru.core.data.repository.SettingsRepositoryImpl
import com.notch.neru.core.data.repository.TimerRepository
import com.notch.neru.core.data.repository.TimerRepositoryImpl
import com.notch.neru.core.data.repository.WidgetConfigRepository
import com.notch.neru.core.data.repository.WidgetConfigRepositoryImpl
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    // A corrupted settings file must degrade to defaults, not crash every collector.
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {

    @Binds
    @Singleton
    abstract fun bindTimerRepository(impl: TimerRepositoryImpl): TimerRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindWidgetConfigRepository(impl: WidgetConfigRepositoryImpl): WidgetConfigRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}

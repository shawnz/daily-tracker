package org.shawnz.dailytracker.data

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): AppDatabase = AppDatabase.get(context)

    @Provides
    fun gameDao(database: AppDatabase): GameDao = database.gameDao()

    @Provides
    fun entryDao(database: AppDatabase): EntryDao = database.entryDao()

    @Provides
    @Singleton
    fun prefs(
        @ApplicationContext context: Context,
    ): Prefs = Prefs(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun repository(impl: DefaultRepository): Repository

    @Suppress("unused") // Used by Hilt.
    @Binds
    abstract fun settingsRepository(impl: DefaultSettingsRepository): SettingsRepository
}

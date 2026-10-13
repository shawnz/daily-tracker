package org.shawnz.dailytracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.shawnz.dailytracker.common.AppDispatchers.IO
import org.shawnz.dailytracker.common.ApplicationScope
import org.shawnz.dailytracker.common.Dispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, "daily-tracker.db")
            .addCallback(GameSourceRule)
            .build()

    @Provides
    fun gameDao(database: AppDatabase): GameDao = database.gameDao()

    @Provides
    fun entryDao(database: AppDatabase): EntryDao = database.entryDao()

    @Provides
    @Singleton
    fun settingsStore(
        @ApplicationContext context: Context,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<StoredSettings> = settingsDataStore(context, CoroutineScope(scope.coroutineContext + ioDispatcher))
}

@Suppress("unused") // Used by Hilt.
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun repository(impl: DefaultRepository): Repository

    @Binds
    @Singleton
    abstract fun settingsRepository(impl: DefaultSettingsRepository): SettingsRepository
}

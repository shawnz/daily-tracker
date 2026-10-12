package org.shawnz.dailytracker.common

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.shawnz.dailytracker.common.AppDispatchers.Default
import org.shawnz.dailytracker.common.AppDispatchers.IO

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @Dispatcher(IO)
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Dispatcher(Default)
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

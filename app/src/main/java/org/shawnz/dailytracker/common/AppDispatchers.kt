package org.shawnz.dailytracker.common

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(
    val dispatcher: AppDispatchers,
)

enum class AppDispatchers {
    Default,
    IO,
}

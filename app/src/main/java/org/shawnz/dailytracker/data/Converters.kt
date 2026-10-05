package org.shawnz.dailytracker.data

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

private const val NANOS_PER_SECOND = 1_000_000_000L

/**
 * Stores a [LocalDate] as days since 1970-01-01, and an [Instant] as nanoseconds since
 * 1970-01-01T00:00Z, which covers the years 1677 to 2262.
 */
class Converters {
    @TypeConverter
    fun toLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun toInstant(epochNanos: Long): Instant = Instant.ofEpochSecond(0, epochNanos)

    @TypeConverter
    fun fromInstant(instant: Instant): Long = instant.epochSecond * NANOS_PER_SECOND + instant.nano
}

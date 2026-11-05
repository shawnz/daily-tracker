package org.shawnz.dailytracker.data

import androidx.room3.ColumnTypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val NANOS_PER_SECOND = 1_000_000_000L

/**
 * Stores a [LocalDate] as days since 1970-01-01, and an [Instant] as nanoseconds since
 * 1970-01-01T00:00Z, which covers the years 1677 to 2262. A [ZoneId] is stored as its ID.
 */
class Converters {
    @ColumnTypeConverter
    fun toLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @ColumnTypeConverter
    fun fromLocalDate(date: LocalDate): Long = date.toEpochDay()

    @ColumnTypeConverter
    fun toInstant(epochNanos: Long): Instant = Instant.ofEpochSecond(0, epochNanos)

    @ColumnTypeConverter
    fun fromInstant(instant: Instant): Long = instant.epochSecond * NANOS_PER_SECOND + instant.nano

    @ColumnTypeConverter
    fun toZoneId(id: String): ZoneId = ZoneId.of(id)

    @ColumnTypeConverter
    fun fromZoneId(zone: ZoneId): String = zone.id
}

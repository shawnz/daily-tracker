package org.shawnz.dailytracker.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** The time zone furthest behind UTC. */
private val EARLIEST_ZONE = ZoneOffset.ofHours(-12)

fun earliestDateAnywhere(now: Instant = Instant.now()): LocalDate = now.atZone(EARLIEST_ZONE).toLocalDate()

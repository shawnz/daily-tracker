package org.shawnz.dailytracker.time

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/** The time zone furthest behind UTC. */
private val EARLIEST_ZONE = ZoneOffset.ofHours(-12)

fun earliestDateAnywhere(clock: Clock = Clock.systemDefaultZone()): LocalDate = LocalDate.now(clock.withZone(EARLIEST_ZONE))

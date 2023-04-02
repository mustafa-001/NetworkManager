package com.example.networkusage.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime


fun Long.toZonedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
    return ZonedDateTime.ofInstant(Instant.ofEpochSecond(this), zoneId)
}

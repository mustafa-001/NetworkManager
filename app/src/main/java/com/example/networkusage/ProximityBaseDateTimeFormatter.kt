package com.example.networkusage

import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

fun LocalDateTime.formatWithReference(reference: LocalDateTime): String {
    if (reference < this) {
        throw (Exception("Earlier Reference Value"))
    } else if (reference.dayOfYear == this.dayOfYear && reference.year == this.year) {
        return this.format(DateTimeFormatter.ofPattern("HH:mm"))
    } else if (reference.year == reference.year) {
        return this.format(DateTimeFormatter.ofPattern("LLL dd"))
    } else {
        return this.format(DateTimeFormatter.ofPattern("yy/MM/dd"))
    }
}
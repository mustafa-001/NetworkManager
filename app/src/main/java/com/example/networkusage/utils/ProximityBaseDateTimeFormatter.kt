package com.example.networkusage

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun ZonedDateTime.formatWithReference(reference: ZonedDateTime): String {
    if (reference < this) {
        throw (Exception("Earlier Reference Value"))
    } else if (reference.dayOfYear == this.dayOfYear && reference.year == this.year) {
        return this.format(DateTimeFormatter.ofPattern("HH:mm"))
    } else if (reference.year == this.year) {
        return this.format(DateTimeFormatter.ofPattern("LLL dd"))
    } else {
        return this.format(DateTimeFormatter.ofPattern("dd/MM/yy"))
    }
}
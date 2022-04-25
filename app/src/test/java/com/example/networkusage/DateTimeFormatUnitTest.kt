package com.example.networkusage

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DateTimeFormatUnitTest {
    val reference: LocalDateTime = LocalDateTime.now()

    @Test
    fun sameDayOnlyReturnsHour() {
        val earlierTime = reference.withHour(reference.hour - 4)
        Assert.assertEquals(
            earlierTime.format(
                DateTimeFormatter.ofPattern("HH:mm")
            ), earlierTime.formatWithReference(reference)
        )
    }

    @Test
    fun sameYearOnlyReturnsMonthAndDay() {
        val earlierTime = reference.withMonth(reference.monthValue - 1)
        Assert.assertEquals(
            earlierTime.format(
                DateTimeFormatter.ofPattern("LLL dd")
            ), earlierTime.formatWithReference(reference)
        )
    }
}
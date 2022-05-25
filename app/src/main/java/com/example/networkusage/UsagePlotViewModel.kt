package com.example.networkusage

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class UsagePlotViewModel(
    private val buckets: List<UsageDetailsManager.GeneralUsageInfo>,
    private val timeFrame: Pair<ZonedDateTime, ZonedDateTime>
) {
    val intervals: List<UsageInterval>
        get() {
            return toIntervals(buckets).fillEmptyIntervals(timeFrame)
        }

    private fun toIntervals(
        buckets: List<UsageDetailsManager.GeneralUsageInfo>,
    ): MutableList<UsageInterval> {
        val bucketsToAdd = mutableListOf<UsageInterval>()
        for (b in buckets) {
            bucketsToAdd.add(
                UsageInterval(
                    b.rxBytes,
                    b.txBytes,
                    Instant.ofEpochMilli(b.startTimeStamp).atZone(ZoneId.systemDefault()),
                    Instant.ofEpochMilli(b.endTimeStamp).atZone(ZoneId.systemDefault())
                )
            )
        }
        return bucketsToAdd.apply { sortBy { it.start.toEpochSecond() } }
    }

    fun MutableList<UsageInterval>.fillEmptyIntervals(
        timeFrame: Pair<ZonedDateTime, ZonedDateTime>
    ): MutableList<UsageInterval> {

        val timeStampDifference = 7200000000 //buckets[0].endTimeStamp - buckets[0].startTimeStamp
        var firstTimeStamp = (timeFrame.first.toEpochSecond() * 1000)
            .floorDiv(timeStampDifference) * timeStampDifference
        val lastTimeStamp = (timeFrame.first.toEpochSecond() * 1000
            .div(timeStampDifference) + 1) * timeStampDifference
        while (firstTimeStamp < lastTimeStamp) {
            this.add(
                UsageInterval(
                    0,
                    0,

                    Instant.ofEpochMilli(firstTimeStamp).atZone(ZoneId.systemDefault()),
                    Instant.ofEpochMilli(firstTimeStamp + timeStampDifference)
                        .atZone(ZoneId.systemDefault()),
//                ZonedDateTime.ofEpochSecond(
//                    firstTimeStamp / 1000 + timeStampDifference,
//                    0,
//                    ZoneOffset.UTC
                )
            )
            firstTimeStamp += timeStampDifference
        }
        return this
    }

}
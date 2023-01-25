package com.example.networkusage.ViewModels

import android.util.Log
import com.example.networkusage.UsageInterval
import com.example.networkusage.usage_details_processor.GeneralUsageInfo
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class BarPlotIntervalListViewModel(
    private val buckets: List<GeneralUsageInfo>,
    private val timeFrame: Pair<ZonedDateTime, ZonedDateTime>
) {
    val intervals: List<UsageInterval> = toIntervals(buckets).fillEmptyIntervals(timeFrame)

    fun groupedByDay(): List<UsageInterval> {

        if (intervals.isEmpty()) {
            Log.d("NetworkUsage", "UsageInterval list given to groupByDay is empty.")
            return listOf(
                UsageInterval(
                    0,
                    0,
                    ZonedDateTime.now().withDayOfMonth(1),
                    ZonedDateTime.now()
                )
            )
        }
        val newIntervalList = mutableListOf<UsageInterval>()
        var currentDay = intervals.first().start

        val groupedIntervals = intervals.groupBy { it.start.dayOfYear }
            .values
        for (group in groupedIntervals) {
            val dailyRxBytes: Long = group.map { it.rxBytes }.reduce { acc, it -> acc + it }
            val dailyTxBytes: Long = group.map { it.txBytes }.reduce { acc, it -> acc + it }
            Log.d(
                "NetworkUsage",
                "Grouping ${group.size} elements belonging to day ${group.first().start} \t Rx: $dailyRxBytes, Tx: $dailyTxBytes"
            )
            newIntervalList.add(
                UsageInterval(
                    dailyRxBytes,
                    dailyTxBytes, group.first().start, group.last().end
                )
            )
        }
        return newIntervalList
    }

    private fun toIntervals(
        buckets: List<GeneralUsageInfo>,
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

    private fun MutableList<UsageInterval>.fillEmptyIntervals(
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
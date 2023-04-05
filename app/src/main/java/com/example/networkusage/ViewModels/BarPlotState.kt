package com.example.networkusage.ViewModels

import android.util.Log
import com.example.networkusage.usageDetailsProcessor.Timeframe
import com.example.networkusage.usageDetailsProcessor.UsageData
import com.example.networkusage.usagePlots.PlotDataPoint
import com.example.networkusage.utils.toZonedDateTime
import java.time.ZonedDateTime

class BarPlotState(
    buckets: List<UsageData>,
    private val timeFrame: Timeframe
) {
    val intervals: List<PlotDataPoint> = toIntervals(buckets).fillEmptyIntervals(timeFrame)

    private val totalRx: Long =
        intervals.fold(0L) { acc, interval -> acc + interval.rxBytes }
    private val totalTx =
        intervals.fold(0L) { acc, interval -> acc + interval.txBytes }
    val usageTotal: UsageData = UsageData(timeFrame, totalTx, totalRx)

    val biggestUsage = if (buckets.isEmpty()) {
        0
    } else {
        buckets.maxOf { it.rxBytes + it.txBytes }
    }

    fun groupedByDay(): List<PlotDataPoint> {
        if (intervals.isEmpty()) {
            Log.d("NetworkUsage", "UsageInterval list given to groupByDay is empty.")
            return listOf(
                PlotDataPoint(
                    0,
                    0,
                    ZonedDateTime.now().withDayOfMonth(1).toEpochSecond(),
                    ZonedDateTime.now().toEpochSecond()
                )
            )
        }
        val newIntervalList = mutableListOf<PlotDataPoint>()

        val groupedIntervals = intervals.groupBy { it.startSeconds.toZonedDateTime().dayOfYear }
            .values
        for (group in groupedIntervals) {
            val dailyRxBytes: Long = group.map { it.rxBytes }.reduce { acc, it -> acc + it }
            val dailyTxBytes: Long = group.map { it.txBytes }.reduce { acc, it -> acc + it }
            Log.d(
                "NetworkUsage",
                "Grouping ${group.size} elements belonging to day ${group.first().startSeconds.toZonedDateTime()} \t" +
                        " Rx: $dailyRxBytes, Tx: $dailyTxBytes"
            )
            newIntervalList.add(
                PlotDataPoint(
                    dailyRxBytes,
                    dailyTxBytes,
                    group.first().startSeconds,
                    group.last().endSeconds
                )
            )
        }
        return newIntervalList
    }

    private fun toIntervals(
        buckets: List<UsageData>,
    ): MutableList<PlotDataPoint> {
        val bucketsToAdd = mutableListOf<PlotDataPoint>()
        for (b in buckets) {
            bucketsToAdd.add(
                PlotDataPoint(
                    b.rxBytes,
                    b.txBytes,
                    b.time.start.toEpochSecond(),
                    b.time.end.toEpochSecond()
                )
            )
        }
        return bucketsToAdd.apply { sortBy { it.startSeconds } }
    }

    private fun MutableList<PlotDataPoint>.fillEmptyIntervals(
        timeFrame: Timeframe
    ): MutableList<PlotDataPoint> {

        val timeStampDifference = 7200000 //buckets[0].endTimeStamp - buckets[0].startTimeStamp
        var firstTimeStamp = (timeFrame.start.toEpochSecond())
            .floorDiv(timeStampDifference) * timeStampDifference
        val lastTimeStamp = (timeFrame.end.toEpochSecond()
            .div(timeStampDifference) + 1) * timeStampDifference
        while (firstTimeStamp + timeStampDifference < lastTimeStamp) {
            this.add(
                PlotDataPoint(
                    0,
                    0,
                    firstTimeStamp,
                    firstTimeStamp + timeStampDifference
                )
            )
            firstTimeStamp += timeStampDifference
        }
        return this
    }
}
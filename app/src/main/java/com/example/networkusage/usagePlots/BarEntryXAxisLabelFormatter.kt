package com.example.networkusage.usagePlots

import com.example.networkusage.formatWithReference
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

class BarEntryXAxisLabelFormatter(val intervalsCallback: () -> List<PlotDataPoint>) :
    ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        //Barchart calls this function when determining size. When x-axis range change it calls this
        // function with older max value. bug?
        if (value.roundToInt() >= intervalsCallback().size) {
            return ""
        }
        val interval = intervalsCallback()[value.roundToInt()]
        var middle = interval.startSeconds + (
                (interval.endSeconds - interval.startSeconds) / 2
                )

        if (middle > ZonedDateTime.now().toEpochSecond()) {
            middle = ZonedDateTime.now().toEpochSecond()
        }

        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(middle), ZoneId.systemDefault())
            .formatWithReference(
                ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
            )
    }
}
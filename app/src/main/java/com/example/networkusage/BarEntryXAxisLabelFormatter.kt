package com.example.networkusage

import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

class BarEntryXAxisLabelFormatter(val intervalCallback: () -> List<UsageInterval>) :
    ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        //Barchart calls this function when determining size. When x-axis range change it calls this
        // function with older max value. bug?
        if (value.roundToInt() >= intervalCallback().size){
            return ""
        }
        val interval = intervalCallback()[value.roundToInt()]
        var middle = interval.start.plus(
            Duration.ofSeconds(
                (interval.end.toEpochSecond() - interval.start.toEpochSecond()) / 2
            )
        )
        if (middle > ZonedDateTime.now()) {
            middle = ZonedDateTime.now()
        }
        return middle.formatWithReference(
            ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        )
    }
}
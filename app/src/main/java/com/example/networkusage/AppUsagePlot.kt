package com.example.networkusage

import android.graphics.Color
import android.util.TypedValue
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.networkusage.ui.theme.DownloadColor
import com.example.networkusage.ui.theme.NetworkUsageTheme
import com.example.networkusage.ui.theme.UploadColor
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.ZoneOffset

data class UsageInterval(
    val rxBytes: Long,
    val txBytes: Long,
    val start: ZonedDateTime,
    val end: ZonedDateTime
)

//TODO Add a onClick callback.
//TODO Add a zoom or sliding view.
//TODO Make x-axis labels nice and round values. eg. 05.00 10.00
@Composable
fun BasicPlot(intervals: List<UsageInterval>) {
    Column {
        val onPrimaryColor = MaterialTheme.colors.onPrimary.toArgb()
        AndroidView(
            factory = { context ->
                LineChart(context)
            }, modifier = Modifier
                .height(200.dp)
                .fillMaxWidth(),
            update = { view ->
                view.setExtraOffsets(0f, 0f, 0f, 0f)
                val timeDifference = intervals.getOrNull(0).let {
                    if (it == null) {
                        0
                    } else {
                        (it.end.toEpochSecond() - it.start.toEpochSecond(

                        ))
                    }
                }

                val rxEntries = mutableListOf<Entry>()
                val txEntries = mutableListOf<Entry>()
                var runningRx = 0f
                var runningTx = 0f
                intervals.map {
                    runningRx += it.rxBytes.toFloat()
                    runningTx += it.rxBytes.toFloat() + it.txBytes.toFloat()
                    rxEntries.add(
                        Entry(
                            it.start.toEpochSecond().toFloat() + timeDifference / 2,
                            runningRx
                        )
                    )
                    txEntries.add(
                        Entry(
                            it.start.toEpochSecond().toFloat() + timeDifference / 2,
                            runningTx
                        )
                    )
                }

                if (txEntries.isEmpty()) {
                    val nowInEpoch =
                        ZonedDateTime.now().toEpochSecond().toFloat()
                    txEntries.add(Entry(nowInEpoch, 0f))
                    rxEntries.add(Entry(nowInEpoch, 0f))
                }
                if (rxEntries.last().x > ZonedDateTime.now().toEpochSecond()
                        .toFloat()
                ) {
                    rxEntries.last().x = ZonedDateTime.now().toEpochSecond().toFloat()
                    txEntries.last().x = ZonedDateTime.now().toEpochSecond().toFloat()
                }
                view.data = LineData(
                    LineDataSet(txEntries, "Transmitted").apply {
                        setDrawFilled(true)
                        color = Color.TRANSPARENT
                        fillColor = UploadColor.toArgb()
                        fillAlpha = 255
                        setDrawCircles(false)
                        setDrawValues(false)

                    },
                    LineDataSet(rxEntries, "Received").apply {
                        setDrawFilled(true)
                        color = Color.TRANSPARENT
                        fillColor = DownloadColor.toArgb()
                        fillAlpha = 255
                        setDrawCircles(false)
                        setDrawValues(false)

                    }
                )
                view.setTouchEnabled(false)
                view.axisLeft.setDrawGridLines(false)
                view.axisLeft.removeAllLimitLines()
                view.axisLeft.setDrawAxisLine(false)
                view.axisLeft.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                view.axisLeft.isEnabled = false
                view.axisRight.isEnabled = false
                view.xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(value.toLong()),
                            ZoneId.systemDefault()
                        ).formatWithReference(
                            ZonedDateTime.now()
                        )
                    }
                }
                view.xAxis.textColor = onPrimaryColor
                view.xAxis.setDrawGridLines(false)
                view.xAxis.position = XAxis.XAxisPosition.BOTTOM
                view.description.isEnabled = false
                view.legend.isEnabled = false
                view.invalidate()
            })
    }
}

private fun getThemeTextColor(view: View): Int {
    val typedValue = TypedValue()
    val theme = view.context.theme
    theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true)
    return typedValue.data
}

@Preview(showBackground = true, backgroundColor = Color.WHITE.toLong())
@Composable
fun BasicPlotPreview() {
    val points = mutableListOf<UsageInterval>()
    for (i in 10 downTo 0) {
        val p = UsageInterval(
            (10 - i.toLong()) * 100000,
            (9 - i.toLong()) * 20000,
            ZonedDateTime.now().minusHours((i * 2).toLong()),
            ZonedDateTime.now().minusHours(
                (i * 2 - 2).toLong()
            )
        )
        points.add(p)
    }
    NetworkUsageTheme {
        BasicPlot(intervals = points)
    }
}
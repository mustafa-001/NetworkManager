package com.example.networkusage

import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Optional

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
fun BarUsagePlot(intervals: List<UsageInterval>, touchListener: Optional<OnChartValueSelectedListener>) {
    Column {
        val onPrimaryColor = MaterialTheme.colors.onPrimary.toArgb()
        AndroidView(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth(),
            factory = { context ->
                BarChart(context)
            },
            update = { barChart ->
                barChart.setExtraOffsets(0f, 0f, 0f, 0f)
                val timeDifference = intervals.getOrNull(0).let {
                    if (it == null) {
                        0
                    } else {
                        (it.end.toEpochSecond() - it.start.toEpochSecond(
                        ))
                    }
                }

                val rxEntries = mutableListOf<BarEntry>()
                val txEntries = mutableListOf<BarEntry>()
                intervals.map {
                    rxEntries.add(
                        BarEntry(
                            it.start.toEpochSecond().toFloat() + timeDifference / 2,
                           it.rxBytes.toFloat()
                        )
                    )
                    txEntries.add(
                        BarEntry(
                            it.start.toEpochSecond().toFloat() + timeDifference / 2,
                            it.rxBytes.toFloat() + it.txBytes.toFloat()
                        )
                    )
                }
                if (txEntries.isEmpty()) {
                    val nowInEpoch =
                        ZonedDateTime.now().toEpochSecond().toFloat()
                    txEntries.add(BarEntry(nowInEpoch, 0f))
                    rxEntries.add(BarEntry(nowInEpoch, 0f))
                }
                if (rxEntries.last().x > ZonedDateTime.now().toEpochSecond()
                        .toFloat()
                ) {
                    rxEntries.last().x = ZonedDateTime.now().toEpochSecond().toFloat()
                    txEntries.last().x = ZonedDateTime.now().toEpochSecond().toFloat()
                }

                barChart.data = BarData(
                    BarDataSet(txEntries, "Transmitted").apply {
                        color = UploadColor.toArgb()
                        setDrawValues(false)
                    },
                    BarDataSet(rxEntries, "Received").apply {
                        color = DownloadColor.toArgb()
                        setDrawValues(false)

                    }
                ).apply {
                    barWidth = timeDifference.toFloat() / 2
                }
                barChart.axisLeft.setDrawGridLines(false)
                barChart.axisLeft.removeAllLimitLines()
                barChart.axisLeft.setDrawAxisLine(false)
                barChart.axisLeft.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                barChart.axisLeft.isEnabled = false
                barChart.axisRight.isEnabled = false
                barChart.xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(value.toLong()),
                            ZoneId.systemDefault()
                        ).formatWithReference(
                            ZonedDateTime.now()
                        )
                    }
                }
                barChart.xAxis.textColor = onPrimaryColor
                barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                barChart.description.isEnabled = false
                barChart.legend.isEnabled = false
                if (touchListener.isPresent){
                    barChart.setOnChartValueSelectedListener(touchListener.get())
                }
                barChart.setTouchEnabled(true)
                barChart.invalidate()
                barChart
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
fun BasicBarPlotPreview() {
    val points = mutableListOf<UsageInterval>()
    for (i in 10 downTo 0) {
        val p = UsageInterval(
            (11 - i.toLong()) * 100,
            (11 - i.toLong()) * 20,
            ZonedDateTime.now().minusHours((i * 2).toLong()),
            ZonedDateTime.now().minusHours(
                (i * 2 - 2).toLong()
            )
        )
        points.add(p)
    }
    NetworkUsageTheme {
        BarUsagePlot(intervals = points, Optional.empty())
    }
}
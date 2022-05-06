package com.example.networkusage

import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Colors
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
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.LocalDateTime
import java.time.ZoneOffset

data class UsagePoint(
    val rxBytes: Long,
    val txBytes: Long,
    val start: LocalDateTime,
    val end: LocalDateTime
)

//TODO Add a onClick callback.
//TODO Add a zoom or sliding view.
//TODO Make x-axis labels nice and round values. eg. 05.00 10.00
@Composable
fun BasicPlot(points: List<UsagePoint>) {
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
                val timeDifference = points.getOrNull(0).let {
                    if (it == null) {
                        0
                    } else {
                        (it.end.toEpochSecond(ZoneOffset.UTC) - it.start.toEpochSecond(
                            ZoneOffset.UTC
                        ))
                    }
                }

                val rxEntries = mutableListOf<Entry>()
                val txEntries = mutableListOf<Entry>()
                var runningRx = 0f
                var runningTx = 0f
                points.map {
                    runningRx += it.rxBytes.toFloat()
                    runningTx += it.rxBytes.toFloat() + it.txBytes.toFloat()
                    rxEntries.add(
                        Entry(
                            it.start.toEpochSecond(ZoneOffset.UTC).toFloat() + timeDifference / 2,
                            runningRx
                        )
                    )
                    txEntries.add(
                        Entry(
                            it.start.toEpochSecond(ZoneOffset.UTC).toFloat() + timeDifference / 2,
                            runningTx
                        )
                    )
                }
                if (txEntries.isEmpty()) {
                    val nowInEpoch =
                        LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toFloat()
                    txEntries.add(Entry(nowInEpoch, 0f))
                    rxEntries.add(Entry(nowInEpoch, 0f))
                }
                if (rxEntries.last().x > LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                        .toFloat()
                ) {
                    rxEntries.last().x = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toFloat()
                    txEntries.last().x = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toFloat()

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
                        return LocalDateTime.ofEpochSecond(
                            (value.toLong()),
                            0,
                            ZoneOffset.UTC
                        ).formatWithReference(
                            LocalDateTime.now()
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

@Preview(showBackground = true, backgroundColor = Color.BLACK.toLong())
@Composable
fun BasicPlotPreview() {
    val points = mutableListOf<UsagePoint>()
    for (i in 10 downTo 0) {
        val p = UsagePoint(
            (10 - i.toLong()) * 100000,
            (9 - i.toLong()) * 20000,
            LocalDateTime.now().minusHours((i * 2).toLong()),
            LocalDateTime.now().minusHours(
                (i * 2 - 2).toLong()
            )
        )
        points.add(p)
    }
    NetworkUsageTheme {
        BasicPlot(points = points)
    }
}
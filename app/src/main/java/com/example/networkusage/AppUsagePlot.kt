package com.example.networkusage

import android.graphics.Color
import android.icu.text.AlphabeticIndex
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.networkusage.ui.theme.NetworkUsageTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.time.LocalDateTime
import java.time.ZoneOffset

data class UsagePoint(
    val rxBytes: Long,
    val txBytes: Long,
    val start: LocalDateTime,
    val end: LocalDateTime
)

@Composable
fun BasicPlot(points: List<UsagePoint>) {
    Column {
        AndroidView(factory = { context ->
            LineChart(context)
        }, modifier = Modifier
            .height(400.dp)
            .fillMaxWidth(),
            update = { view ->
                view.setExtraOffsets(0f, 0f, 0f, 0f)
                val rxEntries = mutableListOf<Entry>()
                val txEntries = mutableListOf<Entry>()
                points.map {
                    rxEntries.add(
                        Entry(
                            it.start.toEpochSecond(ZoneOffset.UTC).toFloat(),
                            it.rxBytes.toFloat()
                        )
                    )
                    txEntries.add(
                        Entry(
                            it.start.toEpochSecond(ZoneOffset.UTC).toFloat(),
                            (it.txBytes + it.rxBytes).toFloat()
                        )
                    )
                }

                val dataset = LineDataSet(rxEntries, "Usage")
                view.data = LineData(
                    LineDataSet(txEntries, "Transmitted").apply {
                        setDrawFilled(true)
                        fillColor = Color.parseColor("#D06F24")
                        setDrawCircles(false)
                        setDrawValues(false)

                    },
                    LineDataSet(rxEntries, "Received").apply {
                        setDrawFilled(true)
                        fillColor = Color.parseColor("#226C0F")
                        setDrawCircles(false)
                        setDrawValues(false)

                    }
                )
                view.axisLeft.setDrawGridLines(false)
                view.axisLeft.removeAllLimitLines()
                view.axisLeft.setDrawAxisLine(false)
                view.axisLeft.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                view.axisRight.isEnabled = false
                view.xAxis.isEnabled = false
                view.description.isEnabled = false
                view.legend.isEnabled = false
                view.invalidate()
            })
    }
}

@Preview(showBackground = true)
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
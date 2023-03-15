package com.example.networkusage

import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.time.ZonedDateTime
import java.util.*

//TODO Rename, possibly BarUsagePlotDataPoint, BarUsagePlotIntervalData, BarUsagePlotEntry
data class UsageInterval(
    val rxBytes: Long,
    val txBytes: Long,
    val start: ZonedDateTime,
    val end: ZonedDateTime
)

//TODO Add a zoom or sliding view.
@Composable
fun BarUsagePlot(
    intervals: List<UsageInterval>,
    touchListener: Optional<OnChartValueSelectedListener>,
    xAxisLabelFormatter: ValueFormatter,
    animationCallbackSetter: (() -> Unit) -> Unit = {}
) {
    Card(
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
        AndroidView(
            modifier = Modifier
                .height(200.dp)
                .padding(bottom = 4.dp)
                .fillMaxWidth(),
            factory = { context ->
                BarChart(context)
            }
        ) { barChart ->
            Log.d("Network Usage", "Composing bar usage plot. $intervals")
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
            intervals.mapIndexed { index, it ->
                Log.d(
                    "Network Usage", "Adding interval to entries as bar entry " +
                            "index: $index interval: $it"
                )
                rxEntries.add(
                    BarEntry(
//                            it.start.toEpochSecond().toFloat() + timeDifference / 2,
                        index.toFloat(),
                        it.rxBytes.toFloat()
                    )
                )
                txEntries.add(
                    BarEntry(
//                            it.start.toEpochSecond().toFloat() + timeDifference / 2,
                        index.toFloat(),
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

            barChart.data = BarData(
                BarDataSet(txEntries, "Transmitted").apply {
                    color = UploadColor.toArgb()
                    setDrawValues(false)
                },
                BarDataSet(rxEntries, "Received").apply {
                    color = DownloadColor.toArgb()
                    setDrawValues(false)

                }
            )
            barChart.axisLeft.setDrawGridLines(false)
            barChart.isDoubleTapToZoomEnabled = false
            barChart.axisLeft.removeAllLimitLines()
            barChart.axisLeft.setDrawAxisLine(false)
            barChart.axisLeft.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
            barChart.axisLeft.isEnabled = false
            barChart.axisRight.isEnabled = false
            barChart.xAxis.valueFormatter = xAxisLabelFormatter
            barChart.xAxis.textColor = onSurfaceColor
            barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            barChart.description.isEnabled = false
            barChart.barData.barWidth = 0.7f
            barChart.legend.isEnabled = false
            if (touchListener.isPresent) {
                barChart.setOnChartValueSelectedListener(touchListener.get())
            } else {
                barChart.setTouchEnabled(false)
            }
            barChart.invalidate()
            //Set calling a callback to animate graph for calling @Composable.
            animationCallbackSetter {
                barChart.animateY(
                    500,
                    com.github.mikephil.charting.animation.Easing.EaseInCubic
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = Color.WHITE.toLong())
@Composable
fun BasicBarPlotPreview() {
    val points = mutableListOf<UsageInterval>()
    for (i in 1..10) {
        val p = UsageInterval(
            (i.toLong()) * 100,
            (i.toLong()) * 20,
            ZonedDateTime.now().minusHours((i * 2 - 2).toLong()),
            ZonedDateTime.now().minusHours(
                (i * 2).toLong()
            )
        )
        points.add(p)
    }
    NetworkUsageTheme {
        BarUsagePlot(
            intervals = points,
            Optional.empty(),
            BarEntryXAxisLabelFormatter { -> points })
    }
}
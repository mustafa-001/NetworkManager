package com.example.networkusage

import android.app.usage.NetworkStats
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LiveData
import com.example.networkusage.ViewModels.UsagePerUIDPlotViewModel
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


@Composable
fun UsageDetailsForUID(
    usageDetailsManager: UsageDetailsManager,
    packageManager: PackageManager,
    uid: Int,
    timeFrame: LiveData<Pair<ZonedDateTime, ZonedDateTime>>
) {
    val time by timeFrame.observeAsState(
        Pair(
            ZonedDateTime.now().withDayOfYear(1),
            ZonedDateTime.now()
        )
    )

    var selectedInterval by remember { mutableStateOf(Pair(time.first, time.second))}


    class BarPlotTouchListener() : OnChartValueSelectedListener {
        init {
            Log.d(
                "NetworkUsage", "PlotTouchListener instantiated."
            )
        }

        /**
         * Called when a value has been selected inside the chart.
         *
         * @param e The selected Entry.
         * @param h The corresponding highlight object that contains information
         * about the highlighted position
         */
        override fun onValueSelected(e: Entry?, h: Highlight?) {
            selectedInterval =
            Pair(ZonedDateTime.ofInstant(Instant.ofEpochSecond(h!!.x.toLong()-60*60*2), ZoneId.systemDefault()),
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(h.x.toLong()+60*60*2), ZoneId.systemDefault()))
            Log.d(
                "NetworkUsage", "Bar plot, a value selected" +
                        "entry: ${e}" +
                        "highlight: $h" +
                        "highlight data index: ${h!!.dataIndex} " +
                        "highlight x value: ${h.x}" +
                        "highlight y value: ${h.y}+" +
                        "\n ${selectedInterval.first}"
            )
        }

        /**
         * Called when nothing has been selected or an "un-select" has been made.
         */
        override fun onNothingSelected() {
            Log.d("NetworkUsage", "Nothing selected")
        }
    }
    LazyColumn(content = {
        val buckets = usageDetailsManager.queryForUid(uid, time)
        val appUsageInfo: UsageDetailsManager.AppUsageInfo = when (uid) {
            NetworkStats.Bucket.UID_ALL -> {
                UsageDetailsManager.AppUsageInfo(
                    uid,
                    "All",
                    "All",
                    0, 0,
                    null
                )
            }
            NetworkStats.Bucket.UID_TETHERING -> {
                UsageDetailsManager.AppUsageInfo(
                    uid,
                    "Tethering",
                    "Tethering",
                    0, 0,
                    null
                )
            }
            NetworkStats.Bucket.UID_REMOVED -> {
                UsageDetailsManager.AppUsageInfo(
                    uid,
                    "Removed",
                    "Removed",
                    0, 0,
                    null
                )
            }
            else -> {
                val p = packageManager.getPackageInfo(
                    packageManager.getPackagesForUid(uid)!![0],
                    PackageManager.GET_META_DATA
                )
                UsageDetailsManager.AppUsageInfo(
                    uid,
                    packageManager.getApplicationLabel(p.applicationInfo).toString(),
                    p.packageName,
                    0, 0,
                    p.applicationInfo.loadIcon(packageManager)
                )
            }
        }
        buckets.forEach {
            appUsageInfo.rxBytes += it.rxBytes
            appUsageInfo.txBytes += it.txBytes
        }
        item {
            PackageInfo(
                usageInfo = appUsageInfo
            )
        }
        class flinger: FlingBehavior{
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                return 1000f
            }

        }
        item {
            Row(Modifier.horizontalScroll(ScrollState(0), flingBehavior = flinger()).fillMaxWidth()) {
                val intervals = UsagePerUIDPlotViewModel(buckets, time).intervals
                CumulativeUsageLinePlot(intervals )
                BarUsagePlot(intervals, Optional.of(BarPlotTouchListener()))
            }
        }
        val timeFormatter = DateTimeFormatter.ofPattern("dd.MM.YY-HH.mm")
        for (bucket in buckets) {
            if (bucket.endTimeStamp / 1000 > selectedInterval.second.toEpochSecond()
                || bucket.startTimeStamp / 1000 < selectedInterval.first.toEpochSecond()){
                continue
            }
            item {
                Column(
                    Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceAround) {
                        Text(
                            text =
                            Instant.ofEpochMilli(bucket.startTimeStamp)
                                .atZone(ZoneId.systemDefault())
                                .format(timeFormatter),
                            modifier = Modifier.padding(2.dp)
                        )
                        Text(
                            text =
                            Instant.ofEpochMilli(bucket.endTimeStamp).atZone(ZoneId.systemDefault())
                                .format(timeFormatter),
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "Received: ${byteToStringRepresentation(bucket.rxBytes)}",
                            modifier = Modifier.padding(2.dp)
                        )
                        Text(
                            text = "Sent: ${byteToStringRepresentation(bucket.txBytes)}",
                            modifier = Modifier.padding(2.dp)
                        )
                    }
//                    Text(text = "UID: " + bucket.uid.toString(), modifier = Modifier.padding(2.dp))
//                    Text(
//                        text = "state: " + bucket.state.toString(),
//                        modifier = Modifier.padding(2.dp)
//                    )
//                    Text(text = "Tag:\t" + bucket.tag.toString(), modifier = Modifier.padding(2.dp))
                }
            }
        }
    }
    )
}

@Composable
fun PackageInfo(usageInfo: UsageDetailsManager.AppUsageInfo) {
    Row() {

        if (usageInfo.icon == null) {
            val vector = ImageVector.vectorResource(id = R.drawable.ic_baseline_settings_24)
            val painter = rememberVectorPainter(image = vector)
            Icon(painter, "")
        } else {
            Icon(
                usageInfo.icon!!.toBitmap().asImageBitmap(),
                "",
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = usageInfo.name ?: (usageInfo.packageName), maxLines = 1
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            modifier = Modifier.width(IntrinsicSize.Max),
            text = "Rx: ${byteToStringRepresentation(usageInfo.rxBytes)}"
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            modifier = Modifier.width(IntrinsicSize.Max),
            text = "Tx: ${byteToStringRepresentation(usageInfo.txBytes)}"
        )

    }
}

@Preview(showBackground = true)
@Composable
fun PackageInfoPreview() {
    PackageInfo(
        usageInfo = UsageDetailsManager.AppUsageInfo(
            100,
            "Android",
            "com.android",
            txBytes = 100000,
            rxBytes = 10000000,
            null
        )
    )
}

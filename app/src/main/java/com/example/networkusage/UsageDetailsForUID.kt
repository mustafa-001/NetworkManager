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
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


@OptIn(ExperimentalPagerApi::class)
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

    val barPlotTouchListener by remember {
        mutableStateOf(BarPlotTouchListener())
    }
    val selectedInterval by barPlotTouchListener.interval.observeAsState(
        Pair(
            ZonedDateTime.now().withDayOfYear(1),
            ZonedDateTime.now()
        )
    )


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
        item {
            val intervals = UsagePerUIDPlotViewModel(buckets, time).intervals
            HorizontalPager(count = 2) { page ->
                if (page == 0) {
                    BarUsagePlot(intervals, Optional.of(barPlotTouchListener))
                } else if (page == 1) {
                    CumulativeUsageLinePlot(intervals)
                }
            }
        }
        val timeFormatter = DateTimeFormatter.ofPattern("dd.MM.YY-HH.mm")
        for (bucket in buckets) {
            if (bucket.endTimeStamp / 1000 > selectedInterval.second.toEpochSecond()
                || bucket.startTimeStamp / 1000 < selectedInterval.first.toEpochSecond()
            ) {
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

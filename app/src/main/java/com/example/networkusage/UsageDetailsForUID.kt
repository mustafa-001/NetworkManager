package com.example.networkusage

import android.app.usage.NetworkStats
import android.content.pm.PackageManager
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
import com.example.networkusage.ViewModels.BarPlotIntervalListViewModel
import com.example.networkusage.usage_details_processor.AppUsageInfo
import com.example.networkusage.usage_details_processor.GeneralUsageInfo
import com.example.networkusage.usage_details_processor.UsageDetailsProcessorInterface
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


@OptIn(ExperimentalPagerApi::class)
@Composable
fun UsageDetailsForPackage(
    usageDetailsManager: UsageDetailsProcessorInterface,
    packageManager: PackageManager,
    uid: Int,
    timeFrame: LiveData<Pair<ZonedDateTime, ZonedDateTime>>
) {
    val timeframe by timeFrame.observeAsState(
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

    val isNeedGrouping by remember(timeframe.first, timeframe.second) {
        mutableStateOf(
            !timeframe.first.plusDays(7)
                .isAfter(timeframe.second)
        )
    }


    LazyColumn {
        val buckets = usageDetailsManager.getUsageByUIDGroupedByTime(uid, timeframe)
        val appUsageInfo: AppUsageInfo = when (uid) {
            NetworkStats.Bucket.UID_ALL -> {
                AppUsageInfo(
                    uid,
                    "All",
                    "All",
                    0, 0,
                    null
                )
            }
            NetworkStats.Bucket.UID_TETHERING -> {
                AppUsageInfo(
                    uid,
                    "Tethering",
                    "Tethering",
                    0, 0,
                    null
                )
            }
            NetworkStats.Bucket.UID_REMOVED -> {
                AppUsageInfo(
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
                AppUsageInfo(
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
            PackageUsageInfoHeader(
                usageInfo = appUsageInfo
            )
        }

        item {
            val barPlotIntervalListViewModel = BarPlotIntervalListViewModel(buckets, timeframe)
            val barPlotIntervals = if (isNeedGrouping)
                barPlotIntervalListViewModel.groupedByDay()
            else {
                barPlotIntervalListViewModel.intervals
            }
            HorizontalPager(count = 2) { page ->
                if (page == 0) {
                    BarUsagePlot(barPlotIntervals, Optional.of(barPlotTouchListener), BarEntryXAxisLabelFormatter({ -> barPlotIntervals}))
                } else if (page == 1) {
                    CumulativeUsageLinePlot(barPlotIntervalListViewModel.intervals)
                }
            }
        }
        val timeFormatter = DateTimeFormatter.ofPattern("dd.MM.YY-HH.mm")
        for (bucket in buckets) {
            val bucketStart = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(bucket.startTimeStamp / 1000),
                ZoneId.systemDefault()
            )
            if ((selectedInterval.first.isBefore(
                    ZonedDateTime.of(
                        1980, 1, 1, 1, 1, 1, 0, ZoneId.systemDefault()
                    )
                ) ||
                        bucket.endTimeStamp / 1000 < selectedInterval.second.toEpochSecond()
                        && bucket.startTimeStamp / 1000 > selectedInterval.first.toEpochSecond()
                        ) ||
                (isNeedGrouping &&
                        bucketStart.dayOfYear == selectedInterval.first.dayOfYear)
            ) {
                item {
                    BucketDetailsRow(bucket, timeFormatter)
                }
            }
        }
    }
}

@Composable
fun BucketDetailsRow(
    bucket: GeneralUsageInfo,
    timeFormatter: DateTimeFormatter
) {
    Column(
        Modifier
            .padding(7.dp)
            .fillMaxWidth()
    ) {
        Row(horizontalArrangement = Arrangement.SpaceAround) {
            Text(
                text =
                Instant.ofEpochMilli(bucket.startTimeStamp)
                    .atZone(ZoneId.systemDefault())
                    .format(timeFormatter),
                modifier = Modifier.padding(1.dp)
            )
            Text(
                text =
                Instant.ofEpochMilli(bucket.endTimeStamp).atZone(ZoneId.systemDefault())
                    .format(timeFormatter),
                modifier = Modifier.padding(1.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Received: ${byteToStringRepresentation(bucket.rxBytes)}",
                modifier = Modifier.padding(1.dp)
            )
            Text(
                text = "Sent: ${byteToStringRepresentation(bucket.txBytes)}",
                modifier = Modifier.padding(1.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IntervalUsageInfoPreview() {
    val bucket = GeneralUsageInfo(
        10000,
        100000,
        ZonedDateTime.now().minusDays(2).toEpochSecond(),
        ZonedDateTime.now().toEpochSecond()
    )
    val timeFormatter = DateTimeFormatter.ofPattern("dd.MM.YY-HH.mm")
    BucketDetailsRow(bucket, timeFormatter)
}


@Composable
fun PackageUsageInfoHeader(usageInfo: AppUsageInfo) {
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
    PackageUsageInfoHeader(
        usageInfo = AppUsageInfo(
            100,
            "Android",
            "com.android",
            txBytes = 100000,
            rxBytes = 10000000,
            null
        )
    )
}

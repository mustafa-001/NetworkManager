package com.example.networkusage

import android.app.usage.NetworkStats
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.networkusage.ViewModels.BarPlotIntervalListViewModel
import com.example.networkusage.ViewModels.CommonTopbarParametersViewModel
import com.example.networkusage.ViewModels.UsageDetailsForUIDViewModel
import com.example.networkusage.ui.theme.DownloadColor
import com.example.networkusage.ui.theme.UploadColor
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
    commonTopBarParametersViewModel: CommonTopbarParametersViewModel,
    packageManager: PackageManager,
    uid: Int,
    usageDetailsProcessor: UsageDetailsProcessorInterface
) {

    Log.d("Network Usage", "composing UsageDetailsForPackage.")
    val timeframe by commonTopBarParametersViewModel.timeFrame.observeAsState(
        commonTopBarParametersViewModel.timeFrame.value!!
    )
    val usageDetailsForUIDViewModel = remember {
        UsageDetailsForUIDViewModel(
            uid,
            commonTopBarParametersViewModel.timeFrame,
            usageDetailsProcessor
        )
    }

    val usageInfos: List<GeneralUsageInfo> by usageDetailsForUIDViewModel.usageByUIDGroupedByTime.observeAsState(
        usageDetailsForUIDViewModel.usageByUIDGroupedByTime.value!!
    )

    val barPlotTouchListener by remember {
        mutableStateOf(BarPlotTouchListener())
    }
    val selectedIntervalIndex by barPlotTouchListener.intervalIndex.observeAsState(
        barPlotTouchListener.intervalIndex.value!!
    )

    //TODO: Move this variable and related logic to barPlotIntervalListViewModel.
    val isNeedGrouping by remember(timeframe.first, timeframe.second) {
        derivedStateOf {
            !timeframe.first.plusDays(7)
                .isAfter(timeframe.second)
        }
    }

    val appsTotalUsageDuringTimeframe: AppUsageInfo by remember(usageInfos) {
        derivedStateOf {
            appUsageInfo(uid, packageManager).also { appUsageInfo ->
                usageInfos.forEach {
                    appUsageInfo.rxBytes += it.rxBytes
                    appUsageInfo.txBytes += it.txBytes
                }
            }
        }
    }

    val barPlotIntervalListViewModel: BarPlotIntervalListViewModel by remember(
        usageInfos,
        timeframe
    ) {
        //Reset selected interval before redrawing this composable.
        barPlotTouchListener.onNothingSelected()
        mutableStateOf(BarPlotIntervalListViewModel(usageInfos, timeframe))
    }

    //Animation function to be set by plotting library.
    var animationCallback: () -> Unit by remember {
        mutableStateOf({ -> })
    }

    val barPlotIntervals by remember(isNeedGrouping) {
        derivedStateOf {
            if (isNeedGrouping)
                barPlotIntervalListViewModel.groupedByDay()
            else {
                barPlotIntervalListViewModel.intervals
            }
        }
    }
    LaunchedEffect(usageInfos) {
        Log.d("Network Usage", "Timeframe changed, calling animationCallback")
        animationCallback()
    }
    val selectedInterval =
        if (barPlotIntervals.isEmpty() || selectedIntervalIndex.isPresent.not()) {
            Log.d("Network Usage", "No interval has been selected.")
            Optional.empty<Pair<ZonedDateTime, ZonedDateTime>>()
        } else {
            try {
                val usageInterval =
                    barPlotIntervals[selectedIntervalIndex.get()]
                Log.d("Network Usage", "interval has been selected: ${usageInterval.start}")
                Optional.of(Pair(usageInterval.start, usageInterval.end))
            } catch (e: IndexOutOfBoundsException) {
                Log.d(
                    "Network Usage",
                    "barPlotIntervals changed but selected intervals is still not resetted..\n" +
                            "barPlotIntervals.size: ${barPlotIntervals.size} \n" +
                            "barPlotIntervals start: $barPlotIntervals[0].start \n" +
                            "barPlotIntervals end: $barPlotIntervals[barPlotIntervals.size - 1].end \n" +
                            "selectedIntervalIndex: $selectedIntervalIndex"
                )
                Optional.empty<Pair<ZonedDateTime, ZonedDateTime>>()
            }
        }

    LazyColumn {
        item {
            PackageUsageInfoHeader(
                usageInfo = appsTotalUsageDuringTimeframe
            )
        }

        item {
            HorizontalPager(count = 2) { page ->
                if (page == 0) {
                    val xAxisLabelFormatter by remember(barPlotIntervals) {
                        derivedStateOf {
                            BarEntryXAxisLabelFormatter({ -> barPlotIntervals })
                        }
                    }
                    BarUsagePlot(
                        barPlotIntervals,
                        Optional.of(barPlotTouchListener),
                        xAxisLabelFormatter,
                        { animationCallback = it }
                    )

                } else if (page == 1) {
                    CumulativeUsageLinePlot(barPlotIntervalListViewModel.intervals)
                }
            }
        }
        val timeFormatter = DateTimeFormatter.ofPattern("dd.MM.YY-HH.mm")
        for (bucket in usageInfos.sortedBy { it.rxBytes + it.txBytes }.reversed()) {
            if (selectedInterval.isPresent.not() ||
                (bucket.endTimeStamp / 1000 <= selectedInterval.get().second.toEpochSecond()
                        && bucket.startTimeStamp / 1000 >= selectedInterval.get().first.toEpochSecond())
            ) {
                item {
                    BucketDetailsRow(bucket, timeFormatter)
                }
            }
        }
    }
}

private fun appUsageInfo(
    uid: Int,
    packageManager: PackageManager
) = when (uid) {
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val start = Instant.ofEpochMilli(bucket.startTimeStamp)
                .atZone(ZoneId.systemDefault())
            val end = Instant.ofEpochMilli(bucket.endTimeStamp)
                .atZone(ZoneId.systemDefault())
            if (start.toLocalDate().dayOfYear != end.toLocalDate().dayOfYear) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = start.format(timeFormatter),
                        color = MaterialTheme.colors.secondaryVariant,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = end.format(timeFormatter),
                        color = MaterialTheme.colors.secondaryVariant,
                        fontSize = 14.sp,
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = start.format(DateTimeFormatter.ofPattern("dd.MM.YY")),
                        color = MaterialTheme.colors.secondaryVariant,
                        fontSize = 13.sp,
                    )
                    Row {
                        Text(
                            text = start.format(DateTimeFormatter.ofPattern("HH:mm")),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = end.format(DateTimeFormatter.ofPattern("HH:mm")),
                        )
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = byteToStringRepresentation(bucket.rxBytes + bucket.txBytes),
                modifier = Modifier
                    .padding(1.dp)
                    .weight(1f)
            )
            Text(
                text = byteToStringRepresentation(bucket.txBytes),
                fontSize = 13.sp,
                color = UploadColor,
                modifier = Modifier.padding(1.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = byteToStringRepresentation(bucket.rxBytes),
                fontSize = 13.sp,
                color = DownloadColor,
                modifier = Modifier.padding(1.dp),
            )
            Text(
                //difference between start and end in minutes
                text = "${(bucket.endTimeStamp - bucket.startTimeStamp) / 1000 / 60} min",
                color = MaterialTheme.colors.secondaryVariant,
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
    Row(verticalAlignment = CenterVertically, modifier = Modifier.height(50.dp)) {
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
            modifier = Modifier.width(IntrinsicSize.Max),
            text = usageInfo.name ?: (usageInfo.packageName), maxLines = 1
        )
        Spacer(
            modifier = Modifier
                .width(8.dp)
                .weight(2f)
        )
        Text(
            modifier = Modifier.width(IntrinsicSize.Max),
            text = byteToStringRepresentation(usageInfo.rxBytes + usageInfo.txBytes)
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
package com.example.networkusage

import android.app.usage.NetworkStats
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
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
import androidx.core.graphics.drawable.toBitmap
import com.example.networkusage.ViewModels.BarPlotIntervalListViewModel
import com.example.networkusage.ViewModels.CommonTopbarParametersViewModel
import com.example.networkusage.usageDetailsForUIDScreen.UsageDetailsForUIDViewModel
import com.example.networkusage.ui.theme.DownloadColor
import com.example.networkusage.ui.theme.NetworkUsageTheme
import com.example.networkusage.ui.theme.UploadColor
import com.example.networkusage.usageDetailsProcessor.AppUsageInfo
import com.example.networkusage.usageDetailsProcessor.GeneralUsageInfo
import com.example.networkusage.usageDetailsProcessor.UsageDetailsProcessorInterface
import com.example.networkusage.usagePlots.BarEntryXAxisLabelFormatter
import com.example.networkusage.usagePlots.BarPlotTouchListener
import com.example.networkusage.usagePlots.BarUsagePlot
import com.example.networkusage.usagePlots.CumulativeUsageLinePlot
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


@OptIn(ExperimentalPagerApi::class)
@Composable
fun UsageDetailsForUIDScreen(
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

    LazyColumn(
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 8.dp),
    ) {
        item {
            PackageUsageInfoHeader(
                usageInfo = appsTotalUsageDuringTimeframe,
                modifier = Modifier.padding(start = 0.dp, end = 0.dp, top = 8.dp, bottom = 4.dp)
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
                        Modifier.padding(top = 4.dp, bottom = 4.dp, start = 0.dp, end = 0.dp),
                        { animationCallback = it },
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
                item(key = bucket.startTimeStamp) {
                    BucketDetailsRow(
                        bucket,
                        timeFormatter,
                        Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                    )
                    Divider()
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
    timeFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val start = Instant.ofEpochMilli(bucket.startTimeStamp)
                    .atZone(ZoneId.systemDefault())
                val end = Instant.ofEpochMilli(bucket.endTimeStamp)
                    .atZone(ZoneId.systemDefault())
                if (start.toLocalDate().dayOfYear != end.toLocalDate().dayOfYear) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = start.format(timeFormatter),
                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                        )
                        Text(
                            text = end.format(timeFormatter),
                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {

                        Row(horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = start.format(DateTimeFormatter.ofPattern("HH:mm")),
                                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = end.format(DateTimeFormatter.ofPattern("HH:mm")),
                                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Text(
                            text = start.format(DateTimeFormatter.ofPattern("dd.MM.YY")),
                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = byteToStringRepresentation(bucket.rxBytes + bucket.txBytes),
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    modifier = Modifier
                        .weight(1f)
                )
                Text(
                    text = byteToStringRepresentation(bucket.txBytes),
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    color = UploadColor,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                )
                Text(
                    text = byteToStringRepresentation(bucket.rxBytes),
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    color = DownloadColor,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                )
            }
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
    NetworkUsageTheme() {
        BucketDetailsRow(bucket, timeFormatter)
    }
}


@Composable
fun PackageUsageInfoHeader(
    usageInfo: AppUsageInfo,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    ElevatedCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = modifier.then(Modifier.height(60.dp))
    ) {
        AnimatedVisibility(visible = visible) {
            Row(
                verticalAlignment = CenterVertically, modifier = Modifier
                    .padding(8.dp)
            ) {
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
        LaunchedEffect(key1 = true) { visible = true }
    }
}

@Preview(showBackground = true)
@Composable
fun PackageInfoPreview() {
    NetworkUsageTheme() {
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
}
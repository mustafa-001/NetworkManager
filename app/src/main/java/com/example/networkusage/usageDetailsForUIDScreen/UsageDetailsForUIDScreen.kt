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
import com.example.networkusage.usageDetailsProcessor.*
import com.example.networkusage.usagePlots.BarEntryXAxisLabelFormatter
import com.example.networkusage.usagePlots.BarPlotTouchListener
import com.example.networkusage.usagePlots.BarUsagePlot
import com.example.networkusage.usagePlots.CumulativeUsageLinePlot
import com.example.networkusage.utils.toZonedDateTime
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

    val appUsageInfo: AppDetailedUsageInfo by usageDetailsForUIDViewModel.usageByUIDGroupedByTime.observeAsState(
        usageDetailsForUIDViewModel.usageByUIDGroupedByTime.value!!
    )

    val barPlotTouchListener by remember {
        mutableStateOf(BarPlotTouchListener())
    }
    //TODO: Move this logic to selectedInterval. We dont need seperate variable.
    val selectedIntervalIndex by barPlotTouchListener.intervalIndex.observeAsState(
        barPlotTouchListener.intervalIndex.value!!
    )

    //TODO: Move this variable and related logic to barPlotIntervalListViewModel.
    val isNeedGrouping by remember(timeframe.start, timeframe.end) {
        derivedStateOf {
            !timeframe.start.plusDays(7)
                .isAfter(timeframe.end)
        }
    }

    val appsTotalUsageDuringTimeframe: UsageData by remember(appUsageInfo) {
        derivedStateOf {
            UsageData(
                Timeframe(ZonedDateTime.now(), ZonedDateTime.now()),
                appUsageInfo.usageData.map { it.rxBytes }.reduce { acc: Long, it ->
                    acc + it
                },
                appUsageInfo.usageData.map { it.txBytes }.reduce { acc, it ->
                    acc + it
                }
            )

        }
    }

    //Dont recreate this viewmodel on every appUsageInfo change.Just update it.
    val barPlotIntervalListViewModel: BarPlotIntervalListViewModel by remember(
        appUsageInfo,
        timeframe
    ) {
        //Reset selected interval before redrawing this composable.
        barPlotTouchListener.onNothingSelected()
        mutableStateOf(BarPlotIntervalListViewModel(appUsageInfo.usageData, timeframe))
    }

    //Animation function to be set by plotting library.
    var animationCallback: () -> Unit by remember {
        mutableStateOf({ -> })
    }

    val barPlotIntervals by remember(barPlotIntervalListViewModel) {
        //derivedStateOf is used to update barPlotIntervals only when isNeedGrouping changed.
        derivedStateOf {
            if (isNeedGrouping)
                barPlotIntervalListViewModel.groupedByDay()
            else {
                barPlotIntervalListViewModel.intervals
            }
        }
    }
    LaunchedEffect(appUsageInfo) {
        Log.d("Network Usage", "Timeframe changed, calling animationCallback")
        animationCallback()
    }
    val selectedInterval: Optional<Timeframe> =
        if (barPlotIntervals.isEmpty() || selectedIntervalIndex.isPresent.not()) {
            Log.d("Network Usage", "No interval has been selected.")
            Optional.empty<Timeframe>()
        } else {
            try {
                val usageInterval =
                    barPlotIntervals[selectedIntervalIndex.get()]
                Log.d(
                    "Network Usage",
                    "selected interval: ${usageInterval.startSeconds.toZonedDateTime()} -" +
                            " ${usageInterval.endSeconds.toZonedDateTime()}"
                )
                Optional.of(
                    Timeframe(
                        usageInterval.startSeconds.toZonedDateTime(),
                        usageInterval.endSeconds.toZonedDateTime()
                    )
                )
            } catch (e: IndexOutOfBoundsException) {
                Log.d(
                    "Network Usage",
                    "barPlotIntervals changed but selected intervals is still not resetted..\n" +
                            "barPlotIntervals.size: ${barPlotIntervals.size} \n" +
                            "barPlotIntervals start: $barPlotIntervals[0].start \n" +
                            "barPlotIntervals end: $barPlotIntervals[barPlotIntervals.size - 1].end \n" +
                            "selectedIntervalIndex: $selectedIntervalIndex"
                )
                Optional.empty<Timeframe>()
            }
        }

    LazyColumn(
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 8.dp),
    ) {
        item {
            PackageUsageInfoHeader(
                appInfo = appUsageInfo.appInfo,
                usageInfo = appsTotalUsageDuringTimeframe,
                modifier = Modifier.padding(start = 0.dp, end = 0.dp, top = 8.dp, bottom = 4.dp)
            )
        }

        item {
            HorizontalPager(count = 2) { page ->
                if (page == 0) {
                    val xAxisLabelFormatter by remember(barPlotIntervals) {
                        mutableStateOf( BarEntryXAxisLabelFormatter { -> barPlotIntervals })
                    }
                    BarUsagePlot(
                        barPlotIntervals,
                        Optional.of(barPlotTouchListener),
                        xAxisLabelFormatter,
                        Modifier.padding(top = 4.dp, bottom = 4.dp, start = 0.dp, end = 0.dp),
                        { animationCallback = it },
                    )

                } else if (page == 1) {
                    CumulativeUsageLinePlot(barPlotIntervals)
                }
            }
        }
        val timeFormatter = DateTimeFormatter.ofPattern("dd.MM.YY-HH.mm")
        for (bucket in appUsageInfo.usageData.sortedBy { it.rxBytes + it.txBytes }.reversed()) {
            if (selectedInterval.isPresent.not() ||
                (bucket.time.start.isBefore(selectedInterval.get().end)
                        && bucket.time.start.isAfter(selectedInterval.get().start)
                        || bucket.time.end.isBefore(selectedInterval.get().end) &&
                        bucket.time.end.isAfter(selectedInterval.get().start))
            ) {
                item {
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

private fun getAppInfo(
    uid: Int,
    packageManager: PackageManager
) = when (uid) {
    NetworkStats.Bucket.UID_ALL -> {
        AppInfo(
            uid,
            "All",
            "All",
            null
        )
    }
    NetworkStats.Bucket.UID_TETHERING -> {
        AppInfo(
            uid,
            "Tethering",
            "Tethering",
            null
        )
    }
    NetworkStats.Bucket.UID_REMOVED -> {
        AppInfo(
            uid,
            "Removed",
            "Removed",
            null
        )
    }
    else -> {
        val p = packageManager.getPackageInfo(
            packageManager.getPackagesForUid(uid)!![0],
            PackageManager.GET_META_DATA
        )
        AppInfo(
            uid,
            packageManager.getApplicationLabel(p.applicationInfo).toString(),
            p.packageName,
            p.applicationInfo.loadIcon(packageManager)
        )
    }
}

@Composable
fun BucketDetailsRow(
    bucket: UsageData,
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
                val start = bucket.time.start
                val end = bucket.time.end
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
    val bucket = UsageData(
        Timeframe(
            ZonedDateTime.now().minusDays(2),
            ZonedDateTime.now()
        ),
        10000,
        100000,
    )
    val timeFormatter = DateTimeFormatter.ofPattern("dd.MM.YY-HH.mm")
    NetworkUsageTheme() {
        BucketDetailsRow(bucket, timeFormatter)
    }
}


@Composable
fun PackageUsageInfoHeader(
    appInfo: AppInfo,
    usageInfo: UsageData,
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
                if (appInfo.icon == null) {
                    val vector = ImageVector.vectorResource(id = R.drawable.ic_baseline_settings_24)
                    val painter = rememberVectorPainter(image = vector)
                    Icon(painter, "")
                } else {
                    Icon(
                        appInfo.icon!!.toBitmap().asImageBitmap(),
                        "",
                        modifier = Modifier.size(40.dp),
                        tint = Color.Unspecified
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    text = appInfo.name ?: (appInfo.packageName), maxLines = 1
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
            AppInfo(
                100,
                "com.android",
                "Android",
                null
            ),
            usageInfo = UsageData(
                time = Timeframe(
                    ZonedDateTime.now(),
                    ZonedDateTime.now()
                ),
                txBytes = 100000,
                rxBytes = 10000000,
            )
        )
    }
}
package com.example.networkusage

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import com.example.networkusage.ViewModels.BarPlotIntervalListViewModel
import com.example.networkusage.ViewModels.CommonTopbarParametersViewModel
import com.example.networkusage.generalUsageScreen.GeneralUsageScreenViewModel
import com.example.networkusage.ui.theme.DownloadColor
import com.example.networkusage.ui.theme.NetworkUsageTheme
import com.example.networkusage.ui.theme.UploadColor
import com.example.networkusage.usageDetailsProcessor.AppUsageInfo
import com.example.networkusage.usageDetailsProcessor.NetworkType
import com.example.networkusage.usagePlots.BarEntryXAxisLabelFormatter
import com.example.networkusage.usagePlots.BarUsagePlot
import java.time.ZonedDateTime
import java.util.*


@Composable
fun GeneralInfoHeader(
    timeframe: LiveData<Pair<ZonedDateTime, ZonedDateTime>>,
    networkType: NetworkType,
    totalUsage: Pair<Long, Long>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = modifier
            .height(100.dp)
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)

    ) {
        val time: Pair<ZonedDateTime, ZonedDateTime> by timeframe.observeAsState(
            Pair(
                ZonedDateTime.now(), ZonedDateTime.now()
            )
        )
        var visible by remember { mutableStateOf(false) }
        AnimatedVisibility(visible = visible) {

            Column {
                Row(
                    horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        byteToStringRepresentation(totalUsage.first),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        fontSize = 14.sp
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sharp_arrow_downward_24),
                        contentDescription = ""
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_arrow_upward_24),
                        contentDescription = ""
                    )
                    Text(
                        byteToStringRepresentation(totalUsage.second),
                        Modifier.weight(1f),
                        fontSize = 14.sp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        time.first.formatWithReference(
                            ZonedDateTime.now()
                        ),
                        style = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.padding(10.dp, 0.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        time.second.formatWithReference(
                            ZonedDateTime.now()
                        ),
                        style = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.padding(10.dp, 0.dp)
                    )
                }
            }
        }
        LaunchedEffect(true, { visible = true })

    }
}

@Preview(showBackground = true)
@Composable
fun GeneralInfoHeaderPreview() {
    NetworkUsageTheme {
        GeneralInfoHeader(MutableLiveData<Pair<ZonedDateTime, ZonedDateTime>>().apply {
            this.postValue(
                Pair(
                    ZonedDateTime.now().minusDays(1).minusHours(4), ZonedDateTime.now()
                )
            )
        } as LiveData<Pair<ZonedDateTime, ZonedDateTime>>, NetworkType.WIFI, Pair(420000, 23499))
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun GeneralUsageScreen(
    generalUsageScreenViewModel: GeneralUsageScreenViewModel,
    commonTopbarParametersViewModel: CommonTopbarParametersViewModel,
    navController: NavController
) {
    val buckets by generalUsageScreenViewModel.usageByUID.observeAsState(
        generalUsageScreenViewModel.usageByUID.value!!
    )
    val usageTotal: Pair<Long, Long> = buckets.let { it ->
        Pair(it.map { it.rxBytes }.ifEmpty { listOf(0L) }.reduce { acc, rx -> acc + rx },
            it.map { it.txBytes }.ifEmpty { listOf(0L) }.reduce { acc, tx -> acc + tx })
    }
    //Animation function to be set by plotting library.
    var animationCallback: () -> Unit by remember {
        mutableStateOf({ -> })
    }
    LaunchedEffect(buckets) {
        Log.d("Network Usage", "Timeframe changed, calling animationCallback")
        animationCallback()
    }

    val networkType by commonTopbarParametersViewModel.networkType.observeAsState()
    val biggestUsage = if (buckets.isEmpty()) {
        0
    } else {
        buckets[0].rxBytes + buckets[0].txBytes
    }

    val onAppInfoRowClick: (Int) -> Unit = {
        navController.navigate("details/${it}")
    }

    LazyColumn {
        item {
            GeneralInfoHeader(
                timeframe = commonTopbarParametersViewModel.timeFrame,
                networkType = networkType!!,
                totalUsage = usageTotal
            )
        }

        val timeframe = commonTopbarParametersViewModel.timeFrame.value!!
        item {
            val barPlotIntervalListViewModel by remember(timeframe) {
                mutableStateOf(
                    BarPlotIntervalListViewModel(
                        generalUsageScreenViewModel.usageDetailsManager.getUsageGroupedByTime(
                            timeframe, networkType!!
                        ), timeframe
                    )
                )
            }
            val intervals = when {
                timeframe.first.plusDays(7)
                    .isAfter(timeframe.second) -> barPlotIntervalListViewModel.intervals
                else -> barPlotIntervalListViewModel.groupedByDay()

            }
            BarUsagePlot(intervals = intervals,
                Optional.empty(),
                BarEntryXAxisLabelFormatter { -> intervals },
                modifier = Modifier.padding(
                    start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp
                ),
                animationCallbackSetter = { animationCallback = it })
        }

        items(items = buckets, key = { it.packageName }) {
            UsageByPackageRow(usage = it, biggestUsage, onAppInfoRowClick)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ApplicationUsageRowPreview() {
    NetworkUsageTheme {
        UsageByPackageRow(usage = AppUsageInfo(
            100,
            "Some App",
            "com.package.someapp",
            rxBytes = 100003,
            txBytes = 14334,
            icon = null
        ), 2000000, onClick = { _ -> })
    }
}

@Composable
fun UsageByPackageRow(usage: AppUsageInfo, biggestUsage: Long, onClick: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier =
        Modifier
            .clickable(onClick = {
                onClick(usage.uid)
            })
            .padding(10.dp)
    ) {
        Icon(
            when (usage.icon) {
                null -> Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                else -> usage.icon!!.toBitmap()
            }.asImageBitmap(),
            "",
            modifier = Modifier.size(40.dp).padding(4.dp),
            tint = androidx.compose.ui.graphics.Color.Unspecified
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = usage.name ?: (usage.packageName), maxLines = 1
            )
            Row(
                Modifier.fillMaxWidth()
            ) {
                UsageBar(rx = usage.rxBytes, tx = usage.txBytes, biggestUsage)
            }
            Row {
                Text(
                    text = byteToStringRepresentation(usage.rxBytes + usage.txBytes),
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    modifier = Modifier
                        .weight(1f)
                )
                Text(
                    text = byteToStringRepresentation(usage.txBytes),
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    color = UploadColor,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                )
                Text(
                    text = byteToStringRepresentation(usage.rxBytes),
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    color = DownloadColor,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                )
            }
        }
    }
}
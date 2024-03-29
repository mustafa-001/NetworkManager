package com.example.networkusage

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.networkusage.ViewModels.BarPlotState
import com.example.networkusage.ViewModels.CommonTopbarParametersViewModel
import com.example.networkusage.generalUsageScreen.GeneralUsageScreenViewModel
import com.example.networkusage.ui.theme.DownloadColor
import com.example.networkusage.ui.theme.NetworkUsageTheme
import com.example.networkusage.ui.theme.UploadColor
import com.example.networkusage.usageDetailsProcessor.*
import com.example.networkusage.usagePlots.BarEntryXAxisLabelFormatter
import com.example.networkusage.usagePlots.BarUsagePlot
import java.time.ZonedDateTime
import java.util.*


@Composable
fun GeneralInfoHeader(
    networkType: NetworkType,
    totalUsage: UsageData,
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
        val time: Timeframe = totalUsage.time
        var currentVisibility = remember {
            MutableTransitionState(false)
        }
        currentVisibility.targetState = true
        val transition = updateTransition(currentVisibility, label = "Visibility Transition")
        AnimatedVisibility(visible = currentVisibility.currentState) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        byteToStringRepresentation(totalUsage.rxBytes),
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
                        byteToStringRepresentation(totalUsage.txBytes),
                        Modifier.weight(1f),
                        fontSize = 14.sp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        time.start.formatWithReference(
                            ZonedDateTime.now()
                        ),
                        style = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.padding(10.dp, 0.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        time.end.formatWithReference(
                            ZonedDateTime.now()
                        ),
                        style = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.padding(10.dp, 0.dp)
                    )
                }
            }
        }

    }
}

@Preview(showBackground = true)
@Composable
fun GeneralInfoHeaderPreview() {
    NetworkUsageTheme {
        GeneralInfoHeader(
            NetworkType.WIFI,
            UsageData(
                Timeframe(
                    ZonedDateTime.now().minusDays(1).minusHours(4), ZonedDateTime.now()
                ),
                420000,
                23499
            )
        )
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

    //Animation function to be set by plotting library.
    var animationCallback: () -> Unit by remember {
        mutableStateOf({ -> })
    }
    LaunchedEffect(buckets) {
        Log.d("Network Usage", "Timeframe changed, calling animationCallback")
        animationCallback()
    }

    val networkType by commonTopbarParametersViewModel.networkType.observeAsState()
    val timeframe = commonTopbarParametersViewModel.timeFrame.value!!

    val barPlotState by remember(timeframe) {
        mutableStateOf(
            BarPlotState(
                generalUsageScreenViewModel.usageDetailsManager.getUsageGroupedByTime(
                    timeframe, networkType!!
                ), timeframe
            )
        )
    }

    val onAppInfoRowClick: (Int) -> Unit = {
        navController.navigate("details/${it}")
    }

    LazyColumn() {
        item {
            GeneralInfoHeader(
                networkType = networkType!!,
                totalUsage = barPlotState.usageTotal
            )
        }

        val intervals = when {
            timeframe.start.plusDays(2)
                .isAfter(timeframe.end) -> barPlotState.intervals
            else -> barPlotState.groupedByDay()
        }
        item {
            BarUsagePlot(intervals = intervals,
                Optional.empty(),
                BarEntryXAxisLabelFormatter { -> intervals },
                modifier = Modifier.padding(
                    start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp
                ),
                animationCallbackSetter = { animationCallback = it })
        }
        items(buckets) {
            UsageByPackageRow(usage = it, barPlotState.biggestUsage, onAppInfoRowClick)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ApplicationUsageRowPreview() {
    NetworkUsageTheme {
        UsageByPackageRow(usage = AppUsageInfo(
            AppInfo(
                100,
                "Some App",
                "com.package.someapp",
                icon = null
            ),
            rxBytes = 100003,
            txBytes = 14334,
        ), 2000000, onClick = { _ -> })
    }
}

@Composable
fun UsageByPackageRow(usage: AppUsageInfo, biggestUsage: Long, onClick: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
        Modifier
            .clickable(onClick = {
                onClick(usage.appInfo.uid)
            })
            .padding(10.dp)
    ) {
        Icon(
            usage.appInfo.iconBitmap!!,
            "",
            modifier = Modifier
                .size(40.dp)
                .padding(4.dp),
            tint = androidx.compose.ui.graphics.Color.Unspecified
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = usage.appInfo.name ?: (usage.appInfo.packageName), maxLines = 1
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
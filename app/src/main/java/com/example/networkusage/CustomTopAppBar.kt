package com.example.networkusage

import android.content.Intent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.networkusage.usageDetailsProcessor.NetworkType
import com.example.networkusage.usageDetailsProcessor.Timeframe
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    activity: MainActivity,
    timeFrameMode: TimeFrameMode,
    onSelectTimeFrameMode: (TimeFrameMode) -> Unit,
    timeframe: Timeframe,
    onChangeTimeFrame: (time: Timeframe) -> Unit,
    networkType: NetworkType,
    onClickNetworkType: () -> Unit,
    useTestData: Boolean,
    onChangeUseTestData: (Boolean) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    TopAppBar(title = { Text("Network Usage") }, colors = TopAppBarDefaults.smallTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onPrimary,
        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
        scrolledContainerColor = MaterialTheme.colorScheme.primary
    ), scrollBehavior = scrollBehavior, actions = {
        var showTimeFrameDropdownMenu by remember { mutableStateOf(false) }
        var showCustomSelectTimeFrame by remember { mutableStateOf(false) }
        var showOverflowMenu by remember { mutableStateOf(false) }

        IconButton(
            onClick = onClickNetworkType
        ) {
            if (networkType == NetworkType.WIFI) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_wifi_24),
                    contentDescription = "Wifi"
                )
            } else {
                Icon(
                    painter = painterResource(
                        id = R.drawable.baseline_signal_cellular_alt_24
                    ), contentDescription = "Cellular"
                )
            }
        }
        IconButton(onClick = {
            showTimeFrameDropdownMenu = !showTimeFrameDropdownMenu
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_access_time_24),
                contentDescription = "Select Time"
            )
        }
        if (showCustomSelectTimeFrame) {
            TimeFrameSelector(activity = activity,
                timeframe,
                mode = timeFrameMode,
                onDismissRequest = { showCustomSelectTimeFrame = false },
                onSubmitRequest = { time ->
                    onChangeTimeFrame(time)
                })
        }
        IconButton(onClick = {
            showOverflowMenu = !showOverflowMenu
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_more_vert_24),
                contentDescription = "Overflow menu"
            )
        }

        TopBarTimeFrameModeSelectionMenu(showTimeFrameModeDropdownMenu = showTimeFrameDropdownMenu,
            onSelectTimeFrameMode = onSelectTimeFrameMode,
            onChangeShowTimeFrameDropdownMenu = { showTimeFrameDropdownMenu = it },
            onChangeCustomSelectTimeFrameDropdownMenu = { showCustomSelectTimeFrame = it })

        TopBarOverflowMenu(showOverflowMenu = showOverflowMenu,
            onChangeShowOverflowMenu = { showOverflowMenu = it },
            useTestData = useTestData,
            onChangeTestData = {
                onChangeUseTestData(it)
                val intent = Intent(activity.applicationContext, MainActivity::class.java)
                activity.applicationContext.startActivity(intent)
            })
    })
}

@Composable
private fun TopBarTimeFrameModeSelectionMenu(
    showTimeFrameModeDropdownMenu: Boolean,
    onSelectTimeFrameMode: (TimeFrameMode) -> Unit,
    onChangeShowTimeFrameDropdownMenu: (Boolean) -> Unit,
    onChangeCustomSelectTimeFrameDropdownMenu: (Boolean) -> Unit

) {
    DropdownMenu(expanded = showTimeFrameModeDropdownMenu,
        onDismissRequest = { onChangeShowTimeFrameDropdownMenu(false) }) {
        DropdownMenuItem(onClick = {
            onSelectTimeFrameMode(TimeFrameMode.TODAY)
            onChangeShowTimeFrameDropdownMenu(false)
        }, text = {
            Text("Today")
        })
        DropdownMenuItem(onClick = {
            onSelectTimeFrameMode(TimeFrameMode.LAST_WEEK)
            onChangeShowTimeFrameDropdownMenu(false)
        }, text = {
            Text("Last Week")
        })
        DropdownMenuItem(onClick = {
            onSelectTimeFrameMode(TimeFrameMode.LAST_30_DAYS)
            onChangeShowTimeFrameDropdownMenu(false)
        }, text = {
            Text("Last 30 Days")
        })
        DropdownMenuItem(onClick = {
            onSelectTimeFrameMode(TimeFrameMode.THIS_MONTH)
            onChangeShowTimeFrameDropdownMenu(false)
        }, text = {
            Text("This Month")
        })
        DropdownMenuItem(onClick = {
            onChangeCustomSelectTimeFrameDropdownMenu(true)
            onChangeShowTimeFrameDropdownMenu(false)
        }, text = {
            Text("Custom")
        })

    }
}

@Composable
private fun TopBarOverflowMenu(
    showOverflowMenu: Boolean,
    onChangeShowOverflowMenu: (Boolean) -> Unit,
    useTestData: Boolean,
    onChangeTestData: (Boolean) -> Unit
) {
    DropdownMenu(expanded = showOverflowMenu,
        onDismissRequest = { onChangeShowOverflowMenu(false) }) {
        DropdownMenuItem(onClick = {
            onChangeTestData(!useTestData)
            onChangeShowOverflowMenu(false)

        }, text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(0.dp)
            ) {
                Checkbox(checked = useTestData, onCheckedChange = { checked ->
                    onChangeTestData(checked)
                    onChangeShowOverflowMenu(false)
                })
                Text("Use Test Data")
            }
        })
    }
}



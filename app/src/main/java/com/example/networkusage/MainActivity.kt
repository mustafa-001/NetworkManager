package com.example.networkusage

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.networkusage.ViewModels.UsageListViewModel
import com.example.networkusage.ViewModels.UsagePlotViewModel
import com.example.networkusage.ui.theme.NetworkUsageTheme
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime


enum class TimeFrameMode {
    TODAY, LAST_WEEK, LAST_30_DAYS, THIS_MONTH, CUSTOM
}

class MainActivity : ComponentActivity() {
    private lateinit var usageListViewModel: UsageListViewModel
    private lateinit var navController: NavController
    private lateinit var sharedPref: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val usageDetailsManager = UsageDetailsManager(
            packageManager = packageManager,
            networkStatsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        )

        sharedPref = application.applicationContext.getSharedPreferences(
            (application.applicationContext.getString(R.string.prefence_file_key)),
            Context.MODE_PRIVATE
        )
//
//        val requestPermissionLauncher =
//            registerForActivityResult(
//                ActivityResultContracts.RequestPermission()
//            ) { isGranted: Boolean ->
//                if (isGranted) {
//                    // Permission is granted. Continue the action or workflow in your
//                    // app.
//                    Log.d("perm", "permission is granted")
//                } else {
//                    Log.d("perm", "permission is not granted")
//                    // Explain to the user that the feature is unavailable because the
//                    // features requires a permission that the user has denied. At the
//                    // same time, respect the user's decision. Don't link to system
//                    // settings in an effort to convince the user to change their
//                    // decision.
//                }
//            }
//        requestPermissionLauncher.launch(Settings.ACTION_DATA_USAGE_SETTINGS)

//        enforceCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS, null)
        val appOPs = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        if (appOPs.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName
            ) != AppOpsManager.MODE_ALLOWED
        ) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        usageListViewModel = UsageListViewModel(
            usageDetailsManager
        )
        usageListViewModel.networkType = UsageDetailsManager.NetworkType.valueOf(
            sharedPref.getString(
                "networkType",
                UsageDetailsManager.NetworkType.GSM.name
            )!!
        )
        val timeFrameMode = TimeFrameMode.valueOf(sharedPref.getString("mode", "CUSTOM")!!)
        if (timeFrameMode != TimeFrameMode.CUSTOM) {
            usageListViewModel.selectPredefinedTimeFrame(timeFrameMode)
        } else {
            usageListViewModel.setTime(
                Pair(
                    Instant.ofEpochSecond(
                        sharedPref.getLong(
                            "start_time",
                            Instant.now().minusSeconds(60 * 60 * 24 * 7).epochSecond
                        )
                    ).atZone(ZoneId.systemDefault()),
                    Instant.ofEpochSecond(
                        sharedPref.getLong(
                            "start_time",
                            Instant.now().epochSecond
                        )
                    ).atZone(ZoneId.systemDefault()),
                )
            )
        }

        setContent {
            Scaffold(
                topBar = {
                    TopAppBar() {
                        Text("Network Stats")
                        IconButton(onClick = {
                            if (usageListViewModel.networkType == UsageDetailsManager.NetworkType.WIFI) {
                                usageListViewModel.networkType =
                                    UsageDetailsManager.NetworkType.GSM
                            } else {
                                usageListViewModel.networkType =
                                    UsageDetailsManager.NetworkType.WIFI
                            }
                            with(
                                this@MainActivity.getSharedPreferences(
                                    this@MainActivity.getString(R.string.prefence_file_key),
                                    Context.MODE_PRIVATE
                                ).edit()
                            ) {
                                putString("networkType", usageListViewModel.networkType.name)
                                apply()
                            }
                        }) {
                            if (usageListViewModel.networkType ==
                                UsageDetailsManager.NetworkType.WIFI
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_baseline_wifi_24),
                                    contentDescription = "Wifi"
                                )
                            } else {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_twotone_signal_cellular_alt_24
                                    ),
                                    contentDescription = "Cellular"
                                )
                            }
                        }

                        var showSelectTimeFrame by remember { mutableStateOf(false) }
                        var dropdownExpanded by remember { mutableStateOf(false) }

                        IconButton(onClick = {
                            dropdownExpanded = !dropdownExpanded
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_access_time_24),
                                contentDescription = "Select Time"
                            )
                        }
                        if (showSelectTimeFrame) {
                            TimeFrameSelector(
                                activity = this@MainActivity,
                                usageListViewModel,
                                mode = timeFrameMode,
                                onDismissRequest = { showSelectTimeFrame = false },
                                onSubmitRequest = { time ->
                                    usageListViewModel.setTime(time)
                                    with(
                                        this@MainActivity.getSharedPreferences(
                                            this@MainActivity.getString(R.string.prefence_file_key),
                                            Context.MODE_PRIVATE
                                        ).edit()
                                    ) {
                                        putLong(
                                            "start_time",
                                            time.first.toEpochSecond()
                                        )
                                        putLong(
                                            "end_time",
                                            time.second.toEpochSecond()
                                        )
                                        apply()
                                    }
                                })
                        }


                        DropdownMenu(
                            expanded = dropdownExpanded,
                            offset = DpOffset(80.dp, 0.dp),
                            onDismissRequest = { dropdownExpanded = false }) {
                            DropdownMenuItem(onClick = {
                                onSelectTimeFrameMode(TimeFrameMode.TODAY)
                                dropdownExpanded = false
                            }) {
                                Text("Today")
                            }
                            DropdownMenuItem(onClick = {
                                onSelectTimeFrameMode(TimeFrameMode.LAST_WEEK)
                                dropdownExpanded = false
                            }) {
                                Text("Last Week")
                            }
                            DropdownMenuItem(onClick = {
                                onSelectTimeFrameMode(TimeFrameMode.LAST_30_DAYS)
                                dropdownExpanded = false
                            }) {
                                Text("Last 30 Days")
                            }
                            DropdownMenuItem(onClick = {
                                onSelectTimeFrameMode(TimeFrameMode.THIS_MONTH)
                                dropdownExpanded = false
                            }) {
                                Text("This Month")
                            }
                            DropdownMenuItem(onClick = {
                                showSelectTimeFrame = true
                                dropdownExpanded = false
                                onSelectTimeFrameMode(TimeFrameMode.CUSTOM)
                            }) {
                                Text("Custom")
                            }

                        }
                    }
                },

                content = {
                    NetworkUsageTheme {
                        // A surface container using the 'background' color from the theme
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colors.background
                        ) {

                            navController = rememberNavController()
                            NavHost(
                                navController = navController as NavHostController,
                                startDestination = "overview"
                            ) {
                                composable("overview") { NetworkActivityOverviewControls() }
                                composable("selectTimeframe") {
                                    TimeFrameSelector(
                                        activity = this@MainActivity,
                                        usageListViewModel,
                                        mode = TimeFrameMode.LAST_30_DAYS,
                                        {}, { _ -> })
                                }
                                composable(
                                    "details/{bucket}",
                                    arguments = listOf(navArgument("bucket") {
                                        type = NavType.IntType
                                    })
                                ) { navBackStackEntry ->
                                    UsageDetailsForUID(
                                        usageDetailsManager,
                                        packageManager,
                                        navBackStackEntry.arguments?.getInt("bucket")!!,
                                        usageListViewModel.timeFrame
                                    )
                                }
                            }

                        }
                    }
                }
            )
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun PreviewTimeFrameSelector() {
        NetworkUsageTheme {
            TimeFrameSelector(
                activity = LocalContext.current as ComponentActivity,
                usageListViewModel,
                TimeFrameMode.LAST_30_DAYS,
                {},
                { _ -> })
        }
    }

    @Composable
    fun GeneralInfoHeader(
        timeframe: LiveData<Pair<ZonedDateTime, ZonedDateTime>>,
        networkType: UsageDetailsManager.NetworkType,
        totalUsage: Pair<Long, Long>
    ) {
        Card() {
            val time: Pair<ZonedDateTime, ZonedDateTime> by timeframe.observeAsState(
                Pair(
                    ZonedDateTime.now(), ZonedDateTime.now()
                )
            )
            Column {
                Row(horizontalArrangement = Arrangement.Center) {
                    Text(
                        byteToStringRepresentation(totalUsage.first),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sharp_arrow_downward_24),
                        contentDescription = ""
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_arrow_upward_24),
                        contentDescription = ""
                    )
                    Text(byteToStringRepresentation(totalUsage.second), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.SpaceAround) {
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
    }

    @Preview(showBackground = true)
    @Composable
    fun GeneralInfoHeaderPreview() {
        GeneralInfoHeader(
            timeframe = MutableLiveData<Pair<ZonedDateTime, ZonedDateTime>>().apply {
                this.postValue(
                    Pair(
                        ZonedDateTime.now().minusDays(1).minusHours(4),
                        ZonedDateTime.now()
                    )
                )
            } as LiveData<Pair<ZonedDateTime, ZonedDateTime>>,
            networkType = UsageDetailsManager.NetworkType.WIFI,
            Pair(420000, 23499)
        )
    }

    @Composable
    fun NetworkActivityOverviewControls() {
        NetworkActivityForAppsList(usageListViewModel)
    }


    @Composable
    fun NetworkActivityForAppsList(viewModel: UsageListViewModel) {
        val buckets = viewModel.usageByUID.observeAsState()
        val usageTotal: Pair<Long, Long> =
            buckets.value?.let { it ->
                Pair(it.map { it.rxBytes }.ifEmpty { listOf(0L) }.reduce { acc, rx -> acc + rx },
                    it.map { it.txBytes }.ifEmpty { listOf(0L) }.reduce { acc, tx -> acc + tx })
            } ?: Pair(0, 0)
        buckets.value?.let {
            LazyColumn {
                this.item {
                    GeneralInfoHeader(
                        timeframe = viewModel.timeFrame,
                        networkType = viewModel.networkType,
                        totalUsage = usageTotal
                    )
                }
                this.item {
                    BasicPlot(
                        intervals = UsagePlotViewModel(
                            viewModel.usageDetailsManager.getUsageByTime(
                                viewModel.timeFrame.value!!,
                                viewModel.networkType
                            ), viewModel.timeFrame.value!!
                        ).intervals
                    )
                }
                this.items(it) { b ->
                    ApplicationUsageRow(usage = b, it[0].rxBytes + it[0].txBytes)
                }
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun ApplicationUsageRowPreview() {
        NetworkUsageTheme {
            ApplicationUsageRow(
                usage = UsageDetailsManager.AppUsageInfo(
                    100,
                    "Some App",
                    "com.package.someapp",
                    rxBytes = 100003,
                    txBytes = 14334,
                    icon = null
                ), 2000000
            )
        }
    }

    @Composable
    fun ApplicationUsageRow(usage: UsageDetailsManager.AppUsageInfo, biggestUsage: Long) {
        Row(
            Modifier
                .clickable(onClick = {
                    val uid = usage.uid
                    navController.navigate("details/$uid")
                })
                .padding(10.dp)
        ) {
            Icon(
                usage.icon!!.toBitmap().asImageBitmap(),
                "",
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = usage.name ?: (usage.packageName), maxLines = 1
                )
                Row(
                    Modifier
                        .fillMaxWidth()) {
                    UsageBar(rx = usage.rxBytes, tx = usage.txBytes, biggestUsage)
                }
                Row {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Tx: ${byteToStringRepresentation(usage.txBytes)}"
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Rx: ${byteToStringRepresentation(usage.rxBytes)}"
                    )
                }
            }
        }

    }

    private fun onSelectTimeFrameMode(mode: TimeFrameMode) {
        if (mode != TimeFrameMode.CUSTOM) {
            usageListViewModel.selectPredefinedTimeFrame(mode)
        }
        with(
            this@MainActivity.getSharedPreferences(
                this@MainActivity.getString(R.string.prefence_file_key),
                Context.MODE_PRIVATE
            ).edit()
        ) {
            putString("mode", mode.name)
            apply()
        }
    }

    @Composable
    fun UsageBucketRow(bucket: NetworkStats.Bucket) {
        Row(
            Modifier
                .clickable(onClick = {
                    val uid = bucket.uid
                    navController.navigate("details/$uid")
                })
                .padding(10.dp)
        ) {
            Column {
                val formatter = SimpleDateFormat.getDateTimeInstance()
                Text(text = "Start: ${formatter.format(bucket.startTimeStamp)} \t")
                Text(text = "End: ${formatter.format(bucket.endTimeStamp)} \t")
            }
            Column {
                Text(text = "Tx: ${byteToStringRepresentation(bucket.txBytes)}")
                Text(text = "Rx: ${byteToStringRepresentation(bucket.rxBytes)}")
            }
            Column {
                Text(text = bucket.tag.toString())
                Text(text = bucket.uid.toString())
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        NetworkUsageTheme {
            UsageBucketRow(NetworkStats.Bucket())
        }
    }
}


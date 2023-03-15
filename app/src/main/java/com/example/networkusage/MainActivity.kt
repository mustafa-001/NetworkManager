package com.example.networkusage

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.networkusage.ViewModels.BarPlotIntervalListViewModel
import com.example.networkusage.ViewModels.CommonTopbarParametersViewModel
import com.example.networkusage.ViewModels.GeneralUsageScreenViewModel
import com.example.networkusage.ui.theme.NetworkUsageTheme
import com.example.networkusage.usage_details_processor.NetworkType
import com.example.networkusage.usage_details_processor.UsageDetailsProcessor
import com.example.networkusage.usage_details_processor.UsageDetailsProcessorInterface
import com.example.networkusage.usage_details_processor.UsageDetailsProcessorWithTestData
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.*
import com.example.networkusage.usage_details_processor.AppUsageInfo as AppUsageInfo1


enum class TimeFrameMode {
    TODAY, LAST_WEEK, LAST_30_DAYS, THIS_MONTH, CUSTOM
}

class MainActivity : ComponentActivity() {
    private lateinit var generalUsageScreenViewModel: GeneralUsageScreenViewModel
    private lateinit var commonTopbarParametersViewModel: CommonTopbarParametersViewModel
    private lateinit var navController: NavController

    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = application.applicationContext.getSharedPreferences(
            (application.applicationContext.getString(R.string.prefence_file_key)),
            Context.MODE_PRIVATE
        )
        commonTopbarParametersViewModel = CommonTopbarParametersViewModel(sharedPref)


        val usageDetailsManager: UsageDetailsProcessorInterface =
            //branch for testing on commonTopbarParametersViewModel.useTestData
            if (commonTopbarParametersViewModel.useTestData) {
                UsageDetailsProcessorWithTestData(packageManager = packageManager)
            } else {
                UsageDetailsProcessor(
                    packageManager = packageManager,
                    networkStatsManager = getSystemService(NETWORK_STATS_SERVICE) as NetworkStatsManager
                )
            }
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

        val timeFrameMode = commonTopbarParametersViewModel.timeFrameMode

        generalUsageScreenViewModel = GeneralUsageScreenViewModel(
            commonTopbarParametersViewModel,
            usageDetailsManager
        )

        setContent {
            navController = rememberNavController()
            val timeframe by commonTopbarParametersViewModel.timeFrame.observeAsState(
                Pair(
                    ZonedDateTime.now(),
                    ZonedDateTime.now()
                )
            )
            val networkType by commonTopbarParametersViewModel.networkType.observeAsState(
                NetworkType.GSM
            )
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            NetworkUsageTheme() {
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        CustomTopAppBar(
                            activity = this,
                            timeFrameMode = timeFrameMode,
                            onSelectTimeFrameMode = {
                                commonTopbarParametersViewModel.selectPredefinedTimeFrame(
                                    it
                                )
                            },
                            timeFrame = timeframe,
                            onChangeTimeFrame = {
                                commonTopbarParametersViewModel.setTime(it)
                            },
                            networkType = networkType,
                            onClickNetworkType = { commonTopbarParametersViewModel.toggleNetworkType() },
                            useTestData = commonTopbarParametersViewModel.useTestData,
                            onChangeUseTestData = {
                                commonTopbarParametersViewModel.useTestData = it
                            },
                            scrollBehavior = scrollBehavior
                        )
                    }
                ) { innerPadding ->
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {

                        navController = rememberNavController()
                        NavHost(
                            navController = navController as NavHostController,
                            startDestination = "overview"
                        ) {
                            composable("overview") { NetworkActivityOverviewControls() }
                            composable(
                                "details/{bucket}",
                                arguments = listOf(navArgument("bucket") {
                                    type = NavType.IntType
                                })
                            ) { navBackStackEntry ->
                                UsageDetailsForPackage(
                                    commonTopbarParametersViewModel,
                                    packageManager,
                                    navBackStackEntry.arguments?.getInt("bucket")!!,
                                    usageDetailsManager
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun GeneralInfoHeader(
        timeframe: LiveData<Pair<ZonedDateTime, ZonedDateTime>>,
        networkType: NetworkType,
        totalUsage: Pair<Long, Long>
    ) {
        ElevatedCard(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier
                .height(100.dp)
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)

        ) {
            val time: Pair<ZonedDateTime, ZonedDateTime> by timeframe.observeAsState(
                Pair(
                    ZonedDateTime.now(), ZonedDateTime.now()
                )
            )
            Column {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(8.dp)
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
                        byteToStringRepresentation(totalUsage.second), Modifier.weight(1f),
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
    }

    @Preview(showBackground = true)
    @Composable
    fun GeneralInfoHeaderPreview() {
        NetworkUsageTheme {
            GeneralInfoHeader(
                MutableLiveData<Pair<ZonedDateTime, ZonedDateTime>>().apply {
                    this.postValue(
                        Pair(
                            ZonedDateTime.now().minusDays(1).minusHours(4),
                            ZonedDateTime.now()
                        )
                    )
                } as LiveData<Pair<ZonedDateTime, ZonedDateTime>>,
                NetworkType.WIFI,
                Pair(420000, 23499)
            )
        }
    }

    @Composable
    fun NetworkActivityOverviewControls() {
        Log.d("Network Usage", "Composing main screen.")
        NetworkActivityForAppsList(generalUsageScreenViewModel)
    }


    @Composable
    fun NetworkActivityForAppsList(viewModel: GeneralUsageScreenViewModel) {
        val buckets by viewModel.usageByUID.observeAsState(
            viewModel.usageByUID.value!!
        )
        val usageTotal: Pair<Long, Long> =
            buckets.let { it ->
                Pair(it.map { it.rxBytes }
                    .ifEmpty { listOf(0L) }
                    .reduce { acc, rx -> acc + rx },
                    it.map { it.txBytes }
                        .ifEmpty { listOf(0L) }
                        .reduce { acc, tx -> acc + tx })
            } ?: Pair(0, 0)
        val networkType by commonTopbarParametersViewModel.networkType.observeAsState()
        //Animation function to be set by plotting library.
        var animationCallback: () -> Unit by remember {
            mutableStateOf({ -> })
        }
        LaunchedEffect(buckets) {
            Log.d("Network Usage", "Timeframe changed, calling animationCallback")
            animationCallback()
        }
        buckets.let {
            val biggestUsage =
                if (it.isEmpty()) {
                    0
                } else {
                    it[0].rxBytes + it[0].txBytes
                }

            LazyColumn {
                this.item {
                    GeneralInfoHeader(
                        timeframe = commonTopbarParametersViewModel.timeFrame,
                        networkType = networkType!!,
                        totalUsage = usageTotal
                    )
                }
                val timeframe = commonTopbarParametersViewModel.timeFrame.value!!
                this.item {
                    val barPlotIntervalListViewModel by remember(timeframe) {
                        mutableStateOf(
                            BarPlotIntervalListViewModel(
                                viewModel.usageDetailsManager.getUsageGroupedByTime(
                                    timeframe,
                                    networkType!!
                                ), timeframe
                            )
                        )
                    }
                    val intervals = when {
                        timeframe.first.plusDays(7)
                            .isAfter(timeframe.second) -> barPlotIntervalListViewModel.intervals
                        else -> barPlotIntervalListViewModel.groupedByDay()

                    }
                    BarUsagePlot(
                        intervals = intervals,
                        Optional.empty(),
                        BarEntryXAxisLabelFormatter { -> intervals },
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
                        animationCallbackSetter = { animationCallback = it }
                    )
                }
                this.items(it) { b ->
                    UsageByPackageRow(usage = b, biggestUsage)
                }
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun ApplicationUsageRowPreview() {
        NetworkUsageTheme {
            UsageByPackageRow(
                usage = AppUsageInfo1(
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
    fun UsageByPackageRow(usage: AppUsageInfo1, biggestUsage: Long) {
        Row(
            Modifier
                .clickable(onClick = {
                    navController.navigate("details/${usage.uid}")
                })
                .padding(10.dp)
        ) {
            Icon(
                when (usage.icon) {
                    null -> Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    else -> usage.icon!!.toBitmap()
                }.asImageBitmap(),
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
                        .fillMaxWidth()
                ) {
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


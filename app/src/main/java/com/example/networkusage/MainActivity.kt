package com.example.networkusage

import android.app.AppOpsManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.networkusage.ViewModels.CommonTopbarParametersViewModel
import com.example.networkusage.generalUsageScreen.GeneralUsageScreenViewModel
import com.example.networkusage.ui.theme.NetworkUsageTheme
import com.example.networkusage.usageDetailsProcessor.*
import java.time.ZonedDateTime
import java.util.*


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
                Timeframe(
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
                            timeframe = timeframe,
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
                            composable("overview") { GeneralUsageScreen(
                                generalUsageScreenViewModel = generalUsageScreenViewModel,
                                commonTopbarParametersViewModel = commonTopbarParametersViewModel,
                                navController = navController
                            ) }
                            composable(
                                "details/{bucket}",
                                arguments = listOf(navArgument("bucket") {
                                    type = NavType.IntType
                                })
                            ) { navBackStackEntry ->
                                UsageDetailsForUIDScreen(
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
}


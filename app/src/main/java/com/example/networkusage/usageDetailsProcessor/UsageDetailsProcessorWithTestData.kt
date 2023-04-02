package com.example.networkusage.usageDetailsProcessor

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import java.time.ZonedDateTime
import kotlin.random.Random

class UsageDetailsProcessorWithTestData(
    private val packageManager: PackageManager,
) : UsageDetailsProcessorInterface {

    private val packages: List<PackageInfo> =
        packageManager.getInstalledPackages(PackageManager.GET_META_DATA)

    override fun getUsageByUIDGroupedByTime(
        uid: Int,
        timeFrame: Timeframe
    ): AppDetailedUsageInfo {

        val random = Random(1)
        Log.d(
            "NetworkUsage",
            "Generating random data for uid $uid, timeFrame: $timeFrame, random seed: 1"
        )
        var next2HourIntervalStart = timeFrame.start
        val r = mutableListOf<UsageData>()
        while (next2HourIntervalStart.isBefore(timeFrame.end)) {
            r.add(
                UsageData(
                    Timeframe(
                        next2HourIntervalStart,
                        when (next2HourIntervalStart.plusHours(3).isAfter(ZonedDateTime.now())) {
                            false -> next2HourIntervalStart.plusHours(2)
                            true -> ZonedDateTime.now()
                        },
                    ),
                    random.nextLong(512) * 1024,
                    random.nextLong(512) * 1024,
                )
            )
            next2HourIntervalStart = next2HourIntervalStart.plusHours(2)

        }
        val androidPackage = packages[random.nextInt(packages.size)]
        return AppDetailedUsageInfo(
            AppInfo(
                androidPackage.applicationInfo.uid,
                androidPackage.packageName,
                androidPackage.packageName,
                androidPackage.applicationInfo.loadIcon(packageManager)
            ),
            r
        )
    }

    override fun getUsageGroupedByTime(
        timeFrame: Timeframe,
        networkType: NetworkType
    ): List<UsageData> {
        val random = Random(2)
        Log.d("NetworkUsage", "Generating random data for timeFrame: $timeFrame, random seed: 2")
        var next2HourIntervalStart = timeFrame.start
        val r = mutableListOf<UsageData>()
        while (next2HourIntervalStart.isBefore(timeFrame.end)) {
            r.add(
                UsageData(
                    Timeframe(
                        next2HourIntervalStart,
                        when (next2HourIntervalStart.plusHours(3).isAfter(ZonedDateTime.now())) {
                            false -> next2HourIntervalStart.plusHours(2)
                            true -> ZonedDateTime.now()
                        },
                    ),
                    random.nextLong(1024) * 1024,
                    random.nextLong(1024) * 1024,
                )
            )
            next2HourIntervalStart = next2HourIntervalStart.plusHours(2)

        }
        return r
    }


    override fun getUsageGroupedByUID(
        timeFrame: Timeframe,
        networkType: NetworkType
    ): List<AppUsageInfo> {
        val random = Random(3)
        val r = mutableListOf<AppUsageInfo>()
        for (i in 0..10) {
            val androidPackage = packages[random.nextInt(packages.size)]
            r.add(
                AppUsageInfo(
                    AppInfo(
                        androidPackage.applicationInfo.uid,
                        androidPackage.packageName,
                        androidPackage.packageName,
                        packages[i].applicationInfo.loadIcon(packageManager)
                    ),
                    random.nextLong(1024 * 1024) * 100,
                    random.nextLong(1024 * 1024) * 10,
                )
            )
        }
        return r
    }

}
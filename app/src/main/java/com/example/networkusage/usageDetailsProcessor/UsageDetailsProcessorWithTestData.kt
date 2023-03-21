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
        timeFrame: Pair<ZonedDateTime, ZonedDateTime>
    ): List<GeneralUsageInfo> {

        val random = Random(1)
        Log.d("NetworkUsage", "Generating random data for uid $uid, timeFrame: $timeFrame, random seed: 1")
        var next2HourIntervalStart = timeFrame.first
        val r = mutableListOf<GeneralUsageInfo>()
        while (next2HourIntervalStart.isBefore(timeFrame.second)) {
            r.add(
                GeneralUsageInfo(
                    random.nextLong(512) * 1024,
                    random.nextLong(512) * 1024,
                    next2HourIntervalStart.toInstant().toEpochMilli(),
                    when (next2HourIntervalStart.plusHours(3).isAfter(ZonedDateTime.now())){
                        false -> next2HourIntervalStart.plusHours(2).toInstant().toEpochMilli()
                        true -> ZonedDateTime.now().toInstant().toEpochMilli()
                    }
                )
            )
            next2HourIntervalStart = next2HourIntervalStart.plusHours(2)

        }
        return r
    }

    override fun getUsageGroupedByTime(
        timeFrame: Pair<ZonedDateTime, ZonedDateTime>,
        networkType: NetworkType
    ): List<GeneralUsageInfo> {
        val random = Random(2)
        Log.d("NetworkUsage", "Generating random data for timeFrame: $timeFrame, random seed: 2")
        var next2HourIntervalStart = timeFrame.first
        val r = mutableListOf<GeneralUsageInfo>()
        while (next2HourIntervalStart.isBefore(timeFrame.second)) {
            r.add(
                GeneralUsageInfo(
                    random.nextLong(1024 ) * 1024,
                    random.nextLong(1024 ) * 1024,
                    next2HourIntervalStart.toInstant().toEpochMilli(),
                    when (next2HourIntervalStart.plusHours(3).isAfter(ZonedDateTime.now())){
                        false -> next2HourIntervalStart.plusHours(2).toInstant().toEpochMilli()
                        true -> ZonedDateTime.now().toInstant().toEpochMilli()
                    }
                )
            )
            next2HourIntervalStart = next2HourIntervalStart.plusHours(2)

        }
        return r
    }


    override fun getUsageGroupedByUID(
        timeFrame: Pair<ZonedDateTime, ZonedDateTime>,
        networkType: NetworkType
    ): List<AppUsageInfo> {
        val random = Random(3)
        val r = mutableListOf<AppUsageInfo>()
        for (i in 0..10) {
            val androidPackage = packages[random.nextInt(packages.size)]
            r.add(
                AppUsageInfo(
                    androidPackage.applicationInfo.uid,
                    androidPackage.packageName,
                    androidPackage.packageName,
                    random.nextLong(1024 * 1024) * 100,
                    random.nextLong(1024 * 1024) * 10,
                    packages[i].applicationInfo.loadIcon(packageManager)
                )
            )
        }
        return r
    }

}
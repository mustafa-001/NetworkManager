package com.example.networkusage.usageDetailsProcessor

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class UsageDetailsProcessor(
    private val packageManager: PackageManager,
    private val networkStatsManager: NetworkStatsManager
) : UsageDetailsProcessorInterface {

    private val packages: List<PackageInfo> =
        packageManager.getInstalledPackages(PackageManager.GET_META_DATA)


    override fun getUsageByUIDGroupedByTime(
        uid: Int,
        timeFrame: Timeframe
    ): AppDetailedUsageInfo {
        Log.d("Network Usage", "timeFrame set to: ${timeFrame.start} and ${timeFrame.end}")
        val usage = runBlocking {
            networkStatsManager.queryDetailsForUid(
                ConnectivityManager.TYPE_MOBILE,
                null,
                timeFrame.start.minusHours(2).toEpochSecond() * 1000,
                timeFrame.end.plusHours(2).toEpochSecond() * 1000,
                uid
            )
        }
        val r = mutableListOf<UsageData>()
        while (usage.hasNextBucket()) {
            val b = NetworkStats.Bucket()
            usage.getNextBucket(b)
            r.add(
                UsageData(
                    rxBytes = b.rxBytes,
                    txBytes = b.txBytes,
                    time = Timeframe(
                        start = ZonedDateTime.ofInstant(Instant.ofEpochMilli(b.startTimeStamp), ZoneId.systemDefault()),
                        end = ZonedDateTime.ofInstant(Instant.ofEpochMilli(b.endTimeStamp), ZoneId.systemDefault())
                    )
                )
            )
            Log.d("netUsage", b.txBytes.toString())
            Log.d("netUsage", b.rxBytes.toString())
        }
        val p = packages.find { it.applicationInfo.uid == uid }!!
        return AppDetailedUsageInfo(
            appInfo = AppInfo(
                uid = uid,
                name = packageManager.getApplicationLabel(p.applicationInfo).toString(),
                packageName = p.packageName,
                icon = p.applicationInfo.loadIcon(packageManager),
            ),
            usageData = r
        )
    }

    private fun calculateBuckets(timeFrame: Pair<ZonedDateTime, ZonedDateTime>): List<NetworkStats.Bucket> {
        val usage = runBlocking {
            networkStatsManager.queryDetails(
                ConnectivityManager.TYPE_WIFI,
                null,
                timeFrame.first.toEpochSecond() * 1000,
                timeFrame.second.toEpochSecond() * 1000,
            )
        }
        val r = mutableListOf<NetworkStats.Bucket>()
        while (usage.hasNextBucket()) {
            val b = NetworkStats.Bucket()
            usage!!.getNextBucket(b)
            r.add(b)
            Log.d("netUsage", b.txBytes.toString())
            Log.d("netUsage", b.rxBytes.toString())
        }
        return r
    }

    suspend fun getUsageByUIDAsync(
        timeFrame: Pair<ZonedDateTime, ZonedDateTime>,
        networkType: NetworkType
    ): List<AppUsageInfo> {
        val r = mutableMapOf<Int, AppUsageInfo>()
        val buckets = networkStatsManager.queryDetails(
            if (networkType == NetworkType.GSM) {
                ConnectivityManager.TYPE_MOBILE
            } else {
                ConnectivityManager.TYPE_WIFI
            },
            null,
            timeFrame.first.toEpochSecond() * 1000,
            timeFrame.second.toEpochSecond() * 1000,
        )
        val bucket = NetworkStats.Bucket()
        while (buckets.hasNextBucket()) {
            buckets!!.getNextBucket(bucket)

            if (!r.containsKey(bucket.uid)) {
                if (bucket.uid == NetworkStats.Bucket.UID_ALL || bucket.uid == NetworkStats.Bucket.UID_TETHERING) {
                    continue
                }
                if (bucket.uid == NetworkStats.Bucket.UID_REMOVED) {
                    r[bucket.uid] = AppUsageInfo(
                        AppInfo(
                            bucket.uid,
                            "Removed",
                            "Removed",
                            null
                        ),
                        bucket.txBytes,
                        bucket.rxBytes,
                    )
                    continue
                }
                val packageName: String = packageManager.getPackagesForUid(bucket.uid).let {
                    if (it == null) {
                        Log.d("NetworkUsage", "Cannot retrieve package name for uid: ${bucket.uid}")
                        null
                    } else {
                        it[0]
                    }
                } ?: continue
                val p = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_META_DATA
                )
                r[bucket.uid] = AppUsageInfo(
                    AppInfo(
                        bucket.uid,
                        packageManager.getApplicationLabel(p.applicationInfo).toString(),
                        p.packageName,
                        p.applicationInfo.loadIcon(packageManager)
                    ),
                    bucket.txBytes,
                    bucket.rxBytes,
                )
            } else {
                val currentBucket = r[bucket.uid]!!
                r[bucket.uid] = currentBucket.copy(
                    txBytes = currentBucket.txBytes + bucket.txBytes,
                    rxBytes = currentBucket.rxBytes + bucket.rxBytes
                )
            }
        }
        val r1 = r.values.toMutableList()
        r1.sortByDescending { (it.rxBytes + it.txBytes) }
        return r1
    }

    override fun getUsageGroupedByTime(
        timeFrame: Timeframe,
        networkType: NetworkType
    ): List<UsageData> {
        val buckets = networkStatsManager.queryDetails(
            if (networkType == NetworkType.GSM) {
                ConnectivityManager.TYPE_MOBILE
            } else {
                ConnectivityManager.TYPE_WIFI
            },
            null,
            timeFrame.start.toInstant().toEpochMilli(),
            timeFrame.end.toInstant().toEpochMilli()
        )
        val r: MutableList<UsageData> = mutableListOf()
        var lastStartTime: Long = 0

        val bucket = NetworkStats.Bucket()
        while (buckets.hasNextBucket()) {
            buckets!!.getNextBucket(bucket)
            if (bucket.startTimeStamp == lastStartTime) {
                val last = r.last().copy(
                    rxBytes = r.last().rxBytes + bucket.rxBytes,
                    txBytes = r.last().txBytes + bucket.txBytes
                )
                r.removeAt(r.lastIndex)
                r.add(last)
            } else if (bucket.startTimeStamp > lastStartTime) {
                r.add(
                    UsageData(
                        rxBytes = bucket.rxBytes,
                        txBytes = bucket.txBytes,
                        time = Timeframe(
                            start = ZonedDateTime.ofInstant(Instant.ofEpochMilli(bucket.startTimeStamp), ZoneId.systemDefault()),
                            end = ZonedDateTime.ofInstant(Instant.ofEpochMilli(bucket.endTimeStamp), ZoneId.systemDefault())
                        )
                    )
                )
                lastStartTime = bucket.startTimeStamp
            }
        }
        return r
    }


    override fun getUsageGroupedByUID(
        timeFrame: Timeframe,
        networkType: NetworkType
    ): List<AppUsageInfo> {
        val r = mutableMapOf<Int, AppUsageInfo>()
        val buckets = networkStatsManager.queryDetails(
            if (networkType == NetworkType.GSM) {
                ConnectivityManager.TYPE_MOBILE
            } else {
                ConnectivityManager.TYPE_WIFI
            },
            null,
            timeFrame.start.toEpochSecond() * 1000,
            timeFrame.end.toEpochSecond() * 1000,
        )
        val bucket = NetworkStats.Bucket()
        if (!buckets.hasNextBucket()) {
            Log.w("Network Usage", "No buckets returned from query.")
        }
        while (buckets.hasNextBucket()) {
            buckets!!.getNextBucket(bucket)

            if (!r.containsKey(bucket.uid)) {
                createNewUIDBucket(bucket, r)
            } else {
                r[bucket.uid] = r[bucket.uid]!!.copy(
                    txBytes = r[bucket.uid]!!.txBytes + bucket.txBytes,
                    rxBytes = r[bucket.uid]!!.rxBytes + bucket.rxBytes
                )

            }
        }
        r[NetworkStats.Bucket.UID_REMOVED]?.appInfo?.icon = r[1000]?.appInfo?.icon
        r[NetworkStats.Bucket.UID_TETHERING]?.appInfo?.icon = r[1000]?.appInfo?.icon
        r[NetworkStats.Bucket.UID_ALL]?.appInfo?.icon = r[1000]?.appInfo?.icon
        val r1 = r.values.toMutableList()
        r1.sortByDescending { (it.rxBytes + it.txBytes) }
        return r1
    }

    private fun createNewUIDBucket(
        bucket: NetworkStats.Bucket,
        r: MutableMap<Int, AppUsageInfo>
    ) {
        if (bucket.uid == NetworkStats.Bucket.UID_ALL) {
            r[bucket.uid] = AppUsageInfo(
                AppInfo(
                    bucket.uid,
                    "All",
                    "All",
                    null
                ),
                bucket.txBytes,
                bucket.rxBytes,
            )
            return
        }
        if (bucket.uid == NetworkStats.Bucket.UID_TETHERING) {
            r[bucket.uid] = AppUsageInfo(
                AppInfo(
                    bucket.uid,
                    "Tethering",
                    "Tethering",
                    null
                ),
                bucket.txBytes,
                bucket.rxBytes,
            )
            return
        }
        if (bucket.uid == NetworkStats.Bucket.UID_REMOVED) {
            r[bucket.uid] = AppUsageInfo(
                AppInfo(
                    bucket.uid,
                    "Removed",
                    "Removed",
                    null
                ),
                bucket.txBytes,
                bucket.rxBytes,
            )
            return
        }
        val packageName: String = packageManager.getPackagesForUid(bucket.uid).let {
            if (it == null) {
                Log.d("NetworkUsage", "Cannot retrieve package name for uid: ${bucket.uid}")
                null
            } else {
                it[0]
            }
        } ?: return
        val p = packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_META_DATA
        )

        //TODO Move name, packageName, icon to out of class initializer
        r[bucket.uid] = AppUsageInfo(
            AppInfo(
                bucket.uid,
                packageManager.getApplicationLabel(p.applicationInfo).toString(),
                p.packageName,
                p.applicationInfo.loadIcon(packageManager)
            ),
            bucket.txBytes,
            bucket.rxBytes,
        )
    }
}
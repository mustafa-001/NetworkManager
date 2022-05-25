package com.example.networkusage

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.sql.Timestamp
import java.time.ZonedDateTime

class UsageDetailsManager(
    private val packageManager: PackageManager,
    private val networkStatsManager: NetworkStatsManager
) {

    private val packages: List<PackageInfo> =
        packageManager.getInstalledPackages(PackageManager.GET_META_DATA)

    data class AppUsageInfo(
        val uid: Int,
        val name: String?,
        val packageName: String,
        var txBytes: Long,
        var rxBytes: Long,
        var icon: Drawable?,
    )

    data class GeneralUsageInfo(
        var txBytes: Long,
        var rxBytes: Long,
        val startTimeStamp: Long,
        val endTimeStamp: Long,
    )


    enum class NetworkType {
        GSM,
        WIFI
    }

    fun queryForUid(
        uid: Int,
        timeFrame: Pair<ZonedDateTime, ZonedDateTime>
    ): List<GeneralUsageInfo> {
        Log.d("Network Usage", "timeFrame set to: ${timeFrame.first} and ${timeFrame.second}")
        val usage = runBlocking {
            networkStatsManager.queryDetailsForUid(
                ConnectivityManager.TYPE_MOBILE,
                null,
                timeFrame.first.minusHours(2).toEpochSecond() * 1000,
                timeFrame.second.plusHours(2).toEpochSecond() * 1000,
                uid
            )
        }
        val r = mutableListOf<GeneralUsageInfo>()
        while (usage.hasNextBucket()) {
            val b = NetworkStats.Bucket()
            usage!!.getNextBucket(b)
            r.add(
                GeneralUsageInfo(
                    rxBytes = b.rxBytes,
                    txBytes = b.txBytes,
                    startTimeStamp = b.startTimeStamp,
                    endTimeStamp = b.endTimeStamp
                )
            )
            Log.d("netUsage", b.txBytes.toString())
            Log.d("netUsage", b.rxBytes.toString())
        }
        return r
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
                        bucket.uid,
                        "Removed",
                        "Removed",
                        bucket.txBytes,
                        bucket.rxBytes,
                        null
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
                    p.applicationInfo.uid,
                    packageManager.getApplicationLabel(p.applicationInfo).toString(),
                    p.packageName,
                    bucket.txBytes,
                    bucket.rxBytes,
                    p.applicationInfo.loadIcon(packageManager)
                )
            } else {
                r[bucket.uid]!!.let {
                    it.txBytes += bucket.txBytes
                    it.rxBytes += bucket.rxBytes
                }

            }
        }
        r[NetworkStats.Bucket.UID_REMOVED]?.icon = r[1000]?.icon
        val r1 = r.values.toMutableList()
        r1.sortByDescending { (it.rxBytes + it.txBytes) }
        return r1
    }

    fun getUsageByTime(
        timeFrame: Pair<ZonedDateTime, ZonedDateTime>,
        networkType: NetworkType
    ): List<GeneralUsageInfo> {
        val buckets = networkStatsManager.queryDetails(
            if (networkType == NetworkType.GSM) {
                ConnectivityManager.TYPE_MOBILE
            } else {
                ConnectivityManager.TYPE_WIFI
            },
            null,
            timeFrame.first.toInstant().toEpochMilli(),
            timeFrame.second.toInstant().toEpochMilli()
        )
        val r: MutableList<GeneralUsageInfo> = mutableListOf()
        var lastStartTime: Long = 0

        val bucket = NetworkStats.Bucket()
        while (buckets.hasNextBucket()) {
            buckets!!.getNextBucket(bucket)
            if (bucket.startTimeStamp == lastStartTime) {
                r.last().rxBytes += bucket.txBytes
                r.last().txBytes += bucket.txBytes
            } else if (bucket.startTimeStamp > lastStartTime) {
                r.add(
                    GeneralUsageInfo(
                        rxBytes = bucket.rxBytes,
                        txBytes = bucket.txBytes,
                        startTimeStamp = bucket.startTimeStamp,
                        endTimeStamp = bucket.endTimeStamp
                    )
                )
                lastStartTime = bucket.startTimeStamp
            }
        }
        return r
    }


    fun getUsageByUID2(
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
                if (bucket.uid == NetworkStats.Bucket.UID_ALL) {
                    r[bucket.uid] = AppUsageInfo(
                        bucket.uid,
                        "All",
                        "All",
                        bucket.txBytes,
                        bucket.rxBytes,
                        null
                    )
                    continue
                }
                if (bucket.uid == NetworkStats.Bucket.UID_TETHERING) {
                    r[bucket.uid] = AppUsageInfo(
                        bucket.uid,
                        "Tethering",
                        "Tethering",
                        bucket.txBytes,
                        bucket.rxBytes,
                        null
                    )
                    continue
                }
                if (bucket.uid == NetworkStats.Bucket.UID_REMOVED) {
                    r[bucket.uid] = AppUsageInfo(
                        bucket.uid,
                        "Removed",
                        "Removed",
                        bucket.txBytes,
                        bucket.rxBytes,
                        null
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

                //TODO Move name, packageName, icon to out of class initializer
                r[bucket.uid] = AppUsageInfo(
                    p.applicationInfo.uid,
                    packageManager.getApplicationLabel(p.applicationInfo).toString(),
                    p.packageName,
                    bucket.txBytes,
                    bucket.rxBytes,
                    p.applicationInfo.loadIcon(packageManager)
                )
            } else {
                r[bucket.uid]!!.let {
                    it.txBytes += bucket.txBytes
                    it.rxBytes += bucket.rxBytes
                }

            }
        }
        r[NetworkStats.Bucket.UID_REMOVED]?.icon = r[1000]?.icon
        r[NetworkStats.Bucket.UID_TETHERING]?.icon = r[1000]?.icon
        r[NetworkStats.Bucket.UID_ALL]?.icon = r[1000]?.icon
        val r1 = r.values.toMutableList()
        r1.sortByDescending { (it.rxBytes + it.txBytes) }
        return r1
    }

    fun getUsageByUID(
        timeFrame: Pair<ZonedDateTime, ZonedDateTime>,
        networkType: NetworkType
    ): List<AppUsageInfo> {
        val r = mutableListOf<AppUsageInfo>()
        val packagesToRemove = mutableListOf<PackageInfo>()
        for (p in packages) {
            val usage = runBlocking {
                networkStatsManager.queryDetailsForUid(
                    if (networkType == NetworkType.GSM) {
                        ConnectivityManager.TYPE_MOBILE
                    } else {
                        ConnectivityManager.TYPE_WIFI
                    },
                    null,
                    timeFrame.first.toEpochSecond() * 1000,
                    timeFrame.second.toEpochSecond() * 1000,
                    packageManager.getPackageUid(p.packageName, 0)
                )
            }
            var rxBytes: Long = 0
            var txBytes: Long = 0
            val bucket = NetworkStats.Bucket()
            while (usage.hasNextBucket()) {
                usage!!.getNextBucket(bucket)
                rxBytes += bucket.rxBytes
                txBytes += bucket.txBytes
                if (usage.hasNextBucket()) {
                    Log.d("usage", "multiple buckets ${bucket.rxBytes}")
                }
            }
            r.add(
                AppUsageInfo(
                    p.applicationInfo.uid,
                    packageManager.getApplicationLabel(p.applicationInfo).toString(),
                    p.packageName,
                    txBytes,
                    rxBytes,
                    p.applicationInfo.loadIcon(packageManager)
                )
            )
            Log.d("NetworkUsage", "AppUsageInfo: ${p.packageName}: $rxBytes")
        }
        r.sortByDescending { (it.rxBytes + it.txBytes) }
        return r
    }
}
package com.example.networkusage.usageDetailsProcessor

import android.graphics.drawable.Drawable
import java.time.ZonedDateTime

//TODO Rename, possibly TimeframeUsageInfo
data class UsageData(
    val time: Timeframe,
    val txBytes: Long,
    val rxBytes: Long,
)

data class Timeframe(
    val start: ZonedDateTime,
    val end: ZonedDateTime
)

data class AppInfo(
    val uid: Int,
    val name: String?,
    val packageName: String,
    var icon: Drawable?,
)

data class AppUsageInfo(
    val appInfo: AppInfo,
    val txBytes: Long,
    val rxBytes: Long
)

data class AppDetailedUsageInfo(
    val appInfo: AppInfo,
    val usageData: List<UsageData>
)
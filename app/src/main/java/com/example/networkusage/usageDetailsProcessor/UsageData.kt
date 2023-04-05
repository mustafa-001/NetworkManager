package com.example.networkusage.usageDetailsProcessor

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import java.time.ZonedDateTime

@Stable
data class UsageData(
    val time: Timeframe,
    val txBytes: Long,
    val rxBytes: Long,
)

@Stable
data class Timeframe(
    val start: ZonedDateTime,
    val end: ZonedDateTime
)

@Stable
data class AppInfo(
    val uid: Int,
    val name: String?,
    val packageName: String,
    var icon: Drawable?,
    var iconBitmap : ImageBitmap? = null
)

@Stable
data class AppUsageInfo(
    val appInfo: AppInfo,
    val txBytes: Long,
    val rxBytes: Long
)

@Stable
data class AppDetailedUsageInfo(
    val appInfo: AppInfo,
    val usageData: List<UsageData>
)
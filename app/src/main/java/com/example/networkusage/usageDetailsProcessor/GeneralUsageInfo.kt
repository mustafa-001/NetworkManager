package com.example.networkusage.usageDetailsProcessor

//TODO Rename, possibly TimeframeUsageInfo
data class GeneralUsageInfo(
    var txBytes: Long,
    var rxBytes: Long,
    val startTimeStamp: Long,
    val endTimeStamp: Long,
)
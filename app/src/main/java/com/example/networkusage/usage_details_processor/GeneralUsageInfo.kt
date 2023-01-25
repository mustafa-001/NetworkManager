package com.example.networkusage.usage_details_processor

//TODO Rename, possibly TimeframeUsageInfo
data class GeneralUsageInfo(
    var txBytes: Long,
    var rxBytes: Long,
    val startTimeStamp: Long,
    val endTimeStamp: Long,
)
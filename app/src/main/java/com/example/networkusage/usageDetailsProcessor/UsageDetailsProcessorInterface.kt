package com.example.networkusage.usageDetailsProcessor


interface UsageDetailsProcessorInterface {


    fun getUsageByUIDGroupedByTime(
        uid: Int,
        timeFrame: Timeframe
    ): AppDetailedUsageInfo

    fun getUsageGroupedByTime(
        timeFrame: Timeframe,
        networkType: NetworkType
    ): List<UsageData>

    fun getUsageGroupedByUID(
        timeFrame: Timeframe,
        networkType: NetworkType
    ): List<AppUsageInfo>
}
package com.example.networkusage.usage_details_processor

import java.time.ZonedDateTime

interface UsageDetailsProcessorInterface {


    fun getUsageByUIDGroupedByTime(
        uid: Int,
        timeFrame: Pair<ZonedDateTime, ZonedDateTime>
    ): List<GeneralUsageInfo>

    fun getUsageGroupedByTime(
        timeFrame: Pair<ZonedDateTime, ZonedDateTime>,
        networkType: NetworkType
    ): List<GeneralUsageInfo>

    fun getUsageGroupedByUID(
        timeFrame: Pair<ZonedDateTime, ZonedDateTime>,
        networkType: NetworkType
    ): List<AppUsageInfo>
}
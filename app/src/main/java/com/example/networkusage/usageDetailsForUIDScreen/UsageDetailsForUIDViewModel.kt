package com.example.networkusage.usageDetailsForUIDScreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.networkusage.usageDetailsProcessor.AppDetailedUsageInfo
import com.example.networkusage.usageDetailsProcessor.Timeframe
import com.example.networkusage.usageDetailsProcessor.UsageData
import com.example.networkusage.usageDetailsProcessor.UsageDetailsProcessorInterface
import java.time.ZonedDateTime

class UsageDetailsForUIDViewModel(
    private val uid: Int,
    private val timeFrame: LiveData<Timeframe>,
    private val usageDetailsProcessor: UsageDetailsProcessorInterface
) {
    private val _usageByUIDGroupedByTime: MutableLiveData<AppDetailedUsageInfo> =
        MutableLiveData()
    val usageByUIDGroupedByTime: LiveData<AppDetailedUsageInfo>
        get() = _usageByUIDGroupedByTime

    init {
        //observe commonTopbarParametersViewModel.timeFrame and update _usageByUIDGroupedByTime
        timeFrame.observeForever {
            _usageByUIDGroupedByTime.value = usageDetailsProcessor.getUsageByUIDGroupedByTime(
                uid, it
            )
        }
    }
}
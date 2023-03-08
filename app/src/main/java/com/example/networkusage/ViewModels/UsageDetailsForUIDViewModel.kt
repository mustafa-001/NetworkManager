package com.example.networkusage.ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.networkusage.usage_details_processor.GeneralUsageInfo
import com.example.networkusage.usage_details_processor.UsageDetailsProcessorInterface
import java.time.ZonedDateTime

class UsageDetailsForUIDViewModel(
    private val uid: Int,
    private val timeFrame: LiveData<Pair<ZonedDateTime, ZonedDateTime>>,
    private val usageDetailsProcessor: UsageDetailsProcessorInterface
) {
    private val _usageByUIDGroupedByTime: MutableLiveData<List<GeneralUsageInfo>> =
        MutableLiveData(emptyList())
    val usageByUIDGroupedByTime: LiveData<List<GeneralUsageInfo>>
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
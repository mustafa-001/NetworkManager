package com.example.networkusage.ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.networkusage.usage_details_processor.GeneralUsageInfo
import com.example.networkusage.usage_details_processor.UsageDetailsProcessorInterface

class UsageDetailsForUIDViewModel(
    private val uid: Int,
    commonTopbarParametersViewModel: CommonTopbarParametersViewModel,
    private val usageDetailsProcessor: UsageDetailsProcessorInterface
) {
    private val _usageByUIDGroupedByTime: MutableLiveData<List<GeneralUsageInfo>> =
        MutableLiveData(emptyList())
    val usageByUIDGroupedByTime: LiveData<List<GeneralUsageInfo>>
        get() = _usageByUIDGroupedByTime

    init {
        //observe commonTopbarParametersViewModel.timeFrame and update _usageByUIDGroupedByTime
        commonTopbarParametersViewModel.timeFrame.observeForever {
            _usageByUIDGroupedByTime.value = usageDetailsProcessor.getUsageByUIDGroupedByTime(
                uid, it
            )
        }
    }
}
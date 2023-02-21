package com.example.networkusage.ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.networkusage.usage_details_processor.GeneralUsageInfo

class UsageDetailsForUIDViewModel(
    val uid: Int,
    val usageListViewModel: UsageListViewModel,
) {
    private val usageDetailsManager = usageListViewModel.usageDetailsManager
    private val _usageByUIDGroupedByTime: MutableLiveData<List<GeneralUsageInfo>> =
        MutableLiveData(emptyList())
    val usageByUIDGroupedByTime: LiveData<List<GeneralUsageInfo>>
        get() = _usageByUIDGroupedByTime

    init {
        usageListViewModel.timeFrame.observeForever {
            _usageByUIDGroupedByTime.value = usageDetailsManager.getUsageByUIDGroupedByTime(
                uid, it
            )
        }
        //observe usageListViewModel.networkType and update _usageByUIDGroupedByTime
        usageListViewModel.networkType.observeForever {
            _usageByUIDGroupedByTime.value = usageDetailsManager.getUsageByUIDGroupedByTime(
                uid, usageListViewModel.timeFrame.value!!
            )
        }
    }
}
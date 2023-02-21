package com.example.networkusage.ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkusage.usage_details_processor.AppUsageInfo
import com.example.networkusage.usage_details_processor.UsageDetailsProcessorInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeneralUsageScreenViewModel(
    val commonTopbarParametersViewModel: CommonTopbarParametersViewModel,
    val usageDetailsManager: UsageDetailsProcessorInterface
) :
    ViewModel() {
    private val _usageByUID: MutableLiveData<List<AppUsageInfo>> =
        MutableLiveData(emptyList())
    val usageByUID: LiveData<List<AppUsageInfo>>
        get() = _usageByUID

    init {
        commonTopbarParametersViewModel.timeFrame.observeForever { timeFrame ->
            viewModelScope.launch(Dispatchers.IO) {
                _usageByUID.postValue(
                    usageDetailsManager.getUsageGroupedByUID(
                        timeFrame,
                        commonTopbarParametersViewModel.networkType.value!!
                    )
                )
            }
        }
        //setup observer for commonTopbarParametersViewModel.networkType
        commonTopbarParametersViewModel.networkType.observeForever { networkType ->
            viewModelScope.launch(Dispatchers.IO) {
                _usageByUID.postValue(
                    usageDetailsManager.getUsageGroupedByUID(
                        commonTopbarParametersViewModel.timeFrame.value!!,
                        networkType
                    )
                )
            }
        }
    }

}
package com.example.networkusage.generalUsageScreen

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkusage.ViewModels.CommonTopbarParametersViewModel
import com.example.networkusage.usageDetailsProcessor.AppUsageInfo
import com.example.networkusage.usageDetailsProcessor.UsageDetailsProcessorInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeneralUsageScreenViewModel(
    private val commonTopbarParametersViewModel: CommonTopbarParametersViewModel,
    val usageDetailsManager: UsageDetailsProcessorInterface
) :
    ViewModel() {
    private val _usageByUID: MutableLiveData<List<AppUsageInfo>> =
        MutableLiveData(emptyList())

    private fun postUsageByUID(usages: List<AppUsageInfo>) {
        usages.map {
            it.appInfo.iconBitmap =
                when (it.appInfo.icon) {
                    null -> Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    else -> it.appInfo.icon!!.toBitmap()
                }.asImageBitmap()
        }
        _usageByUID.postValue(usages)
    }

    val usageByUID: LiveData<List<AppUsageInfo>>
        get() = _usageByUID

    init {
        commonTopbarParametersViewModel.timeFrame.observeForever { timeFrame ->
            viewModelScope.launch(Dispatchers.IO) {
                postUsageByUID(
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
                postUsageByUID(
                    usageDetailsManager.getUsageGroupedByUID(
                        commonTopbarParametersViewModel.timeFrame.value!!,
                        networkType
                    )
                )
            }
        }
    }

}
package com.example.networkusage.ViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkusage.TimeFrameMode
import com.example.networkusage.usage_details_processor.AppUsageInfo
import com.example.networkusage.usage_details_processor.UsageDetailsProcessorInterface
import com.example.networkusage.usage_details_processor.NetworkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class UsageListViewModel(val usageDetailsManager: UsageDetailsProcessorInterface) :
    ViewModel() {


    var networkType = NetworkType.GSM
        set(value) {
            field = value
            //The viewmodel not yet initialized, so change didn't come from user input, no need to query.
            if (timeFrame.value != null) {

                viewModelScope.launch(Dispatchers.IO) {
                    mutableUsageByUID.postValue(
                        usageDetailsManager.getUsageGroupedByUID(
                            timeFrame.value!!,
                            networkType
                        )
                    )
                }
            }
        }

    private val mutableUsageByUID: MutableLiveData<List<AppUsageInfo>> =
        MutableLiveData(emptyList())
    val usageByUID: LiveData<List<AppUsageInfo>>
    get() =  mutableUsageByUID

    private val mutableTimeFrame = MutableLiveData<Pair<ZonedDateTime, ZonedDateTime>>()

    val timeFrame: LiveData<Pair<ZonedDateTime, ZonedDateTime>>
        get() = mutableTimeFrame

    fun setTime(value: Pair<ZonedDateTime, ZonedDateTime>) {
        mutableTimeFrame.value = value
        Log.d("Network Usage", "timeFrame set to: ${value.first} and ${value.second}")
        viewModelScope.launch(Dispatchers.IO) {
            mutableUsageByUID.postValue(usageDetailsManager.getUsageGroupedByUID(value, networkType))
        }
    }


    fun selectPredefinedTimeFrame(timeFrameMode: TimeFrameMode) {
        assert(timeFrameMode != TimeFrameMode.CUSTOM)
        setTime(
            when (timeFrameMode) {
                TimeFrameMode.LAST_WEEK -> Pair(
                    ZonedDateTime.now().minusDays(7),
                    ZonedDateTime.now()
                )
                TimeFrameMode.LAST_30_DAYS -> Pair(
                    ZonedDateTime.now().minusDays(30),
                    ZonedDateTime.now()
                )
                TimeFrameMode.THIS_MONTH -> Pair(
                    ZonedDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0),
                    ZonedDateTime.now()
                )
                TimeFrameMode.TODAY -> Pair(
                    ZonedDateTime.now().withHour(0).withMinute(0),
                    ZonedDateTime.now()
                )
                TimeFrameMode.CUSTOM -> TODO("Unreachable.")
            }
        )
    }
}
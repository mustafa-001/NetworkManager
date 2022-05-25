package com.example.networkusage.ViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkusage.TimeFrameMode
import com.example.networkusage.UsageDetailsManager
import kotlinx.coroutines.*
import java.time.ZonedDateTime

class UsageListViewModel(val usageDetailsManager: UsageDetailsManager) :
    ViewModel() {


    var networkType = UsageDetailsManager.NetworkType.GSM
        set(value) {
            field = value
            //The viewmodel not yet initialized, so change didn't come from user input, no need to query.
            if (timeFrame.value != null) {

                viewModelScope.launch(Dispatchers.IO) {
                    mutableUsageByUID.postValue(
                        usageDetailsManager.getUsageByUID2(
                            timeFrame.value!!,
                            networkType
                        )
                    )
                }
            }
        }

    private val mutableUsageByUID: MutableLiveData<List<UsageDetailsManager.AppUsageInfo>> =
        MutableLiveData(emptyList())
    val usageByUID: LiveData<List<UsageDetailsManager.AppUsageInfo>>
    get() =  mutableUsageByUID

    private val mutableTimeFrame = MutableLiveData<Pair<ZonedDateTime, ZonedDateTime>>()

    val timeFrame: LiveData<Pair<ZonedDateTime, ZonedDateTime>>
        get() = mutableTimeFrame

    public fun setTime(value: Pair<ZonedDateTime, ZonedDateTime>) {
        mutableTimeFrame.postValue(value)
        Log.d("Network Usage", "timeFrame set to: ${value.first} and ${value.second}")
        viewModelScope.launch(Dispatchers.IO) {
            mutableUsageByUID.postValue(usageDetailsManager.getUsageByUID2(value, networkType))
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
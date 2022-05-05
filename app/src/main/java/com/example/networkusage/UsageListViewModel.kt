package com.example.networkusage

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import java.time.LocalDateTime
import kotlin.coroutines.CoroutineContext

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

    private val mutableTimeFrame = MutableLiveData<Pair<LocalDateTime, LocalDateTime>>()

    val timeFrame: LiveData<Pair<LocalDateTime, LocalDateTime>>
        get() = mutableTimeFrame

    public fun setTime(value: Pair<LocalDateTime, LocalDateTime>) {
        mutableTimeFrame.postValue(value)
        Log.d("Network Usage", "timeFrame set to: ${value.first} and ${value.second}")
        viewModelScope.launch(Dispatchers.IO) {
            mutableUsageByUID.postValue(usageDetailsManager.getUsageByUID2(value, networkType))
        }
    }


    fun selectPredefinedTimeFrame(timeFrameMode: TimeFrameMode) {
        setTime(
            when (timeFrameMode) {
                TimeFrameMode.LAST_WEEK -> Pair(
                    LocalDateTime.now().minusDays(7),
                    LocalDateTime.now()
                )
                TimeFrameMode.LAST_30_DAYS -> Pair(
                    LocalDateTime.now().minusDays(30),
                    LocalDateTime.now()
                )
                TimeFrameMode.THIS_MONTH -> Pair(
                    LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0),
                    LocalDateTime.now()
                )
                TimeFrameMode.TODAY -> Pair(
                    LocalDateTime.now().withHour(0).withMinute(9),
                    LocalDateTime.now()
                )
                TimeFrameMode.CUSTOM -> TODO()
            }
        )
    }
}
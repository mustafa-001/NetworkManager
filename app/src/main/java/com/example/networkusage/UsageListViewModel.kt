package com.example.networkusage

import android.util.Log
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
            viewModelScope.launch(Dispatchers.IO) {
                usageByUID.postValue(usageDetailsManager.getUsageByUID2(timeFrame, networkType))
            }
        }

    val usageByUID: MutableLiveData<List<UsageDetailsManager.AppUsageInfo>> =
        MutableLiveData(emptyList())


    var timeFrame: Pair<LocalDateTime, LocalDateTime> = Pair(
        LocalDateTime.now(), LocalDateTime.now()
    )
        set(value) {
            field = value
            Log.d("Network Usage", "timeFrame set to: ${value.first} and ${value.second}")
            viewModelScope.launch(Dispatchers.IO) {
                usageByUID.postValue(usageDetailsManager.getUsageByUID2(timeFrame, networkType))
            }
        }


    fun selectPredefinedTimeFrame(timeFrameMode: TimeFrameMode) {
        timeFrame = when (timeFrameMode) {
            TimeFrameMode.LAST_WEEK -> Pair(LocalDateTime.now().minusDays(7), LocalDateTime.now())
            TimeFrameMode.LAST_30_DAYS -> Pair(LocalDateTime.now().minusDays(30), LocalDateTime.now())
            TimeFrameMode.THIS_MONTH -> Pair(
                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0),
                LocalDateTime.now()
            )
            TimeFrameMode.TODAY -> Pair(LocalDateTime.now().withHour(0).withMinute(9), LocalDateTime.now())
            TimeFrameMode.CUSTOM -> TODO()
        }
    }
}
package com.example.networkusage

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
            viewModelScope.launch(Dispatchers.IO) {
                usageByUID.postValue(usageDetailsManager.getUsageByUID2(timeFrame, networkType))
            }
        }

    fun selectLastWeek(){
        timeFrame =Pair( LocalDateTime.now().minusDays(7),LocalDateTime.now())
    }
    fun selectLast30Days(){
        timeFrame =Pair( LocalDateTime.now().minusDays(30),LocalDateTime.now())
    }
    fun selectThisMOnth(){
        timeFrame =Pair( LocalDateTime.now().withDayOfMonth(1).withHour(1).withMinute(1),LocalDateTime.now())
    }
}
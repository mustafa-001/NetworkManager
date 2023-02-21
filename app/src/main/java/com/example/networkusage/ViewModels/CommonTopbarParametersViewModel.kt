package com.example.networkusage.ViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkusage.TimeFrameMode
import com.example.networkusage.usage_details_processor.NetworkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class CommonTopbarParametersViewModel() : ViewModel() {


    private val _networkType = MutableLiveData(NetworkType.WIFI)
    val networkType: LiveData<NetworkType>
        get() = _networkType

    fun changeNetworkType(value: NetworkType) {
        _networkType.value = value
    }

    fun toggleNetworkType() {
        //Async postValue() causes race condition when saving networkType to SharedPreferences in
        // network selector icon/button onClick() callback.
        _networkType.value = if (networkType.value == NetworkType.WIFI) {
            NetworkType.GSM
        } else {
            NetworkType.WIFI
        }
    }

    private val _timeFrame = MutableLiveData<Pair<ZonedDateTime, ZonedDateTime>>()
    val timeFrame: LiveData<Pair<ZonedDateTime, ZonedDateTime>>
        get() = _timeFrame

    fun setTime(value: Pair<ZonedDateTime, ZonedDateTime>) {
        _timeFrame.value = value
        Log.d("Network Usage", "timeFrame set to: ${value.first} and ${value.second}")
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
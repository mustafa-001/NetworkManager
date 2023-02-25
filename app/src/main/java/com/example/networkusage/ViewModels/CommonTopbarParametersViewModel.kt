package com.example.networkusage.ViewModels

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.networkusage.TimeFrameMode
import com.example.networkusage.usage_details_processor.NetworkType
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class CommonTopbarParametersViewModel(val sharedPref: SharedPreferences) : ViewModel() {

    val timeFrameMode = TimeFrameMode.valueOf(sharedPref.getString("mode", "CUSTOM")!!)
    var useTestData: Boolean = sharedPref.getBoolean("useTestData", false)
    set(value) {
        field = value
        with(sharedPref.edit()) {
            putBoolean("useTestData", value)
            apply()
        }
    }

    private val _timeFrame = MutableLiveData<Pair<ZonedDateTime, ZonedDateTime>>()
    val timeFrame: LiveData<Pair<ZonedDateTime, ZonedDateTime>>
        get() = _timeFrame

    private val _networkType = MutableLiveData(
        NetworkType.valueOf(
            sharedPref.getString(
                "networkType",
                NetworkType.GSM.name
            )!!
        )
    )
    val networkType: LiveData<NetworkType>
        get() = _networkType

    init {
        if (timeFrameMode != TimeFrameMode.CUSTOM) {
            selectPredefinedTimeFrame(timeFrameMode)
        } else {
            setTime(
                Pair(
                    Instant.ofEpochSecond(
                        sharedPref.getLong(
                            "start_time",
                            Instant.now().minusSeconds(60 * 60 * 24 * 7).epochSecond
                        )
                    ).atZone(ZoneId.systemDefault()),
                    Instant.ofEpochSecond(
                        sharedPref.getLong(
                            "start_time",
                            Instant.now().epochSecond
                        )
                    ).atZone(ZoneId.systemDefault()),
                )
            )
        }
    }


    fun changeNetworkType(value: NetworkType) {
        _networkType.value = value
        with(
            sharedPref.edit()
        ) {
            putString("networkType", networkType.value!!.name)
            apply()
        }

    }

    fun toggleNetworkType() {
        //Async postValue() causes race condition when saving networkType to SharedPreferences in
        // network selector icon/button onClick() callback.
        if (networkType.value == NetworkType.WIFI) {
            changeNetworkType(NetworkType.GSM)
        } else {
            changeNetworkType(NetworkType.WIFI)
        }
    }

    fun setTime(value: Pair<ZonedDateTime, ZonedDateTime>) {
        //compare value and timeFrame.value to avoid unnecessary updates
        if (value != timeFrame.value) {
        with(
            sharedPref.edit()
        ) {
            putLong(
                "start_time",
                value.first.toEpochSecond()
            )
            putLong(
                "end_time",
                value.second.toEpochSecond()
            )
            apply()
        }
        }
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
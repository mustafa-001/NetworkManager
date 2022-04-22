package com.example.networkusage

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


enum class TimeFrameMode {
    LAST_WEEK, LAST_30_DAYS, THIS_MONTH, CUSTOM
}

@Composable
fun TimeFrameSelector(
    activity: ComponentActivity,
    viewModel: UsageListViewModel,
    mode: TimeFrameMode,
    onDismissRequest: () -> Unit,
    onSubmitRequest: (TimeFrameMode, Pair<LocalDateTime, LocalDateTime>) -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        var startTime = remember {
            viewModel.timeFrame.first
        }
        var endTime = remember {
            viewModel.timeFrame.second
        }
        var selectedMode by remember { mutableStateOf(mode) }
        val scrollState = rememberScrollState()
//        var customSelectionVisibility by remember { mutableStateOf(mode == TimeFrameMode.CUSTOM) }
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier
                .height(400.dp)
                .padding(10.dp)) {
                Row(modifier = Modifier.horizontalScroll(scrollState) ){
                    val lastWeekButtonColor = if (selectedMode == TimeFrameMode.LAST_WEEK) {Color.Red} else {Color.Unspecified}
                    val last30DaysButtonColor = if (selectedMode == TimeFrameMode.LAST_30_DAYS) {Color.Red} else {Color.Unspecified}
                    val thisMonthButtonColor = if (selectedMode == TimeFrameMode.THIS_MONTH) {Color.Red} else {Color.Unspecified}
                    val customButtonColor = if (selectedMode == TimeFrameMode.CUSTOM) {Color.Red} else {Color.Unspecified}
                    Button(onClick = {
                        selectedMode = TimeFrameMode.LAST_WEEK
                        onSubmitRequest(selectedMode, Pair(startTime, endTime))
                        onDismissRequest()
                    }, Modifier.background(lastWeekButtonColor)) {
                        Text(text = "Last week")
                    }
                    Button(onClick = {
                        selectedMode = TimeFrameMode.LAST_30_DAYS
                        onSubmitRequest(selectedMode, Pair(startTime, endTime))
                        onDismissRequest()
                    }, Modifier.background(last30DaysButtonColor)) {
                        Text(text = "Last 30 days")
                    }
                    Button(onClick = {
                        selectedMode = TimeFrameMode.THIS_MONTH
                        onSubmitRequest(selectedMode, Pair(startTime, endTime))
                        onDismissRequest()
                    }, Modifier.background(thisMonthButtonColor))
                    {
                        Text(text = "This month")
                    }
                    Button(onClick = {
                        selectedMode = TimeFrameMode.CUSTOM
                    }, Modifier.background(customButtonColor)) {
                        Text(text = "Custom")
                    }
                }
                if (selectedMode == TimeFrameMode.CUSTOM) {
                    TimeSelector(
                        label = "Start",
                        time = viewModel.timeFrame.first,
                        activity = activity,
                        visibility = true,
                    ) { time -> startTime = time }
                    TimeSelector(
                        label = "End",
                        time = viewModel.timeFrame.second,
                        activity = activity,
                        visibility = true,
                    )
                    { time -> endTime = time }
                }
                Row(modifier = Modifier.offset(0.dp, (30).dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(modifier = Modifier.weight(1f), onClick = { onDismissRequest() }) {
                        Text(text = "Close")
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(modifier = Modifier.weight(1f), onClick = {
                        onDismissRequest()
//                        with(
//                            activity.getSharedPreferences(
//                                activity.getString(R.string.prefence_file_key), Context.MODE_PRIVATE
//                            ).edit()
//                        ) {
//                            putLong("start_time", startTime.toEpochSecond(ZoneOffset.UTC))
//                            putLong("end_time", endTime.toEpochSecond(ZoneOffset.UTC))
//                            putInt("mode", selectedMode.ordinal)
//                            apply()
//                        }
                        onSubmitRequest(selectedMode, Pair(startTime, endTime))
                    }) {
                        Text(text = "Submit")
                    }
                }
            }
        }
    }
}


@Composable
fun TimeSelector(
    label: String,
    time: LocalDateTime,
    activity: Activity,
    visibility: Boolean,
    callback: ((LocalDateTime) -> Unit)
) {
    Column(modifier = Modifier.padding(2.dp)) {
        if (visibility) {

            val timeState = remember { mutableStateOf(time) }

            Text(
                "Select $label Time",
                modifier = Modifier.padding(8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(Modifier.padding(8.dp)) {

                Button(onClick = {
                    TimePickerDialog(
                        activity,
                        { _, hour, minute ->
                            callback(timeState.value.withHour(hour).withMinute(minute))
                        },
                        timeState.value.hour,
                        timeState.value.minute,
                        false
                    ).show()
                    Log.d("netUsage", "time picker is clicked")
                }, modifier = Modifier.padding(8.dp)) {
                    Text(timeState.value.format(DateTimeFormatter.ofPattern("HH:mm")))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    DatePickerDialog(
                        activity,
                        { _, year, month, dayOfMonth ->
                            callback(
                                timeState.value.withDayOfMonth(dayOfMonth).withMonth(month + 1)
                                    .withYear(year)
                            )
                            timeState.value =
                                timeState.value.withDayOfMonth(dayOfMonth).withMonth(month + 1)
                                    .withYear(year)
                        },
                        timeState.value.year,
                        timeState.value.month.ordinal,
                        timeState.value.dayOfMonth
                    ).show()
                    Log.d("netUsage", "non-implemented")
                }, modifier = Modifier.padding(8.dp)) {
                    Text(timeState.value.format(DateTimeFormatter.ISO_DATE))
                }
            }
        }
    }
}


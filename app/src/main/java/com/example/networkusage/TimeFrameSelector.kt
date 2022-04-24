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
    onSubmitRequest: (Pair<LocalDateTime, LocalDateTime>) -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        var startTime = remember {
            viewModel.timeFrame.first
        }
        var endTime = remember {
            viewModel.timeFrame.second
        }
        val scrollState = rememberScrollState()
//        var customSelectionVisibility by remember { mutableStateOf(mode == TimeFrameMode.CUSTOM) }
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier
                .height(400.dp)
                .padding(10.dp)) {
                    TimeSelector(
                        label = "Start",
                        time = startTime,
                        activity = activity,
                        visibility = true,
                    ) { time -> startTime = time }
                    TimeSelector(
                        label = "End",
                        time = endTime,
                        activity = activity,
                        visibility = true,
                    )
                    { time -> endTime = time }
                Row(modifier = Modifier.offset(0.dp, (30).dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(modifier = Modifier.weight(1f), onClick = { onDismissRequest() }) {
                        Text(text = "Close")
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(modifier = Modifier.weight(1f), onClick = {
                        onDismissRequest()
                        onSubmitRequest(Pair(startTime, endTime))
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


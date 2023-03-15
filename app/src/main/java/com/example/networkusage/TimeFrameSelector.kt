package com.example.networkusage

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.networkusage.ui.theme.NetworkUsageTheme
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


@Composable
fun TimeFrameSelector(
    activity: ComponentActivity,
    timeFrame: Pair<ZonedDateTime, ZonedDateTime>,
    mode: TimeFrameMode,
    onDismissRequest: () -> Unit,
    onSubmitRequest: (Pair<ZonedDateTime, ZonedDateTime>) -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        val viewModelTime = timeFrame
        var startTime = remember {
            viewModelTime.first
        }
        var endTime = remember {
            viewModelTime.second
        }
        Card(
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .height(400.dp)
                    .padding(10.dp)
            ) {
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
                Row(
                    modifier = Modifier.offset(0.dp, (30).dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
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
    time: ZonedDateTime,
    activity: Activity,
    visibility: Boolean,
    callback: ((ZonedDateTime) -> Unit)
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

@Preview(showBackground = true)
@Composable
fun PreviewTimeFrameSelector() {
    NetworkUsageTheme {
        TimeFrameSelector(
            activity = LocalContext.current as ComponentActivity,
            timeFrame = Pair(ZonedDateTime.now(), ZonedDateTime.now()),
            mode = TimeFrameMode.LAST_30_DAYS,
            onDismissRequest = {
                Log.d(
                    "Network Usage",
                    "Custom TimeFrameSelector dismiss clicked."
                )
            },
            onSubmitRequest = { timeframe ->
                Log.d(
                    "Network Usage",
                    "Custom TimeFrameSelector submit clicked ${timeframe.first}, ${timeframe.second}"
                )
            }
        )
    }
}


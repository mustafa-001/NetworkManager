package com.example.networkusage

import android.app.usage.NetworkStats
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun toIntervals(
    buckets: List<NetworkStats.Bucket>,
): MutableList<UsageInterval> {
    val bucketsToAdd = mutableListOf<UsageInterval>()
    for (b in buckets) {
        bucketsToAdd.add(
            UsageInterval(
                b.rxBytes,
                b.txBytes,
                Instant.ofEpochMilli(b.startTimeStamp).atZone(ZoneId.systemDefault()),
                Instant.ofEpochMilli(b.endTimeStamp).atZone(ZoneId.systemDefault())
            )
        )
    }
    return bucketsToAdd.apply { sortBy { it.start.toEpochSecond() } }
}

fun MutableList<UsageInterval>.fillEmptyIntervals(
    timeFrame: Pair<ZonedDateTime, ZonedDateTime>
): MutableList<UsageInterval> {

    val timeStampDifference = 7200000000 //buckets[0].endTimeStamp - buckets[0].startTimeStamp
    var firstTimeStamp = (timeFrame.first.toEpochSecond() * 1000)
        .floorDiv(timeStampDifference) * timeStampDifference
    val lastTimeStamp = (timeFrame.first.toEpochSecond() * 1000
        .div(timeStampDifference) + 1) * timeStampDifference
    while (firstTimeStamp < lastTimeStamp) {
        this.add(
            UsageInterval(
                0,
                0,

                Instant.ofEpochMilli(firstTimeStamp).atZone(ZoneId.systemDefault()),
                Instant.ofEpochMilli(firstTimeStamp + timeStampDifference).atZone(ZoneId.systemDefault()),
//                ZonedDateTime.ofEpochSecond(
//                    firstTimeStamp / 1000 + timeStampDifference,
//                    0,
//                    ZoneOffset.UTC
                )
            )
        firstTimeStamp += timeStampDifference
    }
    return this
}

@Composable
fun UsageDetailsForUID(
    usageDetailsManager: UsageDetailsManager,
    uid: Int,
    timeFrame: LiveData<Pair<ZonedDateTime, ZonedDateTime>>
) {
    val time by timeFrame.observeAsState(
        Pair(
            ZonedDateTime.now().withDayOfYear(1),
            ZonedDateTime.now()
        )
    )
    LazyColumn(content = {
        val buckets = usageDetailsManager.queryForUid(uid, time)
        item {
            BasicPlot(intervals = toIntervals(buckets).fillEmptyIntervals(time))
        }
        for (bucket in buckets) {
            item {
                Column(
                    Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text =
                            Instant.ofEpochMilli(bucket.startTimeStamp).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), modifier = Modifier.padding(2.dp)
                    )
                    Text(
                        text =
                            Instant.ofEpochMilli(bucket.endTimeStamp).atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), modifier = Modifier.padding(2.dp)
                    )
                    Text(
                        text = byteToStringRepresentation(bucket.rxBytes),
                        modifier = Modifier.padding(2.dp)
                    )
                    Text(
                        text = byteToStringRepresentation(bucket.txBytes),
                        modifier = Modifier.padding(2.dp)
                    )
                    Text(text = "UID: " + bucket.uid.toString(), modifier = Modifier.padding(2.dp))
                    Text(
                        text = "state: " + bucket.state.toString(),
                        modifier = Modifier.padding(2.dp)
                    )
                    Text(text = "Tag:\t" + bucket.tag.toString(), modifier = Modifier.padding(2.dp))
                }
            }
        }
    }
    )
}
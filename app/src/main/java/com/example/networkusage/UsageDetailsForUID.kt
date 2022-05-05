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
import java.sql.Time
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun insertEmptyBuckets(
    buckets: List<NetworkStats.Bucket>,
    timeFrame: Pair<LocalDateTime, LocalDateTime>
): MutableList<UsagePoint> {
    val timeStampDifference = 7200000 //buckets[0].endTimeStamp - buckets[0].startTimeStamp
    var firstTimeStamp = (timeFrame.first.toEpochSecond(ZoneOffset.UTC)*1000)
        .floorDiv(timeStampDifference) * timeStampDifference
    val lastTimeStamp = (timeFrame.first.toEpochSecond(ZoneOffset.UTC)*1000
        .div(timeStampDifference) + 1) * timeStampDifference
    val bucketsToAdd = mutableListOf<UsagePoint>()
    for (b in buckets) {
        bucketsToAdd.add(
            UsagePoint(
                b.rxBytes,
                b.txBytes,
                LocalDateTime.ofEpochSecond(b.startTimeStamp/1000, 0, ZoneOffset.UTC),
                LocalDateTime.ofEpochSecond(b.endTimeStamp/1000, 0, ZoneOffset.UTC)
            )
        )
    }
    while (firstTimeStamp < lastTimeStamp) {
        bucketsToAdd.add(
            UsagePoint(
                0,
                0,
                LocalDateTime.ofEpochSecond(firstTimeStamp/1000, 0, ZoneOffset.UTC),
                LocalDateTime.ofEpochSecond(firstTimeStamp/1000 + timeStampDifference, 0, ZoneOffset.UTC)
            )
        )
        firstTimeStamp += timeStampDifference
    }
    return bucketsToAdd.apply {  sortBy { it.start.toEpochSecond(ZoneOffset.UTC) }}
}

@Composable
fun UsageDetailsForUID(
    usageDetailsManager: UsageDetailsManager,
    uid: Int,
    timeFrame: LiveData<Pair<LocalDateTime, LocalDateTime>>
) {
    val time by timeFrame.observeAsState(Pair(LocalDateTime.now().withDayOfYear(1), LocalDateTime.now()))
    LazyColumn(content = {
        val buckets = usageDetailsManager.queryForUid(uid, time)
        item {
            BasicPlot(points = insertEmptyBuckets(buckets, time ))
        }
        for (bucket in buckets) {
            item {
                Column(
                    Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(bucket.startTimeStamp),
                            ZoneOffset.UTC
                        ).toString(), modifier = Modifier.padding(2.dp)
                    )
                    Text(
                        text = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(bucket.endTimeStamp),
                            ZoneOffset.UTC
                        ).toString(), modifier = Modifier.padding(2.dp)
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
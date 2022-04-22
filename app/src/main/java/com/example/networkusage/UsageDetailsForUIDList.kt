package com.example.networkusage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun UsageDetailsForUIDList(usageDetailsManager: UsageDetailsManager, uid: Int) {
    LazyColumn(content = {
        for (bucket in usageDetailsManager.queryForUid(uid)) {
            item {
                Column(Modifier.padding(8.dp)) {
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
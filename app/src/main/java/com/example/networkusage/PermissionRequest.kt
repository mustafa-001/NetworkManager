package com.example.networkusage

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.networkusage.ui.theme.NetworkUsageTheme

@Composable
    fun RequestDialog(onDismissCallback: () -> Unit, onConfirmCallback: ()->Unit) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                "This app requires System Usage Stats permission to query your usage data from system."
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.SpaceAround) {
                Button(onClick = onDismissCallback) {
                    Text("Dismiss")
                }
                Spacer(Modifier.width(10.dp))
                Button(onClick = onConfirmCallback, Modifier.weight(1f)) {
                    Text("Go to System Settings.")
                }
            }
        }

    }

    @Preview
    @Composable
    fun RequestDialogPreview() {
        NetworkUsageTheme() {
            Card() {
                RequestDialog({}, {})
            }
        }
    }
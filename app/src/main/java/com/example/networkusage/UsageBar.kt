package com.example.networkusage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun UsageBar(rx: Long, tx: Long, biggestTotal: Long) {
    val sum: Float = (rx + tx).toFloat()
    val fillRatio = sum / biggestTotal

    BoxWithConstraints(
        Modifier
            .height(10.dp)
            .fillMaxWidth()
            .background(Color.Unspecified)
    ) {
        Row() {
            Box(
                modifier =
                Modifier
                    .size(
                        width = this@BoxWithConstraints.maxWidth
                            .times(fillRatio * rx / sum),
                        height = this@BoxWithConstraints.maxHeight
                    )
                    .background(Color.Green)
            ) {
                Text(text = "")
            }
            Box(
                modifier =
                Modifier
                    .size(
                        width = this@BoxWithConstraints.maxWidth
                            .times(fillRatio * tx / sum),
                        height = this@BoxWithConstraints.maxHeight
                    )
                    .background(Color.Red)
            ) {
                Text(text = "")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewUsageBar() {
    Card() {
        UsageBar(1000, 100, 2000)
    }
}
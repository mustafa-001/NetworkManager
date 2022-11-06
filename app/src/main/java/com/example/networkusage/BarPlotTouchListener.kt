package com.example.networkusage

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class BarPlotTouchListener() : OnChartValueSelectedListener {
    private val intervalMutable =
        MutableLiveData(Pair(
            ZonedDateTime.now().withDayOfYear(1),
            ZonedDateTime.now()
        ))
   val interval = intervalMutable as LiveData<Pair<ZonedDateTime, ZonedDateTime>>

    /**
     * Called when a value has been selected inside the chart.
     *
     * @param e The selected Entry.
     * @param h The corresponding highlight object that contains information
     * about the highlighted position
     */
    override fun onValueSelected(e: Entry?, h: Highlight?) {
        intervalMutable.value =
            Pair(
                ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(h!!.x.toLong() - 60 * 60 * 2),
                    ZoneId.systemDefault()
                ),
                ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(h.x.toLong() + 60 * 60 * 2),
                    ZoneId.systemDefault()
                )
            )
        Log.d(
            "NetworkUsage", "Bar plot, a value selected" +
                    "entry: ${e}" +
                    "highlight: $h" +
                    "highlight data index: ${h!!.dataIndex} " +
                    "highlight x value: ${h.x}" +
                    "highlight y value: ${h.y}+" +
                    "\n ${interval.value!!.first}"
        )
    }

    /**
     * Called when nothing has been selected or an "un-select" has been made.
     */
    override fun onNothingSelected() {
        Log.d("NetworkUsage", "Nothing selected")
    }
}
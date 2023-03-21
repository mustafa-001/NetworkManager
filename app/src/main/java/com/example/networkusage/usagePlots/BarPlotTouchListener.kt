package com.example.networkusage.usagePlots

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.util.Optional
import kotlin.math.roundToInt

class BarPlotTouchListener() : OnChartValueSelectedListener {
    private val intervalIndexMutable =
        MutableLiveData(
            Optional.empty<Int>()
        )
    val intervalIndex = intervalIndexMutable as LiveData<Optional<Int>>

    /**
     * Called when a value has been selected inside the chart.
     *
     * @param e The selected Entry.
     * @param h The corresponding highlight object that contains information
     * about the highlighted position
     */
    override fun onValueSelected(e: Entry?, h: Highlight?) {
        intervalIndexMutable.value = Optional.of(e!!.x.roundToInt())
        Log.d(
            "NetworkUsage", "Bar plot, a value selected" +
                    "entry: ${e}" +
                    "highlight: $h" +
                    "highlight data index: ${h!!.dataIndex} " +
                    "highlight x value: ${h.x}" +
                    "highlight y value: ${h.y}+"
        )
    }

    /**
     * Called when nothing has been selected or an "un-select" has been made.
     */
    override fun onNothingSelected() {
        intervalIndexMutable.value = Optional.empty()
        Log.d(
            "NetworkUsage", "Bar plot selected value \"un-selected\", " +
                    "selected interval resetted to maximum range."
        )
    }
}
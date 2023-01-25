package com.example.networkusage.usage_details_processor

import android.graphics.drawable.Drawable

//TODO Refactor nullables to optional.
data class AppUsageInfo(
    val uid: Int,
    val name: String?,
    val packageName: String,
    var txBytes: Long,
    var rxBytes: Long,
    var icon: Drawable?,
)
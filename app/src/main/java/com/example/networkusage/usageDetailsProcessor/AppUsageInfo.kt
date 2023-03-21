package com.example.networkusage.usageDetailsProcessor

import android.graphics.drawable.Drawable

//TODO Refactor nullables to optional.
//TODO Refactor altogether to use GeneralUsageInfo
data class AppUsageInfo(
    val uid: Int,
    val name: String?,
    val packageName: String,
    var txBytes: Long,
    var rxBytes: Long,
    var icon: Drawable?,
)
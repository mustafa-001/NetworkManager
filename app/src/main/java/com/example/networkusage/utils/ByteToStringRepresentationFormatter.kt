package com.example.networkusage

fun byteToStringRepresentation(bytes: Long): String {
    return (when (bytes) {
       in 0..1023 ->  "%.2f B".format(bytes.toFloat())
        in 1024 until 1024*1024 -> "%.2f KiB".format((bytes.toFloat())/1024)
        in 1024*1024 until 1024*1024*1024 -> "%.2f MiB".format(bytes.toFloat()/(1024*1024))
        else -> "%.2f GiB".format(bytes.toFloat()/(1024*1024*1024))
    })

}
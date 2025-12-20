package io.kitsuri.m1rage.model

import android.graphics.drawable.Drawable
import java.io.File

data class AppLogInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val logFile: File
)
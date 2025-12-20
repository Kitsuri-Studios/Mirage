package io.kitsuri.m1rage.model

import android.graphics.drawable.Drawable

data class PatchedAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable
)
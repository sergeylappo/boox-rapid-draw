package com.sergeylappo.booxrapiddraw.utils

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService

// Deprecation is not a problem for own services
@Suppress("DEPRECATION")
fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val activityManager = getSystemService(context, ActivityManager::class.java)!!
    return activityManager.getRunningServices(Int.MAX_VALUE).asSequence()
        .map { it.service.className }
        .filter { it == serviceClass.name }
        .any()
}

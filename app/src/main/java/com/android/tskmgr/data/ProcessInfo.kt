package com.android.tskmgr.data

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

/** A running application process presented in the Processes tab. */
data class ProcessInfo(
    val pid: Int,
    val uid: Int,
    val processName: String,
    val appLabel: String,
    val appIcon: Drawable?,
    val importance: String,
    val memoryKb: Long,
    val isSystem: Boolean,
    val canKill: Boolean,
)

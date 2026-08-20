package com.android.tskmgr.data

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** Helpers for the "Usage access" special permission. */
object PermissionsHelper {

    fun hasUsageAccess(context: Context): Boolean = try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        false
    }

    /** Open the system "Usage access" settings screen for this app. */
    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallback)
        }
    }
}

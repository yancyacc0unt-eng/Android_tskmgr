package com.android.tskmgr.data

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * Reads the list of running processes. On modern Android (API 31+ without root)
 * a normal app only sees a limited set: its own process plus a handful of
 * others returned by getRunningAppProcesses, and app-level usage via
 * UsageStatsManager. This mirrors exactly the data the platform exposes.
 */
class ProcessRepository(private val context: Context) {

    private val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val pm = context.packageManager

    fun getRunningProcesses(): List<ProcessInfo> {
        val running = try {
            am.runningAppProcesses.orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
        val pids = running.map { it.pid }.toSet()

        return running
            .distinctBy { it.processName }
            .mapNotNull { rp ->
                try {
                    val info = pm.getApplicationInfo(rp.processName, 0)
                    val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    ProcessInfo(
                        pid = rp.pid,
                        uid = rp.uid,
                        processName = rp.processName,
                        appLabel = try { info.loadLabel(pm).toString() } catch (e: Exception) { rp.processName },
                        appIcon = try { info.loadIcon(pm) } catch (e: Exception) { null },
                        importance = importanceToString(rp.importance),
                        memoryKb = estimateMemoryKb(rp.pid),
                        isSystem = isSystem,
                        canKill = rp.pid == android.os.Process.myPid() || !isSystem,
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedWith(compareBy({ it.isSystem }, { -it.memoryKb }))
    }

    /** Attempt to stop a background process. Only works for processes the OS permits. */
    fun killProcess(processName: String, pid: Int): KillResult {
        return try {
            am.killBackgroundProcesses(processName)
            KillResult.SUCCESS
        } catch (e: SecurityException) {
            KillResult.PERMISSION_DENIED
        } catch (e: Exception) {
            KillResult.FAILED
        }
    }

    /**
     * Recently used apps via UsageStatsManager. On Android 12+ the platform
     * hides most running processes, so this is the only way to show a fuller
     * "what the user actually runs" list. Requires PACKAGE_USAGE_STATS.
     * Entries that were active in the last minutes sort first.
     */
    fun getRecentApps(runningProcessNames: Set<String>, limit: Int = 30): List<RecentAppInfo> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = try {
            usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_BEST, now - 7L * 24 * 3600_000, now)
        } catch (e: Exception) {
            emptyList()
        }
        val activePackages = recentlyActivePackages(usm, now)
        return stats
            .filter { it.lastTimeUsed > 0 }
            .sortedWith(
                compareByDescending<android.app.usage.UsageStats> { it.packageName in activePackages }
                    .thenByDescending { it.lastTimeUsed },
            )
            .take(limit)
            .mapNotNull { s ->
                val info = try { pm.getApplicationInfo(s.packageName, 0) } catch (e: Exception) { return@mapNotNull null }
                RecentAppInfo(
                    packageName = s.packageName,
                    appLabel = try { info.loadLabel(pm).toString() } catch (e: Exception) { s.packageName },
                    appIcon = try { info.loadIcon(pm) } catch (e: Exception) { null },
                    lastUsedMillis = s.lastTimeUsed,
                    isRunning = s.packageName in runningProcessNames,
                    isActive = s.packageName in activePackages,
                )
            }
    }

    /** Packages with an ACTIVITY_RESUMED event within the last [windowMs]. */
    private fun recentlyActivePackages(
        usm: android.app.usage.UsageStatsManager,
        now: Long,
        windowMs: Long = 10 * 60_000,
    ): Set<String> {
        return try {
            val events = usm.queryEvents(now - windowMs, now)
            val event = android.app.usage.UsageEvents.Event()
            val active = mutableSetOf<String>()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    if (!event.packageName.isNullOrEmpty()) active.add(event.packageName)
                }
            }
            active
        } catch (e: Exception) {
            emptySet()
        }
    }

    enum class KillResult { SUCCESS, PERMISSION_DENIED, FAILED }

    private fun estimateMemoryKb(pid: Int): Long = try {
        am.getProcessMemoryInfo(intArrayOf(pid)).firstOrNull()?.totalPss?.toLong() ?: 0L
    } catch (e: Exception) {
        0L
    }

    private fun importanceToString(imp: Int): String = when (imp) {
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "Foreground"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "Visible"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "Service"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "Cached"
        else -> "Unknown"
    }
}

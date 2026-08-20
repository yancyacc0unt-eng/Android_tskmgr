package com.android.tskmgr.data

import android.app.ActivityManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Environment
import android.os.StatFs

/**
 * Reads system-wide performance metrics available to a normal (non-root) app.
 *
 * Platform reality on Android 12+ (API 31+):
 *  - /proc/stat is EACCES for non-root apps (SELinux), so system CPU % is not
 *    available; /proc/loadavg is attempted as a fallback.
 *  - /sys/class/net stats and TrafficStats totals are restricted since Android 8,
 *    so network uses NetworkStatsManager (needs PACKAGE_USAGE_STATS).
 *  - /proc/meminfo and StatFs remain readable.
 */
object SystemMetrics {

    data class CpuUsage(
        /** System CPU % (0..100 per core averaged); null when /proc/stat is not readable. */
        val totalPercent: Double?,
        /** Per-core percentages; index 0 is the aggregate line (drop before display). */
        val perCore: List<Double>,
        /** 1-minute load average from /proc/loadavg; null when not readable. */
        val loadAvg: Double?,
    )

    data class MemoryInfo(
        val totalMb: Long,
        val usedMb: Long,
        val freeMb: Long,
        val cachedMb: Long,
        val usedPercent: Double,
    )

    data class StorageInfo(val totalBytes: Long, val usedBytes: Long, val freeBytes: Long)

    data class NetworkInfo(val rxBytesPerSec: Long, val txBytesPerSec: Long, val available: Boolean)

    /**
     * CPU usage. Tries /proc/stat first (works on a few permissive ROMs),
     * falls back to /proc/loadavg. Never throws.
     */
    fun readCpuUsage(): CpuUsage? = try {
        val prev = readCpuTicks()
        Thread.sleep(120)
        val curr = readCpuTicks()
        val usage = computeCpu(prev, curr)
        CpuUsage(usage.totalPercent, usage.perCore, null)
    } catch (e: Exception) {
        // /proc/stat is restricted on Android 12+ → try load average
        CpuUsage(null, emptyList(), readLoadAverage())
    }

    private data class CpuTicks(val cores: List<LongArray>)

    private fun readCpuTicks(): CpuTicks {
        val lines = java.io.File("/proc/stat").readLines()
        val cores = ArrayList<LongArray>()
        for (line in lines) {
            if (!line.startsWith("cpu")) continue
            val parts = line.split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
            if (parts.isEmpty()) continue
            // parts: user nice system idle iowait irq softirq steal
            val idle = (parts.getOrElse(3) { 0L }) + (parts.getOrElse(4) { 0L }) // idle + iowait
            val total = parts.sum()
            cores.add(longArrayOf(idle, total))
        }
        return CpuTicks(cores)
    }

    private fun computeCpu(prev: CpuTicks, curr: CpuTicks): CpuUsage {
        val perCore = ArrayList<Double>()
        for (i in 0 until minOf(prev.cores.size, curr.cores.size)) {
            val p = prev.cores[i]
            val c = curr.cores[i]
            val idleDelta = c[0] - p[0]
            val totalDelta = c[1] - p[1]
            val pct = if (totalDelta <= 0) 0.0 else ((totalDelta - idleDelta).toDouble() / totalDelta) * 100.0
            perCore.add(pct.coerceIn(0.0, 100.0))
        }
        val total = if (perCore.size > 1) perCore.drop(1).average() else perCore.firstOrNull() ?: 0.0
        return CpuUsage(total.coerceIn(0.0, 100.0), perCore, null)
    }

    /** 1-minute load average; null when not readable. */
    private fun readLoadAverage(): Double? = try {
        val fields = java.io.File("/proc/loadavg").readText().trim().split(Regex("\\s+"))
        fields.firstOrNull()?.toDoubleOrNull()
    } catch (e: Exception) {
        null
    }

    /** Memory usage from ActivityManager.MemoryInfo and /proc/meminfo. */
    fun readMemory(context: Context): MemoryInfo? = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val total = mi.totalMem
        val avail = mi.availMem
        val used = total - avail
        val cached = readMeminfoValue("Cached:") ?: 0L
        MemoryInfo(
            totalMb = total / 1048576L,
            usedMb = used / 1048576L,
            freeMb = avail / 1048576L,
            cachedMb = cached / 1048576L,
            usedPercent = if (total > 0) (used.toDouble() / total) * 100.0 else 0.0,
        )
    } catch (e: Exception) {
        null
    }

    private fun readMeminfoValue(key: String): Long? = try {
        val line = java.io.File("/proc/meminfo").readLines().firstOrNull { it.startsWith(key) } ?: return null
        val value = line.split(":")[1].trim().split(Regex("\\s+"))[0].toLongOrNull() ?: return null
        value * 1024 // kB -> bytes
    } catch (e: Exception) {
        null
    }

    /** Storage usage for the shared primary external storage. */
    fun readStorage(): StorageInfo? = try {
        val path = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        StorageInfo(total, total - free, free)
    } catch (e: Exception) {
        null
    }

    /**
     * Network byte counters since the previous call. Uses TrafficStats totals
     * when available, otherwise NetworkStatsManager (requires usage access).
     */
    fun readNetwork(context: Context, prevRx: Long, prevTx: Long): NetworkInfo {
        val cur = queryTotal(context)
        if (cur == null) return NetworkInfo(0, 0, available = false)
        return NetworkInfo(
            rxBytesPerSec = (cur.first - prevRx).coerceAtLeast(0),
            txBytesPerSec = (cur.second - prevTx).coerceAtLeast(0),
            available = true,
        )
    }

    /** Current cumulative RX/TX bytes, or null when no source is readable. */
    private fun queryTotal(context: Context): Pair<Long, Long>? {
        // 1) TrafficStats totals still work on some devices/versions.
        try {
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            if (rx > 0 && tx > 0) return rx to tx
        } catch (e: Exception) {
            // ignore, try next source
        }

        // 2) NetworkStatsManager — official API, needs PACKAGE_USAGE_STATS.
        try {
            val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
            val end = System.currentTimeMillis()
            val start = end - 120_000
            var rx = 0L
            var tx = 0L
            var got = false
            runCatching { nsm.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, start, end) }
                .onSuccess { b -> rx += b.rxBytes; tx += b.txBytes; got = true }
            runCatching { nsm.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, start, end) }
                .onSuccess { b -> rx += b.rxBytes; tx += b.txBytes; got = true }
            if (got) return rx to tx
        } catch (e: Exception) {
            // no usage access or other failure
        }
        return null
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var i = 0
        while (value >= 1024 && i < units.size - 1) {
            value /= 1024
            i++
        }
        return if (i == 0) "${bytes} B" else String.format("%.1f %s", value, units[i])
    }
}

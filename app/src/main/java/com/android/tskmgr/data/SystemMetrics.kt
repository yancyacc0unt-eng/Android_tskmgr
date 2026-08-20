package com.android.tskmgr.data

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs

/**
 * Reads system-wide performance metrics available to a normal (non-root) app.
 * Windows-Task-Manager-style data, sourced from /proc and Android APIs.
 */
object SystemMetrics {

    data class CpuUsage(val totalPercent: Double, val perCore: List<Double>)

    data class MemoryInfo(
        val totalMb: Long,
        val usedMb: Long,
        val freeMb: Long,
        val cachedMb: Long,
        val usedPercent: Double,
    )

    data class StorageInfo(val totalBytes: Long, val usedBytes: Long, val freeBytes: Long)

    data class NetworkInfo(val rxBytesPerSec: Long, val txBytesPerSec: Long)

    /** CPU usage from /proc/stat. Returns null if reading fails. */
    fun readCpuUsage(): CpuUsage? = try {
        val prev = readCpuTicks()
        Thread.sleep(120)
        val curr = readCpuTicks()
        computeCpu(prev, curr)
    } catch (e: Exception) {
        null
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
        val total = if (perCore.isEmpty()) 0.0 else perCore.drop(1).average()
        return CpuUsage(total, perCore)
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

    /** Network traffic delta measured between calls. */
    fun readNetwork(rx: Long, tx: Long): NetworkInfo = try {
        val rxNow = readTraffic("wlan0", 0L) + readTraffic("rmnet0", 0L) + readTraffic("eth0", 0L)
        val txNow = readTrafficTx("wlan0", 0L) + readTrafficTx("rmnet0", 0L) + readTrafficTx("eth0", 0L)
        NetworkInfo((rxNow - rx).coerceAtLeast(0), (txNow - tx).coerceAtLeast(0))
    } catch (e: Exception) {
        NetworkInfo(0, 0)
    }

    private fun readTraffic(iface: String, default: Long): Long =
        readLongFile("/sys/class/net/$iface/statistics/rx_bytes") ?: default

    private fun readTrafficTx(iface: String, default: Long): Long =
        readLongFile("/sys/class/net/$iface/statistics/tx_bytes") ?: default

    private fun readLongFile(path: String): Long? = try {
        java.io.File(path).readText().trim().toLongOrNull()
    } catch (e: Exception) {
        null
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

package com.topster.tv.utils

import android.os.Build
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.system.measureTimeMillis

/**
 * Performance monitoring utility
 * Tracks app performance metrics for optimization
 */
class PerformanceMonitor {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val metrics = mutableMapOf<String, MetricData>()

    companion object {
        private const val TAG = "PerformanceMonitor"

        @Volatile
        private var instance: PerformanceMonitor? = null

        fun getInstance(): PerformanceMonitor {
            return instance ?: synchronized(this) {
                instance ?: PerformanceMonitor().also { instance = it }
            }
        }
    }

    data class MetricData(
        val name: String,
        var count: Long = 0,
        var totalTime: Long = 0,
        var minTime: Long = Long.MAX_VALUE,
        var maxTime: Long = 0,
        var avgTime: Long = 0
    ) {
        fun update(time: Long) {
            count++
            totalTime += time
            minTime = minOf(minTime, time)
            maxTime = maxOf(maxTime, time)
            avgTime = totalTime / count
        }
    }

    /**
     * Measure execution time of a block
     */
    inline fun <T> measure(name: String, block: () -> T): T {
        val time = measureTimeMillis {
            return block()
        }

        recordMetric(name, time)
        return block()
    }

    /**
     * Measure suspend function execution time
     */
    suspend inline fun <T> measureSuspend(name: String, crossinline block: suspend () -> T): T {
        val time = measureTimeMillis {
            return block()
        }

        recordMetric(name, time)
        return block()
    }

    /**
     * Record a metric
     */
    fun recordMetric(name: String, timeMs: Long) {
        val metric = metrics.getOrPut(name) { MetricData(name) }
        metric.update(timeMs)

        // Always log metrics for debugging
        Log.d(TAG, "$name: ${timeMs}ms (avg: ${metric.avgTime}ms, min: ${metric.minTime}ms, max: ${metric.maxTime}ms)")
    }

    /**
     * Get memory usage
     */
    fun getMemoryUsage(): MemoryUsage {
        val runtime = Runtime.getRuntime()
        val nativeHeap = Debug.getNativeHeapAllocatedSize()

        return MemoryUsage(
            totalMemoryMB = runtime.totalMemory() / (1024 * 1024),
            freeMemoryMB = runtime.freeMemory() / (1024 * 1024),
            usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024),
            maxMemoryMB = runtime.maxMemory() / (1024 * 1024),
            nativeHeapMB = nativeHeap / (1024 * 1024)
        )
    }

    /**
     * Log memory usage
     */
    fun logMemoryUsage() {
        val usage = getMemoryUsage()
        Log.d(TAG, "Memory: ${usage.usedMemoryMB}MB / ${usage.maxMemoryMB}MB " +
                "(free: ${usage.freeMemoryMB}MB, native: ${usage.nativeHeapMB}MB)")
    }

    /**
     * Get all metrics
     */
    fun getAllMetrics(): Map<String, MetricData> = metrics.toMap()

    /**
     * Clear all metrics
     */
    fun clearMetrics() {
        metrics.clear()
    }

    /**
     * Start periodic memory logging
     */
    fun startMemoryLogging(intervalMs: Long = 30000) {
        scope.launch {
            while (isActive) {
                logMemoryUsage()
                delay(intervalMs)
            }
        }
    }

    /**
     * Stop monitoring
     */
    fun stop() {
        scope.cancel()
    }
}

data class MemoryUsage(
    val totalMemoryMB: Long,
    val freeMemoryMB: Long,
    val usedMemoryMB: Long,
    val maxMemoryMB: Long,
    val nativeHeapMB: Long
)

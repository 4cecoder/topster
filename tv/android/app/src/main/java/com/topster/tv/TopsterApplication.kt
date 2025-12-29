package com.topster.tv

import android.app.Application
import android.util.Log
import coil.ImageLoader
import com.topster.tv.database.HistoryManager
import com.topster.tv.network.DeviceRegistration
import com.topster.tv.ota.OTAServer
import com.topster.tv.scraper.FlixHQScraper
import com.topster.tv.scraper.IMDbScraper
import com.topster.tv.utils.ImageCache
import com.topster.tv.utils.PerformanceMonitor
import com.topster.tv.utils.Prefetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Enhanced Topster Application with production-grade initialization
 * Following SmartTube's application setup patterns
 */
class TopsterApplication : Application() {

    lateinit var historyManager: HistoryManager
        private set

    lateinit var imageLoader: ImageLoader
        private set

    lateinit var prefetcher: Prefetcher
        private set

    lateinit var scraper: FlixHQScraper
        private set

    lateinit var imdbScraper: IMDbScraper
        private set

    private var otaServer: OTAServer? = null
    private var deviceRegistration: DeviceRegistration? = null
    private val performanceMonitor = PerformanceMonitor.getInstance()

    private val applicationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val TAG = "TopsterApplication"
        private const val WEB_APP_URL = "http://192.168.1.100:3000" // Configure as needed
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Topster TV Application starting...")

        // Initialize core components
        performanceMonitor.measure("app_initialization") {
            initializeCoreComponents()
        }

        // Start background services
        startBackgroundServices()

        // Start performance monitoring (in debug builds, this logs every 30s)
        performanceMonitor.startMemoryLogging()

        Log.i(TAG, "Application initialized successfully")
    }

    /**
     * Initialize core components
     */
    private fun initializeCoreComponents() {
        // History manager
        historyManager = HistoryManager(this)
        Log.d(TAG, "History manager initialized")

        // Image loader with optimized caching
        imageLoader = ImageCache.createImageLoader(this)
        Log.d(TAG, "Image loader initialized")

        // Content prefetcher
        prefetcher = Prefetcher(this, imageLoader)
        Log.d(TAG, "Prefetcher initialized")

        // Media scraper
        scraper = FlixHQScraper()
        Log.d(TAG, "FlixHQ scraper initialized")

        // IMDb scraper
        imdbScraper = IMDbScraper()
        Log.d(TAG, "IMDb scraper initialized")
    }

    /**
     * Start background services
     */
    private fun startBackgroundServices() {
        // Start OTA server for network updates
        try {
            otaServer = OTAServer(this)
            otaServer?.startServer()
            Log.i(TAG, "OTA Server started on port 8765")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start OTA Server", e)
        }

        // Register device with web app and start heartbeat
        applicationScope.launch {
            try {
                deviceRegistration = DeviceRegistration(this@TopsterApplication, WEB_APP_URL)

                val registered = deviceRegistration?.register() ?: false
                if (registered) {
                    deviceRegistration?.startHeartbeat()
                    Log.i(TAG, "Device registered with web app")
                } else {
                    Log.w(TAG, "Failed to register device (web app may be offline)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Device registration error", e)
            }
        }

        // Prefetch trending content in background
        applicationScope.launch {
            try {
                prefetcher.prefetchTrending()
                Log.d(TAG, "Trending content prefetched")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prefetch trending", e)
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        Log.d(TAG, "onTrimMemory: level=$level")

        when (level) {
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                // Clear image cache to free memory
                ImageCache.clearCache(imageLoader)
                Log.i(TAG, "Cleared image cache due to memory pressure")
            }
            TRIM_MEMORY_UI_HIDDEN -> {
                // App UI is hidden, clear non-essential caches
                prefetcher.clear()
                Log.d(TAG, "Cleared prefetch cache (UI hidden)")
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning!")

        // Aggressive memory cleanup
        ImageCache.clearCache(imageLoader)
        prefetcher.clear()
        System.gc()
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.i(TAG, "Application terminating...")

        // Cleanup all resources
        otaServer?.stopServer()
        deviceRegistration?.cleanup()
        prefetcher.cleanup()
        performanceMonitor.stop()
    }
}

// Extension property for easy access from anywhere
val Application.topsterApp: TopsterApplication
    get() = this as TopsterApplication

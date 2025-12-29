package com.topster.tv.network

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

/**
 * Handles device registration with Topster Web App
 * Implements heartbeat mechanism for device discovery
 */
class DeviceRegistration(
    private val context: Context,
    private val webAppUrl: String = "http://192.168.1.100:3000"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null

    companion object {
        private const val TAG = "DeviceRegistration"
        private const val HEARTBEAT_INTERVAL_MS = 60_000L // 60 seconds
        private const val OTA_SERVER_PORT = 8765
    }

    data class DeviceInfo(
        val id: String,
        val name: String,
        val ip: String,
        val port: Int
    )

    data class HeartbeatRequest(
        val deviceId: String
    )

    /**
     * Register device with web app
     */
    suspend fun register(): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceInfo = DeviceInfo(
                id = getDeviceId(),
                name = getDeviceName(),
                ip = getLocalIPAddress() ?: "unknown",
                port = OTA_SERVER_PORT
            )

            val requestBody = gson.toJson(deviceInfo)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$webAppUrl/api/tv/devices")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Log.i(TAG, "Device registered: ${deviceInfo.name} at ${deviceInfo.ip}")
                true
            } else {
                Log.w(TAG, "Registration failed: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device", e)
            false
        }
    }

    /**
     * Start periodic heartbeat to web app
     */
    fun startHeartbeat() {
        stopHeartbeat()

        heartbeatJob = scope.launch {
            while (isActive) {
                sendHeartbeat()
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }

        Log.i(TAG, "Heartbeat started")
    }

    /**
     * Stop heartbeat
     */
    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        Log.i(TAG, "Heartbeat stopped")
    }

    /**
     * Send single heartbeat
     */
    private suspend fun sendHeartbeat() {
        try {
            val heartbeat = HeartbeatRequest(deviceId = getDeviceId())

            val requestBody = gson.toJson(heartbeat)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$webAppUrl/api/tv/heartbeat")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Log.d(TAG, "Heartbeat sent successfully")
            } else {
                Log.w(TAG, "Heartbeat failed: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send heartbeat", e)
        }
    }

    /**
     * Get unique device ID (Android ID)
     */
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown-${System.currentTimeMillis()}"
    }

    /**
     * Get device name
     */
    private fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    /**
     * Get local IP address on WiFi/Ethernet
     */
    private fun getLocalIPAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()

                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback || !networkInterface.isUp) {
                    continue
                }

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()

                    // Return first IPv4 address that's not loopback
                    if (!address.isLoopbackAddress && address.address.size == 4) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get local IP address", e)
        }

        return null
    }

    /**
     * Cleanup on app termination
     */
    fun cleanup() {
        stopHeartbeat()
        scope.cancel()
    }
}

package com.topster.tv.ota

import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface

private const val TAG = "OTAServer"
private const val SERVICE_TYPE = "_topster._tcp"
private const val SERVICE_NAME = "Topster TV"
private const val OTA_PORT = 8888

/**
 * OTA Update Server - Allows remote updates over LAN
 *
 * Features:
 * - mDNS service discovery
 * - APK upload and auto-install
 * - Configuration updates
 * - Status/health endpoints
 */
class OTAServer(private val context: Context) : NanoHTTPD(OTA_PORT) {

    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Start the OTA server and register mDNS service
     */
    fun startServer() {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            Log.i(TAG, "OTA Server started on port $OTA_PORT")
            Log.i(TAG, "Access at: http://${getLocalIpAddress()}:$OTA_PORT")

            registerMdnsService()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start OTA server", e)
        }
    }

    /**
     * Stop the OTA server and unregister mDNS
     */
    fun stopServer() {
        try {
            stop()
            unregisterMdnsService()
            Log.i(TAG, "OTA Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop OTA server", e)
        }
    }

    /**
     * Handle HTTP requests
     */
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        Log.d(TAG, "Request: $method $uri from ${session.remoteIpAddress}")

        return when {
            // Health check
            uri == "/status" && method == Method.GET -> {
                handleStatus()
            }

            // Device info
            uri == "/info" && method == Method.GET -> {
                handleInfo()
            }

            // Upload APK for OTA update
            uri == "/update/apk" && method == Method.POST -> {
                handleApkUpload(session)
            }

            // Restart app
            uri == "/restart" && method == Method.POST -> {
                handleRestart()
            }

            // Root - show simple web UI
            uri == "/" && method == Method.GET -> {
                handleRoot()
            }

            else -> {
                newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "application/json",
                    """{"error": "Endpoint not found"}"""
                )
            }
        }
    }

    /**
     * Status endpoint - health check
     */
    private fun handleStatus(): Response {
        val json = """
        {
            "status": "ok",
            "version": "${getAppVersion()}",
            "device": "${Build.MODEL}",
            "ip": "${getLocalIpAddress()}",
            "port": $OTA_PORT
        }
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    /**
     * Info endpoint - detailed device info
     */
    private fun handleInfo(): Response {
        val json = """
        {
            "app": {
                "name": "Topster TV",
                "version": "${getAppVersion()}",
                "package": "${context.packageName}"
            },
            "device": {
                "model": "${Build.MODEL}",
                "manufacturer": "${Build.MANUFACTURER}",
                "android": "${Build.VERSION.RELEASE}",
                "sdk": ${Build.VERSION.SDK_INT}
            },
            "network": {
                "ip": "${getLocalIpAddress()}",
                "port": $OTA_PORT
            }
        }
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    /**
     * APK upload endpoint - receives APK and triggers installation
     */
    private fun handleApkUpload(session: IHTTPSession): Response {
        try {
            // Parse the multipart request
            val files = HashMap<String, String>()
            session.parseBody(files)

            val apkTempFile = files["apk"]
            if (apkTempFile == null) {
                return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "application/json",
                    """{"error": "No APK file provided"}"""
                )
            }

            // Copy temp file to downloads directory
            val downloadsDir = File(context.getExternalFilesDir(null), "downloads")
            downloadsDir.mkdirs()

            val apkFile = File(downloadsDir, "topster-tv-update.apk")
            File(apkTempFile).copyTo(apkFile, overwrite = true)

            Log.i(TAG, "APK uploaded: ${apkFile.absolutePath} (${apkFile.length()} bytes)")

            // Trigger installation
            scope.launch(Dispatchers.Main) {
                installApk(apkFile)
            }

            return newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                """{"success": true, "message": "APK uploaded, installing..."}"""
            )
        } catch (e: Exception) {
            Log.e(TAG, "APK upload failed", e)
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                """{"error": "${e.message}"}"""
            )
        }
    }

    /**
     * Restart endpoint - restart the app
     */
    private fun handleRestart(): Response {
        scope.launch(Dispatchers.Main) {
            restartApp()
        }

        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            """{"success": true, "message": "Restarting app..."}"""
        )
    }

    /**
     * Root endpoint - simple web UI
     */
    private fun handleRoot(): Response {
        val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Topster TV - OTA Updates</title>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body { font-family: monospace; max-width: 600px; margin: 50px auto; padding: 20px; background: #1a1a1a; color: #0f0; }
                h1 { color: #0f0; text-shadow: 0 0 10px #0f0; }
                .status { padding: 10px; background: #0a0a0a; border: 1px solid #0f0; margin: 20px 0; }
                input[type=file] { display: block; margin: 20px 0; }
                button { padding: 10px 20px; background: #0f0; color: #000; border: none; cursor: pointer; font-family: monospace; }
                button:hover { background: #0c0; }
                .log { background: #0a0a0a; padding: 10px; border: 1px solid #333; margin-top: 20px; max-height: 200px; overflow-y: auto; }
            </style>
        </head>
        <body>
            <h1>📺 Topster TV - OTA Updates</h1>

            <div class="status">
                <strong>Status:</strong> Online<br>
                <strong>Version:</strong> ${getAppVersion()}<br>
                <strong>Device:</strong> ${Build.MODEL}<br>
                <strong>IP:</strong> ${getLocalIpAddress()}:$OTA_PORT
            </div>

            <h2>Upload APK</h2>
            <form id="uploadForm" enctype="multipart/form-data">
                <input type="file" id="apkFile" name="apk" accept=".apk" required>
                <button type="submit">Upload & Install</button>
            </form>

            <div class="log" id="log"></div>

            <script>
                const log = document.getElementById('log');
                function addLog(msg) {
                    log.innerHTML += msg + '<br>';
                    log.scrollTop = log.scrollHeight;
                }

                document.getElementById('uploadForm').onsubmit = async (e) => {
                    e.preventDefault();
                    const file = document.getElementById('apkFile').files[0];
                    if (!file) return;

                    addLog('Uploading ' + file.name + ' (' + (file.size / 1024 / 1024).toFixed(2) + ' MB)...');

                    const formData = new FormData();
                    formData.append('apk', file);

                    try {
                        const res = await fetch('/update/apk', {
                            method: 'POST',
                            body: formData
                        });
                        const data = await res.json();
                        addLog('✅ ' + data.message);
                        addLog('Installation dialog should appear on TV...');
                    } catch (err) {
                        addLog('❌ Error: ' + err.message);
                    }
                };
            </script>
        </body>
        </html>
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    /**
     * Install APK file
     */
    private fun installApk(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            context.startActivity(intent)

            Log.i(TAG, "Install intent sent for: ${apkFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK", e)
        }
    }

    /**
     * Restart the app
     */
    private fun restartApp() {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            // Exit current process
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart app", e)
        }
    }

    /**
     * Register mDNS service for discovery
     */
    private fun registerMdnsService() {
        try {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = SERVICE_NAME
                serviceType = SERVICE_TYPE
                port = OTA_PORT
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "mDNS registration failed: $errorCode")
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "mDNS unregistration failed: $errorCode")
                }

                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    Log.i(TAG, "mDNS service registered: ${serviceInfo.serviceName}")
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    Log.i(TAG, "mDNS service unregistered")
                }
            }

            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            Log.i(TAG, "Registering mDNS service: $SERVICE_NAME")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register mDNS service", e)
        }
    }

    /**
     * Unregister mDNS service
     */
    private fun unregisterMdnsService() {
        try {
            registrationListener?.let {
                nsdManager.unregisterService(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister mDNS service", e)
        }
    }

    /**
     * Get local IP address
     */
    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "unknown"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get IP address", e)
        }
        return "unknown"
    }

    /**
     * Get app version
     */
    private fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}

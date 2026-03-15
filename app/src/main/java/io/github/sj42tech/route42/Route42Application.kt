package io.github.sj42tech.route42

import android.app.Application
import android.app.NotificationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.content.pm.ApplicationInfo
import androidx.core.content.getSystemService
import io.github.sj42tech.route42.tunnel.TunnelRuntime
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.SetupOptions
import java.io.File
import java.util.Locale

class Route42Application : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeLibbox()
    }

    private fun initializeLibbox() {
        runCatching {
            val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            diagnosticsEnabled = isDebuggable
            TunnelRuntime.setDiagnosticsEnabled(isDebuggable)

            val baseDir = File(filesDir, "libbox/base").apply { mkdirs() }
            val workingDir = File(filesDir, "libbox/work").apply { mkdirs() }
            val tempDir = File(cacheDir, "libbox").apply { mkdirs() }

            Libbox.setLocale(Locale.getDefault().toLanguageTag().replace("-", "_"))
            Libbox.setup(
                SetupOptions().apply {
                    basePath = baseDir.path
                    workingPath = workingDir.path
                    tempPath = tempDir.path
                    // Match the official Android client: modern Android versions need this
                    // workaround to avoid network stack regressions in Go/libbox.
                    fixAndroidStack = shouldFixAndroidStack()
                    logMaxLines = 1500
                    debug = isDebuggable
                },
            )
            if (isDebuggable) {
                Libbox.redirectStderr(File(workingDir, "stderr.log").path)
                TunnelRuntime.appendLog("libbox initialized")
            }
        }.onFailure {
            TunnelRuntime.setError("libbox init failed: ${it.message}")
        }
    }

    companion object {
        lateinit var instance: Route42Application
            private set

        var diagnosticsEnabled: Boolean = false
            private set

        val connectivity: ConnectivityManager
            get() = instance.getSystemService()!!

        val wifi: WifiManager
            get() = instance.getSystemService()!!

        val notificationManager: NotificationManager
            get() = instance.getSystemService()!!
    }
}

private fun shouldFixAndroidStack(): Boolean {
    val sdk = Build.VERSION.SDK_INT
    return sdk in Build.VERSION_CODES.N..Build.VERSION_CODES.N_MR1 || sdk >= Build.VERSION_CODES.P
}

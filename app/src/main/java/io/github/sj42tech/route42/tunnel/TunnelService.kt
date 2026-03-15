package io.github.sj42tech.route42.tunnel

import android.content.Intent
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.sj42tech.route42.Route42Application
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NeighborUpdateListener
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.Notification as BoxNotification
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.SystemProxyStatus
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TunnelService : VpnService(), PlatformInterface, CommandServerHandler {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val resolverNetworkController by lazy {
        ResolverNetworkController(
            connectivity = Route42Application.connectivity,
            applyUnderlyingNetworks = ::setUnderlyingNetworks,
            bindProcessToNetwork = Route42Application.connectivity::bindProcessToNetwork,
            log = TunnelRuntime::appendLog,
        )
    }

    private val defaultMonitorCallbacks = ConcurrentHashMap<InterfaceUpdateListener, ConnectivityManager.NetworkCallback>()

    private var commandServer: CommandServer? = null
    private var tunDescriptor: ParcelFileDescriptor? = null
    private var currentProfileId: String? = null
    private var currentProfileName: String? = null
    private var currentConfig: String? = null
    private var myInterfaceName: String? = null
    private var pendingConnection: PendingConnection? = null
    private val connectRequestMutex = Mutex()
    @Volatile
    private var stopping = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when {
            intent == null -> Unit
            TunnelServiceController.isDisconnectAction(this, intent.action) -> stopTunnel("Disconnected by user")
            TunnelServiceController.isConnectAction(this, intent.action) -> {
                val profileId = TunnelServiceController.readProfileId(intent)
                val profileName = TunnelServiceController.readProfileName(intent)
                val config = TunnelServiceController.readConfig(intent)
                if (profileId.isNullOrBlank() || profileName.isNullOrBlank() || config.isNullOrBlank()) {
                    TunnelRuntime.setError("Missing VPN profile payload")
                    stopSelf()
                } else {
                    val request = PendingConnection(
                        profileId = profileId,
                        profileName = profileName,
                        config = config,
                    )
                    serviceScope.launch {
                        connectRequestMutex.withLock {
                            handleConnectRequest(request)
                        }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    override fun onDestroy() {
        val hasActiveResources = commandServer != null || tunDescriptor != null || currentProfileId != null
        if (hasActiveResources && !stopping) {
            stopTunnel("Service destroyed")
        }
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onRevoke() = stopTunnel("VPN permission revoked")

    private suspend fun handleConnectRequest(request: PendingConnection) {
        if (stopping) {
            pendingConnection = request
            TunnelRuntime.appendLog("queued connect for ${request.profileName}")
            return
        }

        val activeStatus = TunnelRuntime.state.value.status
        val hasActiveTunnel = commandServer != null ||
            tunDescriptor != null ||
            activeStatus in setOf(TunnelStatus.STARTING, TunnelStatus.RUNNING, TunnelStatus.STOPPING)
        val isSameProfile = currentProfileId == request.profileId && currentConfig == request.config

        if (hasActiveTunnel && isSameProfile && activeStatus != TunnelStatus.ERROR) {
            TunnelRuntime.appendLog("connect ignored, profile already active")
            return
        }

        if (hasActiveTunnel) {
            pendingConnection = request
            stopTunnel(
                reason = if (currentProfileId == request.profileId) {
                    "Restarting tunnel"
                } else {
                    "Switching tunnel"
                },
            )
            return
        }

        startTunnel(
            profileId = request.profileId,
            profileName = request.profileName,
            config = request.config,
        )
    }

    private suspend fun startTunnel(profileId: String, profileName: String, config: String) {
        if (stopping) return
        TunnelRuntime.setStarting(profileId, profileName)
        currentProfileId = profileId
        currentProfileName = profileName
        currentConfig = config
        resolverNetworkController.start()
        resolverNetworkController.bindProcess()

        withContext(Dispatchers.Main) {
            ensureTunnelForeground(profileName, "Starting tunnel")
        }

        runCatching {
            Libbox.checkConfig(config)
            if (Route42Application.diagnosticsEnabled) {
                TunnelDiagnostics.logUpstreamReachability(
                    config = config,
                    requireResolverNetwork = resolverNetworkController::requireNetwork,
                    protectSocket = { socket -> protect(socket) },
                    log = ::appendDiagnosticLog,
                )
            }
            val server = commandServer ?: Libbox.newCommandServer(this@TunnelService, this@TunnelService).also {
                it.start()
                commandServer = it
            }
            server.startOrReloadService(
                config,
                OverrideOptions().apply {
                    autoRedirect = false
                    // Exclude the VPN app itself so libbox control and outbound sockets
                    // reach the upstream network directly instead of re-entering the TUN.
                    excludePackage = SimpleStringIterator(listOf(packageName))
                },
            )
            TunnelRuntime.setRunning(profileId, profileName)
            serviceScope.launch {
                refreshRouteAddresses(profileId, config)
            }
            withContext(Dispatchers.Main) {
                ensureTunnelForeground(profileName, "Tunnel connected")
            }
        }.onFailure {
            if (it is CancellationException) {
                TunnelRuntime.appendLog("tunnel start cancelled")
                return@onFailure
            }
            TunnelRuntime.setError("Tunnel start failed: ${it.message}")
            Log.e(TAG, "startTunnel", it)
            stopTunnel("Tunnel failed")
        }
    }

    private fun stopTunnel(reason: String? = null) {
        if (stopping) return
        stopping = true
        TunnelRuntime.setStopping()
        serviceScope.launch {
            runCatching { commandServer?.closeService() }
            runCatching { commandServer?.close() }
            commandServer = null
            runCatching { tunDescriptor?.close() }
            tunDescriptor = null
            resolverNetworkController.stop()
            defaultMonitorCallbacks.values.forEach { callback ->
                runCatching { Route42Application.connectivity.unregisterNetworkCallback(callback) }
            }
            defaultMonitorCallbacks.clear()
            currentProfileId = null
            currentProfileName = null
            currentConfig = null
            myInterfaceName = null
            val nextConnection = pendingConnection
            pendingConnection = null
            runCatching {
                Route42Application.connectivity.bindProcessToNetwork(null)
            }
            withContext(Dispatchers.Main) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                TunnelRuntime.setStopped(reason)
                stopping = false
                if (nextConnection == null) {
                    stopSelf()
                }
            }
            if (nextConnection != null) {
                TunnelRuntime.appendLog("restarting tunnel for ${nextConnection.profileName}")
                handleConnectRequest(nextConnection)
            }
        }
    }

    override fun serviceStop() = stopTunnel("Core requested stop")

    override fun serviceReload() {
        val profileId = currentProfileId ?: return
        val profileName = currentProfileName ?: return
        val config = currentConfig ?: return
        serviceScope.launch {
            connectRequestMutex.withLock {
                handleConnectRequest(
                    PendingConnection(
                        profileId = profileId,
                        profileName = profileName,
                        config = config,
                    ),
                )
            }
        }
    }

    override fun getSystemProxyStatus(): SystemProxyStatus =
        SystemProxyStatus().apply { available = false; enabled = false }

    override fun setSystemProxyEnabled(enabled: Boolean) =
        TunnelRuntime.appendLog("system proxy toggle ignored in MVP")

    override fun writeDebugMessage(message: String) {
        TunnelRuntime.appendLog(message)
        Log.d(TAG, message)
    }

    private fun appendDiagnosticLog(message: String) {
        TunnelRuntime.appendLog(message); Log.d(TAG, message)
    }

    private fun refreshRouteAddresses(profileId: String, config: String) {
        val resolved = TunnelAddressResolver.resolve(
            config = config,
            resolverNetwork = resolverNetworkController.currentNetwork,
            connectivity = Route42Application.connectivity,
            log = TunnelRuntime::appendLog,
        )
        val activeState = TunnelRuntime.state.value
        if (activeState.profileId == profileId && activeState.status == TunnelStatus.RUNNING) {
            TunnelRuntime.setResolvedAddresses(
                publicIp = resolved.publicIp,
                directPublicIp = resolved.directPublicIp,
                localNetworkIp = resolved.localNetworkIp,
            )
        }
    }

    override fun localDNSTransport(): LocalDNSTransport =
        AndroidLocalDnsTransport(resolverNetworkController::requireNetwork)

    override fun usePlatformAutoDetectInterfaceControl(): Boolean = false

    override fun autoDetectInterfaceControl(fd: Int) {
        protect(fd)
    }

    override fun openTun(options: TunOptions): Int {
        if (prepare(this) != null) error("VPN permission is missing")

        val builder = Builder()
            .setSession(currentProfileName ?: "Route42")
            .setMtu(options.mtu)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }
        resolverNetworkController.currentNetwork?.let { network ->
            builder.setUnderlyingNetworks(arrayOf(network))
        }

        options.inet4Address.forEach { prefix ->
            builder.addAddress(prefix.address(), prefix.prefix())
        }
        options.inet6Address.forEach { prefix ->
            builder.addAddress(prefix.address(), prefix.prefix())
        }

        configureTunBuilder(builder, options, TunnelRuntime::appendLog)

        val newDescriptor = builder.establish() ?: error("Failed to establish VPN interface")
        tunDescriptor?.close()
        tunDescriptor = newDescriptor
        return newDescriptor.fd
    }

    override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String,
        sourcePort: Int,
        destinationAddress: String,
        destinationPort: Int,
    ): ConnectionOwner = resolveConnectionOwner(
        ipProtocol = ipProtocol,
        sourceAddress = sourceAddress,
        sourcePort = sourcePort,
        destinationAddress = destinationAddress,
        destinationPort = destinationPort,
    )

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                publishDefaultInterfaceSnapshot(listener, resolverNetworkController.currentNetwork)
            }

            override fun onLost(network: android.net.Network) {
                publishDefaultInterfaceSnapshot(listener, resolverNetworkController.currentNetwork)
            }

            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: android.net.NetworkCapabilities,
            ) {
                publishDefaultInterfaceSnapshot(listener, resolverNetworkController.currentNetwork)
            }

            override fun onLinkPropertiesChanged(network: android.net.Network, linkProperties: LinkProperties) {
                publishDefaultInterfaceSnapshot(listener, resolverNetworkController.currentNetwork)
            }
        }
        defaultMonitorCallbacks[listener] = callback
        Route42Application.connectivity.registerDefaultNetworkCallback(callback)
        publishDefaultInterfaceSnapshot(listener, resolverNetworkController.currentNetwork)
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        val callback = defaultMonitorCallbacks.remove(listener) ?: return
        runCatching { Route42Application.connectivity.unregisterNetworkCallback(callback) }
    }

    override fun getInterfaces(): NetworkInterfaceIterator = collectNetworkInterfaces(myInterfaceName)

    override fun underNetworkExtension(): Boolean = false

    override fun includeAllNetworks(): Boolean = false

    override fun readWIFIState(): WIFIState? = readWifiStateSnapshot()

    override fun systemCertificates(): StringIterator = readSystemCertificatesSnapshot()

    override fun clearDNSCache() = Unit

    override fun sendNotification(notification: BoxNotification) {
        val built = NotificationCompat.Builder(this, TunnelForeground.ChannelId)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)
            .build()
        Route42Application.notificationManager.notify(notification.typeID, built)
    }

    override fun startNeighborMonitor(listener: NeighborUpdateListener) = Unit

    override fun closeNeighborMonitor(listener: NeighborUpdateListener) = Unit

    override fun registerMyInterface(name: String) { myInterfaceName = name }

    companion object {
        private const val TAG = "TunnelService"
    }
}

package io.sj42.vpn.tunnel

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

internal class ResolverNetworkController(
    private val connectivity: ConnectivityManager,
    private val applyUnderlyingNetworks: (Array<Network>?) -> Unit,
    private val bindProcessToNetwork: (Network?) -> Boolean,
    private val log: (String) -> Unit,
) {
    var currentNetwork: Network? = null
        private set

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun start() {
        currentNetwork = connectivity.activeNetwork
        syncUnderlyingNetworks()
        if (networkCallback != null) return

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateNetwork(network)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                updateNetwork(network, networkCapabilities)
            }

            override fun onLost(network: Network) {
                if (currentNetwork == network) {
                    currentNetwork = null
                    syncUnderlyingNetworks()
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .build()

        runCatching {
            connectivity.requestNetwork(request, callback)
            networkCallback = callback
        }
    }

    fun stop() {
        networkCallback?.let { callback ->
            runCatching { connectivity.unregisterNetworkCallback(callback) }
        }
        networkCallback = null
        currentNetwork = null
    }

    fun requireNetwork(): Network {
        currentNetwork?.let { return it }
        val activeNetwork = connectivity.activeNetwork
        if (activeNetwork != null) {
            val capabilities = connectivity.getNetworkCapabilities(activeNetwork)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) != true) {
                currentNetwork = activeNetwork
                syncUnderlyingNetworks()
                return activeNetwork
            }
        }
        error("Missing underlying network for DNS resolution")
    }

    fun bindProcess() {
        val network = runCatching { requireNetwork() }.getOrNull() ?: return
        val isBound = bindProcessToNetwork(network)
        log("bound process to underlying network: $isBound")
    }

    private fun updateNetwork(
        network: Network,
        capabilities: NetworkCapabilities? = connectivity.getNetworkCapabilities(network),
    ) {
        if (capabilities == null || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            return
        }
        currentNetwork = network
        syncUnderlyingNetworks()
    }

    private fun syncUnderlyingNetworks() {
        val underlyingNetworks = currentNetwork?.let { arrayOf(it) }
        runCatching {
            applyUnderlyingNetworks(underlyingNetworks)
        }.onFailure {
            log("underlying network sync failed: ${it.message}")
        }
    }
}

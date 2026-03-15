package io.sj42.vpn.tunnel

import android.net.Network
import android.net.NetworkCapabilities
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import android.system.OsConstants
import io.sj42.vpn.Route42Application
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.NetworkInterface
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.TunOptions
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface as JavaNetworkInterface

internal fun configureTunBuilder(
    builder: VpnService.Builder,
    options: TunOptions,
    log: (String) -> Unit,
) {
    if (!options.autoRoute) {
        return
    }

    val dnsAddress = options.dnsServerAddress?.value
    if (!dnsAddress.isNullOrBlank()) {
        builder.addDnsServer(dnsAddress)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val inet4RouteAddress = options.inet4RouteAddress
        if (inet4RouteAddress.hasNext()) {
            inet4RouteAddress.forEach { prefix ->
                builder.addRoute(prefix.toIpPrefix())
            }
        } else if (options.inet4Address.hasNext()) {
            builder.addRoute("0.0.0.0", 0)
        }

        val inet6RouteAddress = options.inet6RouteAddress
        if (inet6RouteAddress.hasNext()) {
            inet6RouteAddress.forEach { prefix ->
                builder.addRoute(prefix.toIpPrefix())
            }
        } else if (options.inet6Address.hasNext()) {
            builder.addRoute("::", 0)
        }

        options.inet4RouteExcludeAddress.forEach { prefix ->
            builder.excludeRoute(prefix.toIpPrefix())
        }
        options.inet6RouteExcludeAddress.forEach { prefix ->
            builder.excludeRoute(prefix.toIpPrefix())
        }
    } else {
        options.inet4RouteRange.forEach { prefix ->
            builder.addRoute(prefix.address(), prefix.prefix())
        }
        options.inet6RouteRange.forEach { prefix ->
            builder.addRoute(prefix.address(), prefix.prefix())
        }
    }

    options.includePackage.forEach { packageName ->
        runCatching { builder.addAllowedApplication(packageName) }
            .onSuccess { log("allowed app: $packageName") }
            .onFailure { log("allow app failed: $packageName (${it.message})") }
    }
    options.excludePackage.forEach { packageName ->
        runCatching { builder.addDisallowedApplication(packageName) }
            .onSuccess { log("disallowed app: $packageName") }
            .onFailure { log("disallow app failed: $packageName (${it.message})") }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && options.isHTTPProxyEnabled) {
        builder.setHttpProxy(
            ProxyInfo.buildDirectProxy(
                options.httpProxyServer,
                options.httpProxyServerPort,
                options.httpProxyBypassDomain.toList(),
            ),
        )
    }
}

internal fun publishDefaultInterfaceSnapshot(
    listener: InterfaceUpdateListener,
    resolverNetwork: Network?,
) {
    val activeNetwork = Route42Application.connectivity.activeNetwork
    val network = activeNetwork?.takeIf { candidate ->
        Route42Application.connectivity.getNetworkCapabilities(candidate)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) != true
    } ?: resolverNetwork
    if (network == null) {
        listener.updateDefaultInterface("", -1, false, false)
        return
    }
    val linkProperties = Route42Application.connectivity.getLinkProperties(network)
    val networkCapabilities = Route42Application.connectivity.getNetworkCapabilities(network)
    val interfaceName = linkProperties?.interfaceName.orEmpty()
    val interfaceIndex = runCatching { JavaNetworkInterface.getByName(interfaceName)?.index ?: -1 }
        .getOrDefault(-1)
    val isExpensive = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
    listener.updateDefaultInterface(interfaceName, interfaceIndex, isExpensive, false)
}

internal fun collectNetworkInterfaces(myInterfaceName: String?): NetworkInterfaceIterator {
    val javaInterfaces = JavaNetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
    val mapped = linkedMapOf<String, NetworkInterface>()
    @Suppress("DEPRECATION")
    val allNetworks = Route42Application.connectivity.allNetworks
    for (network in allNetworks) {
        val linkProperties = Route42Application.connectivity.getLinkProperties(network) ?: continue
        val networkCapabilities = Route42Application.connectivity.getNetworkCapabilities(network) ?: continue
        val interfaceName = linkProperties.interfaceName ?: continue
        if (interfaceName == myInterfaceName) continue
        val javaInterface = javaInterfaces.firstOrNull { it.name == interfaceName } ?: continue
        val flags = buildInterfaceFlags(javaInterface, networkCapabilities)
        mapped[interfaceName] = NetworkInterface().apply {
            name = interfaceName
            index = javaInterface.index
            mtu = runCatching { javaInterface.mtu }.getOrDefault(1500)
            addresses = SimpleStringIterator(
                javaInterface.interfaceAddresses.mapTo(mutableListOf()) { it.toPrefix() },
            )
            setDNSServer(SimpleStringIterator(linkProperties.dnsServers.mapNotNull(InetAddress::getHostAddress)))
            type = when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Libbox.InterfaceTypeWIFI
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Libbox.InterfaceTypeCellular
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Libbox.InterfaceTypeEthernet
                else -> Libbox.InterfaceTypeOther
            }
            metered = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            this.flags = flags
        }
    }
    return SimpleNetworkInterfaceIterator(mapped.values.toList())
}

private fun buildInterfaceFlags(
    javaInterface: JavaNetworkInterface,
    capabilities: NetworkCapabilities,
): Int {
    var flags = 0
    if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
        flags = flags or OsConstants.IFF_UP or OsConstants.IFF_RUNNING
    }
    if (javaInterface.isLoopback) flags = flags or OsConstants.IFF_LOOPBACK
    if (javaInterface.isPointToPoint) flags = flags or OsConstants.IFF_POINTOPOINT
    if (javaInterface.supportsMulticast()) flags = flags or OsConstants.IFF_MULTICAST
    return flags
}

private fun InterfaceAddress.toPrefix(): String = if (address is Inet6Address) {
    "${Inet6Address.getByAddress(address.address).hostAddress}/$networkPrefixLength"
} else {
    "${address.hostAddress}/$networkPrefixLength"
}

internal class SimpleStringIterator(items: List<String>) : StringIterator {
    private val values = items.iterator()
    private val size = items.size

    override fun hasNext(): Boolean = values.hasNext()

    override fun len(): Int = size

    override fun next(): String = values.next()
}

private class SimpleNetworkInterfaceIterator(items: List<NetworkInterface>) : NetworkInterfaceIterator {
    private val values = items.iterator()

    override fun hasNext(): Boolean = values.hasNext()

    override fun next(): NetworkInterface = values.next()
}

internal val TunOptions.mtu: Int
    get() = getMTU()

internal val TunOptions.autoRoute: Boolean
    get() = getAutoRoute()

internal val TunOptions.dnsServerAddress
    get() = runCatching { getDNSServerAddress() }.getOrNull()

internal val TunOptions.inet4Address
    get() = getInet4Address()

internal val TunOptions.inet6Address
    get() = getInet6Address()

internal val TunOptions.inet4RouteRange
    get() = getInet4RouteRange()

internal val TunOptions.inet6RouteRange
    get() = getInet6RouteRange()

internal val TunOptions.inet4RouteAddress
    get() = getInet4RouteAddress()

internal val TunOptions.inet6RouteAddress
    get() = getInet6RouteAddress()

internal val TunOptions.inet4RouteExcludeAddress
    get() = getInet4RouteExcludeAddress()

internal val TunOptions.inet6RouteExcludeAddress
    get() = getInet6RouteExcludeAddress()

internal val TunOptions.includePackage
    get() = getIncludePackage()

internal val TunOptions.excludePackage
    get() = getExcludePackage()

internal val TunOptions.isHTTPProxyEnabled
    get() = isHTTPProxyEnabled()

internal val TunOptions.httpProxyServer
    get() = getHTTPProxyServer()

internal val TunOptions.httpProxyServerPort
    get() = getHTTPProxyServerPort()

internal val TunOptions.httpProxyBypassDomain
    get() = getHTTPProxyBypassDomain()

internal fun io.nekohasekai.libbox.RoutePrefixIterator.forEach(block: (io.nekohasekai.libbox.RoutePrefix) -> Unit) {
    while (hasNext()) {
        block(next())
    }
}

internal fun StringIterator.forEach(block: (String) -> Unit) {
    while (hasNext()) {
        block(next())
    }
}

internal fun StringIterator.toList(): List<String> {
    val list = mutableListOf<String>()
    forEach(list::add)
    return list
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun io.nekohasekai.libbox.RoutePrefix.toIpPrefix(): android.net.IpPrefix =
    android.net.IpPrefix(InetAddress.getByName(address()), prefix())

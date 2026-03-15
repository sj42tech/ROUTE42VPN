package io.github.sj42tech.route42.tunnel

import android.net.Network
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object TunnelDiagnostics {
    fun logUpstreamReachability(
        config: String,
        requireResolverNetwork: () -> Network,
        protectSocket: (Socket) -> Boolean,
        log: (String) -> Unit,
    ) {
        val target = parseProxyTarget(config) ?: return

        runCatching { plainSocketProbe(target.first, target.second) }
            .onSuccess { log("probe plain tcp ${target.first}:${target.second} ok") }
            .onFailure { log("probe plain tcp ${target.first}:${target.second} failed: ${it.message}") }

        runCatching { protectedSocketProbe(target.first, target.second, protectSocket) }
            .onSuccess { log("probe protected tcp ${target.first}:${target.second} ok") }
            .onFailure { log("probe protected tcp ${target.first}:${target.second} failed: ${it.message}") }

        runCatching { networkBoundSocketProbe(target.first, target.second, requireResolverNetwork) }
            .onSuccess { log("probe network tcp ${target.first}:${target.second} ok") }
            .onFailure { log("probe network tcp ${target.first}:${target.second} failed: ${it.message}") }
    }

    private fun parseProxyTarget(config: String): Pair<String, Int>? = runCatching {
        val root = Json.parseToJsonElement(config).jsonObject
        val proxy = root["outbounds"]
            ?.jsonArray
            ?.map { it.jsonObject }
            ?.firstOrNull { outbound -> outbound["tag"]?.jsonPrimitive?.content == "proxy" }
            ?: return null
        val host = proxy["server"]?.jsonPrimitive?.content?.takeIf(String::isNotBlank) ?: return null
        val port = proxy["server_port"]?.jsonPrimitive?.content?.toIntOrNull() ?: return null
        host to port
    }.getOrNull()

    private fun plainSocketProbe(host: String, port: Int) {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 3_000)
        }
    }

    private fun protectedSocketProbe(host: String, port: Int, protectSocket: (Socket) -> Boolean) {
        Socket().use { socket ->
            if (!protectSocket(socket)) {
                error("protect() returned false")
            }
            socket.connect(InetSocketAddress(host, port), 3_000)
        }
    }

    private fun networkBoundSocketProbe(
        host: String,
        port: Int,
        requireResolverNetwork: () -> Network,
    ) {
        val network = requireResolverNetwork()
        network.socketFactory.createSocket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 3_000)
        }
    }
}

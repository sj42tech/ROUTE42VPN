package io.sj42.vpn.tunnel

import android.net.ConnectivityManager
import android.net.Network
import io.nekohasekai.libbox.Libbox
import io.sj42.vpn.config.SingBoxConfigGenerator
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class ResolvedTunnelAddresses(
    val publicIp: String? = null,
    val directPublicIp: String? = null,
    val localNetworkIp: String? = null,
)

internal object TunnelAddressResolver {
    private val ipLookupEndpoints = listOf(
        "https://api.ipify.org",
        "https://ipv4.icanhazip.com",
    )

    fun resolve(
        config: String,
        resolverNetwork: Network?,
        connectivity: ConnectivityManager,
        log: (String) -> Unit,
    ): ResolvedTunnelAddresses {
        val publicIp = runCatching { fetchTunnelPublicIp(config) }
            .onFailure { log("proxy ip lookup failed: ${it.message}") }
            .getOrNull()
            ?.takeIf(String::isNotBlank)
        val directPublicIp = runCatching { fetchDirectPublicIp() }
            .onFailure { log("direct ip lookup failed: ${it.message}") }
            .getOrNull()
            ?.takeIf(String::isNotBlank)

        return ResolvedTunnelAddresses(
            publicIp = publicIp,
            directPublicIp = directPublicIp,
            localNetworkIp = currentLocalNetworkIp(resolverNetwork, connectivity),
        )
    }

    private fun fetchTunnelPublicIp(config: String): String {
        val socksPort = parseProbeSocksPort(config)
        ipLookupEndpoints.forEach { endpoint ->
            runCatching {
                val client = Libbox.newHTTPClient()
                try {
                    client.modernTLS()
                    client.trySocks5(socksPort)
                    val request = client.newRequest()
                    request.setMethod("GET")
                    request.randomUserAgent()
                    request.setURL(endpoint)
                    request.execute().content.value.trim()
                } finally {
                    runCatching { client.close() }
                }
            }.getOrNull()?.takeIf(String::isNotBlank)?.let { return it }
        }
        error("Unable to resolve tunnel public IP")
    }

    private fun fetchDirectPublicIp(): String {
        ipLookupEndpoints.forEach { endpoint ->
            runCatching {
                val connection = URL(endpoint).openConnection() as HttpsURLConnection
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                connection.requestMethod = "GET"
                connection.inputStream.bufferedReader().use { reader ->
                    reader.readText().trim()
                }
            }.getOrNull()?.takeIf(String::isNotBlank)?.let { return it }
        }
        error("Unable to resolve direct public IP")
    }

    private fun parseProbeSocksPort(config: String): Int = runCatching {
        val root = Json.parseToJsonElement(config).jsonObject
        val probeInbound = root["inbounds"]
            ?.jsonArray
            ?.map { it.jsonObject }
            ?.firstOrNull { inbound ->
                inbound["tag"]?.jsonPrimitive?.content == SingBoxConfigGenerator.LocalProbeSocksTag
            }
        probeInbound
            ?.get("listen_port")
            ?.jsonPrimitive
            ?.content
            ?.toIntOrNull()
            ?: SingBoxConfigGenerator.LocalProbeSocksPort
    }.getOrDefault(SingBoxConfigGenerator.LocalProbeSocksPort)

    private fun currentLocalNetworkIp(
        resolverNetwork: Network?,
        connectivity: ConnectivityManager,
    ): String? {
        val network = resolverNetwork ?: return null
        val linkProperties = connectivity.getLinkProperties(network) ?: return null
        return linkProperties.linkAddresses
            .asSequence()
            .mapNotNull { linkAddress ->
                val address = linkAddress.address
                val hostAddress = address.hostAddress?.substringBefore('%').orEmpty()
                if (hostAddress.isBlank() || address.isLoopbackAddress) {
                    null
                } else {
                    hostAddress
                }
            }
            .sortedWith(compareBy<String> { if (it.contains(':')) 1 else 0 })
            .firstOrNull()
    }
}

package io.github.sj42tech.route42.tunnel

import android.net.ConnectivityManager
import android.net.Network
import io.nekohasekai.libbox.Libbox
import io.github.sj42tech.route42.config.SingBoxConfigGenerator
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
    val tunnelSiteProbes: List<TunnelSiteProbe> = emptyList(),
)

internal object TunnelAddressResolver {
    private data class PopularSiteTarget(
        val label: String,
        val url: String,
    )

    private val ipLookupEndpoints = listOf(
        "https://api.ipify.org",
        "https://ipv4.icanhazip.com",
    )
    private val popularSiteTargets = listOf(
        PopularSiteTarget(
            label = "Google",
            url = "https://www.google.com/robots.txt",
        ),
        PopularSiteTarget(
            label = "GitHub",
            url = "https://www.github.com/robots.txt",
        ),
        PopularSiteTarget(
            label = "Cloudflare",
            url = "https://www.cloudflare.com/cdn-cgi/trace",
        ),
    )

    fun resolve(
        config: String,
        resolverNetwork: Network?,
        connectivity: ConnectivityManager,
        log: (String) -> Unit,
    ): ResolvedTunnelAddresses {
        val socksPort = parseProbeSocksPort(config)
        val publicIp = runCatching { fetchTunnelPublicIp(socksPort) }
            .onFailure { log("proxy ip lookup failed: ${it.message}") }
            .getOrNull()
            ?.takeIf(String::isNotBlank)
        val tunnelSiteProbes = runCatching { fetchTunnelSiteProbes(socksPort) }
            .onFailure { log("tunnel site probe failed: ${it.message}") }
            .getOrDefault(emptyList())
        val directPublicIp = runCatching { fetchDirectPublicIp() }
            .onFailure { log("direct ip lookup failed: ${it.message}") }
            .getOrNull()
            ?.takeIf(String::isNotBlank)

        return ResolvedTunnelAddresses(
            publicIp = publicIp,
            directPublicIp = directPublicIp,
            localNetworkIp = currentLocalNetworkIp(resolverNetwork, connectivity),
            tunnelSiteProbes = tunnelSiteProbes,
        )
    }

    private fun fetchTunnelPublicIp(socksPort: Int): String {
        ipLookupEndpoints.forEach { endpoint ->
            runCatching {
                withTunnelHttpClient(socksPort) { client ->
                    val request = client.newRequest()
                    request.setMethod("GET")
                    request.randomUserAgent()
                    request.setURL(endpoint)
                    request.execute().content.value.trim()
                }
            }.getOrNull()?.takeIf(String::isNotBlank)?.let { return it }
        }
        error("Unable to resolve tunnel public IP")
    }

    private fun fetchTunnelSiteProbes(socksPort: Int): List<TunnelSiteProbe> = popularSiteTargets.map { target ->
        runCatching {
            withTunnelHttpClient(socksPort) { client ->
                val request = client.newRequest()
                request.setMethod("GET")
                request.randomUserAgent()
                request.setURL(target.url)
                val response = request.execute()
                val contentPreview = runCatching { response.content.value.trim() }
                    .getOrDefault("")
                    .take(80)
                    .ifBlank { null }
                TunnelSiteProbe(
                    label = target.label,
                    url = target.url,
                    reachable = true,
                    detail = contentPreview,
                )
            }
        }.getOrElse { error ->
            TunnelSiteProbe(
                label = target.label,
                url = target.url,
                reachable = false,
                detail = error.message ?: error::class.java.simpleName,
            )
        }
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

    private fun <T> withTunnelHttpClient(
        socksPort: Int,
        block: (io.nekohasekai.libbox.HTTPClient) -> T,
    ): T {
        val client = Libbox.newHTTPClient()
        return try {
            client.modernTLS()
            client.trySocks5(socksPort)
            block(client)
        } finally {
            runCatching { client.close() }
        }
    }

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

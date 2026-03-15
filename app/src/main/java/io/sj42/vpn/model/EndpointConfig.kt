package io.sj42.vpn.model

import kotlinx.serialization.Serializable

@Serializable
data class EndpointConfig(
    val protocol: EndpointProtocol = EndpointProtocol.VLESS,
    val server: String,
    val serverPort: Int,
    val uuid: String,
    val network: String = "tcp",
    val security: String? = null,
    val encryption: String? = null,
    val flow: String? = null,
    val serverName: String? = null,
    val fingerprint: String? = null,
    val publicKey: String? = null,
    val shortId: String? = null,
    val alpn: List<String> = emptyList(),
    val extraQueryParameters: Map<String, List<String>> = emptyMap(),
)

@Serializable
enum class EndpointProtocol {
    VLESS,
}

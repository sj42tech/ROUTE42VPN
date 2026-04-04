package io.github.sj42tech.route42.config

import io.github.sj42tech.route42.model.ConnectionProfile
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun buildOutbounds(profile: ConnectionProfile): JsonArray = buildJsonArray {
    add(buildProxyOutbound(profile))
    add(buildJsonObject {
        put("type", "direct")
        put("tag", "direct")
    })
    add(buildJsonObject {
        put("type", "block")
        put("tag", "block")
    })
}

private fun buildProxyOutbound(profile: ConnectionProfile): JsonObject = buildJsonObject {
    put("type", "vless")
    put("tag", "proxy")
    put("server", profile.endpoint.server)
    put("server_port", profile.endpoint.serverPort)
    put("uuid", profile.endpoint.uuid)
    profile.endpoint.flow?.let { put("flow", it) }
    put("packet_encoding", "xudp")
    put(
        "tls",
        buildJsonObject {
            put("enabled", true)
            profile.endpoint.serverName?.let { put("server_name", it) }
            if (profile.endpoint.alpn.isNotEmpty()) {
                put("alpn", jsonArrayOf(*profile.endpoint.alpn.toTypedArray()))
            }
            profile.endpoint.fingerprint?.let { fingerprint ->
                put(
                    "utls",
                    buildJsonObject {
                        put("enabled", true)
                        put("fingerprint", fingerprint)
                    },
                )
            }
            if (profile.endpoint.security.equals("reality", ignoreCase = true)) {
                put(
                    "reality",
                    buildJsonObject {
                        put("enabled", true)
                        profile.endpoint.publicKey?.let { put("public_key", it) }
                        profile.endpoint.shortId?.let { put("short_id", it) }
                    },
                )
            }
        },
    )
    buildTransport(profile)?.let { put("transport", it) }
}

private fun buildTransport(profile: ConnectionProfile): JsonObject? {
    val transportType = profile.endpoint.network.trim().lowercase()
    if (transportType.isBlank() || transportType == "tcp") {
        return buildTcpTransport(profile.endpoint.extraQueryParameters)
    }

    val extras = profile.endpoint.extraQueryParameters
    return buildJsonObject {
        when (transportType) {
            "raw" -> return buildTcpTransport(extras)

            "http" -> {
                put("type", "http")
                extras.csvValues("host").takeIf(List<String>::isNotEmpty)?.let { hosts ->
                    put("host", jsonArrayOf(*hosts.toTypedArray()))
                }
                extras.firstValue("path")?.let { put("path", it) }
                extras.firstValue("method")?.let { put("method", it) }
            }

            "ws", "websocket" -> {
                put("type", "ws")
                extras.firstValue("path")?.let { put("path", it) }
                extras.firstValue("host")?.let { host ->
                    put(
                        "headers",
                        buildJsonObject {
                            put("Host", host)
                        },
                    )
                }
                extras.firstValue("ed", "maxEarlyData")
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 }
                    ?.let { put("max_early_data", it) }
                extras.firstValue("eh", "earlyDataHeaderName")
                    ?.let { put("early_data_header_name", it) }
            }

            "grpc" -> {
                put("type", "grpc")
                extras.firstValue("serviceName", "service_name")?.let { put("service_name", it) }
                extras.firstValue("authority")?.let { put("authority", it) }
            }

            "httpupgrade" -> {
                put("type", "httpupgrade")
                extras.firstValue("host")?.let { put("host", it) }
                extras.firstValue("path")?.let { put("path", it) }
            }

            "quic" -> {
                put("type", "quic")
            }

            "xhttp" -> throw UnsupportedEndpointConfigurationException(
                "XHTTP transport is not supported by the current Route42 sing-box client",
            )

            else -> throw UnsupportedEndpointConfigurationException(
                "Unsupported transport type: ${profile.endpoint.network}",
            )
        }
    }
}

private fun buildTcpTransport(extras: Map<String, List<String>>): JsonObject? {
    val headerType = extras.firstValue("headerType")?.lowercase()
    return when {
        headerType.isNullOrBlank() || headerType == "none" -> null
        headerType == "http" -> buildJsonObject {
            put("type", "http")
            extras.csvValues("host").takeIf(List<String>::isNotEmpty)?.let { hosts ->
                put("host", jsonArrayOf(*hosts.toTypedArray()))
            }
            extras.firstValue("path")?.let { put("path", it) }
            extras.firstValue("method")?.let { put("method", it) }
        }

        else -> throw UnsupportedEndpointConfigurationException(
            "Unsupported TCP header type: $headerType",
        )
    }
}

package io.github.sj42tech.route42.config

import io.github.sj42tech.route42.model.ConnectionProfile
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun buildInbounds(profile: ConnectionProfile): JsonArray = buildJsonArray {
    add(
        buildJsonObject {
            put("type", "tun")
            put("tag", "tun-in")
            put("address", jsonArrayOf("172.19.0.1/30", "fdfe:dcba:9876::1/126"))
            put("auto_route", true)
            serverRouteExclude(profile.endpoint.server)?.let { route ->
                put("route_exclude_address", jsonArrayOf(route))
            }
            put("stack", "system")
        },
    )
    add(
        buildJsonObject {
            put("type", "socks")
            put("tag", SingBoxConfigGenerator.LocalProbeSocksTag)
            put("listen", "127.0.0.1")
            put("listen_port", SingBoxConfigGenerator.LocalProbeSocksPort)
        },
    )
}

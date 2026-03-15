package io.github.sj42tech.route42.config

import io.github.sj42tech.route42.model.ConnectionProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object SingBoxConfigGenerator {
    const val LocalProbeSocksTag = "app-socks"
    const val LocalProbeSocksPort = 39080

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private val JsonFormatter = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun generate(profile: ConnectionProfile): String {
        val config = buildJsonObject {
            put("log", buildJsonObject {
                put("level", "info")
            })
            put("dns", buildDnsConfig(profile))
            put("inbounds", buildInbounds(profile))
            put("outbounds", buildOutbounds(profile))
            put("route", buildRouteConfig(profile))
        }

        return JsonFormatter.encodeToString(JsonObject.serializer(), config)
    }
}

package io.github.sj42tech.route42.config

import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.MatchType
import io.github.sj42tech.route42.model.RoutingAction
import io.github.sj42tech.route42.model.RoutingMode
import io.github.sj42tech.route42.model.RoutingRule
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun buildRouteConfig(profile: ConnectionProfile): JsonObject = buildJsonObject {
    put(
        "final",
        when (profile.routing.mode) {
            RoutingMode.DIRECT -> "direct"
            RoutingMode.PROXY -> "proxy"
            RoutingMode.RULE -> "proxy"
        },
    )
    put("rules", buildJsonArray {
        add(
            buildJsonObject {
                put("port", 53)
                put("action", "hijack-dns")
            },
        )
        add(
            buildJsonObject {
                put("port", 853)
                put("ip_cidr", jsonArrayOf("127.0.0.0/8", "::1/128"))
                put("action", "reject")
                put("method", "default")
            },
        )
        add(
            buildJsonObject {
                put("ip_is_private", true)
                put("action", "route")
                put("outbound", "direct")
            },
        )
        add(
            buildJsonObject {
                put("domain_suffix", jsonArrayOf("lan", "local", "home.arpa"))
                put("action", "route")
                put("outbound", "direct")
            },
        )
        if (profile.routing.mode == RoutingMode.RULE) {
            add(
                buildJsonObject {
                    put("domain_suffix", jsonArrayOf("ru", "rf"))
                    put("action", "route")
                    put("outbound", "direct")
                },
            )
        }
        profile.routing.rules.filter(RoutingRule::enabled).forEach { rule ->
            add(buildMatchRule(rule, "outbound", outboundTag(rule.action)))
        }
    })
}

internal fun buildMatchRule(
    rule: RoutingRule,
    targetKey: String,
    targetValue: String,
): JsonObject = buildJsonObject {
    when (rule.matchType) {
        MatchType.DOMAIN -> put("domain", jsonArrayOf(rule.value))
        MatchType.DOMAIN_SUFFIX -> put("domain_suffix", jsonArrayOf(rule.value))
        MatchType.IP_CIDR -> put("ip_cidr", jsonArrayOf(rule.value))
    }
    put("action", "route")
    put(targetKey, targetValue)
}

private fun outboundTag(action: RoutingAction): String = when (action) {
    RoutingAction.DIRECT -> "direct"
    RoutingAction.PROXY -> "proxy"
    RoutingAction.BLOCK -> "block"
}

internal fun serverRouteExclude(server: String): String? = when {
    server.matches(Regex("""^\d{1,3}(\.\d{1,3}){3}$""")) -> "$server/32"
    server.contains(':') -> "$server/128"
    else -> null
}

package io.github.sj42tech.route42.config

import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.MatchType
import io.github.sj42tech.route42.model.RoutingAction
import io.github.sj42tech.route42.model.RoutingMode
import io.github.sj42tech.route42.model.RoutingProfile
import io.github.sj42tech.route42.model.RoutingRule
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Suppress("UNUSED_PARAMETER")
internal fun buildRouteConfig(profile: ConnectionProfile, routingProfile: RoutingProfile): JsonObject = buildJsonObject {
    put(
        "final",
        when (routingProfile.mode) {
            RoutingMode.DIRECT -> "direct"
            RoutingMode.PROXY -> "proxy"
            RoutingMode.RULE -> "proxy"
        },
    )
    val builtInRuleSets = builtInRouteRuleSets(routingProfile)
    if (builtInRuleSets.isNotEmpty()) {
        put("rule_set", buildJsonArray {
            builtInRuleSets.forEach(::add)
        })
    }
    put("rules", buildJsonArray {
        builtInRouteRules(routingProfile).forEach(::add)
        routingProfile.rules.filter(RoutingRule::enabled).forEach { rule ->
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

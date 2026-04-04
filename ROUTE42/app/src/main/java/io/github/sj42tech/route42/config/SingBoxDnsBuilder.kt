package io.github.sj42tech.route42.config

import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.DnsMode
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
internal fun buildDnsConfig(profile: ConnectionProfile, routingProfile: RoutingProfile): JsonObject = buildJsonObject {
    put("servers", buildJsonArray {
        add(
            buildJsonObject {
                put("type", "local")
                put("tag", "direct-dns")
            },
        )
        add(
            buildJsonObject {
                put("type", "https")
                put("tag", "proxy-dns")
                put("server", "1.1.1.1")
                put("server_port", 443)
                put("path", "/dns-query")
                put("detour", "proxy")
            },
        )
    })

    put("rules", buildJsonArray {
        builtInDnsRules(routingProfile).forEach(::add)

        routingProfile.rules
            .filter(RoutingRule::enabled)
            .filter { it.matchType != MatchType.IP_CIDR }
            .forEach { rule ->
                val serverTag = when (rule.action) {
                    RoutingAction.DIRECT -> "direct-dns"
                    RoutingAction.PROXY -> "proxy-dns"
                    RoutingAction.BLOCK -> "direct-dns"
                }
                add(buildMatchRule(rule, "server", serverTag))
            }
    })

    put(
        "final",
        when (routingProfile.dnsMode) {
            DnsMode.LOCAL -> "direct-dns"
            DnsMode.PROXY -> "proxy-dns"
            DnsMode.SPLIT -> "proxy-dns"
        },
    )
}

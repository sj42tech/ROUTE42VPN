package io.sj42.vpn.config

import io.sj42.vpn.model.ConnectionProfile
import io.sj42.vpn.model.DnsMode
import io.sj42.vpn.model.MatchType
import io.sj42.vpn.model.RoutingAction
import io.sj42.vpn.model.RoutingMode
import io.sj42.vpn.model.RoutingRule
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun buildDnsConfig(profile: ConnectionProfile): JsonObject = buildJsonObject {
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
        add(
            buildJsonObject {
                put("domain_suffix", jsonArrayOf("lan", "local", "home.arpa"))
                put("action", "route")
                put("server", "direct-dns")
            },
        )
        if (profile.routing.mode == RoutingMode.RULE) {
            add(
                buildJsonObject {
                    put("domain_suffix", jsonArrayOf("ru", "rf"))
                    put("action", "route")
                    put("server", "direct-dns")
                },
            )
        }

        profile.routing.rules
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
        when (profile.routing.dnsMode) {
            DnsMode.LOCAL -> "direct-dns"
            DnsMode.PROXY -> "proxy-dns"
            DnsMode.SPLIT -> "proxy-dns"
        },
    )
}

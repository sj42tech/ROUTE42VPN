package io.github.sj42tech.route42.config

import io.github.sj42tech.route42.model.RoutingMode
import io.github.sj42tech.route42.model.RoutingPreset
import io.github.sj42tech.route42.model.RoutingProfile
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal const val RuGeoipRuleSetTag = "geoip-ru"

private val LocalSpecialCidrs = listOf(
    "127.0.0.0/8",
    "10.0.0.0/8",
    "100.64.0.0/10",
    "169.254.0.0/16",
    "172.16.0.0/12",
    "192.168.0.0/16",
    "198.18.0.0/15",
    "224.0.0.0/4",
    "255.255.255.255/32",
    "::1/128",
    "fc00::/7",
    "fe80::/10",
    "ff00::/8",
)

private val LocalNameSuffixes = listOf(
    "lan",
    "local",
    "home.arpa",
)

private val RuDomainSuffixes = listOf(
    "ru",
    "su",
    "xn--p1ai",
    "xn--d1acj3b",
    "xn--80adxhks",
    "xn--p1acf",
)

private val DomesticDirectDomains = listOf(
    "yandex.ru",
    "ya.ru",
    "yandex.com",
    "vk.com",
    "vk.me",
    "mail.ru",
    "ok.ru",
    "rutube.ru",
    "dzen.ru",
    "kinopoisk.ru",
    "gosuslugi.ru",
    "avito.ru",
    "wildberries.ru",
    "ozon.ru",
    "rambler.ru",
)

private val RuLocalPresetSummaryLines = listOf(
    "Local and special-use IPv4/IPv6 CIDRs route direct.",
    "localhost, .local, and .home.arpa stay on direct DNS and direct routing.",
    "RU-oriented suffixes (.ru, .su, xn--p1ai, xn--d1acj3b, xn--80adxhks, xn--p1acf) route direct.",
    "Domestic direct bundle (Yandex, VK, Mail.ru, Rutube, Ozon, Wildberries, and others) routes direct.",
    "geoip-ru is applied through a local binary rule-set when available.",
)

internal fun RoutingProfile.isRuLocalPresetActive(): Boolean =
    preset == RoutingPreset.RU_LOCAL_V1 && mode == RoutingMode.RULE

internal fun builtInRouteRules(routingProfile: RoutingProfile): List<JsonObject> = buildList {
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
    add(routeToDirectRule("ip_cidr", LocalSpecialCidrs))
    add(routeToDirectRule("domain", listOf("localhost")))
    add(routeToDirectRule("domain_suffix", LocalNameSuffixes))

    if (routingProfile.isRuLocalPresetActive()) {
        add(routeToDirectRule("domain_suffix", RuDomainSuffixes))
        add(routeToDirectRule("domain_suffix", DomesticDirectDomains))
        if (RoutingPresetRuleSetFiles.ruGeoipRuleSetPathProvider() != null) {
            add(
                buildJsonObject {
                    put("rule_set", jsonArrayOf(RuGeoipRuleSetTag))
                    put("action", "route")
                    put("outbound", "direct")
                },
            )
        }
    }
}

internal fun builtInRouteRuleSets(routingProfile: RoutingProfile): List<JsonObject> = buildList {
    if (!routingProfile.isRuLocalPresetActive()) {
        return@buildList
    }

    val localPath = RoutingPresetRuleSetFiles.ruGeoipRuleSetPathProvider() ?: return@buildList
    add(
        buildJsonObject {
            put("type", "local")
            put("tag", RuGeoipRuleSetTag)
            put("format", "binary")
            put("path", localPath)
        },
    )
}

internal fun builtInDnsRules(routingProfile: RoutingProfile): List<JsonObject> = buildList {
    add(dnsToDirectRule("domain", listOf("localhost")))
    add(dnsToDirectRule("domain_suffix", LocalNameSuffixes))

    if (routingProfile.isRuLocalPresetActive()) {
        add(dnsToDirectRule("domain_suffix", RuDomainSuffixes))
        add(dnsToDirectRule("domain_suffix", DomesticDirectDomains))
    }
}

internal fun builtInPresetSummaryLines(routingProfile: RoutingProfile): List<String> = when (routingProfile.preset) {
    RoutingPreset.NONE -> emptyList()
    RoutingPreset.RU_LOCAL_V1 -> RuLocalPresetSummaryLines
}

private fun routeToDirectRule(matchKey: String, values: List<String>): JsonObject = buildJsonObject {
    put(matchKey, jsonArrayOf(*values.toTypedArray()))
    put("action", "route")
    put("outbound", "direct")
}

private fun dnsToDirectRule(matchKey: String, values: List<String>): JsonObject = buildJsonObject {
    put(matchKey, jsonArrayOf(*values.toTypedArray()))
    put("action", "route")
    put("server", "direct-dns")
}

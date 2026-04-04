package io.github.sj42tech.route42.parser

import io.github.sj42tech.route42.model.ConnectionProfileWithRouting
import io.github.sj42tech.route42.model.DnsMode
import io.github.sj42tech.route42.model.MatchType
import io.github.sj42tech.route42.model.RoutingAction
import io.github.sj42tech.route42.model.RoutingMode
import io.github.sj42tech.route42.model.RoutingPreset
import io.github.sj42tech.route42.model.RoutingRule
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object VlessLinkShareCodec {
    fun export(profile: ConnectionProfileWithRouting): String {
        val endpoint = profile.profile.endpoint
        require(endpoint.protocol.name == "VLESS") {
            "Only VLESS profiles can be exported as Route42 share codes"
        }

        val queryParameters = linkedMapOf<String, MutableList<String>>()
        fun addValue(key: String, value: String?) {
            if (value.isNullOrBlank()) {
                return
            }
            queryParameters.getOrPut(key) { mutableListOf() }.add(value)
        }

        addValue(VlessLinkKeys.Encryption, endpoint.encryption)
        addValue(VlessLinkKeys.Flow, endpoint.flow)
        addValue(VlessLinkKeys.Security, endpoint.security)
        addValue(VlessLinkKeys.ServerName, endpoint.serverName)
        addValue(VlessLinkKeys.Fingerprint, endpoint.fingerprint)
        addValue(VlessLinkKeys.PublicKey, endpoint.publicKey)
        addValue(VlessLinkKeys.ShortId, endpoint.shortId)
        if (endpoint.alpn.isNotEmpty()) {
            addValue(VlessLinkKeys.Alpn, endpoint.alpn.joinToString(","))
        }
        addValue(VlessLinkKeys.Type, endpoint.network.ifBlank { "tcp" })

        endpoint.extraQueryParameters.toSortedMap().forEach { (key, values) ->
            values.forEach { value -> addValue(key, value) }
        }

        if (profile.routingProfile.preset != RoutingPreset.NONE) {
            addValue(VlessLinkKeys.Preset, profile.routingProfile.preset.queryValue())
        }
        addValue(VlessLinkKeys.Mode, profile.routingProfile.mode.queryValue())
        addValue(VlessLinkKeys.Dns, profile.routingProfile.dnsMode.queryValue())

        profile.routingProfile.rules
            .filter(RoutingRule::enabled)
            .forEach { rule ->
                addValue(rule.queryKey(), rule.value)
            }

        profile.profile.importedShareLink?.preservedCustomParameters
            .orEmpty()
            .toSortedMap()
            .forEach { (key, values) ->
                values.forEach { value -> addValue(key, value) }
            }

        val query = queryParameters.entries
            .flatMap { (key, values) ->
                values.map { value -> "${encodeQueryComponent(key)}=${encodeQueryComponent(value)}" }
            }
            .joinToString("&")

        val encodedName = encodeQueryComponent(profile.profile.name)
        val authority = "${endpoint.uuid}@${endpoint.server.asAuthorityHost()}:${endpoint.serverPort}"
        val baseLink = "vless://$authority"

        return buildString {
            append(baseLink)
            if (query.isNotBlank()) {
                append('?')
                append(query)
            }
            append('#')
            append(encodedName)
        }
    }
}

private fun RoutingPreset.queryValue(): String = when (this) {
    RoutingPreset.NONE -> "custom"
    RoutingPreset.RU_LOCAL_V1 -> "ru-local-v1"
}

private fun RoutingMode.queryValue(): String = when (this) {
    RoutingMode.DIRECT -> "direct"
    RoutingMode.PROXY -> "proxy"
    RoutingMode.RULE -> "rule"
}

private fun DnsMode.queryValue(): String = when (this) {
    DnsMode.LOCAL -> "local"
    DnsMode.PROXY -> "proxy"
    DnsMode.SPLIT -> "split"
}

private fun RoutingRule.queryKey(): String = when (action to matchType) {
    RoutingAction.DIRECT to MatchType.DOMAIN -> VlessLinkKeys.DirectDomain
    RoutingAction.DIRECT to MatchType.DOMAIN_SUFFIX -> VlessLinkKeys.DirectSuffix
    RoutingAction.DIRECT to MatchType.IP_CIDR -> VlessLinkKeys.DirectCidr
    RoutingAction.PROXY to MatchType.DOMAIN -> VlessLinkKeys.ProxyDomain
    RoutingAction.PROXY to MatchType.DOMAIN_SUFFIX -> VlessLinkKeys.ProxySuffix
    RoutingAction.PROXY to MatchType.IP_CIDR -> VlessLinkKeys.ProxyCidr
    RoutingAction.BLOCK to MatchType.DOMAIN -> VlessLinkKeys.BlockDomain
    RoutingAction.BLOCK to MatchType.DOMAIN_SUFFIX -> VlessLinkKeys.BlockSuffix
    RoutingAction.BLOCK to MatchType.IP_CIDR -> VlessLinkKeys.BlockCidr
    else -> error("Unsupported routing rule combination: $action / $matchType")
}

private fun encodeQueryComponent(value: String): String = URLEncoder
    .encode(value, StandardCharsets.UTF_8)
    .replace("+", "%20")

private fun String.asAuthorityHost(): String {
    if (contains(':') && !startsWith('[') && !endsWith(']')) {
        return "[$this]"
    }
    return this
}

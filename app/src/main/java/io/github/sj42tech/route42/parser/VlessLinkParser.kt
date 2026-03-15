package io.github.sj42tech.route42.parser

import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.DnsMode
import io.github.sj42tech.route42.model.EndpointConfig
import io.github.sj42tech.route42.model.ImportedShareLink
import io.github.sj42tech.route42.model.MatchType
import io.github.sj42tech.route42.model.RoutingAction
import io.github.sj42tech.route42.model.RoutingMode
import io.github.sj42tech.route42.model.RoutingProfile
import io.github.sj42tech.route42.model.RoutingRule
import io.github.sj42tech.route42.model.defaultDnsMode
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

class LinkParseException(message: String) : IllegalArgumentException(message)

object VlessLinkParser {
    fun parse(rawLink: String): ConnectionProfile {
        val normalizedLink = rawLink.trim()
        if (normalizedLink.isEmpty()) {
            throw LinkParseException("Link is empty")
        }

        val uri = runCatching { URI(normalizedLink) }.getOrElse {
            throw LinkParseException("Link is not a valid URI")
        }

        if (!uri.scheme.equals("vless", ignoreCase = true)) {
            throw LinkParseException("Only vless:// links are supported in MVP")
        }

        val uuid = uri.userInfo?.takeIf(String::isNotBlank)
            ?: throw LinkParseException("VLESS link is missing UUID")
        validateUuid(uuid)

        val server = uri.host?.takeIf(String::isNotBlank)
            ?: throw LinkParseException("VLESS link is missing host")
        val port = if (uri.port == -1) 443 else uri.port

        val queryParameters = parseQueryParameters(uri.rawQuery)
        val endpointExtras = queryParameters
            .filterKeys { key -> key !in VlessLinkKeys.KnownEndpointKeys && !VlessLinkKeys.isCustomKey(key) }
            .toSortedMap()
        val preservedCustomParameters = queryParameters
            .filterKeys { key -> VlessLinkKeys.isCustomKey(key) && key !in VlessLinkKeys.KnownCustomKeys }
            .toSortedMap()

        val endpoint = EndpointConfig(
            server = server,
            serverPort = port,
            uuid = uuid,
            network = queryParameters.lastValue(VlessLinkKeys.Type) ?: "tcp",
            security = queryParameters.lastValue(VlessLinkKeys.Security),
            encryption = queryParameters.lastValue(VlessLinkKeys.Encryption),
            flow = queryParameters.lastValue(VlessLinkKeys.Flow),
            serverName = queryParameters.lastValue(VlessLinkKeys.ServerName),
            fingerprint = queryParameters.lastValue(VlessLinkKeys.Fingerprint),
            publicKey = queryParameters.lastValue(VlessLinkKeys.PublicKey),
            shortId = queryParameters.lastValue(VlessLinkKeys.ShortId),
            alpn = queryParameters[VlessLinkKeys.Alpn]
                ?.flatMap { it.split(',') }
                ?.map(String::trim)
                ?.filter(String::isNotEmpty)
                .orEmpty(),
            extraQueryParameters = endpointExtras,
        )

        val name = uri.rawFragment
            ?.takeIf(String::isNotBlank)
            ?.let(::decodeComponent)
            ?: server

        return ConnectionProfile(
            name = name,
            endpoint = endpoint,
            routing = parseRouting(queryParameters),
            importedShareLink = ImportedShareLink(
                extraQueryParameters = endpointExtras,
                preservedCustomParameters = preservedCustomParameters,
            ),
        )
    }

    private fun parseRouting(queryParameters: Map<String, List<String>>): RoutingProfile {
        val mode = queryParameters.lastValue(VlessLinkKeys.ModeKeys)
            ?.let(::parseRoutingMode)
            ?: RoutingMode.PROXY
        val dnsMode = queryParameters.lastValue(VlessLinkKeys.DnsKeys)
            ?.let(::parseDnsMode)
            ?: mode.defaultDnsMode()

        val rules = buildList {
            addRules(queryParameters.valuesOf(VlessLinkKeys.DirectDomainKeys), RoutingAction.DIRECT, MatchType.DOMAIN)
            addRules(queryParameters.valuesOf(VlessLinkKeys.DirectSuffixKeys), RoutingAction.DIRECT, MatchType.DOMAIN_SUFFIX)
            addRules(queryParameters.valuesOf(VlessLinkKeys.DirectCidrKeys), RoutingAction.DIRECT, MatchType.IP_CIDR)

            addRules(queryParameters.valuesOf(VlessLinkKeys.ProxyDomainKeys), RoutingAction.PROXY, MatchType.DOMAIN)
            addRules(queryParameters.valuesOf(VlessLinkKeys.ProxySuffixKeys), RoutingAction.PROXY, MatchType.DOMAIN_SUFFIX)
            addRules(queryParameters.valuesOf(VlessLinkKeys.ProxyCidrKeys), RoutingAction.PROXY, MatchType.IP_CIDR)

            addRules(queryParameters.valuesOf(VlessLinkKeys.BlockDomainKeys), RoutingAction.BLOCK, MatchType.DOMAIN)
            addRules(queryParameters.valuesOf(VlessLinkKeys.BlockSuffixKeys), RoutingAction.BLOCK, MatchType.DOMAIN_SUFFIX)
            addRules(queryParameters.valuesOf(VlessLinkKeys.BlockCidrKeys), RoutingAction.BLOCK, MatchType.IP_CIDR)
        }

        return RoutingProfile(
            mode = mode,
            dnsMode = dnsMode,
            rules = rules,
        )
    }

    private fun MutableList<RoutingRule>.addRules(
        values: List<String>,
        action: RoutingAction,
        matchType: MatchType,
    ) {
        values
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach { value ->
                add(
                    RoutingRule(
                        action = action,
                        matchType = matchType,
                        value = value,
                    ),
                )
            }
    }

    private fun parseRoutingMode(value: String): RoutingMode = when (value.lowercase()) {
        "direct" -> RoutingMode.DIRECT
        "proxy" -> RoutingMode.PROXY
        "rule" -> RoutingMode.RULE
        else -> throw LinkParseException("Unknown routing mode: $value")
    }

    private fun parseDnsMode(value: String): DnsMode = when (value.lowercase()) {
        "local" -> DnsMode.LOCAL
        "proxy" -> DnsMode.PROXY
        "split" -> DnsMode.SPLIT
        else -> throw LinkParseException("Unknown DNS mode: $value")
    }

    private fun parseQueryParameters(rawQuery: String?): Map<String, List<String>> {
        if (rawQuery.isNullOrBlank()) {
            return emptyMap()
        }

        val valuesByKey = linkedMapOf<String, MutableList<String>>()
        rawQuery.split('&')
            .filter(String::isNotBlank)
            .forEach { entry ->
                val delimiterIndex = entry.indexOf('=')
                val rawKey = if (delimiterIndex >= 0) entry.substring(0, delimiterIndex) else entry
                val rawValue = if (delimiterIndex >= 0) entry.substring(delimiterIndex + 1) else ""
                val key = decodeComponent(rawKey)
                val value = decodeComponent(rawValue)
                valuesByKey.getOrPut(key) { mutableListOf() }.add(value)
            }

        return valuesByKey.mapValues { (_, values) -> values.toList() }
    }

    private fun decodeComponent(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)

    private fun validateUuid(value: String) {
        runCatching { UUID.fromString(value) }.getOrElse {
            throw LinkParseException("Invalid VLESS UUID")
        }
    }
}

private fun Map<String, List<String>>.lastValue(keys: List<String>): String? = keys.firstNotNullOfOrNull { key ->
    this[key]?.lastOrNull()
}

private fun Map<String, List<String>>.lastValue(key: String): String? = this[key]?.lastOrNull()

private fun Map<String, List<String>>.valuesOf(keys: List<String>): List<String> = buildList {
    keys.forEach { key ->
        addAll(this@valuesOf[key].orEmpty())
    }
}

package io.sj42.vpn.parser

import io.sj42.vpn.model.ConnectionProfile
import io.sj42.vpn.model.DnsMode
import io.sj42.vpn.model.EndpointConfig
import io.sj42.vpn.model.ImportedShareLink
import io.sj42.vpn.model.MatchType
import io.sj42.vpn.model.RoutingAction
import io.sj42.vpn.model.RoutingMode
import io.sj42.vpn.model.RoutingProfile
import io.sj42.vpn.model.RoutingRule
import io.sj42.vpn.model.defaultDnsMode
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
            .filterKeys { key -> key !in VlessLinkKeys.KnownEndpointKeys && !key.startsWith(VlessLinkKeys.CustomPrefix) }
            .toSortedMap()
        val preservedCustomParameters = queryParameters
            .filterKeys { key -> key.startsWith(VlessLinkKeys.CustomPrefix) && key !in VlessLinkKeys.KnownCustomKeys }
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
                raw = normalizedLink,
                extraQueryParameters = endpointExtras,
                preservedCustomParameters = preservedCustomParameters,
            ),
        )
    }

    private fun parseRouting(queryParameters: Map<String, List<String>>): RoutingProfile {
        val mode = queryParameters.lastValue(VlessLinkKeys.Mode)
            ?.let(::parseRoutingMode)
            ?: RoutingMode.PROXY
        val dnsMode = queryParameters.lastValue(VlessLinkKeys.Dns)
            ?.let(::parseDnsMode)
            ?: mode.defaultDnsMode()

        val rules = buildList {
            addRules(queryParameters[VlessLinkKeys.DirectDomain], RoutingAction.DIRECT, MatchType.DOMAIN)
            addRules(queryParameters[VlessLinkKeys.DirectSuffix], RoutingAction.DIRECT, MatchType.DOMAIN_SUFFIX)
            addRules(queryParameters[VlessLinkKeys.DirectCidr], RoutingAction.DIRECT, MatchType.IP_CIDR)

            addRules(queryParameters[VlessLinkKeys.ProxyDomain], RoutingAction.PROXY, MatchType.DOMAIN)
            addRules(queryParameters[VlessLinkKeys.ProxySuffix], RoutingAction.PROXY, MatchType.DOMAIN_SUFFIX)
            addRules(queryParameters[VlessLinkKeys.ProxyCidr], RoutingAction.PROXY, MatchType.IP_CIDR)

            addRules(queryParameters[VlessLinkKeys.BlockDomain], RoutingAction.BLOCK, MatchType.DOMAIN)
            addRules(queryParameters[VlessLinkKeys.BlockSuffix], RoutingAction.BLOCK, MatchType.DOMAIN_SUFFIX)
            addRules(queryParameters[VlessLinkKeys.BlockCidr], RoutingAction.BLOCK, MatchType.IP_CIDR)
        }

        return RoutingProfile(
            mode = mode,
            dnsMode = dnsMode,
            rules = rules,
        )
    }

    private fun MutableList<RoutingRule>.addRules(
        values: List<String>?,
        action: RoutingAction,
        matchType: MatchType,
    ) {
        values.orEmpty()
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

private fun Map<String, List<String>>.lastValue(key: String): String? = this[key]?.lastOrNull()

package io.sj42.vpn.model

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class RoutingProfile(
    val mode: RoutingMode = RoutingMode.PROXY,
    val dnsMode: DnsMode = mode.defaultDnsMode(),
    val rules: List<RoutingRule> = emptyList(),
)

@Serializable
enum class RoutingMode {
    DIRECT,
    PROXY,
    RULE,
}

@Serializable
enum class DnsMode {
    LOCAL,
    PROXY,
    SPLIT,
}

@Serializable
data class RoutingRule(
    val id: String = UUID.randomUUID().toString(),
    val action: RoutingAction,
    val matchType: MatchType,
    val value: String,
    val enabled: Boolean = true,
)

@Serializable
enum class RoutingAction {
    DIRECT,
    PROXY,
    BLOCK,
}

@Serializable
enum class MatchType {
    DOMAIN,
    DOMAIN_SUFFIX,
    IP_CIDR,
}

fun RoutingMode.defaultDnsMode(): DnsMode = when (this) {
    RoutingMode.DIRECT -> DnsMode.LOCAL
    RoutingMode.PROXY -> DnsMode.PROXY
    RoutingMode.RULE -> DnsMode.SPLIT
}

package io.github.sj42tech.route42.model

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class RoutingProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = DefaultName,
    val preset: RoutingPreset = RoutingPreset.NONE,
    val mode: RoutingMode = RoutingMode.PROXY,
    val dnsMode: DnsMode = mode.defaultDnsMode(),
    val rules: List<RoutingRule> = emptyList(),
) {
    companion object {
        const val DefaultName = "Custom routing"
    }
}

@Serializable
enum class RoutingPreset {
    NONE,
    RU_LOCAL_V1,
}

fun RoutingPreset.defaultProfileName(): String = when (this) {
    RoutingPreset.NONE -> RoutingProfile.DefaultName
    RoutingPreset.RU_LOCAL_V1 -> "Rule (RU + Local)"
}

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
    val source: RoutingRuleSource = RoutingRuleSource.USER,
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

@Serializable
enum class RoutingRuleSource {
    USER,
    IMPORTED,
    PRESET,
}

fun RoutingMode.defaultDnsMode(): DnsMode = when (this) {
    RoutingMode.DIRECT -> DnsMode.LOCAL
    RoutingMode.PROXY -> DnsMode.PROXY
    RoutingMode.RULE -> DnsMode.SPLIT
}

fun RoutingProfile.duplicate(
    id: String = UUID.randomUUID().toString(),
    name: String = this.name,
): RoutingProfile = copy(
    id = id,
    name = name,
    rules = rules.map { rule -> rule.copy(id = UUID.randomUUID().toString()) },
)

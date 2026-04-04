package io.github.sj42tech.route42.model

fun RoutingPreset.label(): String = when (this) {
    RoutingPreset.NONE -> "Custom"
    RoutingPreset.RU_LOCAL_V1 -> "RU + Local"
}

fun RoutingMode.label(): String = when (this) {
    RoutingMode.DIRECT -> "Direct"
    RoutingMode.PROXY -> "Proxy"
    RoutingMode.RULE -> "Rule"
}

fun DnsMode.label(): String = when (this) {
    DnsMode.LOCAL -> "Local DNS"
    DnsMode.PROXY -> "Proxy DNS"
    DnsMode.SPLIT -> "Split DNS"
}

fun RoutingAction.label(): String = when (this) {
    RoutingAction.DIRECT -> "Direct"
    RoutingAction.PROXY -> "Proxy"
    RoutingAction.BLOCK -> "Block"
}

fun MatchType.label(): String = when (this) {
    MatchType.DOMAIN -> "Domain"
    MatchType.DOMAIN_SUFFIX -> "Suffix"
    MatchType.IP_CIDR -> "CIDR"
}

fun RoutingRuleSource.label(): String = when (this) {
    RoutingRuleSource.USER -> "Custom"
    RoutingRuleSource.IMPORTED -> "Imported"
    RoutingRuleSource.PRESET -> "Preset"
}

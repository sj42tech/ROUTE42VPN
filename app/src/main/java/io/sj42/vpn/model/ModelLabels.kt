package io.sj42.vpn.model

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

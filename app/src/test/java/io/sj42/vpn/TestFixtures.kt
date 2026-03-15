package io.sj42.vpn

import io.sj42.vpn.model.ConnectionProfile
import io.sj42.vpn.model.DnsMode
import io.sj42.vpn.model.EndpointConfig
import io.sj42.vpn.model.MatchType
import io.sj42.vpn.model.RoutingAction
import io.sj42.vpn.model.RoutingMode
import io.sj42.vpn.model.RoutingProfile
import io.sj42.vpn.model.RoutingRule

object TestFixtures {
    const val ProfileName = "edge-profile"
    const val Server = "203.0.113.10"
    const val Port = 443
    const val Uuid = "11111111-2222-4333-8444-555555555555"
    const val ServerName = "cdn.example"
    const val PublicKey = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE"
    const val ShortId = "a1b2"

    val StandardRealityLink =
        "vless://$Uuid@$Server:$Port?" +
            "encryption=none&flow=xtls-rprx-vision&security=reality&sni=$ServerName&" +
            "fp=chrome&pbk=$PublicKey&sid=$ShortId&spx=%2F&type=tcp#$ProfileName"

    fun sampleProfile(
        routingMode: RoutingMode = RoutingMode.RULE,
        dnsMode: DnsMode = DnsMode.SPLIT,
        rules: List<RoutingRule> = listOf(
            RoutingRule(
                action = RoutingAction.DIRECT,
                matchType = MatchType.DOMAIN,
                value = "portal.example",
            ),
            RoutingRule(
                action = RoutingAction.PROXY,
                matchType = MatchType.DOMAIN,
                value = "tunnel.example",
            ),
        ),
    ): ConnectionProfile = ConnectionProfile(
        name = ProfileName,
        endpoint = EndpointConfig(
            server = Server,
            serverPort = Port,
            uuid = Uuid,
            network = "tcp",
            security = "reality",
            flow = "xtls-rprx-vision",
            serverName = ServerName,
            fingerprint = "chrome",
            publicKey = PublicKey,
            shortId = ShortId,
        ),
        routing = RoutingProfile(
            mode = routingMode,
            dnsMode = dnsMode,
            rules = rules,
        ),
    )
}

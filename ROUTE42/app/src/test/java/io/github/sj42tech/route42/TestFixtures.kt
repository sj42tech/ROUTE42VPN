package io.github.sj42tech.route42

import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.ConnectionProfileWithRouting
import io.github.sj42tech.route42.model.DnsMode
import io.github.sj42tech.route42.model.EndpointConfig
import io.github.sj42tech.route42.model.MatchType
import io.github.sj42tech.route42.model.RoutingAction
import io.github.sj42tech.route42.model.RoutingMode
import io.github.sj42tech.route42.model.RoutingProfile
import io.github.sj42tech.route42.model.RoutingPreset
import io.github.sj42tech.route42.model.RoutingRule

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
        endpoint: EndpointConfig = sampleEndpoint(),
    ): ConnectionProfile = ConnectionProfile(
        name = ProfileName,
        endpoint = endpoint,
    )

    fun sampleRoutingProfile(
        preset: RoutingPreset = RoutingPreset.NONE,
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
    ): RoutingProfile = RoutingProfile(
        name = "$ProfileName routing",
        preset = preset,
        mode = routingMode,
        dnsMode = dnsMode,
        rules = rules,
    )

    fun sampleResolvedProfile(
        preset: RoutingPreset = RoutingPreset.NONE,
        routingMode: RoutingMode = RoutingMode.RULE,
        dnsMode: DnsMode = DnsMode.SPLIT,
        endpoint: EndpointConfig = sampleEndpoint(),
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
    ): ConnectionProfileWithRouting {
        val profile = sampleProfile(endpoint = endpoint)
        return ConnectionProfileWithRouting(
            profile = profile,
            routingProfile = sampleRoutingProfile(
                preset = preset,
                routingMode = routingMode,
                dnsMode = dnsMode,
                rules = rules,
            ).copy(id = profile.routingProfileId),
        )
    }

    fun sampleEndpoint(
        network: String = "tcp",
        extraQueryParameters: Map<String, List<String>> = emptyMap(),
    ): EndpointConfig = EndpointConfig(
            server = Server,
            serverPort = Port,
            uuid = Uuid,
            network = network,
            security = "reality",
            flow = "xtls-rprx-vision",
            serverName = ServerName,
            fingerprint = "chrome",
            publicKey = PublicKey,
            shortId = ShortId,
            extraQueryParameters = extraQueryParameters,
        )
}

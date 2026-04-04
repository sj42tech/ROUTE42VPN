package io.github.sj42tech.route42.parser

internal object VlessLinkKeys {
    const val PublicPrefix = "x-route42-"

    const val Encryption = "encryption"
    const val Flow = "flow"
    const val Security = "security"
    const val ServerName = "sni"
    const val Fingerprint = "fp"
    const val PublicKey = "pbk"
    const val ShortId = "sid"
    const val Alpn = "alpn"
    const val Type = "type"

    const val Mode = "${PublicPrefix}mode"
    const val Dns = "${PublicPrefix}dns"
    const val Preset = "${PublicPrefix}preset"
    const val DirectDomain = "${PublicPrefix}direct-domain"
    const val DirectSuffix = "${PublicPrefix}direct-suffix"
    const val DirectCidr = "${PublicPrefix}direct-cidr"
    const val ProxyDomain = "${PublicPrefix}proxy-domain"
    const val ProxySuffix = "${PublicPrefix}proxy-suffix"
    const val ProxyCidr = "${PublicPrefix}proxy-cidr"
    const val BlockDomain = "${PublicPrefix}block-domain"
    const val BlockSuffix = "${PublicPrefix}block-suffix"
    const val BlockCidr = "${PublicPrefix}block-cidr"
    const val HomeSsid = "${PublicPrefix}home-ssid"
    const val HomeMode = "${PublicPrefix}home-mode"

    val ModeKeys = listOf(Mode)
    val DnsKeys = listOf(Dns)
    val PresetKeys = listOf(Preset)
    val DirectDomainKeys = listOf(DirectDomain)
    val DirectSuffixKeys = listOf(DirectSuffix)
    val DirectCidrKeys = listOf(DirectCidr)
    val ProxyDomainKeys = listOf(ProxyDomain)
    val ProxySuffixKeys = listOf(ProxySuffix)
    val ProxyCidrKeys = listOf(ProxyCidr)
    val BlockDomainKeys = listOf(BlockDomain)
    val BlockSuffixKeys = listOf(BlockSuffix)
    val BlockCidrKeys = listOf(BlockCidr)
    val HomeSsidKeys = listOf(HomeSsid)
    val HomeModeKeys = listOf(HomeMode)

    val KnownEndpointKeys = setOf(
        Encryption,
        Flow,
        Security,
        ServerName,
        Fingerprint,
        PublicKey,
        ShortId,
        Alpn,
        Type,
    )

    val KnownCustomKeys = setOf(
        Mode,
        Dns,
        Preset,
        DirectDomain,
        DirectSuffix,
        DirectCidr,
        ProxyDomain,
        ProxySuffix,
        ProxyCidr,
        BlockDomain,
        BlockSuffix,
        BlockCidr,
        HomeSsid,
        HomeMode,
    )

    fun isCustomKey(key: String): Boolean = key.startsWith(PublicPrefix)
}

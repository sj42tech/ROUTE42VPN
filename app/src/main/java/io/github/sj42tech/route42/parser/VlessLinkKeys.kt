package io.github.sj42tech.route42.parser

internal object VlessLinkKeys {
    const val PublicPrefix = "x-route42-"
    const val LegacyPrefix = "x-sj42-"

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

    const val LegacyMode = "${LegacyPrefix}mode"
    const val LegacyDns = "${LegacyPrefix}dns"
    const val LegacyDirectDomain = "${LegacyPrefix}direct-domain"
    const val LegacyDirectSuffix = "${LegacyPrefix}direct-suffix"
    const val LegacyDirectCidr = "${LegacyPrefix}direct-cidr"
    const val LegacyProxyDomain = "${LegacyPrefix}proxy-domain"
    const val LegacyProxySuffix = "${LegacyPrefix}proxy-suffix"
    const val LegacyProxyCidr = "${LegacyPrefix}proxy-cidr"
    const val LegacyBlockDomain = "${LegacyPrefix}block-domain"
    const val LegacyBlockSuffix = "${LegacyPrefix}block-suffix"
    const val LegacyBlockCidr = "${LegacyPrefix}block-cidr"
    const val LegacyHomeSsid = "${LegacyPrefix}home-ssid"
    const val LegacyHomeMode = "${LegacyPrefix}home-mode"

    val ModeKeys = listOf(Mode, LegacyMode)
    val DnsKeys = listOf(Dns, LegacyDns)
    val DirectDomainKeys = listOf(DirectDomain, LegacyDirectDomain)
    val DirectSuffixKeys = listOf(DirectSuffix, LegacyDirectSuffix)
    val DirectCidrKeys = listOf(DirectCidr, LegacyDirectCidr)
    val ProxyDomainKeys = listOf(ProxyDomain, LegacyProxyDomain)
    val ProxySuffixKeys = listOf(ProxySuffix, LegacyProxySuffix)
    val ProxyCidrKeys = listOf(ProxyCidr, LegacyProxyCidr)
    val BlockDomainKeys = listOf(BlockDomain, LegacyBlockDomain)
    val BlockSuffixKeys = listOf(BlockSuffix, LegacyBlockSuffix)
    val BlockCidrKeys = listOf(BlockCidr, LegacyBlockCidr)
    val HomeSsidKeys = listOf(HomeSsid, LegacyHomeSsid)
    val HomeModeKeys = listOf(HomeMode, LegacyHomeMode)

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
        LegacyMode,
        LegacyDns,
        LegacyDirectDomain,
        LegacyDirectSuffix,
        LegacyDirectCidr,
        LegacyProxyDomain,
        LegacyProxySuffix,
        LegacyProxyCidr,
        LegacyBlockDomain,
        LegacyBlockSuffix,
        LegacyBlockCidr,
        LegacyHomeSsid,
        LegacyHomeMode,
    )

    fun isCustomKey(key: String): Boolean = key.startsWith(PublicPrefix) || key.startsWith(LegacyPrefix)
}

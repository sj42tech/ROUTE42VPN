package io.sj42.vpn.parser

internal object VlessLinkKeys {
    const val CustomPrefix = "x-sj42-"

    const val Encryption = "encryption"
    const val Flow = "flow"
    const val Security = "security"
    const val ServerName = "sni"
    const val Fingerprint = "fp"
    const val PublicKey = "pbk"
    const val ShortId = "sid"
    const val Alpn = "alpn"
    const val Type = "type"

    const val Mode = "x-sj42-mode"
    const val Dns = "x-sj42-dns"
    const val DirectDomain = "x-sj42-direct-domain"
    const val DirectSuffix = "x-sj42-direct-suffix"
    const val DirectCidr = "x-sj42-direct-cidr"
    const val ProxyDomain = "x-sj42-proxy-domain"
    const val ProxySuffix = "x-sj42-proxy-suffix"
    const val ProxyCidr = "x-sj42-proxy-cidr"
    const val BlockDomain = "x-sj42-block-domain"
    const val BlockSuffix = "x-sj42-block-suffix"
    const val BlockCidr = "x-sj42-block-cidr"
    const val HomeSsid = "x-sj42-home-ssid"
    const val HomeMode = "x-sj42-home-mode"

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
    )
}

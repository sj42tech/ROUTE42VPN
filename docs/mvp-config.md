# MVP Config

## Goal

The MVP converts an imported `vless://` link into a `sing-box` client config for Android.

Routing target:

- private and LAN traffic goes `direct`;
- selected internal domains can go `direct`;
- explicitly blocked domains go `block`;
- everything else goes through `proxy`;
- DNS follows the selected profile mode.

## Endpoint Example

```text
vless://11111111-2222-4333-8444-555555555555@203.0.113.10:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=cdn.example&fp=chrome&pbk=AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE&sid=a1b2&spx=%2F&type=tcp#edge-profile
```

## Link To `sing-box` Mapping

```text
host = 203.0.113.10
port = 443
uuid = 11111111-2222-4333-8444-555555555555
type = tcp
security = reality
flow = xtls-rprx-vision
sni = cdn.example
fp = chrome
pbk = AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE
sid = a1b2
```

Mapped outbound:

```json
{
  "type": "vless",
  "tag": "proxy",
  "server": "203.0.113.10",
  "server_port": 443,
  "uuid": "11111111-2222-4333-8444-555555555555",
  "flow": "xtls-rprx-vision",
  "network": "tcp",
  "tls": {
    "enabled": true,
    "server_name": "cdn.example",
    "utls": {
      "enabled": true,
      "fingerprint": "chrome"
    },
    "reality": {
      "enabled": true,
      "public_key": "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE",
      "short_id": "a1b2"
    }
  }
}
```

## Generated MVP Shape

```json
{
  "log": {
    "level": "info"
  },
  "dns": {
    "servers": [
      {
        "type": "local",
        "tag": "direct-dns"
      },
      {
        "type": "https",
        "tag": "proxy-dns",
        "server": "1.1.1.1",
        "server_port": 443,
        "path": "/dns-query",
        "detour": "proxy"
      }
    ],
    "rules": [
      {
        "domain_suffix": [
          "lan",
          "local",
          "home.arpa",
          "internal"
        ],
        "action": "route",
        "server": "direct-dns"
      },
      {
        "domain": [
          "portal.example",
          "intranet.example"
        ],
        "action": "route",
        "server": "direct-dns"
      }
    ],
    "final": "proxy-dns"
  },
  "inbounds": [
    {
      "type": "tun",
      "tag": "tun-in",
      "address": [
        "172.19.0.1/30",
        "fdfe:dcba:9876::1/126"
      ],
      "auto_route": true,
      "stack": "system"
    },
    {
      "type": "socks",
      "tag": "app-socks",
      "listen": "127.0.0.1",
      "listen_port": 39080
    }
  ],
  "outbounds": [
    {
      "type": "vless",
      "tag": "proxy",
      "server": "203.0.113.10",
      "server_port": 443,
      "uuid": "11111111-2222-4333-8444-555555555555",
      "flow": "xtls-rprx-vision",
      "packet_encoding": "xudp",
      "tls": {
        "enabled": true,
        "server_name": "cdn.example",
        "utls": {
          "enabled": true,
          "fingerprint": "chrome"
        },
        "reality": {
          "enabled": true,
          "public_key": "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE",
          "short_id": "a1b2"
        }
      }
    },
    {
      "type": "direct",
      "tag": "direct"
    },
    {
      "type": "block",
      "tag": "block"
    }
  ],
  "route": {
    "final": "proxy",
    "rules": [
      {
        "port": 53,
        "action": "hijack-dns"
      },
      {
        "port": 853,
        "ip_cidr": [
          "127.0.0.0/8",
          "::1/128"
        ],
        "action": "reject",
        "method": "default"
      },
      {
        "ip_is_private": true,
        "action": "route",
        "outbound": "direct"
      },
      {
        "domain_suffix": [
          "lan",
          "local",
          "home.arpa",
          "internal"
        ],
        "action": "route",
        "outbound": "direct"
      },
      {
        "domain": [
          "portal.example",
          "intranet.example"
        ],
        "action": "route",
        "outbound": "direct"
      }
    ]
  }
}
```

## Notes

- `spx` is preserved as opaque metadata for round-trip export.
- The generator emits a local SOCKS inbound so the app can probe the real exit IP through the tunnel.
- The TUN config excludes the server IP from auto-routing when the endpoint is an IP literal.
- DNS port `853` loopback noise is suppressed with an explicit reject rule.
- The config generator is source-of-truth; the raw JSON is not manually edited in the UI.

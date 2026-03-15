# Link And Routing Spec

## Purpose

`Route42` imports standard `vless://` links and accepts extra routing parameters with the `x-route42-*` prefix.

For the detailed user-facing guide, see [import-link-routing-guide.md](import-link-routing-guide.md).

The import link is only the transport envelope. After parsing, the app stores a normalized profile with:

- endpoint settings;
- routing mode;
- DNS mode;
- editable routing rules;
- preserved unknown query parameters for safe round-trip export.

## Standard VLESS Fields

The MVP reads the following fields from a regular `vless://` link:

- `uuid`
- `host`
- `port`
- `type`
- `security`
- `encryption`
- `flow`
- `sni`
- `fp`
- `pbk`
- `sid`
- `alpn`
- fragment as display name

Example import link:

```text
vless://11111111-2222-4333-8444-555555555555@203.0.113.10:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=cdn.example&fp=chrome&pbk=AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE&sid=a1b2&spx=%2F&type=tcp#edge-profile
```

Normalized endpoint model:

```json
{
  "type": "vless",
  "server": "203.0.113.10",
  "serverPort": 443,
  "uuid": "11111111-2222-4333-8444-555555555555",
  "network": "tcp",
  "security": "reality",
  "flow": "xtls-rprx-vision",
  "serverName": "cdn.example",
  "fingerprint": "chrome",
  "publicKey": "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE",
  "shortId": "a1b2",
  "displayName": "edge-profile"
}
```

## Preserved Unknown Parameters

Unknown parameters are kept in the saved profile so the original share link can be re-exported without losing metadata.

Example:

```json
{
  "extraQueryParameters": {
    "spx": ["/"]
  }
}
```

## Custom Routing Parameters

Custom keys use the `x-route42-` prefix.

Legacy `x-sj42-*` keys are still accepted for backward compatibility when importing older links.

### Mode

- `x-route42-mode=direct`
- `x-route42-mode=proxy`
- `x-route42-mode=rule`

### DNS

- `x-route42-dns=local`
- `x-route42-dns=proxy`
- `x-route42-dns=split`

### Direct Rules

- `x-route42-direct-domain=portal.example`
- `x-route42-direct-domain=intranet.example`
- `x-route42-direct-suffix=internal`
- `x-route42-direct-cidr=192.168.0.0/16`

### Proxy Rules

- `x-route42-proxy-domain=tunnel.example`
- `x-route42-proxy-suffix=external`
- `x-route42-proxy-cidr=198.51.100.0/24`

### Block Rules

- `x-route42-block-domain=ads.example`
- `x-route42-block-suffix=tracking.example`
- `x-route42-block-cidr=203.0.113.128/25`

### Reserved Keys

- `x-route42-home-ssid`
- `x-route42-home-mode`

These names are reserved, but they are not applied to routing behavior in the current build.

## Example With Routing Parameters

```text
vless://11111111-2222-4333-8444-555555555555@203.0.113.10:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=cdn.example&fp=chrome&pbk=AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE&sid=a1b2&type=tcp&x-route42-mode=rule&x-route42-dns=split&x-route42-direct-domain=portal.example&x-route42-direct-domain=intranet.example&x-route42-direct-suffix=internal&x-route42-direct-cidr=192.168.0.0%2F16&x-route42-proxy-domain=tunnel.example&x-route42-block-suffix=tracking.example#edge-profile-rules
```

## Saved Domain Model

```text
ConnectionProfile
- id
- name
- endpoint: EndpointConfig
- routing: RoutingProfile
- importedShareLink: ImportedShareLink?

EndpointConfig
- protocol
- server
- serverPort
- uuid
- network
- security
- encryption
- flow
- serverName
- fingerprint
- publicKey
- shortId
- alpn
- extraQueryParameters

RoutingProfile
- mode
- dnsMode
- rules: List<RoutingRule>

RoutingRule
- id
- action
- matchType
- value
- enabled

ImportedShareLink
- raw
- extraQueryParameters
- preservedCustomParameters
```

## Import Rules

1. Standard VLESS fields map to `EndpointConfig`.
2. `x-route42-*` routing keys map to `RoutingProfile`.
3. Legacy `x-sj42-*` routing keys are also accepted during import.
4. Unknown non-routing keys are preserved for export.
5. Missing routing parameters default to `RoutingMode.PROXY`.
6. After saving, the UI edits the normalized profile model, not the raw URL.

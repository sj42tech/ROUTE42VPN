# Link And Routing Spec

## Purpose

`Route42` imports standard `vless://` links and accepts extra routing parameters with the `x-route42-*` prefix.

For the detailed user-facing guide, see [import-link-routing-guide.md](import-link-routing-guide.md).

The import link is only the transport envelope. After parsing, the app stores a normalized profile with:

- endpoint settings in `ConnectionProfile`;
- a routing profile reference from the connection;
- routing mode and DNS mode in `RoutingProfile`;
- editable routing rules with rule provenance;
- preserved unknown query parameters as saved metadata.

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

If the link uses `security=reality`, the current parser requires:

- `sni`
- `fp`
- `pbk`
- `sid`

If any of them is missing, import fails before the profile can be saved.

## Preserved Unknown Parameters

Unknown parameters are kept in the saved profile as normalized metadata.

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

## Built-In Presets

Built-in routing presets are app-side routing profiles, not extra `x-route42-*` keys in the raw link.

Current built-in preset:

- `Rule (RU + Local)`

That preset adds:

- local and special-use direct CIDRs;
- `localhost`, `.local`, and `.home.arpa` direct handling;
- RU-oriented direct suffixes;
- curated domestic direct domains;
- `geoip-ru` through a local binary sing-box `rule_set`.

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
- routingProfileId
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
- id
- name
- preset
- mode
- dnsMode
- rules: List<RoutingRule>

RoutingRule
- id
- action
- matchType
- value
- source
- enabled

ImportedShareLink
- extraQueryParameters
- preservedCustomParameters
```

## Import Rules

1. Standard VLESS fields map to `EndpointConfig`.
2. If `security=reality`, `sni`, `fp`, `pbk`, and `sid` are required.
3. `x-route42-*` routing keys map to `RoutingRule` entries inside `RoutingProfile`.
4. Rules imported from `x-route42-*` are saved with `source=IMPORTED`.
5. Rules added later in the UI are saved with `source=USER`.
6. Built-in presets are assigned in the app and are not encoded in the raw import link.
7. Unknown non-routing keys are preserved as saved metadata.
8. Missing routing parameters default to `RoutingMode.PROXY`.
9. After saving, the UI edits the normalized connection/routing models, not the raw URL.

## Runtime Layering

At config generation time the runtime rule layers are:

1. built-in safety rules
2. built-in preset rules, if a preset is assigned
3. imported rules from the link
4. manual user rules
5. final outbound fallback from routing mode

# Import Link And Routing Guide

## Purpose

This guide describes:

- how to import a `vless://` link into `Route42`;
- which transport parameters are read from the link;
- which `x-route42-*` query parameters control routing;
- how imported routing rules behave after the profile is saved.

## How To Import A Link

1. Open the app.
2. Tap `Import`.
3. Paste the full `vless://...` link into the input field.
4. Confirm the parsed preview.
5. Save the profile.

After saving:

- the raw share link is not stored as part of the saved profile;
- the app converts the link into a `ConnectionProfile` plus a reusable `RoutingProfile`;
- transport settings stay in the connection profile;
- imported routing rules are tagged as imported in the UI;
- routing mode, DNS mode, and imported rules become editable on the profile screens;
- routing rules can then be changed manually on the `Routes` screen or reused from another saved routing profile.

## Minimal Supported Link Format

Minimal example:

```text
vless://11111111-2222-4333-8444-555555555555@203.0.113.10:443#edge-profile
```

Typical Reality example:

```text
vless://11111111-2222-4333-8444-555555555555@203.0.113.10:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=cdn.example&fp=chrome&pbk=AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE&sid=a1b2&spx=%2F&type=tcp#edge-profile
```

The app currently supports only `vless://` links in MVP.

If the link uses `security=reality`, the current build requires all of these fields before save:

- `sni`
- `fp`
- `pbk`
- `sid`

If any of them is missing, import fails on the preview screen instead of creating a broken saved profile.

## Standard VLESS Parameters

These fields are read from a normal `vless://` link and mapped into the endpoint profile:

| Link field | Meaning in app | Notes |
| --- | --- | --- |
| `userInfo` | `uuid` | Required. Must be a valid UUID. |
| `host` | `server` | Required. |
| `port` | `serverPort` | Optional. Defaults to `443` if omitted. |
| `type` | `network` | Defaults to `tcp` if omitted. |
| `security` | `security` | Example: `reality`. |
| `encryption` | `encryption` | Example: `none`. |
| `flow` | `flow` | Example: `xtls-rprx-vision`. |
| `sni` | `serverName` | TLS server name. |
| `fp` | `fingerprint` | Example: `chrome`. |
| `pbk` | `publicKey` | Reality public key. |
| `sid` | `shortId` | Reality short id. |
| `alpn` | `alpn` | Comma-separated values are split into a list. |
| `#fragment` | `name` | Used as the visible profile name. |

Unknown non-`x-route42-*` query parameters are preserved in the endpoint extras. This is how parameters such as `spx=%2F` survive import.

## Routing Parameter Prefix

All new routing parameters use this prefix:

```text
x-route42-
```

Example:

```text
x-route42-mode=rule
```

## Routing Modes

The profile mode is controlled by `x-route42-mode`.

Allowed values:

- `x-route42-mode=direct`
- `x-route42-mode=proxy`
- `x-route42-mode=rule`

Behavior:

| Value | Effect |
| --- | --- |
| `direct` | Final outbound is direct. Custom rules still exist and can override specific destinations. |
| `proxy` | Final outbound is proxy. This is the default if the parameter is omitted. |
| `rule` | Final outbound is proxy, but rule-based matching is enabled and built-in local safety exclusions still stay direct. |

Important details:

- local and special-use IP ranges are always routed direct;
- `localhost`, `lan`, `local`, and `home.arpa` are always routed direct;
- the built-in `Rule (RU + Local)` preset is a reusable app-side routing profile, not a raw `x-route42-mode` value.

## DNS Modes

DNS behavior is controlled by `x-route42-dns`.

Allowed values:

- `x-route42-dns=local`
- `x-route42-dns=proxy`
- `x-route42-dns=split`

Behavior:

| Value | Effect |
| --- | --- |
| `local` | Final DNS server is local/direct DNS. |
| `proxy` | Final DNS server is proxy DNS over the tunnel. |
| `split` | DNS rules are used for matching, but unmatched lookups still fall back to proxy DNS. |

Default DNS mode if `x-route42-dns` is omitted:

| Routing mode | Default DNS mode |
| --- | --- |
| `direct` | `local` |
| `proxy` | `proxy` |
| `rule` | `split` |

## Shared Routing Profiles And Presets

Imported links do not store routing inline forever.

Current app behavior:

- one saved connection points at one routing profile;
- one routing profile can be reused by several saved VPS connections;
- imported `x-route42-*` rules are stored as imported rules inside that routing profile;
- the built-in `Rule (RU + Local)` preset can be created in the app and assigned to several connections;
- the preset layer is shown separately from editable imported and custom rules.

## Rule Parameters

Each routing rule is imported from one repeated query parameter.

### Direct Rules

| Parameter | Match type in app | Example |
| --- | --- | --- |
| `x-route42-direct-domain` | `Domain` | `x-route42-direct-domain=bank.example` |
| `x-route42-direct-suffix` | `Suffix` | `x-route42-direct-suffix=corp.local` |
| `x-route42-direct-cidr` | `CIDR` | `x-route42-direct-cidr=192.168.0.0%2F16` |

### Proxy Rules

| Parameter | Match type in app | Example |
| --- | --- | --- |
| `x-route42-proxy-domain` | `Domain` | `x-route42-proxy-domain=youtube.com` |
| `x-route42-proxy-suffix` | `Suffix` | `x-route42-proxy-suffix=googlevideo.com` |
| `x-route42-proxy-cidr` | `CIDR` | `x-route42-proxy-cidr=198.51.100.0%2F24` |

### Block Rules

| Parameter | Match type in app | Example |
| --- | --- | --- |
| `x-route42-block-domain` | `Domain` | `x-route42-block-domain=ads.example` |
| `x-route42-block-suffix` | `Suffix` | `x-route42-block-suffix=tracker.example` |
| `x-route42-block-cidr` | `CIDR` | `x-route42-block-cidr=203.0.113.128%2F25` |

## Meaning Of Match Types

`Domain`

- exact host match;
- use it for a specific hostname like `api.example.com`.

`Suffix`

- suffix match;
- use it when you want a whole zone, for example `example.com` or `corp.local`;
- this is the better choice when subdomains should follow the same route.

`CIDR`

- IP subnet match;
- use it for local subnets, office ranges, or provider ranges;
- the slash in CIDR should be URL-encoded as `%2F`.

## Repeating Parameters

Rule parameters can be repeated multiple times.

Example:

```text
...&x-route42-direct-domain=bank1.example&x-route42-direct-domain=bank2.example&x-route42-direct-domain=bank3.example
```

This imports three separate direct `Domain` rules.

The following parameters use the last value if repeated:

- `x-route42-mode`
- `x-route42-dns`

## URL Encoding Rules

The link should be URL-encoded normally.

Most important cases:

- `/` inside CIDR is recommended to be encoded as `%2F` for portability;
- spaces in profile names or values should be encoded;
- `#` starts the fragment and becomes the visible profile name;
- `&` separates query parameters, so it cannot appear raw inside a value.

Recommended:

```text
x-route42-direct-cidr=192.168.0.0%2F16
```

Also usually works:

```text
x-route42-direct-cidr=192.168.0.0/16
```

## Import Order And Rule Order

Imported rules are grouped by parser category, not by the exact raw query order in the URL.

Current import order inside the saved profile is:

1. direct domains
2. direct suffixes
3. direct CIDRs
4. proxy domains
5. proxy suffixes
6. proxy CIDRs
7. block domains
8. block suffixes
9. block CIDRs

Within one parameter family, repeated values keep their original order.

All rules imported from `x-route42-*` parameters are tagged as `Imported` in the UI. Rules created later from the `Routes` screen are tagged as `Custom`.

## Priority And Overrides

The generated route list puts built-in safety rules before imported custom rules.

In practice this means:

- local and special-use networks keep direct priority;
- `localhost`, `lan`, `local`, and `home.arpa` keep direct priority;
- if the routing profile uses `Rule (RU + Local)`, its built-in direct rules are evaluated before imported and manual rules;
- imported and manual custom rules mainly override the final fallback route, not those built-in exclusions.

## What Gets Added Automatically By The App

The generated `sing-box` config adds built-in behavior on top of imported custom rules.

Always added:

- DNS hijack on port `53`;
- rejection of loopback DoT on `127.0.0.1:853` and `::1:853`;
- direct routing for local and special-use IP ranges such as RFC1918, CGNAT, link-local, multicast, and ULA space;
- direct routing for `localhost`, `lan`, `local`, and `home.arpa`.

Added only when the assigned routing profile uses `Rule (RU + Local)`:

- direct routing for `.ru`, `.su`, `xn--p1ai`, `xn--d1acj3b`, `xn--80adxhks`, and `xn--p1acf`;
- direct routing for the bundled domestic direct domains such as `yandex.ru`, `vk.com`, `mail.ru`, `rutube.ru`, `wildberries.ru`, and `ozon.ru`;
- direct `geoip-ru` matching through a local binary sing-box `rule_set`.

This means you do not need to duplicate local safety rules in every link, and you can move several VPS profiles onto one shared `RU + Local` preset instead of copy-pasting those domains by hand.

## DNS Matching Nuance

DNS rules are generated only for hostname-based rules:

- `Domain`
- `Suffix`

`CIDR` rules affect traffic routing, but they do not create DNS match rules because CIDR is applied after address resolution.

## Editing After Import

After the link is saved, the routing data becomes editable in the app:

- `Mode` can be changed on the profile screen;
- `DNS` mode can be changed on the profile screen;
- the connection can be moved onto another saved routing profile;
- imported rules can be enabled, disabled, edited, or deleted on the `Routes` screen;
- new direct, proxy, and block rules can be added manually;
- manually added rules are tagged as `Custom`;
- preset rules are shown as a read-only built-in summary and are not edited as raw rows.

The app edits the normalized internal profile, not the raw URL text.

## Reserved And Unsupported Keys

The parser currently recognizes these names as reserved:

- `x-route42-home-ssid`
- `x-route42-home-mode`

In the current build they are not applied to routing behavior and should not be used as active configuration knobs.

Unknown `x-route42-*` custom keys are preserved as custom metadata, but they do not affect routing unless the app adds explicit support for them later.

## Recommended Example: Personal Split Routing

```text
vless://11111111-2222-4333-8444-555555555555@203.0.113.10:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=cdn.example&fp=chrome&pbk=AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE&sid=a1b2&spx=%2F&type=tcp&x-route42-mode=rule&x-route42-dns=split&x-route42-direct-domain=gosuslugi.ru&x-route42-direct-domain=online.sberbank.ru&x-route42-direct-suffix=nalog.gov.ru&x-route42-proxy-domain=api.telegram.org&x-route42-proxy-suffix=googlevideo.com&x-route42-block-suffix=doubleclick.net#home-split-profile
```

What this does:

- the tunnel uses the imported Reality transport settings;
- final routing stays proxy because mode is `rule`;
- explicitly listed bank and government hosts also go direct;
- Telegram API and Google video traffic go through the proxy;
- `doubleclick.net` is blocked;
- local names and private networks stay direct.

If you later switch this connection to the built-in `Rule (RU + Local)` preset, the RU suffix bundle, domestic direct bundle, and `geoip-ru` rule-set are added on top of those imported rules.

## Recommended Example: Everything Through Proxy

```text
vless://11111111-2222-4333-8444-555555555555@203.0.113.10:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=cdn.example&fp=chrome&pbk=AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE&sid=a1b2&spx=%2F&type=tcp&x-route42-mode=proxy&x-route42-dns=proxy#full-proxy
```

What this does:

- almost all traffic goes through the proxy;
- private IPs and LAN names still stay direct because they are built-in exclusions.

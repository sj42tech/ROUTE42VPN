# Route42 Team VPS And Routing Brief 2026-04-04

This note is a direct handoff brief for the `Route42` Android team.

Its purpose is to give the app team two things:

- a clear picture of the currently running self-hosted VPS fleet behind the project;
- a practical routing recommendation for `RU + local` direct traffic so the Android client can evolve its split-routing behavior without reverse-engineering the desktop lab setup.

## Why This Exists

The desktop and VPS lab around this project already proved a few practical things:

- multiple VPS backends are needed, because network quality changes by provider, ASN, and operator;
- direct local traffic should not be forced into the tunnel;
- Russian domestic traffic often benefits from a conservative direct policy;
- the Android client should treat transport settings and routing policy as separate layers.

In other words:

- the tunnel endpoint is one layer;
- split-routing policy is another layer;
- the app should model both cleanly.

## Current Running VPS Fleet

As of `2026-04-04`, the project has three active self-hosted VPS nodes.

| Role | Provider | Public IP | Region | Notes |
| --- | --- | --- | --- | --- |
| Primary | `Hostkey` | `5.39.219.74` | Amsterdam, NL | Current best live path and desktop baseline |
| Reserve | `Exoscale` | `194.182.174.240` | Vienna, AT | Useful reserve and comparison path |
| Experimental | `AWS` | `3.34.30.191` | Seoul region, KR | Kept for comparison, but currently weaker as a route |

### Common server-side transport profile

All three servers are currently aligned on the same broad server-side shape:

- `Xray 26.2.6`
- `VLESS`
- `Reality`
- `TCP`
- port `443`

Current operational details such as:

- current `SNI`
- current Reality `publicKey`
- current Reality `shortId`
- and any future transport rotation

must be treated as profile data, not as app logic.

That means:

- the Android app should import and store those fields from the `vless://` link;
- the app must not hardcode one provider, one SNI, or one Reality keypair as product behavior.

## Practical Fleet Recommendation For The App

The Android app should assume that one user can have more than one active backend profile at the same time.

Recommended product stance:

- one profile can be `primary`
- one can be `reserve`
- one can be `experimental`

The app does not need automatic failover first, but it should make manual switching and comparison easy.

At minimum:

- saved profiles should preserve friendly labels such as `Hostkey`, `Exoscale`, `AWS`;
- the app should make it obvious which profile is the current default;
- transport data must stay profile-specific;
- routing policy can be shared across profiles.

## Current Desktop Routing Policy

The desktop lab has already converged on a practical split-routing policy.

The current behavior is:

- local and special-use IP space goes `direct`
- local naming zones go `direct`
- a conservative set of RU-oriented TLDs goes `direct`
- `geoip:ru` goes `direct`
- selected large domestic services also go `direct`
- everything else goes through the proxy tunnel

This is the important mental model for the Android team:

- `transport settings` tell the app how to reach the VPS;
- `routing settings` tell the app what should bypass that VPS.

## Recommended Built-In Direct Rules

These rules are the strongest candidate for a built-in `Rule (RU + Local)` mode in `Route42`.

### 1. Always direct: local and special-use IP ranges

Recommended direct CIDR set:

- `127.0.0.0/8`
- `10.0.0.0/8`
- `100.64.0.0/10`
- `169.254.0.0/16`
- `172.16.0.0/12`
- `192.168.0.0/16`
- `198.18.0.0/15`
- `224.0.0.0/4`
- `255.255.255.255/32`
- `::1/128`
- `fc00::/7`
- `fe80::/10`
- `ff00::/8`

This set covers:

- loopback
- RFC1918 private LAN
- CGNAT
- link-local
- benchmarking/test ranges
- multicast
- local IPv6 and ULA space

The app should treat these as hard safety exclusions and keep them `direct` in every normal routing mode.

### 2. Always direct: local naming zones

Recommended built-in direct local names:

- exact `localhost`
- suffix `.local`
- suffix `.home.arpa`

These should also stay `direct` in every normal routing mode.

### 3. Recommended direct: conservative RU-oriented TLD set

Recommended built-in direct suffixes:

- `.ru`
- `.su`
- `.рф` represented as punycode `xn--p1ai`
- `.дети` represented as `xn--d1acj3b`
- `.москва` represented as `xn--80adxhks`
- `.рус` represented as `xn--p1acf`

This is a broader and more realistic set than only `.ru` and `.rf`.

Important product note:

- these should be built-in defaults for the `RU + Local` mode;
- they should still be visible and overrideable by advanced users.

### 4. Recommended direct: geoip Russia

Recommended built-in direct geo policy:

- `geoip:ru`

Reason:

- many domestic services are not cleanly modeled by suffix alone;
- not everything relevant lives under `.ru`;
- a country-IP direct rule is a useful second layer under the suffix rules.

## Recommended Domestic Direct Domain Set

In addition to the built-in local and RU rules, the desktop setup currently keeps a small curated list of major domestic services `direct`.

Recommended exact or suffix direct entries:

- `yandex.ru`
- `ya.ru`
- `yandex.com`
- `vk.com`
- `vk.me`
- `mail.ru`
- `ok.ru`
- `rutube.ru`
- `dzen.ru`
- `kinopoisk.ru`
- `gosuslugi.ru`
- `avito.ru`
- `wildberries.ru`
- `ozon.ru`
- `rambler.ru`

This list should not necessarily be hardwired forever.

Recommended app behavior:

- ship these as a preset bundle for `Rule (RU + Local)`
- let the user inspect, add, disable, or remove individual entries later

That gives the product a strong default without making it rigid.

## Recommended Final Routing Behavior

The Android app should support a practical rule mode shaped like this:

1. local and special-use IP ranges -> `direct`
2. local naming zones -> `direct`
3. RU-oriented built-in suffixes -> `direct`
4. `geoip:ru` -> `direct`
5. curated domestic domain bundle -> `direct`
6. explicit user direct rules -> `direct`
7. explicit user proxy rules -> `proxy`
8. explicit user block rules -> `block`
9. everything else -> `proxy`

This gives a clean result:

- local network keeps working
- domestic services do not waste tunnel capacity
- foreign traffic still uses the tunnel by default

## Recommended DNS Behavior

For this routing mode, the best default is:

- routing mode: `rule`
- DNS mode: `split`

Practical expectation:

- direct-bound local and domestic traffic should be allowed to use local/direct DNS
- proxy-bound traffic should resolve over proxy DNS

The app does not need to expose every implementation detail to the user, but the internal generator should preserve this separation.

## Product Guidance For Route42

Recommended next product changes:

### 1. Expand built-in `rule` mode

Current docs mention built-ins like `.ru` and `.rf`.
The more practical recommended built-in profile is now:

- local/private CIDRs
- `localhost`, `.local`, `.home.arpa`
- `.ru`, `.su`, `.рф`, `.дети`, `.москва`, `.рус`
- `geoip:ru`

### 2. Separate preset policy from user rules

The app should distinguish:

- built-in preset rules
- imported link rules
- manually edited rules

This makes it easier to update the preset later without corrupting user intent.

### 3. Keep transport import strict and routing import flexible

Transport import should remain strict:

- `server`
- `port`
- `security`
- `flow`
- `sni`
- `fp`
- `pbk`
- `sid`

Routing import should remain flexible:

- built-in route preset
- imported `x-route42-*` rules
- editable user overrides

### 4. Do not hardcode one provider worldview

The app should not assume:

- one provider
- one datacenter
- one SNI
- one Reality keypair

It should assume multiple saved backends and one reusable routing policy layer.

## Recommended Team Mental Model

For the app team, the clean implementation model is:

- a `ConnectionProfile` points at one VPS backend
- a `RoutingProfile` defines split behavior
- one routing profile can be reused by several VPS profiles

That allows this exact real-world setup:

- `Hostkey` profile
- `Exoscale` profile
- `AWS` profile
- one shared `RU + Local` routing profile

## Good First Implementation Target

If the team wants the smallest useful next step, this is it:

1. keep transport import as it already works
2. add a built-in routing preset named something like `Rule (RU + Local)`
3. make that preset generate:
   - direct local CIDRs
   - direct local names
   - direct RU suffixes
   - direct `geoip:ru`
4. let users add optional custom direct domains on top

That alone would already match the desktop operational policy much better than a narrower `.ru/.rf only` rule set.

## Operational Summary

The VPS fleet is real, active, and multi-provider.
The routing policy is now the bigger product opportunity.

The Android app should evolve toward:

- provider-agnostic transport profiles
- reusable routing presets
- stronger built-in `RU + Local` direct behavior
- user-editable overrides on top of those defaults

That would bring `Route42` much closer to the routing behavior that is already proving useful in the desktop lab.

## Implementation Status In App

As of `2026-04-04`, the Android app already implements the main shape recommended in this brief:

- `ConnectionProfile` and `RoutingProfile` are stored separately
- one routing profile can be reused by several saved VPS profiles
- the built-in preset `Rule (RU + Local)` exists in the app UI
- `geoip:ru` is implemented through a local sing-box binary `rule_set`, not the removed legacy `geoip` matcher
- imported `x-route42-*` rules and manual user rules are tracked separately in the UI
- `Reality` transport import is strict and requires `sni`, `fp`, `pbk`, and `sid`

That means the remaining work is mostly release polish, docs, and follow-up routing UX rather than the core architecture itself.

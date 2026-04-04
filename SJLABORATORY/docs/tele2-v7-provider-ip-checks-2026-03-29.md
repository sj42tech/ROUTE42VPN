# Tele2 Provider IP Checks Via MikroTik v7 — 2026-03-29

This note records a stricter provider-IP reachability check through the `Tele2` mobile hotspot path using the isolated `MikroTik v7` lab router.

## Safety

- the MacBook stayed on the working `v6` network
- the live desktop `local.xray` tunnel was not stopped or reconfigured
- the `v7` lab router was used as the test pivot
- after each test, `v7` routing was restored:
  - `ether1` default route distance back to `1`
  - `wifi2` hotspot route distance back to `10`

## Test Method

To make the test stricter than earlier `src-address` probes, the `v7` router was temporarily switched so that the phone hotspot on `wifi2` became the primary default route.

Then, for each target IP:

1. a temporary `dstnat` rule was added on `v7`
2. a high TCP port on `192.168.0.106` was forwarded to `TARGET_IP:443`
3. the MacBook tested that forwarded port with `nc`
4. the temporary rules were removed and routing was restored

This gives a practical answer to:

- can `TCP 443` be reached through the real `Tele2` uplink path
- not just from the phone itself, but from a client routed through the `v7` lab router

## Result Summary

All tested targets were `down` on `TCP 443` through the real `Tele2` path.

This included:

- control IP `1.1.1.1`
- `Telegram` web IP `149.154.167.99`
- all current project VPS ingress IPs
- multiple provider-site IPs from the project provider tracker

## Tested Targets

### Control and Telegram

| Label | IP | Result |
| --- | --- | --- |
| `cf` | `1.1.1.1` | `down` |
| `telegram` | `149.154.167.99` | `down` |

### Current and Historical VPS IPs

| Label | IP | Result |
| --- | --- | --- |
| `hostkey_vps` | `5.39.219.74` | `down` |
| `exoscale_vps` | `194.182.174.240` | `down` |
| `aws_vps` | `3.34.30.191` | `down` |

### Provider Site IPs

| Label | IP | Result |
| --- | --- | --- |
| `hostkey_site` | `8.6.112.6` | `down` |
| `exoscale_site` | `159.100.253.88` | `down` |
| `hetzner_site` | `213.133.116.44` | `down` |
| `ovh_site` | `198.27.92.14` | `down` |
| `scaleway_site` | `172.66.157.101` | `down` |
| `aws_site` | `13.33.235.114` | `down` |
| `alibaba_site` | `47.91.64.21` | `down` |
| `rackcorp_site` | `103.69.72.70` | `down` |
| `u1host_site` | `64.188.114.188` | `down` |
| `cloudmn_site` | `103.50.204.94` | `down` |
| `vultr_site` | `104.17.140.186` | `down` |

Additional note:

- `www.tencentcloud.com` had already resolved to `0.0.0.1` in the direct-provider checks, so it was not treated as a meaningful target IP for this batch

## Interpretation

The hotspot link itself is alive:

- `wifi2` associates to `A35HOTSPOT`
- `wifi2` gets a DHCP lease
- the phone gateway `10.36.173.186` is visible as the first hop

But once the `Tele2` path becomes the real primary WAN, `TCP 443` to the external control set collapses across the board.

Practical conclusion:

- for this project, current `Tele2` behavior is consistent with an effective `blackhole` for the tested encrypted egress path
- the failure is broader than a Telegram-only issue
- the problem appears before traffic ever reaches `Hostkey`, `Exoscale`, `AWS`, or the tested provider control-plane IPs

## Domain-Level Whitelist Check

To test whether a domain-based allowlist still existed for large mainstream vendors, a second batch was run through the same `Tele2`-primary `v7` path.

Method:

- temporary `dstnat` rules on `v7` forwarded high local ports on `192.168.0.106` to remote `:443`
- the MacBook then used `curl --resolve` so that the request still carried the original hostname in `SNI` and HTTP `Host`
- this is stricter than a plain IP-only probe because it tests real domain-oriented `HTTPS`

Tested domains:

- `www.apple.com`
- `apps.apple.com`
- `www.samsung.com`
- `www.microsoft.com`
- `play.google.com`

Result:

- all five failed with `curl: (28) Failed to connect`
- no `TCP` connect phase completed
- no TLS handshake started

Practical interpretation:

- if a `Tele2` whitelist exists in the current path, it does not include ordinary public `HTTPS` access to these tested Apple, Samsung, Microsoft, or Google Play domains
- at least in this lab setup, the path does not look like a normal "public internet minus a few blocked sites" model

## Large-Domain Mixed Batch

A broader mixed-domain batch was then tested through the same `Tele2`-primary `v7` path using the same strict method:

- temporary `dstnat` on `v7`
- MacBook `curl --resolve`
- real hostname preserved in `SNI` and HTTP `Host`

### International and Global Control Domains

| Domain | Result |
| --- | --- |
| `google.com` | `down` |
| `www.youtube.com` | `down` |
| `www.github.com` | `down` |
| `www.cloudflare.com` | `down` |
| `www.wikipedia.org` | `down` |

### RU-Mass-Market Domains

| Domain | Result |
| --- | --- |
| `ya.ru` | `up` (`302`) |
| `yandex.ru` | `up` (`302`) |
| `vk.com` | `up` (`302`) |
| `www.gosuslugi.ru` | `TCP connect ok`, then TLS reset (`SSL_ERROR_SYSCALL`) |
| `www.sberbank.ru` | `down` |
| `www.tbank.ru` | `down` |
| `www.nalog.gov.ru` | `up` (`301`) |

## Updated Interpretation

The current `Tele2` path does not behave like a normal full internet connection, but it also does not behave like a total zero-access blackhole.

What it currently looks like instead:

- most large international public `HTTPS` destinations fail
- some RU-oriented mass-market domains still answer normally
- some government or public-service domains partially work
- some sensitive RU services reach `TCP` but then fail during or before TLS

Practical reading:

- the path is consistent with a highly selective allowlist or policy-gated internet profile
- for VPN and external infrastructure work, this still behaves as effectively unusable

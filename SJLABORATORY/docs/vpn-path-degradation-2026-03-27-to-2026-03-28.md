# VPN Path Degradation 2026-03-27 To 2026-03-28

This note captures the later route-health diagnostics after the original `2026-03-22` VPS audit.

## Scope

- compare the active laptop path to `Hostkey 5.39.219.74` and `Exoscale 194.182.174.240`
- separate server-side health from client-to-server route degradation
- record what is confirmed versus what remains only a hypothesis

## Current Summary

As of `2026-03-28`:

- the live desktop tunnel is back on `Hostkey 5.39.219.74`
- both VPS servers are healthy and reachable on `:443`
- both VPS servers have strong server-side outbound performance
- the degradation is primarily on the client-to-server path, not inside the VPS hosts
- `Hostkey` remains the least-bad live route
- `Exoscale` is still materially worse from the local client network

## What Is Confirmed

### 1. The current live tunnel is on Hostkey

Observed locally on `2026-03-28`:

- `local.xray` was active through `secrets/PROXY/xray/config.json`
- the active outbound server in `config.json` was `5.39.219.74`
- `curl --proxy http://127.0.0.1:8080 https://ifconfig.me/ip` returned `5.39.219.74`

### 2. Both server entry points are still reachable

Observed locally on `2026-03-28`:

- repeated `nc -zvw5 5.39.219.74 443` succeeded
- repeated `nc -zvw5 194.182.174.240 443` succeeded

This matters because the route is not failing at the first TCP handshake step.

### 3. Both VPS servers are healthy on their own side

Observed over SSH on `2026-03-28`:

- `xray.service` was `active` on both servers
- `ss -ltnp` showed `:443` listening on both servers
- both servers could reach external internet targets directly

Server-side sample performance on `2026-03-28`:

- `Hostkey 5.39.219.74`
  - `google_204 total`: about `0.026 s`
  - `github total`: about `0.107 s`
  - `speed.cloudflare.com` 5 MB test: about `27.8 MB/s`
- `Exoscale 194.182.174.240`
  - `google_204 total`: about `0.056 s`
  - `github total`: about `0.178 s`
  - `speed.cloudflare.com` 5 MB test: about `47.5 MB/s`

Operational meaning:

- neither VPS looked overloaded, stalled, or broken during the diagnostic window
- the later slowdown cannot be explained by a simple server outage

## Local Route Diagnostics

### Snapshot: 2026-03-27

Live desktop tunnel through `Hostkey`:

- HTTP `speed.cloudflare.com` 5 MB test: about `2.10 MB/s`
- SOCKS `speed.cloudflare.com` 5 MB test: about `2.16 MB/s`
- `github`, `cloudflare`, `wikipedia`, and `youtube` were usable
- `apple.com` already showed abnormal TLS delay spikes

Temporary canary comparison from the same laptop and network:

- `Hostkey` canary 5 MB test: about `1.80 MB/s`
- `Exoscale` canary 5 MB test: about `242 KB/s`

Interpretation:

- the route to `Exoscale` was already substantially worse than the route to `Hostkey`
- the problem was not limited to the live desktop config

### Snapshot: 2026-03-28

Live desktop tunnel through `Hostkey` degraded further:

- HTTP `speed.cloudflare.com` 5 MB test: about `877 KB/s`
- SOCKS `speed.cloudflare.com` 5 MB test: about `674 KB/s`
- `github`, `cloudflare`, `wikipedia`, and `youtube` all slowed down versus the earlier snapshot

Temporary canary comparison on `2026-03-28`:

- `Hostkey` canary 5 MB test: about `1.82 MB/s`
- `Exoscale` canary 5 MB test: about `219 KB/s`

Other `2026-03-28` canary observations:

- `Hostkey` still led `Exoscale` on `cloudflare`, `youtube`, and large download throughput
- `Exoscale` remained reachable and usable, but significantly slower

Interpretation:

- the route degradation worsened even for the current best path
- `Exoscale` remains more affected than `Hostkey`

## Working Hypothesis

The current best explanation is:

- TCP connection establishment to the VPS still works
- the servers themselves still work
- throughput and higher-layer HTTPS traffic degrade after connection establishment

This pattern is more consistent with one of these:

- DPI-driven traffic shaping
- selective throttling of VPN-like traffic
- route congestion or degraded peering on the client side
- provider or regional network policy changes between the client network and the VPS ASN

## What Is Not Proven

It is not currently proven that:

- the filtering is targeted specifically at this user or this project
- the blocks are being actively tuned on these exact VPS IPs
- one specific operator or regulator action is the only cause

The available evidence supports route degradation and selective impairment.
It does not prove intent.

## Tele2 / T2 Mobile Hypothesis

Direct Tele2 USB-uplink diagnostics have not yet been completed in this repo because the Samsung phone was visible over USB, but macOS did not expose it as a separate network interface during the last attempt.

Still, the external operating context does match the user report that some mobile sessions behave like a whitelist:

- official T2 support note about internet access in blocked areas:
  - <https://ekt.t2.ru/help/article/internet-access-in-blocked-areas>
- March 2026 reporting on whitelist-style mobile internet restrictions:
  - <https://ria.ru/20260312/belyy-spisok-saytov-2080159945.html>
  - <https://www.kommersant.ru/doc/8511876>

These sources support the hypothesis that, in some locations or time windows, mobile internet can degrade into a much narrower allowlist model.
They do not replace a direct Tele2 measurement from the laptop.

## Practical Decision

Keep this operating stance until new evidence appears:

- use `Hostkey 5.39.219.74` as the main live tunnel
- keep `Exoscale 194.182.174.240` only as a reserve and comparison path
- treat current issues as route-health and filtering problems, not as a VPS service outage

## Research Backlog

1. Run `ops/diagnose-secondary-uplink.sh` once Tele2 appears as a real separate macOS interface.
2. Compare Tele2 raw direct tests to Tele2 Xray canary tests against both `Hostkey` and `Exoscale`.
3. Repeat the same diagnostics at multiple times of day to see whether degradation is time-window based.
4. Keep a dated log of `speed.cloudflare.com`, `google_204`, `github`, `youtube`, and `apple.com` results for trend analysis.
5. If needed, add one more reserve VPS in a different ASN for comparison against `Hostkey` and `Exoscale`.

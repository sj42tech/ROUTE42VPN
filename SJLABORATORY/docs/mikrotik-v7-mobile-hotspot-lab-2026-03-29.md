# MikroTik v7 Mobile Hotspot Lab — 2026-03-29

This note records the isolated test setup built to study alternate uplinks such as a phone hotspot without touching the main desktop tunnel on the working router.

## Goal

Keep the production laptop path on the working `v6` MikroTik and the live `local.xray` tunnel, while running a separate lab router behind it for experiments with a mobile hotspot.

## Topology

```text
Provider cable
  ->
MikroTik v6 (working router)
  LAN 192.168.0.0/24
  gateway 192.168.0.1
  live desktop tunnel remains here
  ->
MikroTik v7 on ether1 (lab WAN)
  WAN address from v6: 192.168.0.106
  lab LAN: 192.168.88.0/24
  lab gateway: 192.168.88.1
  ->
MacBook or test clients join v7 only when needed
```

## Lab Router

- Router: `MikroTik hAP ax3`
- RouterOS: `7.18.2`
- LAN address: `192.168.88.1/24`
- WAN uplink to `v6`: `ether1`
- WAN DHCP from `v6`: `192.168.0.106/24`
- WAN gateway: `192.168.0.1`

## Wi-Fi Roles

- `wifi1`:
  - remains the local lab access point
  - used to join the lab router directly when local setup is needed
- `wifi2`:
  - removed from the LAN bridge
  - added to interface list `WAN`
  - used as a client for the phone hotspot

## Remote Management Rule

To allow future work from the working `v6` side without reconnecting the MacBook to `v7`, a dedicated input firewall rule was added on `v7`:

- source network: `192.168.0.0/24`
- ingress interface: `ether1`
- allowed ports: `22,80,8291,8728,8729`
- comment: `allow mgmt from v6 LAN`

This keeps the default WAN drop rule in place for everything else.

## Phone Hotspot Result

The phone hotspot was first visible with a Cyrillic SSID, but RouterOS CLI truncated the non-ASCII name over SSH. After renaming the hotspot to ASCII-only `A35HOTSPOT`, the connection succeeded.

Working hotspot state:

- hotspot SSID: `A35HOTSPOT`
- hotspot password: stored only on the phone and local notes
- `wifi2` mode: `station-pseudobridge`
- `wifi2` associated successfully to the phone hotspot
- `wifi2` DHCP lease:
  - address: `10.36.173.205/24`
  - gateway: `10.36.173.186`
  - route distance: `10`

This means:

- `ether1 -> v6` remains the primary path
- `wifi2 -> mobile hotspot` is an active secondary path
- the lab router can now be used to compare fixed-line and mobile uplinks separately

## Why This Matters

This lab layout avoids disturbing the main desktop environment:

- `v6` remains the production router
- the provider cable still lands on `v6`
- the working desktop tunnel remains on `v6`
- `v7` can be changed, tested, and even broken without taking down the main path

## Practical Workflow

1. Keep the MacBook on `v6` for normal work and the live tunnel.
2. Reach `v7` from `v6` using its WAN address `192.168.0.106`.
3. Use `wifi2` on `v7` for mobile hotspot or Tele2-path experiments.
4. Use `wifi1` only when a direct local recovery or one-time setup on `v7` is required.

## Current Tele2 Finding

The hotspot link itself is up, but the real internet path through it is currently severely degraded.

Observed on `2026-03-29`:

- with `wifi2` left as a secondary path, simple `src-address` tests on `v7` looked partially promising, but these should be treated as non-conclusive because the primary default route still pointed to `ether1`
- when `wifi2` was temporarily promoted to the primary default route on `v7`, the first hop clearly changed to the phone gateway `10.36.173.186`
- in that real Tele2-primary state, external reachability from `v7` collapsed:
  - `ping 1.1.1.1` timed out
  - `ping 5.39.219.74` timed out
  - `ping 149.154.167.99` timed out
  - temporary `Netwatch` `tcp-conn` checks to `1.1.1.1:443`, `5.39.219.74:443`, `194.182.174.240:443`, and `149.154.167.99:443` all stayed `down`

Current interpretation:

- the `v7 -> phone hotspot` Wi-Fi link is healthy
- the blockage starts after traffic leaves `v7` and tries to use the phone's mobile path as the real WAN
- this matches the user-visible symptom on the phone itself, where Telegram stays in endless `connecting`

Follow-up report:

- `docs/tele2-v7-provider-ip-checks-2026-03-29.md` records a stricter `TCP 443` provider-IP batch test through the real Tele2-primary path
- `docs/tele2-domain-matrix-2026-03-29.md` records a broader domain-level matrix through the same `v7 -> Tele2 hotspot` lab path

Operational note:

- after each forced Tele2 test, `ether1` was restored to route distance `1`
- `wifi2` was restored to route distance `10`
- the working `v6` path and the main desktop `local.xray` tunnel were left untouched

## Next Step

Use the `v7` router as the mobile-uplink lab node and run Tele2 diagnostics from there while leaving the main laptop tunnel untouched on the working `v6` path.

# VPS Move Helpers

This folder contains helper scripts for moving the current self-hosted Xray/VLESS setup between providers and for recovering the laptop tunnel when experiments fail.

See also:

- `docs/vps-audit-2026-03-22.md` for the original VPS audit and migration record
- `docs/vpn-path-degradation-2026-03-27-to-2026-03-28.md` for the later route degradation record
- `docs/mikrotik-v6-wan-duplex-fix-2026-04-03.md` for the working-router WAN duplex mismatch incident, the `100M/full` workaround, and the before/after measurements
- `docs/mikrotik-v7-mobile-hotspot-lab-2026-03-29.md` for the isolated `v7` lab topology behind the working `v6` router
- `docs/tele2-domain-matrix-2026-03-29.md` for the broader `Tele2` domain matrix collected through the `v7` hotspot lab
- `docs/dpi-lab.md` for the layered `DNS`/`TCP`/`TLS`/`HTTP` diagnostics guide
- `../../ROUTE42/docs/route42-team-vps-and-routing-brief-2026-04-04.md` for the Android team handoff on the current VPS fleet and the recommended `RU + local` split-routing policy

## Provider Status

- live desktop tunnel: `Hostkey 5.39.219.74`
- rollback baseline: `Hostkey 5.39.219.74`
- degraded reserve path: `Exoscale 194.182.174.240`
- failed experimental provider: `Vultr 108.61.171.121`
- primary next research track: `Exoscale`
- secondary paused research track: `uHost`

## What is here

- `hetzner-create-server.sh`: creates a small Hetzner Cloud server through the official API
- `aws-lightsail-create-server.sh`: creates a small AWS Lightsail server through the official CLI
- `exoscale-create-server.sh`: creates a small Exoscale instance through the official API
- `audit-xray-fleet.sh`: checks `Xray` version, service state, and `:443` listener state across the VPS fleet
- `diagnose-proxy.sh`: measures exit IP, latency, and download throughput through a local HTTP or SOCKS proxy
- `dpi-lab.sh`: compares direct-path `DNS`, `TCP`, `TLS`, and `HTTP` behavior with optional reference-VPS replay and leaves `local.xray` untouched
- `diagnose-direct-provider-path.sh`: checks provider ingress IPs and provider sites directly with no proxy while leaving `local.xray` untouched
- `diagnose-provider-access-island.sh`: checks the direct provider path for gateway surface, DNS, captive-portal or walled-garden signs, first hops, and basic direct internet reachability without touching `local.xray`
- `diagnose-vps-and-tunnels.sh`: runs a matrix check for VPS health and temporary canary tunnels for Hostkey, Exoscale, and AWS configs
- `diagnose-secondary-uplink.sh`: probes a second interface such as a Tele2 hotspot without touching the live `local.xray` tunnel
- `diagnose-tele2-domain-matrix.sh`: runs a larger domain batch through the `v7 -> Tele2 hotspot` lab path and writes a markdown report under `docs`
- `tele2-domain-matrix.domains.tsv`: canonical tracked list of domains used for recurring `Tele2` hotspot checks and historical comparison
- `compare-tele2-domain-matrix.sh`: compares two historical Tele2 matrix markdown reports and summarizes regressions or recoveries by domain
- `watch-v6-wan-link.sh`: shows a live refreshed dashboard for the working MikroTik `v6` WAN link with duplex, partner mode, error-counter deltas, and a short ping to the provider gateway while you physically reseat or move the WAN cable; `MODE=line` keeps the older one-line log style
- `test-v6-forced-full-duplex.sh`: runs a controlled `before -> forced 100M/full -> after` throughput comparison on the working MikroTik `v6` WAN link and records router error-counter deltas for each phase
- `render-client-xray-from-template.sh`: renders a client split-routing config from one canonical template in `secrets`
- `push-server-xray-reference.sh`: pushes the canonical server-side `Xray` config from `secrets` to one or more VPS nodes with backup and rollback
- `switch-to-exoscale.sh`: validates and optionally cuts over `local.xray` to the Exoscale VPS with automatic rollback
- `switch-to-aws.sh`: validates and optionally cuts over `local.xray` to the AWS VPS with automatic rollback
- `sync-xray-from-reference.sh`: copies the exact server-side `Xray` install from the reference host to one or more target VPS nodes
- `vultr-create-server.sh`: creates a small Vultr Cloud Compute instance through the official API
- `migrate-xray-from-old-vps.sh`: installs Xray on the new host and copies the current config and systemd files from the old host
- `switch-to-new-vps.sh`: runs a canary against `108.61.171.121` and only performs a live cutover if you opt in explicitly
- `rollback-to-baseline.sh`: restores the local Xray client config back to the working reference server `5.39.219.74`
- `stop-tunnel-and-disable-wifi-proxy.sh`: stops the local Xray launch agent and disables macOS Wi-Fi HTTP, HTTPS, SOCKS, and auto-proxy settings

## Current Position

- do not stop, restart, unload, or disable `local.xray` unless the user explicitly asks for that action
- keep `5.39.219.74` as the current live desktop tunnel
- keep `194.182.174.240` as a reserve and comparison path only
- do not switch daily traffic to `108.61.171.121`
- keep the Vultr scripts and configs as lab material only
- keep `uHost` as a paused fallback idea only
- keep `Hostkey 5.39.219.74` as the current server-side `Xray` reference host and `26.2.6` as the reference version
- keep MikroTik `v6` `ether1` pinned to `100Mbps full-duplex` with `auto-negotiation=disabled` until the provider side is repaired or a clean auto-negotiated full-duplex link is proven
- keep the local desktop split-routing policy in place:
  private and special-use IP ranges go `direct`
  `localhost`, `.local`, and `.home.arpa` go `direct`
  conservative RU-oriented TLDs `.ru`, `.su`, `.рф`, `.дети`, `.москва`, and `.рус` go `direct`
  all other destinations stay on the active proxy path

## Recommended First Try

- Provider: Exoscale
- Zone: `at-vie-1`
- Fallback zones: `hr-zag-1`, `de-fra-1`

## Alternate Try

- Provider: Hetzner Cloud
- Location: `hel1`
- Fallback locations: `nbg1`, `fsn1`

## AWS Lightsail Try

- Provider: AWS Lightsail
- Region: `ap-northeast-2` (`Seoul`)
- Fallback regions: `ap-south-1` (`Mumbai`), `ap-southeast-1` (`Singapore`)
- Recommended image: `Debian 12`
- Recommended first bundle: `micro_3_0`

## Before you start

1. Create a Hetzner Cloud project and API token in the console.
2. Make sure your local public SSH key exists.
3. Keep the old VPS reachable over SSH during the move.
4. For AWS Lightsail automation, create an IAM admin user and AWS access keys first.

## Create the new server on Vultr

Status: keep for reference only. The current Vultr candidate `108.61.171.121` is not approved for production use.

1. In the Vultr dashboard, create a Personal Access Token.
2. Export it:

```bash
export VULTR_API_KEY='...'
```

3. Create the server:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/vultr-create-server.sh
```

Useful overrides:

```bash
export VULTR_REGION='fra'
export VULTR_PLAN='vc2-1c-1gb'
export VULTR_SSH_KEY_PATH="$HOME/.ssh/sergei-macbook-vps.pub"
```

## Create the new server on Hetzner

```bash
export HCLOUD_TOKEN='...'
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/hetzner-create-server.sh
```

Useful overrides:

```bash
export HCLOUD_LOCATION='hel1'
export HCLOUD_SERVER_TYPE='cx22'
export HCLOUD_SSH_KEY_PATH="$HOME/.ssh/sergei-macbook-vps.pub"
```

Each create script prints the new IPv4 address when the server is ready.

## Create the new server on Exoscale

```bash
export EXOSCALE_API_KEY='...'
export EXOSCALE_API_SECRET='...'
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/exoscale-create-server.sh
```

Useful overrides:

```bash
export EXOSCALE_ZONE='at-vie-1'
export EXOSCALE_PREFERRED_SIZES='tiny,small,micro'
export EXOSCALE_SSH_KEY_PATH="$HOME/.ssh/sergei-macbook-vps.pub"
export EXOSCALE_DRY_RUN=1
```

The script:

- validates the target zone
- accepts asynchronous Exoscale create responses
- imports the SSH public key into Exoscale if needed
- ensures ingress rules for `22/tcp` and `443/tcp`
- picks a small standard instance automatically
- picks Debian 12 first, with Ubuntu 24.04 as fallback
- creates the instance and waits for the public IPv4 address

## Create the new server on AWS Lightsail

This path uses the official `aws` CLI and does not touch the live desktop tunnel.

```bash
export AWS_REGION='ap-northeast-2'
export AWS_LIGHTSAIL_SSH_KEY_PATH="$HOME/.ssh/id_rsa.pub"
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/aws-lightsail-create-server.sh
```

Useful overrides:

```bash
export AWS_LIGHTSAIL_INSTANCE_NAME='xray-aws-seoul'
export AWS_LIGHTSAIL_AVAILABILITY_ZONE='ap-northeast-2a'
export AWS_LIGHTSAIL_PREFERRED_BUNDLES='micro_3_0,small_3_0,nano_3_0'
export AWS_LIGHTSAIL_ATTACH_STATIC_IP=1
export AWS_LIGHTSAIL_DRY_RUN=1
```

The script:

- validates that the AWS CLI is authenticated
- picks an active Lightsail availability zone in the selected region
- prefers `Debian 12`, with `Ubuntu 24.04` as fallback
- prefers a small Linux bundle in the `1-2 GB RAM` range
- imports an `ssh-rsa` public key into Lightsail if needed
- creates the instance
- opens `22/tcp` and `443/tcp`
- allocates and attaches a static IP by default
- prints the SSH command and migration follow-up

For Lightsail Debian, the default SSH user is `admin`.

## Switch To Exoscale

The Exoscale client config lives locally in:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/secrets/PROXY/xray/config.exoscale.json
```

Run a safe canary only:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/switch-to-exoscale.sh
```

That command:

- starts a temporary Xray canary on `19080/19081`
- verifies that the exit IP is the Exoscale VPS
- checks HTTP, HTTPS, and SOCKS
- leaves the live `local.xray` tunnel untouched

To perform the live cutover with automatic rollback back to `5.39.219.74` on failure:

```bash
ALLOW_LIVE_SWITCH=1 /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/switch-to-exoscale.sh
```

Safety guards:

- live cutover is only allowed if `config.json` still points to baseline `5.39.219.74`
- the script confirms the current live exit IP before switching
- if post-restart checks fail, it calls `rollback-to-baseline.sh`

## Switch To AWS

The AWS client config should live locally in:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/secrets/PROXY/xray/config.aws.json
```

Run a safe canary only:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/switch-to-aws.sh
```

To perform the live cutover with automatic rollback back to `5.39.219.74` on failure:

```bash
ALLOW_LIVE_SWITCH=1 /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/switch-to-aws.sh
```

## Current Local Routing Split

The local desktop client configs currently share the same direct-bypass policy:

- RFC1918 and adjacent local/special ranges go `direct`, including `127.0.0.0/8`, `10.0.0.0/8`, `100.64.0.0/10`, `169.254.0.0/16`, `172.16.0.0/12`, `192.168.0.0/16`, `198.18.0.0/15`, `224.0.0.0/4`, `255.255.255.255/32`, `::1/128`, `fc00::/7`, `fe80::/10`, and `ff00::/8`
- `localhost`, `.local`, and `.home.arpa` go `direct`
- conservative RU-oriented TLDs `.ru`, `.su`, `.рф`, `.дети`, `.москва`, and `.рус` go `direct`
- `geoip:ru` also stays `direct`
- everything else continues through the active proxy outbound

Verified locally on `2026-03-22` through `access.log`:

- `www.google.com:443` matched `[proxy]`
- `yandex.ru:443` matched `[direct]`
- `localhost:18787` matched `[direct]`
- `192.168.0.107:18787` matched `[direct]`

## Diagnose Local Proxy

Run diagnostics through the live HTTP proxy:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/diagnose-proxy.sh
```

## Diagnose Provider Access Island

Use this when you want to inspect what still works directly through the current ISP path without disabling the live tunnel.

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/diagnose-provider-access-island.sh
```

Optional interface override:

```bash
TEST_INTERFACE=en0 /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/diagnose-provider-access-island.sh
```

This script:

- keeps `local.xray` untouched
- shows the active direct route, local IP, and gateway
- lists the active DNS resolvers
- probes common gateway ports `53/80/443`
- checks common captive-portal or walled-garden endpoints
- runs short direct DNS checks if `dig` is available
- shows the first few traceroute hops to public targets and current VPS ingress IPs
- confirms whether basic direct HTTPS still works outside the proxy

## Run The DPI Lab

Use this when you want to see exactly where the first divergence appears for the same target:

- `DNS`
- `TCP 443`
- `TLS handshake`
- `HTTP response`
- optional replay from the reference VPS `5.39.219.74`

```bash
bash /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/dpi-lab.sh
```

Useful overrides:

```bash
TEST_INTERFACE=en0 bash /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/dpi-lab.sh
COMPARE_REFERENCE=0 bash /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/dpi-lab.sh
REFERENCE_HOST=debian@194.182.174.240 REFERENCE_SSH_KEY="$HOME/.ssh/sergei-macbook-vps" bash /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/dpi-lab.sh
```

That script:

- does not stop, restart, unload, or reconfigure `local.xray`
- reports the current live tunnel state first
- resolves the same hostnames with `dig`
- tests raw TCP reachability to the resolved IPs
- tests TLS handshake separately from HTTP
- tests real HTTPS requests with timing breakdowns
- optionally reruns the same layers from the reference VPS for comparison

See [docs/dpi-lab.md](../docs/dpi-lab.md) for the meaning of each layer and useful protocol references.

Run diagnostics through a different HTTP port:

```bash
HTTP_PORT=19081 /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/diagnose-proxy.sh
```

Run diagnostics through SOCKS:

```bash
MODE=socks SOCKS_PORT=1080 /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/diagnose-proxy.sh
```

## Diagnose All VPS And Canary Tunnels

Run a combined matrix check for the current providers without touching the live tunnel:

```bash
XRAY_FLEET_SSH_KEY="$HOME/.ssh/sergei-macbook-vps" /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/diagnose-vps-and-tunnels.sh
```

That script:

- reports the current live tunnel exit IP
- checks direct `:443` reachability to each provider IP from the laptop
- audits `xray.service` and version on reachable VPS nodes
- starts temporary canary tunnels for each provider config
- measures `google`, `github`, `cloudflare`, `youtube`, and a `5 MB` download through each canary

## Diagnose Direct Provider Reachability Without Touching Xray

Run direct no-proxy tests while the live tunnel stays up:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/diagnose-direct-provider-path.sh
```

That script:

- does not stop, restart, unload, or reconfigure `local.xray`
- reports the current live proxy listener state first
- checks direct raw TCP reachability to the current provider ingress IPs
- runs direct `curl --noproxy '*'` checks to provider sites and common control sites

Optional interface pinning:

```bash
TEST_INTERFACE=en0 /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/diagnose-direct-provider-path.sh
```

That binds the direct tests to the chosen interface while still leaving the active tunnel alone.

## Compare Two Tele2 Domain Snapshots

If you already have two historical reports, compare them like this:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/compare-tele2-domain-matrix.sh \
  /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/docs/tele2-domain-matrix-2026-03-29.md \
  /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/docs/tele2-domain-matrix-2026-03-29-091200.md
```

If you pass no arguments, the script automatically compares the latest two timestamped reports under `docs/`.

## Probe A Secondary Uplink

If the laptop keeps its main internet on one interface and Tele2 appears on a second interface such as `en0` Wi-Fi hotspot or USB tethering, you can test that path without touching the live tunnel:

```bash
TEST_INTERFACE=en0 /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/diagnose-secondary-uplink.sh
```

That script:

- keeps the current `local.xray` tunnel untouched
- runs raw direct tests bound to the chosen interface
- starts a temporary Xray canary on `49080/49081`
- binds the canary proxy outbound to the chosen interface with Xray `sockopt.interface`
- runs the usual HTTP and SOCKS diagnostics through that temporary canary

Useful overrides:

```bash
TEST_INTERFACE=en0 XRAY_SOURCE_CONFIG=/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/secrets/PROXY/xray/config.working_baseline.json /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/diagnose-secondary-uplink.sh
TEST_INTERFACE=en0 XRAY_SOURCE_CONFIG=/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/secrets/PROXY/xray/config.exoscale.json /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/diagnose-secondary-uplink.sh
TEST_INTERFACE=en7 HTTP_PORT=59081 SOCKS_PORT=59080 /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/diagnose-secondary-uplink.sh
```

If the main internet is already on Wi-Fi, use USB tethering or a second network adapter for Tele2. One Wi-Fi radio cannot stay on two different Wi-Fi networks at the same time.

## Stop The Tunnel Completely

To stop the local tunnel and turn off macOS Wi-Fi proxy settings:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/stop-tunnel-and-disable-wifi-proxy.sh
```

The script:

- auto-detects the current Wi-Fi network service, with `NETWORK_SERVICE` override support
- saves the current Wi-Fi proxy settings into `secrets/PROXY/xray/backups/`
- disables Wi-Fi web, secure web, SOCKS, and auto-proxy states
- stops `local.xray` with `launchctl bootout`

It is meant as a full local stop, so after using it the launch agent is unloaded.
To start it again later:

```bash
launchctl bootstrap gui/$(id -u) "$HOME/Library/LaunchAgents/xray.plist"
launchctl kickstart -k gui/$(id -u)/local.xray
```

## About uHost

There is no self-service `uHost` automation script in this repo yet.

Reason:

- the public `uHost` site currently looks like a managed or quote-led cloud provider, not a standard instant-create VPS console
- we should wait for a confirmed `uHost` offering before automating anything

See `docs/uhost-vps-onboarding.md` for the first-step checklist.

## Migrate the current Xray setup

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/migrate-xray-from-old-vps.sh debian@NEW_IP
```

If the new host needs a dedicated SSH identity:

```bash
NEW_HOST_SSH_KEY=/absolute/path/to/key /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/migrate-xray-from-old-vps.sh debian@NEW_IP
```

If your source host alias changes:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/migrate-xray-from-old-vps.sh debian@NEW_IP old-host-or-ip
```

## Keep Xray Versions Aligned Across Servers

Audit the current fleet:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/audit-xray-fleet.sh
```

If the hosts require an explicit private key:

```bash
XRAY_FLEET_SSH_KEY="$HOME/.ssh/sergei-macbook-vps" /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/audit-xray-fleet.sh 5.39.219.74 debian@194.182.174.240
```

Sync the exact `Xray` install from the reference host `5.39.219.74` to one or more targets:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/sync-xray-from-reference.sh debian@194.182.174.240 admin@203.0.113.10
```

The migration path now verifies that the target host reports the same `Xray` version as the reference host after the copy.

## Canonical Xray Configs In Secrets

The current project now has two canonical Xray config roles in `secrets`:

- server-side reference config:
  `/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/secrets/PROXY/xray/server.reference.json`
- client-side split-routing template:
  `/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/secrets/PROXY/xray/client.split.template.json`

Current meaning:

- the server-side config is the same on `Hostkey` and `Exoscale`
- the client-side split-routing policy is also the same for both providers
- for the client config, the only current per-provider change is the remote server IP address

Render a provider-specific client config from the canonical template:

```bash
SERVER_ADDRESS=5.39.219.74 /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/render-client-xray-from-template.sh /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/secrets/PROXY/xray/config.working_baseline.json
```

```bash
SERVER_ADDRESS=194.182.174.240 /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/render-client-xray-from-template.sh /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/secrets/PROXY/xray/config.exoscale.json
```

Push the canonical server-side config to VPS nodes:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/push-server-xray-reference.sh 5.39.219.74 debian@194.182.174.240
```

## After migration

1. Generate or update the `vless://` link so it uses the new server IP.
2. Test from both home internet and mobile internet.
3. Leave the old server running for a short overlap window.
4. Shut down the old VPS only after the new route is stable.

## Roll Back To The Reference Server

If experiments with a new VPS stop working, restore the local client back to the known-good baseline:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/rollback-to-baseline.sh
```

By default, the script restarts `local.xray` through `launchctl` after restoring the baseline config.

If you use a different service label, override it explicitly:

```bash
LAUNCHCTL_LABEL=some.other.label /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/rollback-to-baseline.sh
```

The script creates a timestamped backup of the current `secrets/PROXY/xray/config.json`, restores the baseline file, validates it with `xray run -test`, restarts `local.xray`, and verifies that `1080/8080` are healthy again.

## Switch To The New VPS

When you want to make the new VPS the active local target for `local.xray`, run:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/switch-to-new-vps.sh
```

This script:

- starts a temporary canary Xray instance on isolated local ports
- validates HTTP and HTTPS through that canary before touching `local.xray`
- stops after canary by default, without changing the live config
- copies the validated local profile for `108.61.171.121` into `secrets/PROXY/xray/config.json`
- preserves a timestamped backup of the previous active config
- validates the switched config with `xray run -test`
- restarts `local.xray`
- reruns health checks on the live ports `1080/8080`
- restores the previous config automatically if the live checks fail

To allow a real live cutover anyway, you must opt in explicitly:

```bash
ALLOW_EXPERIMENTAL_SWITCH=1 /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/switch-to-new-vps.sh
```

If the result is bad, revert in one command:

```bash
LAUNCHCTL_LABEL=local.xray /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/rollback-to-baseline.sh
```

## Notes

- The migration now copies the working `xray` binary and `geoip/geosite` files from the old VPS instead of relying on the upstream installer.
- The migration keeps the current Xray config as-is, including your existing Reality/VLESS settings.
- Because the server IP changes, the share link still needs to be updated on the client side.
- If `hel1` still routes badly from your home ISP, try `nbg1` next before changing the whole provider again.

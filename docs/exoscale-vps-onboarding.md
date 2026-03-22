# Exoscale VPS Onboarding Notes

This note starts the `Exoscale` provider track for the project.

Date of record: `2026-03-22`.

## Current Status

- active live desktop tunnel is now on `Exoscale 194.182.174.240`
- `Hostkey 5.39.219.74` remains the rollback baseline
- `Vultr 108.61.171.121` is already decommissioned on the provider side
- `uHost` stays documented but is currently deprioritized because of exit-risk concerns
- `Exoscale` is now the primary next provider under evaluation

## Account Readiness

Validated on `2026-03-22` without creating resources:

- Exoscale IAM API access works
- available zones include `at-vie-1`, `hr-zag-1`, `de-fra-1`, `de-muc-1`, `bg-sof-1`, and Swiss zones
- the Exoscale account currently has no instances
- the Exoscale account currently has no imported SSH keys

Live provisioning status on `2026-03-22`:

- first Exoscale instance provisioning in `at-vie-1` succeeded
- the VM reached `running` state and accepted SSH with the imported VPS key
- Xray binary, data files, config, and systemd unit were copied from `Hostkey 5.39.219.74`
- `xray.service` is active on the Exoscale VM and listens on `:443`
- local canary client checks succeeded through the Exoscale VM on temporary ports without touching `local.xray`

## Current Practical State

- `local.xray` live cutover to Exoscale succeeded on `2026-03-22`
- active local exit IP is `194.182.174.240`
- `Hostkey 5.39.219.74` remains intact and is still the one-command rollback target
- Exoscale is now both a working server-side candidate and a working live desktop path

## Live Diagnostics Summary

HTTP proxy diagnostics after live cutover:

- `google`, `apple`, `github`, `cloudflare`, `wikipedia`, and `youtube` all returned successful HTTP responses
- `yandex.ru` and `vk.com` also responded through the current routing profile
- latency to several popular services improved versus the earlier Hostkey baseline snapshot
- bulk throughput on the `speed.cloudflare.com` 5 MB test was lower than the earlier Hostkey baseline snapshot

Observed comparison from the same local laptop on `2026-03-22`:

- baseline `Hostkey` 5 MB test: about `416 KB/s`
- live `Exoscale` 5 MB test over HTTP proxy: about `138 KB/s`
- live `Exoscale` 5 MB test over SOCKS: about `208 KB/s`

Practical conclusion:

- browsing-style connectivity currently looks healthy on Exoscale
- raw download throughput looks weaker than the old Hostkey baseline in this specific curl test
- Exoscale is usable now, but it should continue to be watched during real browser use

## Recommended First Zones

Priority order for the first Xray test:

1. `at-vie-1`
2. `hr-zag-1`
3. `de-fra-1`

Why this order:

- all three keep us in central Europe
- they diversify away from the current `Hostkey` path
- they are good first candidates before trying more exotic or more distant routes

## Recommended First Instance Shape

Recommended first try:

- family: `standard`
- size: `tiny`
- vCPU: `1`
- RAM: `1 GB`
- disk: `10 GB`
- image: `Linux Debian 12 (Bookworm) 64-bit`

Practical note:

- `micro` is available but only has `512 MB`, which is too tight for comfortable troubleshooting
- `small` with `2 GB` is the safer fallback if `tiny` feels constrained

## Local SSH Key Choice

Recommended local public key for the first Exoscale import:

- `/Users/sergeibystrov/.ssh/sergei-macbook-vps.pub`

Reason:

- it is clearly named for VPS access
- it avoids reusing older generic workstation keys by default

## Create Script

Use the local helper script:

```bash
export EXOSCALE_API_KEY='...'
export EXOSCALE_API_SECRET='...'
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/exoscale-create-server.sh
```

Useful overrides:

```bash
export EXOSCALE_ZONE='at-vie-1'
export EXOSCALE_PREFERRED_SIZES='tiny,small,micro'
export EXOSCALE_SSH_KEY_PATH="$HOME/.ssh/sergei-macbook-vps.pub"
export EXOSCALE_DRY_RUN=1
```

Operational notes:

- the create script accepts asynchronous Exoscale `pending` responses
- it imports the SSH public key into Exoscale if needed
- it ensures ingress rules for `22/tcp` and `443/tcp`

Migration notes:

- if the new host requires a non-default SSH identity, use `NEW_HOST_SSH_KEY=/absolute/path/to/key`
- the migration script now copies the working Xray binary and data files from the old VPS instead of relying on the upstream installer

## Live Cutover Path

Local Exoscale client config:

- `/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/secrets/PROXY/xray/config.exoscale.json`

Safe canary only:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/switch-to-exoscale.sh
```

Real live cutover with automatic rollback to `5.39.219.74` if checks fail:

```bash
ALLOW_LIVE_SWITCH=1 /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/switch-to-exoscale.sh
```

Current state:

- this live cutover path has already been executed successfully once on `2026-03-22`

Manual rollback:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/rollback-to-baseline.sh
```

## Project Rule

Do not switch daily traffic to a new Exoscale VM until all of this is true:

- SSH access works reliably
- Xray is migrated successfully
- browser traffic works through the tunnel
- rollback to `5.39.219.74` remains one command away

## Official Sources

- Exoscale datacenters: <https://www.exoscale.com/datacenters/>
- Exoscale pricing: <https://www.exoscale.com/pricing/>
- Exoscale create instance API: <https://openapi-v2.exoscale.com/operation/operation-create-instance>
- Exoscale import SSH key API: <https://openapi-v2.exoscale.com/operation/operation-register-ssh-key>

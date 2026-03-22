# VPS Move Helpers

This folder contains helper scripts for moving the current self-hosted Xray/VLESS setup between providers and for recovering the laptop tunnel when experiments fail.

See also: `docs/vps-audit-2026-03-22.md` for the current local audit of the old baseline server and the newer candidate VPS.

## Provider Status

- production baseline: `Hostkey 5.39.219.74`
- failed experimental provider: `Vultr 108.61.171.121`
- next research track: `uHost`

## What is here

- `hetzner-create-server.sh`: creates a small Hetzner Cloud server through the official API
- `vultr-create-server.sh`: creates a small Vultr Cloud Compute instance through the official API
- `migrate-xray-from-old-vps.sh`: installs Xray on the new host and copies the current config and systemd files from the old host
- `switch-to-new-vps.sh`: runs a canary against `108.61.171.121` and only performs a live cutover if you opt in explicitly
- `rollback-to-baseline.sh`: restores the local Xray client config back to the working reference server `5.39.219.74`

## Current Position

- keep `5.39.219.74` as the only active production server
- do not switch daily traffic to `108.61.171.121`
- keep the Vultr scripts and configs as lab material only
- start any next migration attempt with `uHost` research first

## Alternate Try

- Provider: Hetzner Cloud
- Location: `hel1`
- Fallback locations: `nbg1`, `fsn1`

## Before you start

1. Create a Hetzner Cloud project and API token in the console.
2. Make sure your local public SSH key exists.
3. Keep the old VPS reachable over SSH during the move.

## Create the new server on Vultr

Status: keep for reference only. The current Vultr candidate `108.61.171.121` is not approved for production use.

1. In the Vultr dashboard, create a Personal Access Token.
2. Export it:

```bash
export VULTR_API_KEY='...'
```

3. Create the server:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/vultr-create-server.sh
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
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/hetzner-create-server.sh
```

Useful overrides:

```bash
export HCLOUD_LOCATION='hel1'
export HCLOUD_SERVER_TYPE='cx22'
export HCLOUD_SSH_KEY_PATH="$HOME/.ssh/sergei-macbook-vps.pub"
```

Each create script prints the new IPv4 address when the server is ready.

## About uHost

There is no self-service `uHost` automation script in this repo yet.

Reason:

- the public `uHost` site currently looks like a managed or quote-led cloud provider, not a standard instant-create VPS console
- we should wait for a confirmed `uHost` offering before automating anything

See `docs/uhost-vps-onboarding.md` for the first-step checklist.

## Migrate the current Xray setup

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/migrate-xray-from-old-vps.sh root@NEW_IP
```

If your source host alias changes:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/migrate-xray-from-old-vps.sh root@NEW_IP old-host-or-ip
```

## After migration

1. Generate or update the `vless://` link so it uses the new server IP.
2. Test from both home internet and mobile internet.
3. Leave the old server running for a short overlap window.
4. Shut down the old VPS only after the new route is stable.

## Roll Back To The Reference Server

If experiments with a new VPS stop working, restore the local client back to the known-good baseline:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/rollback-to-baseline.sh
```

By default, the script restarts `local.xray` through `launchctl` after restoring the baseline config.

If you use a different service label, override it explicitly:

```bash
LAUNCHCTL_LABEL=some.other.label /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/rollback-to-baseline.sh
```

The script creates a timestamped backup of the current `secrets/PROXY/xray/config.json`, restores the baseline file, validates it with `xray run -test`, restarts `local.xray`, and verifies that `1080/8080` are healthy again.

## Switch To The New VPS

When you want to make the new VPS the active local target for `local.xray`, run:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/switch-to-new-vps.sh
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
ALLOW_EXPERIMENTAL_SWITCH=1 /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/switch-to-new-vps.sh
```

If the result is bad, revert in one command:

```bash
LAUNCHCTL_LABEL=local.xray /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/rollback-to-baseline.sh
```

## Notes

- The migration keeps the current Xray config as-is, including your existing Reality/VLESS settings.
- Because the server IP changes, the share link still needs to be updated on the client side.
- If `hel1` still routes badly from your home ISP, try `nbg1` next before changing the whole provider again.

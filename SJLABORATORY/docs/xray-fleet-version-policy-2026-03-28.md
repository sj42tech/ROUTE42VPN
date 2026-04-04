# Xray Fleet Version Policy 2026-03-28

This note defines how the project should keep the same `Xray` build across all VPS nodes.

## Current Reference Version

Reference host:

- `Hostkey 5.39.219.74`

Reference version on `2026-03-28`:

- `Xray 26.2.6 (Xray, Penetrates Everything.) 12ee51e (go1.25.7 linux/amd64)`

Current audit snapshot on `2026-03-28`:

- `Hostkey 5.39.219.74`: `active`, listening on `:443`, version `26.2.6`
- `Exoscale 194.182.174.240`: `active`, listening on `:443`, version `26.2.6`

## Policy

- Treat `Hostkey 5.39.219.74` as the current reference host for the server-side `Xray` binary, data files, and systemd layout.
- Do not install random upstream `Xray` builds independently on each VPS.
- New providers should receive the exact working `Xray` binary and service layout copied from the reference host.
- Version drift should be checked explicitly before any client cutover.

## Why This Policy Exists

- It removes one variable from provider experiments.
- If a new VPS fails, we can blame routing, filtering, or provider behavior sooner instead of wondering whether `Xray` versions differ.
- It gives us a simple staged-upgrade path for the whole fleet.

## New Server Flow

1. Create the new VPS.
2. Verify SSH access.
3. Copy the working `Xray` install from the reference host:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/migrate-xray-from-old-vps.sh root@203.0.113.10
```

If the new provider uses a non-root login user, pass that user instead:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/migrate-xray-from-old-vps.sh admin@203.0.113.10
```

4. Audit the fleet versions:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/audit-xray-fleet.sh
```

5. Only then prepare client-side canary configs and routing tests.

## Fleet Sync Flow

To re-apply the reference `Xray` build to multiple hosts:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/sync-xray-from-reference.sh debian@194.182.174.240 admin@198.51.100.10
```

That wrapper uses:

- reference host: `5.39.219.74` by default
- the existing migration script for the actual copy and validation

## Audit Flow

To inspect server-side `Xray` versions and service state:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/audit-xray-fleet.sh
```

You can also pass custom hosts:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/audit-xray-fleet.sh 5.39.219.74 debian@194.182.174.240 admin@198.51.100.10
```

If the fleet nodes need an explicit SSH identity:

```bash
XRAY_FLEET_SSH_KEY="$HOME/.ssh/sergei-macbook-vps" /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/audit-xray-fleet.sh 5.39.219.74 debian@194.182.174.240
```

## Upgrade Strategy

The simplest safe strategy for this project is:

1. Choose one host as the reference candidate.
2. Upgrade and verify there first.
3. Run direct provider checks and canary tests.
4. Sync the same validated `Xray` build to the rest of the fleet.
5. Audit versions again after the rollout.

This project should prefer one known-good `Xray` version across servers over independent unmanaged updates.

# SJLABORATORY

`SJLABORATORY` is the private infrastructure and network lab for the `sj42tech` workspace.

This layer holds the operational material that should stay separate from the public `Route42` app:

- VPS onboarding and comparisons
- Xray and Reality templates
- migration and rollback scripts
- local network and router diagnostics
- provider-path research
- operator and hotspot lab notes

Route42 app signing material does not live here anymore. Keep Android keystore and app-release helper files in `../ROUTE42/secrets/`.

## Main Areas

- [docs](docs)
- [ops/README.md](ops/README.md)
- `secrets/`
- `logs/`

## Important Safety Rule

- Do not stop, restart, unload, or disable `local.xray` while a working desktop tunnel is active unless there is an explicit instruction to do that.

## Useful Starting Docs

- [docs/vps-provider-price-tracker-2026-03-28.md](docs/vps-provider-price-tracker-2026-03-28.md)
- [docs/hostkey-like-vps-shortlist-2026-04-04.md](docs/hostkey-like-vps-shortlist-2026-04-04.md)
- [docs/provider-direct-reachability-2026-03-28.md](docs/provider-direct-reachability-2026-03-28.md)
- [docs/vps-audit-2026-03-22.md](docs/vps-audit-2026-03-22.md)
- [docs/mikrotik-v6-wan-duplex-fix-2026-04-03.md](docs/mikrotik-v6-wan-duplex-fix-2026-04-03.md)
- [docs/tele2-domain-matrix-latest.md](docs/tele2-domain-matrix-latest.md)

## Useful Operational Entry Points

- [ops/README.md](ops/README.md)
- [ops/diagnose-proxy.sh](ops/diagnose-proxy.sh)
- [ops/diagnose-vps-and-tunnels.sh](ops/diagnose-vps-and-tunnels.sh)
- [ops/diagnose-direct-provider-path.sh](ops/diagnose-direct-provider-path.sh)
- [ops/push-server-xray-reference.sh](ops/push-server-xray-reference.sh)
- [ops/rollback-to-baseline.sh](ops/rollback-to-baseline.sh)

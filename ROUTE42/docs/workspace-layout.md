# Workspace Index

This workspace is now split into two project layers.

## Route42

Public Android app project:

- [../README.md](../README.md)
- [ROUTE42 docs](.)

Build from the repository root:

```bash
cd ROUTE42
./gradlew testDebugUnitTest assembleDebug assembleRelease
```

## ignored-local-storage

Private lab and infrastructure layer for:

- VPS research
- Xray and Reality configs
- routing experiments
- local network diagnostics
- provider comparisons

Entry points:

- [../../ignored-local-storage/README.md](../../ignored-local-storage/README.md)
- [../../ignored-local-storage/docs](../../ignored-local-storage/docs)
- [../../ignored-local-storage/ops/README.md](../../ignored-local-storage/ops/README.md)

`ROUTE42/secrets/` is the local ignored area for app signing material and release helpers.

`ignored-local-storage/secrets/` is the local ignored area for VPS, Xray, network, and provider credentials.

Live Route42 VPS tuples, reusable smoke-test links, and generated local share-code images should stay under ignored `../ignored-local-storage/secrets/ROUTE42/`.

## GitHub Release Flow

- The signed Android release workflow lives at [../../.github/workflows/release-apk.yml](../../.github/workflows/release-apk.yml).
- The workflow builds the app from `ROUTE42/`.

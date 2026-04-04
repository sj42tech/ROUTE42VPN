# Workspace Index

This workspace is now split into two project layers.

## Route42

Public Android app project:

- [ROUTE42/README.md](ROUTE42/README.md)
- [ROUTE42/docs](ROUTE42/docs)

Build from the app root:

```bash
cd ROUTE42
./gradlew testDebugUnitTest assembleDebug assembleRelease
```

## SJLABORATORY

Private lab and infrastructure layer for:

- VPS research
- Xray and Reality configs
- routing experiments
- local network diagnostics
- provider comparisons

Entry points:

- [SJLABORATORY/README.md](SJLABORATORY/README.md)
- [SJLABORATORY/docs](SJLABORATORY/docs)
- [SJLABORATORY/ops/README.md](SJLABORATORY/ops/README.md)

`ROUTE42/secrets/` is the local ignored area for app signing material and release helpers.

`SJLABORATORY/secrets/` is the local ignored area for VPS, Xray, network, and provider credentials.

## GitHub Release Flow

- The signed Android release workflow lives at [.github/workflows/release-apk.yml](.github/workflows/release-apk.yml).
- The workflow builds the app from `ROUTE42/`.

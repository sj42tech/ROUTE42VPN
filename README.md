# Workspace Index

This repository is organized into two main layers.

## Android App

- [ROUTE42/README.md](ROUTE42/README.md)
- [ROUTE42/docs](ROUTE42/docs)
- Build from the app root:

```bash
cd ROUTE42
./gradlew testDebugUnitTest assembleDebug assembleRelease
```

## Infrastructure And Operations

- [docs](docs)
- [ops/README.md](ops/README.md)
- `secrets/` is the local maintainer area for ignored credentials and helper material.

## GitHub Release Flow

- The signed Android release workflow lives at [.github/workflows/release-apk.yml](.github/workflows/release-apk.yml).
- The workflow now builds from `ROUTE42/`.

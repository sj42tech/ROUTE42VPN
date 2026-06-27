# Repository Layout

This repository tracks the Route42 Android app and GitHub release metadata only.

## Tracked Areas

- `.github/` contains GitHub Actions workflow metadata.
- `ROUTE42/` contains the Android app, docs, source code, tests, assets, Gradle wrapper, and app-local tools.
- `.gitignore` protects local build outputs, private material, and neighboring local workspaces.

## App Build Entry Point

Run app commands from `ROUTE42/`:

```bash
cd ROUTE42
./gradlew testDebugUnitTest assembleDebug assembleRelease
```

## Local-Only Areas

The following paths are intentionally ignored and must not be committed:

- `ROUTE42/secrets/` for local signing helper material.
- `ROUTE42/build/` and `ROUTE42/app/build/` for generated build outputs.
- `ROUTE42/.gradle/` for local Gradle state.
- Neighboring lab, infrastructure, experiment, and private-data directories at the repository root.

## GitHub Release Flow

- The signed Android release workflow lives at `.github/workflows/release-apk.yml`.
- The workflow builds the app from `ROUTE42/`.
- GitHub repository secrets provide release signing material; local keystores must not be committed.

# GitHub Release Signing Setup

This page describes how to configure signed Route42 release builds with GitHub Actions.

## Required GitHub Secrets

Add these repository secrets in GitHub:

- `ROUTE42_KEYSTORE_BASE64`
- `ROUTE42_KEYSTORE_PASSWORD`
- `ROUTE42_KEY_ALIAS`
- `ROUTE42_KEY_PASSWORD`

## Local Secrets Helper

Route42 includes a local helper directory at `secrets/`.

- Keep your real keystore and passwords there locally.
- Those files are ignored by `.gitignore`.
- Use `bash secrets/print-github-secrets.sh` to print the four GitHub secret values in the correct order for copy/paste.

## What The Workflow Does

The `Release APK` workflow:

1. validates the signing secrets;
2. decodes the keystore into a temporary file on the runner;
3. exports `ROUTE42_KEYSTORE_PATH` for Gradle;
4. runs tests and builds a signed `release` APK;
5. uploads the APK as a workflow artifact;
6. publishes the APK to GitHub Releases when the workflow runs from a tag like `v0.1.0`.

Manual workflow runs create a downloadable signed artifact without publishing a GitHub Release.

## How To Generate `ROUTE42_KEYSTORE_BASE64`

On macOS or Linux:

```bash
base64 < route42-release.keystore | tr -d '\n'
```

Copy the resulting single-line string into the `ROUTE42_KEYSTORE_BASE64` GitHub secret.

If you prefer, place the keystore or the precomputed base64 string in `secrets/` and use the helper script instead of running the command manually.

## Local Signed Release Build

You can also build a signed release locally with environment variables:

```bash
export ROUTE42_KEYSTORE_PATH="/absolute/path/to/route42-release.keystore"
export ROUTE42_KEYSTORE_PASSWORD="your-store-password"
export ROUTE42_KEY_ALIAS="your-key-alias"
export ROUTE42_KEY_PASSWORD="your-key-password"
./gradlew assembleRelease
```

If these values are not set, Gradle falls back to the normal unsigned release output.

## Expected Release Output

- Signed local build: `app/build/outputs/apk/release/app-release.apk`
- Unsigned local fallback: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Recommended Tag Flow

1. Push your changes to `main`.
2. Create a version tag such as `v0.1.0`.
3. Push the tag.
4. GitHub Actions builds a signed APK and uploads it to the matching GitHub Release.

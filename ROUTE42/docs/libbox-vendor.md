# Vendored libbox Build

Route42 vendors an arm64 Android `libbox.aar` built from the official SagerNet sing-box source repository.

## Current Artifact

- Upstream tag: `v1.13.12`
- Upstream commit: `1086ab2563320e0da0c23b3a491d8dfa0939dff4`
- Target: `android/arm64`
- Android API used by the upstream main variant: `23`
- Go: `1.26.0`
- OpenJDK: `17.0.18`
- Android NDK: `28.0.13004108`
- AAR SHA-256: `bbbd71babf37bddb21e31025a3a1cbc2d5b6feae17e7d593133502f2a4923cf7`

## Build

Check out the exact upstream tag in a clean directory, verify the resolved commit, and run the upstream builder:

```bash
git clone --depth 1 --branch v1.13.12 https://github.com/SagerNet/sing-box.git
cd sing-box
git rev-parse HEAD
export JAVA_HOME="/path/to/openjdk-17"
export ANDROID_HOME="$HOME/Library/Android/sdk"
go run ./cmd/internal/build_libbox -target android -platform android/arm64
```

The builder creates both `libbox.aar` and a legacy variant. Route42 uses the main `libbox.aar` only.

## Verification

Before replacing `app/libs/libbox.aar`:

1. Verify the upstream tag resolves to the recorded commit.
2. Verify the AAR contains only `jni/arm64-v8a/libbox.so`.
3. Verify the native library contains the `v1.13.12` and `github.com/sagernet/sing-box/build/arm64/libbox` provenance markers.
4. Verify the SHA-256 digest against this document and `THIRD_PARTY_NOTICES.md`.
5. Run Route42 unit tests, debug and release builds, instrumentation tests, and live tunnel smoke tests.

Do not download or vendor an AAR from an unverified third-party release.

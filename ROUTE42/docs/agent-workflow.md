# Agent Workflow

This document defines the default Android-agent workflow for Route42.

It is based on the Android Developers Blog post [Build Android apps 3x faster using any agent](https://android-developers.googleblog.com/2026/04/build-android-apps-3x-faster-using-any-agent.html?m=1) and the official Android agent tooling guidance at [developer.android.com/tools/agents](https://developer.android.com/tools/agents).

## Pre-Task Rule

Before Android platform, build, emulator, UI, permission, release, or SDK-related work, the agent must check this document and prefer current official Android guidance over memory.

This is especially important for:

- `VpnService`
- foreground services
- permissions
- target SDK changes
- Android Gradle Plugin changes
- R8 and shrinking
- Compose UI behavior
- edge-to-edge
- emulator setup
- release and signing workflows

Route42-specific `sing-box`, `libbox`, `Xray`, and VPS behavior still requires Route42 tests and lab verification. Android agent guidance does not replace tunnel verification.

## Android CLI

The official Android CLI should be used when available for:

- SDK setup checks
- emulator discovery and startup
- app install and run workflows
- scripted local smoke-test setup
- future CI bootstrap experiments

Verified local status on 2026-05-01:

- `android` is installed at `/usr/local/bin/android`.
- `android --version` reports `0.7.15326717`.
- `android info` resolves the SDK to `$HOME/Library/Android/sdk`.
- Known local AVD: `Medium_Phone_API_36`.
- `android emulator list` shows `Medium_Phone_API_36`.
- `android emulator start Medium_Phone_API_36` successfully starts the emulator.
- `android run --device=emulator-5554 --type=ACTIVITY --activity=io.github.sj42tech.route42.MainActivity --apks=app/build/outputs/apk/debug/app-debug.apk` successfully installs and launches the debug APK after `assembleDebug`.

Use this local bootstrap before Gradle commands unless the shell already exports `ANDROID_HOME`:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

Preferred local emulator and install flow:

```bash
android emulator list
android emulator start Medium_Phone_API_36
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew assembleDebug
android run --device=emulator-5554 --type=ACTIVITY --activity=io.github.sj42tech.route42.MainActivity --apks=app/build/outputs/apk/debug/app-debug.apk
android emulator stop emulator-5554
```

Fallback commands if Android CLI preview tooling is unavailable:

```bash
emulator -list-avds
adb devices -l
./gradlew testDebugUnitTest assembleDebug assembleRelease
./gradlew :app:connectedDebugAndroidTest
```

Current preview limitation:

- `android describe --project_dir=$PWD` is not release-ready for Route42 yet. It created an ignored `.gradle/init.gradle.kts` helper, then failed on `:app:dumpAndroidProjectModel` with `No Android project model available`.
- Do not make GitHub Actions release builds depend on Android CLI until Route42 has a green manual workflow run using that exact setup.

## Android Skills

Use official Android Skills when they are available for the task area.

Installed project-local skills:

- `skills/android-cli/SKILL.md`
- `skills/edge-to-edge/SKILL.md`
- `skills/r8-analyzer/SKILL.md`
- `skills/agp-9-upgrade/SKILL.md`
- `skills/migrate-xml-views-to-jetpack-compose/SKILL.md`
- `skills/navigation-3/SKILL.md`

Before touching any matching area, read the relevant skill first:

- use `android-cli` for Android CLI, emulator, run, layout, screen, SDK, and docs workflows;
- use `edge-to-edge` for status bar, navigation bar, IME, and target-SDK-driven UI inset work;
- use `r8-analyzer` before changing R8, ProGuard, shrinking, or keep rules;
- use `agp-9-upgrade` before Android Gradle Plugin migration work;
- use `migrate-xml-views-to-jetpack-compose` for XML-to-Compose migration tasks;
- use `navigation-3` for Navigation and Compose navigation changes.

If Android Skills are missing in a future checkout, reinstall them with:

```bash
android skills add --skill=android-cli --project="$PWD"
android skills add --skill=edge-to-edge --project="$PWD"
android skills add --skill=r8-analyzer --project="$PWD"
android skills add --skill=agp-9-upgrade --project="$PWD"
android skills add --skill=migrate-xml-views-to-jetpack-compose --project="$PWD"
android skills add --skill=navigation-3 --project="$PWD"
```

## Android Knowledge Base

Use Android Knowledge Base or current official Android documentation before touching fast-moving platform areas.

Required check areas:

- `VpnService`
- foreground service types and notifications
- permission behavior
- target SDK migration notes
- Android Gradle Plugin migration notes
- emulator and managed-device behavior
- Play and release signing requirements

Verified Knowledge Base commands:

```bash
android docs search VpnService
android docs search "foreground service"
android docs search "target SDK"
android docs fetch kb://android/develop/connectivity/vpn
```

For Route42 VPN work, start with `kb://android/develop/connectivity/vpn`, then fetch current foreground service and target SDK docs if the change touches service lifecycle, notifications, manifest service declarations, or SDK levels.

## Route42 Build Workflow

Run commands from the `ROUTE42/` directory.

Baseline local verification:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew testDebugUnitTest assembleDebug assembleRelease
```

Instrumentation verification:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:connectedDebugAndroidTest
```

Live tunnel smoke tests are opt-in and must use ignored local lab inputs. Never commit live VPS links, keys, or share-code content.

## GitHub Actions

The release workflow currently uses Gradle and GitHub signing secrets.

Android CLI may be tested in GitHub Actions only after it is stable locally. Keep it experimental until:

- SDK setup is reproducible;
- release signing still works;
- `testDebugUnitTest` and `assembleRelease` pass;
- artifacts are attached to a tagged GitHub Release;
- no live tunnel secrets are exposed.

## Practical Default

Use Android CLI when it is present and stable. Otherwise use the established Gradle, ADB, and emulator commands above.

The goal is to reduce manual SDK and emulator handling while keeping Route42 release and tunnel checks deterministic.

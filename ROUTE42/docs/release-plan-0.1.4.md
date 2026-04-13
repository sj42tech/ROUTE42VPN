# Route42 0.1.4 Release Plan

Status: draft

Date prepared: `2026-04-13`

## Goal

Freeze a clean `0.1.4` release that reflects the current working state of `Route42`:

- `Hostkey` and `Exoscale` are validated as working candidates
- strict smoke tests verify real tunnel egress
- `AWS` is excluded from the recommended profile set until repaired

## Must

1. confirm the final recommended `Hostkey` and `Exoscale` share links in the private lab flow
2. keep the strict Android smoke test as the release gate for live-profile verification
3. update release notes and operator guidance so older stale profiles are re-imported
4. exclude `AWS` from current recommended live-profile guidance
5. run before-release checks from `ROUTE42/`:
   - `./gradlew testDebugUnitTest`
   - `./gradlew assembleDebug`
   - `./gradlew assembleRelease`
   - targeted `connectedDebugAndroidTest` for the strict connect smoke test

## Should

1. add a small in-app hint or support note that older imported profiles may need re-import after server-side `Reality` changes
2. archive the exact smoke-test verdict for `Hostkey`, `Exoscale`, and `AWS` in the release discussion
3. keep the Tele2 lab path reproducible from `SJLABORATORY` without leaving temporary router forwarding rules behind

## Could

1. add an explicit bad-profile warning when a tunnel reaches `VPN CONNECTED` but does not produce a valid exit IP
2. introduce an operator-only live profile manifest format version so stale private links are easier to detect

## Proposed Release Sequence

1. finalize the two working live profiles
2. re-import and verify them on a real phone
3. bump `versionCode` and `versionName` to `0.1.4`
4. tag `v0.1.4`
5. let GitHub Actions build and sign the release APK

## Release Verdict

`0.1.4` should be treated as ready when:

- `Hostkey` and `Exoscale` pass the strict Route42 connect smoke test
- `AWS` is clearly marked as not recommended
- release notes mention the stale-profile re-import requirement

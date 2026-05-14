# Route42 0.1.6 Release Notes

Status: release candidate

Date prepared: `2026-05-14`

## Summary

`0.1.6` adds app-scope routing for compatibility-sensitive Android apps and expands the `RU + Local` routing preset with commerce helper domains observed during the DNS Shop / QRator incident.

## What Is Included

- New `Only selected apps use VPN` mode for routing profiles.
- App picker UI for selecting which launchable Android apps enter the VPN.
- sing-box `include_package` generation for selected-app VPN scope.
- Safe validation that blocks starting selected-app VPN mode when no app is selected.
- Expanded `RU + Local` direct bundle for `dns-shop.ru`, `vchecks.me`, `yandexcloud.net`, `yastatic.net`, and related Yandex helper domains.
- Documentation updates for app-scope routing and commerce-helper direct routing.

## User Impact

Users can now keep banking, marketplace, government, and other compatibility-sensitive apps on the phone's normal network path while tunneling only selected apps such as a browser, Telegram, or YouTube.

Existing routing profiles stay compatible because the default app scope remains `All apps`.

## Verification

Local release preparation checks:

- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`

The signed release APK is expected to be built by GitHub Actions from tag `v0.1.6`.

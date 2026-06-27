# Route42 0.1.8 Release Notes

`0.1.8` adds a safer app-list workflow for users who want only selected apps to enter Android VPN.

## Highlights

- Added a recommended VPN app preset for common browser, messaging, video, and social apps.
- Banks, marketplaces, delivery, and government apps are intentionally not included in the recommended preset, so they stay on the phone's normal network path unless the user manually selects them.
- Added `x-route42-app-mode` and repeatable `x-route42-app-package` import/share parameters.
- Route42 share codes can now carry `Only selected apps` routing together with the VLESS endpoint and route rules.
- Existing manual app selection remains editable per routing profile.

## Release Build

The signed release APK is expected to be built by GitHub Actions from tag `v0.1.8`.

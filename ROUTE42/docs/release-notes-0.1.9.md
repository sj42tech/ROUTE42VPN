# Route42 0.1.9 Release Notes

`0.1.9` improves Android tunnel reliability and makes Reality target rotation safe for existing profiles.

## Changes

- Updated the vendored official sing-box `libbox` build from `v1.13.3` to `v1.13.12`.
- Added safe connection refresh when an imported VLESS profile matches an existing protocol, server, port, and UUID.
- Connection refresh updates the profile name, endpoint, Reality, transport, and preserved share-link fields.
- Existing profile identity, creation time, shared routing assignment, custom rules, RU + Local preset, DNS mode, and selected-app scope remain unchanged.
- The import screen clearly shows when a scan will update an existing connection instead of creating a duplicate.
- Added a local QR rendering utility for release-download and private connection share codes.

## Upgrade Notes

Installing the APK preserves Route42 application data. Scan a newly issued connection code after upgrading when a VPS changes its Reality target or other endpoint fields. Route42 will show `Update Connection` and retain the phone's current routing configuration.

Live VPS links, UUIDs, Reality keys, and generated private QR codes are not included in the public repository.

The signed release APK is expected to be built by GitHub Actions from tag `v0.1.9`.

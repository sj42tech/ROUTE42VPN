# Third-Party Notices

Route42 includes third-party software. Each third-party component remains under its own license.

Update this file whenever a vendored binary, upstream version, or third-party dependency record changes.

## sing-box / libbox

- Component: `libbox.aar`
- Local path: `app/libs/libbox.aar`
- Upstream project: [SagerNet/sing-box](https://github.com/SagerNet/sing-box)
- Upstream license: `GPL-3.0-or-later`
- Recorded upstream version tag: `v1.1.0`

The vendored Android library contains the following provenance markers in the shipped binary:

- `github.com/sagernet/sing-box`
- `github.com/sagernet/sing-box/build/arm64/libbox`
- `v1.1.0`

These markers identify the vendored `libbox.aar` as a build derived from the upstream `SagerNet/sing-box` project.

Upstream Android `libbox` builds are produced from the `sing-box` repository. Upstream build references include `go run ./cmd/internal/build_libbox -target android`.

Route42 ships this binary as a third-party dependency. Route42 is an independent client project and is not an official sing-box client.

## License Summary

- Route42: `GPL-3.0-or-later` as declared in [NOTICE.md](NOTICE.md)
- sing-box / libbox: `GPL-3.0-or-later`

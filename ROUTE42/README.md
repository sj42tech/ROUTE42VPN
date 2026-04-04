# Route42

Route42 is an Android VLESS client for self-hosted VPN and sing-box-compatible profiles.

It lets you import `vless://` links, connect through Android `VpnService`, and manage reusable routing profiles for direct, proxy, and split traffic. Route42 is designed for users who run their own VPS or already have sing-box-compatible access profiles and want a simple Android client with configurable routing behavior.

## Contact And VPS Setup

For project questions, collaboration, or custom VPS setup help, contact `sj42tech@proton.me`.

Route42 is an independent client project and is not an official sing-box client.

## License

Route42 is distributed under `GPL-3.0-or-later`. Third-party components remain under their own licenses.

See [NOTICE.md](NOTICE.md), [LICENSE](../LICENSE), and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for the current licensing and third-party component records.

## Before You Start

Route42 is a client app only. It does not include a VPN server, a VPS, or bundled access.

To use this app, you need one of the following:

- your own VPS with `Xray-core` configured for `VLESS` or another compatible setup;
- a `vless://` share URL from a third-party provider or community that gives you access to its VPS service.

Public or free share URLs can work, but they are often unstable, short-lived, or unsafe. Use trusted sources only. Route42 does not verify or endorse third-party public services.

Useful starting points:

- [Project X / Xray-core](https://xtls.github.io/en/)
- [Xray Quick Start](https://xtls.github.io/en/document/)
- [Xray Download and Install](https://xtls.github.io/en/document/install.html)
- [Xray Configuration Guide](https://xtls.github.io/en/config/)
- [VLESS Protocol](https://xtls.github.io/en/development/protocols/vless.html)
- [XTLS/Xray-install](https://github.com/XTLS/Xray-install)
- [XTLS/Xray-examples](https://github.com/XTLS/Xray-examples)
- [sing-box Documentation](https://sing-box.sagernet.org/configuration/)

Public app name: `Route42`.

Repository name: `ROUTE42VPN`.

GitHub: [sj42tech/ROUTE42VPN](https://github.com/sj42tech/ROUTE42VPN)

## Required Project Rules

All changes in code, docs, assets, and project config must follow [PROJECT_RULES.md](PROJECT_RULES.md).

This applies to:

- directory and file structure;
- package, class, and document naming;
- documentation examples;
- local and generated files;
- anonymization rules and the ban on personal data in the repo.

## Live Tunnel Safety

- Do not stop, restart, unload, or disable `local.xray` while a working desktop tunnel is active unless the user gives an explicit instruction to do that.
- Do not disable macOS system proxy settings while the live desktop tunnel is considered working unless the user explicitly requests that action.
- Provider and path diagnostics should be run as direct no-proxy checks that bypass the current tunnel while leaving the active `xray` session untouched.
- For direct provider checks from the laptop without touching the live tunnel, use [diagnose-direct-provider-path.sh](/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/diagnose-direct-provider-path.sh).

## Project At A Glance

- imports standard `vless://` links;
- strictly validates `Reality` transport links before save;
- supports Route42 routing parameters on top of imported profiles;
- separates connection profiles from reusable routing profiles;
- includes a built-in `Rule (RU + Local)` preset with local safety rules and RU direct-routing defaults;
- generates `sing-box` config from internal models instead of editing raw config by hand;
- supports `direct`, `proxy`, and `rule` routing modes;
- runs the VPN tunnel through Android `VpnService`.

## Main App Docs

- [PROJECT_RULES.md](PROJECT_RULES.md)
- [NOTICE.md](NOTICE.md)
- [LICENSE](../LICENSE)
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
- [docs/github-repository-metadata.md](docs/github-repository-metadata.md)
- [docs/android-vless-client-overview.md](docs/android-vless-client-overview.md)
- [docs/github-release-signing.md](docs/github-release-signing.md)
- [docs/use-your-own-vps-on-android.md](docs/use-your-own-vps-on-android.md)
- [docs/import-vless-links-on-android.md](docs/import-vless-links-on-android.md)
- [docs/sing-box-routing-on-android.md](docs/sing-box-routing-on-android.md)
- [docs/self-hosted-vpn-android-guide.md](docs/self-hosted-vpn-android-guide.md)
- [docs/import-link-routing-guide.md](docs/import-link-routing-guide.md)
- [docs/link-and-routing-spec.md](docs/link-and-routing-spec.md)
- [docs/route42-team-vps-and-routing-brief-2026-04-04.md](docs/route42-team-vps-and-routing-brief-2026-04-04.md)
- [docs/mvp-config.md](docs/mvp-config.md)
- [DONATE.md](DONATE.md)

## Workspace Docs

- [../SJLABORATORY/docs/vps-audit-2026-03-22.md](../SJLABORATORY/docs/vps-audit-2026-03-22.md)
- [../SJLABORATORY/docs/exoscale-vps-onboarding.md](../SJLABORATORY/docs/exoscale-vps-onboarding.md)
- [../SJLABORATORY/docs/uhost-vps-onboarding.md](../SJLABORATORY/docs/uhost-vps-onboarding.md)
- [../SJLABORATORY/docs/vultr-zero-cost-exit.md](../SJLABORATORY/docs/vultr-zero-cost-exit.md)
- [../SJLABORATORY/ops/README.md](../SJLABORATORY/ops/README.md)

## GitHub Releases

This repo includes a signed release APK workflow for GitHub Actions.

- The canonical signed release path for this repo is GitHub Actions, and the required signing secrets are already configured there.
- App-local keystore material lives in the ignored `secrets/` directory under `ROUTE42/`.
- A local helper script is available at `secrets/print-github-secrets.sh` to print the four GitHub secret values from ignored local files.
- Run the `Release APK` workflow manually to build a signed release artifact.
- Push a tag like `v0.1.1` to build a signed APK and attach it to the matching GitHub Release.

## Support Route42

If Route42 is useful to you, you can support development with a voluntary donation.

Details and the current wallet address are in [DONATE.md](DONATE.md).

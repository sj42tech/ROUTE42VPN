# Route42 Android VLESS Client Overview

Route42 is an Android VLESS client for self-hosted VPN and sing-box-compatible profiles. It is built for people who already have access to a VPS, a private VPN setup, or a provider that shares a `vless://` connection link.

The app focuses on three practical jobs: importing connection links, running the tunnel through Android `VpnService`, and applying routing behavior that is easy to understand and edit on the device. Instead of editing raw config files by hand, users can work with profile settings and routing rules in the app and let Route42 generate the runtime config.

Route42 is a good fit if you want:

- Android support for `vless://` links
- a client for self-hosted VPN access
- rule-based routing for direct, proxy, and split traffic
- a simple Android UI on top of sing-box-compatible profiles

Route42 is not a VPN provider. It does not include a VPS, shared traffic, or public access by itself. You bring your own server or import a profile from a provider you trust.

See also:

- [How to Use Your Own VPS for VPN on Android with Route42](use-your-own-vps-on-android.md)
- [How to Import VLESS Links on Android](import-vless-links-on-android.md)
- [Custom sing-box Routing on Android with Route42](sing-box-routing-on-android.md)

# Self-Hosted VPN on Android: Route42 Setup Guide

Route42 is designed for a straightforward self-hosted workflow on Android. You bring the server or access profile, Route42 handles the client side, and Android `VpnService` provides the local tunnel integration on the device.

If you are new to this kind of setup, the simplest way to think about it is this:

- your VPS runs the server side
- your `vless://` link carries the connection details
- Route42 imports that profile on Android
- the app applies your routing rules and starts the VPN tunnel

This is a better fit than a generic public VPN app when you want control over where the server is, how traffic is routed, and which profiles you trust. It is also useful when you already work with sing-box-compatible or Xray-based profiles and want a dedicated Android client that stays focused on import, connection, and routing.

Suggested path:

1. Set up your VPS or obtain a trusted profile.
2. Import the `vless://` link into Route42.
3. Review direct, proxy, and split routing behavior.
4. Connect the profile and test both tunnel traffic and direct traffic.

Helpful next reads:

- [Route42 Android VLESS Client Overview](android-vless-client-overview.md)
- [How to Use Your Own VPS for VPN on Android with Route42](use-your-own-vps-on-android.md)
- [How to Import VLESS Links on Android](import-vless-links-on-android.md)
- [Custom sing-box Routing on Android with Route42](sing-box-routing-on-android.md)

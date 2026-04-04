# How to Use Your Own VPS for VPN on Android with Route42

If you already run your own VPS, Route42 works as the Android client layer on top of that server. The usual setup is simple: deploy `Xray-core` or another sing-box-compatible server profile on your VPS, export or generate a `vless://` link, then import that link into Route42 on Android.

This approach is useful when you want control over server location, access credentials, routing rules, and DNS behavior. Instead of relying on a public VPN service, you manage your own endpoint and use Route42 as the mobile client that handles import, connection, and per-profile routing behavior.

Typical flow:

1. Deploy a VPS in the region you want.
2. Configure a VLESS-compatible server profile.
3. Export a `vless://` share link.
4. Import the link into Route42.
5. Adjust routing rules for direct, proxy, or split traffic.
6. Connect through Android `VpnService`.

Useful upstream references:

- [Project X / Xray-core](https://xtls.github.io/en/)
- [Xray Configuration Guide](https://xtls.github.io/en/config/)
- [XTLS/Xray-install](https://github.com/XTLS/Xray-install)
- [sing-box Documentation](https://sing-box.sagernet.org/configuration/)

For the import side of the flow, continue with [How to Import VLESS Links on Android](import-vless-links-on-android.md).

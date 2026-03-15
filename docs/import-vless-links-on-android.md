# How to Import VLESS Links on Android

Route42 supports importing standard `vless://` links on Android. This is the fastest way to add a working profile when you already have access credentials from your own VPS or from a trusted provider.

In practice, a `vless://` link usually includes the remote server address, port, user ID, transport settings, and any additional parameters required by the profile. Route42 reads that link, stores the profile, and lets you edit routing behavior separately inside the app.

Typical import flow:

1. Copy your `vless://` link.
2. Open Route42.
3. Go to the import screen.
4. Paste the link and save the profile.
5. Open the profile and review routing settings if needed.
6. Connect the profile from the main screen.

Route42 also supports custom Route42 routing parameters in imported links. Those parameters can define direct, proxy, or blocked domains and CIDR ranges before or after import.

For the full parameter reference, see:

- [Route42 Link Import And Routing Parameter Guide](import-link-routing-guide.md)
- [Route42 Link And Routing Specification](link-and-routing-spec.md)

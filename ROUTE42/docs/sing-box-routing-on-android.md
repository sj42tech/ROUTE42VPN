# Custom sing-box Routing on Android with Route42

One of the main reasons to use Route42 instead of a minimal share-link client is custom routing. Route42 is built around the idea that connection setup and routing behavior should be easy to manage from Android without editing a full raw config file every time.

The app supports routing models such as `direct`, `proxy`, and rule-based split traffic. That means you can send some traffic directly, push some traffic through the remote server, and keep specific local or private network ranges outside the tunnel. This is especially useful when you want a practical split setup for local services, selected domains, or region-specific behavior.

Common routing use cases:

- keep local or private network access direct
- send selected domains through the proxy
- keep some domains or IP ranges outside the tunnel
- combine a self-hosted VPS with rule-based Android traffic control

Route42 stores routing as reusable app-level profile data and generates a compatible runtime config for the tunnel. This keeps the UI easier to work with than hand-editing large sing-box config files on a phone.

Current routing model:

- one `ConnectionProfile` stores the VPS endpoint and transport settings;
- one `RoutingProfile` stores routing mode, DNS mode, imported rules, and manual rules;
- one routing profile can be reused by several saved VPS profiles;
- the built-in `Rule (RU + Local)` preset adds local safety rules, RU-oriented suffix defaults, curated domestic direct domains, and `geoip-ru` through a local sing-box `rule_set`.

For the exact parameter model and examples, see:

- [Route42 Link Import And Routing Parameter Guide](import-link-routing-guide.md)
- [Route42 Link And Routing Specification](link-and-routing-spec.md)
- [Route42 MVP Config Notes](mvp-config.md)

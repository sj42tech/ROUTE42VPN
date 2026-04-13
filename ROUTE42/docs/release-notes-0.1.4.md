# Route42 0.1.4 Release Notes

Status: planned

Date prepared: `2026-04-13`

## Summary

`0.1.4` is the release candidate that stabilizes real tunnel verification and clears up the recent `Hostkey` and `Exoscale` connection confusion.

The main finding from the latest diagnostics is that the application was not blocked by `Tele2` for these two VPS endpoints. The actual issue was stale `Reality` profile data in the active share links. After switching the client profiles to the current server-side `Reality` tuple, strict Route42 smoke tests passed both directly and through the `v7 -> Tele2` lab path.

`AWS` remains a known bad candidate and should stay out of the recommended profile set for now.

## What Is Included

- stricter smoke-test logic that requires real tunnel egress instead of only `VPN CONNECTED`
- exit IP verification in the Android instrumentation flow
- popular-site reachability verification through the active tunnel
- clearer diagnosis of bad VPS paths, especially timeouts on `AWS`

## User Impact

Users with older imported `Hostkey` or `Exoscale` profiles may still have stale `Reality` parameters saved locally.

Recommended user action for `0.1.4`:

1. delete older `Hostkey` and `Exoscale` profiles in Route42
2. import the refreshed profiles again from the current private share links or share codes
3. connect again and confirm the tunnel shows the expected VPS exit IP

## Verified On 2026-04-13

Strict Route42 instrumentation checks passed for:

- `Hostkey` direct path
- `Exoscale` direct path
- `Hostkey` through `MikroTik v7 -> Tele2`
- `Exoscale` through `MikroTik v7 -> Tele2`

Strict Route42 instrumentation checks failed for:

- `AWS`

Observed `AWS` failure mode:

- tunnel egress checks timed out on `3.34.30.191:443`
- popular-site probes through the tunnel never reached a healthy state

## Release Recommendation

Ship `0.1.4` with refreshed profile guidance for `Hostkey` and `Exoscale`, and do not promote `AWS` as a working default profile until its ingress path is fixed and re-verified.

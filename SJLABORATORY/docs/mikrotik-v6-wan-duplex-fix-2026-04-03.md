# MikroTik v6 WAN Duplex Fix 2026-04-03

This note records the working-router WAN incident on `2026-04-03`, the evidence that the fault was at the `ether1` negotiation layer, and the workaround that restored normal direct internet performance.

## Scope

- working router: MikroTik `v6` LAN gateway at `192.168.0.1`
- WAN interface under test: `ether1`
- upstream path under test: local ISP on `10.12.109.5`
- desktop tunnel policy: keep `local.xray` untouched while diagnosing the direct path

## Symptoms Before The Fix

Observed on the working `v6` router before the fix:

- `ether1` repeatedly negotiated as `100Mbps`, `full-duplex=no`
- link partner advertised `100M-half`
- router logs repeatedly showed:
  - `ether1 excessive or late collision, link duplex mismatch?`
- `watch-v6-wan-link.sh` showed non-zero growth in:
  - `rx-align-error`
  - `tx-collision`
  - `tx-late-collision`

Practical user-visible symptoms:

- direct provider internet was slow and inconsistent
- desktop tunnel performance also degraded because it sat on top of the same bad direct path

## Controlled Test

Two tools were used:

- `/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/watch-v6-wan-link.sh`
- `/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/test-v6-forced-full-duplex.sh`

The controlled test compared three phases:

1. `baseline_autoneg`
2. `forced_full_duplex`
3. `post_autoneg`

The test URL was:

- `https://speed.cloudflare.com/__down?bytes=20000000`

## Baseline In Auto-Negotiation

Baseline WAN state:

- `status=link-ok`
- `rate=100Mbps`
- `full-duplex=no`
- `partner=100M-half`

During the `60s` baseline:

- both transfer attempts hit the `30s` curl timeout
- total transferred data was only about `0.03 MB`
- average throughput was about `545 B/s`

Router error deltas during the same baseline minute:

- `rx_align +10`
- `tx_collision +11`
- `tx_late +6`

Interpretation:

- the WAN path was functionally degraded even before higher-layer routing was considered
- the router still had a live link, but the Ethernet negotiation state was clearly unhealthy

## Forced 100M/Full Test

The interface was temporarily forced to:

- `auto-negotiation=no`
- `speed=100Mbps`
- `full-duplex=yes`

During the `300s` forced phase:

- the link stayed at `100Mbps full-duplex`
- router error deltas stayed at exactly zero:
  - `rx_align=0`
  - `tx_collision=0`
  - `tx_late=0`

Traffic results improved sharply:

- many `20 MB` downloads completed in about `4.8-5.1s`
- typical throughput on successful runs was about `3.9-4.1 MB/s`
- phase average was about `19.58 Mbps`

Some individual attempts still timed out, so the upstream path was not perfectly clean.
But the duplex-mismatch symptom itself disappeared completely in forced mode.

## Return To Auto-Negotiation

After returning `ether1` to auto-negotiation:

- the link fell back to `100M-half`
- the direct-path degradation returned
- router counters jumped sharply again during the short post phase:
  - `rx_align +756`
  - `tx_collision +721`
  - `tx_late +30`

Interpretation:

- the failure mode tracks the negotiation mode directly
- this is strong evidence that the root problem is at the `autonegotiation / duplex mismatch` layer between `ether1` and the provider side

## Final Workaround Left In Place

Because the forced mode was clearly healthier, the working router was left in this state:

- `ether1`
- `auto-negotiation=disabled`
- `speed=100Mbps`
- `full-duplex=yes`

Post-fix verification:

- direct internet remained up
- gateway ping stayed at `0% loss`
- `1.1.1.1` ping stayed at `0% loss`
- `Hostkey 5.39.219.74` ping stayed at `0% loss`
- `watch-v6-wan-link.sh` showed zero new `rx_align`, `tx_collision`, and `tx_late` growth over repeated live samples
- a direct `20 MB` download completed in about `4.86s` at about `4.12 MB/s`

User-reported result after the fix:

- about `28.98 Mbps` down
- about `28.98 Mbps` up

## Operational Conclusion

The main problem was not the VPS fleet.
The main problem was the physical or negotiation state of the WAN Ethernet link on the working MikroTik `v6` router.

Most likely causes:

- bad cable or connector history
- bad negotiation with the provider-side port
- provider-side port or media issue that collapses the link into `100M-half`

The current practical workaround is valid and should stay in place unless the provider side is repaired and proven to negotiate correctly in auto mode.

## Reuse

To monitor the current WAN behavior live:

```bash
bash /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/watch-v6-wan-link.sh
```

To repeat the comparative throughput test:

```bash
bash /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/test-v6-forced-full-duplex.sh
```

To return `ether1` to auto-negotiation manually if needed:

```routeros
/interface ethernet set ether1 auto-negotiation=yes
```

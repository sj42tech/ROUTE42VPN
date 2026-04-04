# Hostkey-Like VPS Shortlist 2026-04-04

This note narrows the next-provider search down to small self-service VPS providers that are realistically close to the current `Hostkey` role in this project.

Selection rules used here:

- public self-service VPS plans only
- small Linux-capable plans in the practical `1-2 vCPU`, `1-2 GB RAM`, `10-100 GB` range when available
- provider site checked directly from the current fixed network with `curl --noproxy '*'`
- live desktop `local.xray` tunnel left untouched during direct checks
- current direct checks were re-run after the MikroTik `v6` WAN fix, so these measurements are materially more trustworthy than the old half-duplex period

## Reachability Rule

For the provider-site check:

- `200` means direct reachability looked healthy
- `403` still counts as reachable enough for registration and docs work, because DNS, TCP, and TLS clearly worked and only the site-side bot/WAF policy denied the request
- `000` means timeout or no usable direct page response from the current network

## Current Shortlist

| Provider | Official small plan checked | Price on 2026-04-04 | Direct provider-site result from current network | Fit for this project | Recommendation |
| --- | --- | --- | --- | --- | --- |
| `Aruba Cloud` | `Cloud VPS OpenStack Starter` = `1 vCPU / 1 GB / 20 GB / 2 TB/month` | `€1.99 + VAT / month` | `200`, about `0.72s total` | Very strong like-for-like cheap reserve VPS. Public plan includes one public IP and Linux support. | `Best next provider to test first` |
| `Time4VPS` | `Linux 1` = `1 CPU / 1 GB / 10 GB`; next step `Linux 2` = `1 CPU / 2 GB / 20 GB` | `€2.99 / month`; `Linux 2` promo `€3.85 / month`, renews at `€7.69 / month` | `403`, about `0.20s total` | Very strong cheap fit. Route to the site is alive, and the product shape is close to what we want for Xray. | `Best cheap exact-fit fallback after Aruba` |
| `UpCloud` | Developer plan `1 GB / 1 CPU / 10 GB`; next exact-fit step `1 GB / 1 CPU / 20 GB`; next practical step `2 GB / 1 CPU / 30 GB` | `€3 / month`; `€4.5 / month`; `€8 / month` | `403`, about `0.23s total` | More premium than bargain VPS hosts, but clean pricing, included IPv4, included transfer, and strong product polish. | `Best premium small-VPS candidate` |
| `Akamai Cloud / Linode` | `Nanode 1 GB` = `1 GB / 1 CPU / 25 GB / 1 TB`; next step `Linode 2 GB` = `2 GB / 1 CPU / 50 GB / 2 TB` | `$5 / month`; `$12 / month` | `403`, about `0.31s total` | More expensive than the cheapest EU hosts, but still a credible self-service fallback with a mature API/tooling story. | `Good global fallback if we want a safer mainstream provider` |
| `Contabo` | `Cloud VPS 10` = `3 vCPU / 8 GB / 75 GB NVMe` | `€4.50 / month` | `403`, about `0.25s total` | Route to the site is alive, but this is not a true like-for-like tiny VPS. It is cheap, but materially oversized for our Xray-only use case. | `Only if we accept a larger box instead of a small Hostkey-like node` |

## Not First Picks From The Current Network

These providers are not the best immediate next tests from the current fixed network because their public pages timed out or remain otherwise inconvenient right now:

- `Hetzner` -> direct site check `000`, timeout at `10s`
- `IONOS` -> direct site check `000`, timeout at `10s`
- `OVHcloud` -> direct site check `000`, timeout at `10s`
- `DigitalOcean` -> direct site check `000`, timeout at `10s`
- `Kamatera` -> direct site check `000`, timeout at `10s`

Already-known special cases:

- `Scaleway` still looks technically viable, but payment setup is paused and the public page was slower than the top shortlist candidates
- `RackCorp` remains too expensive for this project
- `AWS` already exists in the fleet, but route quality from the current network remains weak compared to `Hostkey`

## Practical Recommendation Order

If we want to add two more providers for real switching options, the best current order is:

1. `Aruba Cloud`
2. `Time4VPS`
3. `UpCloud`
4. `Akamai Cloud / Linode`

Why this order:

- `Aruba Cloud` is currently the cleanest blend of low price, small shape, and direct reachability
- `Time4VPS` is still one of the cheapest exact-fit VPS products we have found
- `UpCloud` is not the cheapest, but it looks clean and operationally serious
- `Linode` is a safe mainstream fallback, but it costs more than the EU bargain hosts

## Direct Check Snapshot

The direct no-proxy provider-site batch that informed this shortlist produced these results on `2026-04-04`:

| Provider | Direct result |
| --- | --- |
| `Hostkey` | `200|0.037154|0.083743|0.749790|0.803387|221754` |
| `Exoscale` | `200|0.065038|0.140499|0.226288|0.573312|388247` |
| `Time4VPS` | `403|0.047902|0.138261|0.204153|0.204478|5466` |
| `Aruba Cloud` | `200|0.102792|0.196168|0.305573|0.720037|104258` |
| `UpCloud` | `403|0.062235|0.191404|0.227380|0.230308|4607` |
| `Contabo` | `403|0.063202|0.133867|0.247801|0.251442|19961` |
| `Hetzner` | `000|0.000000|0.000000|0.000000|10.006354|0` |
| `IONOS` | `000|0.000000|0.000000|0.000000|10.005271|0` |
| `OVHcloud` | `000|0.000000|0.000000|0.000000|10.002270|0` |
| `Scaleway` | `200|0.078081|0.198566|0.394536|10.004172|19139` |
| `Akamai Cloud / Linode` | `403|0.042668|0.101052|0.305460|0.305721|2982` |
| `DigitalOcean` | `000|0.000000|0.000000|0.000000|10.003034|0` |

Field format:

- `http_code|connect_s|tls_s|ttfb_s|total_s|bytes`

## Official Sources

- [Aruba Cloud VPS](https://www.arubacloud.com/vps/)
- [Time4VPS Linux VPS](https://www.time4vps.com/linux-vps/)
- [UpCloud Pricing](https://upcloud.com/pricing/)
- [Akamai Cloud Pricing](https://www.akamai.com/cloud/pricing)
- [Linode Pricing](https://www.linode.com/pricing/)
- [Contabo Pricing](https://contabo.com/en/pricing/)

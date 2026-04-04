# Provider Direct Reachability 2026-03-28

This note records direct provider-path checks that were run from the laptop without touching the live local Xray tunnel.

Safety rule used during this check:

- `local.xray` was not stopped
- macOS proxy settings were not disabled
- direct checks were run with no proxy while the live tunnel stayed active

Tool used:

- `ops/diagnose-direct-provider-path.sh`

## Live Tunnel State Before Direct Checks

Observed locally on `2026-03-28`:

- `http_proxy_listener=up:8080`
- `socks_proxy_listener=up:1080`
- `proxy_exit_ip=5.39.219.74`

Conclusion:

- the working live desktop tunnel stayed on `Hostkey 5.39.219.74`
- the direct provider checks below were not performed by disabling Xray

## Direct Path Egress

- `direct_exit_ip=91.218.116.71`
- direct path mode: default system route, no interface pinning

## Direct TCP Reachability To Current Provider Ingress IPs

- `5.39.219.74:443` reachable
- `194.182.174.240:443` reachable

Conclusion:

- both current VPS ingress IPs were reachable directly from the current network

## Direct HTTPS Checks To Control Sites And Provider Sites

Output fields:

- `http_code|connect_s|tls_s|ttfb_s|total_s|bytes|bytes_per_s`

Results:

- `google_204|204|0.056335|0.072788|0.089794|0.089830|0|0`
- `github|200|0.083419|0.174917|0.245314|1.639332|565983|345252`
- `cloudflare|200|0.065713|0.092579|0.467337|0.762885|981685|1286806`
- `hostkey_site|200|0.090783|0.164585|0.910642|1.236990|233690|188918`
- `exoscale_site|200|0.099014|0.172513|0.248950|0.673437|51898|77064`
- `aws_lightsail_site|200|1.179545|1.254486|1.450515|1.639261|234497|143050`
- `tencent_lighthouse_site|000|0.000000|0.000000|0.000000|0.463895|0|0`
- `alibaba_sas_site|200|0.747066|0.870134|0.963520|1.419749|69154|48708`

## Interpretation

- `AWS Lightsail` site was directly reachable from the current network without using the tunnel
- `Alibaba Cloud` Simple Application Server pricing page was also directly reachable
- `Tencent Cloud Lighthouse` site did not connect directly on this check
- `Hostkey` and `Exoscale` public sites were both directly reachable

Practical implication for the next provider track:

1. `AWS Lightsail` is a clean next registration candidate because both its public pricing/docs path and our current live tunnel can coexist during evaluation
2. `Tencent Cloud Lighthouse` still looks good on paper for price, but the failed direct site connection is already a red flag from the current network
3. `Alibaba Cloud` remains possible, but `AWS Lightsail` currently looks easier to test first


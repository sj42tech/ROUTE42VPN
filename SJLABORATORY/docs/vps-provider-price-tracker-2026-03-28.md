# VPS Provider Price Tracker 2026-03-28

This note tracks the small-VPS pricing that matters for this project.

Target envelope for comparison:

- `1-2 vCPU`
- `1-2 GB RAM`
- `10-100 GB` disk

Rules used here:

- if a provider has a public self-service plan inside that envelope, record that plan
- if no exact public plan exists, record the closest cheap public plan and mark it as outside the target
- if no public numeric tariff is visible, mark it as `quote-only` or `public price not verified`
- prices are copied as shown on the provider side on `2026-03-28`
- VAT/tax treatment is not normalized across providers

## Quick Read

- `RackCorp` is expensive for this project
- `RackCorp` price catalog looked identical for both `Kyrgyzstan` and `Mongolia` on the check date
- the cheapest exact-fit public options in this list are currently `Time4VPS` and `Scaleway`
- among the Asia-oriented public options checked here, the most attractive low-cost choices are `Tencent Cloud Lighthouse` and `AWS Lightsail`
- `uHost` remains a `quote-only` / higher-exit-risk provider, not a clean disposable VPS choice
- the current next-plan European candidates in the `Hostkey` replacement track are `OVHcloud` and `Scaleway`
- `Scaleway` looks technically viable, but this track is currently paused because payment setup did not complete

## Active Project Provider List

| Provider | Region / Product | Fits Target Envelope | Public plan or formula checked on 2026-03-28 | Price on check date | Notes | Source |
| --- | --- | --- | --- | --- | --- | --- |
| `Hostkey` | EU VPS hourly | `No exact public fit` | `Basic Plan` = `2 cores / 4 GB RAM / 50 GB SSD` | `$3.60/month` or `$0.005/hour` | Cheapest public Hostkey plan found, but RAM is above target. Current production baseline is still `5.39.219.74`. | [HOSTKEY hourly VPS](https://hostkey.com/vps/hourly/) |
| `Vultr` | Cloud Compute, Regular Performance | `Yes` | `1 vCPU / 1 GB / 25 GB` | `$5.00/month` | Exact fit exists publicly. Next step in range is `1 vCPU / 2 GB / 55 GB` at `$10/month`. Provider is already decommissioned for this project, but pricing stays useful as a benchmark. | [Vultr pricing](https://www.vultr.com/pricing/) |
| `Exoscale` | Standard instance + local storage | `Yes` | `Tiny` = `1 core / 1 GB` plus `10 GB` local storage | `~€11.51/month` | Computed from official hourly pricing: `Tiny` `0.01458/hour` plus local storage `0.00014/GiB/hour`, using `720h`. Practical `2 vCPU / 2 GB` option is `Small + 10 GB` at `~€17.81/month`. | [Exoscale pricing page](https://www.exoscale.com/pricing/), [Exoscale opencompute pricing API](https://portal.exoscale.com/api/pricing/opencompute) |
| `Hetzner Cloud` | Shared cloud | `No exact public fit` | `CX23` = `2 vCPU / 4 GB / 40 GB` | `€3.49/month` on `2026-03-28` | Closest cheap public plan found; RAM is above target. Official docs also say this rises to `€4.09/month` from `2026-04-01`, so this provider should be rechecked if ordered later. | [Hetzner Cloud](https://www.hetzner.com/cloud/), [Hetzner price adjustment](https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/) |
| `OVHcloud` | VPS 2026 range | `No exact public fit` | `VPS-1` = `4 vCores / 8 GB / 75 GB SSD` | `from $6.46/month` | Current public VPS range starts above the target envelope. Good to know as a larger fallback, but not a like-for-like tiny VPS. | [OVHcloud VPS](https://www.ovhcloud.com/en/vps/) |
| `Scaleway` | Virtual Instances + block storage + IPv4 | `Yes` | Cheapest exact-fit path: `STARDUST1-S` = `1 vCPU / 1 GB` + `10 GB` block storage + `1 IPv4` | `~€3.89/month` | Approx breakdown: compute `~€0.10/month`, block storage `~€0.86/month`, IPv4 `~€2.92/month`. Practical `DEV1-S` `2 vCPU / 2 GB` path with the same `10 GB` and one IPv4 is about `~€10.20/month`. API checks on `2026-03-28` showed `DEV1-S` as the smallest visible practical type in tested zones; `STARDUST1-S` did not appear in the API catalog for the checked zones. The provider track is currently paused because payment setup with the available card did not complete. | [Scaleway virtual instances pricing](https://www.scaleway.com/en/pricing/virtual-instances/), [Scaleway storage pricing](https://www.scaleway.com/en/pricing/storage/) |
| `AWS Lightsail` | Asia regions | `Yes` | Exact-fit public IPv4 plan: `1 GB / 2 vCPU / 40 GB SSD` | `$7/month` | Official Lightsail regions include `Mumbai`, `Seoul`, `Singapore`, and `Tokyo`. The next exact-fit step in range is `2 GB / 2 vCPU / 60 GB` at `$12/month`. There is also a cheaper `IPv6-only` bundle at `$3.50/month`, but it sits below the target because it has only `512 MB` RAM. | [Amazon Lightsail pricing](https://aws.amazon.com/lightsail/pricing/), [Lightsail regions](https://docs.aws.amazon.com/lightsail/latest/userguide/understanding-regions-and-availability-zones-in-amazon-lightsail.html) |
| `Tencent Cloud Lighthouse` | Asia regions | `Yes` | `Linux Starter` = `2 vCPU / 2 GB / 40 GB SSD` | `$4.2/month` | Strong low-cost Asia candidate. The same product page also shows `Razor Speed` = `2 vCPU / 1 GB / 40 GB SSD` at `$5.0/month` and `Linux General` = `2 vCPU / 4 GB / 90 GB SSD` at `$8.5/month`. | [Tencent Cloud Lighthouse](https://www.tencentcloud.com/jp/products/lighthouse) |
| `Alibaba Cloud Simple Application Server` | Asia regions | `Yes` | Closest exact-fit public plan: `1 GB / 1 CPU / 20 GB SSD` | `$4.5/month` | Public plans continue with `2 GB / 1 CPU / 40 GB` at `$9/month` and `2 GB / 2 CPU / 60 GB` at `$15/month`. Supported Asia regions include `China (Hong Kong)`, `Singapore`, `Japan (Tokyo)`, and `South Korea (Seoul)`. Mainland China is possible, but that path carries ICP filing and domain constraints. | [Alibaba SAS pricing](https://www.alibabacloud.com/en/product/swas/pricing?_p_lc=1), [Alibaba SAS regions](https://www.alibabacloud.com/help/en/simple-application-server/product-overview/regions-and-network-connectivity), [Alibaba ICP filing note](https://www.alibabacloud.com/help/doc-detail/36891.html) |
| `Time4VPS` | Linux VPS | `Yes` | `Linux 1` = `1 CPU / 1 GB / 10 GB` | `€2.99/month` | Exact fit exists publicly. Another exact-fit step is `Linux 2` = `1 CPU / 2 GB / 20 GB` at promo `€3.85/month`, renewing at `€7.69/month`. | [Time4VPS Linux VPS](https://www.time4vps.com/linux-vps/) |
| `RackCorp` | `KG` and `MN` cloud VM via unit pricing | `Yes` | Smallest checked exact-fit custom build: `1 CPU / 1 GB / 10 GB / 1 IPv4 / 100 Mbit / 100 GB traffic` | `61.50 AUD/month` | This provider is expensive. On the check date, the same official price catalog was returned for both `Kyrgyzstan` and `Mongolia`. The more realistic Hostkey-like build `2 CPU / 2 GB / 60 GB` came out at `101.50 AUD/month`. | [RackCorp REST API](https://wiki.rackcorp.com/books/help-and-support-en/page/rackcorp-rest-api), [RackCorp REST API examples](https://wiki.rackcorp.com/books/help-and-support-en/page/rackcorp-rest-api-examples), [RackCorp DC codes](https://wiki.rackcorp.com/books/help-and-support-en/page/rackcorp-datacenter-locations-and-codes) |
| `uHost` | Cloud hosting | `No public price verified` | `quote-only` | `N/A` | Public site did not expose a clean small VPS tariff. Exit conditions remain unfriendly for disposable experiments. | [uHost request a quote](https://uhost.com/request-a-quote.php), [uHost legal](https://www.uhost.com/legal.php) |
| `Cloud.mn` | Mongolia cloud / virtual server | `Public price not verified` | Public pricing page exists, but no clear numeric small VPS tier was visible without deeper console flow | `N/A` | Keep as a Mongolia-specific research lead, but not a priced candidate yet. | [Cloud.mn pricing](https://cloud.mn/en/pricing) |
| `mCloud` | Mongolia VPS | `No exact public fit` | `SSD-1` = `4 Core / 8 GB / 50 GB SSD` | `₮70,000/month` | Public smallest VPS found is far above the target envelope. Useful as a Mongolia-local reference, but not a tiny cheap VPS. | [mCloud VPS store](https://mcloud.mn/store/vps), [mCloud VPS hosting](https://mcloud.mn/vps-hosting) |

## RackCorp Cost Breakdown

RackCorp is the one provider in this list where we have a live unit-price formula from the official API rather than a single public SKU.

Smallest exact-fit build checked:

- `CPU`: `1 x 20 AUD = 20 AUD`
- `MEMORYGB`: `1 x 10 AUD = 10 AUD`
- `STORAGEGB`: `10 x 0.2 AUD = 2 AUD`
- `IPV4`: `5 AUD`
- `NT-SPEED100`: `15 AUD`
- `TRAFFICGB-100`: `9.5 AUD`
- `BKP-FREE`: `0 AUD`
- `SUPPORT-STD`: `0 AUD`

Total:

- `61.50 AUD/month`

Hostkey-like comparison build:

- `2 CPU / 2 GB / 60 GB / 1 IPv4 / 100 Mbit / 100 GB traffic`
- `101.50 AUD/month`

Conclusion:

- `RackCorp Kyrgyzstan` and `RackCorp Mongolia` are not cheap backup VPS options for this project
- use RackCorp only if the geography itself is worth the price premium

## Practical Takeaway

For cheap self-service experiments, the price picture on `2026-03-28` looks like this:

1. `Time4VPS` is currently the cheapest exact-fit public VPS in this tracker
2. `Scaleway` can also be cheap, but you need to account for separate storage and IPv4
3. for Asia-specific experiments, `Tencent Cloud Lighthouse` and `AWS Lightsail` are the most interesting low-cost options in this tracker
4. `Vultr` remains a reasonable public benchmark even though it failed as a working path in this project
5. `RackCorp` is too expensive to be treated as a casual reserve VPS

## Plan-State Candidates

These providers are currently in the plan queue, not in the active fleet:

- `OVHcloud`
  - good direct site reachability from the current network
  - stronger self-service shape than `uHost` / `U1HOST`
  - public VPS line currently starts above the tiny `1-2 GB` envelope, but still looks like a realistic next provider to test

- `Scaleway`
  - good direct site reachability from the current network
  - still attractive on price if we account carefully for separate IPv4 and storage billing
  - account exists, but this path is paused because payment setup did not complete with the available card
  - should be treated as a planned experiment, not an active provider

## Asia Notes

- `AWS Lightsail` currently covers the cleanest low-friction Asia regions for this project: `Mumbai`, `Seoul`, `Singapore`, and `Tokyo`
- `Tencent Cloud Lighthouse` currently looks like the cheapest strong Asia fit in this tracker
- `Alibaba Cloud Simple Application Server` is attractive for `Hong Kong`, `Singapore`, `Tokyo`, and `Seoul`, but `China mainland` deployment is a different class of project because of ICP filing and related domain constraints

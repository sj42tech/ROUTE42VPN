# Tele2 Domain Matrix 2026-03-29

This report was generated through the isolated MikroTik v7 hotspot lab without touching the live desktop Xray tunnel.

## Safety

- MacBook stayed on the working `v6` LAN
- live desktop tunnel was not stopped
- Tele2 was temporarily promoted to the primary WAN only on `v7` during the test run
- `v7` was restored to `ether1 distance=1` and `wifi2 distance=10` after the run

## Context

- MikroTik test host: `192.168.0.106`
- Source client IP on working LAN: `192.168.0.107`
- Live desktop proxy exit IP before run: `5.39.219.74`
- Connect timeout: `5s`
- Curl max time: `10s`
- Domain list: `/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/tele2-domain-matrix.domains.tsv`

## Tele2 Route Promotion

```text
spawn ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null admin@192.168.0.106 
/ip dhcp-client set [find comment=defconf] default-route-distance=20;
/ip dhcp-client set [find comment=tele2-hotspot] default-route-distance=1;
/ip route print detail where dst-address=0.0.0.0/0


Warning: Permanently added '192.168.0.106' (RSA) to the list of known hosts.
** WARNING: connection is not using a post-quantum key exchange algorithm.
** This session may be vulnerable to "store now, decrypt later" attacks.
** The server may need to be upgraded. See https://openssh.com/pq.html
admin@192.168.0.106's password: 
Flags: D - dynamic; X - disabled, I - inactive, A - active; 
c - connect, s - static, r - rip, b - bgp, o - ospf, i - is-is, d - dhcp, v - vpn, m - modem, y - bgp-mpls-vpn; 
H - hw-offloaded; + - ecmp 
   DAd   dst-address=0.0.0.0/0 routing-table=main 

   D d   dst-address=0.0.0.0/0 routing-table=main gateway=192.168.0.1 
         immediate-gw=192.168.0.1%ether1 distance=20 scope=30 target-scope=10 
         vrf-interface=ether1 

   DAd + dst-address=0.0.0.0/0 routing-table=main gateway=10.36.173.186 
         immediate-gw=10.36.173.186%wifi2 distance=1 scope=30 target-scope=10 
         vrf-interface=wifi2 

```

## Domain Matrix

| Category | Domain | IPv4 | TCP 443 | HTTP Code | Connect s | TLS s | TTFB s | Total s | Outcome |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| global | google.com | 142.251.38.78 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006354 | down |
| global | www.youtube.com | 64.233.162.198 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.007052 | down |
| global | m.youtube.com | 216.58.209.174 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.004261 | down |
| global | youtubei.googleapis.com | 216.239.34.223 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006103 | down |
| global | i.ytimg.com | 192.178.25.86 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006628 | down |
| global | s.ytimg.com | 64.233.162.198 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.001199 | down |
| global | googlevideo.com | 216.58.209.164 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006733 | down |
| global | redirector.googlevideo.com | 64.233.162.198 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006891 | down |
| global | www.github.com | 140.82.121.3 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006271 | down |
| global | www.cloudflare.com | 104.16.124.96 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006366 | down |
| global | www.wikipedia.org | 185.15.59.224 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006138 | down |
| global | www.microsoft.com | 2.23.90.89 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.005566 | down |
| global | www.apple.com | 184.24.145.53 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.007033 | down |
| global | apps.apple.com | 184.24.144.29 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.007082 | down |
| global | www.samsung.com | 104.103.64.59 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.001669 | down |
| global | play.google.com | 192.178.25.78 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006655 | down |
| global | www.amazon.com | 3.164.64.231 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006888 | down |
| global | www.openai.com | 104.18.33.45 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.002403 | down |
| messaging | telegram.org | 149.154.167.99 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006420 | down |
| messaging | web.telegram.org | 149.154.167.99 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006505 | down |
| messaging | api.telegram.org | 149.154.166.110 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.003920 | down |
| ru_portal | ya.ru | 77.88.55.242 | up | 302 | 0.066678 | 0.260154 | 0.324751 | 0.325102 | http_up |
| ru_portal | yandex.ru | 77.88.55.88 | up | 302 | 0.062007 | 0.132579 | 0.193828 | 0.194053 | http_up |
| ru_portal | vk.com | 93.186.225.194 | up | 302 | 0.088405 | 0.157924 | 0.217900 | 0.218098 | http_up |
| ru_portal | mail.ru | 90.156.232.4 | up | 302 | 0.130672 | 0.279663 | 0.447499 | 0.447911 | http_up |
| ru_portal | ok.ru | 95.163.61.74 | up | 200 | 0.061858 | 0.142562 | 0.303869 | 0.632326 | http_up |
| ru_portal | dzen.ru | 185.180.200.2 | up | 404 | 0.051673 | 0.132684 | 0.203060 | 0.203075 | http_up |
| ru_media | rutube.ru | 109.238.90.239 | up | 403 | 0.060259 | 0.140170 | 0.212890 | 0.213230 | http_up |
| ru_media | ria.ru | 194.190.139.47 | up | 200 | 0.049143 | 0.144329 | 0.278936 | 0.437639 | http_up |
| ru_media | lenta.ru | 81.19.72.32 | up | 200 | 0.048152 | 0.178252 | 1.210415 | 1.348136 | http_up |
| ru_media | www.rbc.ru | 178.248.234.119 | up | 200 | 0.054540 | 0.124549 | 0.180489 | 0.180697 | http_up |
| ru_media | www.kommersant.ru | 178.248.238.19 | up | 200 | 0.062757 | 0.130533 | 0.278042 | 0.587680 | http_up |
| ru_commerce | www.ozon.ru | 185.73.193.68 | up | 307 | 0.055925 | 0.126979 | 0.181989 | 0.182424 | http_up |
| ru_commerce | www.wildberries.ru | 185.62.202.2 | up | 498 | 0.057889 | 0.134765 | 0.196163 | 0.196746 | http_up |
| ru_commerce | www.avito.ru | 176.114.122.24 | up | 403 | 0.043393 | 0.110245 | 0.206306 | 0.228643 | http_up |
| ru_gov | www.gosuslugi.ru | 213.59.254.7 | up | 000 | 0.053395 | 0.000000 | 0.000000 | 0.216195 | tls_reset |
| ru_gov | www.nalog.gov.ru | 212.193.146.145 | up | 301 | 0.047899 | 0.116224 | 0.182921 | 0.183482 | http_up |
| ru_gov | www.mos.ru | 212.11.151.56 | up | 000 | 0.062503 | 0.000000 | 0.000000 | 0.229038 | tls_reset |
| ru_bank | www.sberbank.ru | 84.252.149.206 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.005856 | down |
| ru_bank | www.tbank.ru | 178.130.128.27 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006528 | down |
| ru_bank | alfabank.ru | 217.12.104.100 | up | 403 | 0.051448 | 0.115173 | 0.190492 | 0.190643 | http_up |
| ru_bank | www.vtb.ru | 195.242.82.13 | up | 403 | 0.079732 | 0.168114 | 0.233965 | 0.234136 | http_up |
| provider_site | hostkey.com | 8.47.69.6 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006434 | down |
| provider_site | exoscale.com | 159.100.253.88 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.004517 | down |
| provider_site | hetzner.com | 213.133.116.44 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006867 | down |
| provider_site | www.ovhcloud.com | 198.27.92.14 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006896 | down |
| provider_site | www.scaleway.com | 104.20.44.139 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.005044 | down |
| provider_site | aws.amazon.com | 13.33.235.119 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006588 | down |
| provider_site | www.tencentcloud.com | 0.0.0.1 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006174 | down |
| provider_site | www.alibabacloud.com | 47.91.64.23 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.004828 | down |
| provider_site | www.time4vps.com | 8.47.69.6 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.002059 | down |
| provider_site | www.rackcorp.mn | 103.43.119.187 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006737 | down |
| provider_site | u1host.com | 64.188.114.188 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006379 | down |
| provider_site | cloud.mn | 103.50.204.94 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.001411 | down |
| provider_site | mcloud.mn | 188.114.97.1 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.006171 | down |
| provider_site | www.vultr.com | 104.17.141.186 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.003189 | down |

## Summary

- tested IPv4-backed domains: `56`
- TCP 443 reachable: `19`
- HTTP/TLS succeeded with non-zero HTTP code: `17`
- partial cases (TCP only or TLS reset): `2`
- complete failures: `37`

## Working Domains

- `ya.ru` -> `302`
- `yandex.ru` -> `302`
- `vk.com` -> `302`
- `mail.ru` -> `302`
- `ok.ru` -> `200`
- `dzen.ru` -> `404`
- `rutube.ru` -> `403`
- `ria.ru` -> `200`
- `lenta.ru` -> `200`
- `www.rbc.ru` -> `200`
- `www.kommersant.ru` -> `200`
- `www.ozon.ru` -> `307`
- `www.wildberries.ru` -> `498`
- `www.avito.ru` -> `403`
- `www.nalog.gov.ru` -> `301`
- `alfabank.ru` -> `403`
- `www.vtb.ru` -> `403`

## Partial Domains

- `www.gosuslugi.ru` -> TLS reset / interrupted after connect
- `www.mos.ru` -> TLS reset / interrupted after connect

## Down Domains

- `google.com`
- `www.youtube.com`
- `m.youtube.com`
- `youtubei.googleapis.com`
- `i.ytimg.com`
- `s.ytimg.com`
- `googlevideo.com`
- `redirector.googlevideo.com`
- `www.github.com`
- `www.cloudflare.com`
- `www.wikipedia.org`
- `www.microsoft.com`
- `www.apple.com`
- `apps.apple.com`
- `www.samsung.com`
- `play.google.com`
- `www.amazon.com`
- `www.openai.com`
- `telegram.org`
- `web.telegram.org`
- `api.telegram.org`
- `www.sberbank.ru`
- `www.tbank.ru`
- `hostkey.com`
- `exoscale.com`
- `hetzner.com`
- `www.ovhcloud.com`
- `www.scaleway.com`
- `aws.amazon.com`
- `www.tencentcloud.com`
- `www.alibabacloud.com`
- `www.time4vps.com`
- `www.rackcorp.mn`
- `u1host.com`
- `cloud.mn`
- `mcloud.mn`
- `www.vultr.com`

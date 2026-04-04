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
   D d   dst-address=0.0.0.0/0 routing-table=main gateway=192.168.0.1 
         immediate-gw=192.168.0.1%ether1 distance=20 scope=30 target-scope=10 
         vrf-interface=ether1 

   DAd   dst-address=0.0.0.0/0 routing-table=main gateway=10.36.173.186 
         immediate-gw=10.36.173.186%wifi2 distance=1 scope=30 target-scope=10 
         vrf-interface=wifi2 

```

## Domain Matrix

| Category | Domain | IPv4 | TCP 443 | HTTP Code | Connect s | TLS s | TTFB s | Total s | Outcome |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| global | google.com | 142.251.38.78 | down | 000 | 0.000000 | 0.000000 | 0.000000 | 5.003228 | down |
| global | www.youtube.com | 64.233.162.198 | up | 200 | 0.013571 | 0.033915 | 0.100363 | 0.268371 | http_up |
| global | www.github.com | 140.82.121.4 | up | 301 | 0.060213 | 0.137097 | 0.202785 | 0.202904 | http_up |
| global | www.cloudflare.com | 104.16.123.96 | up | 200 | 0.019650 | 0.070137 | 0.830349 | 1.584302 | http_up |
| global | www.wikipedia.org | 185.15.59.224 | up | 200 | 0.058780 | 0.141640 | 0.297795 | 0.473040 | http_up |
| global | www.microsoft.com | 2.23.90.89 | up | 200 | 0.035243 | 0.104304 | 0.152836 | 0.272756 | http_up |
| global | www.apple.com | 184.24.145.53 | up | 200 | 0.031270 | 0.445762 | 0.488104 | 0.640433 | http_up |
| global | apps.apple.com | 184.24.144.29 | up | 301 | 0.030853 | 0.166231 | 0.204232 | 0.204347 | http_up |
| global | www.samsung.com | 23.32.24.52 | up | 301 | 0.043028 | 0.098154 | 0.165415 | 0.175125 | http_up |
| global | play.google.com | 192.178.25.78 | up | 302 | 0.013769 | 0.038134 | 0.066344 | 0.066389 | http_up |
| global | www.amazon.com | 23.32.25.222 | up | 503 | 0.037936 | 0.084885 | 0.265719 | 0.266186 | http_up |
| global | www.openai.com | 104.18.33.45 | up | 308 | 0.020531 | 0.070045 | 0.111291 | 0.111506 | http_up |
| messaging | telegram.org | 149.154.167.99 | up | 000 | 0.055676 | 0.000000 | 0.000000 | 5.004429 | tcp_only |
| messaging | web.telegram.org | 149.154.167.99 | up | 000 | 0.064426 | 0.000000 | 0.000000 | 5.006859 | tcp_only |
| messaging | api.telegram.org | 149.154.166.110 | up | 000 | 0.065337 | 0.000000 | 0.000000 | 5.006670 | tcp_only |
| ru_portal | ya.ru | 5.255.255.242 | up | 302 | 0.023815 | 0.057797 | 0.082100 | 0.082171 | http_up |
| ru_portal | yandex.ru | 77.88.44.55 | up | 302 | 0.020070 | 0.049212 | 0.074143 | 0.074225 | http_up |
| ru_portal | vk.com | 87.240.132.67 | up | 302 | 0.010372 | 0.027020 | 0.048337 | 0.048392 | http_up |
| ru_portal | mail.ru | 89.221.239.1 | up | 302 | 0.019939 | 0.221755 | 0.252824 | 0.253530 | http_up |
| ru_portal | ok.ru | 95.163.61.74 | up | 200 | 0.018174 | 0.043724 | 0.138800 | 0.279854 | http_up |
| ru_portal | dzen.ru | 5.61.23.39 | up | 404 | 0.018002 | 0.141329 | 0.162748 | 0.162757 | http_up |
| ru_media | rutube.ru | 178.248.233.148 | up | 403 | 0.017369 | 0.044596 | 0.067279 | 0.067422 | http_up |
| ru_media | ria.ru | 194.190.139.47 | up | 200 | 0.019257 | 0.056117 | 0.079615 | 0.182771 | http_up |
| ru_media | lenta.ru | 81.19.72.33 | up | 200 | 0.018117 | 0.061716 | 0.080679 | 0.158831 | http_up |
| ru_media | www.rbc.ru | 178.248.234.119 | up | 200 | 0.016967 | 0.255831 | 0.276319 | 0.276372 | http_up |
| ru_media | www.kommersant.ru | 178.248.238.19 | up | 200 | 0.021019 | 0.045828 | 0.146425 | 0.341429 | http_up |
| ru_commerce | www.ozon.ru | 185.73.193.68 | up | 307 | 0.019431 | 0.048707 | 0.070692 | 0.070842 | http_up |
| ru_commerce | www.wildberries.ru | 185.62.202.2 | up | 498 | 0.021285 | 0.069884 | 0.148322 | 0.148737 | http_up |
| ru_commerce | www.avito.ru | 176.114.120.24 | up | 403 | 0.016532 | 0.070408 | 0.115698 | 0.129833 | http_up |
| ru_gov | www.gosuslugi.ru | 213.59.253.7 | up | 000 | 0.017520 | 0.000000 | 0.000000 | 0.135289 | tls_reset |
| ru_gov | www.nalog.gov.ru | 212.193.146.145 | up | 301 | 0.019489 | 0.051673 | 0.071675 | 0.071924 | http_up |
| ru_gov | www.mos.ru | 94.79.51.169 | up | 000 | 0.021663 | 0.000000 | 0.000000 | 0.079856 | tls_reset |
| ru_bank | www.sberbank.ru | 84.252.149.206 | up | 200 | 0.018121 | 0.051843 | 0.071466 | 0.071619 | http_up |
| ru_bank | www.tbank.ru | 178.130.128.27 | up | 200 | 0.018481 | 0.049476 | 0.369159 | 3.716681 | http_up |
| ru_bank | alfabank.ru | 217.12.104.100 | up | 403 | 0.020276 | 0.050891 | 0.077915 | 0.078009 | http_up |
| ru_bank | www.vtb.ru | 195.242.82.13 | up | 403 | 0.022806 | 0.052066 | 0.075143 | 0.075259 | http_up |

## Summary

- tested IPv4-backed domains: `36`
- TCP 443 reachable: `35`
- HTTP/TLS succeeded with non-zero HTTP code: `30`
- partial cases (TCP only or TLS reset): `5`
- complete failures: `1`

## Working Domains

- `www.youtube.com` -> `200`
- `www.github.com` -> `301`
- `www.cloudflare.com` -> `200`
- `www.wikipedia.org` -> `200`
- `www.microsoft.com` -> `200`
- `www.apple.com` -> `200`
- `apps.apple.com` -> `301`
- `www.samsung.com` -> `301`
- `play.google.com` -> `302`
- `www.amazon.com` -> `503`
- `www.openai.com` -> `308`
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
- `www.sberbank.ru` -> `200`
- `www.tbank.ru` -> `200`
- `alfabank.ru` -> `403`
- `www.vtb.ru` -> `403`

## Partial Domains

- `telegram.org` -> TCP up, HTTPS failed
- `web.telegram.org` -> TCP up, HTTPS failed
- `api.telegram.org` -> TCP up, HTTPS failed
- `www.gosuslugi.ru` -> TLS reset / interrupted after connect
- `www.mos.ru` -> TLS reset / interrupted after connect

## Down Domains

- `google.com`

## Focused YouTube Follow-Up

After the broad matrix run, a narrower `YouTube` family check was run because real user behavior on the phone did not match the optimistic initial matrix row for `www.youtube.com`.

Domains checked through the same `v7 -> Tele2` path:

- `www.youtube.com`
- `m.youtube.com`
- `youtubei.googleapis.com`
- `i.ytimg.com`
- `s.ytimg.com`
- `googlevideo.com`
- `redirector.googlevideo.com`

Observed result:

- all of them failed to connect in the focused follow-up
- no TLS handshake completed
- the media-related `googlevideo` side also failed

Practical correction:

- treat `YouTube` as unstable or effectively non-working on the current `Tele2` path
- the original broad-matrix `www.youtube.com` success should not be trusted as proof that actual YouTube playback is usable

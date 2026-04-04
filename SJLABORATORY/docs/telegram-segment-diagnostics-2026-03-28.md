# Telegram Segment Diagnostics — 2026-03-28

Goal: check whether the direct provider path blocks all Telegram-owned address space or mainly the web entry path.

## Confirmed Telegram prefixes

RIPE lookups for `AS62041` and `AS62014` showed Telegram-owned space including:

- `149.154.164.0/22`
- `149.154.166.0/24`
- `149.154.167.0/24`
- `91.108.4.0/22`
- `91.108.8.0/22`
- `91.108.16.0/22`
- `91.108.56.0/22`
- `95.161.64.0/20`

Representative domain-to-IP mapping at test time:

- `telegram.org` -> `149.154.167.99`
- `t.me` -> `149.154.167.99`
- `web.telegram.org` -> `149.154.167.99`
- `api.telegram.org` -> `149.154.166.110`

## Direct laptop path

TCP `443` from the laptop direct path:

- `149.154.166.110:443` -> reachable
- `149.154.167.99:443` -> reachable
- `91.108.16.1:443` -> reachable
- `91.108.4.1:443` -> timeout
- `91.108.8.1:443` -> timeout
- `91.108.56.1:443` -> timeout
- `95.161.64.1:443` -> connection refused

TLS handshake from the laptop direct path:

- `telegram.org` on `149.154.167.99:443` -> TCP connect succeeds, TLS handshake times out
- `api.telegram.org` on `149.154.166.110:443` -> TCP connect succeeds, TLS handshake times out

## Reference path via Hostkey

From `Hostkey 5.39.219.74`:

- `telegram.org` on `149.154.167.99:443` -> TCP and TLS handshake succeed
- `api.telegram.org` on `149.154.166.110:443` -> TCP and TLS handshake succeed
- `91.108.16.1:443` -> TCP reachable
- `91.108.4.1:443` -> timeout
- `91.108.8.1:443` -> timeout
- `91.108.56.1:443` -> timeout
- `95.161.64.1:443` -> connection refused

## Conclusion

This does not look like a pure DNS block.

This also does not look like a full blackhole of all Telegram-owned space:

- direct TCP to the active web IPs still opens
- at least one non-web Telegram prefix sample (`91.108.16.1:443`) is still directly reachable

The strongest current signal is:

- the direct provider path breaks or stalls the TLS/HTTPS stage specifically for Telegram web entry IPs
- the same TLS handshake succeeds immediately from the reference VPS

## MTProto-oriented checks

Telegram transport documentation describes client transport over ports such as `80`, `443`, and `5222`, and also HTTPS transport endpoints like `pluto.web.telegram.org`, `venus.web.telegram.org`, `aurora.web.telegram.org`, `vesta.web.telegram.org`, and `flora.web.telegram.org`.

Representative results:

- direct laptop path:
  - `149.154.166.110:80` -> reachable
  - `149.154.166.110:443` -> reachable
  - `149.154.166.110:5222` -> timeout
  - `149.154.167.99:80` -> reachable
  - `149.154.167.99:443` -> reachable
  - `149.154.167.99:5222` -> timeout
  - `91.108.16.1:80` -> timeout
  - `91.108.16.1:443` -> reachable
  - `91.108.16.1:5222` -> timeout
- reference `Hostkey` path:
  - same picture for these sampled ports

This means the sampled TCP port behavior itself does not currently prove a local-only MTProto block.

But the HTTPS transport endpoints do show a strong split:

- direct laptop path:
  - `https://pluto.web.telegram.org/api` -> timeout
  - `https://venus.web.telegram.org/api` -> timeout
  - `https://aurora.web.telegram.org/api` -> timeout
  - `https://vesta.web.telegram.org/api` -> timeout
  - `https://flora.web.telegram.org/api` -> timeout
- reference `Hostkey` path:
  - all five endpoints return fast live HTTP responses (`403`), which proves the path is alive and the endpoint is reachable

Working interpretation:

- the direct path appears to break Telegram web transport in addition to `telegram.org` / `api.telegram.org`
- sampled TCP ports alone are not enough to prove that every non-web MTProto route is blocked
- but Telegram's HTTPS transport path is clearly degraded or filtered on the direct provider path

Working hypothesis: the current direct path is degrading or filtering Telegram HTTPS/TLS flows rather than removing every Telegram route globally.

## Tooling update

The direct diagnostics now include Telegram segment probes:

- `ops/diagnose-provider-access-island.sh`
- `ops/diagnose-direct-provider-path.sh`

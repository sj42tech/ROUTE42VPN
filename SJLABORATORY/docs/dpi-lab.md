# DPI Lab

`ops/dpi-lab.sh` is a safe research script for comparing direct path behavior by protocol layer while leaving the current desktop `xray` tunnel untouched.

It is not a bypass tool. It is a measurement tool.

## Safety rule

- Do not stop, restart, unload, or disable `local.xray` while the desktop tunnel is considered working unless the user explicitly requests that action.
- `dpi-lab.sh` only runs direct no-proxy checks from the laptop and optional comparison checks from the reference VPS.

## What the lab checks

The lab tries to answer one simple question:

Where does the first visible failure appear?

It checks the same targets step by step:

1. `DNS`
2. `TCP connect`
3. `TLS handshake`
4. `HTTP response`
5. optional comparison from the reference host `5.39.219.74`

Default targets:

- `www.google.com`
- `github.com`
- `telegram.org`
- `api.telegram.org`
- `pluto.web.telegram.org`

## How to read the layers

### 1. DNS layer

The script resolves A records with `dig`.

What this checks:

- whether the domain name resolves at all
- which IP addresses are returned
- whether the first visible break already happens at the resolver layer

How to read it:

- if DNS fails, the path is already broken before any network connect
- if DNS succeeds, the next step is the raw transport path to the returned IP

Useful resource:

- DNS spec: [RFC 1035](https://www.rfc-editor.org/rfc/rfc1035)

### 2. TCP layer

The script opens a raw TCP socket to port `443` on the resolved IP.

What this checks:

- whether a TCP connection can be established at all
- whether the path to the destination IP and port is reachable

How to read it:

- if TCP fails after DNS succeeds, the first visible problem is below TLS and HTTP
- this usually points at routing failure, ACL filtering, blackhole behavior, or resets or timeouts on the transport path

Useful resource:

- TCP spec: [RFC 9293](https://www.rfc-editor.org/rfc/rfc9293)

### 3. TLS layer

The script performs a TLS handshake with SNI against the resolved IP.

What this checks:

- whether TCP is alive but the encrypted session fails to come up
- whether the path breaks during the handshake metadata stage
- whether a hostname-sensitive TLS step behaves differently from plain TCP

How to read it:

- if TCP succeeds but TLS fails, the first visible divergence is at the handshake stage
- that often points at interference around TLS metadata, including `SNI` or other handshake characteristics

Useful resources:

- TLS 1.3: [RFC 8446](https://www.rfc-editor.org/rfc/rfc8446)
- TLS extensions including SNI: [RFC 6066](https://www.rfc-editor.org/rfc/rfc6066)

### 4. HTTP layer

The script sends a real HTTPS request with `curl` and records:

- `http_code`
- `remote_ip`
- `time_connect`
- `time_appconnect`
- `time_starttransfer`
- `time_total`
- `detail` as `curl exitcode:errormsg`

What this checks:

- whether the endpoint answers as a live application
- whether the connection is slow or times out after TLS
- whether the issue is application-facing rather than handshake-facing

How to read it:

- if TLS succeeds but HTTP fails, the first visible problem is above the TLS handshake
- if HTTP works from the reference VPS but not from the laptop direct path, the difference is likely in the local or provider path rather than the remote service

Useful resource:

- HTTP semantics: [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110)

### 5. Reference host comparison

The script can rerun the same layers from the reference VPS `5.39.219.74`.

What this checks:

- whether the remote service is actually alive
- whether the same hostname and IP behave differently from the laptop direct path

How to read it:

- if the reference host succeeds and the laptop direct path fails, the strongest clue is that the issue is local-path or provider-path specific
- if both fail the same way, the endpoint itself may be down, filtered globally, or not serving that protocol on that IP

Useful resource:

- Telegram datacenter overview: [Working with Different Data Centers](https://core.telegram.org/api/datacenter)

## Run it

Default run:

```bash
bash /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/dpi-lab.sh
```

Force a specific interface:

```bash
TEST_INTERFACE=en0 bash /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/dpi-lab.sh
```

Skip the reference VPS comparison:

```bash
COMPARE_REFERENCE=0 bash /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/dpi-lab.sh
```

Use a different reference host or key:

```bash
REFERENCE_HOST=debian@194.182.174.240 \
REFERENCE_SSH_KEY="$HOME/.ssh/sergei-macbook-vps" \
bash /Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/dpi-lab.sh
```

## Current interpretation pattern for this project

For the current Telegram issue, the important pattern has been:

- DNS works
- TCP to the active Telegram web IP can still work
- TLS or HTTPS then stalls on the direct laptop path
- the same TLS and HTTPS succeed from the reference VPS

That pattern is much more specific than a generic "the internet is down" event.

It means the research focus should stay on the direct path behavior between the laptop and the remote service, not on the health of the remote service alone.

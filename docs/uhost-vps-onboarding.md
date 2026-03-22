# uHost VPS Onboarding Notes

This note starts the `uHost` provider track for the project.

Date of record: `2026-03-22`.

## Current Status

- production stays on `Hostkey 5.39.219.74`
- `Vultr 108.61.171.121` remains a non-working experiment for browser traffic
- `uHost` is the next provider under evaluation

## What uHost Looks Like Right Now

Based on the public site, `uHost` looks closer to a managed hosting / managed cloud provider than a fast self-service VPS console.

Practical implication:

- expect a quote, sales contact, or engineer-assisted provisioning flow
- do not assume an instant “create VPS now” button like Vultr or Hetzner
- confirm the commercial terms before paying

## Commercial Warning

The public `uHost` legal page is not written like a disposable trial VPS offering.

What to watch carefully:

- the agreement text describes an initial term and automatic renewal
- the same page also mentions 30 days prior written notice and a `$50.00` early cancellation fee in some cancellation cases

Practical project rule:

- do not place an order until `uHost` confirms a true month-to-month VPS or cloud VM with no long-term lock-in
- get the cancellation terms in writing before paying

## Recommended First Request To uHost

Ask for the smallest month-to-month Linux cloud VM or VPS that can support one `Xray` endpoint for personal use.

Target request:

- `Ubuntu 24.04 LTS` or `Debian 12`
- `1 vCPU`
- `1 GB` or `2 GB RAM`
- one public IPv4 address
- SSH root or sudo access
- inbound `TCP/443`
- outbound DNS plus standard web access for package installs and connectivity tests
- no control panel
- no managed add-ons unless they are bundled and unavoidable
- monthly billing with no long commitment

## Questions To Ask Before Paying

1. Is this a self-service VM, or is every change done through support?
2. Can they provision a plain Linux VPS with root SSH access?
3. Is inbound `TCP/443` allowed without special approval?
4. Can they provide a location with the best route toward Russia and Europe?
5. Is the plan month-to-month, and what is the cancellation policy?
6. Do they provide console access or a reinstall path if SSH breaks?
7. Are there extra charges for backups, public IPs, or support?

## How To Start With uHost

1. Open the request/quote flow on the `uHost` site. If the page is slow or unavailable, contact `sales@uHost.com` or call `1.888.698.4678`.
2. Ask for the minimal Linux VM described above.
3. State clearly that the workload is a personal self-hosted network endpoint and that you need direct SSH administration.
4. Do not prepay for a long term.
5. Wait for the exact offer and provisioning details.
6. Only after you receive the server IP and login details, run the existing migration and validation steps from this repo.

## Suggested Message

Use this as a starting template:

```text
Hello,

I need the smallest month-to-month Linux cloud VM or VPS with a public IPv4 address for a personal self-hosted service. I need root SSH access, inbound TCP/443, and standard outbound internet access for package installation and updates.

Preferred OS: Ubuntu 24.04 LTS or Debian 12
Preferred size: 1 vCPU, 1-2 GB RAM

Please confirm:
- whether this is self-service or engineer-managed
- monthly price
- cancellation terms
- whether backups, support, or public IPs cost extra
- which location would give the best route toward Russia / Europe

Thank you.
```

## Internal Project Rule For uHost

Until a real `uHost` VM is provisioned and validated:

- do not add `uHost` as a production provider
- keep all existing Vultr test material for comparison
- keep `Hostkey 5.39.219.74` as the working baseline

## Official Sources

- uHost homepage: <https://www.uhost.com/>
- uHost cloud hosting overview: <https://www.uhost.com/solutions-cloud-hosting.php>
- uHost request-a-quote page: <https://www.uhost.com/request-a-quote.php>
- uHost contact details example page: <https://www.uhost.com/hosting-api.php>
- uHost legal terms: <https://www.uhost.com/legal.php>

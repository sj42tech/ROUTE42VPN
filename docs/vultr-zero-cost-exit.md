# Vultr Zero-Cost Exit Plan

This note documents the safest path to stop paying for the non-working Vultr candidate server `108.61.171.121` while keeping all local research material in this repo.

Date of record: `2026-03-22`.

## Goal

Reach an effective `$0` Vultr state for this experiment:

- no active compute instance for `108.61.171.121`
- no paid snapshots left behind
- no accidental reliance on the broken route in daily use
- all local configs and test notes preserved in this repo for future reference

## Current Decision

- keep `Hostkey 5.39.219.74` as the only production server
- keep `Vultr 108.61.171.121` only as an archived experiment
- decommission the Vultr resources if we do not plan another rebuild there

## Before You Destroy Anything

1. Confirm the laptop is already back on the baseline tunnel:
   - `secrets/PROXY/xray/config.json` should point to `5.39.219.74`
   - `/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/ops/rollback-to-baseline.sh` should complete successfully
2. If there is anything on the Vultr server worth keeping, export it over SSH first.
3. Keep the local files in `secrets/PROXY/xray/` exactly as they are. Repo-side configs cost nothing and are the only historical record of the failed Vultr attempt.

## Practical Zero-Cost Exit

1. Log in to the Vultr customer portal.
2. Open `Products` -> `Compute`.
3. Select the instance for `108.61.171.121`.
4. Use `Destroy Server`.
5. Review snapshots and delete any snapshot you do not want to keep.
6. Review whether you intentionally preserved any backups. If not, let retained automatic backups age out or delete converted snapshots manually.
7. Check billing again after the deletion is reflected in the portal.

## Important Billing Notes

- Destroying the instance is the step that stops the Cloud Compute server itself.
- Automatic backups cost extra while the instance exists.
- Vultr keeps the two most recent automatic backups for up to 7 days after instance deletion.
- Snapshots are user-managed and continue to bill until you delete them.

## Optional Final Cleanup

If you want to leave Vultr completely:

1. Destroy all active services.
2. Pay any outstanding balance.
3. Use the account closure flow in the Vultr portal, or open a support ticket for account cancellation.

If you only want `$0` cost, closing the account is optional. The important part is to remove billable resources.

## Repo Policy

Even after destroying the Vultr resources, keep these local files:

- `secrets/PROXY/xray/config.hostkey.json`
- `secrets/PROXY/xray/config.vultr.json`
- `secrets/PROXY/xray/config.vultr-ru-direct.json`
- `secrets/client_config.md`
- `docs/vps-audit-2026-03-22.md`

They remain useful as test artifacts and as a record of what failed.

## Official Sources

- Vultr Cloud Compute delete flow: <https://docs.vultr.com/products/compute/cloud-compute/management/destroy-instance>
- Vultr stopped instance billing note: <https://docs.vultr.com/support/platform/billing/are-stopped-instances-still-billed-on-vultr>
- Vultr automatic backup pricing note: <https://docs.vultr.com/support/platform/billing/how-much-does-it-cost-to-enable-automatic-backups>
- Vultr stored snapshot billing note: <https://docs.vultr.com/support/platform/billing/does-vultr-charge-for-stored-snapshots>
- Vultr snapshot delete flow: <https://docs.vultr.com/products/orchestration/snapshots/management/delete-snapshots>
- Vultr account closure flow: <https://docs.vultr.com/support/platform/profile/can-i-delete-or-close-my-vultr-account>

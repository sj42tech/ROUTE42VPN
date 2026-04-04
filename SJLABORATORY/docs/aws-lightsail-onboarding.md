# AWS Lightsail Onboarding 2026-03-28

This note records the current AWS Lightsail registration and first-launch path for this project.

Current project-specific context:

- `AWS Lightsail` public site was directly reachable from the current network on `2026-03-28`
- that reachability check was performed without stopping or disabling the working local `xray` tunnel
- this makes `AWS Lightsail` a strong next provider candidate for a controlled test
- current reference server-side `Xray` version is `26.2.6` on `Hostkey 5.39.219.74`

## Recommended First Try

Suggested first regions for this project:

- `Seoul`
- `Mumbai`
- `Singapore`

This region order is an engineering recommendation, not an AWS guarantee. The official Lightsail docs say to choose a region close to your physical location for lower latency, but our real acceptance test should still be direct no-proxy reachability plus later Xray canary checks.

## Registration

Official AWS setup flow:

1. Open the AWS sign-up page: <https://portal.aws.amazon.com/billing/signup>
2. Follow the online sign-up flow.
3. Complete phone verification.
4. Wait for the AWS confirmation email.

Official AWS notes:

- AWS says part of the sign-up flow involves a phone call or text message and entering a verification code
- AWS also says the sign-up creates an AWS account root user

## Immediate Security Setup After Sign-Up

Do this before normal day-to-day use:

1. Sign in to the AWS Management Console as `Root user`
2. Enable MFA for the root user
3. Enable `IAM Identity Center`
4. Create an administrative user
5. Use that admin user for normal work instead of using the root user every day

This is AWS's own recommended posture for Lightsail onboarding.

## IAM And API Path

For this project, the safe AWS operating model is:

1. Keep `root` only for account recovery and billing tasks.
2. Enable MFA on `root`.
3. Create a separate IAM administrative user.
4. Create API access keys only for that IAM user.
5. Use those IAM credentials with the AWS CLI for automation.

Project rule:

- do not create long-term API access keys for the `root` user

## CLI-Based Create Path

After the IAM user and access keys are ready, the repo can create a Lightsail VPS through:

```bash
export AWS_REGION='ap-northeast-2'
export AWS_LIGHTSAIL_SSH_KEY_PATH="$HOME/.ssh/id_rsa.pub"
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/aws-lightsail-create-server.sh
```

Recommended first region:

- `ap-northeast-2` (`Seoul`)

Fallback regions:

- `ap-south-1` (`Mumbai`)
- `ap-southeast-1` (`Singapore`)

The automation path will:

- validate AWS credentials
- select an active availability zone
- prefer `Debian 12`
- open `22/tcp` and `443/tcp`
- attach a static IP by default

## Open Lightsail

After the account is ready:

1. Open the Lightsail getting-started page or go directly to the Lightsail console
2. Sign in to the Lightsail console
3. Choose `Create instance`

Useful entry points:

- Lightsail getting started: <https://aws.amazon.com/lightsail/getting-started/>
- Lightsail console: <https://lightsail.aws.amazon.com/>

## First Test Instance

Recommended first safe VM shape for this project:

- platform: `Linux/Unix`
- image type: `OS Only`
- OS: `Debian` or `Ubuntu`
- networking: prefer standard `IPv4` first for compatibility

Recommended first public plans:

- cheap check: `1 GB / 2 vCPU / 40 GB SSD` at `$7/month`
- more realistic check: `2 GB / 2 vCPU / 60 GB SSD` at `$12/month`

Why:

- both plans are still simple enough for a personal Xray endpoint
- the smaller plan is enough for a first connectivity experiment
- the larger one is closer to a practical always-on tunnel test

## Create The Instance

AWS's documented basic flow is:

1. Sign in to the Lightsail console
2. Choose `Create instance`
3. Select an AWS Region and Availability Zone
4. Choose `OS Only` or `Apps + OS`
5. Choose the instance plan
6. Enter an instance name
7. Choose `Create instance`

AWS also notes:

- new AWS accounts created within the past `24 hours` might not yet have access to all services used in Lightsail tutorials
- instances incur usage fees from creation time until you delete them

## First Connection

After creation:

1. Open the instance in the Lightsail console
2. Use `Connect` or `Connect using SSH`
3. Confirm the VM is reachable in the browser-based SSH terminal

If you connect from a local SSH client instead of the console, AWS documents these default usernames:

- `Debian`: `admin`
- `Ubuntu`: `ubuntu`

Only after that should we continue with:

- server hardening
- Xray installation
- local canary tests
- any live tunnel cutover discussion

## Project Safety Rule

When we test `AWS Lightsail` from this repo:

- do not stop or disable `local.xray` unless the user explicitly requests that action
- direct provider reachability should be checked with `ops/diagnose-direct-provider-path.sh`
- server migration to Lightsail should happen through an isolated canary path before any live desktop switch

## Xray Alignment Rule

When Lightsail joins the VPS fleet, it should receive the same server-side `Xray` build as the current reference host:

```bash
/Users/sergeibystrov/PROJECTS/test/VPNCLIENT/SJLABORATORY/ops/migrate-xray-from-old-vps.sh admin@203.0.113.10
```

That migration path now verifies that the target host reports the same `Xray` version as the reference host after the copy.

## Sources

- AWS Lightsail getting started: <https://aws.amazon.com/lightsail/getting-started/>
- Lightsail setup prerequisites and admin-user guidance: <https://docs.aws.amazon.com/lightsail/latest/userguide/setting-up.html>
- Lightsail VPS getting-started flow: <https://docs.aws.amazon.com/lightsail/latest/userguide/getting-started.html>
- Connect to Lightsail Linux or Unix instances with SSH command: <https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-ssh-using-terminal.html>
- AWS CLI `create-instances`: <https://docs.aws.amazon.com/cli/latest/reference/lightsail/create-instances.html>
- AWS CLI `import-key-pair`: <https://docs.aws.amazon.com/cli/latest/reference/lightsail/import-key-pair.html>
- AWS CLI `get-bundles`: <https://docs.aws.amazon.com/cli/latest/reference/lightsail/get-bundles.html>
- Create and attach a static IP in Lightsail: <https://docs.aws.amazon.com/lightsail/latest/userguide/lightsail-create-static-ip.html>
- AWS account setup and root-user security guidance: <https://docs.aws.amazon.com/IAM/latest/UserGuide/getting-started-account-iam.html>
- AWS console sign-in guide: <https://docs.aws.amazon.com/signin/latest/userguide/how-to-sign-in.html>

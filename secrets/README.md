# Local Release Secrets

This directory is for local release-signing inputs only.

Do not commit real keystores, passwords, exported base64 values, or `.env` files.

Use one of these local layouts:

## Option A: Keep the keystore file here

Place your local keystore file at:

- `secrets/route42-release.jks`

Then create these text files:

- `secrets/ROUTE42_KEYSTORE_PASSWORD.txt`
- `secrets/ROUTE42_KEY_ALIAS.txt`
- `secrets/ROUTE42_KEY_PASSWORD.txt`

The helper script will read the keystore file, convert it to single-line base64, and print all four GitHub secret values in order.

## Option B: Keep the base64 value here

If you already exported the keystore to base64, create these text files:

- `secrets/ROUTE42_KEYSTORE_BASE64.txt`
- `secrets/ROUTE42_KEYSTORE_PASSWORD.txt`
- `secrets/ROUTE42_KEY_ALIAS.txt`
- `secrets/ROUTE42_KEY_PASSWORD.txt`

The helper script will read the saved base64 value directly.

## Print Values For GitHub Secrets

Run:

```bash
bash secrets/print-github-secrets.sh
```

The script prints these values in order:

1. `ROUTE42_KEYSTORE_BASE64`
2. `ROUTE42_KEYSTORE_PASSWORD`
3. `ROUTE42_KEY_ALIAS`
4. `ROUTE42_KEY_PASSWORD`

Paste each value into the matching GitHub Actions repository secret.

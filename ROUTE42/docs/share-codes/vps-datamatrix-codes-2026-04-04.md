# Route42 Local Share Code Workflow

This note describes the Route42 camera-import flow without storing live links in tracked app files.

## In-App Flow

To import from another device:

- open Route42 on the target Android device;
- tap `Import`;
- tap `Scan Code`;
- scan a `Data Matrix` or `QR` code that contains a `vless://` link.

## Repository Rule

Live share links and generated share-code images must not be committed.

Keep them in ignored local app storage instead, for example:

- `ROUTE42/secrets/share-links.tsv`
- `ROUTE42/build/share-codes/`

## Local Manifest Format

The generic rendering helper expects a two-column TSV file:

```text
# filename<TAB>vless-link
example-profile-datamatrix.png	vless://11111111-2222-4333-8444-555555555555@203.0.113.10:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=cdn.example&fp=chrome&pbk=AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE&sid=a1b2&type=tcp#example-profile
```

## Rendering Helper

Use [RenderVpsDataMatrix.java](../../tools/RenderVpsDataMatrix.java) with:

- a local manifest path as the first argument;
- an optional output directory as the second argument.

The default output directory is `build/share-codes`.

Generated images should stay in ignored local storage rather than tracked app docs.

## Recommended Local-Only Workflow

1. Maintain live share links in ignored local storage.
2. Render local share codes into ignored `ROUTE42/build/share-codes/`.
3. Use Route42 camera import on the target Android device.
4. Keep any live notes, exported links, and generated images out of tracked docs.

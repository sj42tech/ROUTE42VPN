# Route42 Local Share Code Workflow

This note describes the Route42 camera-import flow without storing live VPS links in the public app layer.

## In-App Flow

To import from another device:

- open Route42 on the target Android device;
- tap `Import`;
- tap `Scan Code`;
- scan a `Data Matrix` or `QR` code that contains a `vless://` link.

## Public Repository Rule

Live share links and generated share-code images must not be committed to `ROUTE42`.

Keep them in ignored local lab storage instead, for example:

- `../SJLABORATORY/secrets/ROUTE42/live-vps-links.tsv`
- `../SJLABORATORY/secrets/ROUTE42/share-codes/`

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

1. Maintain live share links in ignored `../SJLABORATORY/secrets/ROUTE42/live-vps-links.tsv`.
2. Render local share codes into ignored `../SJLABORATORY/secrets/ROUTE42/share-codes/`.
3. Use Route42 camera import on the target Android device.
4. Keep any live notes or exported links in `SJLABORATORY/secrets/ROUTE42/`, not in tracked `ROUTE42` docs.

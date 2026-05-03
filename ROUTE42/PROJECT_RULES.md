# Project Rules

These rules are mandatory for every project file: source code, tests, Markdown docs, assets, config, and examples.

## 1. Project Identity

- Public project name: `Route42`.
- Public repository name: `ROUTE42VPN`.
- Technical namespace: `io.github.sj42tech.route42`.
- Primary custom query prefix for imported links: `x-route42-*`.
- Imported custom routing links must use `x-route42-*` only.
- Public branding, titles, app labels, and docs use `Route42` unless there is a clear reason to mention the technical namespace.
- Personal names, usernames, machine names, private IPs or domains, UUIDs, keys, links to personal servers, and any other real-environment identifiers are not allowed in the repo.
- The project must not mention who worked on the code or private conversations.
- One explicit public project contact method is allowed in the repo for Route42 support, collaboration, or VPS setup inquiries.
- Any allowed public contact must be intentional, stable, and documented as an official project contact, not as private personal information.
- The project must not mention draft origins, external helpers, or internal file preparation history.

## 2. File Structure

- One file should handle one main job.
- Large UI screens should be split into separate screen, component, and navigation files.
- Large services should be split into a main lifecycle file plus helper files for notifications, network logic, intent wrappers, and utilities.
- New docs should live either in the project root or in `docs/` for architecture or product documentation.
- File names should be predictable and match their purpose.

## 3. Size And Cohesion

- UI files should usually stay around ~300 lines; if a screen keeps growing, split it.
- Service and infrastructure files should usually stay around ~400 lines; repeated and side logic should be extracted.
- Functions should stay short and readable; reduce nesting and branching with small helper functions.
- Repeated strings, key lists, and magic numbers should be moved into named constants.

## 4. Kotlin And Android Code

- Prefer immutable data models.
- Represent UI and tunnel state with explicit state models.
- Side effects should live in clear places: `ViewModel`, service layer, and repository layer.
- Keep business rules, parsing, and config generation separate from UI.
- Use comments only when the intent would otherwise be hard to understand.
- Every new API should have a clear name without private shorthand.

## 5. Documentation Rules

- Docs describe architecture and public behavior, not personal development history.
- All examples use anonymized placeholders or documentation test ranges like `203.0.113.0/24`, `2001:db8::/32`, and `example.com`.
- Docs must not publish real keys, UUIDs, servers, SNI values, domains, or any other live credentials.
- The README should stay short and point people to the required project rules.
- All docs, code comments, and user-facing copy must be written in clear English only.

## 6. Local And Generated Files

- The repo must not store `build/`, APKs, temp logs, dumps, debug screenshots, or machine-specific files.
- `local.properties` must not be committed.
- The local build environment should be configured outside the repo with environment variables or uncommitted local files.
- `.gitignore` must cover those files.

## 7. Security And Privacy

- Never commit real secrets or live configs.
- Live VPS tuples, share links, and fleet inventories must stay in ignored local lab files, not in tracked `ROUTE42` sources.
- Every sample profile must be a safe template.
- If a file contains real data, replace it with placeholders or remove it from the project.

## 8. Tests

- Parsers, config generators, and state transformations should have deterministic unit tests.
- Test data must stay anonymized and stable.
- Tests should verify behavior, not random local artifacts.

## 9. Change Rule

- Before adding a new file, decide which project layer it belongs to.
- Before making an existing file larger, first try to extract part of the logic into a separate file.
- Any change that breaks these rules is not done until the project is brought back into compliance.

## 10. Release Readiness

These checks define the release readiness baseline for Route42.

### Required Green Checks

- `./gradlew testDebugUnitTest assembleDebug assembleRelease assembleAndroidTest connectedDebugAndroidTest` must pass before a public release decision.
- The instrumentation smoke test in `app/src/androidTest/java/io/github/sj42tech/route42/MainActivitySmokeTest.kt` must pass on the current emulator setup.
- A signed release build must be verified locally or in CI with `apksigner verify`.

### Required Repository State

- The repository must stay aligned with the current Route42 naming, privacy, and release-safety rules.
- Root screenshots, temporary PNG files, and local source image imports must stay ignored by `.gitignore` unless they are intentionally added as project assets.
- Public docs, branding, and release workflow files must stay aligned with the actual product behavior.

### Required Release Infrastructure

- GitHub signing secrets described in `docs/github-release-signing.md` must be configured before publishing a tagged GitHub Release.
- The GitHub release workflow must build a signed `release` APK, not a debug artifact.

### Residual Risks That Do Not Block A Public Repository

- A clean release candidate can still be pushed publicly even if GitHub release secrets are not configured yet.
- CI coverage may still have gaps, such as not running emulator instrumentation tests in GitHub Actions, as long as those gaps are explicitly understood and local validation has been completed.

### Release Verdict Rule

- Route42 is ready for the first public commit and push when the required green checks and repository state rules are satisfied.
- Route42 is ready for the first tagged GitHub Release only when the signing secrets are configured and the signed release APK path is verified end-to-end.

## 11. Agent Android Workflow

Before starting Android platform, build, emulator, UI, permission, release, or SDK-related work, review [docs/agent-workflow.md](docs/agent-workflow.md).

The required pre-task check is:

- verify whether the official Android CLI is available locally;
- prefer official Android agent guidance and Android Knowledge Base material for Android platform decisions;
- use the project-local Android Skills under `skills/` when they are available for the task area, especially Android CLI, R8, edge-to-edge, AGP, Compose, and navigation work;
- keep the existing Gradle, emulator, and ADB commands as the fallback when Android CLI preview tooling is unavailable;
- treat `VpnService`, foreground service behavior, permissions, target SDK, release signing, and emulator setup as documentation-sensitive areas that require current Android guidance before code changes.

Android CLI is preview tooling. It may be used for local emulator start and APK install workflows, but do not make release CI depend on it until it has been proven stable for Route42.

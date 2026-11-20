# Zodiary project status and next steps

- **Updated:** 2026-11-20
- **Stage:** Android frontend prototype with Supabase client integration prepared; hosted backend deployment is pending
- **Production readiness:** Not ready; hosted backend deployment, release decisions, and full device QA remain.

## Project overview

Zodiary is a personal journaling app that uses zodiac readings as a prompt for reflection. The journal is the main experience; readings are general entertainment content, not personalized predictions or professional advice.

The app is native Android, built with Kotlin and Jetpack Compose; it is not an Expo app. Root `npm run dev` serves a separate, sample-only browser mirror under `web-preview/` for localhost screen browsing. The product and architecture baseline is in [project.md](project.md), the visual guidance is in [design.md](design.md), and screen references are in [screens/](screens/).

## What is implemented

### Android frontend

- Single-activity Compose app with welcome, birthday/sign setup, sign reveal, preferences, trust note, Home, Journal, Calendar, Profile, and reading detail destinations.
- Ten debug-only `@Preview` functions cover the onboarding and main destination screens, using fixture state behind a read-only screen model so previews do not initialize app services.
- VS Code workspace settings and tasks use the existing JBR and Android SDK, cap Gradle at a 2 GB heap/two workers, and disable the Gradle daemon for one-shot tasks.
- A no-dependency Node server serves a browser mirror at `http://localhost:4173` when started with `npm.cmd run dev`; it previews sample layouts only and does not execute the Compose app.
- Sign selection and birthday-based sign lookup; basic interests and journal-focus preferences.
- Journal prompts selected from the current interests, prompt cycling, mood selection, draft retention, and saved entries.
- Calendar history for saved entries and a Profile view for sign and preference changes.
- Profile supports optional email/password account creation and sign-in. Guest mode remains local-first, with a local copy retained after sync.
- Daily affirmation card and saved reminder preference. Reminder notifications are not active.

### CosmyDay daily reading

- A typed adapter parses CosmyDay's public `/content/daily` all-sign response, normalizes sign names, checks for all 12 readings, and keeps provider content as plain text.
- The Home and reading-detail screens show the reading date separately from fetch time and link the required “Astrology data by CosmyDay” attribution to [cosmyday.com](https://cosmyday.com).
- Moon details are shown only when present in the daily response; the app does not make a separate moon request.
- The all-sign response is cached locally and reused when the selected sign changes. Refresh uses bounded timeouts and retry behavior, keeps the last valid cached batch on a failed or incomplete response, and avoids replacing a newer cached edition with an older one.
- When Supabase settings are present, the client reads the shared `daily_readings` batch. Without those settings, it uses the original direct CosmyDay development source.
- The live all-sign and single-sign response shapes were inspected on 2026-11-18. Runtime use currently calls only the all-sign endpoint. CosmyDay's current API documentation is at [cosmyday.com/api-docs](https://cosmyday.com/api-docs), with the [OpenAPI spec](https://cosmyday.com/openapi.json).

### Local data and project setup

- Sign, interests, reminder choice, journal drafts, moods, entries, and the reading cache are stored on-device in `SharedPreferences`.
- Account sync writes user-owned entries and preferences through authenticated Supabase requests. Entry conflicts merge by the latest `updated_at` timestamp.
- The project targets Android API 36, compiles against API 37, and supports Android API 26 and newer.
- Gradle wrapper and Android Studio project files are included. After a debug build, the APK is at `app/build/outputs/apk/debug/app-debug.apk`.

### Supabase files and setup

- `supabase/migrations/20261118092602_initial_zodiary_schema.sql` defines shared readings, user preferences, and journal tables with explicit grants and RLS policies.
- `supabase/functions/daily-cosmyday-import/` contains a bounded, validated server-side CosmyDay importer that rejects incomplete or older batches.
- `supabase/scripts/schedule_daily_import.sql` schedules the importer using a secret API key held in Vault.
- Root `.env` and `.env.*` files are ignored. Gradle reads only the project URL and publishable key; no secret key is embedded in the app.
- Email confirmation returns through the `zodiary://auth` deep link; add that callback to the hosted project's Auth redirect allowlist.
- The supplied project API responds, but the connected Supabase management account could not inspect or deploy to project `mxxzczkvcbhlmdupwbbu`. The migration and importer are local files and have not been applied to the hosted project.

## Verification and preview

- `:app:testDebugUnitTest` passed: 16 tests covering response parsing, sign normalization, reading dates, cache policy, invalid responses, and refresh failures.
- `:app:assembleDebug` completed successfully.
- The VS Code Compose Preview extension discovered the annotated functions, but rendering aborted with `Failed to find required symbol ANativeWindow_fromSurface!`; no static preview image has been verified on this machine.
- Google's Android CLI preview renderer was blocked by local Windows App Control when it attempted to load its runtime. This setup did not launch Android Studio or an emulator.
- The localhost browser mirror served its HTML, JavaScript, and CSS successfully from the root `npm.cmd run dev` script. It contains sample-only interactions and remains separate from Android runtime verification.
- Deno 2.9.6 `deno check --lock=deno.lock index.ts` passed for the daily CosmyDay importer.
- Earlier runtime check (before this VS Code setup): the app reached the welcome screen in the API 37 emulator, but Android System UI became unresponsive and ADB disconnected. Full interactive QA remains outstanding. No emulator was launched during this VS Code setup.

To build and verify from PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Open the repository root in VS Code. **Terminal → Run Task** provides `Android: Build debug APK`, `Android: Run unit tests`, and `Android: Install debug app on connected device`. The install task runs `:app:installDebug` only against a device already visible to ADB; it does not start an emulator. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

For static screens, open the Compose Preview view and run **Compose Preview: Render All Previews** or **Refresh Previews**. The workspace selects the extension's low-memory `minimal` mode, which renders on demand. Static previews do not exercise navigation or app services. Rendering is currently blocked by the native symbol error above; retry visual review only after the renderer reports success.

## Current limitations

- The hosted Supabase schema, importer function, and schedule have not been deployed. Shared readings and account sync need the correct Supabase management access and a completed migration.
- Password recovery, account deletion/export, privacy copy, and data retention decisions are not implemented.
- Account synchronization runs when the app opens, after sign-in, after a saved entry or preference change, or when the user taps Sync; there is no durable background/offline retry queue.
- Reminders are saved as a preference but do not schedule notifications. Reading feedback and affirmation save/share are not implemented.
- Interactive runtime QA is incomplete because the earlier API 37 emulator became unstable. A full navigation/touch-flow check requires an ADB-connected Android device; the VS Code static preview workflow does not start one.
- VS Code static previews are configured but currently cannot render due to the `ANativeWindow_fromSurface` native symbol failure. The official Android CLI is blocked by local App Control policy. The localhost browser mirror provides a separate verified screen preview, but it is not rendered from Compose.
- The browser mirror is available for layout browsing, but it is not a faithful Compose renderer. Kotlin UI changes must also be reflected in the separate `web-preview/` files to appear there.
- Weekly/monthly readings and event or transit screens are not implemented; the current integration and UI cover daily readings only.
- App signing, privacy disclosures, support contact, provider commercial-use/traffic expectations, and release configuration remain to be decided before distribution.

## Recommended next steps

1. **Deploy and inspect Supabase.** Connect the management tooling to the account that owns the configured project, inspect existing tables, then apply the migration and deploy the importer. Confirm the `zodiary://auth` redirect in Supabase Auth settings.
2. **Enable and validate the daily job.** Add the project URL and a secret API key to Vault, confirm CosmyDay's date rollover and commercial-use expectations, then run the cron SQL and verify all 12 readings appear through the Data API.
3. **Finish interactive QA if needed.** Use an Android device that is already connected to ADB to walk through guest onboarding, account sign-in, entry sync, daily reading, offline/cache/error states, and Calendar/Profile. Static previews do not require that device, and this VS Code workflow does not start an emulator.
4. **Strengthen local persistence and synchronization.** Move structured entries and cached readings to Room and settings to DataStore; add migrations and a durable sync/retry model as cloud sync is rolled out.
5. **Complete MVP flows.** Add password recovery, account deletion/export, reminder scheduling, reading feedback, affirmation save/share, and remaining empty/loading/error states. Confirm prompt eligibility and that reading-inspired labels reflect real matches.
6. **Prepare a release candidate.** Review accessibility and privacy requirements, set app/support contact details, confirm CosmyDay attribution and commercial terms, configure signing and release builds, and run a final device test pass.

## Documentation map

- [README.md](README.md) — short project introduction and Android setup.
- [project.md](project.md) — product scope, architecture, data and privacy recommendations, and open decisions.
- [design.md](design.md) — screen behavior, copy, visual system, and design gaps.
- [supabase/README.md](supabase/README.md) - client configuration, schema, function, scheduling, and deployment notes.
- [screens/](screens/) — supplied visual references.

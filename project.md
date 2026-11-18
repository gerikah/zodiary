# Zodiary project specification

- **Status:** Product and architecture baseline; Android frontend and initial CosmyDay daily-reading integration implemented
- **Prepared:** 2026-11-18
- **Platform target:** Native Android app, built later in Android Studio
- **Product version:** MVP with a staged V2 and V3 roadmap

## 1. Executive summary

Zodiary is a personal journal that uses zodiac readings, prompts, moods, and affirmations to make daily reflection easier. The user’s own writing remains the product’s core. Astrology is a reflective lens, not a claim about what must happen.

The supplied screens establish the visual direction and onboarding outline. The v3 copy/data-flow brief is the latest working draft for the MVP behavior, attribution, privacy constraints, and backend content pipeline; it explicitly marks some items for confirmation. The v2 brand library is the approved brand baseline. This document treats v3 refinements as recommendations where they differ from v2 and labels decisions that still need product confirmation.

### Recommended architecture in one sentence

Build a **native Kotlin Android app using Jetpack Compose and a layered, offline-first architecture**: Compose screens render immutable state from screen-level ViewModels; repositories mediate all data access; Room is the local source of truth for entries and cached content; DataStore holds small preferences; a Zodiary backend handles account sync and cached readings; and WorkManager retries durable background sync work.

This matches Android’s current guidance for layered apps, repositories, Compose, state flowing down and events flowing up, and a local source of truth for offline-first data. Keep the domain layer and Gradle module count modest until real reuse or team size warrants more structure. Official references are listed in [Architecture references](#architecture-references).

~~~mermaid
flowchart LR
    UI[Compose screens] --> VM[Screen ViewModels]
    VM --> Repo[Repositories and product rules]
    Repo --> Room[(Room local source of truth)]
    Repo --> API[Zodiary API]
    DS[DataStore preferences] --> Repo
    WM[WorkManager sync retries] --> API
    API --> DB[(Account data and cached readings)]
    Job[Daily content import job] --> Adapter[CosmyDay adapter]
    Adapter --> Provider[CosmyDay API]
    Adapter --> DB
~~~

## 2. Product definition

### Product promise

- Discover your sign.
- Reflect on your days.
- Write your own story.

### Core loop

1. Read a short daily reading for the user’s sign.
2. Offer a prompt based on the user’s preferences, sign/element, or a reading theme when a real match exists.
3. Let the user record a mood and write an entry.
4. Ask whether the reading fit after the user has had time to reflect.
5. Make the reading, mood, feedback, and entry available in calendar history.

### Product principles

- The sign is a lens; the journal is the user’s evidence and story.
- Readings are entertainment and personal reflection, not professional, scientific, medical, financial, or psychological advice.
- Personal reflection stays editable and separate from fixed zodiac descriptions.
- The interface celebrates returning and writing without shaming missed days.
- Data-source limitations must be reflected in UI claims and fallback behavior.

## 3. Scope and phasing

### MVP

- Welcome, sign setup and reveal, user preferences, trust note, registration/login, and password recovery.
- Daily reading for all 12 signs, provided to the app through the Zodiary backend with CosmyDay attribution.
- Preference-, sign-, and element-based journal prompts; reading-theme matching only when implemented and truthful.
- Daily mood check-in and journal entries, with local draft preservation.
- Sign-linked affirmations and save/share actions.
- Reading feedback: “Right on”, “Sort of”, or “Not really”.
- Calendar history for saved reflections.
- Editable sign and preferences, account settings, and optional reminders.
- Cached content and honest reading/sync failure states.

### V2

- Trait resonance responses on the zodiac profile.
- Larger-scale reading-matched prompts, monthly mood summaries, seasonal and birthday recaps.
- Expanded astrology calendar content such as cosmic highlights, only when connected to a real reading or sky event and never presented as guaranteed or “unlucky”.
- Habit tracking if it adds user value without guilt-based streak language.

### V3

- Optional zodiac mascots and richer seasonal experiences.

### Out of MVP

- Moon/transit articles and upcoming sky events unless separately approved.
- Forecasting claims, deterministic “lucky/unlucky” promises, or medical/financial/relationship prescriptions.
- Treating sign-trait feedback as diagnosis or inferred personality truth.

## 4. User journeys and screen inventory

### First-time journey

1. Splash and welcome.
2. Select birthday and calculate a sign.
3. Review the sign reveal and allow a correction.
4. Select exploration topics, journal focus, and optional reminder timing.
5. Read the astrology trust note.
6. Create an account or sign in before cloud sync.
7. Enter Home and start the first journal page.

**Account-gating proposal:** Collect non-sensitive personalization first, then ask the user to create or access an account before cloud-backed entry sync. A local draft can remain available while sign-up is pending. Decide explicitly whether a guest can continue journaling without an account; the MVP privacy and sync model depends on this.

### Returning journey

1. Restore the authenticated session.
2. Load cached Home and journal data immediately.
3. Refresh today’s reading through the Zodiary API.
4. Let the user write, revisit the calendar, or update preferences.

### Navigation

The references show four signed-in destinations: Home, Journal, Calendar, and Profile. Use a single-activity Compose app with destination navigation and a bottom bar for those four sections. Reading detail and day detail are reached from Home and Calendar, respectively. Keep onboarding and authentication outside the signed-in bottom navigation.

See [design.md](design.md) for visual tokens, copy treatment, and screen behavior.

## 5. Android architecture recommendation

### App layers

| Layer | Responsibility | Recommended shape |
|---|---|---|
| UI | Render screen state; send user events | Jetpack Compose destinations and reusable components; screen-level ViewModels expose immutable UI state and event handlers. |
| Domain (selective) | Reusable product rules | Add small use cases when rules are reused or too complex for one ViewModel, such as sign lookup, prompt selection, or saving an entry. Do not create a use-case class for every repository call. |
| Data | Business rules and all data access | Repositories for readings, prompts, entries, moods/feedback, profile/preferences, and affirmations. Data sources are local Room, DataStore, and the Zodiary API. |
| Backend | Authenticated account data and provider integration | Zodiary API/database plus a scheduled content-import job. The Android app never calls CosmyDay directly. |

Use unidirectional data flow: UI events go up to a ViewModel or state holder; immutable state flows back down to the UI. Let repositories own source selection and synchronization rather than calling a database or network client directly from a composable.

### Android implementation choices

| Concern | Recommendation | Why |
|---|---|---|
| Language and UI | Kotlin + Jetpack Compose | Native Android and the modern UI toolkit recommended in Android architecture guidance. |
| App shape | One Activity with Compose navigation | Fits the screens and keeps app entry simple. |
| Screen state | ViewModel + StateFlow / lifecycle-aware collection | Keeps business state out of composables and survives ordinary configuration changes. |
| Structured local data | Room | Entries, reading cache, prompts, feedback, and pending sync state need queries, relations, and migrations. |
| Small preferences | DataStore | Suitable for reminder choice, onboarding state, and lightweight local preferences. |
| Async/background work | Kotlin coroutines and Flow; WorkManager for persistent queued sync | Coroutines/Flow handle app data operations; WorkManager can retry durable work after connectivity returns. Do not use a client timer to run the once-daily provider import. |
| Network | A typed HTTP client behind a ZodiaryApi interface and repository | Keeps provider/backend changes out of screens and allows local fake implementations. Select the HTTP/auth SDK after choosing the backend. |
| Dependency injection | Constructor injection; use a DI container such as Hilt if the dependency graph warrants it | Keep dependencies explicit and replaceable. Avoid creating global service locators in feature code. |
| Gradle structure | Start with one app module organized by feature and layer; split into Gradle modules when build time, ownership, or reuse justify it | Avoid premature module overhead while keeping boundaries visible. |

Do not pin Android Gradle Plugin, Kotlin, Compose, or library versions in this documentation. Select mutually compatible stable releases in Android Studio when implementation begins.

### Suggested package organization

Start in one Android module, with package boundaries that can later become modules:

- app: application entry, dependency setup, navigation.
- core/designsystem: theme, tokens, reusable controls.
- core/model: shared domain-facing models and enums.
- data/local: Room database, DAOs, and local sources.
- data/remote: API contracts, DTOs, and remote sources.
- data/repository: implementations that coordinate local and remote data.
- feature/onboarding, feature/auth, feature/home, feature/reading, feature/journal, feature/calendar, feature/profile, feature/affirmations.

Keep Android framework types near the app/UI boundary. The core data and product rules should be straightforward to construct with fakes in future tests.

## 6. Offline-first behavior and synchronization

Journaling should not lose text when a connection drops. The local database is the app’s immediate source for entries and cached readings; the backend is the authenticated account’s sync destination.

### Read path

1. Screens observe repository data backed by Room.
2. The app renders cached values immediately.
3. A user opening Home may trigger a refresh request to the Zodiary API.
4. A valid response is stored locally, then the observed UI updates.
5. If refresh fails, keep cached content and show the matching stale/unavailable message from the copy library.

### Write path

1. Save the draft locally as the user types, with debouncing and safe cancellation behavior.
2. On Save, persist the entry and a pending sync operation locally before reporting that the entry is kept.
3. Queue sync using an idempotent operation identifier; retry with backoff when connectivity returns.
4. Mark the entry synced only after the backend acknowledges it.
5. If sync fails, preserve the local entry and show “We couldn’t sync your entry. Your draft is still here.” only when the app can truthfully retain and retry it.

### Sync contract requirements

- Writes must be idempotent so retries do not create duplicate entries.
- Each entry has a stable ID, owner ID, local diary date, created/updated timestamps, and sync state held locally.
- Preserve the diary’s local calendar date and the IANA timezone used when the entry was created; do not let travel or timezone conversion silently move a past entry to another day.
- Define how edits from two devices are reconciled. For MVP, prefer a version check and conflict state that preserves both copies over silent text overwrite. Exact UI for conflict resolution remains an open decision.
- Do not claim end-to-end encryption. This proposal is account-based synchronization with server-accessible content, protected by access controls and encryption in transit/at rest.

## 7. Backend and content architecture

### Trust boundary

**Production target:** The Android client talks only to the Zodiary backend. A scheduled server-side adapter fetches CosmyDay content, applies shared caching and validation, and serves it to the app. No provider secret or privileged backend credential belongs in the app.

**Current standalone frontend:** This repository has no Zodiary backend, serverless function, or shared database yet. The initial integration therefore calls CosmyDay's public no-key daily endpoint through a typed service and caches the complete twelve-sign response on-device. Move the fetch to the backend when its host is selected so users share one provider refresh. The app identifies itself with a `Zodiary-Android` User-Agent; no project URL or support contact is configured yet.

### Daily reading import

1. A backend scheduled job requests /content/daily from CosmyDay once per day for all 12 signs, with an identifying User-Agent and contact address.
2. Validate success, presence of all signs, non-empty text, sane length, and no unresolved template tokens.
3. Reject a bad batch and retain the previous valid readings.
4. Store each valid reading with provider, sign, provider date when supplied, fetched time, text, and status.
5. Optionally assign one to three theme tags from a fixed vocabulary. If tagging fails, omit tags and continue.
6. Serve a cached reading and attribution object to the app. The UI must render the provider credit wherever that reading is shown.
7. Monitor provider health and alert on repeated failures. Avoid polling or per-user provider calls.

Put CosmyDay behind a provider adapter/interface so another source or curated fallback can replace it without changes to Android screens. The `/content/daily` and `/content/daily/{sign}` response shapes were inspected against live responses on 2026-11-18; the current adapter supports the all-sign response and preserves provider text and dates. Confirm provider rollover semantics and any commercial-use or traffic requirements before public release.

### Reading fallback order

1. Today’s cached reading.
2. Most recent cached reading up to about 36 hours old, clearly labeled “Today’s reading is on its way. Here’s your latest one for now.”
3. Optional evergreen note for the sign, only if approved and authored by Zodiary.
4. No reading: “We couldn’t load today’s reading. Your journal is still here.” Prompts, mood tracking, and journaling continue.

### Prompt selection

Use this priority order:

1. Prompt whose themes genuinely match the reading and the user’s preferences.
2. Prompt from a selected preference pool.
3. Prompt from the user’s sign or element pool.
4. General fallback.

Do not show prompts already presented in the previous 30 days. If this must apply across devices, the backend needs to record prompt impressions; local-only history can only enforce it per device. The product needs to choose the MVP source of truth. The user can cycle through prompts at the current priority before moving to the next fallback level.

### Attribution and content safety

- Credit CosmyDay visibly and link to cosmyday.com wherever its content appears, including any share image.
- Do not attribute CosmyDay data to Zodiary or claim it is written by astrologers.
- Lucky number/color/time can be shown only when present in provider data or supplied by Zodiary. If curated, label the section “Traditional associations”. Otherwise omit it.
- Avoid hard predictions about health, money, or relationships. Define a review/filtering policy before shipping provider text.
- Keep disclaimers visible on onboarding, reading detail, and About.

## 8. Data model baseline

The model below is a starting point based on the v3 data-flow document. Add fields only when tied to a user-facing requirement.

| Entity | Core fields | Notes |
|---|---|---|
| Profile | user_id, display_name, sign, timezone_id, optional birth_date | Prefer storing the calculated sign rather than retaining a full birth date unless birthday features need it. |
| Preferences | user_id, topic selections, journal focus, reminder choice | Preferences must change prompt eligibility or notification behavior. |
| Reading | id, provider, sign, content/provider date, text, theme tags, fetched time, status | Preserve source and date; include provider name and URL in the API response. |
| Prompt | id, text, element/sign/preference, theme tags, active state | Prompt labels are based on actual selection source. |
| Prompt impression | user_id, prompt_id, shown_at | Needed to enforce a cross-device 30-day exclusion. Can be deferred only if copy/behavior is scoped to one device. |
| Entry | id, user_id, local diary date, timezone, mood, text, prompt ID, reading ID, created/updated times | Owner scoping is mandatory. Local Room also tracks draft/sync state. |
| Reading feedback | id, user_id, reading_id, value, timestamp | Values: right_on, sort_of, not_really; associate with reading, not profile. |
| Affirmation | id, text, sign, active/source metadata | User save/share state may be separate from content. |
| Event (V2) | id, date, kind, headline, summary, importance, ET time, cached time | Do not include in MVP. Convert event time to the user’s timezone before display. |

Mood names, whether multiple moods can be selected, and whether entries support multiple saves per day remain product decisions. Do not hard-code assumptions from the mockup placeholders.

## 9. Privacy, security, and trust

### Recommended MVP privacy model

Use **account-private server sync** for the MVP, with per-user access rules enforced by the backend data layer. Cache user-owned records locally in Room. This supports login and multi-device continuity without claiming cryptographic privacy the product does not provide.

This is not end-to-end encryption: the service can technically access entry plaintext. Use the copy “Your journal is private to your account” only after owner isolation and backend access rules are implemented and reviewed. Do not use “Your entries stay private to you” or “only you can access your entries” unless data is on-device only or end-to-end encrypted.

### Required safeguards

- Require authenticated ownership for every server-side entry, preference, and feedback operation.
- Apply row-level/per-user authorization at the data layer, not only in app UI.
- Use TLS for network traffic and the backend’s supported encryption-at-rest controls.
- Never store raw passwords in Zodiary data. Use the selected authentication provider’s session flow.
- Keep privileged keys on the server. The Android app may contain only the public client configuration required by its backend integration.
- Minimize personal data. A birthday picker can calculate a sun sign locally; store the sign by default and retain the full date only if a later feature needs it and the user is told why.
- Do not send journal text or mood values to analytics by default. Operational telemetry should exclude private content and credentials.
- Notifications should not contain reading content or sensitive journal information.
- Document account deletion, export, retention, and backup behavior before public release.

## 10. Content and copy baseline

Use the v2 PDF as the approved brand baseline. For the proposed MVP, use the later v3 wording and behavior as recommendations where it is more specific; confirm changes from v2 before release.

- Brand: “Dear universe, it’s me.” / “The stars inspire. You write.” / “Your personal cosmic journal.”
- App reading label: “Today’s reading”; use “horoscope” only in store/search copy.
- Affirmation label: “Words to carry today.”
- Journal placeholder: “Dear universe, today I…”
- Reading feedback: “Did today’s reading fit?” with “Right on”, “Sort of”, “Not really”.
- Trust statement: “The stars offer a perspective; you decide what resonates.”
- Avoid repetitive use of “little”, “cosmic”, “stars”, and “inspired”.
- Keep button labels functional: “Save entry”, “Share affirmation”, “Sign in”, “Create account”.

## 11. Nonfunctional requirements

- **Durability:** A saved entry must remain locally available through app restart and transient network loss.
- **Offline behavior:** Core journal history and cached content must render without a network connection. Show unavailable/stale states honestly.
- **Privacy:** Every account-owned record is scoped to the authenticated owner; logs do not expose entry text.
- **Performance:** Show cached content before waiting for remote refresh. Avoid blocking the main thread.
- **Accessibility:** Support screen readers, font scaling, keyboard use, adequate touch targets, and contrast.
- **Time correctness:** Distinguish provider content date, user-local diary date, UTC timestamps, and timezone display.
- **Maintainability:** Keep provider and backend implementation behind interfaces and keep UI screens free from persistence/network details.
- **Content reliability:** Reject incomplete or invalid 12-sign batches without replacing the last good data.

## 12. Acceptance criteria for MVP readiness

- A user can complete onboarding, change a sign, set preferences, and reach Home.
- Every preference selection has a visible effect on prompt eligibility.
- The app displays daily content with source attribution and the required disclaimer.
- A user can write, save, reopen, edit, and revisit entries by local diary date.
- Draft and sync states are truthful; text survives an offline save and retry path before offline copy ships.
- Reading fallback states preserve journaling functionality.
- Feedback is stored against the reading and does not interrupt the first read.
- Calendar history links to the correct reading, mood, feedback, and reflection.
- Backend authorization prevents one account from reading another account’s journal.
- Account deletion/export, reminder permission behavior, and privacy copy are documented before public launch.

## 13. Open decisions and recommended defaults

| Decision | Recommended default | Why it remains open |
|---|---|---|
| Entry privacy/storage | Account-private sync with Room cache; no E2EE claim | Product copy and threat model must match server access. |
| Account gate / guest mode | Account required for cloud sync; retain local draft while signing up | Guest writing changes data ownership and recovery expectations. |
| Backend host/auth provider | Keep API provider-neutral until selected | The PDFs specify a Zodiary backend but not its host or auth contract. |
| CosmyDay commercial attribution | Confirm format and expected traffic with provider | The content brief explicitly requests confirmation. |
| CosmyDay response shape | Live all-sign and single-sign responses inspected; adapter covers the all-sign batch | Continue checking provider changes; the public OpenAPI spec does not fully describe response fields. |
| Reading rollover/date | Use provider date if present; confirm timezone and rollover first | Users in different zones can otherwise see inconsistent “today” readings. |
| Birthday retention and sign rules | Calculate locally; store sign; allow manual correction | Full birth date is personal data; date-only input has cusp limitations. |
| Prompt 30-day history | Backend records prompts shown for cross-device enforcement | The rule applies to prompts shown, not only saved entries. |
| Prompt tagging | Start with preference/sign/element pools; add deterministic theme tags after validation | Reading match must be real and visible label must be accurate. |
| Evergreen fallback | No at initial MVP unless 12 reviewed notes are authored | Avoid presenting stale or invented content as today’s reading. |
| Lucky details | Omit unless response includes values; curated values need “Traditional associations” | Provider field availability is not verified. |
| Entry conflict policy | Version check; preserve conflicting text for user resolution | Multi-device edits must not silently overwrite journal content. |
| Mood taxonomy | Agree a small accessible, non-diagnostic set | The PDFs do not define exact mood options. |
| Authentication fields | Email/password baseline; confirm username, OAuth, and verification flow | Mockups show username and social buttons; v3 copy does not establish them. |
| Reminders | Optional, local-time scheduling with opt-in permission flow | Notification platform, scheduling behavior, and permission timing need definition. |
| Generated reading content policy | Do not rewrite provider content until review rules are approved | The source brief flags strong predictions as a content policy decision. |

## 14. Architecture references

These current Android Developers references support the recommended structure:

- [Guide to app architecture](https://developer.android.com/topic/architecture) — UI/data layers, separation of responsibility, data persistence, and maintainability.
- [Recommendations for Android architecture](https://developer.android.com/topic/architecture/recommendations) — repositories, Compose, coroutines/Flow, and when to add a domain layer.
- [Compose UI architecture](https://developer.android.com/develop/ui/compose/architecture) — unidirectional data flow, events, state holders, and ViewModels.
- [Data layer](https://developer.android.com/topic/architecture/data-layer) — repositories as the app’s data entry point and a single source of truth.
- [Build an offline-first app](https://developer.android.com/topic/architecture/data-layer/offline-first) — local reads, local source of truth, queued writes, and sync considerations.
- [Save data in a local database using Room](https://developer.android.com/training/data-storage/room) — structured local persistence and offline cache guidance.
- [Guide to app modularization](https://developer.android.com/topic/modularization) — when and how to introduce Gradle module boundaries.

## 15. Project source materials

- [README.md](README.md) — initial project summary and feature list.
- [Screens folder](screens/) — supplied 390 × 840/844 visual references.
- [Zodiary v3 Copy Library & Content Data Flow.pdf](<Zodiary v3 Copy Library & Content Data Flow.pdf>) — latest proposed copy, MVP, backend flow, data model, and open decisions.
- [Zodiary Brand Messaging Revised v2.pdf](<Zodiary_Brand_Messaging_Revised_v2 (1).pdf>) — earlier approved brand and UX copy context.

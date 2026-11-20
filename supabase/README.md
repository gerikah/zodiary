# Supabase setup

The app uses Supabase for the shared daily-reading cache and optional account sync. Guest journaling stays on-device. The app embeds only the project URL and publishable key; never put a Supabase secret key in `.env` or Android configuration.

## Local Android configuration

Create a root `.env` file with:

```dotenv
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
```

Gradle also accepts the `NEXT_PUBLIC_` aliases. `.env` and `.env.*` are ignored by Git. Gradle reads only the project URL and publishable key to generate Android `BuildConfig` fields; it ignores any secret key and JWKS setting. `.env.example` is a blank template.

The account screen supports email/password sign-up and sign-in. For email confirmation, add `zodiary://auth` to **Authentication → URL Configuration → Redirect URLs** in the hosted project's dashboard. The app keeps local journal entries if sign-in, confirmation, or sync fails.

## Schema and importer deployment

The migration creates:

- `daily_readings`: the current public twelve-sign batch; anonymous users can only read it.
- `journal_entries`: private, owner-scoped diary rows with row-level security.
- `user_preferences`: private, owner-scoped zodiac and journal settings.

Before a hosted schema change, verify that the connected Supabase project is the project from the configured URL and inspect its existing tables. The Supabase management connection used while preparing these files could reach the project API but could not inspect or manage project `mxxzczkvcbhlmdupwbbu`; do not push migrations to another project by mistake.

Once the right Supabase account/project is connected and existing schema is reviewed:

```powershell
supabase link --project-ref mxxzczkvcbhlmdupwbbu
supabase db push
supabase functions deploy daily-cosmyday-import
```

The importer accepts only the project's secret API key in the `apikey` header. The mobile publishable key cannot trigger provider refreshes. It validates the full provider response before upserting, and rejects a batch older than the current cache.

## Daily import schedule

In the hosted project's **Vault**, add these secrets:

- `zodiary_project_url`: the hosted Supabase project URL.
- `zodiary_cron_api_key`: an `sb_secret_...` API key for that project.

Then run [`scripts/schedule_daily_import.sql`](scripts/schedule_daily_import.sql) in the hosted SQL Editor. It installs `pg_cron` and `pg_net` if needed and schedules the import at 04:00 UTC (12:00 Manila). Confirm the provider's date rollover first and adjust the schedule if needed. The SQL file contains no credential values.

## Client behavior and rollout

When both client settings exist, the app reads `daily_readings` through the Data API. If settings are absent, it retains the earlier direct CosmyDay development source. Once Supabase is configured, an API error is shown with the last valid local reading retained; the app does not silently bypass the shared cache.

After account sign-in, app open, or a saved change, local and cloud entries are merged by diary date. The entry with the later `updated_at` wins; exact timestamp ties keep the cloud copy. Preferences use the same last-write-wins rule. A failed sync leaves all local data in place and can be retried from Profile.

The Android journal and reading cache still use `SharedPreferences`; Room/DataStore migrations, account deletion/export, device QA, and final production release setup remain follow-up work.

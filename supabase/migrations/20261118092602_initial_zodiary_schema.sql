-- Shared daily provider content is public; user-owned journal data is private.
-- Explicit grants are required because new Supabase projects no longer expose
-- public tables to the Data API by default.

create table public.daily_readings (
  sign_slug text primary key check (
    sign_slug = any (array[
      'aries', 'taurus', 'gemini', 'cancer', 'leo', 'virgo',
      'libra', 'scorpio', 'sagittarius', 'capricorn', 'aquarius', 'pisces'
    ]::text[])
  ),
  sign_name text not null,
  provider_date date not null,
  weekday text,
  generated_at timestamptz,
  fetched_at timestamptz not null default now(),
  content text not null check (char_length(btrim(content)) between 1 and 12000),
  closing text,
  moon jsonb,
  updated_at timestamptz not null default now(),
  constraint daily_readings_moon_object check (moon is null or jsonb_typeof(moon) = 'object')
);

alter table public.daily_readings enable row level security;
revoke all on table public.daily_readings from anon, authenticated;
grant select on table public.daily_readings to anon, authenticated;
grant select, insert, update on table public.daily_readings to service_role;

create policy "Daily readings are publicly readable"
  on public.daily_readings
  for select
  to anon, authenticated
  using (true);

create table public.journal_entries (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  diary_date date not null,
  mood text not null default '' check (
    mood in ('', 'Calm', 'Hopeful', 'Tender', 'Restless', 'Heavy')
  ),
  entry_text text not null check (
    char_length(btrim(entry_text)) between 1 and 50000
  ),
  prompt text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, diary_date)
);

alter table public.journal_entries enable row level security;
revoke all on table public.journal_entries from anon, authenticated;
grant select, insert, update, delete on table public.journal_entries to authenticated;

create policy "Users can read their own journal entries"
  on public.journal_entries
  for select
  to authenticated
  using ((select auth.uid()) = user_id);

create policy "Users can create their own journal entries"
  on public.journal_entries
  for insert
  to authenticated
  with check ((select auth.uid()) = user_id);

create policy "Users can update their own journal entries"
  on public.journal_entries
  for update
  to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

create policy "Users can delete their own journal entries"
  on public.journal_entries
  for delete
  to authenticated
  using ((select auth.uid()) = user_id);

create table public.user_preferences (
  user_id uuid primary key references auth.users (id) on delete cascade,
  sign_slug text not null check (
    sign_slug = any (array[
      'aries', 'taurus', 'gemini', 'cancer', 'leo', 'virgo',
      'libra', 'scorpio', 'sagittarius', 'capricorn', 'aquarius', 'pisces'
    ]::text[])
  ),
  topics text[] not null default '{}'::text[],
  journal_focus text not null default 'Reflect' check (
    journal_focus in ('Reflect', 'Track moods', 'Keep memories', 'A bit of everything')
  ),
  reminder text not null default 'Not now' check (
    reminder in ('Morning', 'Evening', 'Not now')
  ),
  updated_at timestamptz not null default now()
);

alter table public.user_preferences enable row level security;
revoke all on table public.user_preferences from anon, authenticated;
grant select, insert, update, delete on table public.user_preferences to authenticated;

create policy "Users can read their own preferences"
  on public.user_preferences
  for select
  to authenticated
  using ((select auth.uid()) = user_id);

create policy "Users can create their own preferences"
  on public.user_preferences
  for insert
  to authenticated
  with check ((select auth.uid()) = user_id);

create policy "Users can update their own preferences"
  on public.user_preferences
  for update
  to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

create policy "Users can delete their own preferences"
  on public.user_preferences
  for delete
  to authenticated
  using ((select auth.uid()) = user_id);

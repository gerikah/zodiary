-- Run once in the hosted project's SQL Editor after pg_cron and pg_net are enabled.
-- Add zodiary_project_url and zodiary_cron_api_key in Supabase Vault first.
-- The second secret must be an sb_secret_... key, not the mobile publishable key.

create extension if not exists pg_cron;
create extension if not exists pg_net with schema extensions;

select cron.schedule(
  'zodiary-cosmyday-daily',
  '0 4 * * *', -- 04:00 UTC / 12:00 Manila; confirm CosmyDay's date rollover before enabling.
  $$
  select net.http_post(
    url := (
      select decrypted_secret
      from vault.decrypted_secrets
      where name = 'zodiary_project_url'
    ) || '/functions/v1/daily-cosmyday-import',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'apikey', (
        select decrypted_secret
        from vault.decrypted_secrets
        where name = 'zodiary_cron_api_key'
      )
    ),
    body := '{}'::jsonb
  ) as request_id;
  $$
);

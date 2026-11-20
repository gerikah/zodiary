import { withSupabase } from "@supabase/server";

const PROVIDER_URL = "https://api.cosmyday.com/content/daily";
const USER_AGENT = "Zodiary-Supabase/0.1.0";
const MAX_ATTEMPTS = 2;
const MAX_RESPONSE_BYTES = 1_000_000;
const MAX_CONTENT_CHARS = 12_000;
const SIGNS = [
  "aries", "taurus", "gemini", "cancer", "leo", "virgo",
  "libra", "scorpio", "sagittarius", "capricorn", "aquarius", "pisces",
] as const;

type JsonRecord = Record<string, unknown>;
type ReadingRow = {
  sign_slug: string;
  sign_name: string;
  provider_date: string;
  weekday: string | null;
  generated_at: string | null;
  fetched_at: string;
  content: string;
  closing: string | null;
  moon: JsonRecord | null;
};

class ProviderHttpError extends Error {
  constructor(readonly status: number) {
    super(`CosmyDay returned HTTP ${status}`);
  }
}

export default {
  // CLI auth mode "apikey" disables the gateway's JWT-only check. This handler
  // then accepts only a Supabase secret key, which the database cron job sends
  // in the apikey header from Vault. The publishable mobile key cannot invoke it.
  fetch: withSupabase({ auth: ["secret"] }, async (request, context) => {
    if (request.method !== "POST") {
      return Response.json({ error: "Method not allowed" }, { status: 405 });
    }

    if (context.authMode !== "secret") {
      return Response.json({ error: "Unauthorized" }, { status: 401 });
    }

    try {
      const payload = await fetchDailyPayload();
      const rows = adaptDailyPayload(payload, new Date().toISOString());
      const { data: existing, error: readError } = await context.supabaseAdmin
        .from("daily_readings")
        .select("sign_slug,provider_date,generated_at");

      if (readError) throw new Error(`Reading cache check failed (${readError.code ?? "unknown"})`);

      const existingBySlug = new Map(
        (existing ?? []).map((row: {
          sign_slug: string;
          provider_date: string;
          generated_at: string | null;
        }) => [row.sign_slug, row] as const),
      );
      const olderRows = rows
        .filter((row) => isOlderThanExisting(row, existingBySlug.get(row.sign_slug)))
        .map((row) => row.sign_slug);

      if (olderRows.length > 0) {
        return Response.json({
          status: "older_provider_edition_skipped",
          provider_date: rows[0].provider_date,
          skipped_signs: olderRows,
        });
      }

      const { error: writeError } = await context.supabaseAdmin
        .from("daily_readings")
        .upsert(rows, { onConflict: "sign_slug" });

      if (writeError) throw new Error(`Reading cache write failed (${writeError.code ?? "unknown"})`);

      return Response.json({
        status: "updated",
        provider_date: rows[0].provider_date,
        generated_at: rows[0].generated_at,
        fetched_at: rows[0].fetched_at,
        readings: rows.length,
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : "Unknown import error";
      console.error(`Daily CosmyDay import failed: ${message}`);
      return Response.json({ error: "Daily reading import failed" }, { status: 502 });
    }
  }),
};

async function fetchDailyPayload(): Promise<JsonRecord> {
  let lastError: unknown = new Error("CosmyDay request failed");

  for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt += 1) {
    try {
      const response = await fetch(PROVIDER_URL, {
        method: "GET",
        headers: {
          Accept: "application/json",
          "User-Agent": USER_AGENT,
        },
        signal: AbortSignal.timeout(12_000),
      });

      if (!response.ok) {
        if (response.status < 500 || attempt === MAX_ATTEMPTS) {
          throw new ProviderHttpError(response.status);
        }
        throw new ProviderHttpError(response.status);
      }

      const raw = await readBoundedBody(response);
      const parsed: unknown = JSON.parse(raw);
      const root = asRecord(parsed);
      if (!root) throw new Error("CosmyDay response was not a JSON object");
      return root;
    } catch (error) {
      lastError = error;
      if (error instanceof ProviderHttpError && error.status < 500) throw error;
      if (attempt < MAX_ATTEMPTS) await delay(500);
    }
  }

  throw lastError instanceof Error ? lastError : new Error("CosmyDay request failed");
}

async function readBoundedBody(response: Response): Promise<string> {
  if (!response.body) return "";
  const reader = response.body.getReader();
  const chunks: Uint8Array[] = [];
  let byteCount = 0;

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    byteCount += value.byteLength;
    if (byteCount > MAX_RESPONSE_BYTES) {
      await reader.cancel();
      throw new Error("CosmyDay response exceeded the size limit");
    }
    chunks.push(value);
  }

  const bytes = new Uint8Array(byteCount);
  let offset = 0;
  for (const chunk of chunks) {
    bytes.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return new TextDecoder().decode(bytes);
}

function adaptDailyPayload(root: JsonRecord, fetchedAt: string): ReadingRow[] {
  const horoscopes = asRecord(root.horoscopes);
  if (!horoscopes) throw new Error("CosmyDay response did not contain horoscopes");

  const sharedDate = parseProviderDate(root.date);
  const sharedGeneratedAt = parseTimestamp(root.generated_at);
  const sky = asRecord(root.sky);
  const sharedMoon = normalizeMoon(sky?.moon);
  const rowsBySlug = new Map<string, ReadingRow>();

  for (const [responseSign, value] of Object.entries(horoscopes)) {
    const slug = normalizeSign(responseSign);
    if (!slug) continue;

    const item = asRecord(value);
    if (!item) throw new Error(`CosmyDay entry for ${slug} was not an object`);
    const itemSlug = item.sign === undefined ? slug : normalizeSign(item.sign);
    if (itemSlug !== slug) throw new Error(`CosmyDay sign did not match key ${responseSign}`);

    const content = typeof item.content === "string" ? item.content : "";
    if (content.trim().length === 0 || content.length > MAX_CONTENT_CHARS) {
      throw new Error(`CosmyDay content for ${slug} was empty or too long`);
    }

    const providerDate = parseProviderDate(item.date) ?? sharedDate;
    if (!providerDate) throw new Error(`CosmyDay date for ${slug} was missing or invalid`);
    const generatedAt = parseTimestamp(item.generated_at) ?? sharedGeneratedAt;
    const closing = typeof item.closing === "string" && item.closing.trim().length > 0
      ? item.closing
      : null;
    const weekdayValue = item.weekday ?? root.weekday;

    rowsBySlug.set(slug, {
      sign_slug: slug,
      sign_name: typeof item.sign === "string" ? item.sign : titleCase(slug),
      provider_date: providerDate,
      weekday: typeof weekdayValue === "string" ? weekdayValue : null,
      generated_at: generatedAt,
      fetched_at: fetchedAt,
      content,
      closing,
      moon: normalizeMoon(item.moon) ?? sharedMoon,
    });
  }

  const missing = SIGNS.filter((sign) => !rowsBySlug.has(sign));
  if (missing.length > 0) throw new Error(`CosmyDay batch was incomplete (${missing.length} signs missing)`);

  const rows = SIGNS.map((sign) => rowsBySlug.get(sign)!);
  if (new Set(rows.map((row) => row.provider_date)).size !== 1) {
    throw new Error("CosmyDay batch contained mixed provider dates");
  }
  return rows;
}

function isOlderThanExisting(
  incoming: ReadingRow,
  existing: { provider_date: string; generated_at: string | null } | undefined,
): boolean {
  if (!existing) return false;
  if (incoming.provider_date < existing.provider_date) return true;
  if (incoming.provider_date > existing.provider_date) return false;
  if (incoming.generated_at && existing.generated_at) {
    return Date.parse(incoming.generated_at) < Date.parse(existing.generated_at);
  }
  return false;
}

function normalizeSign(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const normalized = value.trim().toLowerCase();
  return SIGNS.includes(normalized as (typeof SIGNS)[number]) ? normalized : null;
}

function parseProviderDate(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const date = value.trim();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) return null;
  const parsed = new Date(`${date}T00:00:00.000Z`);
  return Number.isNaN(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== date ? null : date;
}

function parseTimestamp(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const milliseconds = Date.parse(value);
  return Number.isNaN(milliseconds) ? null : new Date(milliseconds).toISOString();
}

function normalizeMoon(value: unknown): JsonRecord | null {
  const moon = asRecord(value);
  if (!moon) return null;
  const normalized: JsonRecord = {};
  for (const key of ["sign", "phase", "phase_name"] as const) {
    if (typeof moon[key] === "string" && moon[key].trim().length > 0) normalized[key] = moon[key];
  }
  const illumination = moon.illumination;
  if (typeof illumination === "number" && Number.isFinite(illumination) && illumination >= 0 && illumination <= 100) {
    normalized.illumination = illumination;
  }
  return Object.keys(normalized).length > 0 ? normalized : null;
}

function asRecord(value: unknown): JsonRecord | null {
  return typeof value === "object" && value !== null && !Array.isArray(value)
    ? value as JsonRecord
    : null;
}

function titleCase(value: string): string {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function delay(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

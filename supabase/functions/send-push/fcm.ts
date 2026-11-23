// FCM HTTP v1 helpers: message building, error classification, the OAuth2 service-account
// flow and the sender with retries. Network goes through an injectable `fetch` and there is no
// environment access here, so everything runs under `deno test`.

export const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
export const DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";

/** The fields of `public.notifications` the push needs. */
export interface NotificationRecord {
  id: string;
  user_id: string;
  title: string;
  body: string | null;
  report_id?: string | null;
}

/** The fields of the Firebase service-account JSON this function uses. */
export interface ServiceAccount {
  project_id: string;
  client_email: string;
  private_key: string;
  token_uri: string;
}

/**
 * Data-only message (no `notification` block) with high priority, as documented in
 * `supabase/README.md`: the app builds the notification itself. Every data value is a string.
 */
export function buildFcmMessage(token: string, record: NotificationRecord) {
  const data: Record<string, string> = {
    title: String(record.title),
    body: String(record.body ?? ""),
  };
  if (record.report_id) data.report_id = String(record.report_id);
  return {
    message: {
      token,
      android: { priority: "high" },
      data,
    },
  };
}

export function fcmSendUrl(projectId: string): string {
  return `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/messages:send`;
}

/**
 * True when FCM says the token can never receive pushes again, so its row should be deleted:
 * an `FcmError` detail with `errorCode` `UNREGISTERED` (app uninstalled, token deleted at
 * sign-out), or `INVALID_ARGUMENT` that points at the registration token (a `message.token`
 * field violation, or an error message about the registration token). A bare 404 `NOT_FOUND`
 * is not enough: a wrong project id returns it too, and deleting on it would wipe every token.
 * Other `INVALID_ARGUMENT` errors mean our payload is wrong, so they keep the token as well.
 */
export function isUnregisteredError(body: unknown): boolean {
  const error = fcmErrorOf(body);
  if (!error) return false;
  const details = Array.isArray(error.details) ? error.details as Record<string, unknown>[] : [];
  const codes = details.map((d) => d?.errorCode).filter((c) => typeof c === "string");
  if (codes.includes("UNREGISTERED")) return true;
  const invalidArgument = codes.includes("INVALID_ARGUMENT") || error.status === "INVALID_ARGUMENT";
  if (!invalidArgument) return false;
  const tokenViolation = details.some((d) =>
    Array.isArray(d?.fieldViolations) &&
    (d.fieldViolations as Record<string, unknown>[]).some((v) => v?.field === "message.token")
  );
  const message = typeof error.message === "string" ? error.message : "";
  return tokenViolation || /registration token/i.test(message);
}

/**
 * True when FCM refused the request because of our setup, not the token: 401/403 (credentials,
 * sender mismatch) or a 404 that is not `UNREGISTERED` (wrong project id). Every token fails
 * the same way then, so the handler reports it as a server error.
 */
export function isConfigError(status: number, body: unknown): boolean {
  if (status === 401 || status === 403) return true;
  return status === 404 && !isUnregisteredError(body);
}

function fcmErrorOf(body: unknown): Record<string, unknown> | null {
  if (typeof body !== "object" || body === null) return null;
  const error = (body as { error?: unknown }).error;
  return typeof error === "object" && error !== null ? error as Record<string, unknown> : null;
}

/** A broken `FCM_SERVICE_ACCOUNT` or a refused OAuth grant: no send succeeds until it is fixed. */
export class PushConfigError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "PushConfigError";
  }
}

/** Parses `FCM_SERVICE_ACCOUNT`; the error never echoes the input, since it holds a private key. */
export function parseServiceAccount(raw: string | undefined): ServiceAccount {
  if (!raw) throw new PushConfigError("FCM_SERVICE_ACCOUNT is not set");
  let json: unknown;
  try {
    json = JSON.parse(raw);
  } catch {
    throw new PushConfigError("FCM_SERVICE_ACCOUNT is not valid JSON");
  }
  if (typeof json !== "object" || json === null || Array.isArray(json)) {
    throw new PushConfigError("FCM_SERVICE_ACCOUNT is not a JSON object");
  }
  const fields = json as Record<string, unknown>;
  for (const field of ["project_id", "client_email", "private_key"]) {
    if (typeof fields[field] !== "string" || fields[field] === "") {
      throw new PushConfigError(`FCM_SERVICE_ACCOUNT is missing ${field}`);
    }
  }
  return {
    project_id: fields.project_id as string,
    client_email: fields.client_email as string,
    private_key: fields.private_key as string,
    token_uri: typeof fields.token_uri === "string" ? fields.token_uri : DEFAULT_TOKEN_URI,
  };
}

export function base64UrlEncode(bytes: Uint8Array): string {
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToDer(pem: string): Uint8Array {
  // JSON.parse already turns the key's `\n` escapes into newlines, but a secret that was escaped
  // twice before pasting (dashboard, env file) keeps them as the two characters `\` and `n`,
  // which are not base64. Both forms are removed so either paste works.
  const base64 = pem
    .replace(/-----BEGIN [A-Z ]+-----/, "")
    .replace(/-----END [A-Z ]+-----/, "")
    .replace(/\\n/g, "")
    .replace(/\s+/g, "");
  return Uint8Array.from(atob(base64), (c) => c.charCodeAt(0));
}

/** Signs the RS256 JWT assertion Google's token endpoint exchanges for an access token. */
export async function signServiceAccountJwt(account: ServiceAccount, nowSeconds: number): Promise<string> {
  const encoder = new TextEncoder();
  const header = base64UrlEncode(encoder.encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const claims = base64UrlEncode(encoder.encode(JSON.stringify({
    iss: account.client_email,
    scope: FCM_SCOPE,
    aud: account.token_uri,
    iat: nowSeconds,
    exp: nowSeconds + 3600,
  })));
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToDer(account.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const unsigned = `${header}.${claims}`;
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, encoder.encode(unsigned));
  return `${unsigned}.${base64UrlEncode(new Uint8Array(signature))}`;
}

/** Where the sender gets its OAuth2 access token; `AccessTokenCache` in production. */
export interface AccessTokenSource {
  get(): Promise<string>;
  /** Drops `token` after FCM rejected it (401), so the next `get()` fetches a fresh one. */
  invalidate(token: string): void;
}

/** Leaves room for one FCM request inside the webhook's 5 s timeout. */
export const OAUTH_TIMEOUT_MS = 1_500;

/**
 * Caches the OAuth2 access token until 5 minutes before it expires. Concurrent sends share one
 * in-flight token request instead of each signing and fetching their own. `fetchFn` and `now`
 * are injectable so the cache is testable without network.
 */
export class AccessTokenCache implements AccessTokenSource {
  private token: string | null = null;
  private expiresAtMs = 0;
  private inFlight: Promise<string> | null = null;
  private readonly account: ServiceAccount;
  private readonly fetchFn: typeof fetch;
  private readonly now: () => number;
  private readonly timeoutMs: number;

  constructor(
    account: ServiceAccount,
    fetchFn: typeof fetch = (input, init) => fetch(input, init),
    now: () => number = Date.now,
    timeoutMs: number = OAUTH_TIMEOUT_MS,
  ) {
    this.account = account;
    this.fetchFn = fetchFn;
    this.now = now;
    this.timeoutMs = timeoutMs;
  }

  get(): Promise<string> {
    if (this.token && this.now() < this.expiresAtMs - 5 * 60_000) return Promise.resolve(this.token);
    if (!this.inFlight) {
      this.inFlight = this.fetchToken().finally(() => {
        this.inFlight = null;
      });
    }
    return this.inFlight;
  }

  invalidate(token: string): void {
    // Only the rejected token: a concurrent send may already have fetched a fresh one.
    if (this.token === token) {
      this.token = null;
      this.expiresAtMs = 0;
    }
  }

  private async fetchToken(): Promise<string> {
    let assertion: string;
    try {
      assertion = await signServiceAccountJwt(this.account, Math.floor(this.now() / 1000));
    } catch {
      // The decoding or import error could quote key material, so it is not forwarded.
      throw new PushConfigError("FCM_SERVICE_ACCOUNT private_key cannot be used to sign");
    }
    const response = await this.fetchFn(this.account.token_uri, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion,
      }),
      signal: AbortSignal.timeout(this.timeoutMs),
    });
    if (!response.ok) {
      // Google's error body names the problem (invalid_grant, ...) without echoing the key.
      const message = `OAuth token request failed (${response.status}): ${await response.text()}`;
      // 4xx is our account (revoked key, wrong client email); 5xx is Google's and may pass.
      throw response.status < 500 ? new PushConfigError(message) : new Error(message);
    }
    const json = await response.json() as { access_token?: string; expires_in?: number };
    if (!json.access_token) throw new Error("OAuth token response has no access_token");
    this.token = json.access_token;
    this.expiresAtMs = this.now() + (json.expires_in ?? 3600) * 1000;
    return this.token;
  }
}

/** FCM statuses worth retrying: quota exceeded (429), internal (500) and unavailable (503). */
export const RETRYABLE_STATUSES: ReadonlySet<number> = new Set([429, 500, 503]);

/** `Retry-After` (seconds or HTTP date) in milliseconds, or null when absent or invalid. */
export function parseRetryAfterMs(value: string | null, nowMs: number): number | null {
  const trimmed = value?.trim() ?? "";
  if (trimmed === "") return null;
  if (/^\d+$/.test(trimmed)) return Number(trimmed) * 1000;
  const date = Date.parse(trimmed);
  return Number.isNaN(date) ? null : Math.max(0, date - nowMs);
}

export interface FcmSendOptions {
  projectId: string;
  tokens: AccessTokenSource;
  fetchFn?: typeof fetch;
  now?: () => number;
  sleep?: (ms: number) => Promise<void>;
  /** Time one send may spend on FCM, retries included; the rest of 5 s is for the database. */
  budgetMs?: number;
  /** Upper bound for each FCM request. */
  requestTimeoutMs?: number;
  maxRetries?: number;
}

/** No new attempt starts with less time than this left in the budget. */
const MIN_ATTEMPT_MS = 500;

/**
 * Builds the function that POSTs one FCM v1 message. It retries 429/500/503 up to `maxRetries`
 * times with exponential backoff (or the `Retry-After` delay) while the budget allows, and on a
 * 401 drops the cached access token and retries once with a fresh one. Every request has a
 * timeout, so a send finishes before the Database Webhook gives up at 5 s.
 */
export function createFcmSend(
  options: FcmSendOptions,
): (message: unknown) => Promise<{ status: number; body: unknown }> {
  const fetchFn: typeof fetch = options.fetchFn ?? ((input, init) => fetch(input, init));
  const now: () => number = options.now ?? Date.now;
  const sleep: (ms: number) => Promise<void> = options.sleep ??
    ((ms) => new Promise<void>((resolve) => setTimeout(resolve, ms)));
  const budgetMs = options.budgetMs ?? 3_000;
  const requestTimeoutMs = options.requestTimeoutMs ?? 2_000;
  const maxRetries = options.maxRetries ?? 2;
  const url = fcmSendUrl(options.projectId);

  return async (message) => {
    const deadline = now() + budgetMs;
    let refreshed = false;
    let retries = 0;
    while (true) {
      const accessToken = await options.tokens.get();
      const remaining = Math.max(MIN_ATTEMPT_MS, deadline - now());
      const response = await fetchFn(url, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${accessToken}` },
        body: JSON.stringify(message),
        signal: AbortSignal.timeout(Math.min(requestTimeoutMs, remaining)),
      });
      const body = await response.json().catch(() => null);
      if (response.status === 401 && !refreshed && deadline - now() >= MIN_ATTEMPT_MS) {
        refreshed = true;
        options.tokens.invalidate(accessToken);
        continue;
      }
      if (RETRYABLE_STATUSES.has(response.status) && retries < maxRetries) {
        const delay = parseRetryAfterMs(response.headers.get("retry-after"), now()) ?? 250 * 2 ** retries;
        if (now() + delay + MIN_ATTEMPT_MS <= deadline) {
          retries++;
          await sleep(delay);
          continue;
        }
      }
      return { status: response.status, body };
    }
  };
}

/** Constant-time comparison for the webhook secret. */
export function secretsMatch(given: string | null, expected: string): boolean {
  if (given === null) return false;
  const a = new TextEncoder().encode(given);
  const b = new TextEncoder().encode(expected);
  let diff = a.length ^ b.length;
  for (let i = 0; i < Math.max(a.length, b.length); i++) diff |= (a[i] ?? 0) ^ (b[i] ?? 0);
  return diff === 0;
}

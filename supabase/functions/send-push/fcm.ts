// Pure FCM HTTP v1 helpers: message building, error classification and the OAuth2
// service-account flow. No network or environment access here, so they run under `deno test`.

export const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
export const DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";

/** The fields of `public.notifications` the push needs. */
export interface NotificationRecord {
  id: string;
  user_id: string;
  title: string;
  body: string;
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
 * `UNREGISTERED` (app uninstalled, token deleted at sign-out), or `INVALID_ARGUMENT` that names
 * the token itself. Other `INVALID_ARGUMENT` errors mean our payload is wrong; deleting on those
 * would wipe valid tokens.
 */
export function isUnregisteredError(status: number, body: unknown): boolean {
  const error = (body as { error?: Record<string, unknown> } | null)?.error;
  if (!error) return false;
  const details = Array.isArray(error.details) ? error.details as Record<string, unknown>[] : [];
  const codes = details.map((d) => d?.errorCode).filter((c) => typeof c === "string");
  if (codes.includes("UNREGISTERED")) return true;
  if (status === 404 && error.status === "NOT_FOUND") return true;
  const invalidArgument = codes.includes("INVALID_ARGUMENT") || error.status === "INVALID_ARGUMENT";
  if (!invalidArgument) return false;
  const tokenViolation = details.some((d) =>
    Array.isArray(d?.fieldViolations) &&
    (d.fieldViolations as Record<string, unknown>[]).some((v) => v?.field === "message.token")
  );
  const message = typeof error.message === "string" ? error.message : "";
  return tokenViolation || /registration token/i.test(message);
}

/** Parses `FCM_SERVICE_ACCOUNT`; the error never echoes the input, since it holds a private key. */
export function parseServiceAccount(raw: string | undefined): ServiceAccount {
  if (!raw) throw new Error("FCM_SERVICE_ACCOUNT is not set");
  let json: Record<string, unknown>;
  try {
    json = JSON.parse(raw);
  } catch {
    throw new Error("FCM_SERVICE_ACCOUNT is not valid JSON");
  }
  for (const field of ["project_id", "client_email", "private_key"]) {
    if (typeof json[field] !== "string" || json[field] === "") {
      throw new Error(`FCM_SERVICE_ACCOUNT is missing ${field}`);
    }
  }
  return {
    project_id: json.project_id as string,
    client_email: json.client_email as string,
    private_key: json.private_key as string,
    token_uri: typeof json.token_uri === "string" ? json.token_uri : DEFAULT_TOKEN_URI,
  };
}

export function base64UrlEncode(bytes: Uint8Array): string {
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToDer(pem: string): Uint8Array {
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

/**
 * Caches the OAuth2 access token until 5 minutes before it expires. `fetchFn` and `now` are
 * injectable so the cache is testable without network.
 */
export class AccessTokenCache {
  private token: string | null = null;
  private expiresAtMs = 0;
  private readonly account: ServiceAccount;
  private readonly fetchFn: typeof fetch;
  private readonly now: () => number;

  constructor(account: ServiceAccount, fetchFn: typeof fetch = (input, init) => fetch(input, init), now: () => number = Date.now) {
    this.account = account;
    this.fetchFn = fetchFn;
    this.now = now;
  }

  async get(): Promise<string> {
    if (this.token && this.now() < this.expiresAtMs - 5 * 60_000) return this.token;
    const assertion = await signServiceAccountJwt(this.account, Math.floor(this.now() / 1000));
    const response = await this.fetchFn(this.account.token_uri, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion,
      }),
    });
    if (!response.ok) {
      // Google's error body names the problem (invalid_grant, ...) without echoing the key.
      throw new Error(`OAuth token request failed (${response.status}): ${await response.text()}`);
    }
    const json = await response.json() as { access_token?: string; expires_in?: number };
    if (!json.access_token) throw new Error("OAuth token response has no access_token");
    this.token = json.access_token;
    this.expiresAtMs = this.now() + (json.expires_in ?? 3600) * 1000;
    return this.token;
  }
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

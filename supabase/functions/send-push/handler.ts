import {
  buildFcmMessage,
  isConfigError,
  isUnregisteredError,
  type NotificationRecord,
  PushConfigError,
  secretsMatch,
} from "./fcm.ts";

/** Everything the handler touches outside itself; `index.ts` wires the real ones. */
export interface PushDeps {
  webhookSecret: string | undefined;
  /** `notification_prefs.push_enabled`; no row means the default (true). */
  pushEnabled(userId: string): Promise<boolean>;
  deviceTokens(userId: string): Promise<string[]>;
  deleteToken(userId: string, token: string): Promise<void>;
  /**
   * POSTs one FCM v1 message (retries and token refresh included); returns the HTTP status and
   * the parsed JSON body. Throws `PushConfigError` when the service account cannot be used.
   */
  send(message: unknown): Promise<{ status: number; body: unknown }>;
  log(message: string): void;
}

type Outcome = "sent" | "deleted" | "failed" | "config";

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

/** Only the token's tail is logged, never the whole token. */
function tokenTail(token: string): string {
  return `…${token.slice(-6)}`;
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

const optionalString = (value: unknown) => value === undefined || value === null || typeof value === "string";

/** The webhook's `record` with the fields the push needs, or null when one is missing or mistyped. */
function notificationRecord(value: unknown): NotificationRecord | null {
  if (!isObject(value)) return null;
  const { id, user_id, title, body, report_id } = value;
  if (typeof id !== "string" || id === "") return null;
  if (typeof user_id !== "string" || user_id === "") return null;
  if (typeof title !== "string" || title === "") return null;
  if (!optionalString(body) || !optionalString(report_id)) return null;
  return { id, user_id, title, body: (body as string | null | undefined) ?? null, report_id: report_id as string | null | undefined };
}

/** Handles one Database Webhook call: `{ type: 'INSERT', table: 'notifications', record }`. */
export async function handlePush(req: Request, deps: PushDeps): Promise<Response> {
  if (!deps.webhookSecret) {
    deps.log("PUSH_WEBHOOK_SECRET is not set; refusing every request");
    return json(500, { error: "not configured" });
  }
  if (!secretsMatch(req.headers.get("x-webhook-secret"), deps.webhookSecret)) {
    return json(401, { error: "unauthorized" });
  }
  if (req.method !== "POST") return json(405, { error: "method not allowed" });

  let payload: unknown;
  try {
    payload = await req.json();
  } catch {
    return json(400, { error: "invalid JSON" });
  }
  if (!isObject(payload)) return json(400, { error: "body must be a JSON object" });
  if (payload.type !== "INSERT" || (payload.table && payload.table !== "notifications")) {
    return json(200, { skipped: "not a notifications insert" });
  }
  const record = notificationRecord(payload.record);
  if (!record) return json(400, { error: "record needs id, user_id and title" });

  const userId = record.user_id;
  if (!(await deps.pushEnabled(userId))) {
    deps.log(`notification ${record.id}: push disabled by the user's preferences`);
    return json(200, { skipped: "push disabled", sent: 0 });
  }
  const tokens = await deps.deviceTokens(userId);
  if (tokens.length === 0) return json(200, { skipped: "no device tokens", sent: 0 });

  const results = await Promise.all(tokens.map(async (token): Promise<Outcome> => {
    try {
      const { status, body } = await deps.send(buildFcmMessage(token, record));
      if (status >= 200 && status < 300) return "sent";
      if (isUnregisteredError(body)) {
        await deps.deleteToken(userId, token);
        deps.log(`notification ${record.id}: deleted unregistered token ${tokenTail(token)}`);
        return "deleted";
      }
      deps.log(`notification ${record.id}: FCM ${status} for token ${tokenTail(token)}: ${JSON.stringify(body)}`);
      return isConfigError(status, body) ? "config" : "failed";
    } catch (e) {
      deps.log(`notification ${record.id}: send error for token ${tokenTail(token)}: ${(e as Error).message}`);
      return e instanceof PushConfigError ? "config" : "failed";
    }
  }));
  const count = (kind: Outcome) => results.filter((r) => r === kind).length;
  const summary = { sent: count("sent"), deleted: count("deleted"), failed: count("failed") + count("config") };
  // Nothing went out because of our setup (service account, OAuth grant, project id): a 500 makes
  // it visible in the function logs and the webhook history instead of looking like a success.
  if (summary.sent === 0 && summary.deleted === 0 && count("config") > 0) {
    return json(500, { error: "push misconfigured", ...summary });
  }
  return json(200, summary);
}

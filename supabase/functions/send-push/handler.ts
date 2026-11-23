import { buildFcmMessage, isUnregisteredError, type NotificationRecord, secretsMatch } from "./fcm.ts";

/** Everything the handler touches outside itself; `index.ts` wires the real ones. */
export interface PushDeps {
  webhookSecret: string | undefined;
  /** `notification_prefs.push_enabled`; no row means the default (true). */
  pushEnabled(userId: string): Promise<boolean>;
  deviceTokens(userId: string): Promise<string[]>;
  deleteToken(userId: string, token: string): Promise<void>;
  /** POSTs one FCM v1 message; returns the HTTP status and the parsed JSON body. */
  send(message: unknown): Promise<{ status: number; body: unknown }>;
  log(message: string): void;
}

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

/** Only the token's tail is logged, never the whole token. */
function tokenTail(token: string): string {
  return `…${token.slice(-6)}`;
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

  let payload: { type?: string; table?: string; record?: Partial<NotificationRecord> };
  try {
    payload = await req.json();
  } catch {
    return json(400, { error: "invalid JSON" });
  }
  const record = payload.record;
  if (payload.type !== "INSERT" || (payload.table && payload.table !== "notifications")) {
    return json(200, { skipped: "not a notifications insert" });
  }
  if (!record?.user_id || !record.title) return json(400, { error: "record needs user_id and title" });

  const userId = record.user_id;
  if (!(await deps.pushEnabled(userId))) {
    deps.log(`notification ${record.id}: push disabled by the user's preferences`);
    return json(200, { skipped: "push disabled", sent: 0 });
  }
  const tokens = await deps.deviceTokens(userId);
  if (tokens.length === 0) return json(200, { skipped: "no device tokens", sent: 0 });

  const results = await Promise.all(tokens.map(async (token) => {
    try {
      const { status, body } = await deps.send(buildFcmMessage(token, record as NotificationRecord));
      if (status >= 200 && status < 300) return "sent";
      if (isUnregisteredError(status, body)) {
        await deps.deleteToken(userId, token);
        deps.log(`notification ${record.id}: deleted unregistered token ${tokenTail(token)}`);
        return "deleted";
      }
      deps.log(`notification ${record.id}: FCM ${status} for token ${tokenTail(token)}: ${JSON.stringify(body)}`);
      return "failed";
    } catch (e) {
      deps.log(`notification ${record.id}: send error for token ${tokenTail(token)}: ${(e as Error).message}`);
      return "failed";
    }
  }));
  const count = (kind: string) => results.filter((r) => r === kind).length;
  return json(200, { sent: count("sent"), deleted: count("deleted"), failed: count("failed") });
}

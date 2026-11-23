// send-push: Database Webhook on INSERT into public.notifications → FCM HTTP v1 push to every
// device token of the recipient (RF-03). Setup and secrets: supabase/functions/send-push/README.md.
import { createClient } from "npm:@supabase/supabase-js@2";
import { AccessTokenCache, fcmSendUrl, parseServiceAccount } from "./fcm.ts";
import { handlePush } from "./handler.ts";

// SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are injected by Supabase into every Edge Function.
// The service-role client bypasses RLS, so it never leaves this function.
const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
  auth: { persistSession: false, autoRefreshToken: false },
});

// Lazily parsed so a missing or broken secret is logged per request instead of crashing the boot.
let tokenCache: AccessTokenCache | null = null;
let projectId = "";
function accessTokens(): AccessTokenCache {
  if (!tokenCache) {
    const account = parseServiceAccount(Deno.env.get("FCM_SERVICE_ACCOUNT"));
    projectId = account.project_id;
    tokenCache = new AccessTokenCache(account);
  }
  return tokenCache;
}

Deno.serve((req) =>
  handlePush(req, {
    webhookSecret: Deno.env.get("PUSH_WEBHOOK_SECRET"),
    async pushEnabled(userId) {
      const { data, error } = await supabase
        .from("notification_prefs")
        .select("push_enabled")
        .eq("user_id", userId)
        .maybeSingle();
      if (error) throw new Error(`notification_prefs: ${error.message}`);
      return data?.push_enabled ?? true;
    },
    async deviceTokens(userId) {
      const { data, error } = await supabase.from("device_tokens").select("fcm_token").eq("user_id", userId);
      if (error) throw new Error(`device_tokens: ${error.message}`);
      return (data ?? []).map((row) => row.fcm_token as string);
    },
    async deleteToken(userId, token) {
      const { error } = await supabase.from("device_tokens").delete().eq("user_id", userId).eq("fcm_token", token);
      if (error) throw new Error(`device_tokens delete: ${error.message}`);
    },
    async send(message) {
      const accessToken = await accessTokens().get();
      const response = await fetch(fcmSendUrl(projectId), {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${accessToken}` },
        body: JSON.stringify(message),
      });
      const body = await response.json().catch(() => null);
      return { status: response.status, body };
    },
    log: (message) => console.log(`[send-push] ${message}`),
  }).catch((e) => {
    console.error(`[send-push] ${(e as Error).message}`);
    return new Response(JSON.stringify({ error: "internal error" }), { status: 500 });
  })
);

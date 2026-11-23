// send-push: Database Webhook on INSERT into public.notifications → FCM HTTP v1 push to every
// device token of the recipient (RF-03). Setup and secrets: supabase/functions/send-push/README.md.
import { createClient } from "npm:@supabase/supabase-js@2";
import { AccessTokenCache, createFcmSend, parseServiceAccount } from "./fcm.ts";
import { handlePush } from "./handler.ts";

// SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are injected by Supabase into every Edge Function.
// The service-role client bypasses RLS, so it never leaves this function.
const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
  auth: { persistSession: false, autoRefreshToken: false },
});

type FcmSend = ReturnType<typeof createFcmSend>;

// Built on first use so a missing or broken secret fails each request (PushConfigError → 500)
// instead of crashing the boot. Once built, the token cache lives as long as the worker.
let fcmSend: FcmSend | null = null;
function fcmSender(): FcmSend {
  if (!fcmSend) {
    const account = parseServiceAccount(Deno.env.get("FCM_SERVICE_ACCOUNT"));
    fcmSend = createFcmSend({ projectId: account.project_id, tokens: new AccessTokenCache(account) });
  }
  return fcmSend;
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
    send: (message) => fcmSender()(message),
    log: (message) => console.log(`[send-push] ${message}`),
  }).catch((e) => {
    console.error(`[send-push] ${(e as Error).message}`);
    return new Response(JSON.stringify({ error: "internal error" }), { status: 500 });
  })
);

# send-push Edge Function

Sends the FCM push for every row inserted into `public.notifications` (RF-03), with the
FCM HTTP v1 API and the payload documented in `supabase/README.md` (data-only, high priority,
`title`, `body`, optional `report_id`).

What it does on each call:

1. Rejects the request unless header `x-webhook-secret` equals the secret `PUSH_WEBHOOK_SECRET`.
2. Skips the recipient when `notification_prefs.push_enabled` is false (no row means on).
   Priorities are already filtered when the notification row is inserted.
3. Sends one message per row of the recipient in `device_tokens`.
4. Deletes the tokens FCM reports as `UNREGISTERED` (or `INVALID_ARGUMENT` about the token).
   Transient errors keep the token.

The OAuth2 access token is obtained from the service account (RS256 JWT signed with Web Crypto)
and cached until 5 minutes before it expires. Logs never contain the secret, the access token or
a full FCM token (only its last 6 characters).

## Files

| File | Purpose |
|---|---|
| `index.ts` | Entry point: Supabase service-role client, FCM `fetch`, wiring |
| `handler.ts` | Request handling with injected dependencies |
| `fcm.ts` | Pure helpers: message builder, error check, JWT, token cache |
| `*_test.ts` | `deno test` suites (no network) |

## Secrets (names only; values never go in the repository)

| Name | Value |
|---|---|
| `FCM_SERVICE_ACCOUNT` | The whole JSON of a Firebase service account (Firebase console → Project settings → Service accounts → Generate new private key), on one line |
| `PUSH_WEBHOOK_SECRET` | A long random string you generate (for example `openssl rand -hex 32`) |

`SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` are provided by Supabase to every Edge Function;
do not set them. The service-role client is used only inside this function.

## Deploy

With the Supabase CLI, from the repository root:

```bash
supabase login
supabase link --project-ref <your project ref>
supabase secrets set --env-file <path outside the repo with the two secrets>
supabase functions deploy send-push --no-verify-jwt
```

`--no-verify-jwt` is required: the webhook authenticates with `x-webhook-secret`, not with a
user JWT. Without the CLI: **Edge Functions → Deploy a new function → Via editor**, name it
`send-push`, paste the three `.ts` files (not the tests), turn **Verify JWT** off, and add the two
secrets under **Edge Functions → Secrets**.

## Database Webhook (dashboard)

**Database → Webhooks → Create a new hook** (enable webhooks first if asked):

1. Name: `send_push_on_notification`.
2. Table: `public.notifications`. Events: tick **Insert** only.
3. Type of webhook: **Supabase Edge Functions**; function `send-push`; method `POST`;
   timeout `5000` ms.
4. HTTP Headers: add `x-webhook-secret` with the same value as `PUSH_WEBHOOK_SECRET`
   (keep `Content-Type: application/json`).
5. **Create webhook**.

The webhook sends `{ "type": "INSERT", "table": "notifications", "record": { ... } }`.

## Check

1. Sign in on the phone with an account (the row appears in `device_tokens`) and close the app.
2. As service role in the SQL Editor:
   `insert into notifications (user_id, title, body) values ('<that user id>', 'Prueba', 'Hola');`
3. The push arrives. **Edge Functions → send-push → Logs** shows the result, and
   `net._http_response` holds the webhook's HTTP response.
4. With `push_enabled = false` in `notification_prefs`, the same insert sends nothing.

## Tests

```bash
cd supabase/functions/send-push
deno test
deno check index.ts
```

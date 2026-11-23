# Phase 4 — Notifications and polish

Objective: notifications to nearby affiliates by radius and priority, live updates with Realtime, FCM push with the app closed, and the polish backlog.
Branch: `master`. Every unit is a small commit set that goes through native review before the next one ("En la fase 4 quiero que si dejes que te chequen absolutamente todo"). Push at phase end. Test runner: `./gradlew :shared:testAndroidHostTest`, `./gradlew :androidApp:assembleDebug`, emulator against the real project.

## Specs

- S1 (`CLAUDE.md`): "Fase 4: notificaciones en tiempo real por radio y prioridad, y pulido."
- S2 RF-03: "Cada reporte nuevo notifica a los afiliados dentro de un radio configurable, por ejemplo 5 km por defecto." "Preferencias: cada afiliado y cada usuario elige qué prioridades quiere recibir y cuáles omitir." "Se respetan las preferencias (notification_prefs): si alguien desactivó una prioridad, no recibe ni la push ni la notificación en la app."
- S3 RF-03: "Las notificaciones se guardan en la tabla notifications y se muestran en la pantalla de Notificaciones. Mientras la app está abierta llegan en vivo con Supabase Realtime."
- S4 RF-03 push: "Las notificaciones push son obligatorias. Llegan al celular aunque la app esté cerrada, mediante Firebase Cloud Messaging (FCM)": token in `device_tokens` at sign-in, removed at sign-out; an Edge Function sends with the FCM HTTP v1 API; "Al tocar la notificación, la app abre el detalle del reporte correspondiente."; push also for contact requests received/accepted, affiliation approved, and adoption requests approved/rejected (RF-06); "En Android 13 o más reciente se pide el permiso POST_NOTIFICATIONS, con una pantalla que explica para qué se usa."; `google-services.json` gitignored; the Firebase service account is only a Supabase Edge Function secret.
- S5 polish (`CLAUDE.md` Phase 4 + earlier logs): accessibility, empty and error states, Figma copy fixes; bottom-nav labels must not clip at large font scales; Home "Ver adopciones" must not sit under the green wave; keep the fake auth and the demo-account hint out of Supabase/release builds.
- S6: "dejes que te chequen absolutamente todo" — native review on every unit.

## Tasks

| ID | Specs | Route | Status | Commit |
|---|---|---|---|---|
| T1 | S2 | delegated (worktree) — SQL: notify affiliates in radius by priority on new reports, prefs respected | done — reviewed (approved) + follow-ups 0900/1000 reviewed (approved) | 75c6a72, 919c3c9, 4ddefdf |
| T2 | S5 | delegated (worktree) — polish | done — reviewed (approved); Home fix partial (button ~10 dp under the wave), emulator check pending | 1299afc..06cd3be |
| T3 | S3 | delegated (worktree) + follow-up — Realtime notifications, unread badge, error/retry | done — 2 reviews approved | f24311c..98b84eb, 6b6fa0a, 5233d8e |
| T4 | S4 | delegated — FCM client: tokens, permission screen, tap opens report | done — 2 reviews approved | ebd37ce..2f14330, 9728c75..21734e8 |
| T5 | S4 | delegated — Edge Function send-push, webhook wiring, FCM deleteToken on sign-out | done — 2 reviews approved (1 bounded correction) | 6fc6ec5..da8a026, f15ead8, d9cca00, bc8bdaa |
| T6 | S1 | user: Firebase project, google-services.json, Edge Function deploy + secret; parent emulator check; push | pending | |

Route evidence: T1 (`supabase/`) and T2 (`ui/`) are independent with disjoint surfaces → parallel. T3–T5 depend on each other (notification repository, Gradle, manifest) → sequential. Each unit reviewed natively before the next.

## Log

- L1 (2026-11-22, user): "1.- y 2.- Si estoy de acuerdo, 3.- Si quiero que envie un motivo de porque el rechazo. En la fase 4 quiero que si dejes que te chequen absolutamente todo, pero antes de iniciar la fase 4 arregla lo que dije del punto 3 y esperas mi respuesta o confirmacion"
- L2 (2026-11-22, user): "Ya ejecute el archivo, puedes seguir con la fase 4"
- L3: Reviews: T1 approved (lineage review-cb51ab19a084440e); 0900+polish approved (review-5d61d4f46b353b65); 1000 approved (review-4a431e4b65287616). Follow-ups applied: exception-safe nearby notify, per-reporter rate limit serialized with advisory lock, server-set created_at. Documented: anonymous-session rotation relies on Supabase anonymous sign-in rate limit. User must apply migrations 0800, 0900, 1000.
- L4: T3 reviews approved (review-c50b2960ccdeb30e, review-560e9ef803e8e9f8). Follow-up fixed: retry on any channel stop, catch-up after every resubscribe, DELETE handling, HomeRoute. Remaining non-blocking (polish later): flapping channel resets the retry budget; give-up flag can be cleared by an unrelated refresh; repository-level live wiring has no tests; one tautological badge assertion. User added androidApp/google-services.json (gitignored in ac6cccc).
- L5: T4 reviews approved (review-c95e9d7b659041dd, review-1caa9408dd780de9). Correction budget for T4 used. Remaining risk (slow-network sign-out can leave a token row; a late retry can re-register) is closed in T5 by invalidating the FCM token on the device at sign-out (FirebaseMessaging.deleteToken) and by the Edge Function deleting UNREGISTERED tokens. Permission pref migration not needed: the app has not been released.
- L6: T5 reviews: review-fa0d8e233333b7e4 approved; follow-up review-2b88af8f1a2153b3 required one bounded correction (unterminated test string in fcm_test.ts, fixed in bc8bdaa) and was approved by the targeted validator. Deno is not installed locally: the Deno tests were written but never run. User must deploy send-push, set secrets and create the webhook.

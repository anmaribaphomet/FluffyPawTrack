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
| T1 | S2 | delegated (worktree) — SQL: notify affiliates in radius by priority on new reports, prefs respected | pending | |
| T2 | S5 | delegated (worktree, parallel with T1) — polish | pending | |
| T3 | S3 | delegated after T1 — Realtime notifications in the app | pending | |
| T4 | S4 | delegated after T3 — FCM client: tokens, permission screen, tap opens report | pending | |
| T5 | S4 | delegated after T4 — Edge Function + trigger to send pushes | pending | |
| T6 | S1 | user: Firebase project, google-services.json, Edge Function deploy + secret; parent emulator check; push | pending | |

Route evidence: T1 (`supabase/`) and T2 (`ui/`) are independent with disjoint surfaces → parallel. T3–T5 depend on each other (notification repository, Gradle, manifest) → sequential. Each unit reviewed natively before the next.

## Log

- L1 (2026-11-22, user): "1.- y 2.- Si estoy de acuerdo, 3.- Si quiero que envie un motivo de porque el rechazo. En la fase 4 quiero que si dejes que te chequen absolutamente todo, pero antes de iniciar la fase 4 arregla lo que dije del punto 3 y esperas mi respuesta o confirmacion"
- L2 (2026-11-22, user): "Ya ejecute el archivo, puedes seguir con la fase 4"

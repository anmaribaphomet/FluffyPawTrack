# Phase 2 — Supabase

Objective: replace the fake repositories with Supabase (auth incl. anonymous sign-in, tables, RLS, storage) without changing the screens.
Branch: `master`, small commits (one unit each, so native review fits), push at phase end. Test runner: `./gradlew :shared:testAndroidHostTest`, `./gradlew :androidApp:assembleDebug`, emulator against the real project after the user applies the migrations.

## Specs

- S1 (roadmap, `CLAUDE.md` Phase 2): "Fase 2: Supabase (auth con anonymous sign-in, tablas, RLS, storage de fotos) y cambiar el repositorio fake por el real."
- S2 (`CLAUDE.md` rules): "las credenciales de Supabase se leen de local.properties (SUPABASE_URL y SUPABASE_PUBLISHABLE_KEY) y nunca se escriben en archivos versionados"; "el esquema de Supabase va como migraciones SQL en supabase/migrations/, con políticas RLS en todas las tablas."
- S3 (`docs/REQUIREMENTS.md` §3): "Las tablas se crean con migraciones SQL guardadas en el repo (supabase/migrations/) ... Se aplican desde el SQL Editor de Supabase." "La secret key y la contraseña de la base de datos nunca se usan en la app."
- S4: every table of `docs/REQUIREMENTS.md` §7 including `adoption_requests`, with RLS policies; `get_contact_details(request_id)` RPC (RF-05 "Reglas técnicas"); storage buckets `report-photos` and `adoption-photos` (plus place photos); photos compressed before upload (§8).
- S5: the business rules already enforced by the fakes (RF-05 contact requests, RF-06 adoption requests, report status) are enforced server-side by RLS, constraints and RPC functions — "La seguridad se aplica con Row Level Security (RLS) en Supabase, no solo ocultando botones en la app."
- S6: screens do not change; `AppContainer` switches from fake to Supabase implementations of the same interfaces.
- Assumptions: the user applies migrations in the SQL Editor and enables "Allow anonymous sign-ins" in the dashboard; sample data is seeded by a separate seed migration whose image refs keep using the bundled `res:` images; if email confirmation is on, sign-up shows "Revisa tu correo para confirmar tu cuenta" instead of entering the app.

## Tasks

| ID | Specs | Route | Status | Commit |
|---|---|---|---|---|
| T1 | S2 | delegated (worktree) — deps, BuildConfig config, client provider | done — supabase-kt 3.9.0, Ktor 3.6.0; key not in tracked files | 3a5277c |
| T2 | S3, S4, S5 | delegated (worktree) — SQL migrations, RLS, RPC, storage, seed | done — 11 tables with RLS, 15 RPCs, 3 buckets; local PG18: applied twice, 45 behavior scenarios OK | 0ddc77f..43b99dc |
| T3 | S1, S6 | delegated (worktree) — Supabase repositories + AppContainer switch | done — 243 tests, no network in tests | 3f7c7bc..3791a59 |
| T4 | S1 | user applies migrations + dashboard settings; parent emulator check; push | done — live checks below; pushed | see L7 |

Route evidence: T1 and T2 are independent units with disjoint surfaces (Gradle/Kotlin config vs `supabase/`), each several files → parallel writers. T3 depends on both. Risk: item 5 (dependencies) for T1, item 1/2 (schema, RLS, auth) for T2/T3 → small commits and native review.

## Log

- L1 (2026-11-22, user): "Ok sigue con la siguiente fase"
- L2: Maven Central 2026-11-22: supabase-kt BOM 3.9.0, Ktor 3.6.1 (writer verifies compatibility).
- L3: T1 merged. Note: the project URL (not the key) is in docs/REQUIREMENTS.md §3, written by the user; left as is. T2 hit the usage limit and was resumed. The emulator was stopped by the system for low memory; not restarted.
- L4: T2 merged (8308ad1). Not tested against real Supabase auth/storage schemas yet. User asked to apply migrations 0100-0400 (+ optional 0500 seed) in the SQL Editor and enable anonymous sign-ins.
- L5: T3 merged (265b663); app now uses Supabase when local.properties has both keys. To verify live: RPC composite results decode, sign-up metadata to profiles, session restore within splash, anonymous report + photo upload + public URL in AppImage, Spanish RPC errors reach UI, notify() unsupported on Supabase (server creates notifications).
- L6: Native review of the phase (user granted) stopped with lens_context_budget_exceeded. Independent read-only verifier (high-risk items 1/2): S1-S4, S6 PASS; S5 PARTIAL with 2 blockers (closing a case left pending contact requests acceptable; deleting an organization with affiliates failed). Fixed in migration 0600 (64f204a); probes 1-9 documented in supabase/README.md. Non-blocking suggestions kept for later (private bucket for proof photos, hide reporter ids from anon, org-type check on take_report and adoption-photos, org phone check, delete of taken reports).
- L7 (2026-11-22, user): "ya hice todo, ya subi las migraciones a supabase, active allow anonymoous sign ins y desactive confirmacion de email". Live emulator check against the real project: wrong credentials → generic message (server reached); registration of demo@correo.com via the app → session + profile from metadata (Account shows María Valenzuela); map loads seed organizations/reports; new report with camera photo → upload to report-photos and public URL shown in Mis reportes; GPS + Geocoder returned "Calle Av. Rosales 171, Centro, 83079 Hermosillo, Son., México"; adoption request submitted through submit_adoption_request. Not verified live: duplicate-request error text, contact request RPCs, affiliate flows (patitas account not registered/promoted yet), anonymous report on the real project. Open: keep fake auth/demo hint out of release builds.

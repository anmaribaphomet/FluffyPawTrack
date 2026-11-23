# Phase 3 — Affiliate and admin views

Objective: role-based navigation and the affiliate and admin screens, on both the fake and the Supabase repositories.
Branch: `master`, small commits (each unit reviewable on its own), push at phase end. Test runner: `./gradlew :shared:testAndroidHostTest`, `./gradlew :androidApp:assembleDebug`, emulator against the real project.

## Specs

- S1 (roadmap `CLAUDE.md`): "Fase 3: vistas de afiliado y administrador según el rol."
- S2 (`CLAUDE.md` Phase 3): "Role-based navigation after login".
- S3 Affiliate (`docs/REQUIREMENTS.md` §6 "Afiliado"): "Bandeja de reportes cercanos, con filtro por prioridad"; "Solicitudes de contacto recibidas en los reportes de su organización (ver RF-05)"; "Detalle del reporte → Tomar caso → cambiar estado"; "Mis casos en curso"; "Mis fichas de adopción (crear, editar, marcar adoptado)"; "Solicitudes de adopción de sus animales (aprobar o rechazar con motivo, ver RF-06)"; "Perfil de la organización y preferencias de notificación (radio y prioridades)". RF-04: "Pendiente de atención → En camino → Resguardado → Caso cerrado"; "Solo un afiliado puede tomar el caso. Una vez tomado, los demás lo ven como "En camino (Albergue X)" y no pueden tomarlo"; "Cada cambio de estado notifica a quien hizo el reporte"; "Se guarda un historial de estados con fecha y responsable."
- S4 Admin (§6 "Administrador", RF-07): "Solicitudes de afiliación pendientes (aprobar o rechazar)"; "Directorio de lugares (CRUD: albergues, centros, veterinarias, puntos seguros)"; "Supervisión de reportes (ver, ocultar, cerrar)"; "El admin da de alta, valida o rechaza albergues, centros de adopción, veterinarias aliadas y puntos seguros: nombre, tipo, ubicación, horario, capacidad y contacto." "Mientras un lugar esté activo, aparece permanentemente en el mapa."
- S5 (`CLAUDE.md` Phase 3): "Affiliates see contact requests received by their organization (Recibidas)".
- S6: RF-06 listings need ≥1 photo and the required fields "fotos, tipo de animal, raza, edad, vacunación y descripción"; images and "Cómo llegar" rules from `CLAUDE.md` apply to new screens.
- S7: the server already enforces these rules (`supabase/migrations`, RPCs `take_report`, `set_report_status`, `set_report_hidden`, `review_affiliation`, `mark_listing_adopted`, `respond_adoption_request`); the app calls them and shows their Spanish errors.

## Tasks

| ID | Specs | Route | Status | Commit |
|---|---|---|---|---|
| T1 | S3–S7 contract | delegated (worktree) — repository additions in domain, fake and Supabase | done — 288 tests | 29e9264..8862c40 |
| T2 | S3, S5, S6 | delegated (worktree) — affiliate screens | done — 319 tests | 1b53243..b6385ec |
| T3 | S4 | delegated (worktree) — admin screens | done — 317 tests | 06445e3..43e311a |
| T4 | S2 | delegated — role-based start and wiring | done — 356 tests | 3c3f0bb |
| T5 | S1 | emulator check against Supabase (needs patitas affiliate + an admin account), push | pending | |

Route evidence: T1 is the shared contract both screen units need; T2 and T3 are independent units with disjoint surfaces (`ui/affiliate` vs `ui/admin`), each several files → parallel writers after T1. Navigation is owned by T4 to avoid conflicts.

## Log

- L1 (2026-11-22, user): "Empieza con la siguiente fase"
- L2: T1 merged. Deviations: reviewAffiliation has no reason (RPC/table have none); prefs update takes userId; status-history responsible shows the org name or "Administración" because other profiles are private under RLS. Demo: admin@correo.com, patitas@correo.com (case report-7, request-3), carlos@correo.com requester of Rescate Animal Norte.
- L3: T2-T4 merged. Choices to confirm with user: admin place editor requires phone (10 digits), email, schedule, description; affiliate prefs require at least one priority, radius 1-20 km; Cuenta for affiliate/admin shows Información personal + Ajustes. User asked to register patitas/admin and run the promotion SQL.

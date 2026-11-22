# Phase 1 — All user screens with fake data

Objective: every citizen (`user`) screen working on the fake repositories, including login and registration. Target: Monday, Nov 23, 2026.
Branch: `master` (commit per unit, push at phase end). Parallel units run in isolated git worktrees and are merged by the parent. Test runner: `./gradlew :shared:testAndroidHostTest`, then `./gradlew :androidApp:assembleDebug`.

## Specs

- S1 (roadmap): "Fase 1: TODAS las pantallas del usuario funcionando con datos fake, incluidos login y registro (meta del lunes 23)."
- S2 (order, L2): "navegación y datos fake → mapa → flujo de reporte → cuenta y Mis reportes → adopciones → login y registro → solicitudes de contacto → ajustes".
- S3 Map — `docs/REQUIREMENTS.md` RF-02: reports, adoptions, affiliated shelters, vets and safe points; "Un pin de color distinto por categoría"; filters "Todos, cada categoría de reporte, Adopciones y Lugares de apoyo"; "Búsqueda en el mapa"; tapping a pin opens a detail sheet "con foto, descripción, categoría, prioridad, estado y fecha". Figma: MAPA (11:277).
- S4 New report — RF-01: category (6 options), photo (camera or gallery), GPS location with permission dialog and optional reference, description, priority (suggested by category, locked to urgent for "Estado crítico o moribundo"), summary → modal "¡Reporte publicado!" → "Ver mapa". Figma screens 10–16.
- S5 Account — RF-08: personal info, "Mis reportes" (history with Editar and Eliminar, Figma 17–18), Favoritos, counters Reportes / Atendidos / Favoritos.
- S6 Adoptions — RF-06 user side: centers map → center detail → animal detail → favorites → "Formulario de adopción" (Figma 6–7, nodes 301:731, 309:770).
- S7 Auth — RF-08: "Registro e inicio de sesión con correo y contraseña. *No hay pantalla en Figma: se diseña con el mismo estilo.*" Plus splash/loading with the logo (Figma 1–2) and anonymous report option (§4).
- S8 Contact requests — RF-05 user side: "Es mi mascota" form (message 20–500 chars, optional photo) and screen "Solicitudes de contacto" with tabs Enviadas / Recibidas.
- S9 Settings — RF-08: "idioma (ES), modo oscuro, notificaciones, accesibilidad, permisos, FAQ y Contáctanos" (Figma 265:73).
- S10 Notifications — RF-03 UI: list from the repository and "Marcar todas como leídas".
- S11 Affiliation request screen (REQUIREMENTS §6 "Común").
- S12: UI copy in neutral Spanish; follow `docs/design-system.md`; `assembleDebug` passes before done; push at phase end.

## Tasks

| ID | Specs | Route | Status | Commit |
|---|---|---|---|---|
| T0 | S12 | inline | done — shared deps (MapLibre, activity-compose), permissions, demo session | (prep commit) |
| T1 | S3 | delegated W1 (worktree) | done — merged, emulator: tiles, pins, detail sheet OK | 60cfe94, d766b14 |
| T2 | S4 | delegated W2 (worktree) | done — merged, emulator: full anonymous flow to "¡Reporte publicado!" | 5a3f912, b0b1da9 |
| T3 | S5, S9 | delegated W3 (worktree) | done — merged, emulator: account data, my reports, settings OK | 6881c7f, 69ddbf0, 7ea1216 |
| T4 | S7 | delegated W4 (worktree) | done — merged, emulator: splash → login → home | 074f888, e1158d3 |
| T5 | S6 | delegated W5 (worktree) | done — merged and wired (map "Ver ficha" / "Ver centro") | de9e869 |
| T6 | S8, S11 | delegated W6 (worktree) | done — merged and wired (map "Es mi mascota", Cuenta → Solicitudes, Login → afiliación) | d45eaf5, d62506f, c075a09 |
| T7 | S10 | inline | done — RED compile, GREEN 4/4, emulator checked | see log |
| T8 | S1, S12 | inline — wire routes, merge, build, push | pending | |

Route evidence: writer trigger (a) — four independent units with disjoint edit surfaces, each several files, launched together; worktrees because they share the `shared` compilation unit. Parent owns `Routes.kt`, `AppNavHost.kt`, Gradle files and the manifest.

## Log

- L1 (2026-11-22, user): "ok empieza la fase 1 ahora"
- L2 (2026-11-22, user, earlier): "1. Sí, ese orden está bien." (order in S2)
- L3: T0 prep: MapLibre Android 13.7.1 (Maven Central), activity-compose in shared androidMain, INTERNET + location permissions, `FakeAuthRepository(initialUser)` with demo session until login exists.
- L4: Shared labels/pin colors/date format committed before launching writers (7df92a2). T7 notifications wired to the repository with "Marcar todas como leídas".
- L5: Worktrees branch from `origin/master` (cb31946), not local master; writers were told to `git merge master` first. Batch 1 merged without conflicts; 80/80 tests, assembleDebug OK. Emulator fixes: photo frame 4:3 so camera/gallery buttons stay visible. Map detail sheets got `reportActions` / `placeActions` / `adoptionActions` slots so batch 2 does not edit `ui/map`.
- L6: Open decisions reported by W2 (kept as built): photo optional; GPS address saved as coordinates; anonymous-session users send their anonymous id.
- L7: Native review for batch 1 (user granted) stopped with `lens_context_budget_exceeded` (64 files, ~5,700 lines): no review authority created. Splitting would rewrite committed history; deferred — Phase 2 real auth will use small commits so review fits.
- L8: Batch 2 writers hit the usage limit and were resumed. W5 and W6 merged without conflicts; 144/144 tests, assembleDebug OK. Added entry points: "Solicitudes de contacto" row in Cuenta, affiliation link on Login. W6 added `ReportRepository.closeReport` and `OrganizationRepository.requestAffiliation`; received requests for organizations are listed in Phase 3 (affiliate side).

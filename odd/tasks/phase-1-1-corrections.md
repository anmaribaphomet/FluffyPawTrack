# Phase 1.1 — Review corrections

Objective: apply the user's review of the Phase 1 screens (points A–G plus decisions 1–3) on the fake repositories.
Branch: `master`, one commit per point, push at the end. Test runner: `./gradlew :shared:testAndroidHostTest`, `./gradlew :androidApp:assembleDebug`, emulator check.

## Specs

- S0 (docs): "Antes de programar, actualiza docs/REQUIREMENTS.md y el CLAUDE.md con estos cambios. Después impleméntalos y haz un commit por cada punto."
- S1 (decision 1 / F): "Foto del reporte: NO es opcional, es OBLIGATORIA (ver punto F)."
- S2 (decision 2): "Ubicación por GPS: guarda las coordenadas Y, si se puede obtener, la dirección en texto con el Geocoder de Android. Si no se obtiene, muestra solo las coordenadas. Las coordenadas son las que se usan para el enlace de Google Maps."
- S3 (decision 3 / D): "Formulario de adopción: SÍ debe guardarse. Crea la tabla adoption_requests (ver punto D)."
- SA: "Cuando un dato sea inválido, muestra solo: "Datos inválidos, intenta de nuevo." No des pistas (no digas si el correo no existe o si la contraseña está mal)."
- SB: "El buscador y el botón Filtros quedan encimados sobre la onda del encabezado (logo y botón Regresar). Bájalos para que queden completamente debajo de la onda, sin tocarla. Revísalo en el emulador."
- SC: "Quita los pines individuales de los animales en adopción (Michi, Canela, Toby, etc.). En el mapa solo aparece UN pin por centro de adopción." Flow: "1. Tocar el pin del centro → hoja con la info preliminar (como está ahora). 2. "Ver centro" → ficha completa del centro (la info actual está bien) y debajo la sección "Animales en adopción". 3. Cada animal es una tarjeta plegada con: foto, nombre, tipo, raza y edad, más una flecha. 4. Tocar la flecha → ficha completa del animal (todas sus fotos, vacunación, descripción, etc.). 5. Al final de la ficha del animal → botón "Quiero adoptarlo"." "Quita el botón "Formulario de adopción" de la ficha del centro. El formulario SOLO se abre desde "Quiero adoptarlo" en un animal específico."
- SD: adoption requests exactly as `docs/REQUIREMENTS.md` RF-06 "Solicitud de adopción" (quoted verbatim from the user): table fields and RLS; form with locked "Interesado en:" ("Canela · Perro · Mestiza"); editable prefilled contact; housing casa/departamento and propia/rentada; "¿Tienes otras mascotas?"; "¿Por qué quieres adoptarlo?" required; text "Una vez aprobada tu solicitud, el centro se pondrá en contacto contigo mediante los datos de contacto que proporcionaste."; rules (account, only role user, one active per user per animal, cancel while pending); center section "Solicitudes de adopción" with Aprobar / Rechazar; approve message "¡Buenas noticias! El centro aprobó tu solicitud para adoptar a [Nombre]. Pronto se pondrán en contacto contigo."; reject message "Tu solicitud para adoptar a [Nombre] no fue aprobada esta vez. ¡Hay muchos más esperando un hogar!" plus reason; adopted → pending rejected with the same notice; Cuenta → "Mis solicitudes de adopción".
- SE: "La categoría "Estado crítico o moribundo" se muestra en toda la app solo como "Estado crítico"."
- SF: "Reporte: foto del animal obligatoria. No se puede avanzar del paso de foto sin ella." "Animal en adopción: al menos 1 foto obligatoria." "Centro, albergue, veterinaria o punto seguro: foto del lugar o logo obligatoria." "Toda ficha y tarjeta muestra su imagen. Actualiza los datos fake para que todos tengan imagen de ejemplo."
- SG: "Toda dirección o ubicación (reportes, centros, albergues, veterinarias, puntos seguros) tiene un botón o enlace "Cómo llegar" que abre Google Maps con la ruta hacia esas coordenadas: https://www.google.com/maps/dir/?api=1&destination=LAT,LNG" "Las redes sociales de los afiliados son enlaces que abren la app o el navegador." "Teléfono → abre el marcador. Correo → abre la app de correo."
- S9: "Al terminar, compila, pruébalo en el emulador y muéstrame un resumen de lo que cambió."
- Assumptions (fake phase): adoption requests live in the fake repository now; the SQL migration and RLS policies are written in Phase 2. "Push" is an in-app notification now; FCM push stays in Phase 4. A demo affiliate account for Patitas Callejeras is added so the center section can be tested.

## Tasks

| ID | Specs | Route | Status | Commit |
|---|---|---|---|---|
| T0 | S0 | inline | done | 69cf6fa |
| T1 | SA | inline | done — RED/GREEN on Login/Register VM tests | 216967a |
| T2 | SE | inline | done — RED/GREEN LabelsTest | a12f46f |
| T3 | SB | inline | done — emulator: search below wave | see log |
| T1b | SA | delegated (worktree) | done — 156 tests | f0e0f5e |
| T4 | SF, S1 | delegated (context backstop) | done — 152/152 tests; 19 Commons images (licenses/SAMPLE_IMAGES.md) | d2fe939 |
| T5 | SC | delegated (worktree) | done — 157 tests; nav hooks removed by parent | 31c095a + cleanup |
| T6 | SD, S3 | delegated (context backstop) | pending | |
| T7 | SG, S2 | delegated (context backstop) | pending | |
| T8 | S9 | inline — build, emulator, push | pending | |

Route evidence: T4–T7 touch shared screens (adoptions, map sheets, SampleData), so they run one after another in the main tree; delegated because the parent context is past the long-session backstop.

## Log

- L1 (2026-11-22, user): "Revisé la Fase 2 y hay correcciones. Antes de programar, actualiza docs/REQUIREMENTS.md y el CLAUDE.md con estos cambios. Después impleméntalos y haz un commit por cada punto." (full message with points A–G and decisions 1–3; quoted in Specs)
- L2: T3 emulator evidence: map opened after demo login; search pill and Filtros start below the header wave (screenshot 8:24, top 150 dp + 8 dp), no overlap with logo or "Regresar".
- L3: Native review of A/E/B + docs (lineage review-1c59285481371975, 4 lenses, user granted) approved and acknowledged; non-blocking findings. Follow-up T1b: show "Datos inválidos, intenta de nuevo." only for invalid data (typed domain error), keep a separate generic message for connection/server failures; remove dead per-field register errors and readableMessage leftovers; rename stale test; fix the CLAUDE.md FCM bullet.
- L4: Native review of F + T1b (lineage review-b197610350b2854f, 4 lenses, user granted) approved and acknowledged. Fixed now: listing cover photo guarded (coverPhotoUrl, blank ref → placeholder). Remaining suggestions (AppImage contentScale on local photos, shared photo-error constant, gallery width constant) are minor and left for polish.

# Phase 0 — Foundations

Objective: navigation library, layered architecture, and a fake repository with Hermosillo sample data.
Branch: `master` (project rule: commit per unit, push at phase end). Strategy: single-pr not applicable; direct commits. Test runner: `./gradlew :shared:testAndroidHostTest` + `./gradlew :androidApp:assembleDebug`.

## Specs

- S1: "Fase 0: librería de navegación" — use "Navigation Compose, la oficial de JetBrains" (`org.jetbrains.androidx.navigation:navigation-compose`), replacing the `enum` + `when` navigation in `App.kt`.
- S2: "arquitectura" — layers as described in `CLAUDE.md` § Architecture: "capas, un repositorio fake y uno de Supabase con la misma interfaz, y expect/actual para mapa, cámara y GPS".
- S3: "repositorio fake con datos de ejemplo de Hermosillo (reportes, albergues, adopciones)".
- S4: Align `ui/theme` with `docs/design-system.md` (roadmap item in `CLAUDE.md`; design system: "Single family: **Inter**", consolidated tokens).
- S5: "siempre compilar con gradlew :androidApp:assembleDebug antes de dar algo por terminado"; "push al terminar cada fase en master".

## Tasks

| ID | Specs | Route | Status | Commit |
|---|---|---|---|---|
| T1 | S1 | inline | done | 39b2bf8 |
| T2 | S2 | inline | done (platform expect/actual deferred to Phase 1 features) | 71a1051 |
| T3 | S3 | inline | done — RED compile failure, GREEN 15/15 | 6b2c980 |
| T4 | S4 | inline | partial — Home card text overflows, needs user decision | (this commit) |
| T5 | S5 | inline | pending | |

Route evidence: one sequential feature, no parallel independent units, no context backstop → no writer trigger.

## Log

- L1 (2026-11-22, user): "1. Sí, ese orden está bien. 2. Usa Navigation Compose, la oficial de JetBrains. 3. Usa mx.unison.fluffypawtrack como identificador de la app. 4. Sí, actualiza la sección 9 para que coincida con design-system.md. Después de eso, empieza la Fase 0."
- L2: Decisions 3 and 4 done before Phase 0 (`a1c5063`, `0e11dfd`). Versions checked on Maven Central: navigation-compose 2.10.0, kotlinx-serialization-json 1.13.0, kotlinx-coroutines 1.11.0.
- L3: T1 no deterministic test for navigation in commonTest; checked by assembleDebug + emulator.
- L4: T4 emulator check (Medium_Phone_API_37). Fixed: cards were green and grass pink (Material slots remapped), profile subtitles white on peach. Corrected design-system: Home background is peach-200 (#FCD4A1, the Figma/code value), not peach-300.
- L5: T4 open: with title-card 20 Bold + body 14, Home card text no longer fits the fixed 157/163 dp card bodies (description clipped, pill button hidden). Needs decision: grow cards or shrink description.
- L6: Old build `com.example.fluffypawtrack` is still installed on the emulator next to `mx.unison.fluffypawtrack`.

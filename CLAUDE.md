# Fluffy PawTrack

Mobile app for Hermosillo that lets citizens report stray, lost, or injured animals
(photo + GPS) on a collaborative map. Nearby affiliated shelters are notified and track
each rescue; the app also centralizes adoptions and support places (shelters, vets,
safe points). Three roles: `user`, `affiliate`, `admin`.

Sources of truth:

- Requirements: `docs/REQUIREMENTS.md`
- Design system (palette, type scale, contrast rules): `docs/design-system.md`
- Figma: https://www.figma.com/design/wW2C6PzFMazd7DsElw6Q38/Fluffy-PawTrack

## Stack

| Layer | Technology |
|---|---|
| App | Kotlin Multiplatform + Compose Multiplatform (`shared/`, `androidApp/`) |
| Backend | Supabase: Auth (email + anonymous sign-in), PostgreSQL, Storage, Realtime, Edge Functions |
| Supabase client | `supabase-kt` (check the current version with context7 before adding it) |
| Push | Firebase Cloud Messaging, Android only (`androidMain`) |
| Map | OpenStreetMap-based, no API key, behind `expect`/`actual` |
| Camera, GPS | Android APIs in `androidMain`, behind `expect`/`actual` |
| Navigation | A Compose Multiplatform navigation library (chosen in Phase 0) |

Only Android is built and tested. iOS code stays in the repo but is not compiled.

## Architecture

Layers inside `shared/src/commonMain/kotlin/com/example/fluffypawtrack/`:

```
domain/
  model/        Plain Kotlin data classes and enums (Report, Place, AdoptionListing, ...)
  repository/   Repository interfaces (ReportRepository, PlaceRepository, AuthRepository, ...)
data/
  fake/         In-memory implementations with Hermosillo sample data
  supabase/     supabase-kt implementations of the same interfaces
platform/       expect declarations: map view, camera/gallery picker, location provider
di/             AppContainer: manual wiring, selects fake or Supabase repositories
ui/
  theme/        Tokens from docs/design-system.md (colors, type scale, shapes)
  components/   Reusable composables (wave header, pill button, settings row, cards, ...)
  navigation/   Routes and the nav host
  <feature>/    One package per feature: Screen composables + ViewModel
```

Rules:

- Dependencies point inward: `ui` → `domain` ← `data`. `domain` imports nothing from
  `ui`, `data`, Compose, or Supabase.
- Screens never touch a repository directly. They receive state and callbacks from
  their ViewModel; the ViewModel receives repository interfaces.
- The fake and the Supabase repository implement the **same interface**. Switching
  between them is a one-line change in `di/AppContainer.kt`; no screen changes.
- Map, camera, and GPS are `expect` declarations in `platform/` with `actual`
  implementations in `androidMain` (and stubs in `iosMain` so the source set stays valid).
- No DI framework: manual constructor injection through `AppContainer`.

## Code conventions

- Kotlin official style. 4-space indent. Trailing commas in multi-line lists.
- Names in English. Composables in PascalCase; one public screen per file
  (`ReportCategoryScreen.kt`).
- UI state: one immutable `data class <Feature>UiState` per screen, exposed as
  `StateFlow` from the ViewModel. Events go in as plain functions (`onSubmit()`).
- Colors and text styles come only from `ui/theme`; never hardcode hex values or
  font sizes in screens. Text on colored surfaces is `brown-900`, never white.
- Repository functions are `suspend` or return `Flow`. Errors are returned as
  `Result<T>`; screens show a readable message, never a stack trace.
- Tests in `commonTest` with `kotlin-test`: fake repositories and ViewModels.
- Comments only where the *why* is not obvious.

## Rules

- **Credentials:** Supabase settings are read from `local.properties`
  (`SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`) by Gradle and exposed through
  `BuildConfig`. They are never written to any versioned file. The secret key and the
  database password are never used in the app. `google-services.json` and any Firebase
  service-account file are never committed.
- **Language:** UI copy in neutral Spanish (no voseo). Code, identifiers, comments, and
  commit messages in English.
- **Git:** work on `master`. One Conventional Commit per unit of work. Push at the end
  of each phase.
- **Definition of done:** `./gradlew :androidApp:assembleDebug` succeeds before
  anything is reported as finished. Tests in `commonTest` pass when they exist.
- **Platform:** test on Android only (emulator or device).
- **Database:** the Supabase schema lives as SQL migrations in `supabase/migrations/`
  (`<timestamp>_<description>.sql`), applied from the Supabase SQL Editor. Every table
  has RLS enabled **and** explicit policies; security is enforced by RLS, not by hiding
  buttons.
- **Dependencies:** keep it light. Add a library only when it removes real work.
- **Design:** follow `docs/design-system.md`. Screens missing from Figma reuse the
  existing components (wave header, pill buttons, cards, settings rows).

## Roadmap

### Phase 0 — Foundations
- [ ] Add the navigation library and replace the `enum` + `when` navigation
- [ ] Create the layer structure (`domain`, `data`, `platform`, `di`, `ui/navigation`)
- [ ] Domain models and repository interfaces for reports, places, adoptions, auth,
      notifications, favorites, and contact requests
- [ ] Fake repositories with Hermosillo sample data: reports, shelters, adoption listings
- [ ] Align `ui/theme` with `docs/design-system.md` (Inter only, consolidated tokens)
- [ ] `assembleDebug` passes; push

### Phase 1 — All user screens with fake data (target: Monday, Nov 23, 2026)
- [ ] Splash, login, and registration
- [ ] Home wired to navigation
- [ ] Map: pins by category, filters, search, pin detail sheet
- [ ] New report flow: category → photo → location (with permission dialog) →
      description and priority → summary → "¡Reporte publicado!"
- [ ] Adoptions: centers map → center detail → animal detail → adoption form
- [ ] Notifications with "Marcar todas como leídas"
- [ ] Account: personal info, My reports (edit, delete), Favorites, counters
- [ ] Contact requests: Sent and Received tabs, "Es mi mascota" form
- [ ] Affiliation request screen
- [ ] Settings
- [ ] `assembleDebug` passes; push

### Phase 2 — Supabase
- [ ] Add `supabase-kt`; read credentials from `local.properties`
- [ ] Auth: email/password and anonymous sign-in
- [ ] Migrations for every table in `docs/REQUIREMENTS.md` §7, with RLS policies
- [ ] `get_contact_details(request_id)` RPC
- [ ] Storage buckets `report-photos` and `adoption-photos`; compress photos before upload
- [ ] Supabase repositories; switch `AppContainer` from fake to Supabase
- [ ] `assembleDebug` passes; push

### Phase 3 — Affiliate and admin views
- [ ] Role-based navigation after login
- [ ] Affiliate: nearby reports inbox, take case and change status, my cases,
      received contact requests, adoption listings, organization profile and preferences
- [ ] Admin: affiliation requests, places directory (CRUD), report supervision
- [ ] `assembleDebug` passes; push

### Phase 4 — Notifications and polish
- [ ] Notifications to affiliates by radius and priority, respecting `notification_prefs`
- [ ] Live updates with Supabase Realtime
- [ ] FCM push (app closed): device tokens, Edge Function, tap opens the report,
      `POST_NOTIFICATIONS` permission screen
- [ ] Polish: accessibility, empty and error states, Figma copy fixes
- [ ] `assembleDebug` passes; push

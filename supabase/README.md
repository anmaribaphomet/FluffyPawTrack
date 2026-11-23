# Supabase backend

Schema, Row Level Security, RPC functions, storage and demo data for Fluffy PawTrack
(`docs/REQUIREMENTS.md` sections 4, 5 and 7). No keys, URLs or passwords live here: the app
reads `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` from `local.properties`.

## Apply the migrations

Open the project's **SQL Editor** and run each file in `migrations/`, in this order. Each file
can be run again safely.

| Order | File | What it creates |
|---|---|---|
| 1 | `20261122000100_schema.sql` | Enums, the 11 tables, constraints, partial unique indexes, the profile-on-signup trigger, Realtime for `notifications` and `reports` |
| 2 | `20261122000200_rls.sql` | Policy helper functions, RLS and policies on every table, table/column grants, guard triggers |
| 3 | `20261122000300_functions.sql` | RPC functions, status history and notification triggers |
| 4 | `20261122000400_storage.sql` | Buckets `report-photos`, `adoption-photos`, `place-photos` and their policies |
| 5 | `20261122000500_seed.sql` | Optional demo content (organizations, listings, reports) |
| 6 | `20261122000600_review_fixes.sql` | Fixes from the security review (close rejects pending contact requests; organization delete) |
| 7 | `20261122000700_affiliation_rejection_reason.sql` | `organizations.rejection_reason`; `review_affiliation(org, approve, reason)` requires a reason when rejecting and sends it to the requester |
| 8 | `20261122000800_nearby_report_notifications.sql` | RF-03: a new report notifies affiliates within their radius who accept its priority; status-change notifications respect the reporter's priorities |
| 9 | `20261122000900_report_notification_safety.sql` | Nearby notifications never roll back a report; at most 5 reports per reporter every 10 minutes |
| 10 | `20261122001000_report_rate_limit_hardening.sql` | Rate limit serialized per reporter (advisory lock) and server-set `created_at` |

## Dashboard settings

- **Authentication → Sign In / Providers → Allow anonymous sign-ins: on.** Anonymous
  sessions can only create reports and upload report photos.
- **Authentication → Email → Confirm email:** your choice. When on, sign-up does not open a
  session until the email is confirmed, and the app shows "Revisa tu correo para confirmar tu
  cuenta". For a quick demo, turn it off.
- Sign-up must send `first_name`, `last_name` and `phone` in the user `data`; the trigger
  copies them into `profiles`.

## Demo accounts

Auth users are not created in SQL. Register these accounts in the app, then run the
statements at the end of `20261122000500_seed.sql` to promote them:

| Email | Becomes |
|---|---|
| `patitas@correo.com` | `affiliate` of Patitas Callejeras HMO (`a0000000-0000-4000-8000-000000000003`) |
| `huellitas@correo.com` | `affiliate` of Refugio Huellitas del Desierto (`a0000000-0000-4000-8000-000000000004`) |
| `admin@correo.com` | `admin` |

Other accounts stay `user`. An affiliation approved with `review_affiliation` promotes its
requester automatically.

## How the rules are enforced

- **RLS** decides which rows each role can read or write.
- **Grants** limit columns: a profile's `role` and `organization_id`, and every column of a
  notification except `read`, cannot be changed through the API.
- **Guard triggers** limit what a direct update may change (a reporter edits only text while
  pending; an affiliate only moves the status forward; only the admin changes an
  organization's type, status or visibility).
- **RPC functions** (`security definer`) run the multi-row rules in one transaction and fail
  with Spanish messages that match the fake repositories. Contact requests are written only
  through RPCs; adoption request status changes too.

### Policies and the requirement each one enforces

| Table | Policy | Requirement |
|---|---|---|
| `profiles` | `profiles_select_own`, `profiles_update_own` | RF-05: nobody reads another person's phone, email or last name |
| `profiles` | `profiles_select_admin` | Section 4: admin manages accounts |
| `organizations` | `organizations_select_public` | RF-07: approved and active places appear on the map, also without an account |
| `organizations` | `organizations_select_requester`, `organizations_select_member` | Requester follows a pending affiliation; affiliates see their organization |
| `organizations` | `organizations_insert_affiliation` | Section 4: affiliation is requested from the app as `pending` (non-anonymous) |
| `organizations` | `organizations_update_member` + guard | Affiliates edit their organization's profile, never its status |
| `organizations` | `organizations_*_admin` | Section 7: only the admin manages organizations |
| `reports` | `reports_select_visible` | RF-02: every visible report is on the map |
| `reports` | `reports_select_own`, `reports_select_admin` | RF-08 "Mis reportes"; RF-07 admin sees all, hidden included |
| `reports` | `reports_insert_own` | RF-01 and section 4: anonymous sessions may report, always as `pending` |
| `reports` | `reports_update_owner` + guard | RF-08: the reporter edits description and reference while pending |
| `reports` | `reports_update_assigned_org` + guard | RF-04: only the assigned organization advances the status |
| `reports` | `reports_update_admin`, `reports_delete_admin` | RF-07: admin hides and closes reports |
| `reports` | `reports_delete_owner` | RF-08: the reporter deletes a report |
| `report_status_history` | `report_status_history_select` | RF-04: history visible with its report; written only by trigger |
| `adoption_listings` | `adoption_listings_select_all` | RF-06: listings are public |
| `adoption_listings` | `adoption_listings_insert/update/delete_center` | RF-06: affiliates of an adoption center manage its listings |
| `adoption_listings` | `adoption_listings_admin` | RF-07 supervision |
| `adoption_requests` | `adoption_requests_select_requester` | RF-06: the user sees their requests |
| `adoption_requests` | `adoption_requests_select_center` | RF-06: affiliates see only their center's requests |
| `adoption_requests` | `adoption_requests_insert_user` | RF-06: only a non-anonymous `user`, for an available animal, as `pending` |
| `contact_requests` | `contact_requests_select_requester` | RF-05: the requester sees what they sent |
| `contact_requests` | `contact_requests_select_recipient_user`, `contact_requests_select_recipient_org` | RF-05: the recipient sees the requests of their reports or their organization |
| `notifications` | `notifications_select/update/delete_own` | RF-03: each user reads and marks their notifications |
| `notification_prefs` | `notification_prefs_own` | RF-03: each user sets their preferences |
| `device_tokens` | `device_tokens_own` | RF-03: each device registers and removes its FCM token |
| `favorites` | `favorites_select/insert/delete_own` | Section 4: favorites need an account (no anonymous inserts) |
| `storage.objects` | `report_photos_*_own` | RF-01: sessions (anonymous included) upload into `report-photos/<uid>/` |
| `storage.objects` | `adoption_photos_*_center` | RF-06: affiliates upload into `adoption-photos/<organization_id>/` |
| `storage.objects` | `place_photos_*_own` | RF-07: requesters upload into `place-photos/<uid>/` (non-anonymous) |
| `storage.objects` | `app_photos_admin` | RF-07 supervision |

Constraints: contact message 20 to 500 characters, at least one listing photo, required photo
URLs, "Estado crítico" always `urgent`, one active contact request per (report, requester),
one active adoption request per (listing, requester).

## RPC functions

Call with `postgrest.rpc("<name>", params)`. Parameter names are exact.

| Function | Parameters | Rule |
|---|---|---|
| `get_contact_details` | `request_id` | RF-05: other party's `full_name`, `phone`, `email`; no rows unless accepted and the caller is a party. Organization recipients return the organization's data |
| `send_contact_request` | `p_report_id`, `p_message`, `p_photo_url` | RF-05 "Qué NO se puede hacer"; notifies the recipient |
| `respond_contact_request` | `p_request_id`, `p_accept` | Accept or reject a pending request; notifies the requester |
| `cancel_contact_request` | `p_request_id` | The requester cancels while pending |
| `mark_pet_delivered` | `p_request_id` | Closes the report, rejects other pending requests with "El caso se cerró" |
| `submit_adoption_request` | `p_listing_id`, `p_full_name`, `p_phone`, `p_email`, `p_housing_type`, `p_housing_tenure`, `p_has_other_pets`, `p_other_pets`, `p_reason` | RF-06 form rules |
| `respond_adoption_request` | `p_request_id`, `p_approve`, `p_reason` | Approve, or reject with a reason; inserts the RF-06 notification text |
| `cancel_adoption_request` | `p_request_id` | The requester cancels while pending |
| `mark_listing_adopted` | `p_listing_id` | Rejects pending requests with "El animal ya fue adoptado" (trigger) |
| `update_report` | `p_report_id`, `p_description`, `p_reference` | The reporter edits a pending report |
| `take_report` | `p_report_id` | RF-04: first affiliate takes the case (`on_the_way`) |
| `set_report_status` | `p_report_id`, `p_status` | RF-04: the assigned organization advances; the admin sets any |
| `set_report_hidden` | `p_report_id`, `p_hidden` | RF-07: admin only; pending contact requests get "El reporte ya no está disponible" |
| `request_affiliation` | `p_type`, `p_name`, `p_address`, `p_phone`, `p_email`, `p_description`, `p_photo_url` | Creates a pending organization |
| `review_affiliation` | `p_organization_id`, `p_approve`, `p_reason` | Admin approves (promotes the requester) or rejects with a required reason (max 500 characters), stored in `organizations.rejection_reason` and sent to the requester |

Triggers also store every report status in `report_status_history`, notify the reporter on
each status change, and reject pending contact requests when a report is hidden or deleted.

## Enum mapping (for the Kotlin repositories)

Database values are lower snake case; Kotlin enums are PascalCase.

| Postgres type | Values | Kotlin enum |
|---|---|---|
| `user_role` | `user`, `affiliate`, `admin` | `Role.User`, `Affiliate`, `Admin` |
| `organization_type` | `shelter`, `adoption_center`, `vet`, `safe_point` | `OrganizationType.Shelter`, `AdoptionCenter`, `Vet`, `SafePoint` |
| `organization_status` | `pending`, `approved`, `rejected` | `OrganizationStatus.Pending`, `Approved`, `Rejected` |
| `report_category` | `street`, `possibly_lost`, `mistreated`, `injured`, `critical`, `abandoned` | `ReportCategory.Street`, `PossiblyLost`, `Mistreated`, `Injured`, `Critical`, `Abandoned` |
| `report_priority` | `low`, `medium`, `high`, `urgent` | `Priority.Low`, `Medium`, `High`, `Urgent` |
| `report_status` | `pending`, `on_the_way`, `sheltered`, `closed` | `ReportStatus.Pending`, `OnTheWay`, `Sheltered`, `Closed` |
| `listing_status` | `available`, `adopted` | `ListingStatus.Available`, `Adopted` |
| `housing_type` | `house`, `apartment` | `HousingType.House`, `Apartment` |
| `housing_tenure` | `owned`, `rented` | `HousingTenure.Owned`, `Rented` |
| `adoption_request_status` | `pending`, `approved`, `rejected`, `cancelled` | `AdoptionRequestStatus.Pending`, `Approved`, `Rejected`, `Cancelled` |
| `contact_request_status` | `pending`, `accepted`, `rejected`, `cancelled` | `ContactRequestStatus.Pending`, `Accepted`, `Rejected`, `Cancelled` |
| `notification_type` | `report`, `adoption`, `favorite`, `general` | `NotificationType.Report`, `Adoption`, `Favorite`, `General` |

Column notes for the mapping:

- `reports.reporter_id` is set for anonymous sessions too; map `Report.reporterId` to null
  when `reporter_is_anonymous` is true. `reports.address` may be null (use the coordinates).
- `organizations.lat`/`lng` and `reports.lat`/`lng` map to `GeoPoint`.
- `adoption_listings.photo_urls` maps to `photoUrls`; `contact_requests.recipient_org_id`
  maps to `recipientOrganizationId`; `notifications.body`/`read`/`created_at` map to
  `message`/`isRead`/`receivedAt`.
- The user's email comes from the auth session, not from `profiles`.
- Seed images keep the `res:sample_*` refs of `SampleData`; uploaded photos use the public
  Storage URL.

## Affiliate and admin calls (Phase 3)

Besides the RPCs above, the app writes these tables directly; RLS and the guard triggers
decide what succeeds, and an update or delete that returns no row is shown as a rule message.

| Repository call | Table access | Policy / guard |
|---|---|---|
| `ReportRepository.observeStatusHistory` | `select report_status_history` + the report's `assigned_org_id` name | `report_status_history_select`. Profiles are private, so a change by anyone but the reporter is shown as the assigned organization (or "Administración" without one) |
| `OrganizationRepository.createOrganization` | `insert organizations` with `status = 'approved'`, `active = true`, `capacity` | `organizations_insert_admin` |
| `updateOrganization`, `setActive`, `deleteOrganization` | `update`/`delete organizations` | `organizations_update_admin`, `organizations_delete_admin` |
| `updateOwnOrganization` | `update organizations` (description, schedule, phone, email, social, photo_url) | `organizations_update_member` + `organizations_guard_update` |
| `AdoptionRepository.createListing`, `updateListing` | `insert`/`update adoption_listings`; photos in `adoption-photos/<organization_id>/` | `adoption_listings_insert_center`, `adoption_listings_update_center` |
| `NotificationPrefsRepository.update` | `upsert notification_prefs` on `user_id` (`priorities` as `report_priority[]`, `radius_km`, `push_enabled`) | `notification_prefs_own`; no row reads as the column defaults |

## Review fixes (apply after the seed)

`20261122000600_review_fixes.sql` comes from the independent security review:

- Closing a report by any path rejects its pending contact requests with "El caso se cerró",
  and a request on a closed report can no longer be accepted.
- Deleting an organization returns its affiliates to the `user` role instead of failing the
  `profiles_affiliate_has_org` check.

## Nearby report notifications (RF-03)

`20261122000800_nearby_report_notifications.sql` adds the trigger `reports_notify_nearby`
(after insert on `reports`):

- Each affiliate of an `approved` and `active` organization receives one `report`
  notification ("Nuevo reporte cercano", `report_id` set) when the organization is within
  that affiliate's `notification_prefs.radius_km` and the report's priority is in
  `notification_prefs.priorities`. No prefs row means 5 km and every priority.
- Body: `<Categoría> · prioridad <Prioridad> a <X.X> km: <descripción>`, labels as in
  `ui/common/Labels.kt`, description cut to 80 characters plus "…".
- The reporter is never notified of their own report; hidden reports notify nobody.
- Distance is haversine (`private.distance_km`), no PostGIS.
- `private.reports_track_status` now also skips the reporter's status-change notification
  when their prefs exclude the report's priority. Contact, adoption and affiliation
  decisions are always delivered.

## Regression probes

Run in the SQL Editor as `authenticated` (set `request.jwt.claims`) on a test project:

1. `update profiles set role = 'admin' where id = auth.uid()` → permission denied.
2. Anonymous claims (`"is_anonymous": true`): `send_contact_request`, `submit_adoption_request`,
   `request_affiliation`, `insert into favorites` → fail; `insert into reports` (own reporter, pending) → succeeds.
3. Anon role: `select phone from profiles` and `select * from contact_requests` → denied.
4. `insert into notifications` as authenticated → denied.
5. A third user calling `get_contact_details(<accepted id>)` → 0 rows; the requester of an
   organization-recipient request gets the organization's phone and email.
6. Two affiliates calling `take_report` on the same report → the second fails.
7. Storage: upload to `report-photos/<other uid>/x.jpg` → denied; a `text/plain` file → rejected.
8. `set_report_status(r, 'closed')`, then `respond_contact_request(<pending>, true)` → fails with "El caso se cerró".
9. As admin, `delete from organizations where id = <org with affiliates>` → succeeds; its affiliates become `user`.
10. Nearby reports (affiliates A of an approved organization ~1 km away, B of one ~20 km
    away, C near with `priorities = '{urgent}'`; claims of a reporter R): `insert into reports`
    with priority `high` → A has exactly one "Nuevo reporte cercano" for it; B and C have none.
11. Claims of an affiliate of the near organization: their own `insert into reports` → they get
    no notification for it. As service role, `insert into reports (..., hidden) values (..., true)`
    → no notification has that `report_id`.
12. A reporter with `priorities = '{low}'` whose `high` report changes status → no
    "Actualización de tu reporte"; a reporter without prefs row → one.

## Report flood protection (limits)

- Migrations 0900/1000 limit each reporter (including each anonymous session) to 5 reports
  every 10 minutes. A client that keeps creating new anonymous sessions gets a new `auth.uid()`
  each time, so the per-reporter limit alone does not stop it: keep Supabase's
  **Authentication → Rate Limits → anonymous sign-ins** limit enabled (default 30 per hour per IP).
- A failed nearby notification is logged with `RAISE WARNING` and does not cancel the report.
  Each affiliate notification runs in its own subtransaction; this is fine for the expected
  number of affiliates per area in Hermosillo.

## Push notifications (FCM contract for the Edge Function)

The app registers its FCM token in `device_tokens` (`platform = 'android'`) when an account signs
in or its session is restored, and again when FCM rotates the token. Signing out deletes this
device's row first. Anonymous sessions never register a token.

The Edge Function sends with the FCM HTTP v1 API to every `fcm_token` of the recipient. Send a
**data message** (no `notification` block) so the app builds the notification itself, with high
priority so it arrives while the app is closed:

```json
{
  "message": {
    "token": "<fcm_token>",
    "android": { "priority": "high" },
    "data": {
      "title": "Nuevo reporte cerca",
      "body": "Estado crítico a 1.2 km de tu organización",
      "report_id": "<reports.id uuid>"
    }
  }
}
```

- `title` (required) and `body` are shown as is on the "Avisos" channel (id `avisos`).
- `report_id` is optional; when present, tapping the notification opens that report: a user sees
  the report detail, an affiliate the case detail, an admin the supervision detail. Omit it for
  pushes not about a report (contact requests, affiliation, adoption requests); the tap then just
  opens the app.
- All `data` values are strings (FCM requirement).
- If a `notification` block is ever added, also set `android.notification.channel_id = "avisos"`
  and keep `report_id` in `data`, so the tap still opens the report.
- A token FCM reports with `errorCode: UNREGISTERED` (or `INVALID_ARGUMENT` about the token) is
  deleted from `device_tokens` by the function; a bare `404` keeps it.

The function is `functions/send-push`; its secrets, deploy command and Database Webhook setup
are in `functions/send-push/README.md`. Signing out also deletes the FCM token on the phone, so
a row left in `device_tokens` by a failed sign-out stops receiving pushes and is removed the
next time the function sends to it.

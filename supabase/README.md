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
| `review_affiliation` | `p_organization_id`, `p_approve` | Admin approves (promotes the requester) or rejects |

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

`review_affiliation` takes no rejection reason, so the app does not ask for one.

## Review fixes (apply after the seed)

`20261122000600_review_fixes.sql` comes from the independent security review:

- Closing a report by any path rejects its pending contact requests with "El caso se cerró",
  and a request on a closed report can no longer be accepted.
- Deleting an organization returns its affiliates to the `user` role instead of failing the
  `profiles_affiliate_has_org` check.

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

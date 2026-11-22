-- Fluffy PawTrack: Row Level Security on every table, table grants and guard triggers.
-- Apply after 20261122000100_schema.sql. Safe to re-run.
--
-- Model: RLS decides which rows each role reads and writes; guard triggers decide which
-- columns a direct API update may change; multi-row rules (contact and adoption requests,
-- taking a case, affiliations) go through the security definer RPCs in
-- 20261122000300_functions.sql. Security definer functions run as their owner, so the
-- guards below only apply to direct calls made as `anon` or `authenticated`.

-- ---------------------------------------------------------------------------
-- Helper functions used by the policies
-- ---------------------------------------------------------------------------

-- True for Supabase anonymous sign-in sessions (they still use the `authenticated` role).
create or replace function public.is_anonymous()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false);
$$;

-- Role of the caller's profile; null without a session.
create or replace function public.current_user_role()
returns public.user_role
language sql
stable
security definer
set search_path = ''
as $$
    select p.role from public.profiles p where p.id = auth.uid();
$$;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select coalesce(public.current_user_role() = 'admin', false);
$$;

-- A signed-in, non-anonymous account with role `user` (the only one that may send contact
-- and adoption requests, RF-05 and RF-06).
create or replace function public.is_regular_user()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select auth.uid() is not null
        and not public.is_anonymous()
        and coalesce(public.current_user_role() = 'user', false);
$$;

-- Organization of the caller when it is an affiliate of an approved organization.
create or replace function public.my_organization_id()
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
    select p.organization_id
    from public.profiles p
    join public.organizations o on o.id = p.organization_id
    where p.id = auth.uid() and p.role = 'affiliate' and o.status = 'approved';
$$;

create or replace function public.is_affiliate_of(org_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select org_id is not null and coalesce(public.my_organization_id() = org_id, false);
$$;

-- ---------------------------------------------------------------------------
-- Enable RLS on every table
-- ---------------------------------------------------------------------------

alter table public.profiles enable row level security;
alter table public.organizations enable row level security;
alter table public.reports enable row level security;
alter table public.report_status_history enable row level security;
alter table public.adoption_listings enable row level security;
alter table public.adoption_requests enable row level security;
alter table public.contact_requests enable row level security;
alter table public.notifications enable row level security;
alter table public.notification_prefs enable row level security;
alter table public.device_tokens enable row level security;
alter table public.favorites enable row level security;

-- ---------------------------------------------------------------------------
-- Table grants (RLS still filters rows; grants limit operations and columns)
-- ---------------------------------------------------------------------------

revoke all on public.profiles, public.organizations, public.reports, public.report_status_history,
    public.adoption_listings, public.adoption_requests, public.contact_requests, public.notifications,
    public.notification_prefs, public.device_tokens, public.favorites
    from anon, authenticated;

-- Public catalog: readable without an account.
grant select on public.organizations, public.reports, public.report_status_history, public.adoption_listings
    to anon, authenticated;

-- Profiles: no insert (the sign-up trigger creates them); role and organization_id are not updatable.
grant select on public.profiles to authenticated;
grant update (first_name, last_name, phone, avatar_url) on public.profiles to authenticated;

grant insert, update, delete on public.organizations to authenticated;
grant insert, update, delete on public.reports to authenticated;
grant insert, update, delete on public.adoption_listings to authenticated;

-- Requests: inserts of adoption requests are allowed by policy; every status change goes
-- through the RPCs. Contact requests are written only by the RPCs.
grant select, insert on public.adoption_requests to authenticated;
grant select on public.contact_requests to authenticated;

-- Notifications are created by the server; the owner may only mark them read or delete them.
grant select, delete on public.notifications to authenticated;
grant update (read) on public.notifications to authenticated;

grant select, insert, update, delete on public.notification_prefs to authenticated;
grant select, insert, update, delete on public.device_tokens to authenticated;
grant select, insert, delete on public.favorites to authenticated;

-- ---------------------------------------------------------------------------
-- profiles
-- ---------------------------------------------------------------------------

drop policy if exists profiles_select_own on public.profiles;
create policy profiles_select_own on public.profiles
    for select to authenticated
    using (id = (select auth.uid()));

drop policy if exists profiles_select_admin on public.profiles;
create policy profiles_select_admin on public.profiles
    for select to authenticated
    using ((select public.is_admin()));

drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own on public.profiles
    for update to authenticated
    using (id = (select auth.uid()))
    with check (id = (select auth.uid()));

-- ---------------------------------------------------------------------------
-- organizations
-- ---------------------------------------------------------------------------

drop policy if exists organizations_select_public on public.organizations;
create policy organizations_select_public on public.organizations
    for select to anon, authenticated
    using (status = 'approved' and active);

drop policy if exists organizations_select_requester on public.organizations;
create policy organizations_select_requester on public.organizations
    for select to authenticated
    using (requested_by = (select auth.uid()));

drop policy if exists organizations_select_member on public.organizations;
create policy organizations_select_member on public.organizations
    for select to authenticated
    using (public.is_affiliate_of(id));

drop policy if exists organizations_select_admin on public.organizations;
create policy organizations_select_admin on public.organizations
    for select to authenticated
    using ((select public.is_admin()));

-- Affiliation request: a signed-in, non-anonymous account creates a pending organization.
drop policy if exists organizations_insert_affiliation on public.organizations;
create policy organizations_insert_affiliation on public.organizations
    for insert to authenticated
    with check (
        requested_by = (select auth.uid())
        and status = 'pending'
        and not (select public.is_anonymous())
    );

drop policy if exists organizations_insert_admin on public.organizations;
create policy organizations_insert_admin on public.organizations
    for insert to authenticated
    with check ((select public.is_admin()));

-- Affiliates edit their organization's profile; the guard trigger keeps type, status,
-- active and requested_by admin-only.
drop policy if exists organizations_update_member on public.organizations;
create policy organizations_update_member on public.organizations
    for update to authenticated
    using (public.is_affiliate_of(id))
    with check (public.is_affiliate_of(id));

drop policy if exists organizations_update_admin on public.organizations;
create policy organizations_update_admin on public.organizations
    for update to authenticated
    using ((select public.is_admin()))
    with check ((select public.is_admin()));

drop policy if exists organizations_delete_admin on public.organizations;
create policy organizations_delete_admin on public.organizations
    for delete to authenticated
    using ((select public.is_admin()));

create or replace function public.organizations_guard_update()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if current_user not in ('anon', 'authenticated') or public.is_admin() then
        return new;
    end if;
    if (to_jsonb(new) - array['name', 'description', 'photo_url', 'lat', 'lng', 'address', 'schedule',
                              'capacity', 'phone', 'email', 'social'])
       is distinct from
       (to_jsonb(old) - array['name', 'description', 'photo_url', 'lat', 'lng', 'address', 'schedule',
                              'capacity', 'phone', 'email', 'social']) then
        raise exception 'Solo el administrador puede cambiar el tipo, el estado o la visibilidad de la organización';
    end if;
    return new;
end;
$$;

drop trigger if exists organizations_guard_update on public.organizations;
create trigger organizations_guard_update
    before update on public.organizations
    for each row execute function public.organizations_guard_update();

-- ---------------------------------------------------------------------------
-- reports
-- ---------------------------------------------------------------------------

drop policy if exists reports_select_visible on public.reports;
create policy reports_select_visible on public.reports
    for select to anon, authenticated
    using (not hidden);

drop policy if exists reports_select_own on public.reports;
create policy reports_select_own on public.reports
    for select to authenticated
    using (reporter_id = (select auth.uid()));

drop policy if exists reports_select_admin on public.reports;
create policy reports_select_admin on public.reports
    for select to authenticated
    using ((select public.is_admin()));

-- Anyone with a session (anonymous sign-in included, RF-01) reports as themselves.
drop policy if exists reports_insert_own on public.reports;
create policy reports_insert_own on public.reports
    for insert to authenticated
    with check (
        reporter_id = (select auth.uid())
        and status = 'pending'
        and assigned_org_id is null
        and not hidden
    );

-- The reporter edits description and reference while the report is pending (guard trigger).
drop policy if exists reports_update_owner on public.reports;
create policy reports_update_owner on public.reports
    for update to authenticated
    using (reporter_id = (select auth.uid()) and status = 'pending')
    with check (reporter_id = (select auth.uid()));

-- Affiliates of the assigned organization move the status forward (guard trigger).
drop policy if exists reports_update_assigned_org on public.reports;
create policy reports_update_assigned_org on public.reports
    for update to authenticated
    using (public.is_affiliate_of(assigned_org_id))
    with check (public.is_affiliate_of(assigned_org_id));

-- RF-07: the admin hides false reports and closes cases.
drop policy if exists reports_update_admin on public.reports;
create policy reports_update_admin on public.reports
    for update to authenticated
    using ((select public.is_admin()))
    with check ((select public.is_admin()));

drop policy if exists reports_delete_owner on public.reports;
create policy reports_delete_owner on public.reports
    for delete to authenticated
    using (reporter_id = (select auth.uid()));

drop policy if exists reports_delete_admin on public.reports;
create policy reports_delete_admin on public.reports
    for delete to authenticated
    using ((select public.is_admin()));

-- Marks reports created from an anonymous session; the client cannot set the flag.
create or replace function public.reports_before_insert()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if current_user in ('anon', 'authenticated') then
        new.reporter_is_anonymous := public.is_anonymous();
    end if;
    return new;
end;
$$;

drop trigger if exists reports_before_insert on public.reports;
create trigger reports_before_insert
    before insert on public.reports
    for each row execute function public.reports_before_insert();

create or replace function public.reports_guard_update()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
    unchanged_except_text boolean;
    unchanged_except_status boolean;
begin
    if current_user not in ('anon', 'authenticated') or public.is_admin() then
        return new;
    end if;

    unchanged_except_text :=
        (to_jsonb(new) - array['description', 'reference', 'updated_at'])
        = (to_jsonb(old) - array['description', 'reference', 'updated_at']);
    unchanged_except_status :=
        (to_jsonb(new) - array['status', 'updated_at']) = (to_jsonb(old) - array['status', 'updated_at']);

    if old.reporter_id = auth.uid() and old.status = 'pending' and unchanged_except_text then
        return new;
    end if;

    if public.is_affiliate_of(old.assigned_org_id) and unchanged_except_status then
        if new.status < old.status then
            raise exception 'El estado del reporte solo puede avanzar';
        end if;
        return new;
    end if;

    raise exception 'No tienes permiso para hacer este cambio en el reporte';
end;
$$;

drop trigger if exists reports_guard_update on public.reports;
create trigger reports_guard_update
    before update on public.reports
    for each row execute function public.reports_guard_update();

-- ---------------------------------------------------------------------------
-- report_status_history (written only by triggers)
-- ---------------------------------------------------------------------------

drop policy if exists report_status_history_select on public.report_status_history;
create policy report_status_history_select on public.report_status_history
    for select to anon, authenticated
    using (exists (select 1 from public.reports r where r.id = report_id));

-- ---------------------------------------------------------------------------
-- adoption_listings
-- ---------------------------------------------------------------------------

-- Listings are public catalog data; adopted ones stay readable so the requester's history
-- can show the animal. The app filters `status = 'available'` for browsing.
drop policy if exists adoption_listings_select_all on public.adoption_listings;
create policy adoption_listings_select_all on public.adoption_listings
    for select to anon, authenticated
    using (true);

-- RF-06: affiliates of an approved adoption center create listings for their center.
drop policy if exists adoption_listings_insert_center on public.adoption_listings;
create policy adoption_listings_insert_center on public.adoption_listings
    for insert to authenticated
    with check (
        public.is_affiliate_of(organization_id)
        and exists (
            select 1 from public.organizations o
            where o.id = organization_id and o.type = 'adoption_center'
        )
    );

drop policy if exists adoption_listings_update_center on public.adoption_listings;
create policy adoption_listings_update_center on public.adoption_listings
    for update to authenticated
    using (public.is_affiliate_of(organization_id))
    with check (public.is_affiliate_of(organization_id));

drop policy if exists adoption_listings_delete_center on public.adoption_listings;
create policy adoption_listings_delete_center on public.adoption_listings
    for delete to authenticated
    using (public.is_affiliate_of(organization_id));

drop policy if exists adoption_listings_admin on public.adoption_listings;
create policy adoption_listings_admin on public.adoption_listings
    for all to authenticated
    using ((select public.is_admin()))
    with check ((select public.is_admin()));

-- ---------------------------------------------------------------------------
-- adoption_requests
-- ---------------------------------------------------------------------------

drop policy if exists adoption_requests_select_requester on public.adoption_requests;
create policy adoption_requests_select_requester on public.adoption_requests
    for select to authenticated
    using (requester_id = (select auth.uid()));

drop policy if exists adoption_requests_select_center on public.adoption_requests;
create policy adoption_requests_select_center on public.adoption_requests
    for select to authenticated
    using (public.is_affiliate_of(organization_id));

-- RF-06: only a non-anonymous `user` submits, for an available animal of that center, as
-- a fresh pending request. The partial unique index keeps one active request per animal.
drop policy if exists adoption_requests_insert_user on public.adoption_requests;
create policy adoption_requests_insert_user on public.adoption_requests
    for insert to authenticated
    with check (
        requester_id = (select auth.uid())
        and (select public.is_regular_user())
        and status = 'pending'
        and rejection_reason is null
        and responded_at is null
        and exists (
            select 1 from public.adoption_listings l
            where l.id = listing_id and l.organization_id = adoption_requests.organization_id
                and l.status = 'available'
        )
    );

-- ---------------------------------------------------------------------------
-- contact_requests (RF-05 "Reglas técnicas": the requester sees what they sent; the
-- recipient sees the requests of their reports or of their organization)
-- ---------------------------------------------------------------------------

drop policy if exists contact_requests_select_requester on public.contact_requests;
create policy contact_requests_select_requester on public.contact_requests
    for select to authenticated
    using (requester_id = (select auth.uid()));

drop policy if exists contact_requests_select_recipient_user on public.contact_requests;
create policy contact_requests_select_recipient_user on public.contact_requests
    for select to authenticated
    using (recipient_user_id = (select auth.uid()));

drop policy if exists contact_requests_select_recipient_org on public.contact_requests;
create policy contact_requests_select_recipient_org on public.contact_requests
    for select to authenticated
    using (public.is_affiliate_of(recipient_org_id));

-- ---------------------------------------------------------------------------
-- notifications, notification_prefs, device_tokens, favorites (own rows)
-- ---------------------------------------------------------------------------

drop policy if exists notifications_select_own on public.notifications;
create policy notifications_select_own on public.notifications
    for select to authenticated
    using (user_id = (select auth.uid()));

drop policy if exists notifications_update_own on public.notifications;
create policy notifications_update_own on public.notifications
    for update to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

drop policy if exists notifications_delete_own on public.notifications;
create policy notifications_delete_own on public.notifications
    for delete to authenticated
    using (user_id = (select auth.uid()));

drop policy if exists notification_prefs_own on public.notification_prefs;
create policy notification_prefs_own on public.notification_prefs
    for all to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

drop policy if exists device_tokens_own on public.device_tokens;
create policy device_tokens_own on public.device_tokens
    for all to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

drop policy if exists favorites_select_own on public.favorites;
create policy favorites_select_own on public.favorites
    for select to authenticated
    using (user_id = (select auth.uid()));

-- Favorites need an account (section 4): anonymous sessions cannot save them.
drop policy if exists favorites_insert_own on public.favorites;
create policy favorites_insert_own on public.favorites
    for insert to authenticated
    with check (user_id = (select auth.uid()) and not (select public.is_anonymous()));

drop policy if exists favorites_delete_own on public.favorites;
create policy favorites_delete_own on public.favorites
    for delete to authenticated
    using (user_id = (select auth.uid()));

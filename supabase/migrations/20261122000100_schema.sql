-- Fluffy PawTrack: enums, tables, constraints and the profile-on-signup trigger.
-- Data model: docs/REQUIREMENTS.md section 7. Enum values map 1:1 to the Kotlin enums in
-- shared/.../domain/model (see supabase/README.md, "Enum mapping").
-- Apply first. Safe to re-run: every object is created only when missing.

-- ---------------------------------------------------------------------------
-- Enums
-- ---------------------------------------------------------------------------

do $$
begin
    create type public.user_role as enum ('user', 'affiliate', 'admin');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.organization_type as enum ('shelter', 'adoption_center', 'vet', 'safe_point');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.organization_status as enum ('pending', 'approved', 'rejected');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.report_category as enum (
        'street', 'possibly_lost', 'mistreated', 'injured', 'critical', 'abandoned'
    );
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.report_priority as enum ('low', 'medium', 'high', 'urgent');
exception when duplicate_object then null;
end $$;

-- Rescue lifecycle (RF-04): pending -> on_the_way -> sheltered -> closed.
do $$
begin
    create type public.report_status as enum ('pending', 'on_the_way', 'sheltered', 'closed');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.listing_status as enum ('available', 'adopted');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.housing_type as enum ('house', 'apartment');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.housing_tenure as enum ('owned', 'rented');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.adoption_request_status as enum ('pending', 'approved', 'rejected', 'cancelled');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.contact_request_status as enum ('pending', 'accepted', 'rejected', 'cancelled');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.notification_type as enum ('report', 'adoption', 'favorite', 'general');
exception when duplicate_object then null;
end $$;

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

-- Places on the map and affiliate organizations (RF-07). `requested_by` is the account that
-- asked for the affiliation; it becomes an affiliate when the admin approves.
create table if not exists public.organizations (
    id uuid primary key default gen_random_uuid(),
    type public.organization_type not null,
    name text not null check (btrim(name) <> ''),
    description text not null default '',
    photo_url text not null check (btrim(photo_url) <> ''),
    lat double precision not null check (lat between -90 and 90),
    lng double precision not null check (lng between -180 and 180),
    address text not null default '',
    schedule text not null default '',
    capacity integer check (capacity is null or capacity >= 0),
    phone text not null default '',
    email text not null default '',
    social text,
    status public.organization_status not null default 'pending',
    active boolean not null default true,
    requested_by uuid references auth.users (id) on delete set null,
    created_at timestamptz not null default now()
);

-- One row per auth user, created by the trigger below. The email lives in auth.users.
create table if not exists public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    first_name text not null default '',
    last_name text not null default '',
    -- Required before sending or accepting contact requests (RF-05); checked by the RPCs.
    phone text,
    avatar_url text,
    role public.user_role not null default 'user',
    organization_id uuid references public.organizations (id) on delete set null,
    created_at timestamptz not null default now(),
    constraint profiles_affiliate_has_org check (role <> 'affiliate' or organization_id is not null)
);

create table if not exists public.reports (
    id uuid primary key default gen_random_uuid(),
    -- auth.uid() of the session that created it (also for anonymous sessions); null for seed data.
    reporter_id uuid references auth.users (id) on delete set null,
    -- True when it was created from an anonymous session: such a report has no contact
    -- recipient until an organization takes it (RF-05). Set by a trigger, never by the client.
    reporter_is_anonymous boolean not null default false,
    category public.report_category not null,
    priority public.report_priority not null,
    description text not null default '',
    photo_url text not null check (btrim(photo_url) <> ''),
    lat double precision not null check (lat between -90 and 90),
    lng double precision not null check (lng between -180 and 180),
    -- Text address from the Android Geocoder; null when it could not be resolved.
    address text,
    reference text,
    status public.report_status not null default 'pending',
    assigned_org_id uuid references public.organizations (id) on delete set null,
    hidden boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    -- RF-01: "Estado crítico" is always urgent.
    constraint reports_critical_is_urgent check (category <> 'critical' or priority = 'urgent'),
    -- RF-04: once a case is taken it belongs to one organization.
    constraint reports_taken_has_org check (status in ('pending', 'closed') or assigned_org_id is not null)
);

create index if not exists reports_reporter_idx on public.reports (reporter_id);
create index if not exists reports_assigned_org_idx on public.reports (assigned_org_id);
create index if not exists reports_status_idx on public.reports (status) where not hidden;

create table if not exists public.report_status_history (
    id uuid primary key default gen_random_uuid(),
    report_id uuid not null references public.reports (id) on delete cascade,
    status public.report_status not null,
    changed_by uuid references auth.users (id) on delete set null,
    created_at timestamptz not null default now()
);

create index if not exists report_status_history_report_idx on public.report_status_history (report_id);

create table if not exists public.adoption_listings (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations (id) on delete cascade,
    name text not null check (btrim(name) <> ''),
    species text not null check (btrim(species) <> ''),
    breed text not null check (btrim(breed) <> ''),
    age text not null check (btrim(age) <> ''),
    vaccination text not null check (btrim(vaccination) <> ''),
    description text not null check (btrim(description) <> ''),
    -- RF-06: at least one photo, none blank. The first one is the cover.
    photo_urls text[] not null
        check (cardinality(photo_urls) >= 1 and array_position(photo_urls, '') is null
               and array_position(photo_urls, null) is null),
    status public.listing_status not null default 'available',
    created_at timestamptz not null default now()
);

create index if not exists adoption_listings_org_idx on public.adoption_listings (organization_id);

create table if not exists public.adoption_requests (
    id uuid primary key default gen_random_uuid(),
    listing_id uuid not null references public.adoption_listings (id) on delete cascade,
    organization_id uuid not null references public.organizations (id) on delete cascade,
    requester_id uuid not null references auth.users (id) on delete cascade,
    full_name text not null check (btrim(full_name) <> ''),
    phone text not null check (btrim(phone) <> ''),
    email text not null check (btrim(email) <> ''),
    housing_type public.housing_type not null,
    housing_tenure public.housing_tenure not null,
    has_other_pets boolean not null default false,
    other_pets text,
    reason text not null check (btrim(reason) <> ''),
    status public.adoption_request_status not null default 'pending',
    rejection_reason text,
    created_at timestamptz not null default now(),
    responded_at timestamptz,
    constraint adoption_requests_other_pets check (not has_other_pets or btrim(coalesce(other_pets, '')) <> ''),
    constraint adoption_requests_rejection_reason
        check (status <> 'rejected' or btrim(coalesce(rejection_reason, '')) <> '')
);

-- RF-06: at most one active (pending or approved) request per (listing, requester).
create unique index if not exists adoption_requests_one_active_idx
    on public.adoption_requests (listing_id, requester_id)
    where status in ('pending', 'approved');
create index if not exists adoption_requests_org_idx on public.adoption_requests (organization_id);
create index if not exists adoption_requests_requester_idx on public.adoption_requests (requester_id);

create table if not exists public.contact_requests (
    id uuid primary key default gen_random_uuid(),
    -- Null only after the report was deleted (its pending requests were rejected first).
    report_id uuid references public.reports (id) on delete set null,
    requester_id uuid not null references auth.users (id) on delete cascade,
    -- Only the first name is visible to the recipient before acceptance (RF-05).
    requester_first_name text not null default '',
    recipient_user_id uuid references auth.users (id) on delete cascade,
    recipient_org_id uuid references public.organizations (id) on delete cascade,
    message text not null check (char_length(btrim(message)) between 20 and 500),
    photo_url text,
    status public.contact_request_status not null default 'pending',
    status_note text,
    created_at timestamptz not null default now(),
    responded_at timestamptz,
    -- RF-05: the recipient is either the assigned organization or the reporter, never both.
    constraint contact_requests_one_recipient check (num_nonnulls(recipient_user_id, recipient_org_id) = 1)
);

-- RF-05: at most one active (pending or accepted) request per (report, requester).
create unique index if not exists contact_requests_one_active_idx
    on public.contact_requests (report_id, requester_id)
    where status in ('pending', 'accepted');
create index if not exists contact_requests_requester_idx on public.contact_requests (requester_id);
create index if not exists contact_requests_recipient_user_idx on public.contact_requests (recipient_user_id);
create index if not exists contact_requests_recipient_org_idx on public.contact_requests (recipient_org_id);

create table if not exists public.notifications (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    type public.notification_type not null default 'general',
    title text not null,
    body text not null,
    report_id uuid references public.reports (id) on delete set null,
    read boolean not null default false,
    created_at timestamptz not null default now()
);

create index if not exists notifications_user_idx on public.notifications (user_id, created_at desc);

create table if not exists public.notification_prefs (
    user_id uuid primary key references auth.users (id) on delete cascade,
    priorities public.report_priority[] not null default array['low', 'medium', 'high', 'urgent']::public.report_priority[],
    radius_km numeric(6, 2) not null default 5 check (radius_km > 0),
    push_enabled boolean not null default true,
    updated_at timestamptz not null default now()
);

create table if not exists public.device_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    fcm_token text not null check (btrim(fcm_token) <> ''),
    platform text not null default 'android' check (platform = 'android'),
    updated_at timestamptz not null default now(),
    constraint device_tokens_user_token_key unique (user_id, fcm_token)
);

create table if not exists public.favorites (
    user_id uuid not null references auth.users (id) on delete cascade,
    listing_id uuid not null references public.adoption_listings (id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, listing_id)
);

-- ---------------------------------------------------------------------------
-- updated_at maintenance
-- ---------------------------------------------------------------------------

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

drop trigger if exists reports_set_updated_at on public.reports;
create trigger reports_set_updated_at
    before update on public.reports
    for each row execute function public.set_updated_at();

drop trigger if exists notification_prefs_set_updated_at on public.notification_prefs;
create trigger notification_prefs_set_updated_at
    before update on public.notification_prefs
    for each row execute function public.set_updated_at();

drop trigger if exists device_tokens_set_updated_at on public.device_tokens;
create trigger device_tokens_set_updated_at
    before update on public.device_tokens
    for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- Profile on sign-up
-- ---------------------------------------------------------------------------

-- Every new auth user (anonymous sessions included) gets a `user` profile. The app sends
-- first_name, last_name and phone in the sign-up `data` (raw_user_meta_data).
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.profiles (id, first_name, last_name, phone, role)
    values (
        new.id,
        coalesce(btrim(new.raw_user_meta_data ->> 'first_name'), ''),
        coalesce(btrim(new.raw_user_meta_data ->> 'last_name'), ''),
        nullif(btrim(new.raw_user_meta_data ->> 'phone'), ''),
        'user'
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------------
-- Realtime: live notifications and map updates (RF-03)
-- ---------------------------------------------------------------------------

do $$
begin
    if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
        if not exists (
            select 1 from pg_publication_tables
            where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'notifications'
        ) then
            alter publication supabase_realtime add table public.notifications;
        end if;
        if not exists (
            select 1 from pg_publication_tables
            where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'reports'
        ) then
            alter publication supabase_realtime add table public.reports;
        end if;
    end if;
end $$;

-- RF-03: every new report notifies the affiliates whose organization is within their radius,
-- and report notifications respect each person's chosen priorities (notification_prefs).
-- Apply after 20261122000700_affiliation_rejection_reason.sql. Safe to re-run.

-- Great-circle distance in kilometres (haversine, Earth radius 6371 km). No PostGIS needed.
create or replace function private.distance_km(
    lat1 double precision,
    lng1 double precision,
    lat2 double precision,
    lng2 double precision
)
returns double precision
language sql
immutable
set search_path = ''
as $$
    select 2 * 6371 * asin(least(1, sqrt(
        power(sin(radians(lat2 - lat1) / 2), 2)
        + cos(radians(lat1)) * cos(radians(lat2)) * power(sin(radians(lng2 - lng1) / 2), 2)
    )));
$$;

-- True when [p_user_id] accepts [p_priority]. No notification_prefs row means every priority.
create or replace function private.accepts_priority(p_user_id uuid, p_priority public.report_priority)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select coalesce(
        (select p_priority = any (np.priorities) from public.notification_prefs np where np.user_id = p_user_id),
        true
    );
$$;

-- Spanish labels, same text as ui/common/Labels.kt.
create or replace function private.report_category_label(p_category public.report_category)
returns text
language sql
immutable
set search_path = ''
as $$
    select case p_category
        when 'street' then 'Situación de calle'
        when 'possibly_lost' then 'Posible mascota extraviada'
        when 'mistreated' then 'Maltratado'
        when 'injured' then 'Herido o lesionado'
        when 'critical' then 'Estado crítico'
        when 'abandoned' then 'Abandono'
    end;
$$;

create or replace function private.report_priority_label(p_priority public.report_priority)
returns text
language sql
immutable
set search_path = ''
as $$
    select case p_priority
        when 'low' then 'Baja'
        when 'medium' then 'Media'
        when 'high' then 'Alta'
        when 'urgent' then 'Urgente'
    end;
$$;

-- Each affiliate of an approved, active organization within that affiliate's radius
-- (default 5 km) and accepting the report's priority gets one notification. The reporter
-- is never notified of their own report, and hidden reports notify nobody.
create or replace function private.reports_notify_nearby()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    target record;
    description text := btrim(coalesce(new.description, ''));
begin
    if new.hidden then
        return null;
    end if;
    if char_length(description) > 80 then
        description := left(description, 80) || '…';
    end if;

    for target in
        select p.id as user_id,
               min(private.distance_km(o.lat, o.lng, new.lat, new.lng)) as distance
        from public.profiles p
        join public.organizations o on o.id = p.organization_id
        left join public.notification_prefs np on np.user_id = p.id
        where p.role = 'affiliate'
          and o.status = 'approved'
          and o.active
          and p.id is distinct from new.reporter_id
          and new.priority = any (coalesce(np.priorities,
                array['low', 'medium', 'high', 'urgent']::public.report_priority[]))
          and private.distance_km(o.lat, o.lng, new.lat, new.lng) <= coalesce(np.radius_km, 5)
        group by p.id
    loop
        perform private.notify(
            target.user_id,
            'report',
            'Nuevo reporte cercano',
            private.report_category_label(new.category)
                || ' · prioridad ' || private.report_priority_label(new.priority)
                || ' a ' || round(target.distance::numeric, 1)::text || ' km'
                || case when description = '' then '' else ': ' || description end,
            new.id
        );
    end loop;
    return null;
end;
$$;

drop trigger if exists reports_notify_nearby on public.reports;
create trigger reports_notify_nearby
    after insert on public.reports
    for each row execute function private.reports_notify_nearby();

-- Same as 20261122000300_functions.sql, plus one condition: the reporter is notified of a
-- status change only when their notification_prefs accept the report's priority.
create or replace function private.reports_track_status()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if tg_op = 'INSERT' or new.status is distinct from old.status then
        insert into public.report_status_history (report_id, status, changed_by)
        values (new.id, new.status, auth.uid());
    end if;

    if tg_op = 'UPDATE' and new.status is distinct from old.status
       and new.reporter_id is not null and not new.reporter_is_anonymous
       and new.reporter_id is distinct from auth.uid()
       and private.accepts_priority(new.reporter_id, new.priority) then
        perform private.notify(
            new.reporter_id,
            'report',
            'Actualización de tu reporte',
            'Tu reporte ahora está en "' || private.report_status_label(new.status) || '".',
            new.id
        );
    end if;
    return null;
end;
$$;

revoke all on function private.distance_km(double precision, double precision, double precision, double precision) from public;
revoke all on function private.accepts_priority(uuid, public.report_priority) from public;
revoke all on function private.report_category_label(public.report_category) from public;
revoke all on function private.report_priority_label(public.report_priority) from public;
revoke all on function private.reports_notify_nearby() from public;
revoke all on function private.reports_track_status() from public;

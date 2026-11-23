-- Follow-up to 20261122000800 from its review:
-- 1. A failure while notifying one affiliate must never roll back the citizen's report.
-- 2. A reporter (including anonymous sessions) cannot flood nearby affiliates: at most
--    5 reports per 10 minutes per reporter. Admins are exempt.

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
        begin
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
        exception when others then
            -- Never lose the report because one notification failed.
            raise warning 'reports_notify_nearby: could not notify %: %', target.user_id, sqlerrm;
        end;
    end loop;
    return null;
end;
$$;

create or replace function private.reports_rate_limit()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.reporter_id is null or public.is_admin() then
        return new;
    end if;
    if (select count(*) from public.reports r
        where r.reporter_id = new.reporter_id
          and r.created_at > now() - interval '10 minutes') >= 5 then
        raise exception 'Has enviado muchos reportes en poco tiempo. Intenta de nuevo en unos minutos.';
    end if;
    return new;
end;
$$;

drop trigger if exists reports_rate_limit on public.reports;
create trigger reports_rate_limit
    before insert on public.reports
    for each row execute function private.reports_rate_limit();

revoke all on function private.reports_rate_limit() from public;

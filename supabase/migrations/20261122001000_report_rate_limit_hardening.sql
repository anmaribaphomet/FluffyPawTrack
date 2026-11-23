-- Hardens the report rate limit from 20261122000900 after its review:
-- * concurrent inserts from one reporter are serialized with a transaction-scoped advisory
--   lock, so a burst of parallel requests cannot all pass the count;
-- * created_at is always set by the server, so a client cannot back-date rows to escape
--   the 10-minute window;
-- * reporter_id is never null for client inserts (policy reports_insert_own requires
--   reporter_id = auth.uid(), anonymous sessions included); null only comes from seed or
--   SQL run by the project owner, which is why it is not limited.

create or replace function private.reports_rate_limit()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    -- Seed rows (no reporter) keep their sample dates.
    if new.reporter_id is null then
        return new;
    end if;
    new.created_at := now();
    if public.is_admin() then
        return new;
    end if;
    perform pg_advisory_xact_lock(hashtextextended('reports_rate_limit:' || new.reporter_id::text, 0));
    if (select count(*) from public.reports r
        where r.reporter_id = new.reporter_id
          and r.created_at > now() - interval '10 minutes') >= 5 then
        raise exception 'Has enviado muchos reportes en poco tiempo. Intenta de nuevo en unos minutos.';
    end if;
    return new;
end;
$$;

revoke all on function private.reports_rate_limit() from public;

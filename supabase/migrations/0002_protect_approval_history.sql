-- Tighten approval-ledger permissions. A user may submit and decide their own
-- proposals, but cannot rewrite their history, payload, owner or reopen a decision.
drop policy if exists "Users manage their own approvals" on public.pending_approvals;
drop policy if exists "Users read their own approvals" on public.pending_approvals;
drop policy if exists "Users create their own approvals" on public.pending_approvals;
drop policy if exists "Users decide their own pending approvals" on public.pending_approvals;

create policy "Users read their own approvals"
  on public.pending_approvals for select
  using (auth.uid() = user_id);

create policy "Users create their own approvals"
  on public.pending_approvals for insert
  with check (auth.uid() = user_id and status = 'PENDING' and decided_at is null);

create policy "Users decide their own pending approvals"
  on public.pending_approvals for update
  using (auth.uid() = user_id and status = 'PENDING')
  with check (
    auth.uid() = user_id
    and status in ('APPROVED', 'REJECTED', 'CANCELLED')
    and decided_at is not null
  );

create or replace function public.protect_approval_history()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  if new.id <> old.id
     or new.user_id <> old.user_id
     or new.action_type <> old.action_type
     or new.payload <> old.payload
     or new.created_at <> old.created_at
     or old.status <> 'PENDING'
     or new.status not in ('APPROVED', 'REJECTED', 'CANCELLED')
     or new.decided_at is null then
    raise exception 'Approval records are immutable; only a pending decision may be finalised';
  end if;
  return new;
end;
$$;

drop trigger if exists protect_approval_history on public.pending_approvals;
create trigger protect_approval_history
before update on public.pending_approvals
for each row execute function public.protect_approval_history();

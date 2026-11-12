create extension if not exists pgcrypto;

create table if not exists public.squad_players (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (length(trim(name)) between 1 and 80),
  position text not null check (position in ('GK','CB','LB','RB','CDM','CM','CAM','LM','RM','LW','RW','ST','CF')),
  rating integer not null check (rating between 1 and 100),
  created_at timestamptz not null default now(),
  unique (id, user_id)
);
create table if not exists public.match_records (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  played_at timestamptz not null default now(),
  result text not null check (result in ('W','D','L')),
  goals_for integer not null check (goals_for >= 0),
  goals_against integer not null check (goals_against >= 0),
  notes text not null default '',
  created_at timestamptz not null default now(),
  unique (id, user_id)
);
create table if not exists public.pending_approvals (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  action_type text not null check (action_type in ('BUY_PLAYER','RELEASE_PLAYER','SPEND_PREMIUM_CURRENCY','ACCOUNT_CHANGE')),
  payload jsonb not null default '{}'::jsonb,
  status text not null default 'PENDING' check (status in ('PENDING','APPROVED','REJECTED','CANCELLED')),
  created_at timestamptz not null default now(),
  decided_at timestamptz
);
alter table public.squad_players enable row level security;
alter table public.match_records enable row level security;
alter table public.pending_approvals enable row level security;
drop policy if exists "Users manage their own squad" on public.squad_players;
create policy "Users manage their own squad" on public.squad_players for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists "Users manage their own matches" on public.match_records;
create policy "Users manage their own matches" on public.match_records for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists "Users manage their own approvals" on public.pending_approvals;
create policy "Users manage their own approvals" on public.pending_approvals for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

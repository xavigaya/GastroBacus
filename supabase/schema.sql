-- GastroBacus: esquema per a Supabase (fase 2: dades compartides entre la tauleta i els mòbils)
-- S'executa a Supabase > SQL Editor. Encara no està connectat a l'app: la primera versió desa les dades al navegador.

create extension if not exists pgcrypto;

create table socis (
  id uuid primary key default gen_random_uuid(),
  auth_user uuid unique references auth.users(id) on delete set null, -- compte per entrar des del mòbil
  nom text not null,
  cognoms text not null default '',
  email text not null unique,
  telefon text,
  rol text not null default 'soci' check (rol in ('soci', 'junta')),
  pin_hash text not null, -- crypt(pin, gen_salt('bf'))
  ref_banc text not null default '',
  actiu boolean not null default true,
  creat timestamptz not null default now()
);

create table productes (
  id uuid primary key default gen_random_uuid(),
  nom text not null,
  categoria text not null,
  preu numeric(8,2) not null check (preu >= 0),
  preu_convidat numeric(8,2),
  estoc integer not null default 0,
  estoc_minim integer not null default 0,
  controla_estoc boolean not null default true,
  actiu boolean not null default true
);

create table consums (
  id uuid primary key default gen_random_uuid(),
  soci_id uuid not null references socis(id),
  producte_id uuid not null references productes(id),
  nom_producte text not null,
  quantitat integer not null check (quantitat > 0),
  preu_unitari numeric(8,2) not null,
  convidat text,
  ts timestamptz not null default now(),
  origen text not null check (origen in ('tauleta', 'mobil', 'junta')),
  anulat_ts timestamptz,
  anulat_per uuid references socis(id)
);
create index on consums (soci_id, ts);
create index on consums (ts);

create table reposicions (
  id uuid primary key default gen_random_uuid(),
  producte_id uuid not null references productes(id),
  quantitat integer not null,
  ts timestamptz not null default now(),
  per_id uuid references socis(id),
  nota text
);

create table apats (
  id uuid primary key default gen_random_uuid(),
  titol text not null,
  data date not null,
  organitzador_id uuid not null references socis(id),
  mode text not null check (mode in ('iguals', 'proporcional')),
  estat text not null default 'obert' check (estat in ('obert', 'tancat'))
);

create table apat_assistents (
  apat_id uuid references apats(id) on delete cascade,
  soci_id uuid references socis(id),
  convidats integer not null default 0,
  parts numeric(6,2) not null default 1,
  primary key (apat_id, soci_id)
);

create table apat_despeses (
  id uuid primary key default gen_random_uuid(),
  apat_id uuid not null references apats(id) on delete cascade,
  pagador_id uuid not null references socis(id),
  concepte text not null,
  import numeric(8,2) not null check (import >= 0)
);

create table reserves (
  id uuid primary key default gen_random_uuid(),
  soci_id uuid not null references socis(id),
  data date not null,
  franja text not null check (franja in ('Dinar', 'Sopar', 'Tot el dia')),
  espai text not null,
  persones integer not null check (persones > 0),
  notes text not null default '',
  creada timestamptz not null default now()
);

create table tancaments (
  mes text primary key, -- 'YYYY-MM'
  estat text not null check (estat in ('informat', 'tancat')),
  informat_ts timestamptz not null,
  tancat_ts timestamptz
);

create table correus (
  id uuid primary key default gen_random_uuid(),
  ts timestamptz not null default now(),
  per text not null,
  destinatari_original text not null,
  assumpte text not null,
  cos text not null,
  estat text not null default 'pendent',
  error text
);

create table config (
  id boolean primary key default true check (id), -- una sola fila
  dades jsonb not null
);

-- Verificació del PIN a la tauleta sense exposar els hash
create or replace function verificar_pin(p_soci uuid, p_pin text)
returns boolean language sql security definer set search_path = public as $$
  select exists (select 1 from socis where id = p_soci and actiu and pin_hash = crypt(p_pin, pin_hash));
$$;

-- Seguretat per files (resum): els socis llegeixen el catàleg i el seu consum; la junta ho gestiona tot.
alter table socis enable row level security;
alter table productes enable row level security;
alter table consums enable row level security;
alter table reposicions enable row level security;
alter table apats enable row level security;
alter table apat_assistents enable row level security;
alter table apat_despeses enable row level security;
alter table reserves enable row level security;
alter table tancaments enable row level security;
alter table correus enable row level security;
alter table config enable row level security;

create or replace function es_junta() returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from socis where auth_user = auth.uid() and rol = 'junta' and actiu);
$$;
create or replace function soci_actual() returns uuid language sql stable security definer set search_path = public as $$
  select id from socis where auth_user = auth.uid();
$$;

create policy "catàleg visible" on productes for select using (auth.role() = 'authenticated');
create policy "junta gestiona productes" on productes for all using (es_junta()) with check (es_junta());
create policy "llista de socis" on socis for select using (auth.role() = 'authenticated');
create policy "junta gestiona socis" on socis for all using (es_junta()) with check (es_junta());
create policy "el meu consum" on consums for select using (soci_id = soci_actual() or es_junta());
create policy "apuntar el meu" on consums for insert with check (soci_id = soci_actual() or es_junta());
create policy "junta corregeix" on consums for update using (es_junta());
-- La tauleta entra amb un usuari propi (rol 'tauleta') i apunta via funcions security definer després de verificar el PIN.

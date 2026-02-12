create extension if not exists pgcrypto;

create table orgs (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    created_at timestamptz not null default now()
);

create table users (
    id uuid primary key default gen_random_uuid(),
    email text not null unique,
    name text,
    created_at timestamptz not null default now()
);

create table org_members (
    org_id uuid not null references orgs(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    role text not null check (role in ('ADMIN', 'EDITOR', 'VIEWER')),
    created_at timestamptz not null default now(),
    primary key (org_id, user_id)
);

create table projects (
    id uuid primary key default gen_random_uuid(),
    org_id uuid not null references orgs(id) on delete cascade,
    name text not null,
    description text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table resource_nodes (
    id uuid primary key default gen_random_uuid(),
    project_id uuid not null references projects(id) on delete cascade,
    provider text not null check (provider in ('AWS')),
    type text not null check (type in ('REGION', 'VPC', 'SUBNET', 'SG', 'EC2', 'RDS', 'S3', 'ELB')),
    name text not null,
    region text,
    zone text,
    state text,
    source text not null check (source in ('PLANNED', 'DISCOVERED')),
    cost_estimate numeric(14, 2),
    metadata_json jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index resource_nodes_project_id_idx on resource_nodes(project_id);
create index resource_nodes_type_idx on resource_nodes(type);

create table resource_edges (
    id uuid primary key default gen_random_uuid(),
    project_id uuid not null references projects(id) on delete cascade,
    from_node_id uuid not null references resource_nodes(id) on delete cascade,
    to_node_id uuid not null references resource_nodes(id) on delete cascade,
    relation_type text not null check (relation_type in ('CONTAINS', 'CONNECTS', 'DEPENDS_ON')),
    created_at timestamptz not null default now()
);

create index resource_edges_project_id_idx on resource_edges(project_id);
create index resource_edges_from_idx on resource_edges(from_node_id);
create index resource_edges_to_idx on resource_edges(to_node_id);

create table cost_snapshots (
    id uuid primary key default gen_random_uuid(),
    project_id uuid not null references projects(id) on delete cascade,
    total_cost numeric(14, 2) not null,
    currency text not null default 'USD',
    breakdown_json jsonb,
    pricing_version text,
    created_at timestamptz not null default now()
);

create index cost_snapshots_project_id_idx on cost_snapshots(project_id);

create table discovery_runs (
    id uuid primary key default gen_random_uuid(),
    project_id uuid not null references projects(id) on delete cascade,
    provider text not null check (provider in ('AWS')),
    account_id text,
    status text not null,
    started_at timestamptz,
    finished_at timestamptz,
    summary_json jsonb
);

create index discovery_runs_project_id_idx on discovery_runs(project_id);

create table terraform_exports (
    id uuid primary key default gen_random_uuid(),
    project_id uuid not null references projects(id) on delete cascade,
    status text not null,
    created_at timestamptz not null default now(),
    summary_json jsonb,
    artifact_path text
);

create index terraform_exports_project_id_idx on terraform_exports(project_id);

create table audit_events (
    id uuid primary key default gen_random_uuid(),
    project_id uuid not null references projects(id) on delete cascade,
    actor_id uuid references users(id) on delete set null,
    action text not null,
    entity_type text not null,
    entity_id uuid,
    changes_json jsonb,
    created_at timestamptz not null default now()
);

create index audit_events_project_id_idx on audit_events(project_id);
create index audit_events_actor_id_idx on audit_events(actor_id);

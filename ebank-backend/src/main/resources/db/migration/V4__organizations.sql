-- Multi-tenant organizations: lifecycle, administrative roles, and tenant isolation enforced by the database.

-- A suspended organization keeps its data, but none of its users can sign in or use an existing session.
alter table organization add column status varchar(20) not null default 'ACTIVE';
alter table organization add constraint ck_organization_status check (status in ('ACTIVE', 'SUSPENDED'));

-- PLATFORM_ADMIN: operates the platform (provisions and suspends organizations), belongs to no organization.
-- ORG_ADMIN: administers one organization (staff, overview, audit log).
insert into app_role (role_name) values ('PLATFORM_ADMIN'), ('ORG_ADMIN');

-- Tenant isolation at the database level: rows that reference each other must belong to the same organization.
-- The unique (id, organization_id) pairs are the targets of composite foreign keys.
alter table app_user add constraint uk_app_user_id_organization unique (id, organization_id);
alter table bank_account add constraint uk_bank_account_id_organization unique (id, organization_id);

-- A client's login belongs to the client's organization
alter table client add constraint fk_client_app_user_same_org
    foreign key (app_user_id, organization_id) references app_user (id, organization_id);

-- A transfer never crosses organizations
alter table transfer add constraint fk_transfer_source_same_org
    foreign key (source_account_id, organization_id) references bank_account (id, organization_id);
alter table transfer add constraint fk_transfer_destination_same_org
    foreign key (destination_account_id, organization_id) references bank_account (id, organization_id);

-- A ledger entry is in its account's organization
alter table ledger_entry add constraint fk_ledger_entry_account_same_org
    foreign key (account_id, organization_id) references bank_account (id, organization_id);

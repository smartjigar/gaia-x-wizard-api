--liquibase formatted sql
--changeset Jigar:1
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE tenants (
	id UUID NOT NULL DEFAULT uuid_generate_v4(),
	"name" varchar NOT NULL,
	description varchar NULL,
	alias varchar NOT NULL,
	created_at timestamp NOT NULL,
	updated_at timestamp NOT NULL,
	active bool NOT NULL DEFAULT true,
	CONSTRAINT tenants_pk PRIMARY KEY (id),
	CONSTRAINT name_tenants_un UNIQUE ("name"),
	CONSTRAINT alias_tenants_un UNIQUE ("alias")
);

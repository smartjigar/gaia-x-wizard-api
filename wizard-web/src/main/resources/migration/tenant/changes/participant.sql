--liquibase formatted sql
--changeset Dilip:1
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE participant (
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	did varchar(200) NULL,
	email varchar(100) NOT NULL,
	legal_name varchar(200) NOT NULL,
	short_name varchar(200) NOT NULL,
	entity_type_id uuid NULL,
	sub_domain varchar(100) NULL,
	private_key_id varchar(100) NULL,
	participant_type varchar(100) NULL,
	own_did_solution bool NOT NULL,
	status int4 NULL,
	credential_request text NULL,
	created_at timestamp(6) NULL,
	updated_at timestamp(6) NULL,
	key_stored bool NULL DEFAULT false,
	profile_image varchar(100) NULL,
	CONSTRAINT email_unique UNIQUE (email),
	CONSTRAINT legal_name_unique UNIQUE (legal_name),
	CONSTRAINT participant_pkey PRIMARY KEY (id),
	CONSTRAINT short_name_unique UNIQUE (short_name)
);

CREATE TABLE credential (
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	vc_url text NOT NULL,
	vc_json text NOT NULL,
	"type" varchar(100) NOT NULL,
	participant_id uuid NOT NULL,
	metadata text NULL,
	created_at timestamp(6) NULL,
	updated_at timestamp(6) NULL,
	CONSTRAINT credential_pkey PRIMARY KEY (id),
	CONSTRAINT vc_url_type_unique UNIQUE (vc_url, type),
	CONSTRAINT fk_participant_id FOREIGN KEY (participant_id) REFERENCES participant(id)
);
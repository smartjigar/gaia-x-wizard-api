--liquibase formatted sql
--changeset Dilip:1
CREATE TABLE resource (
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	credential_id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	"type" varchar NOT NULL,
	participant_id uuid NOT NULL,
	publish_to_kafka bool NOT NULL,
	created_at timestamp(6) NULL,
	updated_at timestamp(6) NULL,
	obsolete_date timestamp(6) NULL,
	expiry_date timestamp(6) NULL,
	CONSTRAINT resource_pkey PRIMARY KEY (id),
	CONSTRAINT fk_credential_id FOREIGN KEY (credential_id) REFERENCES credential(id),
	CONSTRAINT fk_participant_id FOREIGN KEY (participant_id) REFERENCES participant(id)
);




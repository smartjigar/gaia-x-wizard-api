--liquibase formatted sql
--changeset Dilip:1

CREATE TABLE service_offer (
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	credential_id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	participant_id uuid NOT NULL,
	created_at timestamp(6) NULL,
	updated_at timestamp(6) NULL,
	veracity_data text NULL,
	label_level varchar(20) NULL,
	message_reference_id varchar(50) NULL,
	CONSTRAINT service_offer_pkey PRIMARY KEY (id),
	CONSTRAINT fk_credential_id FOREIGN KEY (credential_id) REFERENCES credential(id),
	CONSTRAINT fk_participant_id FOREIGN KEY (participant_id) REFERENCES participant(id)
);

CREATE TABLE label_level_answer(
    id UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    participant_id UUID NOT NULL,
    service_offer_id UUID NOT NULL,
    question_id UUID NOT NULL,
    answer Boolean NOT NULL,
    created_at timestamp(6) NULL,
    updated_at timestamp(6) NULL,
    CONSTRAINT fk_participant_id FOREIGN KEY (participant_id) REFERENCES participant(id),
    CONSTRAINT fk_service_offer_id FOREIGN KEY (service_offer_id) REFERENCES service_offer(id),
    CONSTRAINT fk_label_level_question_id FOREIGN KEY (question_id) REFERENCES label_level_question_master(id)
);

CREATE TABLE label_level_upload_files(
    id UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    participant_id UUID NOT NULL,
    service_offer_id UUID NOT NULL,
    file_path varchar(200) NOT NULL,
    description varchar(100) NULL,
    created_at timestamp(6) NULL,
    updated_at timestamp(6) NULL,
    CONSTRAINT fk_participant_id FOREIGN KEY (participant_id) REFERENCES participant(id),
    CONSTRAINT fk_service_offer_id FOREIGN KEY (service_offer_id) REFERENCES service_offer(id)
);

CREATE TABLE service_offer_standard_type(
    id UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    service_offer_id UUID NOT NULL,
    standard_type_id UUID NOT NULL,
    CONSTRAINT fk_service_offer_id FOREIGN KEY (service_offer_id) REFERENCES service_offer(id),
    CONSTRAINT fk_standard_type_id FOREIGN KEY (standard_type_id) REFERENCES standard_type_master(id)
);

CREATE TABLE service_label_level(
    id UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    credential_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    service_offer_id UUID NOT NULL,
    created_at timestamp(6) NULL,
    updated_at timestamp(6) NULL,
    CONSTRAINT fk_participant_id FOREIGN KEY (participant_id) REFERENCES participant(id),
    CONSTRAINT fk_service_offer FOREIGN KEY (service_offer_id) REFERENCES service_offer(id),
    CONSTRAINT fk_credential_id FOREIGN KEY (credential_id) REFERENCES credential(id)
);

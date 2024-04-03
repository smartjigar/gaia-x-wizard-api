/*
 * Copyright (c) 2023 | smartSense
 */

package eu.gaiax.wizard.core.service.credential;

import eu.gaiax.wizard.api.model.CredentialTypeEnum;
import eu.gaiax.wizard.dao.tenant.entity.Credential;
import eu.gaiax.wizard.dao.tenant.entity.participant.Participant;
import eu.gaiax.wizard.dao.tenant.repo.CredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The type Credential service.
 */
@Service
@RequiredArgsConstructor
public class CredentialService {

    private final CredentialRepository credentialRepository;

    public Credential createCredential(String vcJson, String vcUrl, String credentialType, String metadata, Participant participant) {
        return credentialRepository.save(Credential.builder()
                .vcJson(vcJson)
                .vcUrl(vcUrl)
                .credentialType(credentialType)
                .metadata(metadata)
                .participant(participant)
                .build());
    }

    public Credential getLegalParticipantCredential(UUID participantId) {
        return getByParticipantWithCredentialType(participantId, CredentialTypeEnum.LEGAL_PARTICIPANT.getCredentialType());
    }

    public Credential getByParticipantWithCredentialType(UUID participantId, String credentialType) {
        return credentialRepository.findByParticipantIdAndCredentialType(participantId, credentialType);
    }
}

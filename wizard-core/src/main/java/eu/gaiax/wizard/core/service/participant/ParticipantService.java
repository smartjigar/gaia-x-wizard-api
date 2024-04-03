package eu.gaiax.wizard.core.service.participant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import eu.gaiax.wizard.api.exception.BadDataException;
import eu.gaiax.wizard.api.exception.EntityNotFoundException;
import eu.gaiax.wizard.api.model.*;
import eu.gaiax.wizard.api.model.request.ParticipantCreationRequest;
import eu.gaiax.wizard.api.model.request.ParticipantOnboardRequest;
import eu.gaiax.wizard.api.model.request.ParticipantRegisterRequest;
import eu.gaiax.wizard.api.model.request.ParticipantValidatorRequest;
import eu.gaiax.wizard.api.model.service_offer.CredentialDto;
import eu.gaiax.wizard.api.utils.CommonUtils;
import eu.gaiax.wizard.api.utils.S3Utils;
import eu.gaiax.wizard.api.utils.Validate;
import eu.gaiax.wizard.core.service.InvokeService;
import eu.gaiax.wizard.core.service.credential.CredentialService;
import eu.gaiax.wizard.core.service.domain.DomainService;
import eu.gaiax.wizard.core.service.keycloak.KeycloakService;
import eu.gaiax.wizard.core.service.signer.SignerService;
import eu.gaiax.wizard.dao.master.entity.EntityTypeMaster;
import eu.gaiax.wizard.dao.master.repo.EntityTypeMasterRepository;
import eu.gaiax.wizard.dao.master.repo.TenantsRepository;
import eu.gaiax.wizard.dao.tenant.entity.Credential;
import eu.gaiax.wizard.dao.tenant.entity.participant.Participant;
import eu.gaiax.wizard.dao.tenant.repo.participant.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static eu.gaiax.wizard.api.utils.StringPool.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParticipantService extends BaseService<Participant, UUID> {
    private final ParticipantRepository participantRepository;
    private final EntityTypeMasterRepository entityTypeMasterRepository;
    private final SignerService signerService;
    private final DomainService domainService;
    private final VaultService vaultService;
    private final CredentialService credentialService;
    private final KeycloakService keycloakService;
    private final S3Utils s3Utils;
    private final ObjectMapper mapper;
    private final MessageSource messageSource;
    private final SpecificationUtil<Participant> specificationUtil;
    private final TenantsRepository tenantsRepository;
    @Value("${wizard.domain}")
    private String domain;


    @Transactional
    @SneakyThrows
    public Participant registerParticipant(ParticipantRegisterRequest request, String tenantAlias) {
        log.debug("ParticipantService(registerParticipant) -> Participant registration with email {}", request.email());
        Validate.isFalse(StringUtils.hasText(request.email())).launch("email.required");
        ParticipantOnboardRequest onboardRequest = request.onboardRequest();
        validateParticipantOnboardRequest(onboardRequest);

        EntityTypeMaster entityType = null;

        if (StringUtils.hasText(onboardRequest.entityType())) {
            entityType = entityTypeMasterRepository.findById(UUID.fromString(onboardRequest.entityType())).orElseThrow(() -> new BadDataException("invalid.entity.type"));
        }
        Participant participant = participantRepository.getByEmail(request.email());
        Validate.isNotNull(participant).launch("participant.already.registered");
        Validate.isNotNull(participantRepository.getByLegalName(request.onboardRequest().legalName())).launch("legal.name.already.registered");
        Validate.isNotNull(participantRepository.getByShortName(request.onboardRequest().shortName().toLowerCase())).launch("short.name.already.registered");

        participant = create(Participant.builder()
                .email(request.email().toLowerCase())
                .legalName(onboardRequest.legalName())
                .shortName(onboardRequest.shortName().toLowerCase())
                .entityType(entityType)
                .domain(onboardRequest.ownDid() ? null : onboardRequest.shortName().toLowerCase() + "." + domain)
                .participantType("REGISTERED")
                .credentialRequest(mapper.writeValueAsString(onboardRequest.credential()))
                .ownDidSolution(onboardRequest.ownDid())
                .build());

        keycloakService.createParticipantUser(participant.getId().toString(), participant.getLegalName(), participant.getEmail(), tenantAlias);

        return participant;
    }

    private void validateParticipantOnboardRequest(ParticipantOnboardRequest request) {
        Validate.isFalse(StringUtils.hasText(request.legalName())).launch("invalid.legal.name");
        Validate.isFalse(StringUtils.hasText(request.shortName())).launch("invalid.short.name");
        Validate.isTrue(CollectionUtils.isEmpty(request.credential())).launch("invalid.credential");
        Validate.isFalse(request.acceptedTnC()).launch("tnc.acceptance.required");
        Map<String, Object> credential = request.credential();
        Object legalParticipant = credential.get("legalParticipant");
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
        };
        Object credentialSubject = mapper.convertValue(legalParticipant, typeReference).get("credentialSubject");
        validateOnboardedCredentialSubject(credentialSubject);
        Validate.isFalse(signerService.validateRegistrationNumber(credential)).launch("invalid.registration.number.details");
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public Participant initiateOnboardParticipantProcess(String participantId, ParticipantCreationRequest request) {
        log.debug("ParticipantService(initiateOnboardParticipantProcess) -> Prepare legal participant json with participant {}", participantId);
        Participant participant = findParticipantById(UUID.fromString(participantId));
        Validate.isFalse(StringUtils.hasText(participant.getShortName())).launch("required.shortname");
        if (Objects.nonNull(request.ownDid()) && participant.isOwnDidSolution() != request.ownDid()) {
            participant.setDomain(request.ownDid() ? null : participant.getShortName().toLowerCase() + "." + domain);
            participant.setOwnDidSolution(request.ownDid());
            participantRepository.save(participant);
        }

        if (participant.isOwnDidSolution()) {
            log.debug("ParticipantService(initiateOnboardParticipantProcess) -> Validate participant {} has own did solution.", participantId);
            Validate.isFalse(StringUtils.hasText(request.issuer())).launch("invalid.did");
            Validate.isFalse(StringUtils.hasText(request.privateKey())).launch("invalid.private.key");
            Validate.isFalse(StringUtils.hasText(request.verificationMethod())).launch("invalid.verification.method");
            Validate.isFalse(validateDidWithPrivateKey(request.issuer(), request.verificationMethod(), request.privateKey())).launch("invalid.did.or.private.key");
            Participant participantFromDid = participantRepository.getByDid(request.issuer());
            if (Objects.nonNull(participantFromDid) && participantFromDid.getId().toString().equals(participantId)) {
                throw new BadDataException(messageSource.getMessage("did.already.registered", new String[]{}, LocaleContextHolder.getLocale()));
            }
        }

        if (Objects.nonNull(request.ownDid()) && request.ownDid()) {
            participant.setDid(request.issuer());
            participant.setOwnDidSolution(request.ownDid());
            participant = participantRepository.save(participant);
        }

        Credential credentials = credentialService.getLegalParticipantCredential(participant.getId());
        Validate.isNotNull(credentials).launch("already.legal.participant");
        createLegalParticipantJson(participant, request);
        if (request.store()) {
            participant.setKeyStored(request.store());
            vaultService.uploadCertificatesToVault(participantId, null, null, null, request.privateKey());
            participantRepository.save(participant);
        }
        return participant;
    }

    private void createLegalParticipantJson(Participant participant, ParticipantCreationRequest request) {
        if (participant.isOwnDidSolution()) {
            log.debug("ParticipantService(createLegalParticipantJson) -> Create Legal participant {} who has own did solutions.", participant.getId());
            createLegalParticipantWithDidSolutions(participant, request);
        } else {
            log.debug("ParticipantService(createLegalParticipantJson) -> Create Legal participant {} who don't have own did solutions.", participant.getId());
            createLegalParticipantWithoutDidSolutions(participant);
        }
    }

    private void createLegalParticipantWithDidSolutions(Participant participant, ParticipantCreationRequest request) {
        log.debug("ParticipantService(createLegalParticipantJson) -> Create participant json.");
        signerService.createSignedLegalParticipant(participant, request.issuer(), request.verificationMethod(), request.privateKey(), true);
    }

    private void createLegalParticipantWithoutDidSolutions(Participant participant) {
        log.debug("ParticipantService(createLegalParticipantJson) -> Create Subdomain, Certificate, Ingress, Did and participant json.");
        domainService.createSubDomain(participant.getId());
    }

    private void validateOnboardedCredentialSubject(Object credentialSubject) {
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
        };
        Map<String, Object> credentials = mapper.convertValue(credentialSubject, typeReference);
        String legalName = (String) credentials.get("gx:legalName");
        Validate.isFalse(StringUtils.hasText(legalName)).launch("invalid.legal.name");
        Object legalAddress = credentials.get(GX_LEGAL_ADDRESS);
        Object headquarterAddress = credentials.get("gx:headquarterAddress");
        String legalCountry = (String) mapper.convertValue(legalAddress, typeReference).get(GX_COUNTRY_SUBDIVISION);
        String headQuarterCountry = (String) mapper.convertValue(headquarterAddress, typeReference).get(GX_COUNTRY_SUBDIVISION);
        Validate.isFalse(StringUtils.hasText(legalCountry)).launch("invalid.legal.address");
        Validate.isFalse(StringUtils.hasText(headQuarterCountry)).launch("invalid.headquarter.address");
        Object parentOrganization = credentials.get("gx:parentOrganization");
        if (Objects.nonNull(parentOrganization)) {
            TypeReference<List<Map<String, String>>> orgTypeReference = new TypeReference<>() {
            };
            List<String> parentOrg = mapper.convertValue(parentOrganization, orgTypeReference).stream().map(s -> s.get("id")).toList();
            parentOrg.parallelStream().forEach(url -> signerService.validateRequestUrl(Collections.singletonList(url), List.of(GX_LEGAL_PARTICIPANT), LABEL_PARENT_ORGANIZATION, "invalid.parent.organization", null));
        }
        Object subOrganization = credentials.get("gx:subOrganization");
        if (Objects.nonNull(subOrganization)) {
            TypeReference<List<Map<String, String>>> orgTypeReference = new TypeReference<>() {
            };
            List<String> subOrg = mapper.convertValue(subOrganization, orgTypeReference).stream().map(s -> s.get("id")).toList();
            subOrg.parallelStream().forEach(url -> signerService.validateRequestUrl(Collections.singletonList(url), List.of(GX_LEGAL_PARTICIPANT), LABEL_SUB_ORGANIZATION, "invalid.sub.organization", null));
        }
    }

    private boolean validateDidWithPrivateKey(String did, String verificationMethod, String privateKey) {
        return signerService.validateDid(did, verificationMethod, privateKey);
    }

    @SneakyThrows
    public Participant validateParticipant(ParticipantValidatorRequest request) {
        signerService.validateRequestUrl(Collections.singletonList(request.participantJsonUrl()), List.of(GX_LEGAL_PARTICIPANT), null, "participant.url.not.found", null);
        String participantJson = InvokeService.executeRequest(request.participantJsonUrl(), HttpMethod.GET);
        JsonNode root = mapper.readTree(participantJson);
        String issuer = null;
        JsonNode selfDescriptionCredential = root.get("selfDescriptionCredential");
        if (selfDescriptionCredential != null) {
            issuer = selfDescriptionCredential.path("verifiableCredential").path(0).path("issuer").asText();
        }
        Validate.isNull(issuer).launch(new EntityNotFoundException("issuer.not.found"));


        Participant participant = participantRepository.getByDid(issuer);
        if (Objects.isNull(participant)) {
            participant = Participant.builder()
                    .did(issuer)
                    .email(issuer)
                    .legalName(issuer)
                    .shortName(issuer)
                    .ownDidSolution(request.ownDid())
                    .keyStored(request.store())
                    .build();
            participant = participantRepository.save(participant);
            Credential credential = credentialService.getLegalParticipantCredential(participant.getId());
            if (Objects.isNull(credential)) {
                credentialService.createCredential(participantJson, request.participantJsonUrl(), CredentialTypeEnum.LEGAL_PARTICIPANT.getCredentialType(), null, participant);
            }
        }

        if (request.store()) {
            vaultService.uploadCertificatesToVault(participant.getId().toString(), null, null, null, request.privateKey());
        }
        return participant;
    }


    public String getWellKnownFiles(String host, String fileName) throws IOException {
        log.info("ParticipantService(getWellKnownFiles) -> Fetch wellKnown file for host {} and filename {}", host, fileName);
        Validate.isTrue(fileName.endsWith("key") || fileName.endsWith("csr")).launch(new EntityNotFoundException("Cannot find file -> " + fileName));
        Participant participant = participantRepository.getByDomain(host);
        Validate.isNull(participant).launch(new EntityNotFoundException("subdomain.not.found"));
        if (fileName.equals(DID_JSON)) {
            return getLegalParticipantOrDidJson(participant.getId().toString(), fileName);
        }

        Map<String, Object> certificates = vaultService.getParticipantSecretData(participant.getId().toString());
        Object certificate = certificates.get(fileName);
        Validate.isNull(certificate).launch(new EntityNotFoundException("certificate.not.found"));
        return (String) certificate;
    }

    public String getLegalParticipantOrDidJson(String participantId, String filename) throws IOException {
        String fetchedFileName = UUID.randomUUID().toString() + "." + FilenameUtils.getExtension(filename);
        File file = new File(fetchedFileName);
        findParticipantById(UUID.fromString(participantId));

        try {
            log.info("ParticipantService(getParticipantFile) -> Fetch files from s3 bucket with Id {} and filename {}", participantId, filename);
            file = s3Utils.getObject(participantId + "/" + filename, fetchedFileName);
            return FileUtils.readFileToString(file, Charset.defaultCharset());
        } finally {
            CommonUtils.deleteFile(file);
        }
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public CheckParticipantRegisteredResponse checkIfParticipantRegistered(String email) {
        boolean participantExists = participantRepository.existsByEmail(email);

        return new CheckParticipantRegisteredResponse(
                participantExists,
                participantExists ? keycloakService.isLoginDeviceConfigured(email) : null
        );
    }

    public Participant changeStatus(UUID participantId, int status) {
        Participant participant = findParticipantById(participantId);
        participant.setStatus(status);
        return create(participant);
    }

    @Override
    protected BaseRepository<Participant, UUID> getRepository() {
        return participantRepository;
    }

    @Override
    protected SpecificationUtil<Participant> getSpecificationUtil() {
        return specificationUtil;
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED, readOnly = true)
    public ParticipantConfigDTO getParticipantConfig(String uuid) {
        Participant participant = findParticipantById(UUID.fromString(uuid));

        ParticipantConfigDTO participantConfigDTO = mapper.convertValue(participant, ParticipantConfigDTO.class);
        if (participant.isOwnDidSolution()) {
            participantConfigDTO.setPrivateKeyRequired(!StringUtils.hasText(participant.getPrivateKeyId()));
        }

        Credential credential = credentialService.getByParticipantWithCredentialType(participant.getId(), CredentialTypeEnum.LEGAL_PARTICIPANT.getCredentialType());
        if (credential != null) {
            participantConfigDTO.setLegalParticipantUrl(credential.getVcUrl());
        }

        return participantConfigDTO;
    }

    public ParticipantAndKeyResponse exportParticipantAndKey(String uuid) {
        Participant participant = findParticipantById(UUID.fromString(uuid));
        ParticipantAndKeyResponse participantAndKeyResponse = new ParticipantAndKeyResponse();

        Credential legalParticipantCredential = credentialService.getLegalParticipantCredential(participant.getId());
        Validate.isNull(legalParticipantCredential).launch(new BadDataException("participant.credential.not.found"));
        participantAndKeyResponse.setParticipantJson(legalParticipantCredential.getVcUrl());

        if (participant.isOwnDidSolution()) {
            return participantAndKeyResponse;
        }

        String privateKeySecret = vaultService.getParticipantPrivateKeySecret(participant.getId().toString());
        if (StringUtils.hasText(privateKeySecret)) {
            participantAndKeyResponse.setPrivateKey(privateKeySecret);
        }

        return participantAndKeyResponse;
    }

    public void sendRegistrationLink(String email) {
        keycloakService.sendRequiredActionsEmail(email);
        log.info("registration email sent to email: {}", email);
    }

    @SneakyThrows
    @Transactional(propagation = Propagation.REQUIRED)
    public ParticipantProfileDto getParticipantProfile(String participantId) {
        Participant participant = findParticipantById(UUID.fromString(participantId));

        ParticipantProfileDto participantProfileDto = mapper.convertValue(participant, ParticipantProfileDto.class);
        JsonNode participantCredentialRequest = mapper.readTree(participant.getCredentialRequest());
        participantProfileDto.setLegalRegistrationNumber(participantCredentialRequest.get(LEGAL_REGISTRATION_NUMBER));

        Credential credential = credentialService.getLegalParticipantCredential(participant.getId());
        if (credential != null) {
            participantProfileDto.setCredential(mapper.convertValue(credential, CredentialDto.class));
        }

        JsonNode credentialSubject = participantCredentialRequest.get(LEGAL_PARTICIPANT).get(CREDENTIAL_SUBJECT);
        participantProfileDto.setHeadquarterAddress(credentialSubject.get(HEADQUARTER_ADDRESS).get(SUBDIVISION_CODE).asText());
        participantProfileDto.setLegalAddress(credentialSubject.get(LEGAL_ADDRESS).get(SUBDIVISION_CODE).asText());

        if (StringUtils.hasText(participant.getProfileImage())) {
            participantProfileDto.setProfileImage(s3Utils.getPreSignedUrl(participant.getProfileImage()));
        }

        List<String> organizationList;
        if (credentialSubject.has(PARENT_ORGANIZATION)) {
            organizationList = getParentOrSubOrganizationList((ArrayNode) credentialSubject.get(PARENT_ORGANIZATION));
            participantProfileDto.setParentOrganization(organizationList);
        }

        if (credentialSubject.has(SUB_ORGANIZATION)) {
            organizationList = getParentOrSubOrganizationList((ArrayNode) credentialSubject.get(SUB_ORGANIZATION));
            participantProfileDto.setSubOrganization(organizationList);
        }

        return participantProfileDto;
    }

    private List<String> getParentOrSubOrganizationList(ArrayNode organizationArrayNode) {
        List<Map<String, String>> organizationlist = mapper.convertValue(organizationArrayNode, new TypeReference<>() {
        });
        return organizationlist.stream().map(org -> org.get(ID)).sorted().toList();
    }

    public String updateParticipantProfileImage(String participantId, MultipartFile multipartFile) {
        Participant participant = findParticipantById(UUID.fromString(participantId));

        if (StringUtils.hasText(participant.getProfileImage())) {
            s3Utils.deleteFile(participant.getProfileImage());
        }

        String fileName = "participant/" + participantId + "_" + System.currentTimeMillis() + "." + FilenameUtils.getExtension(multipartFile.getOriginalFilename());
        File profileImage = new File(TEMP_FOLDER + fileName);
        try {
            FileUtils.copyToFile(multipartFile.getInputStream(), profileImage);
            s3Utils.uploadFile(fileName, profileImage);
        } catch (Exception e) {
            log.error("Error while saving profile picture for participantId: {}", participant.getId(), e);
            throw new BadDataException("invalid.file");
        } finally {
            FileUtils.deleteQuietly(profileImage);
        }

        participant.setProfileImage(fileName);
        participantRepository.save(participant);

        return s3Utils.getPreSignedUrl(fileName);
    }

    public void deleteParticipantProfileImage(String participantId) {
        Participant participant = findParticipantById(UUID.fromString(participantId));

        if (!StringUtils.hasText(participant.getProfileImage())) {
            throw new BadDataException("file.not.found");
        }

        participant.setProfileImage(null);
        participantRepository.save(participant);
    }

    public Participant findParticipantById(UUID id) {
        return participantRepository.findById(id).orElseThrow(() -> new BadDataException("participant.not.found"));
    }

    public Participant save(Participant participant) {
        return participantRepository.save(participant);
    }
}

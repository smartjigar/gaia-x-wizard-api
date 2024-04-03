package eu.gaiax.wizard.core.service.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartsensesolutions.java.commons.FilterRequest;
import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.filter.FilterCriteria;
import com.smartsensesolutions.java.commons.operator.Operator;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import eu.gaiax.wizard.api.exception.BadDataException;
import eu.gaiax.wizard.api.model.CredentialTypeEnum;
import eu.gaiax.wizard.api.model.PageResponse;
import eu.gaiax.wizard.api.model.ResourceFilterResponse;
import eu.gaiax.wizard.api.model.ResourceType;
import eu.gaiax.wizard.api.model.did.ServiceEndpointConfig;
import eu.gaiax.wizard.api.model.request.ParticipantValidatorRequest;
import eu.gaiax.wizard.api.model.service_offer.CreateResourceRequest;
import eu.gaiax.wizard.api.model.setting.ContextConfig;
import eu.gaiax.wizard.api.utils.StringPool;
import eu.gaiax.wizard.api.utils.TenantContext;
import eu.gaiax.wizard.api.utils.Validate;
import eu.gaiax.wizard.core.service.credential.CredentialService;
import eu.gaiax.wizard.core.service.hashing.HashingService;
import eu.gaiax.wizard.core.service.keycloak.KeycloakService;
import eu.gaiax.wizard.core.service.participant.ParticipantService;
import eu.gaiax.wizard.core.service.participant.VaultService;
import eu.gaiax.wizard.core.service.service_offer.PolicyService;
import eu.gaiax.wizard.core.service.signer.SignerService;
import eu.gaiax.wizard.dao.tenant.entity.Credential;
import eu.gaiax.wizard.dao.tenant.entity.participant.Participant;
import eu.gaiax.wizard.dao.tenant.entity.resource.Resource;
import eu.gaiax.wizard.dao.tenant.repo.participant.ParticipantRepository;
import eu.gaiax.wizard.dao.tenant.repo.resource.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static eu.gaiax.wizard.api.utils.StringPool.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceService extends BaseService<Resource, UUID> {

    private final ResourceRepository resourceRepository;

    private final ParticipantService participantService;

    private final VaultService vaultService;

    private final ParticipantRepository participantRepository;

    private final ContextConfig contextConfig;

    private final ObjectMapper objectMapper;

    private final CredentialService credentialService;

    private final SpecificationUtil<Resource> specificationUtil;

    private final SignerService signerService;

    private final ServiceEndpointConfig serviceEndpointConfig;

    private final PolicyService policyService;

    private final KeycloakService keycloakService;

    @Value("${wizard.host.wizard}")
    private String wizardHost;

    @NotNull
    private static List<Map<String, Object>> getPermissionMaps(Participant participant) {
        List<Map<String, Object>> permission = new ArrayList<>();
        Map<String, Object> perMap = new HashMap<>();
        perMap.put(TARGET, participant.getDid());
        perMap.put(ASSIGNER, participant.getDid());
        perMap.put(ACTION, "view");

        permission.add(perMap);
        return permission;
    }

    @SneakyThrows
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRES_NEW)
    public Resource createResource(CreateResourceRequest request, String id) {
        Participant participant;
        if (StringUtils.hasText(id)) {
            participant = participantRepository.findById(UUID.fromString(id)).orElseThrow(() -> new BadDataException("participant.not.found"));
            addPrivateKey(participant, request);

            Credential participantCred = credentialService.getByParticipantWithCredentialType(participant.getId(), CredentialTypeEnum.LEGAL_PARTICIPANT.getCredentialType());
            signerService.validateRequestUrl(Collections.singletonList(participantCred.getVcUrl()), List.of(GX_LEGAL_PARTICIPANT), null, "participant.url.not.found", null);
        } else {
            ParticipantValidatorRequest participantValidatorRequest = new ParticipantValidatorRequest(request.getParticipantJsonUrl(), request.getVerificationMethod(), request.getPrivateKey(), false, true);
            participant = participantService.validateParticipant(participantValidatorRequest);
        }

        Validate.isNull(participant).launch(new BadDataException("participant.not.found"));
        validateResourceRequest(request);
        String name = "resource_" + UUID.randomUUID();
        String json = generateResourceVc(request, participant, name);
        String hostUrl = wizardHost + participant.getId() + "/" + name + JSON_EXTENSION;

        if (!participant.isOwnDidSolution()) {
            signerService.addServiceEndpoint(participant.getId(), hostUrl, serviceEndpointConfig.linkDomainType(), hostUrl);
        }

        if (StringUtils.hasText(json)) {
            Credential resourceVc = credentialService.createCredential(json, hostUrl, CredentialTypeEnum.RESOURCE.getCredentialType(), "", participant);
            Resource resource = Resource.builder().name(request.getCredentialSubject().get("gx:name").toString())
                    .credential(resourceVc)
                    .type(request.getCredentialSubject().get(TYPE).toString())
                    .description((String) request.getCredentialSubject().getOrDefault("gx:description", null))
                    .participant(participant)
                    .build();

            if (resource.getType().equals(ResourceType.VIRTUAL_DATA_RESOURCE.getValue())) {
                SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_FORMAT);
                if (request.getCredentialSubject().containsKey(OBSOLETE_TIME)) {
                    resource.setObsoleteDate(formatter.parse((String) request.getCredentialSubject().get(OBSOLETE_TIME)));
                }

                if (request.getCredentialSubject().containsKey(EXPIRATION_TIME)) {
                    resource.setExpiryDate(formatter.parse((String) request.getCredentialSubject().get(EXPIRATION_TIME)));
                }
            }

            if (StringUtils.hasText(id) && request.isStoreVault() && !participant.isKeyStored()) {
                storePrivateKeyToVault(participant, request.getPrivateKey());
            }

            return resourceRepository.save(resource);
        }

        return null;
    }

    private void storePrivateKeyToVault(Participant participant, String privateKey) {
        vaultService.uploadCertificatesToVault(participant.getId().toString(), null, null, null, privateKey);
        participant.setKeyStored(true);
        participantRepository.save(participant);
    }

    private void addPrivateKey(Participant participant, CreateResourceRequest request) {
        if (participant.isKeyStored()) {
            String privateKeySecret = vaultService.getParticipantPrivateKeySecret(participant.getId().toString());
            if (!StringUtils.hasText(privateKeySecret)) {
                throw new BadDataException("private.key.not.found");
            }

            request.setPrivateKey(privateKeySecret);
            request.setVerificationMethod(participant.getDid());
        }

        if (request.isStoreVault() && !participant.isKeyStored()) {
            vaultService.uploadCertificatesToVault(participant.getId().toString(), null, null, null, request.getPrivateKey());
            participant.setKeyStored(true);
            participantRepository.save(participant);
        }
    }

    private String createAndHostPolicy(Participant participant) throws JsonProcessingException {
        Map<String, Object> policyMap = new HashMap<>();
        String hostUrl = participant.getId() + "/resource_policy_" + UUID.randomUUID() + JSON_EXTENSION;
        policyMap.put(CONTEXT, contextConfig.ODRLPolicy());
        policyMap.put(TYPE, "Offer");
        policyMap.put(ID, wizardHost + hostUrl);
        List<Map<String, Object>> permission = getPermissionMaps(participant);
        policyMap.put("permission", permission);
        String policyJson = objectMapper.writeValueAsString(policyMap);

        policyService.hostPolicy(policyJson, hostUrl);
        return wizardHost + hostUrl;
    }

    public void validateResourceRequest(CreateResourceRequest request) throws JsonProcessingException {
        Validate.isFalse(StringUtils.hasText(request.getCredentialSubject().get(NAME).toString())).launch("invalid.resource.name");
        validateAggregationOf(request);
        JsonObject jsonObject = JsonParser.parseString(objectMapper.writeValueAsString(request)).getAsJsonObject();

        if (request.getCredentialSubject().get(TYPE).toString().contains(PHYSICAL)) {
            validatePhysicalResource(request, jsonObject);
        } else {
            validateVirtualResource(request, jsonObject);
        }

        if (request.getCredentialSubject().containsKey(SUBTYPE) && ResourceType.VIRTUAL_DATA_RESOURCE.getValue().equals("gx:" + request.getCredentialSubject().get(SUBTYPE).toString())) {
            if (request.getCredentialSubject().containsKey(OBSOLETE_TIME)) {
                validateDate((String) request.getCredentialSubject().get(OBSOLETE_TIME), "invalid.obsolete.date");
            }

            if (request.getCredentialSubject().containsKey(EXPIRATION_TIME)) {
                validateDate((String) request.getCredentialSubject().get(EXPIRATION_TIME), "invalid.expiry.date");
            }
        }
    }

    private void validateDate(String dateString, String errorMessage) {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_FORMAT);
        try {
            Date expiryDate = formatter.parse(dateString);
            if (expiryDate.before(new Date())) {
                throw new BadDataException(errorMessage);
            }
        } catch (Exception e) {
            log.error("Exception caught while parsing date: {}", dateString);
            throw new BadDataException(errorMessage);
        }
    }

    private void validatePhysicalResource(CreateResourceRequest request, JsonObject jsonObject) {
        if (request.getCredentialSubject().containsKey(MAINTAINED_BY)) {
            JsonArray aggregationArray = jsonObject
                    .getAsJsonObject(CREDENTIAL_SUBJECT)
                    .getAsJsonArray(MAINTAINED_BY);
            List<String> ids = new ArrayList<>();

            for (int i = 0; i < aggregationArray.size(); i++) {
                JsonObject aggregationObject = aggregationArray.get(i).getAsJsonObject();
                String idValue = aggregationObject.get(ID).getAsString();
                ids.add(idValue);
            }
            signerService.validateRequestUrl(ids, List.of(GX_LEGAL_PARTICIPANT), LABEL_MAINTAINED_BY, "maintained.by.not.found", null);
        }
        if (request.getCredentialSubject().containsKey(OWNED_BY)) {
            JsonArray aggregationArray = jsonObject
                    .getAsJsonObject(CREDENTIAL_SUBJECT)
                    .getAsJsonArray(OWNED_BY);
            List<String> ids = new ArrayList<>();

            for (int i = 0; i < aggregationArray.size(); i++) {
                JsonObject aggregationObject = aggregationArray.get(i).getAsJsonObject();
                String idValue = aggregationObject.get(ID).getAsString();
                ids.add(idValue);
            }
            signerService.validateRequestUrl(ids, List.of(GX_LEGAL_PARTICIPANT), LABEL_OWNED_BY, "owned.by.not.found", null);
        }
        if (request.getCredentialSubject().containsKey(MANUFACTURED_BY)) {
            JsonArray aggregationArray = jsonObject
                    .getAsJsonObject(CREDENTIAL_SUBJECT)
                    .getAsJsonArray(MANUFACTURED_BY);
            List<String> ids = new ArrayList<>();

            for (int i = 0; i < aggregationArray.size(); i++) {
                JsonObject aggregationObject = aggregationArray.get(i).getAsJsonObject();
                String idValue = aggregationObject.get(ID).getAsString();
                ids.add(idValue);
            }
            signerService.validateRequestUrl(ids, List.of(GX_LEGAL_PARTICIPANT), LABEL_MANUFACTURED_BY, "manufactured.by.not.found", null);
        }
    }

    private void validateVirtualResource(CreateResourceRequest request, JsonObject jsonObject) {
        if (request.getCredentialSubject().containsKey(COPYRIGHT_OWNED_BY)) {
            JsonArray aggregationArray = jsonObject
                    .getAsJsonObject(CREDENTIAL_SUBJECT)
                    .getAsJsonArray(COPYRIGHT_OWNED_BY);
            List<String> ids = new ArrayList<>();

            for (int i = 0; i < aggregationArray.size(); i++) {
                JsonObject aggregationObject = aggregationArray.get(i).getAsJsonObject();
                String idValue = aggregationObject.get(ID).getAsString();
                ids.add(idValue);
            }
            signerService.validateRequestUrl(ids, List.of(GX_LEGAL_PARTICIPANT), LABEL_MANUFACTURED_BY, "manufactured.by.not.found", null);
        }
        if (request.getCredentialSubject().containsKey(PRODUCED_BY)) {
            JsonObject produceBy = jsonObject
                    .getAsJsonObject(CREDENTIAL_SUBJECT)
                    .getAsJsonObject(PRODUCED_BY);

            String idValue = produceBy.get(ID).getAsString();

            signerService.validateRequestUrl(List.of(idValue), List.of(GX_LEGAL_PARTICIPANT), LABEL_PRODUCED_BY, "produced.by.not.found", null);
        }

        if (request.getCredentialSubject().containsKey(CONTAINS_PII) && Boolean.parseBoolean(request.getCredentialSubject().get(CONTAINS_PII).toString())) {

            if (!request.getCredentialSubject().containsKey(LEGAL_BASIS)) {
                throw new BadDataException("invalid.legal.basis");
            }
            if (!(request.getCredentialSubject().containsKey(GX_EMAIL) || request.getCredentialSubject().containsKey(GX_URL))) {
                throw new BadDataException("data.protection.contact.required");
            }
        }
    }

    private void validateAggregationOf(CreateResourceRequest request) throws JsonProcessingException {
        if (request.getCredentialSubject().containsKey(AGGREGATION_OF)) {
            JsonObject jsonObject = JsonParser.parseString(objectMapper.writeValueAsString(request)).getAsJsonObject();
            JsonArray aggregationArray = jsonObject
                    .getAsJsonObject(CREDENTIAL_SUBJECT)
                    .getAsJsonArray(AGGREGATION_OF);
            List<String> ids = new ArrayList<>();

            for (int i = 0; i < aggregationArray.size(); i++) {
                JsonObject aggregationObject = aggregationArray.get(i).getAsJsonObject();
                String idValue = aggregationObject.get(ID).getAsString();
                ids.add(idValue);
            }
            signerService.validateRequestUrl(ids, new ArrayList<>(ResourceType.getValueSet()), LABEL_AGGREGATION_OF, "aggregation.of.not.found", Collections.singletonList("holderSignature"));
        }
    }

    protected String generateResourceVc(CreateResourceRequest request, Participant participant, String name) throws
            JsonProcessingException {
        String id = wizardHost + participant.getId() + "/" + name + JSON_EXTENSION;
        String issuanceDate = LocalDateTime.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Map<String, Object> resourceRequest = new HashMap<>();
        Map<String, Object> map = new HashMap<>();
        resourceRequest.put(CONTEXT, contextConfig.resource());
        resourceRequest.put(TYPE, Collections.singleton(VERIFIABLE_CREDENTIAL));
        resourceRequest.put(ID, id);
        resourceRequest.put(ISSUER, participant.getDid());
        resourceRequest.put(ISSUANCE_DATE, issuanceDate);
        Map<String, Object> credentialSub = request.getCredentialSubject();
        if (credentialSub != null) {
            credentialSub.put(CONTEXT, contextConfig.resource());
            credentialSub.put(ID, id);
            if (request.getCredentialSubject().get(TYPE).toString().contains("Physical")) {
                credentialSub.put(TYPE, "gx:" + request.getCredentialSubject().get(TYPE).toString());
            } else {
                if (!request.getCredentialSubject().containsKey(SUBTYPE) || !StringUtils.hasText(request.getCredentialSubject().get(SUBTYPE).toString())) {
                    throw new BadDataException("sub.type.required");
                }
                credentialSub.put(TYPE, "gx:" + request.getCredentialSubject().get(SUBTYPE).toString());
                credentialSub.remove(SUBTYPE);
                if (request.getCredentialSubject().containsKey(GX_POLICY) && request.getCredentialSubject().get(GX_POLICY) != null) {
                    Map<String, String> policy = objectMapper.convertValue(request.getCredentialSubject().get(GX_POLICY), Map.class);
                    String customAttribute = policy.get(CUSTOM_ATTRIBUTE);
                    credentialSub.put(GX_POLICY, List.of(customAttribute));
                } else {
                    credentialSub.put(GX_POLICY, List.of(createAndHostPolicy(participant)));
                }
            }
        }
        resourceRequest.put(CREDENTIAL_SUBJECT, credentialSub);
        map.put("resource", resourceRequest);
        Map<String, Object> resourceMap = new HashMap<>();
        resourceMap.put(ISSUER, participant.getDid());
        resourceMap.put("verificationMethod", request.getVerificationMethod());
        resourceMap.put("vcs", map);
        resourceMap.put("isVault", participant.isKeyStored());
        if (!participant.isKeyStored()) {
            resourceMap.put("privateKey", HashingService.encodeToBase64(request.getPrivateKey()));
        } else {
            resourceMap.put("privateKey", participant.getId().toString());
        }
        return signerService.signResource(resourceMap, participant.getId(), name);
    }

    public PageResponse<ResourceFilterResponse> filterResource(FilterRequest filterRequest, String participantId) {

        if (StringUtils.hasText(participantId)) {
            FilterCriteria participantCriteria = new FilterCriteria(StringPool.PARTICIPANT_ID, Operator.CONTAIN, Collections.singletonList(participantId));
            List<FilterCriteria> filterCriteriaList = filterRequest.getCriteria() != null ? filterRequest.getCriteria() : new ArrayList<>();
            filterCriteriaList.add(participantCriteria);
            filterRequest.setCriteria(filterCriteriaList);

            String tenantAlias = keycloakService.fetchTenantFromParticipantId(participantId);
            TenantContext.setCurrentTenant(tenantAlias);
        }

        Page<Resource> resourcePage = filter(filterRequest);
        List<ResourceFilterResponse> resourceList = objectMapper.convertValue(resourcePage.getContent(), new TypeReference<>() {
        });

        return PageResponse.of(resourceList, resourcePage, filterRequest.getSort());
    }

    @Override
    protected BaseRepository<Resource, UUID> getRepository() {
        return resourceRepository;
    }

    @Override
    protected SpecificationUtil<Resource> getSpecificationUtil() {
        return specificationUtil;
    }
}

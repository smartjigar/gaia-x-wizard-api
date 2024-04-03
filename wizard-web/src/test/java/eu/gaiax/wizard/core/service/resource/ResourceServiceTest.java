package eu.gaiax.wizard.core.service.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.smartsensesolutions.java.commons.FilterRequest;
import eu.gaiax.wizard.api.exception.BadDataException;
import eu.gaiax.wizard.api.model.PageResponse;
import eu.gaiax.wizard.api.model.ResourceFilterResponse;
import eu.gaiax.wizard.api.model.ResourceType;
import eu.gaiax.wizard.api.model.did.ServiceEndpointConfig;
import eu.gaiax.wizard.api.model.service_offer.CreateResourceRequest;
import eu.gaiax.wizard.api.model.setting.ContextConfig;
import eu.gaiax.wizard.core.service.credential.CredentialService;
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
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.*;

import static eu.gaiax.wizard.api.utils.StringPool.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    private final String randomUUID = UUID.randomUUID().toString();
    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private ParticipantService participantService;
    @Mock
    private ParticipantRepository participantRepository;
    private ObjectMapper objectMapper;
    @Mock
    private CredentialService credentialService;
    @Mock
    private SignerService signerService;
    @Mock
    private PolicyService policyService;
    @Mock
    private VaultService vaultService;
    private ResourceService resourceService;
    private Resource resource;
    private Credential credential;
    @Mock
    private KeycloakService keycloakService;

    @BeforeEach
    void setUp() {
        objectMapper = configureObjectMapper();
        ContextConfig contextConfig = new ContextConfig(null, null, null, null, null, List.of("http://www.w3.org/ns/odrl.jsonld", "https://www.w3.org/ns/odrl/2/ODRL22.json"), List.of("https://www.w3.org/2018/credentials/v1", "https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#"));
        ServiceEndpointConfig serviceEndpointConfig = new ServiceEndpointConfig(randomUUID, randomUUID, randomUUID);
        resourceService = Mockito.spy(new ResourceService(resourceRepository, participantService, vaultService, participantRepository, contextConfig,
                objectMapper, credentialService, null, signerService, serviceEndpointConfig, policyService, keycloakService));
        credential = generateMockCredential();
        resource = generateMockResource();
    }

    @AfterEach
    void tearDown() {
        objectMapper = null;
        resourceService = null;
        credential = null;
        resource = null;
    }

    private ObjectMapper configureObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    @Test
    void testCreateResource_physical() {
        Participant participant = generateMockParticipant();
        participant.setOwnDidSolution(false);

        doReturn(credential).when(credentialService).createCredential(anyString(), anyString(), anyString(), anyString(), any());
        doReturn(participant).when(participantService).validateParticipant(any());
        doReturn(resource).when(resourceRepository).save(any());
        doReturn(getResourceCredentialMock()).when(signerService).signResource(anyMap(), any(), anyString());

        doNothing().when(signerService).validateRequestUrl(anyList(), anyList(), nullable(String.class), anyString(), nullable(List.class));
        doNothing().when(signerService).addServiceEndpoint(any(), anyString(), anyString(), anyString());

        Resource resourceActual = resourceService.createResource(generateMockCreatePhysicalResourceRequest(), null);
        assertThat(resourceActual.getName()).isEqualTo(resource.getName());
    }

    @Test
    void testFilterResource() {

        doReturn(new PageImpl<>(Collections.singletonList(resource))).when(resourceService).filter(any());
        FilterRequest filterRequest = new FilterRequest();
        filterRequest.setPage(0);
        filterRequest.setSize(1);
        PageResponse<ResourceFilterResponse> resourceFilterPageResponse = resourceService.filterResource(filterRequest, randomUUID);

        assertThat(resourceFilterPageResponse.getContent().iterator().next().getName()).isEqualTo(resource.getName());
    }

    private Credential generateMockCredential() {
        return Credential.builder()
                .vcUrl(randomUUID)
                .vcJson("{}")
                .participantId(UUID.fromString(randomUUID))
                .build();
    }

    private Participant generateMockParticipant() {
        Participant participant = new Participant();
        participant.setId(UUID.fromString(randomUUID));
        participant.setOwnDidSolution(true);
        return participant;
    }

    private CreateResourceRequest generateMockCreatePhysicalResourceRequest() {
        CreateResourceRequest createResourceRequest = new CreateResourceRequest();
        createResourceRequest.setStoreVault(false);
        createResourceRequest.setPrivateKey(randomUUID);

        Map<String, Object> credentialSubject = new HashMap<>();
        credentialSubject.put("gx:description", randomUUID);
        credentialSubject.put(TYPE, ResourceType.PHYSICAL_RESOURCE.getValue());
        credentialSubject.put(NAME, randomUUID);
        credentialSubject.put(MAINTAINED_BY, Collections.singletonList(Map.of(ID, randomUUID)));
        credentialSubject.put(OWNED_BY, Collections.singletonList(Map.of(ID, randomUUID)));
        credentialSubject.put(MANUFACTURED_BY, Collections.singletonList(Map.of(ID, randomUUID)));

        createResourceRequest.setCredentialSubject(credentialSubject);
        return createResourceRequest;
    }

    private CreateResourceRequest generateMockCreateVirtualDataResourceRequest() {
        CreateResourceRequest createResourceRequest = new CreateResourceRequest();
        createResourceRequest.setStoreVault(false);
        createResourceRequest.setPrivateKey(randomUUID);

        Map<String, Object> credentialSubject = new HashMap<>();
        credentialSubject.put(TYPE, "VirtualResource");
        credentialSubject.put(SUBTYPE, "VirtualDataResource");
        credentialSubject.put(NAME, randomUUID);
        credentialSubject.put("gx:description", randomUUID);
        credentialSubject.put("gx:license", "http://localhost");
        credentialSubject.put("gx:containsPII", true);
        credentialSubject.put(LEGAL_BASIS, randomUUID);
        credentialSubject.put(GX_EMAIL, randomUUID);
        credentialSubject.put(PRODUCED_BY, Map.of(ID, randomUUID));

        final String testDate = "2030-12-01 00:00:00.000";
        credentialSubject.put(OBSOLETE_TIME, testDate);
        credentialSubject.put(EXPIRATION_TIME, testDate);

        createResourceRequest.setCredentialSubject(credentialSubject);
        return createResourceRequest;
    }

    private CreateResourceRequest generateMockCreateVirtualSoftwareResourceRequest() {
        CreateResourceRequest createResourceRequest = new CreateResourceRequest();
        createResourceRequest.setStoreVault(false);
        createResourceRequest.setPrivateKey(randomUUID);

        Map<String, Object> credentialSubject = new HashMap<>();
        credentialSubject.put(TYPE, "VirtualResource");
        credentialSubject.put(SUBTYPE, "VirtualSoftwareResource");
        credentialSubject.put(NAME, randomUUID);
        credentialSubject.put("gx:description", randomUUID);
        credentialSubject.put("gx:license", "http://localhost");
        credentialSubject.put(COPYRIGHT_OWNED_BY, Collections.singletonList(Map.of(ID, randomUUID)));
        credentialSubject.put(AGGREGATION_OF, Collections.singletonList(Map.of(ID, randomUUID)));
        credentialSubject.put(GX_POLICY, Map.of(CUSTOM_ATTRIBUTE, randomUUID));

        createResourceRequest.setCredentialSubject(credentialSubject);
        return createResourceRequest;
    }

    private Resource generateMockResource() {
        Resource resource = new Resource();
        resource.setName(randomUUID);
        resource.setCredential(credential);
        resource.setType(ResourceType.PHYSICAL_RESOURCE.getValue());
        return resource;
    }

    private String getResourceCredentialMock() {
        Map<String, Object> selfDescriptionCredential = new HashMap<>();
        Map<String, Object> resourceVerifiableCredential = new HashMap<>();
        resourceVerifiableCredential.put(CREDENTIAL_SUBJECT, Map.of(TYPE, ResourceType.PHYSICAL_RESOURCE.getValue()));
        selfDescriptionCredential.put(VERIFIABLE_CREDENTIAL_CAMEL_CASE, Collections.singletonList(resourceVerifiableCredential));

        try {
            return objectMapper.writeValueAsString(Map.of(SELF_DESCRIPTION_CREDENTIAL, selfDescriptionCredential));
        } catch (Exception ignored) {

        }
        return null;
    }

    @Test
    void testCreateResource_virtualData_200() {
        doReturn(credential).when(credentialService).createCredential(anyString(), anyString(), anyString(), anyString(), any());
        doReturn(Optional.of(generateMockParticipant())).when(participantRepository).findById(UUID.fromString(randomUUID));
        doNothing().when(signerService).validateRequestUrl(Collections.singletonList(randomUUID), List.of(GX_LEGAL_PARTICIPANT), null, "participant.url.not.found", null);
        doReturn(resource).when(resourceRepository).save(any());
        doReturn(credential).when(credentialService).getByParticipantWithCredentialType(any(), anyString());
        doReturn(getResourceCredentialMock()).when(signerService).signResource(anyMap(), any(), anyString());

        Resource resourceActual = resourceService.createResource(generateMockCreateVirtualDataResourceRequest(), randomUUID);
        assertThat(resourceActual.getName()).isEqualTo(resource.getName());
    }

    @Test
    void testCreateResource_virtualSoftware_200() {
        doReturn(credential).when(credentialService).createCredential(anyString(), anyString(), anyString(), anyString(), any());
        doReturn(Optional.of(generateMockParticipant())).when(participantRepository).findById(UUID.fromString(randomUUID));
        doNothing().when(signerService).validateRequestUrl(anyList(), anyList(), nullable(String.class), anyString(), nullable(List.class));
        doReturn(resource).when(resourceRepository).save(any());
        doReturn(credential).when(credentialService).getByParticipantWithCredentialType(any(), anyString());
        doReturn(getResourceCredentialMock()).when(signerService).signResource(anyMap(), any(), anyString());

        Resource resourceActual = resourceService.createResource(generateMockCreateVirtualSoftwareResourceRequest(), randomUUID);
        assertThat(resourceActual.getName()).isEqualTo(resource.getName());
    }

    @Test
    void testCreateResource_virtualSoftware_subtype_400() {
        doReturn(Optional.of(generateMockParticipant())).when(participantRepository).findById(UUID.fromString(randomUUID));
        doNothing().when(signerService).validateRequestUrl(anyList(), anyList(), nullable(String.class), anyString(), nullable(List.class));
        doReturn(credential).when(credentialService).getByParticipantWithCredentialType(any(), anyString());

        CreateResourceRequest createResourceRequest = generateMockCreateVirtualSoftwareResourceRequest();
        createResourceRequest.getCredentialSubject().remove(SUBTYPE);
        assertThrows(BadDataException.class, () -> resourceService.createResource(createResourceRequest, randomUUID));
    }

    @Test
    void testCreateResource_virtualSoftware_vault_400() {
        Participant participant = generateMockParticipant();
        participant.setKeyStored(true);
        doReturn(Optional.of(participant)).when(participantRepository).findById(UUID.fromString(randomUUID));
        doReturn(StringUtils.EMPTY).when(vaultService).getParticipantPrivateKeySecret(anyString());
        CreateResourceRequest createResourceRequest = generateMockCreateVirtualSoftwareResourceRequest();
        assertThrows(BadDataException.class, () -> resourceService.createResource(createResourceRequest, randomUUID));
    }

    @Test
    void testCreateResource_virtualData_legalBasis_400() {

        doReturn(Optional.of(generateMockParticipant())).when(participantRepository).findById(UUID.fromString(randomUUID));
        doNothing().when(signerService).validateRequestUrl(anyList(), anyList(), nullable(String.class), anyString(), nullable(List.class));
        doReturn(credential).when(credentialService).getByParticipantWithCredentialType(any(), anyString());

        CreateResourceRequest createVirtualDataResourceRequest = generateMockCreateVirtualDataResourceRequest();
        createVirtualDataResourceRequest.getCredentialSubject().remove(LEGAL_BASIS);

        assertThrows(BadDataException.class, () -> resourceService.createResource(createVirtualDataResourceRequest, randomUUID));

    }

    @Test
    void testCreateResource_virtualData_piiEmail_400() {

        doReturn(Optional.of(generateMockParticipant())).when(participantRepository).findById(UUID.fromString(randomUUID));
        doNothing().when(signerService).validateRequestUrl(anyList(), anyList(), nullable(String.class), anyString(), nullable(List.class));
        doReturn(credential).when(credentialService).getByParticipantWithCredentialType(any(), anyString());

        CreateResourceRequest createVirtualDataResourceRequest = generateMockCreateVirtualDataResourceRequest();
        createVirtualDataResourceRequest.getCredentialSubject().remove(GX_EMAIL);

        assertThrows(BadDataException.class, () -> resourceService.createResource(createVirtualDataResourceRequest, randomUUID));
    }

    @Test
    void testCreateResource_virtualData_obsoleteDate_400() {

        doReturn(Optional.of(generateMockParticipant())).when(participantRepository).findById(UUID.fromString(randomUUID));
        doNothing().when(signerService).validateRequestUrl(anyList(), anyList(), nullable(String.class), anyString(), nullable(List.class));
        doReturn(credential).when(credentialService).getByParticipantWithCredentialType(any(), anyString());

        CreateResourceRequest createVirtualDataResourceRequest = generateMockCreateVirtualDataResourceRequest();
        createVirtualDataResourceRequest.getCredentialSubject().put(OBSOLETE_TIME, "2010-12-01 00:00:00.000");

        assertThrows(BadDataException.class, () -> resourceService.createResource(createVirtualDataResourceRequest, randomUUID));
    }

    @Test
    void testCreateResource_virtualData_parse_obsoleteDate_400() {

        doReturn(Optional.of(generateMockParticipant())).when(participantRepository).findById(UUID.fromString(randomUUID));
        doNothing().when(signerService).validateRequestUrl(anyList(), anyList(), nullable(String.class), anyString(), nullable(List.class));
        doReturn(credential).when(credentialService).getByParticipantWithCredentialType(any(), anyString());

        CreateResourceRequest createVirtualDataResourceRequest = generateMockCreateVirtualDataResourceRequest();
        createVirtualDataResourceRequest.getCredentialSubject().put(OBSOLETE_TIME, randomUUID);

        assertThrows(BadDataException.class, () -> resourceService.createResource(createVirtualDataResourceRequest, randomUUID));
    }

    @Test
    void testCreateResource_virtualData_expiryDate_400() {

        doReturn(Optional.of(generateMockParticipant())).when(participantRepository).findById(UUID.fromString(randomUUID));
        doNothing().when(signerService).validateRequestUrl(anyList(), anyList(), nullable(String.class), anyString(), nullable(List.class));
        doReturn(credential).when(credentialService).getByParticipantWithCredentialType(any(), anyString());

        CreateResourceRequest createVirtualDataResourceRequest = generateMockCreateVirtualDataResourceRequest();
        createVirtualDataResourceRequest.getCredentialSubject().put(EXPIRATION_TIME, "2010-12-01 00:00:00.000");

        assertThrows(BadDataException.class, () -> resourceService.createResource(createVirtualDataResourceRequest, randomUUID));
    }

    @Test
    void testCreateResource_virtualData_parse_expiryDate_400() {

        doReturn(Optional.of(generateMockParticipant())).when(participantRepository).findById(UUID.fromString(randomUUID));
        doNothing().when(signerService).validateRequestUrl(anyList(), anyList(), nullable(String.class), anyString(), nullable(List.class));
        doReturn(credential).when(credentialService).getByParticipantWithCredentialType(any(), anyString());

        CreateResourceRequest createVirtualDataResourceRequest = generateMockCreateVirtualDataResourceRequest();
        createVirtualDataResourceRequest.getCredentialSubject().put(EXPIRATION_TIME, randomUUID);

        assertThrows(BadDataException.class, () -> resourceService.createResource(createVirtualDataResourceRequest, randomUUID));
    }

}
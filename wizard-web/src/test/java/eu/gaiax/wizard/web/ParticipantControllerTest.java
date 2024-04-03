package eu.gaiax.wizard.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.wizard.GaiaXWizardApplication;
import eu.gaiax.wizard.api.client.SignerClient;
import eu.gaiax.wizard.api.exception.BadDataException;
import eu.gaiax.wizard.api.exception.EntityNotFoundException;
import eu.gaiax.wizard.api.model.*;
import eu.gaiax.wizard.api.model.request.ParticipantCreationRequest;
import eu.gaiax.wizard.api.model.request.ParticipantRegisterRequest;
import eu.gaiax.wizard.api.model.request.ParticipantValidatorRequest;
import eu.gaiax.wizard.controller.ParticipantController;
import eu.gaiax.wizard.core.service.InvokeService;
import eu.gaiax.wizard.core.service.data_master.EntityTypeMasterService;
import eu.gaiax.wizard.core.service.data_master.SubdivisionCodeMasterService;
import eu.gaiax.wizard.core.service.domain.DomainService;
import eu.gaiax.wizard.core.service.participant.VaultService;
import eu.gaiax.wizard.dao.tenant.entity.Credential;
import eu.gaiax.wizard.dao.tenant.entity.participant.Participant;
import eu.gaiax.wizard.dao.tenant.repo.CredentialRepository;
import eu.gaiax.wizard.dao.tenant.repo.participant.ParticipantRepository;
import eu.gaiax.wizard.util.ContainerContextInitializer;
import eu.gaiax.wizard.util.HelperService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.BeforeTransaction;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static eu.gaiax.wizard.api.utils.StringPool.*;
import static eu.gaiax.wizard.util.constant.TestConstant.*;
import static eu.gaiax.wizard.utils.WizardRestConstant.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpMethod.POST;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {GaiaXWizardApplication.class})
@ActiveProfiles("test")
@ContextConfiguration(initializers = {ContainerContextInitializer.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParticipantControllerTest {

    private final String randomUUID = UUID.randomUUID().toString();
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ParticipantRepository participantRepository;
    @Autowired
    private EntityTypeMasterService entityTypeMasterService;
    @Autowired
    private SubdivisionCodeMasterService subdivisionCodeMasterService;
    @Autowired
    private ObjectMapper mapper;
    @MockBean
    @Autowired
    private SignerClient signerClient;
    @Autowired
    private ParticipantController participantController;
    @Autowired
    @SpyBean
    private CredentialRepository credentialRepository;
    @Autowired
    private MessageSource messageSource;
    @MockBean
    @Autowired
    private VaultService vaultService;
    @MockBean
    @Autowired
    private DomainService domainService;

    @BeforeEach
    @BeforeTransaction
    public final void setUp() {
        try {
            credentialRepository.deleteAll();
            participantRepository.deleteAll();
        } catch (Exception ignored) {

        }
    }

    @Test
    void register_participant_own_did_200() {
        doReturn(HelperService.getValidateRegistrationNumberResponse()).when(signerClient).validateRegistrationNumber(anyMap());
        doReturn(HelperService.getVerifyUrlResponseMock(mapper)).when(signerClient).verify(any());

        String entityTypeId = entityTypeMasterService.getAll().get(0).getId().toString();
        String subDivision = subdivisionCodeMasterService.getAll().get(0).getSubdivisionCode();
        ParticipantRegisterRequest request = HelperService.registerParticipantRequest(LEGAL_NAME, SHORT_NAME, entityTypeId,
                HelperService.prepareDefaultCredential(LEGAL_NAME, subDivision, subDivision),
                true, false, true);

        ResponseEntity<CommonResponse> response = restTemplate.exchange(REGISTER, POST, new HttpEntity<>(request), CommonResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> payload = (Map<String, Object>) response.getBody().getPayload();
        assertEquals(request.email(), payload.get("email"));
        assertEquals(request.onboardRequest().legalName(), payload.get("legalName"));
        assertEquals(request.onboardRequest().shortName(), payload.get("shortName"));
        assertNotNull(payload.get("credentialRequest"));
    }

    @Test
    void send_required_actions_email_200() {
        register_participant_own_did_200();

        ResponseEntity<CommonResponse> response = restTemplate.exchange(SEND_REQUIRED_ACTIONS_EMAIL, POST, new HttpEntity<>(new SendRegistrationEmailRequest(EMAIL)), CommonResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(response.getBody().getMessage(), messageSource.getMessage("registration.mail.sent", null, Locale.ENGLISH));
    }

    @Test
    void check_participant_registered_false() {
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(URI.create(CHECK_REGISTRATION + "?email=" + EMAIL), CommonResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> payload = (Map<String, Object>) response.getBody().getPayload();

        assertFalse((Boolean) payload.get("userRegistered"));
    }

    @Test
    void check_participant_registered_true() {
        register_participant_own_did_200();

        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(URI.create(CHECK_REGISTRATION + "?email=" + EMAIL), CommonResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> payload = (Map<String, Object>) response.getBody().getPayload();

        assertTrue((Boolean) payload.get("userRegistered"));
        assertFalse((Boolean) payload.get("deviceConfigured"));
    }

    @Test
    void initiate_onboarding_participant_own_did_200() {
        register_participant_own_did_200();

        Participant participant = participantRepository.findAll().get(0);
        Map<String, Object> validateDidResponse = new HashMap<>();
        validateDidResponse.put(DATA, Map.of(IS_VALID, true));
        doReturn(ResponseEntity.ok(validateDidResponse)).when(signerClient).validateDid(any());

        Map<String, Object> vcMap = new HashMap<>();
        vcMap.put(COMPLETE_SD, Map.of(randomUUID, randomUUID));
        doReturn(ResponseEntity.ok(Map.of(DATA, vcMap))).when(signerClient).createVc(any());

        ParticipantCreationRequest participantCreationRequest = new ParticipantCreationRequest(true, "did:web:" + randomUUID, "did:web:" + randomUUID, randomUUID, false);
        ResponseEntity<CommonResponse> response = restTemplate.exchange(ONBOARD_PARTICIPANT.replace("{participantId}", participant.getId().toString()), POST, new HttpEntity<>(participantCreationRequest), CommonResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> payload = (Map<String, Object>) response.getBody().getPayload();

        assertEquals(participantCreationRequest.issuer(), payload.get("did"));
        assertEquals(participantCreationRequest.ownDid(), payload.get("ownDidSolution"));
        assertEquals(participantCreationRequest.store(), payload.get("keyStored"));
    }

    @Test
    void initiate_onboarding_participant_no_did_200() {
        // mimics unit test behaviour
        register_participant_own_did_200();

        Participant participant = participantRepository.findAll().get(0);
        Map<String, Object> validateDidResponse = new HashMap<>();
        validateDidResponse.put(DATA, Map.of(IS_VALID, true));
        doReturn(ResponseEntity.ok(validateDidResponse)).when(signerClient).validateDid(any());
        doNothing().when(domainService).createSubDomain(any());

        Map<String, Object> vcMap = new HashMap<>();
        vcMap.put(COMPLETE_SD, Map.of(randomUUID, randomUUID));
        doReturn(ResponseEntity.ok(Map.of(DATA, vcMap))).when(signerClient).createVc(any());

        ParticipantCreationRequest participantCreationRequest = new ParticipantCreationRequest(false, null, null, null, false);
        ResponseEntity<CommonResponse> response = restTemplate.exchange(ONBOARD_PARTICIPANT.replace("{participantId}", participant.getId().toString()), POST, new HttpEntity<>(participantCreationRequest), CommonResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void validate_participant_200() {
        doReturn(HelperService.getVerifyUrlResponseMock(mapper)).when(signerClient).verify(any());
        ParticipantValidatorRequest participantValidatorRequest = new ParticipantValidatorRequest("http://localhost/" + randomUUID, "did:web:" + randomUUID, randomUUID, false, false);

        try (MockedStatic<InvokeService> invokeServiceMockedStatic = Mockito.mockStatic(InvokeService.class)) {
            invokeServiceMockedStatic.when(() -> InvokeService.executeRequest(anyString(), any())).thenReturn(HelperService.generateLegalParticipantMock(randomUUID));
            String response = participantController.validateParticipant(anyString(), participantValidatorRequest);
            assertEquals("Success", response);
        }
    }

    @Test
    void get_legal_participant_200() throws IOException {
        initiate_onboarding_participant_own_did_200();
        Participant participant = participantRepository.findAll().get(0);

        String legalParticipantJson = participantController.getLegalParticipantJson(participant.getId().toString(), "participant.json");
        assertThat(legalParticipantJson).isEqualTo(mapper.writeValueAsString(Map.of(randomUUID, randomUUID)));
    }

    @Test
    void get_well_known_file_404() {
        initiate_onboarding_participant_own_did_200();
        Participant participant = participantRepository.findAll().get(0);
        String participantId = participant.getId().toString();
        String domain = participant.getDomain();
        assertThrows(EntityNotFoundException.class, () -> participantController.getWellKnownFiles(participantId + ".key", domain));
    }

    @Test
    void get_well_known_file_200() throws IOException {
        // mimics unit test behaviour
        initiate_onboarding_participant_own_did_200();
        Participant participant = participantRepository.findAll().get(0);

        doReturn(Map.of("x509CertificateChain.pem", randomUUID)).when(vaultService).getParticipantSecretData(anyString());

        String wellKnownFile = participantController.getWellKnownFiles("x509CertificateChain.pem", participant.getDomain());
        assertThat(wellKnownFile).isEqualTo(randomUUID);
    }

    @Test
    void get_config_200() {
        initiate_onboarding_participant_own_did_200();
        Participant participant = participantRepository.findAll().get(0);

        JwtAuthenticationToken mockPrincipal = Mockito.mock(JwtAuthenticationToken.class);
        doReturn(Map.of(ID, participant.getId().toString())).when(mockPrincipal).getTokenAttributes();

        CommonResponse<ParticipantConfigDTO> participantConfigResponse = participantController.getConfig(mockPrincipal);
        assertThat(participantConfigResponse.getPayload().getId()).isEqualTo(participant.getId());
        assertThat(participantConfigResponse.getPayload().getDid()).isEqualTo("did:web:" + randomUUID);
        assertThat(participantConfigResponse.getPayload().getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void get_participant_profile_200() {
        update_participant_profile_image_200();
        Participant participant = participantRepository.findAll().get(0);

        CommonResponse<ParticipantProfileDto> participantConfigResponse = participantController.getParticipantProfile(participant.getId().toString());
        assertThat(participantConfigResponse.getPayload().getId()).isEqualTo(participant.getId().toString());
        assertThat(participantConfigResponse.getPayload().getLegalName()).isEqualTo(participant.getLegalName());
        assertThat(participantConfigResponse.getPayload().getEmail()).isEqualTo(participant.getEmail());
        assertThat(participantConfigResponse.getPayload().getShortName()).isEqualTo(participant.getShortName());
        assertThat(participantConfigResponse.getPayload().getProfileImage()).isNotNull();
    }

    @Test
    void update_participant_profile_image_400() {
        initiate_onboarding_participant_own_did_200();

        Participant participant = participantRepository.findAll().get(0);
        String participantId = participant.getId().toString();
        FileUploadRequest updateProfileImageRequest = HelperService.getValidUpdateProfileImageRequest();

        try (MockedStatic<FileUtils> fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMockedStatic.when(() -> FileUtils.copyToFile(any(), any()))
                    .thenThrow(new IOException());
            assertThrows(BadDataException.class, () -> participantController.updateParticipantProfileImage(participantId, updateProfileImageRequest));
        }
    }

    @Test
    void update_participant_profile_image_200() {
        initiate_onboarding_participant_own_did_200();
        Participant participant = participantRepository.findAll().get(0);

        CommonResponse<Map<String, Object>> participantImageUploadResponse = participantController.updateParticipantProfileImage(participant.getId().toString(), HelperService.getValidUpdateProfileImageRequest());
        assertThat(participantImageUploadResponse.getPayload().get("imageUrl")).isNotNull();
    }

    @Test
    void update_participant_profile_image_existing_image_200() {
        initiate_onboarding_participant_own_did_200();
        Participant participant = participantRepository.findAll().get(0);
        participantController.updateParticipantProfileImage(participant.getId().toString(), HelperService.getValidUpdateProfileImageRequest());

//       Update picture with deleting existing picture
        CommonResponse<Map<String, Object>> participantImageUploadResponse = participantController.updateParticipantProfileImage(participant.getId().toString(), HelperService.getValidUpdateProfileImageRequest());
        assertThat(participantImageUploadResponse.getPayload().get("imageUrl")).isNotNull();
    }

    @Test
    void delete_participant_profile_image_200() {
        update_participant_profile_image_200();
        Participant participant = participantRepository.findAll().get(0);

        assertDoesNotThrow(() -> participantController.deleteParticipantProfileImage(participant.getId().toString()));
    }

    @Test
    void delete_participant_profile_image_400() {
        initiate_onboarding_participant_own_did_200();
        Participant participant = participantRepository.findAll().get(0);
        assertThrowsExactly(BadDataException.class, () -> participantController.deleteParticipantProfileImage(participant.getId().toString()));
    }

    @Test
    void export_participant_and_key_own_did_200() {
        initiate_onboarding_participant_own_did_200();
        Participant participant = participantRepository.findAll().get(0);

        JwtAuthenticationToken mockPrincipal = Mockito.mock(JwtAuthenticationToken.class);
        doReturn(Map.of(ID, participant.getId().toString())).when(mockPrincipal).getTokenAttributes();

        CommonResponse<ParticipantAndKeyResponse> participantAndKeyResponse = participantController.exportParticipantAndKey(participant.getId().toString(), mockPrincipal);
        Credential legalParticipantCredential = credentialRepository.findByParticipantIdAndCredentialType(participant.getId(), CredentialTypeEnum.LEGAL_PARTICIPANT.getCredentialType());

        assertThat(participantAndKeyResponse.getPayload().getParticipantJson()).isEqualTo(legalParticipantCredential.getVcUrl());
    }

    @Test
    void export_participant_and_key_no_did_200() {
        // mimics unit test behaviour
        initiate_onboarding_participant_no_did_200();

        Participant participant = participantRepository.findAll().get(0);
        participant.setOwnDidSolution(false);
        participantRepository.save(participant);

        doReturn(Credential.builder()
                .participantId(participant.getId())
                .participant(participant)
                .credentialType(CredentialTypeEnum.LEGAL_PARTICIPANT.getCredentialType())
                .vcUrl("http://localhost/" + randomUUID)
                .build()
        ).when(credentialRepository).findByParticipantIdAndCredentialType(participant.getId(), CredentialTypeEnum.LEGAL_PARTICIPANT.getCredentialType());

        JwtAuthenticationToken mockPrincipal = Mockito.mock(JwtAuthenticationToken.class);
        doReturn(Map.of(ID, participant.getId().toString())).when(mockPrincipal).getTokenAttributes();

        CommonResponse<ParticipantAndKeyResponse> participantAndKeyResponse = participantController.exportParticipantAndKey(participant.getId().toString(), mockPrincipal);
        Credential legalParticipantCredential = credentialRepository.findByParticipantIdAndCredentialType(participant.getId(), CredentialTypeEnum.LEGAL_PARTICIPANT.getCredentialType());

        assertThat(participantAndKeyResponse.getPayload().getParticipantJson()).isEqualTo(legalParticipantCredential.getVcUrl());
    }

}

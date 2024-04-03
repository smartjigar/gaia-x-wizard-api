package eu.gaiax.wizard.core.service.signer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.gaiax.wizard.api.client.SignerClient;
import eu.gaiax.wizard.api.exception.BadDataException;
import eu.gaiax.wizard.api.exception.ConflictException;
import eu.gaiax.wizard.api.exception.RemoteServiceException;
import eu.gaiax.wizard.api.exception.SignerException;
import eu.gaiax.wizard.api.model.did.ServiceEndpointConfig;
import eu.gaiax.wizard.api.model.service_offer.CreateServiceOfferingRequest;
import eu.gaiax.wizard.api.model.setting.ContextConfig;
import eu.gaiax.wizard.api.utils.S3Utils;
import eu.gaiax.wizard.core.service.InvokeService;
import eu.gaiax.wizard.core.service.credential.CredentialService;
import eu.gaiax.wizard.core.service.job.ScheduleService;
import eu.gaiax.wizard.dao.tenant.entity.Credential;
import eu.gaiax.wizard.dao.tenant.entity.participant.Participant;
import eu.gaiax.wizard.dao.tenant.repo.participant.ParticipantRepository;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static eu.gaiax.wizard.api.utils.StringPool.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({OutputCaptureExtension.class, MockitoExtension.class})
class SignerServiceUnitTest {

    private final String randomUUID = UUID.randomUUID().toString();
    private ContextConfig contextConfig;
    @Mock
    private CredentialService credentialService;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private SignerClient signerClient;
    @Mock
    private S3Utils s3Utils;
    private ObjectMapper objectMapper;
    @Mock
    private ScheduleService scheduleService;
    @Mock
    private MessageSource messageSource;
    private SignerService signerService;
    private Participant participant;

    @BeforeEach
    void setUp() {
        System.setProperty("wizard.gaiax.tnc", "In publishing and graphic design, Lorem ipsum is a placeholder text commonly used to demonstrate the visual form of a document or a typeface without relying on meaningful content.");
        participant = generateMockParticipant();
        objectMapper = configureObjectMapper();
        contextConfig = new ContextConfig(
                List.of("https://www.w3.org/2018/credentials/v1", "https://w3id.org/security/suites/jws-2020/v1"),
                List.of("https://www.w3.org/2018/credentials/v1", "https://w3id.org/security/suites/jws-2020/v1", "https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#"),
                List.of("https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/participant"),
                List.of("https://www.w3.org/2018/credentials/v1,https://w3id.org/security/suites/jws-2020/v1,https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#"),
                List.of("https://www.w3.org/2018/credentials/v1,https://w3id.org/security/suites/jws-2020/v1,https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#"),
                List.of("http://www.w3.org/ns/odrl.jsonld,https://www.w3.org/ns/odrl/2/ODRL22.json"),
                List.of("https://www.w3.org/2018/credentials/v1,https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#")
        );
        ServiceEndpointConfig serviceEndpointConfig = new ServiceEndpointConfig(randomUUID, randomUUID, randomUUID);
        signerService = Mockito.spy(new SignerService(contextConfig, credentialService, participantRepository, signerClient,
                s3Utils, objectMapper, scheduleService, serviceEndpointConfig, messageSource, List.of("integrityCheck", "holderSignature", "complianceSignature", "complianceCheck"), "http://localhost/", randomUUID));
    }

    private ObjectMapper configureObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    @AfterEach
    void tearDown() {
        objectMapper = null;
        contextConfig = null;
        signerService = null;
        participant = null;
        System.clearProperty("wizard.gaiax.tnc");
    }

    @Test
    void testCreateParticipantJson_credentialExists(CapturedOutput output) {
        doReturn(Optional.of(participant)).when(participantRepository).findById(any());
        doReturn(Credential.builder().vcJson(randomUUID).vcUrl(randomUUID).build()).when(credentialService).getLegalParticipantCredential(any());
        signerService.createSignedLegalParticipant(UUID.fromString(randomUUID));

        assertThat(output.getOut()).contains("Legal Participant exists");
    }

    @Test
    void testCreateParticipantJson_credentialDoesNotExist(CapturedOutput output) {
        doReturn(Optional.of(generateMockParticipantWithDid())).when(participantRepository).findById(any());
        doReturn(null).when(credentialService).getLegalParticipantCredential(any());
        doNothing().when(signerService).addServiceEndpoint(any(), anyString(), anyString(), anyString());

        Map<String, Object> vcMap = new HashMap<>();
        vcMap.put(DATA, Map.of(randomUUID, randomUUID));
        doReturn(ResponseEntity.ok(vcMap)).when(signerClient).createVc(any());

        signerService.createSignedLegalParticipant(UUID.fromString(randomUUID));
        assertThat(output.getOut()).contains("Receive success response from signer tool");
    }

    @Test
    void testCreateSignedLegalParticipant_credentialDoesNotExist() {
        Map<String, Object> vcMap = new HashMap<>();
        vcMap.put(DATA, Map.of(randomUUID, randomUUID));
        doReturn(ResponseEntity.ok(vcMap)).when(signerClient).createVc(any());

        doReturn(null).when(credentialService).createCredential(anyString(), anyString(), anyString(), nullable(String.class), any());
        doReturn(participant).when(participantRepository).save(any());
        doNothing().when(s3Utils).uploadFile(anyString(), any());

        assertDoesNotThrow(() -> signerService.createSignedLegalParticipant(participant, randomUUID, randomUUID, randomUUID, true));
    }

    @Test
    void testCreateSignedLegalParticipant_exception(CapturedOutput output) {
        Map<String, Object> vcMap = new HashMap<>();
        vcMap.put(DATA, Map.of(randomUUID, randomUUID));
        doThrow(new BadDataException()).when(signerClient).createVc(any());
        doReturn(participant).when(participantRepository).save(any());

        assertDoesNotThrow(() -> signerService.createSignedLegalParticipant(participant, randomUUID, randomUUID, randomUUID, true));
        assertThat(output.getOut()).contains("Error while creating participant json for participant");
    }

    @Test
    void testCreateDid_didExists(CapturedOutput output) {
        doReturn(Optional.of(generateMockParticipantWithDid())).when(participantRepository).findById(any());

        try (MockedStatic<InvokeService> invokeServiceMockedStatic = Mockito.mockStatic(InvokeService.class)) {
            invokeServiceMockedStatic.when(() -> InvokeService.executeRequest(anyString(), any())).thenReturn(randomUUID);
            assertDoesNotThrow(() -> signerService.createDid(UUID.fromString(randomUUID)));
            assertThat(output).contains("DID exists for participantId " + randomUUID);
        }
    }

    @Test
    void testCreateDid_exception(CapturedOutput output) throws SchedulerException {
        doReturn(Optional.of(participant)).when(participantRepository).findById(any());
        doThrow(new BadDataException()).when(signerClient).createDid(any());

        try (MockedStatic<InvokeService> invokeServiceMockedStatic = Mockito.mockStatic(InvokeService.class)) {
            invokeServiceMockedStatic.when(() -> InvokeService.executeRequest(anyString(), any())).thenReturn(randomUUID);
            assertDoesNotThrow(() -> signerService.createDid(UUID.fromString(randomUUID)));
            assertThat(output).contains("SignerService(createDid) -> Error while creating did json for participantID -" + randomUUID);
        }
    }

    @Test
    void testCreateDid_withCertificate(CapturedOutput output) throws SchedulerException {
        doReturn(Optional.of(participant)).when(participantRepository).findById(any());
        doNothing().when(s3Utils).uploadFile(anyString(), any());
        doNothing().when(scheduleService).createJob(anyString(), anyString(), anyInt(), anyString());

        Map<String, Object> vcMap = new HashMap<>();
        vcMap.put(DATA, Map.of("did", randomUUID));
        doReturn(ResponseEntity.ok(vcMap)).when(signerClient).createDid(any());

        try (MockedStatic<InvokeService> invokeServiceMockedStatic = Mockito.mockStatic(InvokeService.class)) {
            invokeServiceMockedStatic.when(() -> InvokeService.executeRequest(anyString(), any())).thenReturn(randomUUID);
            assertDoesNotThrow(() -> signerService.createDid(UUID.fromString(randomUUID)));
            assertThat(output).contains("DID Document has been created");
        }
    }

    @Test
    void testCreateDid_withoutCertificate(CapturedOutput output) throws SchedulerException {
        doReturn(Optional.of(participant)).when(participantRepository).findById(any());
        doNothing().when(scheduleService).createJob(anyString(), anyString(), anyInt(), anyString());
        doReturn(false).when(signerService).fetchX509Certificate(anyString());

        assertDoesNotThrow(() -> signerService.createDid(UUID.fromString(randomUUID)));
        assertThat(output).contains("DID creation cron has been scheduled.");
    }

    @Test
    void testCreateDid_fetchCertificateException(CapturedOutput output) throws SchedulerException {
        doReturn(Optional.of(participant)).when(participantRepository).findById(any());

        try (MockedStatic<InvokeService> invokeServiceMockedStatic = Mockito.mockStatic(InvokeService.class)) {
            invokeServiceMockedStatic.when(() -> InvokeService.executeRequest(anyString(), any())).thenThrow(new BadDataException());
            assertDoesNotThrow(() -> signerService.createDid(UUID.fromString(randomUUID)));
            assertThat(output.getOut()).contains("Not able to fetch x509 certificate");
        }
    }

    @Test
    void testSignResource() {
        doNothing().when(s3Utils).uploadFile(anyString(), any());

        Map<String, Object> signerResponse = new HashMap<>();
        signerResponse.put(DATA, Map.of(COMPLETE_SD, Map.of(randomUUID, randomUUID)));
        doReturn(ResponseEntity.ok(signerResponse)).when(signerClient).signResource(any());

        assertThat(signerService.signResource(Map.of(randomUUID, randomUUID), UUID.fromString(randomUUID), randomUUID))
                .isEqualTo("{\"" + randomUUID + "\":\"" + randomUUID + "\"}");
    }

    @Test
    void testSignResource_exception() {
        Map<String, Object> resourceRequest = Map.of(randomUUID, randomUUID);
        UUID participantId = UUID.fromString(randomUUID);

        try (MockedStatic<FileUtils> fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMockedStatic.when(() -> FileUtils.writeStringToFile(any(), anyString(), (Charset) any()))
                    .thenThrow(new IOException());
            assertThrows(SignerException.class, () -> signerService.signResource(resourceRequest, participantId, randomUUID));
        }
    }

    @Test
    void testSignLabelLevel_200() {
        doNothing().when(s3Utils).uploadFile(anyString(), any());

        Map<String, Object> signerResponse = new HashMap<>();
        signerResponse.put(DATA, Map.of("selfDescriptionCredential", Map.of(randomUUID, randomUUID)));
        doReturn(ResponseEntity.ok(signerResponse)).when(signerClient).signLabelLevel(any());

        assertThat(signerService.signLabelLevel(Map.of(randomUUID, randomUUID), UUID.fromString(randomUUID), randomUUID)).isEqualTo("{\"" + randomUUID + "\":\"" + randomUUID + "\"}");
    }

    @Test
    void testSignLabelLevel_400() {
        UUID participantId = UUID.fromString(randomUUID);
        Map<String, Object> labelLevelRequest = Map.of(randomUUID, randomUUID);
        doThrow(new BadDataException()).when(signerClient).signLabelLevel(any());
        assertThrows(BadDataException.class, () -> signerService.signLabelLevel(labelLevelRequest, participantId, randomUUID));
    }

    @Test
    void testSignLabelLevel_409() {
        UUID participantId = UUID.fromString(randomUUID);
        Map<String, Object> labelLevelRequest = Map.of(randomUUID, randomUUID);
        doThrow(new ConflictException()).when(signerClient).signLabelLevel(any());
        assertThrows(ConflictException.class, () -> signerService.signLabelLevel(labelLevelRequest, participantId, randomUUID));
    }

    @Test
    void testSignLabelLevel_500() {
        UUID participantId = UUID.fromString(randomUUID);
        Map<String, Object> labelLevelRequest = Map.of(randomUUID, randomUUID);
        doThrow(new RuntimeException()).when(signerClient).signLabelLevel(any());
        assertThrows(SignerException.class, () -> signerService.signLabelLevel(labelLevelRequest, participantId, randomUUID));
    }

    @Test
    void testValidateRequestUrl() {
        Map<String, Object> signerResponseMap = new HashMap<>();
        String randomUUID = UUID.randomUUID().toString();
        signerResponseMap.put(DATA, Map.of(VERIFY_URL_TYPE, GX_LEGAL_PARTICIPANT));
        signerResponseMap.put("message", randomUUID);
        doReturn(ResponseEntity.ok(objectMapper.valueToTree(signerResponseMap))).when(signerClient).verify(any());

        assertDoesNotThrow(() -> signerService.validateRequestUrl(List.of(this.randomUUID), List.of(GX_LEGAL_PARTICIPANT), null, "participant.not.found", null));
    }

    @Test
    void testValidateRequestUrl_400() {
        Map<String, Object> signerResponseMap = new HashMap<>();
        String randomUUID = UUID.randomUUID().toString();
        signerResponseMap.put(DATA, Map.of(VERIFY_URL_TYPE, GX_SERVICE_OFFERING));
        signerResponseMap.put("message", randomUUID);
        doReturn(ResponseEntity.ok(objectMapper.valueToTree(signerResponseMap))).when(signerClient).verify(any());

        List<String> urlList = Collections.singletonList(this.randomUUID);
        List<String> urlTypeList = Collections.singletonList(GX_LEGAL_PARTICIPANT);
        assertThrows(BadDataException.class, () -> signerService.validateRequestUrl(urlList, urlTypeList, null, "participant.not.found", null));
    }

    @Test
    void testValidateRequestUrl_remoteException() {
        doThrow(new RemoteServiceException()).when(signerClient).verify(any());

        List<String> urlList = Collections.singletonList(randomUUID);
        List<String> urlTypeList = Collections.singletonList(GX_LEGAL_PARTICIPANT);
        assertThrows(BadDataException.class, () -> signerService.validateRequestUrl(urlList, urlTypeList, null, "participant.not.found", null));
    }

    @Test
    void testSignService() {
        doNothing().when(s3Utils).uploadFile(anyString(), any());

        Map<String, Object> serviceOfferVc = new HashMap<>();
        serviceOfferVc.put(COMPLETE_SD, new HashMap<>());
        serviceOfferVc.put(TRUST_INDEX, new HashMap<>());
        doReturn(ResponseEntity.ok(Map.of(DATA, serviceOfferVc))).when(signerClient).createServiceOfferVc(any());

        Map<String, String> signedService = signerService.signService(generateMockParticipantWithDid(), generateMockServiceOfferRequest(), randomUUID);

        assertThat(signedService)
                .containsKey(SERVICE_VC)
                .containsKey(TRUST_INDEX);
    }

    @Test
    void testSignService_exception() {
        doThrow(new BadDataException()).when(signerClient).createServiceOfferVc(any());
        Participant participantWithoutKeyStored = generateMockParticipant();
        participantWithoutKeyStored.setKeyStored(false);

        CreateServiceOfferingRequest createServiceOfferingRequest = generateMockServiceOfferRequest();
        createServiceOfferingRequest.setVerificationMethod("did:web:" + randomUUID);
        createServiceOfferingRequest.setPrivateKey(randomUUID);
        createServiceOfferingRequest.setParticipantJsonUrl(randomUUID);

        assertThrows(SignerException.class, () -> signerService.signService(participantWithoutKeyStored, createServiceOfferingRequest, randomUUID));
    }

    @Test
    void testAddServiceEndpoint() throws IOException {
        doNothing().when(s3Utils).uploadFile(anyString(), any());
        doReturn(generateMockDidFile()).when(s3Utils).getObject(anyString(), anyString());

        assertDoesNotThrow(() -> signerService.addServiceEndpoint(UUID.fromString(randomUUID), randomUUID, randomUUID, randomUUID));
    }

    private File generateMockDidFile() throws IOException {
        File updatedFile = new File(TEMP_FOLDER + UUID.randomUUID() + JSON_EXTENSION);
        Map<String, Object> didMap = new HashMap<>();
        FileUtils.writeStringToFile(updatedFile, objectMapper.writeValueAsString(didMap), Charset.defaultCharset());
        return updatedFile;
    }

    @Test
    void testValidateRegistrationNumber() {
        Map<String, Object> validateDidResponse = new HashMap<>();
        validateDidResponse.put(DATA, Map.of(IS_VALID, true));
        doReturn(ResponseEntity.ok(validateDidResponse)).when(signerClient).validateRegistrationNumber(any());

        Map<String, Object> request = new HashMap<>();
        request.put(LEGAL_REGISTRATION_NUMBER, Map.of("gx:vatID", "FR79537407926"));

        boolean isRegistrationNumberValid = signerService.validateRegistrationNumber(request);
        assertThat(isRegistrationNumberValid).isTrue();
    }

    @Test
    void testValidateRegistrationNumber_exception() {
        doThrow(new RemoteServiceException()).when(signerClient).validateRegistrationNumber(any());

        Map<String, Object> request = new HashMap<>();
        request.put(LEGAL_REGISTRATION_NUMBER, Map.of("gx:vatID", "FR79537407926"));

        boolean isRegistrationNumberValid = signerService.validateRegistrationNumber(request);
        assertThat(isRegistrationNumberValid).isFalse();
    }

    @Test
    void testValidateDid() {
        Map<String, Object> validateDidResponse = new HashMap<>();
        validateDidResponse.put(DATA, Map.of(IS_VALID, true));
        doReturn(ResponseEntity.ok(validateDidResponse)).when(signerClient).validateDid(any());

        boolean isDidValid = signerService.validateDid(randomUUID, randomUUID, randomUUID);
        assertThat(isDidValid).isTrue();
    }

    @Test
    void testValidateDid_exception() {
        doThrow(new RemoteServiceException()).when(signerClient).validateDid(any());

        boolean isDidValid = signerService.validateDid(randomUUID, randomUUID, randomUUID);
        assertThat(isDidValid).isFalse();
    }

    private Participant generateMockParticipant() {
        Participant participant = new Participant();
        participant.setId(UUID.fromString(randomUUID));
        participant.setOwnDidSolution(false);
        participant.setDomain(randomUUID);
        participant.setKeyStored(true);
        participant.setCredentialRequest("{\"legalParticipant\":{\"credentialSubject\":{\"gx:legalName\":\"Participant Example\",\"gx:headquarterAddress\":{\"gx:countrySubdivisionCode\":\"BE-BRU\"},\"gx:legalAddress\":{\"gx:countrySubdivisionCode\":\"BE-BRU\"}}},\"legalRegistrationNumber\":{\"gx:leiCode\":\"9695007586XZAKPYJ703\"}}");
        return participant;
    }

    private Participant generateMockParticipantWithDid() {
        Participant participant = new Participant();
        participant.setId(UUID.fromString(randomUUID));
        participant.setOwnDidSolution(false);
        participant.setDomain(randomUUID);
        participant.setDid("did:web:" + randomUUID);
        participant.setKeyStored(true);
        participant.setCredentialRequest("{\"legalParticipant\":{\"credentialSubject\":{\"gx:legalName\":\"Participant Example\",\"gx:headquarterAddress\":{\"gx:countrySubdivisionCode\":\"BE-BRU\"},\"gx:legalAddress\":{\"gx:countrySubdivisionCode\":\"BE-BRU\"}}},\"legalRegistrationNumber\":{\"gx:leiCode\":\"9695007586XZAKPYJ703\"}}");
        return participant;
    }

    private CreateServiceOfferingRequest generateMockServiceOfferRequest() {
        CreateServiceOfferingRequest createServiceOfferingRequest = new CreateServiceOfferingRequest();
        createServiceOfferingRequest.setName(randomUUID);
        createServiceOfferingRequest.setDescription(randomUUID);

        Map<String, Object> credentialSubject = new HashMap<>();
        credentialSubject.put(GX_POLICY, Map.of("gx:location", Collections.singletonList(randomUUID)));
        credentialSubject.put(AGGREGATION_OF, Collections.singletonList(Map.of(ID, randomUUID)));
        credentialSubject.put(DEPENDS_ON, Collections.singletonList(Map.of(ID, randomUUID)));
        credentialSubject.put(GX_TERMS_AND_CONDITIONS, Map.of("gx:URL", randomUUID));

        Map<String, Object> dataExport = new HashMap<>();
        dataExport.put(GX_REQUEST_TYPE, randomUUID);
        dataExport.put(GX_ACCESS_TYPE, randomUUID);
        dataExport.put(GX_FORMAT_TYPE, randomUUID);

        credentialSubject.put(GX_DATA_ACCOUNT_EXPORT, dataExport);
        credentialSubject.put(GX_CRITERIA, Map.of(randomUUID, randomUUID));
        createServiceOfferingRequest.setCredentialSubject(credentialSubject);
        createServiceOfferingRequest.setPrivateKey(randomUUID);
        createServiceOfferingRequest.setVerificationMethod("did:web:" + randomUUID);

        return createServiceOfferingRequest;
    }
}
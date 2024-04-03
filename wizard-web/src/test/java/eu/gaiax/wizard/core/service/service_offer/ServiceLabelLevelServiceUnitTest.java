package eu.gaiax.wizard.core.service.service_offer;

import eu.gaiax.wizard.api.model.LabelLevelFileTypeEnum;
import eu.gaiax.wizard.api.model.did.ServiceEndpointConfig;
import eu.gaiax.wizard.api.model.service_offer.LabelLevelFileUpload;
import eu.gaiax.wizard.api.model.service_offer.LabelLevelRequest;
import eu.gaiax.wizard.api.model.setting.ContextConfig;
import eu.gaiax.wizard.api.utils.S3Utils;
import eu.gaiax.wizard.core.service.credential.CredentialService;
import eu.gaiax.wizard.core.service.signer.SignerService;
import eu.gaiax.wizard.dao.tenant.entity.Credential;
import eu.gaiax.wizard.dao.tenant.entity.participant.Participant;
import eu.gaiax.wizard.dao.tenant.entity.service_offer.ServiceLabelLevel;
import eu.gaiax.wizard.dao.tenant.entity.service_offer.ServiceOffer;
import eu.gaiax.wizard.dao.tenant.repo.service_offer.ServiceLabelLevelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class ServiceLabelLevelServiceUnitTest {

    private final String randomUUID = UUID.randomUUID().toString();
    @Mock
    private S3Utils s3Utils;
    @Mock
    private SignerService signerService;
    @Mock
    private CredentialService credentialService;
    @Mock
    private ServiceLabelLevelRepository serviceLabelLevelRepository;
    private ServiceLabelLevelService serviceLabelLevelService;
    private Participant participant;

    @BeforeEach
    void setUp() {
        ContextConfig contextConfig = new ContextConfig(null, null, null, List.of("https://www.w3.org/2018/credentials/v1,https://w3id.org/security/suites/jws-2020/v1", "https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#"), null, null, null);
        ServiceEndpointConfig serviceEndpointConfig = new ServiceEndpointConfig(randomUUID, randomUUID, randomUUID);
        serviceLabelLevelService = new ServiceLabelLevelService(s3Utils, contextConfig, signerService, credentialService, null, serviceLabelLevelRepository, serviceEndpointConfig);
        participant = new Participant();
        participant.setId(UUID.fromString(randomUUID));
        participant.setOwnDidSolution(false);
    }

    @AfterEach
    void tearDown() {
        serviceLabelLevelService = null;
        participant = null;
    }

    @Test
    void testCreateLabelLevelVc() {
        doNothing().when(signerService).addServiceEndpoint(any(), anyString(), anyString(), anyString());
        doReturn(randomUUID).when(signerService).signLabelLevel(anyMap(), any(), anyString());

        LabelLevelRequest labelLevelRequest = new LabelLevelRequest(Map.of(randomUUID, randomUUID), randomUUID, randomUUID, randomUUID, true);
        Map<String, String> labelLevelVc = serviceLabelLevelService.createLabelLevelVc(labelLevelRequest, participant, randomUUID);
        assertThat(labelLevelVc).containsKey("labelLevelVc").containsKey("vcUrl");
    }

    @Test
    void testSaveServiceLabelLevelLink() {
        Credential credential = Credential.builder()
                .vcUrl(randomUUID)
                .vcJson("{}")
                .participantId(UUID.fromString(randomUUID))
                .build();
        doReturn(credential).when(credentialService).createCredential(anyString(), anyString(), anyString(), anyString(), any());

        ServiceOffer serviceOffer = ServiceOffer.builder().name(randomUUID).build();
        ServiceLabelLevel serviceLabelLevel = ServiceLabelLevel.builder()
                .credential(credential)
                .serviceOffer(serviceOffer)
                .participant(participant)
                .build();
        doReturn(serviceLabelLevel).when(serviceLabelLevelRepository).save(any());

        ServiceLabelLevel actualServiceLabelLevel = serviceLabelLevelService.saveServiceLabelLevelLink(randomUUID, randomUUID, participant, serviceOffer);
        assertThat(actualServiceLabelLevel.getServiceOffer().getName()).isEqualTo(serviceOffer.getName());
    }

    @Test
    void testUploadLabelLevelFile() throws IOException {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        doReturn("testFile.pdf").when(mockFile).getOriginalFilename();
        doReturn("testFile.pdf".getBytes()).when(mockFile).getBytes();

        doNothing().when(s3Utils).uploadFile(anyString(), any());
        doReturn(randomUUID).when(s3Utils).getObject(anyString());

        String filePath = serviceLabelLevelService.uploadLabelLevelFile(new LabelLevelFileUpload(mockFile, LabelLevelFileTypeEnum.PDF.name()));
        assertThat(filePath).isEqualTo(randomUUID);
    }
}
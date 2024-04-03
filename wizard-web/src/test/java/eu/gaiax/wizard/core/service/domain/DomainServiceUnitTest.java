package eu.gaiax.wizard.core.service.domain;

import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.model.ChangeInfo;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.amazonaws.services.route53.model.ChangeStatus;
import com.amazonaws.services.route53.model.GetChangeResult;
import eu.gaiax.wizard.api.model.setting.AWSSettings;
import eu.gaiax.wizard.api.utils.StringPool;
import eu.gaiax.wizard.core.service.job.ScheduleService;
import eu.gaiax.wizard.dao.tenant.entity.participant.Participant;
import eu.gaiax.wizard.dao.tenant.repo.participant.ParticipantRepository;
import eu.gaiax.wizard.util.constant.TestConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomainServiceUnitTest {


    private final String randomUUID = UUID.randomUUID().toString();
    private DomainService domainService;
    private AWSSettings awsSettings;
    @Mock
    private AmazonRoute53 amazonRoute53;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        awsSettings = new AWSSettings(null, null, null, null, randomUUID, "0.0.0.0", null, null);
        domainService = spy(new DomainService(awsSettings, amazonRoute53, participantRepository, scheduleService));
    }

    @AfterEach
    void tearDown() {
        domainService = null;
        awsSettings = null;
    }

    @Test
    void testUpdateTxtRecordForSSLCertificate_delete() {
        ChangeResourceRecordSetsResult changeResourceRecordSetsResult = new ChangeResourceRecordSetsResult();
        changeResourceRecordSetsResult.setChangeInfo(new ChangeInfo(randomUUID, ChangeStatus.INSYNC, new Date()));
        doReturn(changeResourceRecordSetsResult).when(amazonRoute53).changeResourceRecordSets(any());

        assertDoesNotThrow(() -> domainService.updateTxtRecords(randomUUID, randomUUID, StringPool.DELETE));
    }

    @Test
    void testUpdateTxtRecordForSSLCertificate_create() {
        ChangeResourceRecordSetsResult changeResourceRecordSetsResult = new ChangeResourceRecordSetsResult()
                .withChangeInfo(new ChangeInfo(randomUUID, ChangeStatus.PENDING, new Date()));
        doReturn(changeResourceRecordSetsResult).when(amazonRoute53).changeResourceRecordSets(any());
        doReturn(new GetChangeResult().withChangeInfo(new ChangeInfo(randomUUID, ChangeStatus.INSYNC, new Date()))).when(amazonRoute53).getChange(any());

        assertDoesNotThrow(() -> domainService.updateTxtRecords(randomUUID, randomUUID, StringPool.CREATE));
    }

    @Test
    void testCreateSubDomain() throws SchedulerException {
        Participant participant = generateMockParticipant();
        doReturn(Optional.of(participant)).when(participantRepository).findById(UUID.fromString(randomUUID));
        doReturn(participant).when(participantRepository).save(any());

        ChangeResourceRecordSetsResult changeResourceRecordSetsResult = new ChangeResourceRecordSetsResult()
                .withChangeInfo(new ChangeInfo(randomUUID, ChangeStatus.PENDING, new Date()));
        doReturn(changeResourceRecordSetsResult).when(amazonRoute53).changeResourceRecordSets(any());
        doNothing().when(scheduleService).createJob(anyString(), anyString(), anyInt(), anyString());

        assertDoesNotThrow(() -> domainService.createSubDomain(UUID.fromString(randomUUID)));
    }

    private Participant generateMockParticipant() {
        Participant participant = new Participant();
        participant.setId(UUID.fromString(randomUUID));
        participant.setDomain(TestConstant.SHORT_NAME);
        return participant;
    }
}
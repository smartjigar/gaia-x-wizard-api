/*
 * Copyright (c) 2023 | smartSense
 */

package eu.gaiax.wizard.core.service.domain;

import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.model.*;
import eu.gaiax.wizard.api.exception.EntityNotFoundException;
import eu.gaiax.wizard.api.model.RegistrationStatus;
import eu.gaiax.wizard.api.model.setting.AWSSettings;
import eu.gaiax.wizard.api.utils.StringPool;
import eu.gaiax.wizard.api.utils.TenantContext;
import eu.gaiax.wizard.core.service.job.ScheduleService;
import eu.gaiax.wizard.dao.tenant.entity.participant.Participant;
import eu.gaiax.wizard.dao.tenant.repo.participant.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DomainService {

    private final AWSSettings awsSettings;
    private final AmazonRoute53 amazonRoute53;
    private final ParticipantRepository participantRepository;
    private final ScheduleService scheduleService;

    public void updateTxtRecords(String domainName, String value, String action) {
        log.info("DomainService(updateTxtRecords) -> Txt update process initiated for domain {} with value {} and action {} ", domainName, value, action);
        ResourceRecord resourceRecord = new ResourceRecord();
        resourceRecord.setValue("\"" + value + "\"");

        ResourceRecordSet recordsSet = new ResourceRecordSet();
        recordsSet.setResourceRecords(List.of(resourceRecord));
        recordsSet.setType(RRType.TXT);
        recordsSet.setTTL(900L);
        recordsSet.setName(domainName);

        Change change = new Change(ChangeAction.fromValue(action), recordsSet);

        ChangeBatch batch = new ChangeBatch(List.of(change));

        ChangeResourceRecordSetsRequest request = new ChangeResourceRecordSetsRequest();
        request.setChangeBatch(batch);


        request.setHostedZoneId(awsSettings.hostedZoneId());
        ChangeResourceRecordSetsResult result = amazonRoute53.changeResourceRecordSets(request);

        if (action.equalsIgnoreCase(StringPool.CREATE)) {
            String status = result.getChangeInfo().getStatus();
            String changeId = result.getChangeInfo().getId();
            int count = 0;
            log.info("DomainService(updateTxtRecords) -> Status {} and count {}", status, ++count);

            while (!status.equalsIgnoreCase(ChangeStatus.INSYNC.name()) && count <= 12) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    log.error("DomainService(updateTxtRecords) -> Thread has been interrupted!", e);
                    Thread.currentThread().interrupt();
                }
                GetChangeRequest getChangeRequest = new GetChangeRequest()
                        .withId(changeId);
                ChangeInfo changeInfo = amazonRoute53.getChange(getChangeRequest).getChangeInfo();
                status = changeInfo.getStatus();
                log.info("DomainService(updateTxtRecords) -> Status {} and count {}", status, ++count);
            }
        }

        log.info("DomainService(updateTxtRecords) -> Txt record has been updated for {} with result {}", domainName, result);
    }

    public void createSubDomain(UUID participantId) {
        log.info("DomainService(createSubDomain) -> Initiate process for creating sub domain for participant {}.", participantId);
        Participant participant = participantRepository.findById(participantId).orElseThrow(() -> new EntityNotFoundException("participant.not.found"));

        try {
            String domainName = participant.getDomain();
            log.info("DomainService(createSubDomain) -> Prepare domain {} for participant {}.", domainName, participantId);
            ResourceRecord resourceRecord = new ResourceRecord();
            resourceRecord.setValue(awsSettings.serverIp());

            ResourceRecordSet recordsSet = new ResourceRecordSet();
            recordsSet.setResourceRecords(List.of(resourceRecord));
            recordsSet.setType(RRType.A);
            recordsSet.setTTL(900L);
            recordsSet.setName(domainName);

            Change change = new Change(ChangeAction.CREATE, recordsSet);

            ChangeBatch batch = new ChangeBatch(List.of(change));

            ChangeResourceRecordSetsRequest request = new ChangeResourceRecordSetsRequest();
            request.setChangeBatch(batch);

            request.setHostedZoneId(awsSettings.hostedZoneId());
            ChangeResourceRecordSetsResult result = amazonRoute53.changeResourceRecordSets(request);
            log.info("DomainService(createSubDomain) -> Subdomain {} is created for participant {} and AWS Route result is {}", domainName, participantId, result);
            participant.setStatus(RegistrationStatus.DOMAIN_CREATED.getStatus());

            //create job to create certificate
            createCertificateCreationJob(participant);
        } catch (Exception e) {
            log.error("DomainService(createSubDomain) -> Error occurred while creating the sub domain for participant {}", participant.getId(), e);
            participant.setStatus(RegistrationStatus.DOMAIN_CREATION_FAILED.getStatus());
        } finally {
            participantRepository.save(participant);
            log.debug("DomainService(createSubDomain) -> Participant details has been updated.");
        }
    }

    private void createCertificateCreationJob(Participant participant) {
        try {
            String tenantAlias = TenantContext.getCurrentTenant();
            scheduleService.createJob(participant.getId().toString(), StringPool.JOB_TYPE_CREATE_CERTIFICATE, 0, tenantAlias); //try for 3 time for certificate
            log.info("DomainService(createCertificateCreationJob) -> Process has been scheduled for participant {} with status {}", participant.getId(), StringPool.JOB_TYPE_CREATE_CERTIFICATE);
        } catch (SchedulerException e) {
            log.error("DomainService(createCertificateCreationJob) -> Process has been failed while schedule certificate creation job for participant {}", participant.getId(), e);
            participant.setStatus(RegistrationStatus.CERTIFICATE_CREATION_FAILED.getStatus());
        }
    }
}
/*
 * Copyright (c) 2023 | smartSense
 */

package eu.gaiax.wizard.core.service.job;

import eu.gaiax.wizard.api.utils.StringPool;
import eu.gaiax.wizard.api.utils.TenantContext;
import eu.gaiax.wizard.core.service.domain.DomainService;
import eu.gaiax.wizard.core.service.k8s.K8SService;
import eu.gaiax.wizard.core.service.signer.SignerService;
import eu.gaiax.wizard.core.service.ssl.CertificateService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The type Scheduled job bean.
 */
@Component
@Slf4j
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class ScheduledJobBean extends QuartzJobBean {
    private final DomainService domainService;
    private final CertificateService certificateService;
    private final K8SService k8SService;
    private final SignerService signerService;

    @SneakyThrows
    @Override
    protected void executeInternal(JobExecutionContext context) {
        JobDetail jobDetail = context.getJobDetail();
        String jobType = jobDetail.getJobDataMap().getString(StringPool.JOB_TYPE);
        UUID participantId = UUID.fromString(jobDetail.getJobDataMap().getString(StringPool.ID));
        String tenantAlias = jobDetail.getJobDataMap().getString(StringPool.JOB_TENANT_ALIAS);
        if (tenantAlias == null) {
            log.error("ScheduledJobBean(executeInternal) -> tenantAlias not found.");
            return;
        }
        log.info("ScheduledJobBean(executeInternal) -> Job execution initiate for JobType:{} Participant:{}, Tenant:{}.", jobType, participantId, tenantAlias);
        TenantContext.setCurrentTenant(tenantAlias);
        switch (jobType) {
            case StringPool.JOB_TYPE_CREATE_SUB_DOMAIN -> domainService.createSubDomain(participantId);
            case StringPool.JOB_TYPE_CREATE_CERTIFICATE ->
                    certificateService.createSSLCertificate(participantId, jobDetail.getKey());
            case StringPool.JOB_TYPE_CREATE_INGRESS -> k8SService.createIngress(participantId);
            case StringPool.JOB_TYPE_CREATE_DID -> signerService.createDid(participantId);
            case StringPool.JOB_TYPE_CREATE_PARTICIPANT -> signerService.createSignedLegalParticipant(participantId);
            default -> log.error("ScheduledJobBean(executeInternal) -> JobType {} is invalid.", jobType);
        }
        log.info("ScheduledJobBean(executeInternal) -> Job has been executed for JobType:{} Participant:{}, Tenant:{}.", jobType, participantId, tenantAlias);
    }

}

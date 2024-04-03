/*
 * Copyright (c) 2024-25 Smart Sense Consulting Solutions Pvt. Ltd.
 */
package eu.gaiax.wizard.aop;

import eu.gaiax.wizard.utils.WizardRestConstant;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * Logging aspect that logs every request and response
 *
 * @author Jigar Gadhiya
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingAspect {


    private final HttpServletRequest request;


    @Pointcut("execution(* eu.gaiax.wizard.controller.*.*(..))")
    public void printLogs() {
    }

    @SneakyThrows
    @Around("printLogs()")
    public Object logMethod(ProceedingJoinPoint joinPoint) {
        String targetClass = joinPoint.getTarget().getClass().getSimpleName();
        String targetMethod = joinPoint.getSignature().getName();
        Thread currentThread = Thread.currentThread();
        String ipAddress = request.getHeader("X-Forwarded-For");
        String tenantName = request.getHeader(WizardRestConstant.TENANT_HEADER_KEY);
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }


        log.info("==> Request : {}, Class: {}, method(s): {}, Thread :{}, IpAddress :{}, tenantName:{} ",
                request.getRequestURL(), targetClass, targetMethod, currentThread.getId(), ipAddress, tenantName);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object response;
        try {
            response = joinPoint.proceed();
        } finally {
            stopWatch.stop();
            log.info("<== Responding in time:{} ms, Thread :{} ", stopWatch.getTotalTimeMillis(), currentThread.getId());
        }
        return response;
    }
}

package com.example.payment.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.aspectj.lang.annotation.Pointcut;

@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @Pointcut("within(*..application.service..*)")
    public void serviceLayer() {}

    @Before("serviceLayer()")
    public void logBefore(JoinPoint joinPoint) {
        log.info("[AUDIT] Start: {} with args: {}", joinPoint.getSignature(), joinPoint.getArgs());
    }

    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void logAfter(JoinPoint joinPoint, Object result) {
        log.info("[AUDIT] End: {} with result: {}", joinPoint.getSignature(), result);
    }

    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        log.error("[AUDIT] Exception in {}: {}", joinPoint.getSignature(), ex.getMessage());
    }
} 
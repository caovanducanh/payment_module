package com.example.payment.apo;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    @Around("execution(* application..*.*(..))")
    public Object logUseCase(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        try {
            Object result = joinPoint.proceed();
            System.out.printf("✅ %s executed in %dms%n", method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            System.err.printf("❌ %s failed: %s%n", method, e.getMessage());
            throw e;
        }
    }
}
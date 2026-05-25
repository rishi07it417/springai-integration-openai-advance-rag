package com.demo.test.openai.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AppLogger {

    @Around("execution(* com.demo.test.openai..*.*(..))")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {

        log.info("Entering :: {}", joinPoint.getSignature());

        Object result = joinPoint.proceed();

        log.info("Exiting :: {}", joinPoint.getSignature());

        return result;
    }
}
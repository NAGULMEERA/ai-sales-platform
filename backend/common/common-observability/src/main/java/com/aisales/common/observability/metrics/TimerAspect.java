package com.aisales.common.observability.metrics;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class TimerAspect {

    private final CustomMetrics customMetrics;

    @Around("@annotation(timed)")
    public Object timeMethod(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
        Timer.Sample sample = customMetrics.startTimer(timed.value());
        try {
            return joinPoint.proceed();
        } finally {
            customMetrics.recordTimer(sample, timed.value());
        }
    }
}

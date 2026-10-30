package com.example.is.ascpect;

import com.example.is.service.StatisticsService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class StatisticsAspect {
    private final StatisticsService statisticsService;

    public StatisticsAspect(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Around(
            "execution(public * com.example.is.service..*(..))"
          + " && !within(com.example.is.service.StatisticsService)"
    )
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } finally {
            statisticsService.printStatistics();
        }
    }
}

package com.example.is.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StatisticsService {
    private final SessionFactory sessionFactory;
    private volatile boolean isOn = true;

    private static final Logger log = LoggerFactory.getLogger(StatisticsService.class);

    public StatisticsService(EntityManagerFactory factory) {
        this.sessionFactory = factory.unwrap(SessionFactory.class);
    }

    public void setIsOn(boolean isOn) {
        this.isOn = isOn;
    }

    public boolean isOn() {
        return isOn;
    }

    public void printStatistics() {
        if(!isOn) {
            return;
        }

        Statistics statistics = sessionFactory.getStatistics();

        log.info("L2 hits: " + statistics.getSecondLevelCacheHitCount());
        log.info("L2 misses: " + statistics.getSecondLevelCacheMissCount());

        log.info("\n");

        log.info("Query hits: " + statistics.getQueryCacheHitCount());
        log.info("Query misses: " + statistics.getQueryCacheMissCount());

    }
}

package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

@Service
public class CarSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(CarSchedulerService.class);

    private final CarParserService carParserService;
    private final CarNotificationService carNotificationService;
    private final ParserRunStatsService parserRunStatsService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public CarSchedulerService(CarParserService carParserService,
                               CarNotificationService carNotificationService,
                               ParserRunStatsService parserRunStatsService) {
        this.carParserService = carParserService;
        this.carNotificationService = carNotificationService;
        this.parserRunStatsService = parserRunStatsService;
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 30 * 60 * 1000)
    public void fetchAndStoreCarsScheduled() {

        String runId = UUID.randomUUID().toString().substring(0, 8);

        if (!running.compareAndSet(false, true)) {
            log.warn("[{}] Scheduler skipped: previous run still in progress", runId);
            return;
        }

        log.info("[{}] Scheduler started", runId);

        try {

            List<CarEntity> newCars = carParserService.fetchAndStoreCars();

            int newCarsCount = newCars == null ? 0 : newCars.size();

            log.info("[{}] Scheduler finished parsing/storing. New cars saved={}",
                    runId, newCarsCount);

            if (newCarsCount == 0) {
                log.info("[{}] No new cars found, notifications skipped", runId);
                return;
            }

            try {

                int sentCount = carNotificationService.notifySubscribers(newCars);

                log.info("[{}] Notifications sent={}", runId, sentCount);

            } catch (Exception e) {

                log.error("[{}] Notification step failed", runId, e);

            }

        } catch (Exception e) {

            log.error("[{}] Scheduler failed", runId, e);

        } finally {

            running.set(false);

            log.info("[{}] Scheduler unlocked", runId);
        }
    }
}
package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.UUID;

@Service
public class CarSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(CarSchedulerService.class);

    private final CarParserService carParserService;
    private final CarNotificationService carNotificationService;
    private final ParserRunStatsService parserRunStatsService;
    private final SchedulerDatabaseLock schedulerDatabaseLock;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public CarSchedulerService(CarParserService carParserService,
                               CarNotificationService carNotificationService,
                               ParserRunStatsService parserRunStatsService,
                               SchedulerDatabaseLock schedulerDatabaseLock) {
        this.carParserService = carParserService;
        this.carNotificationService = carNotificationService;
        this.parserRunStatsService = parserRunStatsService;
        this.schedulerDatabaseLock = schedulerDatabaseLock;
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 30 * 60 * 1000)
    public void fetchAndStoreCarsScheduled() {
        runScheduled("regular", carParserService::fetchAndStoreCars);
    }

    @Scheduled(cron = "0 0 9,21 * * *", zone = "Europe/Prague")
    public void fetchAndStoreTipCarsScheduled() {
        runScheduled("tipcars_xml", carParserService::fetchAndStoreTipCars);
    }

    private void runScheduled(String jobName, Supplier<List<CarEntity>> fetchCars) {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        if (!running.compareAndSet(false, true)) {
            log.warn("[{}] Scheduler skipped: previous run still in progress", runId);
            return;
        }

        log.info("[{}] Scheduler started job={}", runId, jobName);

        SchedulerDatabaseLock.LockHandle databaseLock = null;

        try {
            databaseLock = schedulerDatabaseLock.tryAcquire();

            if (databaseLock == null) {
                log.info("[{}] Scheduler skipped: another application instance is running", runId);
                return;
            }

            List<CarEntity> newCars = fetchCars.get();

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

            if (databaseLock != null) {
                databaseLock.close();
            }

            running.set(false);

            log.info("[{}] Scheduler unlocked", runId);
        }
    }
}

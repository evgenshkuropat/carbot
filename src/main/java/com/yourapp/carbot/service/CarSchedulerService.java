package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(CarSchedulerService.class);

    private final CarParserService carParserService;
    private final CarNotificationService carNotificationService;

    public CarSchedulerService(CarParserService carParserService,
                               CarNotificationService carNotificationService) {
        this.carParserService = carParserService;
        this.carNotificationService = carNotificationService;
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void fetchAndStoreCarsScheduled() {
        log.info("Car scheduler started");

        try {
            List<CarEntity> newCars = carParserService.fetchAndStoreCars();

            log.info("Car scheduler finished. New cars saved: {}", newCars.size());

            if (!newCars.isEmpty()) {
                int sentCount = carNotificationService.notifySubscribers(newCars);
                log.info("Notifications actually sent: {}", sentCount);
            }

        } catch (Exception e) {
            log.error("Car scheduler failed", e);
        }
    }
}
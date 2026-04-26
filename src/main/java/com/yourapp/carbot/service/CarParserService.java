package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.service.dto.CarDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CarParserService {

    private static final Logger log = LoggerFactory.getLogger(CarParserService.class);

    private final List<CarSourceParser> parsers;
    private final CarStorageService carStorageService;
    private final ParserRunStatsService parserRunStatsService;

    public CarParserService(List<CarSourceParser> parsers,
                            CarStorageService carStorageService,
                            ParserRunStatsService parserRunStatsService) {
        this.parsers = parsers;
        this.carStorageService = carStorageService;
        this.parserRunStatsService = parserRunStatsService;
    }

    public List<CarDto> findCars() {
        List<CarDto> allCars = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        parserRunStatsService.reset();

        for (CarSourceParser parser : parsers) {
            String sourceName = parser.getSourceName();

            try {
                List<CarDto> parsedCars = parser.fetchCars();

                if (parsedCars == null || parsedCars.isEmpty()) {
                    log.info("Parser {} returned 0 cars", sourceName);
                    parserRunStatsService.recordParserResult(sourceName, 0, 0, 0, 0);
                    continue;
                }

                int addedCount = 0;
                int duplicateCount = 0;
                int invalidCount = 0;

                for (CarDto car : parsedCars) {
                    if (car == null || car.getUrl() == null || car.getUrl().isBlank()) {
                        invalidCount++;
                        continue;
                    }

                    String normalizedUrl = car.getUrl().trim();

                    if (!seenUrls.add(normalizedUrl)) {
                        duplicateCount++;
                        continue;
                    }

                    allCars.add(car);
                    addedCount++;
                }

                parserRunStatsService.recordParserResult(
                        sourceName,
                        parsedCars.size(),
                        addedCount,
                        duplicateCount,
                        invalidCount
                );

                log.info("Parser {} returned={} added={} duplicates_skipped={} invalid_skipped={}",
                        sourceName,
                        parsedCars.size(),
                        addedCount,
                        duplicateCount,
                        invalidCount
                );

            } catch (Exception e) {
                parserRunStatsService.recordParserFailed(sourceName);

                log.error("Parser {} failed", sourceName, e);
            }
        }

        parserRunStatsService.setTotalParsedUnique(allCars.size());

        log.info("Total parsed unique cars={}", allCars.size());
        return allCars;
    }

    public List<CarEntity> fetchAndStoreCars() {
        List<CarDto> cars = findCars();

        if (cars.isEmpty()) {
            log.info("No cars parsed, nothing to store");
            parserRunStatsService.setTotalSaved(0);
            return List.of();
        }

        List<CarEntity> savedCars = carStorageService.saveNewCars(cars);

        parserRunStatsService.setTotalSaved(savedCars.size());

        log.info("Stored new cars={}", savedCars.size());

        return savedCars;
    }
}
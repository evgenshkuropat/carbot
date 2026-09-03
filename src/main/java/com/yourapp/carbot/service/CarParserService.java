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

        log.info("Registered car parsers={}", this.parsers.stream()
                .map(CarSourceParser::getSourceName)
                .toList());
    }

    public List<CarDto> findCars() {
        List<CarDto> allCars = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        parserRunStatsService.reset();

        log.info("Starting parser run parsers={}", parsers.stream()
                .map(CarSourceParser::getSourceName)
                .toList());

        for (CarSourceParser parser : parsers) {
            String sourceName = parser.getSourceName();

            log.info("Starting parser {}", sourceName);

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
        try {
            List<CarDto> cars = findCars();

            if (cars.isEmpty()) {
                log.info("No cars parsed, nothing to store");
                parserRunStatsService.setTotalSaved(0);
                return List.of();
            }

            List<CarEntity> savedCars = carStorageService.saveNewCars(cars);

            parserRunStatsService.setTotalSaved(savedCars.size());
            parserRunStatsService.recordSavedBySource(countSavedBySource(savedCars));
            logSavedBySource();

            log.info("Stored new cars={}", savedCars.size());

            return savedCars;
        } finally {
            parserRunStatsService.finish();
        }
    }

    private Map<String, Integer> countSavedBySource(List<CarEntity> savedCars) {
        Map<String, Integer> savedBySource = new LinkedHashMap<>();

        for (CarEntity car : savedCars) {
            if (car == null || car.getSource() == null || car.getSource().isBlank()) {
                continue;
            }

            savedBySource.merge(car.getSource().trim(), 1, Integer::sum);
        }

        return savedBySource;
    }

    private void logSavedBySource() {
        Map<String, ParserRunStatsService.ParserStats> stats = parserRunStatsService.getParserStats();

        for (Map.Entry<String, ParserRunStatsService.ParserStats> entry : stats.entrySet()) {
            ParserRunStatsService.ParserStats stat = entry.getValue();
            log.info("Parser {} storage_summary returned={} parsed_unique={} saved={} duplicates_skipped={} invalid_skipped={}",
                    entry.getKey(),
                    stat.returned(),
                    stat.added(),
                    stat.saved(),
                    stat.duplicatesSkipped(),
                    stat.invalidSkipped());
        }
    }
}

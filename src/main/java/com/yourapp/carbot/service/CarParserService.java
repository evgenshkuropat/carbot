package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.service.dto.CarDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CarParserService {

    private static final Logger log = LoggerFactory.getLogger(CarParserService.class);

    private final List<CarSourceParser> parsers;
    private final CarStorageService carStorageService;

    public CarParserService(List<CarSourceParser> parsers,
                            CarStorageService carStorageService) {
        this.parsers = parsers;
        this.carStorageService = carStorageService;
    }

    public List<CarDto> findCars() {
        List<CarDto> result = new ArrayList<>();

        for (CarSourceParser parser : parsers) {
            try {
                List<CarDto> parsedCars = parser.fetchCars();
                log.info("Parser {} returned {} cars", parser.getSourceName(), parsedCars.size());
                result.addAll(parsedCars);
            } catch (Exception e) {
                log.error("Parser {} failed", parser.getSourceName(), e);
            }
        }

        log.info("Total cars parsed from all sources: {}", result.size());
        return result;
    }

    public List<CarEntity> fetchAndStoreCars() {
        List<CarDto> cars = findCars();
        return carStorageService.saveNewCars(cars);
    }
}
package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.repository.CarRepository;
import com.yourapp.carbot.service.dto.CarDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CarStorageService {

    private static final Logger log =
            LoggerFactory.getLogger(CarStorageService.class);

    private final CarRepository carRepository;

    public CarStorageService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public List<CarEntity> saveNewCars(List<CarDto> cars) {

        if (cars == null || cars.isEmpty()) {
            log.info("No cars to save");
            return List.of();
        }

        Set<String> incomingUrls = extractValidUrls(cars);

        if (incomingUrls.isEmpty()) {
            log.info("No valid URLs found in incoming cars");
            return List.of();
        }

        List<CarEntity> existingCars =
                carRepository.findByUrlIn(incomingUrls);

        Set<String> existingUrls = new HashSet<>();

        for (CarEntity car : existingCars) {
            existingUrls.add(car.getUrl());
        }

        int alreadyExistingCount = existingUrls.size();

        List<CarEntity> savedCars = new ArrayList<>();

        for (CarDto car : cars) {

            if (car == null || car.getUrl() == null || car.getUrl().isBlank()) {
                continue;
            }

            if (existingUrls.contains(car.getUrl())) {
                continue;
            }

            try {

                CarEntity saved = carRepository.save(toEntity(car));

                savedCars.add(saved);

                existingUrls.add(car.getUrl());

            } catch (DataIntegrityViolationException e) {

                log.warn(
                        "Car already saved concurrently. url={}",
                        car.getUrl()
                );

            } catch (Exception e) {

                log.error(
                        "Failed to save car. url={}",
                        car.getUrl(),
                        e
                );
            }
        }

        log.info(
                "Incoming cars={}, already existing={}, newly saved={}",
                cars.size(),
                alreadyExistingCount,
                savedCars.size()
        );

        return savedCars;
    }


    private Set<String> extractValidUrls(List<CarDto> cars) {

        Set<String> urls = new LinkedHashSet<>();

        for (CarDto car : cars) {

            if (car == null) {
                continue;
            }

            String url = car.getUrl();

            if (url != null && !url.isBlank()) {
                urls.add(url);
            }
        }

        return urls;
    }


    private CarEntity toEntity(CarDto car) {
        CarEntity entity = new CarEntity();
        entity.setSource(car.getSource());
        entity.setTitle(car.getTitle());
        entity.setPrice(car.getPrice());
        entity.setPriceValue(extractPriceValue(car.getPrice()));
        entity.setLocation(car.getLocation());
        entity.setUrl(car.getUrl());
        entity.setImageUrl(car.getImageUrl());
        entity.setCreatedAt(LocalDateTime.now());

        entity.setBrand(car.getBrand());
        entity.setYear(car.getYear());
        entity.setMileage(car.getMileage());
        entity.setTransmission(car.getTransmission());

        return entity;
    }


    private Integer extractPriceValue(String price) {

        if (price == null || price.isBlank()) {
            return null;
        }

        Matcher matcher =
                Pattern.compile("\\d+").matcher(price);

        StringBuilder digits = new StringBuilder();

        while (matcher.find()) {
            digits.append(matcher.group());
        }

        if (digits.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {

            log.warn(
                    "Failed to parse price value from '{}'",
                    price
            );

            return null;
        }
    }
}
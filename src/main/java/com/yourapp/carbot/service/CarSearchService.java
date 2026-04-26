package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.UserFilterEntity;
import com.yourapp.carbot.repository.CarRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class CarSearchService {

    private final CarRepository carRepository;
    private final UserFilterService userFilterService;
    private final CarFilterMatcher carFilterMatcher;

    public CarSearchService(CarRepository carRepository,
                            UserFilterService userFilterService,
                            CarFilterMatcher carFilterMatcher) {
        this.carRepository = carRepository;
        this.userFilterService = userFilterService;
        this.carFilterMatcher = carFilterMatcher;
    }

    public List<CarEntity> findMatchingCars(Long chatId, int limit) {
        UserFilterEntity filter = userFilterService.findByChatId(chatId).orElse(null);

        if (filter == null) {
            System.out.println("DEBUG SEARCH: filter is null for chatId=" + chatId);
            return List.of();
        }

        List<CarEntity> allCars = carRepository.findAll();

        long carTypePassed = 0;
        long brandPassed = 0;
        long pricePassed = 0;
        long locationPassed = 0;
        long mileagePassed = 0;
        long fuelPassed = 0;
        long transmissionPassed = 0;
        long yearPassed = 0;
        long finalPassed = 0;

        for (CarEntity car : allCars) {
            FilterCheckResult check = carFilterMatcher.check(car, filter);

            if (check.carTypeOk()) {
                carTypePassed++;
            }
            if (check.brandOk()) {
                brandPassed++;
            }
            if (check.maxPriceOk()) {
                pricePassed++;
            }
            if (check.locationOk()) {
                locationPassed++;
            }
            if (check.mileageOk()) {
                mileagePassed++;
            }
            if (check.fuelTypeOk()) {
                fuelPassed++;
            }
            if (check.transmissionOk()) {
                transmissionPassed++;
            }
            if (check.yearOk()) {
                yearPassed++;
            }
            if (check.result()) {
                finalPassed++;
            }
        }

        System.out.println("========== DEBUG SEARCH ==========");
        System.out.println("ALL CARS IN DB = " + allCars.size());
        System.out.println("FILTER:");
        System.out.println("carType = " + filter.getCarType());
        System.out.println("brand = " + filter.getBrand());
        System.out.println("maxPrice = " + filter.getMaxPrice());
        System.out.println("location = " + filter.getLocation());
        System.out.println("maxMileage = " + filter.getMaxMileage());
        System.out.println("fuelType = " + filter.getFuelType());
        System.out.println("transmission = " + filter.getTransmission());
        System.out.println("yearFrom = " + filter.getYearFrom());
        System.out.println("----------------------------------");
        System.out.println("PASSED carType = " + carTypePassed);
        System.out.println("PASSED brand = " + brandPassed);
        System.out.println("PASSED price = " + pricePassed);
        System.out.println("PASSED location = " + locationPassed);
        System.out.println("PASSED mileage = " + mileagePassed);
        System.out.println("PASSED fuelType = " + fuelPassed);
        System.out.println("PASSED transmission = " + transmissionPassed);
        System.out.println("PASSED year = " + yearPassed);
        System.out.println("FINAL MATCHED = " + finalPassed);
        System.out.println("==================================");
        System.out.println("========== DEBUG RENAULT CARS ==========");

        allCars.stream()
                .filter(car -> "RENAULT".equalsIgnoreCase(car.getBrand()))
                .limit(30)
                .forEach(car -> System.out.println(
                        "title=" + car.getTitle()
                                + ", price=" + car.getPriceValue()
                                + ", mileage=" + car.getMileage()
                                + ", year=" + car.getYear()
                                + ", fuel=" + car.getFuelType()
                                + ", type=" + car.getCarType()
                ));

        System.out.println("========================================");

        return allCars.stream()
                .filter(car -> carFilterMatcher.matches(car, filter))
                .sorted(Comparator.comparing(CarEntity::getCreatedAt).reversed())
                .limit(limit)
                .toList();
    }


}
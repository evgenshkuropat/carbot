package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.UserFilterEntity;
import org.springframework.stereotype.Service;

@Service
public class CarFilterMatcher {

    public boolean matches(CarEntity car, UserFilterEntity filter) {
        if (car == null || filter == null) {
            return false;
        }

        if (!matchesBrand(car, filter)) {
            return false;
        }

        if (!matchesMinPrice(car, filter)) {
            return false;
        }

        if (!matchesPrice(car, filter)) {
            return false;
        }

        if (!matchesLocation(car, filter)) {
            return false;
        }

        if (!matchesMileage(car, filter)) {
            return false;
        }

        if (!matchesTransmission(car, filter)) {
            return false;
        }

        if (!matchesYear(car, filter)) {
            return false;
        }

        return true;
    }

    private boolean matchesBrand(CarEntity car, UserFilterEntity filter) {
        if (isBlank(filter.getBrand())) {
            return true;
        }

        String brand = normalize(filter.getBrand());

        if (!isBlank(car.getBrand())) {
            return normalize(car.getBrand()).equals(brand);
        }

        if (isBlank(car.getTitle())) {
            return false;
        }

        String title = normalize(car.getTitle());
        return title.contains(brand);
    }

    private boolean matchesMinPrice(CarEntity car, UserFilterEntity filter) {
        if (filter.getMinPrice() == null) {
            return true;
        }

        if (car.getPriceValue() == null) {
            return false;
        }

        return car.getPriceValue() >= filter.getMinPrice();
    }

    private boolean matchesPrice(CarEntity car, UserFilterEntity filter) {
        if (filter.getMaxPrice() == null) {
            return true;
        }

        if (car.getPriceValue() == null) {
            return false;
        }

        return car.getPriceValue() <= filter.getMaxPrice();
    }

    private boolean matchesLocation(CarEntity car, UserFilterEntity filter) {
        if (isBlank(filter.getLocation())) {
            return true;
        }

        if (isBlank(car.getLocation())) {
            return false;
        }

        String filterLocation = normalize(filter.getLocation());
        String carLocation = normalize(car.getLocation());

        return carLocation.contains(filterLocation);
    }

    private boolean matchesMileage(CarEntity car, UserFilterEntity filter) {
        if (filter.getMaxMileage() == null) {
            return true;
        }

        if (car.getMileage() == null) {
            return false;
        }

        return car.getMileage() <= filter.getMaxMileage();
    }

    private boolean matchesTransmission(CarEntity car, UserFilterEntity filter) {
        if (isBlank(filter.getTransmission())) {
            return true;
        }

        if (isBlank(car.getTransmission())) {
            return false;
        }

        return normalize(filter.getTransmission()).equals(normalize(car.getTransmission()));
    }

    private boolean matchesYear(CarEntity car, UserFilterEntity filter) {
        if (filter.getYearFrom() == null) {
            return true;
        }

        if (car.getYear() == null) {
            return false;
        }

        return car.getYear() >= filter.getYearFrom();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toUpperCase()
                .replace("Š", "S")
                .replace("Č", "C")
                .replace("Ř", "R")
                .replace("Ž", "Z")
                .replace("Ý", "Y")
                .replace("Á", "A")
                .replace("Í", "I")
                .replace("É", "E")
                .replace("Ě", "E")
                .replace("Ú", "U")
                .replace("Ů", "U");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
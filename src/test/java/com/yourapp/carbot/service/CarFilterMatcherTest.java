package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.UserFilterEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CarFilterMatcherTest {

    private final CarFilterMatcher matcher = new CarFilterMatcher();

    @Test
    void doesNotTreatAlfaQ4DrivetrainAsSuvModel() {
        UserFilterEntity filter = new UserFilterEntity();
        filter.setCarType("SUV");

        CarEntity car = new CarEntity();
        car.setTitle("Alfa Romeo 159 2006 3.2 JTS Q4 manual CZ puvod");
        car.setCarType("SEDAN");

        assertThat(matcher.check(car, filter).carTypeOk()).isFalse();
    }

    @Test
    void stillTreatsAudiQ4AsSuv() {
        UserFilterEntity filter = new UserFilterEntity();
        filter.setCarType("SUV");

        CarEntity car = new CarEntity();
        car.setTitle("Audi Q4 e-tron 40");
        car.setCarType("HATCHBACK");

        assertThat(matcher.check(car, filter).carTypeOk()).isTrue();
    }

    @Test
    void doesNotLetKnownMinivansPassStoredWagonFilter() {
        UserFilterEntity filter = new UserFilterEntity();
        filter.setCarType("WAGON");

        assertThat(matcher.check(car("Citroen C8 2.2HDI/AT/CR/Exclusive/7.Mist", "WAGON"), filter).carTypeOk())
                .isFalse();
        assertThat(matcher.check(car("Citroen Berlingo 1,6HDi 2xSOUPACKY", "WAGON"), filter).carTypeOk())
                .isFalse();
        assertThat(matcher.check(car("Peugeot Partner 1,6HDi 2xSOUPACKY", "WAGON"), filter).carTypeOk())
                .isFalse();
    }

    @Test
    void doesNotLetKnownHatchbacksPassStoredWagonFilter() {
        UserFilterEntity filter = new UserFilterEntity();
        filter.setCarType("WAGON");

        assertThat(matcher.check(car("Renault Twingo 1.2 Dynamique PANORAMA", "WAGON"), filter).carTypeOk())
                .isFalse();
    }

    private CarEntity car(String title, String carType) {
        CarEntity car = new CarEntity();
        car.setTitle(title);
        car.setCarType(carType);
        return car;
    }
}

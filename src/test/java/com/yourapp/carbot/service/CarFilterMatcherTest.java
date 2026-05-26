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
}

package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CarCardFormatterTest {

    private final CarCardFormatter formatter = new CarCardFormatter();

    @Test
    void formatsPriceFromNumericValueWhenTextPriceIsMissing() {
        CarEntity car = new CarEntity();
        car.setPriceValue(349_900);

        assertThat(formatter.formatPrice(car)).isEqualTo("349 900 Kč");
    }

    @Test
    void mapsKnownSourcesToReadableNames() {
        assertThat(formatter.formatSource("TIPCARS")).isEqualTo("TipCars.cz");
        assertThat(formatter.formatSource("USER")).isEqualTo("AutoCZ users");
    }

    @Test
    void formatsRecentListingAgeInSelectedLanguage() {
        String value = formatter.formatFreshness("cs", LocalDateTime.now().minusMinutes(10));

        assertThat(value).isEqualTo("před 10 min");
    }
}

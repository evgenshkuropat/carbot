package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CarSearchServiceTest {

    private final CarSearchService service = new CarSearchService(null, null, null);

    @Test
    void deduplicatesSameSearchResultWithDifferentUrls() throws Exception {
        CarEntity olderDuplicate = seatIbiza(
                "https://www.sbazar.cz/inzerat/older-seat-ibiza",
                LocalDateTime.now().minusMinutes(5)
        );
        CarEntity newerDuplicate = seatIbiza(
                "https://www.sbazar.cz/inzerat/newer-seat-ibiza",
                LocalDateTime.now()
        );
        CarEntity otherCar = new CarEntity();
        otherCar.setTitle("Seat Ibiza 1.2 TSI");
        otherCar.setPriceValue(129_000);
        otherCar.setYear(2014);
        otherCar.setMileage(120_000);
        otherCar.setLocation("Praha");

        List<CarEntity> deduplicated = deduplicateSearchResults(
                List.of(newerDuplicate, olderDuplicate, otherCar),
                10
        );

        assertThat(deduplicated)
                .containsExactly(newerDuplicate, otherCar);
    }

    @Test
    void hidesMalformedTipCarsRecordsFromSearch() throws Exception {
        CarEntity renaultWithVolkswagenUrl = new CarEntity();
        renaultWithVolkswagenUrl.setSource("TIPCARS");
        renaultWithVolkswagenUrl.setBrand("RENAULT");
        renaultWithVolkswagenUrl.setTitle("Renault Fluence 1.6 16V, Tempomat");
        renaultWithVolkswagenUrl.setUrl("https://www.tipcars.com/volkswagen-golf/kombi/benzin/volkswagen-golf-1-4-tsi-7042596.html");

        CarEntity renaultWithMainUrl = new CarEntity();
        renaultWithMainUrl.setSource("TIPCARS");
        renaultWithMainUrl.setBrand("RENAULT");
        renaultWithMainUrl.setTitle("Renault Fluence 1.6 16V, Tempomat");
        renaultWithMainUrl.setUrl("https://www.tipcars.com/osobni/");

        CarEntity validRenault = new CarEntity();
        validRenault.setSource("TIPCARS");
        validRenault.setBrand("RENAULT");
        validRenault.setTitle("Renault Fluence 1.6 16V, Tempomat");
        validRenault.setUrl("https://www.tipcars.com/renault-fluence/sedan/benzin/renault-fluence-1-6-16v-tempomat-7042596.html");

        assertThat(isSearchableCar(renaultWithVolkswagenUrl)).isFalse();
        assertThat(isSearchableCar(renaultWithMainUrl)).isFalse();
        assertThat(isSearchableCar(validRenault)).isTrue();
    }

    @Test
    void hidesStaleTipCarsRecordsFromSearch() throws Exception {
        CarEntity staleToyota = new CarEntity();
        staleToyota.setSource("TIPCARS");
        staleToyota.setBrand("TOYOTA");
        staleToyota.setTitle("Toyota Yaris 1.5VVT-i SELECTION CZ");
        staleToyota.setUrl("https://www.tipcars.com/toyota-yaris/hatchback/benzin/toyota-yaris-1-5vvti-selection-cz-49547096.html");
        staleToyota.setCreatedAt(LocalDateTime.now().minusDays(60));

        CarEntity freshToyota = new CarEntity();
        freshToyota.setSource("TIPCARS");
        freshToyota.setBrand("TOYOTA");
        freshToyota.setTitle("Toyota Yaris 1.5VVT-i SELECTION CZ");
        freshToyota.setUrl("https://www.tipcars.com/toyota-yaris/hatchback/benzin/toyota-yaris-1-5vvti-selection-cz-49547096.html");
        freshToyota.setCreatedAt(LocalDateTime.now().minusDays(2));

        assertThat(isSearchableCar(staleToyota)).isFalse();
        assertThat(isSearchableCar(freshToyota)).isTrue();
    }

    private CarEntity seatIbiza(String url, LocalDateTime createdAt) {
        CarEntity car = new CarEntity();
        car.setTitle("Seat Ibiza 1,4 MPI 16V 114 tis Km");
        car.setPriceValue(149_900);
        car.setYear(2015);
        car.setMileage(114_000);
        car.setLocation("v Louny");
        car.setUrl(url);
        car.setCreatedAt(createdAt);
        return car;
    }

    @SuppressWarnings("unchecked")
    private List<CarEntity> deduplicateSearchResults(List<CarEntity> cars, int limit) throws Exception {
        Method method = CarSearchService.class.getDeclaredMethod("deduplicateSearchResults", List.class, int.class);
        method.setAccessible(true);
        return (List<CarEntity>) method.invoke(service, cars, limit);
    }

    private boolean isSearchableCar(CarEntity car) throws Exception {
        Method method = CarSearchService.class.getDeclaredMethod("isSearchableCar", CarEntity.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, car);
    }
}

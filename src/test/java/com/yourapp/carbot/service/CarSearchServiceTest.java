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
}

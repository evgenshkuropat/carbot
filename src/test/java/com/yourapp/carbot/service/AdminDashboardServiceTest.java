package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.repository.CarRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminDashboardServiceTest {

    @Test
    void calculatesListingStatusSummaryForOwnerListings() {
        CarRepository carRepository = mock(CarRepository.class);
        TelegramSubscriberService subscriberService = mock(TelegramSubscriberService.class);
        FavoriteCarService favoriteCarService = mock(FavoriteCarService.class);
        when(carRepository.findTop50BySourceOrderByCreatedAtDesc("USER")).thenReturn(List.of(
                listing("PENDING"), listing("ACTIVE"), listing("ACTIVE"), listing("REJECTED")
        ));
        when(carRepository.countBySource("USER")).thenReturn(6L);
        AdminDashboardService service = new AdminDashboardService(carRepository, subscriberService, favoriteCarService);

        AdminDashboardService.OwnerListings result = service.ownerListings();

        assertThat(result.total()).isEqualTo(6L);
        assertThat(result.pending()).isEqualTo(1L);
        assertThat(result.active()).isEqualTo(2L);
        assertThat(result.rejected()).isEqualTo(1L);
        assertThat(result.inactive()).isZero();
    }

    private CarEntity listing(String status) {
        CarEntity car = new CarEntity();
        car.setListingStatus(status);
        return car;
    }
}

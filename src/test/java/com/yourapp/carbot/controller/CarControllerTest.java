package com.yourapp.carbot.controller;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.repository.CarRepository;
import com.yourapp.carbot.service.CarParserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarControllerTest {

    @Mock
    private CarParserService carParserService;

    @Mock
    private CarRepository carRepository;

    @Test
    void readsFilteredCarsFromDatabaseWithoutStartingParsers() {
        CarEntity car = new CarEntity(
                "BAZOS", "Skoda Octavia", "250000 Kč", 250000,
                "Praha", "https://example.test/car", null, LocalDateTime.now(),
                "SKODA", 2020, 80000, "PETROL", "AUTOMATIC", "WAGON"
        );

        when(carRepository.findFiltered(eq("BAZOS"), eq("Praha"), eq(300000), eq("Octavia"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(car)));

        CarController controller = new CarController(carParserService, carRepository, "admin-token");
        Page<?> result = controller.getCars(" BAZOS ", " Praha ", 300000, " Octavia ", -1, 200);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(carRepository).findFiltered(eq("BAZOS"), eq("Praha"), eq(300000), eq("Octavia"), pageableCaptor.capture());
        verifyNoInteractions(carParserService);

        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}

package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.repository.CarRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserListingServiceTest {

    private final CarRepository carRepository = mock(CarRepository.class);
    private final UserListingService service = new UserListingService(carRepository);

    @Test
    void savesCompletedDraftAsPendingUserListing() {
        UserListingService.Draft draft = completeDraft(42L);
        when(carRepository.save(any(CarEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CarEntity saved = service.submitDraft(42L, "Škoda Octavia").orElseThrow();

        assertThat(saved.getSource()).isEqualTo("USER");
        assertThat(saved.getListingStatus()).isEqualTo("PENDING");
        assertThat(saved.getOwnerChatId()).isEqualTo(42L);
        assertThat(saved.getUrl()).startsWith("user://42/");
        assertThat(service.findDraft(42L)).isEmpty();
        verify(carRepository).save(saved);
    }

    @Test
    void preventsEditingAnotherUsersListing() {
        CarEntity car = new CarEntity();
        car.setOwnerChatId(10L);
        when(carRepository.findById(5L)).thenReturn(Optional.of(car));

        boolean editingStarted = service.startEditing(20L, 5L, "price");

        assertThat(editingStarted).isFalse();
        assertThat(service.findEditSession(20L)).isEmpty();
    }

    private UserListingService.Draft completeDraft(Long chatId) {
        UserListingService.Draft draft = service.startDraft(chatId);
        draft.brand = "SKODA";
        draft.title = "Octavia";
        draft.priceValue = 350_000;
        draft.year = 2020;
        draft.mileage = 120_000;
        draft.location = "Praha";
        draft.fuelType = "PETROL";
        draft.transmission = "AUTOMATIC";
        draft.carType = "WAGON";
        draft.sellerContact = "+420 123 456 789";
        return draft;
    }
}

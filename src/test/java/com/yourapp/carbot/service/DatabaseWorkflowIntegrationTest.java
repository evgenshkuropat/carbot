package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.UserFilterEntity;
import com.yourapp.carbot.repository.CarRepository;
import com.yourapp.carbot.repository.TelegramSubscriberRepository;
import com.yourapp.carbot.repository.UserFilterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:carbot;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        UserListingService.class,
        UserFilterService.class,
        TelegramSubscriberService.class,
        NotificationSettingsService.class
})
class DatabaseWorkflowIntegrationTest {

    @Autowired
    private UserListingService userListingService;

    @Autowired
    private UserFilterService userFilterService;

    @Autowired
    private NotificationSettingsService notificationSettingsService;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserFilterRepository userFilterRepository;

    @Autowired
    private TelegramSubscriberRepository subscriberRepository;

    @Test
    void savesUserListingAndRestrictsEditingToTheOwner() {
        long ownerChatId = 101L;
        UserListingService.Draft draft = userListingService.startDraft(ownerChatId);
        draft.brand = "SKODA";
        draft.title = "Octavia Combi";
        draft.priceValue = 350_000;
        draft.year = 2020;
        draft.mileage = 120_000;
        draft.location = "Praha";
        draft.fuelType = "PETROL";
        draft.transmission = "AUTOMATIC";
        draft.carType = "WAGON";
        draft.sellerContact = "+420 123 456 789";

        CarEntity saved = userListingService.submitDraft(ownerChatId, draft.title).orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(carRepository.findById(saved.getId())).isPresent();
        assertThat(userListingService.startEditing(202L, saved.getId(), "price")).isFalse();
        assertThat(userListingService.startEditing(ownerChatId, saved.getId(), "price")).isTrue();

        UserListingService.UpdateResult result = userListingService.applyEdit(
                ownerChatId,
                userListingService.findEditSession(ownerChatId).orElseThrow(),
                "375000"
        );

        assertThat(result.status()).isEqualTo(UserListingService.UpdateResult.Status.UPDATED);
        assertThat(carRepository.findById(saved.getId()).orElseThrow().getPriceValue()).isEqualTo(375_000);
    }

    @Test
    void persistsAndClearsUserFilter() {
        UserFilterEntity filter = userFilterService.getOrCreate(303L);
        filter.setBrand("SKODA");
        filter.setMaxPrice(400_000);
        userFilterService.save(filter);

        UserFilterEntity persisted = userFilterRepository.findByChatId(303L).orElseThrow();
        assertThat(userFilterService.isConfigured(persisted)).isTrue();
        assertThat(persisted.getUpdatedAt()).isNotNull();

        userFilterService.clearFilter(303L);

        UserFilterEntity cleared = userFilterRepository.findByChatId(303L).orElseThrow();
        assertThat(userFilterService.isConfigured(cleared)).isFalse();
        assertThat(cleared.getBrand()).isNull();
        assertThat(cleared.getMaxPrice()).isNull();
    }

    @Test
    void storesNotificationPreferencesForSubscriber() {
        notificationSettingsService.setMode(404L, "digest");
        notificationSettingsService.setDailyLimit(404L, 15);
        notificationSettingsService.togglePause(404L);

        var subscriber = subscriberRepository.findByChatId(404L).orElseThrow();
        assertThat(subscriber.getNotificationMode()).isEqualTo("DIGEST");
        assertThat(subscriber.getDailyNotificationLimit()).isEqualTo(15);
        assertThat(subscriber.isNotificationsPaused()).isTrue();
        assertThat(subscriber.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}

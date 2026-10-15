package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.TelegramSubscriberEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationSettingsServiceTest {

    @Test
    void returnsCurrentSettingsForChat() {
        TelegramSubscriberService subscriberService = mock(TelegramSubscriberService.class);
        TelegramSubscriberEntity subscriber = new TelegramSubscriberEntity();
        subscriber.setChatId(42L);
        when(subscriberService.getOrCreate(42L)).thenReturn(subscriber);
        NotificationSettingsService service = new NotificationSettingsService(subscriberService);

        TelegramSubscriberEntity result = service.settings(42L);

        assertThat(result).isSameAs(subscriber);
        verify(subscriberService).getOrCreate(42L);
    }
}

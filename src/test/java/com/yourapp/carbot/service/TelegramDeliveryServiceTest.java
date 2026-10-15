package com.yourapp.carbot.service;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class TelegramDeliveryServiceTest {

    @Test
    void reportsFailedTextDeliveryWithoutThrowing() throws Exception {
        TelegramClient telegramClient = mock(TelegramClient.class);
        doThrow(new RuntimeException("Telegram unavailable"))
                .when(telegramClient).execute(any(SendMessage.class));
        TelegramDeliveryService service = new TelegramDeliveryService(telegramClient, mock(CarImageDownloadService.class));

        boolean delivered = service.sendText(42L, "Test", null);

        assertThat(delivered).isFalse();
    }
}

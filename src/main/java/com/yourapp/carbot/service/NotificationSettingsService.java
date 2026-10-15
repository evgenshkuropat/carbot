package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.TelegramSubscriberEntity;
import org.springframework.stereotype.Service;

@Service
public class NotificationSettingsService {

    private final TelegramSubscriberService subscriberService;

    public NotificationSettingsService(TelegramSubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    public TelegramSubscriberEntity settings(Long chatId) {
        return subscriberService.getOrCreate(chatId);
    }

    public TelegramSubscriberEntity togglePause(Long chatId) {
        return subscriberService.toggleNotificationsPaused(chatId);
    }

    public TelegramSubscriberEntity setMode(Long chatId, String mode) {
        return subscriberService.setNotificationMode(chatId, mode);
    }

    public TelegramSubscriberEntity setDailyLimit(Long chatId, Integer limit) {
        return subscriberService.setDailyNotificationLimit(chatId, limit);
    }
}

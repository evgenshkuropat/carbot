package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.TelegramSubscriberEntity;
import com.yourapp.carbot.repository.TelegramSubscriberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TelegramSubscriberService {

    private static final Logger log =
            LoggerFactory.getLogger(TelegramSubscriberService.class);

    private final TelegramSubscriberRepository repository;

    public TelegramSubscriberService(
            TelegramSubscriberRepository repository
    ) {
        this.repository = repository;
    }

    public void subscribe(Long chatId, String username) {

        Optional<TelegramSubscriberEntity> existing = repository.findByChatId(chatId);
        if (existing.isPresent()) {
            TelegramSubscriberEntity entity = existing.get();
            entity.setUsername(username);
            entity.setUpdatedAt(LocalDateTime.now());
            repository.save(entity);
            return;
        }

        TelegramSubscriberEntity entity =
                new TelegramSubscriberEntity();

        entity.setChatId(chatId);
        entity.setUsername(username);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        repository.save(entity);

        log.info("Subscriber added chatId={}", chatId);
    }

    public void unsubscribe(Long chatId) {

        repository.deleteByChatId(chatId);

        log.warn("Subscriber removed chatId={} (blocked bot)", chatId);
    }

    public long countAllSubscribers() {
        return repository.count();
    }

    public long countActiveSubscribers() {
        return repository.countByNotificationsPausedFalse();
    }

    public Optional<TelegramSubscriberEntity> findByChatId(Long chatId) {
        return repository.findByChatId(chatId);
    }

    public TelegramSubscriberEntity getOrCreate(Long chatId) {
        return repository.findByChatId(chatId).orElseGet(() -> {
            TelegramSubscriberEntity entity = new TelegramSubscriberEntity();
            entity.setChatId(chatId);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return repository.save(entity);
        });
    }

    public TelegramSubscriberEntity toggleNotificationsPaused(Long chatId) {
        TelegramSubscriberEntity entity = getOrCreate(chatId);
        entity.setNotificationsPaused(!entity.isNotificationsPaused());
        entity.setUpdatedAt(LocalDateTime.now());
        return repository.save(entity);
    }

    public TelegramSubscriberEntity setNotificationMode(Long chatId, String mode) {
        TelegramSubscriberEntity entity = getOrCreate(chatId);
        entity.setNotificationMode("DIGEST".equalsIgnoreCase(mode) ? "DIGEST" : "INSTANT");
        entity.setUpdatedAt(LocalDateTime.now());
        return repository.save(entity);
    }

    public TelegramSubscriberEntity setDailyNotificationLimit(Long chatId, Integer limit) {
        TelegramSubscriberEntity entity = getOrCreate(chatId);
        entity.setDailyNotificationLimit(limit == null || limit <= 0 ? null : limit);
        entity.setUpdatedAt(LocalDateTime.now());
        return repository.save(entity);
    }
}

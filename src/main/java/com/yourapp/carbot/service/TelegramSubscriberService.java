package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.TelegramSubscriberEntity;
import com.yourapp.carbot.repository.TelegramSubscriberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

        if (repository.findByChatId(chatId).isPresent()) {
            return;
        }

        TelegramSubscriberEntity entity =
                new TelegramSubscriberEntity();

        entity.setChatId(chatId);
        entity.setUsername(username);
        entity.setCreatedAt(LocalDateTime.now());

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
        return repository.count();
    }
}
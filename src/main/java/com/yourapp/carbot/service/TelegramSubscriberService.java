package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.TelegramSubscriberEntity;
import com.yourapp.carbot.repository.TelegramSubscriberRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TelegramSubscriberService {

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
    }

    public long countAllSubscribers() {
        return repository.count();
    }

    public long countActiveSubscribers() {
        return repository.count();
    }
}
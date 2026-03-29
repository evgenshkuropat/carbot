package com.yourapp.carbot.repository;

import com.yourapp.carbot.entity.TelegramSubscriberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelegramSubscriberRepository
        extends JpaRepository<TelegramSubscriberEntity, Long> {

    Optional<TelegramSubscriberEntity> findByChatId(Long chatId);
}
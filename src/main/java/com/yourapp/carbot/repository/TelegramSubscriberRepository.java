package com.yourapp.carbot.repository;

import com.yourapp.carbot.entity.TelegramSubscriberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface TelegramSubscriberRepository
        extends JpaRepository<TelegramSubscriberEntity, Long> {

    Optional<TelegramSubscriberEntity> findByChatId(Long chatId);

    @Modifying
    @Transactional
    void deleteByChatId(Long chatId);
}
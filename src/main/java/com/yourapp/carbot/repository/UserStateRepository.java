package com.yourapp.carbot.repository;

import com.yourapp.carbot.entity.UserStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStateRepository extends JpaRepository<UserStateEntity, Long> {

    Optional<UserStateEntity> findByChatId(Long chatId);
}
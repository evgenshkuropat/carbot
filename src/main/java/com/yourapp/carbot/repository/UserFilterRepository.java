package com.yourapp.carbot.repository;

import com.yourapp.carbot.entity.UserFilterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserFilterRepository extends JpaRepository<UserFilterEntity, Long> {

    Optional<UserFilterEntity> findByChatId(Long chatId);
}
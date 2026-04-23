package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.UserFilterEntity;
import com.yourapp.carbot.repository.UserFilterRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserFilterService {

    private final UserFilterRepository repository;

    public UserFilterService(UserFilterRepository repository) {
        this.repository = repository;
    }

    public UserFilterEntity getOrCreate(Long chatId) {
        return repository.findByChatId(chatId).orElseGet(() -> {
            UserFilterEntity entity = new UserFilterEntity();
            entity.setChatId(chatId);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return repository.save(entity);
        });
    }

    public void save(UserFilterEntity filter) {
        if (filter.getCreatedAt() == null) {
            filter.setCreatedAt(LocalDateTime.now());
        }

        filter.setUpdatedAt(LocalDateTime.now());
        repository.save(filter);
    }

    public Optional<UserFilterEntity> findByChatId(Long chatId) {
        return repository.findByChatId(chatId);
    }

    public void clearFilter(Long chatId) {
        Optional<UserFilterEntity> existing = repository.findByChatId(chatId);
        if (existing.isEmpty()) {
            return;
        }

        UserFilterEntity filter = existing.get();
        filter.setCarType(null);
        filter.setBrand(null);
        filter.setMaxPrice(null);
        filter.setMaxMileage(null);
        filter.setLocation(null);
        filter.setFuelType(null);
        filter.setTransmission(null);
        filter.setYearFrom(null);
        filter.setUpdatedAt(LocalDateTime.now());

        repository.save(filter);
    }
}
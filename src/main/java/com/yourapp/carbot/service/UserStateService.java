package com.yourapp.carbot.service;

import com.yourapp.carbot.bot.BotStep;
import com.yourapp.carbot.entity.UserStateEntity;
import com.yourapp.carbot.repository.UserStateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserStateService {

    private final UserStateRepository repository;

    public UserStateService(UserStateRepository repository) {
        this.repository = repository;
    }

    public UserStateEntity getOrCreate(Long chatId) {
        return repository.findByChatId(chatId)
                .orElseGet(() -> {
                    UserStateEntity entity = new UserStateEntity();
                    entity.setChatId(chatId);
                    entity.setStep(BotStep.NONE);
                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setUpdatedAt(LocalDateTime.now());
                    return repository.save(entity);
                });
    }

    public BotStep getStep(Long chatId) {
        return getOrCreate(chatId).getStep();
    }

    public void setStep(Long chatId, BotStep step) {
        UserStateEntity entity = getOrCreate(chatId);
        entity.setStep(step);
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
    }

    public void reset(Long chatId) {
        setStep(chatId, BotStep.NONE);
    }
}
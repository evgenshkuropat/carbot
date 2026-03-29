package com.yourapp.carbot.entity;

import com.yourapp.carbot.bot.BotStep;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_states")
public class UserStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long chatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BotStep step;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserStateEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public BotStep getStep() {
        return step;
    }

    public void setStep(BotStep step) {
        this.step = step;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
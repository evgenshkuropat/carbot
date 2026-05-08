package com.yourapp.carbot.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_subscribers")
public class TelegramSubscriberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long chatId;

    private String username;

    private boolean notificationsPaused = false;

    private Integer dailyNotificationLimit;

    private Integer notificationsSentToday = 0;

    private LocalDate notificationCountDate;

    private String notificationMode = "INSTANT";

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isNotificationsPaused() {
        return notificationsPaused;
    }

    public void setNotificationsPaused(boolean notificationsPaused) {
        this.notificationsPaused = notificationsPaused;
    }

    public Integer getDailyNotificationLimit() {
        return dailyNotificationLimit;
    }

    public void setDailyNotificationLimit(Integer dailyNotificationLimit) {
        this.dailyNotificationLimit = dailyNotificationLimit;
    }

    public Integer getNotificationsSentToday() {
        return notificationsSentToday;
    }

    public void setNotificationsSentToday(Integer notificationsSentToday) {
        this.notificationsSentToday = notificationsSentToday;
    }

    public LocalDate getNotificationCountDate() {
        return notificationCountDate;
    }

    public void setNotificationCountDate(LocalDate notificationCountDate) {
        this.notificationCountDate = notificationCountDate;
    }

    public String getNotificationMode() {
        return notificationMode;
    }

    public void setNotificationMode(String notificationMode) {
        this.notificationMode = notificationMode;
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

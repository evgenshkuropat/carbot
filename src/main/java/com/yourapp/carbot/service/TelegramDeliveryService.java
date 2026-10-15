package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.nio.file.Files;

@Service
public class TelegramDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(TelegramDeliveryService.class);

    private final TelegramClient telegramClient;
    private final CarImageDownloadService carImageDownloadService;

    public TelegramDeliveryService(TelegramClient telegramClient, CarImageDownloadService carImageDownloadService) {
        this.telegramClient = telegramClient;
        this.carImageDownloadService = carImageDownloadService;
    }

    public boolean sendText(Long chatId, String text, ReplyKeyboard keyboard) {
        try {
            sendTextOrThrow(chatId, text, keyboard);
            return true;
        } catch (Exception e) {
            log.warn("Telegram text delivery failed chatId={}", chatId, e);
            return false;
        }
    }

    public void sendTextOrThrow(Long chatId, String text, ReplyKeyboard keyboard) throws Exception {
        telegramClient.execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(keyboard)
                .build());
    }

    public boolean sendCarCard(Long chatId, CarEntity car, String caption, InlineKeyboardMarkup keyboard) {
        if (car != null && hasImage(car.getImageUrl())) {
            if (sendPhotoByUrl(chatId, car.getImageUrl(), caption, keyboard)) return true;
            if (sendPhotoByDownload(chatId, car, caption, keyboard)) return true;
        }

        return sendText(chatId, caption, keyboard);
    }

    private boolean sendPhotoByUrl(Long chatId, String imageUrl, String caption, InlineKeyboardMarkup keyboard) {
        try {
            telegramClient.execute(SendPhoto.builder()
                    .chatId(chatId.toString())
                    .photo(new InputFile(imageUrl))
                    .caption(caption)
                    .replyMarkup(keyboard)
                    .build());
            return true;
        } catch (Exception e) {
            log.debug("Telegram could not send image by URL chatId={} imageUrl={}", chatId, imageUrl, e);
            return false;
        }
    }

    private boolean sendPhotoByDownload(Long chatId, CarEntity car, String caption, InlineKeyboardMarkup keyboard) {
        File tempFile = null;
        try {
            tempFile = carImageDownloadService.downloadToTempFile(car);
            if (tempFile == null || !tempFile.exists() || tempFile.length() == 0) return false;
            telegramClient.execute(SendPhoto.builder()
                    .chatId(chatId.toString())
                    .photo(new InputFile(tempFile))
                    .caption(caption)
                    .replyMarkup(keyboard)
                    .build());
            return true;
        } catch (Exception e) {
            log.debug("Telegram could not send downloaded image chatId={} source={}", chatId, car.getSource(), e);
            return false;
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean hasImage(String imageUrl) {
        return imageUrl != null && !imageUrl.isBlank() && !imageUrl.toLowerCase().contains("empty.gif");
    }
}

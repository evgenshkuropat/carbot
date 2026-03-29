package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.TelegramSubscriberEntity;
import com.yourapp.carbot.entity.UserFilterEntity;
import com.yourapp.carbot.i18n.MessageService;
import com.yourapp.carbot.repository.TelegramSubscriberRepository;
import com.yourapp.carbot.repository.UserFilterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Service
public class CarNotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(CarNotificationService.class);

    private final TelegramSubscriberRepository subscriberRepository;
    private final UserFilterRepository userFilterRepository;
    private final CarFilterMatcher carFilterMatcher;
    private final TelegramClient telegramClient;
    private final MessageService messages;

    public CarNotificationService(
            TelegramSubscriberRepository subscriberRepository,
            UserFilterRepository userFilterRepository,
            CarFilterMatcher carFilterMatcher,
            TelegramClient telegramClient,
            MessageService messages
    ) {
        this.subscriberRepository = subscriberRepository;
        this.userFilterRepository = userFilterRepository;
        this.carFilterMatcher = carFilterMatcher;
        this.telegramClient = telegramClient;
        this.messages = messages;
    }

    public int notifySubscribers(List<CarEntity> cars) {

        if (cars == null || cars.isEmpty()) {
            log.info("No cars to notify");
            return 0;
        }

        int sentCount = 0;

        List<TelegramSubscriberEntity> subscribers = subscriberRepository.findAll();

        log.info("Start notifications. Subscribers={}, cars={}",
                subscribers.size(), cars.size());

        for (TelegramSubscriberEntity subscriber : subscribers) {

            if (subscriber == null || subscriber.getChatId() == null) {
                continue;
            }

            Long chatId = subscriber.getChatId();

            UserFilterEntity filter = userFilterRepository
                    .findByChatId(chatId)
                    .orElse(null);

            String lang = resolveLanguage(filter);

            int sentToSubscriber = 0;

            for (CarEntity car : cars) {

                if (car == null) {
                    continue;
                }

                if (filter == null || carFilterMatcher.matches(car, filter)) {
                    boolean sent = sendCar(chatId, car, lang);

                    if (sent) {
                        sentCount++;
                        sentToSubscriber++;
                    }
                }
            }

            log.info("Subscriber {} received {} notifications",
                    chatId, sentToSubscriber);
        }

        log.info("Notifications finished. Total sent={}", sentCount);

        return sentCount;
    }

    private boolean sendCar(Long chatId, CarEntity car, String lang) {
        try {
            InlineKeyboardMarkup keyboard = buildOpenUrlKeyboard(car.getUrl(), lang);

            if (hasImage(car.getImageUrl())) {
                SendPhoto photo = SendPhoto.builder()
                        .chatId(chatId.toString())
                        .photo(new InputFile(car.getImageUrl()))
                        .caption(format(car, lang))
                        .replyMarkup(keyboard)
                        .build();

                telegramClient.execute(photo);
            } else {
                SendMessage message = SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(format(car, lang))
                        .replyMarkup(keyboard)
                        .build();

                telegramClient.execute(message);
            }

            return true;

        } catch (Exception e) {
            log.error("Failed to send car notification. chatId={}, url={}",
                    chatId, safe(car.getUrl()), e);
            return false;
        }
    }

    private InlineKeyboardMarkup buildOpenUrlKeyboard(String url, String lang) {

        if (url == null || url.isBlank()) {
            return null;
        }

        InlineKeyboardButton openButton = InlineKeyboardButton.builder()
                .text(messages.get(lang, "car.open"))
                .url(url)
                .build();

        InlineKeyboardRow row = new InlineKeyboardRow(openButton);

        return InlineKeyboardMarkup.builder()
                .keyboardRow(row)
                .build();
    }

    private boolean hasImage(String imageUrl) {
        return imageUrl != null
                && !imageUrl.isBlank()
                && !imageUrl.toLowerCase().contains("empty.gif");
    }

    private String format(CarEntity car, String lang) {
        return """
                🚗 %s
                
                💰 %s: %s
                📍 %s: %s
                🏷 %s: %s
                """.formatted(
                safe(car.getTitle()),
                messages.get(lang, "label.price"),
                safe(car.getPrice()),
                messages.get(lang, "label.location"),
                safe(car.getLocation()),
                messages.get(lang, "label.source"),
                safe(car.getSource())
        );
    }

    private String resolveLanguage(UserFilterEntity filter) {

        if (filter == null || filter.getLanguageCode() == null || filter.getLanguageCode().isBlank()) {
            return "cs";
        }

        String lang = filter.getLanguageCode().toLowerCase();

        if (lang.startsWith("ru")) {
            return "ru";
        }

        if (lang.startsWith("uk")) {
            return "uk";
        }

        if (lang.startsWith("en")) {
            return "en";
        }

        return "cs";
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
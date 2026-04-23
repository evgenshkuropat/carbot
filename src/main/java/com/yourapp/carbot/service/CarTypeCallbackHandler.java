package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.UserFilterEntity;
import com.yourapp.carbot.i18n.MessageService;
import com.yourapp.carbot.repository.UserFilterRepository;
import com.yourapp.carbot.util.FilterValueUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
public class CarTypeCallbackHandler {

    private static final String PREFIX = "filter:carType:";

    private final UserFilterRepository userFilterRepository;
    private final CarTypeKeyboardFactory carTypeKeyboardFactory;
    private final MessageService messages;
    private final TelegramClient telegramClient;

    public CarTypeCallbackHandler(UserFilterRepository userFilterRepository,
                                  CarTypeKeyboardFactory carTypeKeyboardFactory,
                                  MessageService messages,
                                  TelegramClient telegramClient) {
        this.userFilterRepository = userFilterRepository;
        this.carTypeKeyboardFactory = carTypeKeyboardFactory;
        this.messages = messages;
        this.telegramClient = telegramClient;
    }

    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(PREFIX);
    }

    public boolean handle(Long chatId, Integer messageId, String callbackData, String languageCode) {
        if (!supports(callbackData) || chatId == null || messageId == null) {
            return false;
        }

        String action = callbackData.substring(PREFIX.length()).trim();

        UserFilterEntity filter = userFilterRepository.findByChatId(chatId)
                .orElseGet(() -> {
                    UserFilterEntity entity = new UserFilterEntity();
                    entity.setChatId(chatId);
                    entity.setLanguageCode(resolveLanguage(languageCode));
                    entity.setCarType("ANY");
                    return entity;
                });

        String lang = resolveLanguage(
                filter.getLanguageCode() != null && !filter.getLanguageCode().isBlank()
                        ? filter.getLanguageCode()
                        : languageCode
        );

        try {
            if ("done".equalsIgnoreCase(action)) {
                filter.setCarType(FilterValueUtils.normalizeStoredValue(filter.getCarType()));
                userFilterRepository.save(filter);

                EditMessageText edit = EditMessageText.builder()
                        .chatId(chatId.toString())
                        .messageId(messageId)
                        .text(buildDoneText(lang, filter.getCarType()))
                        .build();

                telegramClient.execute(edit);
                return true;
            }

            String updatedValue = FilterValueUtils.toggleMultiValue(filter.getCarType(), action);
            filter.setCarType(updatedValue);
            filter.setLanguageCode(lang);
            userFilterRepository.save(filter);

            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .text(messages.getOrDefault(
                            lang,
                            "filter.chooseCarType",
                            "Выберите один или несколько типов кузова:"
                    ))
                    .replyMarkup(carTypeKeyboardFactory.build(filter.getCarType(), lang))
                    .build();

            telegramClient.execute(edit);
            return true;

        } catch (TelegramApiException e) {
            try {
                EditMessageReplyMarkup fallback = EditMessageReplyMarkup.builder()
                        .chatId(chatId.toString())
                        .messageId(messageId)
                        .replyMarkup(carTypeKeyboardFactory.build(filter.getCarType(), lang))
                        .build();

                telegramClient.execute(fallback);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private String buildDoneText(String lang, String storedCarType) {
        return messages.getOrDefault(
                lang,
                "filter.chooseCarType",
                "Выберите один или несколько типов кузова:"
        ) + "\n\n" + messages.getOrDefault(
                lang,
                "common.done",
                "Готово"
        ) + ": " + formatSelectedTypes(lang, storedCarType);
    }

    private String formatSelectedTypes(String lang, String storedCarType) {
        if (FilterValueUtils.isAny(storedCarType)) {
            return messages.getOrDefault(lang, "common.any", "Any");
        }

        StringBuilder result = new StringBuilder();

        for (String raw : storedCarType.split(",")) {
            String value = raw == null ? "" : raw.trim().toUpperCase();

            if (value.isBlank()) {
                continue;
            }

            String label = switch (value) {
                case "SEDAN" -> messages.getOrDefault(lang, "carType.sedan", "Sedan");
                case "HATCHBACK" -> messages.getOrDefault(lang, "carType.hatchback", "Hatchback");
                case "WAGON" -> messages.getOrDefault(lang, "carType.wagon", "Wagon");
                case "SUV" -> messages.getOrDefault(lang, "carType.suv", "SUV");
                case "MINIVAN" -> messages.getOrDefault(lang, "carType.minivan", "Minivan");
                default -> value;
            };

            if (!result.isEmpty()) {
                result.append(", ");
            }
            result.append(label);
        }

        return result.isEmpty()
                ? messages.getOrDefault(lang, "common.any", "Any")
                : result.toString();
    }

    private String resolveLanguage(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return "cs";
        }

        String lang = languageCode.toLowerCase();

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
}
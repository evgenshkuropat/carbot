package com.yourapp.carbot.service;

import com.yourapp.carbot.i18n.MessageService;
import com.yourapp.carbot.util.FilterValueUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
public class CarTypeKeyboardFactory {

    private final MessageService messages;

    public CarTypeKeyboardFactory(MessageService messages) {
        this.messages = messages;
    }

    public InlineKeyboardMarkup build(String currentValue, String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                button(
                        mark(FilterValueUtils.containsValue(currentValue, "ANY"),
                                messages.getOrDefault(lang, "common.any", "Any")),
                        "filter:carType:ANY"
                )
        ));

        rows.add(new InlineKeyboardRow(
                button(
                        mark(FilterValueUtils.containsValue(currentValue, "SEDAN"),
                                messages.getOrDefault(lang, "carType.sedan", "Sedan")),
                        "filter:carType:SEDAN"
                ),
                button(
                        mark(FilterValueUtils.containsValue(currentValue, "HATCHBACK"),
                                messages.getOrDefault(lang, "carType.hatchback", "Hatchback")),
                        "filter:carType:HATCHBACK"
                )
        ));

        rows.add(new InlineKeyboardRow(
                button(
                        mark(FilterValueUtils.containsValue(currentValue, "WAGON"),
                                messages.getOrDefault(lang, "carType.wagon", "Wagon")),
                        "filter:carType:WAGON"
                ),
                button(
                        mark(FilterValueUtils.containsValue(currentValue, "SUV"),
                                messages.getOrDefault(lang, "carType.suv", "SUV")),
                        "filter:carType:SUV"
                )
        ));

        rows.add(new InlineKeyboardRow(
                button(
                        mark(FilterValueUtils.containsValue(currentValue, "MINIVAN"),
                                messages.getOrDefault(lang, "carType.minivan", "Minivan")),
                        "filter:carType:MINIVAN"
                )
        ));

        rows.add(new InlineKeyboardRow(
                button(
                        messages.getOrDefault(lang, "common.done", "Done"),
                        "filter:carType:done"
                )
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    private InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    private String mark(boolean selected, String label) {
        return (selected ? "✅ " : "▫️ ") + label;
    }
}
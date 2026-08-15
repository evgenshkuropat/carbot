package com.yourapp.carbot.bot;

import com.yourapp.carbot.i18n.MessageService;
import com.yourapp.carbot.entity.TelegramSubscriberEntity;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class CarBotKeyboardFactory {

    private final MessageService messages;

    public CarBotKeyboardFactory(MessageService messages) {
        this.messages = messages;
    }

    public ReplyKeyboard mainMenuKeyboard(String lang) {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🔍 " + messages.get(lang, "menu.search")));
        row1.add(new KeyboardButton("⚙️ " + messages.get(lang, "menu.filter")));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📋 " + messages.get(lang, "menu.myFilter")));
        row2.add(new KeyboardButton("🧰 " + messages.get(lang, "menu.services")));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("⭐ " + messages.get(lang, "menu.favorites")));
        row3.add(new KeyboardButton("🌐 " + messages.get(lang, "menu.language")));

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton(sellMenuText(lang)));

        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2, row3, row4))
                .resizeKeyboard(true)
                .selective(true)
                .build();
    }

    public InlineKeyboardMarkup servicesKeyboard(String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(singleButtonRow(myCarsMenuText(lang), "sell_mycars"));
        rows.add(singleButtonRow(
                "\uD83C\uDDFA\uD83C\uDDE6 " + messages.get(lang, "services.dpDocument"),
                "service_dp_document"
        ));

        rows.add(singleUrlButtonRow(
                "🏠 " + messages.get(lang, "services.housingBot"),
                "https://t.me/zhytloCZ_bot"
        ));

        rows.add(singleUrlButtonRow(
                "✉️ " + messages.get(lang, "services.feedback"),
                "https://t.me/evzen_cz"
        ));

        rows.add(singleUrlButtonRow(
                "☕ " + messages.get(lang, "services.supportProject"),
                "https://revolut.me/evzen13"
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup dpDocumentKeyboard(String lang) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(singleUrlButtonRow(
                        "\uD83D\uDCF1 " + messages.get(lang, "services.dpDocument.open"),
                        "https://t.me/dpdoc_prague"
                )))
                .build();
    }

    public InlineKeyboardMarkup languageKeyboard() {
        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(InlineKeyboardButton.builder().text("🇺🇦 Українська").callbackData("lang:uk").build());
        row1.add(InlineKeyboardButton.builder().text("🇨🇿 Čeština").callbackData("lang:cs").build());

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(InlineKeyboardButton.builder().text("🇬🇧 English").callbackData("lang:en").build());
        row2.add(InlineKeyboardButton.builder().text("🇷🇺 Русский").callbackData("lang:ru").build());

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .build();
    }

    public InlineKeyboardMarkup afterLanguageChangedKeyboard(String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(singleButtonRow(
                "📋 " + messages.get(lang, "button.showFilter"),
                "show_myfilter"
        ));

        rows.add(singleButtonRow(
                "⚙️ " + messages.get(lang, "button.editFilter"),
                "myfilter_edit"
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup myFilterActionsKeyboard(String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(singleButtonRow(
                "🔍 " + messages.get(lang, "button.findCars"),
                "myfilter_find"
        ));

        rows.add(twoButtonsRow(
                "✏️ " + messages.get(lang, "button.editFilter"),
                "myfilter_edit",
                "↩️ " + messages.get(lang, "button.resetFilter"),
                "myfilter_reset"
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup myFilterEditFieldsKeyboard(String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(twoButtonsRow(
                messages.get(lang, "label.carType"),
                "myfilter_field:car_type",
                messages.get(lang, "label.brand"),
                "myfilter_field:brand"
        ));

        rows.add(singleButtonRow(
                messages.get(lang, "label.model"),
                "myfilter_field:model_query"
        ));

        rows.add(twoButtonsRow(
                messages.get(lang, "label.maxPrice"),
                "myfilter_field:max_price",
                messages.get(lang, "label.location"),
                "myfilter_field:location"
        ));

        rows.add(twoButtonsRow(
                messages.get(lang, "label.maxMileage"),
                "myfilter_field:max_mileage",
                messages.get(lang, "label.transmission"),
                "myfilter_field:transmission"
        ));

        rows.add(twoButtonsRow(
                messages.get(lang, "label.fuelType"),
                "myfilter_field:fuel_type",
                messages.get(lang, "label.yearFrom"),
                "myfilter_field:year_from"
        ));

        rows.add(singleButtonRow(
                "⬅️ " + messages.get(lang, "button.prev"),
                "show_myfilter"
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup resetConfirmKeyboard(String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(twoButtonsRow(
                resetConfirmText(lang),
                "myfilter_reset_confirm",
                cancelText(lang),
                "myfilter_reset_cancel"
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup notificationSettingsKeyboard(String lang, TelegramSubscriberEntity subscriber) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        boolean paused = subscriber != null && subscriber.isNotificationsPaused();
        String mode = subscriber == null || subscriber.getNotificationMode() == null
                ? "INSTANT"
                : subscriber.getNotificationMode();

        rows.add(singleButtonRow(
                (paused ? resumeNotificationsText(lang) : pauseNotificationsText(lang)),
                "notif_pause_toggle"
        ));

        rows.add(twoButtonsRow(
                selectableText("INSTANT".equalsIgnoreCase(mode), instantModeText(lang)),
                "notif_mode:INSTANT",
                selectableText("DIGEST".equalsIgnoreCase(mode), digestModeText(lang)),
                "notif_mode:DIGEST"
        ));

        rows.add(twoButtonsRow("5 / day", "notif_limit:5", "10 / day", "notif_limit:10"));
        rows.add(twoButtonsRow("20 / day", "notif_limit:20", noDailyLimitText(lang), "notif_limit:0"));

        rows.add(singleButtonRow(
                "⬅️ " + messages.get(lang, "button.prev"),
                "show_myfilter"
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup myFilterResetKeyboard(String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(singleButtonRow(
                "➕ " + messages.get(lang, "button.createNewFilter"),
                "myfilter_edit"
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup carTypeKeyboard(String lang, String selectedCarTypes, boolean showBack) {
        Set<String> selected = parseSelectedValues(selectedCarTypes);
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("SEDAN"), messages.get(lang, "carType.SEDAN")),
                "car_type:toggle:SEDAN",
                buildSelectableText(selected.contains("HATCHBACK"), messages.get(lang, "carType.HATCHBACK")),
                "car_type:toggle:HATCHBACK"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("WAGON"), messages.get(lang, "carType.WAGON")),
                "car_type:toggle:WAGON",
                buildSelectableText(selected.contains("SUV"), messages.get(lang, "carType.SUV")),
                "car_type:toggle:SUV"
        ));

        rows.add(singleButtonRow(
                buildSelectableText(selected.contains("MINIVAN"), messages.get(lang, "carType.MINIVAN")),
                "car_type:toggle:MINIVAN"
        ));

        rows.add(singleButtonRow(
                anyOptionText(lang),
                "car_type:any"
        ));

        rows.add(singleButtonRow(
                "✅ " + messages.get(lang, "common.done"),
                "car_type:done"
        ));

        if (showBack) {
            rows.add(singleButtonRow(
                    "⬅️ " + messages.get(lang, "button.prev"),
                    "wizard_back:menu"
            ));
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup brandKeyboard(String lang, String selectedBrands, boolean showBack) {
        Set<String> selected = parseSelectedValues(selectedBrands);
        List<InlineKeyboardRow> rows = new ArrayList<>();

        addBrandRows(rows, lang, selected, "brand:toggle:",
                "SKODA", "VOLKSWAGEN",
                "TOYOTA", "FORD",
                "RENAULT", "HYUNDAI",
                "KIA", "PEUGEOT",
                "CITROEN", "OPEL",
                "BMW", "AUDI",
                "MERCEDES", "SEAT",
                "DACIA", "FIAT"
        );

        rows.add(twoButtonsRow(
                "➕ " + moreBrandsText(lang),
                "brand_page:other",
                anyOptionText(lang),
                "brand:any"
        ));

        rows.add(singleButtonRow(
                "✅ " + messages.get(lang, "common.done"),
                "brand:done"
        ));

        if (showBack) {
            rows.add(singleButtonRow(
                    "⬅️ " + messages.get(lang, "button.prev"),
                    "wizard_back:car_type"
            ));
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup brandOtherKeyboard(String lang, String selectedBrands, boolean showBack) {
        Set<String> selected = parseSelectedValues(selectedBrands);
        List<InlineKeyboardRow> rows = new ArrayList<>();

        addBrandRows(rows, lang, selected, "brand_other:toggle:",
                "MAZDA", "HONDA",
                "VOLVO", "NISSAN",
                "SUZUKI", "TESLA",
                "CUPRA", "LEXUS",
                "BYD", "JEEP",
                "SUBARU", "MITSUBISHI",
                "PORSCHE", "LAND_ROVER",
                "MINI", "ALFA_ROMEO",
                "CHEVROLET", "DS",
                "DODGE", "MG"
        );

        rows.add(twoButtonsRow(
                "⬅️ " + mainBrandsText(lang),
                "brand_page:main",
                "✅ " + messages.get(lang, "common.done"),
                "brand:done"
        ));

        if (showBack) {
            rows.add(singleButtonRow(
                    "⬅️ " + messages.get(lang, "button.prev"),
                    "wizard_back:car_type"
            ));
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup modelQueryKeyboard(String lang, boolean showBack) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(singleButtonRow(
                "⏭ " + messages.get(lang, "button.skip"),
                "model_query:skip"
        ));

        if (showBack) {
            rows.add(singleButtonRow(
                    "⬅️ " + messages.get(lang, "button.prev"),
                    "model_query:back"
            ));
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    private void addBrandRows(
            List<InlineKeyboardRow> rows,
            String lang,
            Set<String> selected,
            String callbackPrefix,
            String... brandCodes
    ) {
        for (int i = 0; i < brandCodes.length; i += 2) {
            String first = brandCodes[i];
            String second = i + 1 < brandCodes.length ? brandCodes[i + 1] : null;

            if (second == null) {
                rows.add(singleButtonRow(
                        buildSelectableText(selected.contains(first), messages.get(lang, "brand." + first)),
                        callbackPrefix + first
                ));
            } else {
                rows.add(twoButtonsRow(
                        buildSelectableText(selected.contains(first), messages.get(lang, "brand." + first)),
                        callbackPrefix + first,
                        buildSelectableText(selected.contains(second), messages.get(lang, "brand." + second)),
                        callbackPrefix + second
                ));
            }
        }
    }

    private String moreBrandsText(String lang) {
        return switch (lang) {
            case "ru" -> "Другие марки";
            case "uk" -> "Інші марки";
            case "cs" -> "Další značky";
            default -> "More brands";
        };
    }

    private String mainBrandsText(String lang) {
        return switch (lang) {
            case "ru" -> "Основные марки";
            case "uk" -> "Основні марки";
            case "cs" -> "Hlavní značky";
            default -> "Main brands";
        };
    }

    public InlineKeyboardMarkup maxPriceKeyboard(String lang, boolean showBack) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(twoButtonsRow("30 000 Kč", "max_price:30000", "50 000 Kč", "max_price:50000"));
        rows.add(twoButtonsRow("80 000 Kč", "max_price:80000", "100 000 Kč", "max_price:100000"));
        rows.add(twoButtonsRow("150 000 Kč", "max_price:150000", "200 000 Kč", "max_price:200000"));
        rows.add(twoButtonsRow("300 000 Kč", "max_price:300000", "500 000 Kč", "max_price:500000"));
        rows.add(twoButtonsRow("700 000 Kč", "max_price:700000", "1 000 000 Kč", "max_price:1000000"));
        rows.add(singleButtonRow("1 500 000 Kč", "max_price:1500000"));

        rows.add(singleButtonRow(
                noLimitOptionText(lang),
                "max_price:0"
        ));

        if (showBack) {
            rows.add(twoButtonsRow(
                    "⬅️ " + messages.get(lang, "button.prev"),
                    "wizard_back:model_query",
                    "⏭ " + messages.get(lang, "button.skip"),
                    "max_price:0"
            ));
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup locationKeyboard(String lang, boolean showBack) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(twoButtonsRow(
                messages.get(lang, "location.PRAHA"), "location:PRAHA",
                messages.get(lang, "location.STREDOCESKY"), "location:STREDOCESKY"
        ));

        rows.add(twoButtonsRow(
                messages.get(lang, "location.JIHOMORAVSKY"), "location:JIHOMORAVSKY",
                messages.get(lang, "location.MORAVSKOSLEZSKY"), "location:MORAVSKOSLEZSKY"
        ));

        rows.add(twoButtonsRow(
                messages.get(lang, "location.USTECKY"), "location:USTECKY",
                messages.get(lang, "location.PLZENSKY"), "location:PLZENSKY"
        ));

        rows.add(twoButtonsRow(
                messages.get(lang, "location.JIHOCESKY"), "location:JIHOCESKY",
                messages.get(lang, "location.KRALOVEHRADECKY"), "location:KRALOVEHRADECKY"
        ));

        rows.add(twoButtonsRow(
                messages.get(lang, "location.LIBERECKY"), "location:LIBERECKY",
                messages.get(lang, "location.OLOMOUCKY"), "location:OLOMOUCKY"
        ));

        rows.add(twoButtonsRow(
                messages.get(lang, "location.PARDUBICKY"), "location:PARDUBICKY",
                messages.get(lang, "location.ZLINSKY"), "location:ZLINSKY"
        ));

        rows.add(twoButtonsRow(
                messages.get(lang, "location.VYSOCINA"), "location:VYSOCINA",
                messages.get(lang, "location.KARLOVARSKY"), "location:KARLOVARSKY"
        ));

        rows.add(singleButtonRow(
                anyOptionText(lang),
                "location:ANY"
        ));

        if (showBack) {
            rows.add(twoButtonsRow(
                    "⬅️ " + messages.get(lang, "button.prev"),
                    "wizard_back:max_price",
                    "⏭ " + messages.get(lang, "button.skip"),
                    "location:ANY"
            ));
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup mileageKeyboard(String lang, boolean showBack) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(twoButtonsRow("50 000 km", "mileage:50000", "100 000 km", "mileage:100000"));
        rows.add(twoButtonsRow("150 000 km", "mileage:150000", "200 000 km", "mileage:200000"));
        rows.add(twoButtonsRow("250 000 km", "mileage:250000", "300 000 km", "mileage:300000"));

        rows.add(singleButtonRow(
                noLimitOptionText(lang),
                "mileage:0"
        ));

        if (showBack) {
            rows.add(twoButtonsRow(
                    "⬅️ " + messages.get(lang, "button.prev"),
                    "wizard_back:location",
                    "⏭ " + messages.get(lang, "button.skip"),
                    "mileage:0"
            ));
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup transmissionKeyboard(String lang, boolean showBack) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(twoButtonsRow(
                messages.get(lang, "transmission.MANUAL"), "transmission:MANUAL",
                messages.get(lang, "transmission.AUTOMATIC"), "transmission:AUTOMATIC"
        ));

        rows.add(singleButtonRow(
                anyOptionText(lang),
                "transmission:ANY"
        ));

        if (showBack) {
            rows.add(twoButtonsRow(
                    "⬅️ " + messages.get(lang, "button.prev"),
                    "wizard_back:max_mileage",
                    "⏭ " + messages.get(lang, "button.skip"),
                    "transmission:ANY"
            ));
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup fuelTypeKeyboard(String lang, boolean showBack) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(twoButtonsRow(
                messages.get(lang, "fuelType.PETROL"), "fuel_type:PETROL",
                messages.get(lang, "fuelType.DIESEL"), "fuel_type:DIESEL"
        ));

        rows.add(twoButtonsRow(
                messages.get(lang, "fuelType.HYBRID"), "fuel_type:HYBRID",
                messages.get(lang, "fuelType.PLUGIN_HYBRID"), "fuel_type:PLUGIN_HYBRID"
        ));

        rows.add(twoButtonsRow(
                messages.get(lang, "fuelType.ELECTRIC"), "fuel_type:ELECTRIC",
                messages.get(lang, "fuelType.LPG"), "fuel_type:LPG"
        ));

        rows.add(singleButtonRow(
                messages.get(lang, "fuelType.CNG"),
                "fuel_type:CNG"
        ));

        rows.add(singleButtonRow(
                anyOptionText(lang),
                "fuel_type:ANY"
        ));

        if (showBack) {
            rows.add(twoButtonsRow(
                    "⬅️ " + messages.get(lang, "button.prev"),
                    "wizard_back:transmission",
                    "⏭ " + messages.get(lang, "button.skip"),
                    "fuel_type:ANY"
            ));
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup yearFromKeyboard(String lang, boolean showBack) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(twoButtonsRow("2000+", "year_from:2000", "2005+", "year_from:2005"));
        rows.add(twoButtonsRow("2010+", "year_from:2010", "2015+", "year_from:2015"));
        rows.add(twoButtonsRow("2018+", "year_from:2018", "2020+", "year_from:2020"));
        rows.add(twoButtonsRow("2022+", "year_from:2022", "2023+", "year_from:2023"));
        rows.add(twoButtonsRow("2024+", "year_from:2024", "2025+", "year_from:2025"));

        rows.add(singleButtonRow(
                notImportantOptionText(lang),
                "year_from:0"
        ));

        if (showBack) {
            rows.add(twoButtonsRow(
                    "⬅️ " + messages.get(lang, "button.prev"),
                    "wizard_back:fuel_type",
                    "⏭ " + messages.get(lang, "button.skip"),
                    "year_from:0"
            ));
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup carCardKeyboard(String lang, Long carId, String url, boolean favoriteAlreadyAdded) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        if (isExternalUrl(url)) {
            rows.add(singleUrlButtonRow(
                    "🚀 " + messages.get(lang, "button.open"),
                    url
            ));
        }

        if (favoriteAlreadyAdded) {
            rows.add(singleButtonRow(
                    "🗑 " + messages.get(lang, "button.removeFavorite"),
                    "fav_remove:" + carId
            ));
        } else {
            rows.add(singleButtonRow(
                    "⭐ " + messages.get(lang, "button.addFavorite"),
                    "fav_add:" + carId
            ));
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup searchBrowseKeyboard(String lang,
                                                     Long carId,
                                                     String url,
                                                     boolean hasPrev,
                                                     boolean hasNext) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        if (hasPrev || hasNext) {
            InlineKeyboardRow navRow = new InlineKeyboardRow();

            if (hasPrev) {
                navRow.add(InlineKeyboardButton.builder()
                        .text("⬅️ " + messages.get(lang, "button.prev"))
                        .callbackData("browse_prev")
                        .build());
            }

            if (hasNext) {
                navRow.add(InlineKeyboardButton.builder()
                        .text("➡️ " + messages.get(lang, "button.next"))
                        .callbackData("browse_next")
                        .build());
            }

            rows.add(navRow);
        }

        InlineKeyboardRow actionRow = new InlineKeyboardRow();

        if (isExternalUrl(url)) {
            actionRow.add(InlineKeyboardButton.builder()
                    .text("🚀 " + messages.get(lang, "button.open"))
                    .url(url)
                    .build());
        }

        actionRow.add(InlineKeyboardButton.builder()
                .text("⭐ " + messages.get(lang, "button.addFavorite"))
                .callbackData("fav_add:" + carId)
                .build());

        rows.add(actionRow);

        rows.add(twoButtonsRow(
                "🔄 " + messages.get(lang, "button.restart"),
                "find_restart",
                "⬅️ " + messages.get(lang, "button.stop"),
                "find_stop"
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup sellConfirmKeyboard(String lang) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(twoButtonsRow(
                        confirmSellText(lang),
                        "sell_confirm",
                        cancelText(lang),
                        "sell_cancel"
                )))
                .build();
    }

    public InlineKeyboardMarkup sellAdminReviewKeyboard(Long carId) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(twoButtonsRow(
                        "✅ Схвалити",
                        "sell_admin_approve:" + carId,
                        "⛔ Відхилити",
                        "sell_admin_reject:" + carId
                )))
                .build();
    }

    public InlineKeyboardMarkup userListingKeyboard(String lang, Long carId, String status) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(twoButtonsRow(
                editListingText(lang),
                "sell_edit_menu:" + carId,
                deleteListingText(lang),
                "sell_delete:" + carId
        ));

        if ("ACTIVE".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)) {
            rows.add(singleButtonRow(removeListingText(lang), "sell_remove:" + carId));
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup userListingEditKeyboard(String lang, Long carId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(twoButtonsRow(
                editFieldText(lang, "title"),
                "sell_edit:" + carId + ":title",
                editFieldText(lang, "price"),
                "sell_edit:" + carId + ":price"
        ));
        rows.add(twoButtonsRow(
                editFieldText(lang, "year"),
                "sell_edit:" + carId + ":year",
                editFieldText(lang, "mileage"),
                "sell_edit:" + carId + ":mileage"
        ));
        rows.add(twoButtonsRow(
                editFieldText(lang, "location"),
                "sell_edit:" + carId + ":location",
                editFieldText(lang, "contact"),
                "sell_edit:" + carId + ":contact"
        ));
        rows.add(twoButtonsRow(
                editFieldText(lang, "fuel"),
                "sell_edit:" + carId + ":fuel",
                editFieldText(lang, "transmission"),
                "sell_edit:" + carId + ":transmission"
        ));
        rows.add(twoButtonsRow(
                editFieldText(lang, "carType"),
                "sell_edit:" + carId + ":carType",
                editFieldText(lang, "photo"),
                "sell_edit:" + carId + ":photo"
        ));
        rows.add(singleButtonRow(editFieldText(lang, "description"), "sell_edit:" + carId + ":description"));
        rows.add(singleButtonRow("⬅️ " + messages.get(lang, "button.prev"), "sell_mycars"));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    private InlineKeyboardRow singleButtonRow(String text, String callbackData) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build());
        return row;
    }

    private InlineKeyboardRow singleUrlButtonRow(String text, String url) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(InlineKeyboardButton.builder()
                .text(text)
                .url(url)
                .build());
        return row;
    }

    private boolean isExternalUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private InlineKeyboardRow twoButtonsRow(String text1, String callback1, String text2, String callback2) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(InlineKeyboardButton.builder().text(text1).callbackData(callback1).build());
        row.add(InlineKeyboardButton.builder().text(text2).callbackData(callback2).build());
        return row;
    }

    private String sellMenuText(String lang) {
        return switch (lang) {
            case "ru" -> "🚗 Продать авто";
            case "uk" -> "🚗 Продати авто";
            case "cs" -> "🚗 Prodat auto";
            default -> "🚗 Sell car";
        };
    }

    private String myCarsMenuText(String lang) {
        return switch (lang) {
            case "ru" -> "📦 Мои авто";
            case "uk" -> "📦 Мої авто";
            case "cs" -> "📦 Moje auta";
            default -> "📦 My cars";
        };
    }

    private String confirmSellText(String lang) {
        return switch (lang) {
            case "ru" -> "✅ Отправить на проверку";
            case "uk" -> "✅ Надіслати на перевірку";
            case "cs" -> "✅ Odeslat ke kontrole";
            default -> "✅ Submit for review";
        };
    }

    private String removeListingText(String lang) {
        return switch (lang) {
            case "ru" -> "🗑 Снять с продажи";
            case "uk" -> "🗑 Зняти з продажу";
            case "cs" -> "🗑 Stáhnout z prodeje";
            default -> "🗑 Remove listing";
        };
    }

    private String editListingText(String lang) {
        return switch (lang) {
            case "ru" -> "✏️ Редактировать";
            case "uk" -> "✏️ Редагувати";
            case "cs" -> "✏️ Upravit";
            default -> "✏️ Edit";
        };
    }

    private String deleteListingText(String lang) {
        return switch (lang) {
            case "ru" -> "🗑 Удалить";
            case "uk" -> "🗑 Видалити";
            case "cs" -> "🗑 Smazat";
            default -> "🗑 Delete";
        };
    }

    private String editFieldText(String lang, String field) {
        return switch (field) {
            case "title" -> switch (lang) {
                case "ru" -> "Название";
                case "uk" -> "Назва";
                case "cs" -> "Název";
                default -> "Title";
            };
            case "price" -> switch (lang) {
                case "ru" -> "Цена";
                case "uk" -> "Ціна";
                case "cs" -> "Cena";
                default -> "Price";
            };
            case "year" -> switch (lang) {
                case "ru" -> "Год";
                case "uk" -> "Рік";
                case "cs" -> "Rok";
                default -> "Year";
            };
            case "mileage" -> switch (lang) {
                case "ru" -> "Пробег";
                case "uk" -> "Пробіг";
                case "cs" -> "Nájezd";
                default -> "Mileage";
            };
            case "location" -> switch (lang) {
                case "ru" -> "Город";
                case "uk" -> "Місто";
                case "cs" -> "Město";
                default -> "Location";
            };
            case "contact" -> switch (lang) {
                case "ru" -> "Контакт";
                case "uk" -> "Контакт";
                case "cs" -> "Kontakt";
                default -> "Contact";
            };
            case "fuel" -> switch (lang) {
                case "ru" -> "Топливо";
                case "uk" -> "Пальне";
                case "cs" -> "Palivo";
                default -> "Fuel";
            };
            case "transmission" -> switch (lang) {
                case "ru" -> "Коробка";
                case "uk" -> "Коробка";
                case "cs" -> "Převodovka";
                default -> "Transmission";
            };
            case "carType" -> switch (lang) {
                case "ru" -> "Кузов";
                case "uk" -> "Кузов";
                case "cs" -> "Karoserie";
                default -> "Body";
            };
            case "photo" -> switch (lang) {
                case "ru", "uk" -> "Фото";
                case "cs" -> "Foto";
                default -> "Photo";
            };
            case "description" -> switch (lang) {
                case "ru" -> "Описание";
                case "uk" -> "Опис";
                case "cs" -> "Popis";
                default -> "Description";
            };
            default -> field;
        };
    }

    private String buildSelectableText(boolean selected, String label) {
        return (selected ? "✅ " : "▫️ ") + label;
    }

    private String anyOptionText(String lang) {
        return "↪️ " + messages.get(lang, "common.any");
    }

    private String noLimitOptionText(String lang) {
        return "∞ " + messages.get(lang, "common.noLimit");
    }

    private String notImportantOptionText(String lang) {
        return "↪️ " + messages.get(lang, "common.notImportant");
    }

    private String selectableText(boolean selected, String label) {
        return (selected ? "✅ " : "▫️ ") + label;
    }

    private String notificationButtonText(String lang) {
        return switch (lang) {
            case "ru" -> "🔔 Уведомления";
            case "uk" -> "🔔 Сповіщення";
            case "cs" -> "🔔 Upozornění";
            default -> "🔔 Notifications";
        };
    }

    private String resetConfirmText(String lang) {
        return switch (lang) {
            case "ru" -> "Да, сбросить";
            case "uk" -> "Так, скинути";
            case "cs" -> "Ano, resetovat";
            default -> "Yes, reset";
        };
    }

    private String cancelText(String lang) {
        return switch (lang) {
            case "ru" -> "Отмена";
            case "uk" -> "Скасувати";
            case "cs" -> "Zrušit";
            default -> "Cancel";
        };
    }

    private String pauseNotificationsText(String lang) {
        return switch (lang) {
            case "ru" -> "⏸ Поставить на паузу";
            case "uk" -> "⏸ Поставити на паузу";
            case "cs" -> "⏸ Pozastavit";
            default -> "⏸ Pause";
        };
    }

    private String resumeNotificationsText(String lang) {
        return switch (lang) {
            case "ru" -> "▶️ Возобновить";
            case "uk" -> "▶️ Відновити";
            case "cs" -> "▶️ Obnovit";
            default -> "▶️ Resume";
        };
    }

    private String instantModeText(String lang) {
        return switch (lang) {
            case "ru" -> "Сразу";
            case "uk" -> "Одразу";
            case "cs" -> "Ihned";
            default -> "Instant";
        };
    }

    private String digestModeText(String lang) {
        return switch (lang) {
            case "ru" -> "Дайджест";
            case "uk" -> "Дайджест";
            case "cs" -> "Souhrn";
            default -> "Digest";
        };
    }

    private String noDailyLimitText(String lang) {
        return switch (lang) {
            case "ru" -> "Без лимита";
            case "uk" -> "Без ліміту";
            case "cs" -> "Bez limitu";
            default -> "No limit";
        };
    }

    private Set<String> parseSelectedValues(String raw) {
        Set<String> result = new LinkedHashSet<>();

        if (raw == null || raw.isBlank()) {
            return result;
        }

        for (String part : raw.split(",")) {
            String value = part.trim();
            if (!value.isBlank()) {
                result.add(value);
            }
        }

        return result;
    }
}

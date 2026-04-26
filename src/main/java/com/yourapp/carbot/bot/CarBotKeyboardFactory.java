package com.yourapp.carbot.bot;

import com.yourapp.carbot.i18n.MessageService;
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
        row2.add(new KeyboardButton("🆕 " + messages.get(lang, "menu.latest")));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("⭐ " + messages.get(lang, "menu.favorites")));
        row3.add(new KeyboardButton("🌐 " + messages.get(lang, "menu.language")));

        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2, row3))
                .resizeKeyboard(true)
                .selective(true)
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
                "♻️ " + messages.get(lang, "button.resetFilter"),
                "myfilter_reset"
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
                "🔘 " + messages.get(lang, "common.any"),
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

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("SKODA"), messages.get(lang, "brand.SKODA")),
                "brand:toggle:SKODA",
                buildSelectableText(selected.contains("VOLKSWAGEN"), messages.get(lang, "brand.VOLKSWAGEN")),
                "brand:toggle:VOLKSWAGEN"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("AUDI"), messages.get(lang, "brand.AUDI")),
                "brand:toggle:AUDI",
                buildSelectableText(selected.contains("BMW"), messages.get(lang, "brand.BMW")),
                "brand:toggle:BMW"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("MERCEDES"), messages.get(lang, "brand.MERCEDES")),
                "brand:toggle:MERCEDES",
                buildSelectableText(selected.contains("TOYOTA"), messages.get(lang, "brand.TOYOTA")),
                "brand:toggle:TOYOTA"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("FORD"), messages.get(lang, "brand.FORD")),
                "brand:toggle:FORD",
                buildSelectableText(selected.contains("RENAULT"), messages.get(lang, "brand.RENAULT")),
                "brand:toggle:RENAULT"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("HYUNDAI"), messages.get(lang, "brand.HYUNDAI")),
                "brand:toggle:HYUNDAI",
                buildSelectableText(selected.contains("KIA"), messages.get(lang, "brand.KIA")),
                "brand:toggle:KIA"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("PEUGEOT"), messages.get(lang, "brand.PEUGEOT")),
                "brand:toggle:PEUGEOT",
                buildSelectableText(selected.contains("CITROEN"), messages.get(lang, "brand.CITROEN")),
                "brand:toggle:CITROEN"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("OPEL"), messages.get(lang, "brand.OPEL")),
                "brand:toggle:OPEL",
                buildSelectableText(selected.contains("MAZDA"), messages.get(lang, "brand.MAZDA")),
                "brand:toggle:MAZDA"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("HONDA"), messages.get(lang, "brand.HONDA")),
                "brand:toggle:HONDA",
                buildSelectableText(selected.contains("VOLVO"), messages.get(lang, "brand.VOLVO")),
                "brand:toggle:VOLVO"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("SEAT"), messages.get(lang, "brand.SEAT")),
                "brand:toggle:SEAT",
                buildSelectableText(selected.contains("DACIA"), messages.get(lang, "brand.DACIA")),
                "brand:toggle:DACIA"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("FIAT"), messages.get(lang, "brand.FIAT")),
                "brand:toggle:FIAT",
                buildSelectableText(selected.contains("TESLA"), messages.get(lang, "brand.TESLA")),
                "brand:toggle:TESLA"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("CUPRA"), messages.get(lang, "brand.CUPRA")),
                "brand:toggle:CUPRA",
                buildSelectableText(selected.contains("LEXUS"), messages.get(lang, "brand.LEXUS")),
                "brand:toggle:LEXUS"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("NISSAN"), messages.get(lang, "brand.NISSAN")),
                "brand:toggle:NISSAN",
                buildSelectableText(selected.contains("SUZUKI"), messages.get(lang, "brand.SUZUKI")),
                "brand:toggle:SUZUKI"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("JEEP"), messages.get(lang, "brand.JEEP")),
                "brand:toggle:JEEP",
                buildSelectableText(selected.contains("SUBARU"), messages.get(lang, "brand.SUBARU")),
                "brand:toggle:SUBARU"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("MITSUBISHI"), messages.get(lang, "brand.MITSUBISHI")),
                "brand:toggle:MITSUBISHI",
                buildSelectableText(selected.contains("PORSCHE"), messages.get(lang, "brand.PORSCHE")),
                "brand:toggle:PORSCHE"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("LAND_ROVER"), messages.get(lang, "brand.LAND_ROVER")),
                "brand:toggle:LAND_ROVER",
                buildSelectableText(selected.contains("MINI"), messages.get(lang, "brand.MINI")),
                "brand:toggle:MINI"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("ALFA_ROMEO"), messages.get(lang, "brand.ALFA_ROMEO")),
                "brand:toggle:ALFA_ROMEO",
                buildSelectableText(selected.contains("CHEVROLET"), messages.get(lang, "brand.CHEVROLET")),
                "brand:toggle:CHEVROLET"
        ));

        rows.add(twoButtonsRow(
                buildSelectableText(selected.contains("BYD"), messages.get(lang, "brand.BYD")),
                "brand:toggle:BYD",
                "🔘 " + messages.get(lang, "common.any"),
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

    public InlineKeyboardMarkup maxPriceKeyboard(String lang, boolean showBack) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(twoButtonsRow("30 000 Kč", "max_price:30000", "50 000 Kč", "max_price:50000"));
        rows.add(twoButtonsRow("80 000 Kč", "max_price:80000", "100 000 Kč", "max_price:100000"));
        rows.add(twoButtonsRow("150 000 Kč", "max_price:150000", "200 000 Kč", "max_price:200000"));
        rows.add(twoButtonsRow("300 000 Kč", "max_price:300000", "500 000 Kč", "max_price:500000"));

        rows.add(singleButtonRow(
                "🔘 " + messages.get(lang, "common.noLimit"),
                "max_price:0"
        ));

        if (showBack) {
            rows.add(twoButtonsRow(
                    "⬅️ " + messages.get(lang, "button.prev"),
                    "wizard_back:brand",
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
                "🔘 " + messages.get(lang, "common.any"),
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
                "🔘 " + messages.get(lang, "common.noLimit"),
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
                "🔘 " + messages.get(lang, "common.any"),
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
                "🔘 " + messages.get(lang, "common.any"),
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

        rows.add(singleButtonRow(
                "🔘 " + messages.get(lang, "common.notImportant"),
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

        if (url != null && !url.isBlank()) {
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

        if (url != null && !url.isBlank()) {
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

    private InlineKeyboardRow twoButtonsRow(String text1, String callback1, String text2, String callback2) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(InlineKeyboardButton.builder().text(text1).callbackData(callback1).build());
        row.add(InlineKeyboardButton.builder().text(text2).callbackData(callback2).build());
        return row;
    }

    private String buildSelectableText(boolean selected, String label) {
        return (selected ? "✅ " : "▫️ ") + label;
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
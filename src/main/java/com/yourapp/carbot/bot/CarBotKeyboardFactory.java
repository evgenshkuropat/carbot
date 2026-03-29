package com.yourapp.carbot.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
public class CarBotKeyboardFactory {

    public ReplyKeyboardMarkup mainMenuKeyboard(String lang) {
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();

        row1.add(switch (lang) {
            case "ru" -> "🔍 Поиск";
            case "uk" -> "🔍 Пошук";
            case "cs" -> "🔍 Hledat";
            default -> "🔍 Search";
        });
        row1.add(switch (lang) {
            case "ru" -> "⚙️ Фильтр";
            case "uk" -> "⚙️ Фільтр";
            case "cs" -> "⚙️ Filtr";
            default -> "⚙️ Filter";
        });

        row2.add(switch (lang) {
            case "ru" -> "📝 Мой фильтр";
            case "uk" -> "📝 Мій фільтр";
            case "cs" -> "📝 Můj filtr";
            default -> "📝 My filter";
        });
        row2.add(switch (lang) {
            case "ru" -> "⭐ Избранное";
            case "uk" -> "⭐ Обране";
            case "cs" -> "⭐ Oblíbené";
            default -> "⭐ Favorites";
        });

        row3.add(switch (lang) {
            case "ru" -> "🆕 Последние";
            case "uk" -> "🆕 Останні";
            case "cs" -> "🆕 Nejnovější";
            default -> "🆕 Latest";
        });
        row3.add(switch (lang) {
            case "ru" -> "🌐 Язык";
            case "uk" -> "🌐 Мова";
            case "cs" -> "🌐 Jazyk";
            default -> "🌐 Language";
        });

        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2, row3))
                .resizeKeyboard(true)
                .isPersistent(true)
                .selective(false)
                .build();
    }

    public InlineKeyboardMarkup languageKeyboard() {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                button("🇺🇦 Українська", "lang:uk"),
                button("🇨🇿 Česky", "lang:cs")
        ));

        rows.add(new InlineKeyboardRow(
                button("🇬🇧 English", "lang:en"),
                button("🇷🇺 Русский", "lang:ru")
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup afterLanguageChangedKeyboard(String lang) {
        String filterText = switch (lang) {
            case "ru" -> "⚙️ Настроить фильтр";
            case "uk" -> "⚙️ Налаштувати фільтр";
            case "cs" -> "⚙️ Nastavit filtr";
            default -> "⚙️ Set filter";
        };

        String myFilterText = switch (lang) {
            case "ru" -> "📝 Мой фильтр";
            case "uk" -> "📝 Мій фільтр";
            case "cs" -> "📝 Můj filtr";
            default -> "📝 My filter";
        };

        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                button(filterText, "myfilter_edit"),
                button(myFilterText, "show_myfilter")
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup carTypeKeyboard(String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                button(text(lang, "Седан", "Седан", "Sedan", "Sedan"), "car_type:SEDAN"),
                button(text(lang, "Хэтчбек", "Хетчбек", "Hatchback", "Hatchback"), "car_type:HATCHBACK")
        ));

        rows.add(new InlineKeyboardRow(
                button(text(lang, "Универсал", "Універсал", "Kombi", "Wagon"), "car_type:WAGON"),
                button(text(lang, "SUV", "SUV", "SUV", "SUV"), "car_type:SUV")
        ));

        rows.add(new InlineKeyboardRow(
                button(text(lang, "Минивэн", "Мінівен", "Minivan", "Minivan"), "car_type:MINIVAN")
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup brandKeyboard(String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                button("Škoda", "brand:SKODA"),
                button("Volkswagen", "brand:VOLKSWAGEN")
        ));

        rows.add(new InlineKeyboardRow(
                button("Audi", "brand:AUDI"),
                button("BMW", "brand:BMW")
        ));

        rows.add(new InlineKeyboardRow(
                button("Mercedes", "brand:MERCEDES"),
                button("Toyota", "brand:TOYOTA")
        ));

        rows.add(new InlineKeyboardRow(
                button("Ford", "brand:FORD"),
                button("Renault", "brand:RENAULT")
        ));

        rows.add(new InlineKeyboardRow(
                button("Hyundai", "brand:HYUNDAI"),
                button("Kia", "brand:KIA")
        ));

        rows.add(new InlineKeyboardRow(
                button("Peugeot", "brand:PEUGEOT"),
                button("Citroën", "brand:CITROEN")
        ));

        rows.add(new InlineKeyboardRow(
                button("Opel", "brand:OPEL"),
                button("Mazda", "brand:MAZDA")
        ));

        rows.add(new InlineKeyboardRow(
                button("Honda", "brand:HONDA"),
                button("Volvo", "brand:VOLVO")
        ));

        rows.add(new InlineKeyboardRow(
                button("Seat", "brand:SEAT"),
                button("Dacia", "brand:DACIA")
        ));

        rows.add(new InlineKeyboardRow(
                button("Fiat", "brand:FIAT"),
                button("Tesla", "brand:TESLA")
        ));

        rows.add(new InlineKeyboardRow(
                button(text(lang, "Любая", "Будь-яка", "Libovolná", "Any"), "brand:ANY")
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup maxPriceKeyboard(String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                button("100 000", "max_price:100000"),
                button("200 000", "max_price:200000")
        ));

        rows.add(new InlineKeyboardRow(
                button("300 000", "max_price:300000"),
                button("500 000", "max_price:500000")
        ));

        rows.add(new InlineKeyboardRow(
                button("1 000 000", "max_price:1000000"),
                button("2 000 000", "max_price:2000000")
        ));

        rows.add(new InlineKeyboardRow(
                button(text(lang, "Без ограничения", "Без обмежень", "Bez omezení", "No limit"), "max_price:0")
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup locationKeyboard(String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                button("Praha", "location:PRAHA"),
                button("Středočeský", "location:STREDOCESKY")
        ));

        rows.add(new InlineKeyboardRow(
                button("Jihomoravský", "location:JIHOMORAVSKY"),
                button("Moravskoslezský", "location:MORAVSKOSLEZSKY")
        ));

        rows.add(new InlineKeyboardRow(
                button("Ústecký", "location:USTECKY"),
                button("Plzeňský", "location:PLZENSKY")
        ));

        rows.add(new InlineKeyboardRow(
                button("Jihočeský", "location:JIHOCESKY"),
                button("Liberecký", "location:LIBERECKY")
        ));

        rows.add(new InlineKeyboardRow(
                button("Olomoucký", "location:OLOMOUCKY"),
                button("Pardubický", "location:PARDUBICKY")
        ));

        rows.add(new InlineKeyboardRow(
                button("Zlínský", "location:ZLINSKY"),
                button("Vysočina", "location:VYSOCINA")
        ));

        rows.add(new InlineKeyboardRow(
                button("Karlovarský", "location:KARLOVARSKY"),
                button("Královéhradecký", "location:KRALOVEHRADECKY")
        ));

        rows.add(new InlineKeyboardRow(
                button(text(lang, "Любой регион", "Будь-який регіон", "Libovolný kraj", "Any region"), "location:ANY")
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup mileageKeyboard(String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                button("50 000 km", "mileage:50000"),
                button("100 000 km", "mileage:100000")
        ));

        rows.add(new InlineKeyboardRow(
                button("150 000 km", "mileage:150000"),
                button("200 000 km", "mileage:200000")
        ));

        rows.add(new InlineKeyboardRow(
                button("250 000 km", "mileage:250000"),
                button("300 000 km", "mileage:300000")
        ));

        rows.add(new InlineKeyboardRow(
                button(text(lang, "Без ограничения", "Без обмежень", "Bez omezení", "No limit"), "mileage:0")
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup transmissionKeyboard(String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                button(text(lang, "Механика", "Механіка", "Manuální", "Manual"), "transmission:MANUAL"),
                button(text(lang, "Автомат", "Автомат", "Automat", "Automatic"), "transmission:AUTOMATIC")
        ));

        rows.add(new InlineKeyboardRow(
                button(text(lang, "Любая", "Будь-яка", "Libovolná", "Any"), "transmission:ANY")
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup yearFromKeyboard(String lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                button("2000", "year_from:2000"),
                button("2005", "year_from:2005")
        ));

        rows.add(new InlineKeyboardRow(
                button("2010", "year_from:2010"),
                button("2015", "year_from:2015")
        ));

        rows.add(new InlineKeyboardRow(
                button("2020", "year_from:2020"),
                button(text(lang, "Не важно", "Не важливо", "Nezáleží", "Not important"), "year_from:0")
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup myFilterActionsKeyboard(String lang) {
        String findText = switch (lang) {
            case "ru" -> "🔍 Найти";
            case "uk" -> "🔍 Знайти";
            case "cs" -> "🔍 Hledat";
            default -> "🔍 Find";
        };

        String editText = switch (lang) {
            case "ru" -> "✏️ Изменить";
            case "uk" -> "✏️ Змінити";
            case "cs" -> "✏️ Upravit";
            default -> "✏️ Edit";
        };

        String resetText = switch (lang) {
            case "ru" -> "♻️ Сбросить";
            case "uk" -> "♻️ Скинути";
            case "cs" -> "♻️ Resetovat";
            default -> "♻️ Reset";
        };

        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                button(findText, "myfilter_find"),
                button(editText, "myfilter_edit")
        ));

        rows.add(new InlineKeyboardRow(
                button(resetText, "myfilter_reset")
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup myFilterResetKeyboard(String lang) {
        String text = switch (lang) {
            case "ru" -> "⚙️ Настроить фильтр";
            case "uk" -> "⚙️ Налаштувати фільтр";
            case "cs" -> "⚙️ Nastavit filtr";
            default -> "⚙️ Set filter";
        };

        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(button(text, "myfilter_edit")))
                .build();
    }

    public InlineKeyboardMarkup findNavigationKeyboard(String lang) {
        String moreText = switch (lang) {
            case "ru" -> "Показать ещё";
            case "uk" -> "Показати ще";
            case "cs" -> "Ukázat další";
            default -> "Show more";
        };

        String restartText = switch (lang) {
            case "ru" -> "Новый поиск";
            case "uk" -> "Новий пошук";
            case "cs" -> "Nové hledání";
            default -> "New search";
        };

        String stopText = switch (lang) {
            case "ru" -> "Завершить поиск";
            case "uk" -> "Завершити пошук";
            case "cs" -> "Ukončit hledání";
            default -> "Finish search";
        };

        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                button(moreText, "find_more"),
                button(restartText, "find_restart")
        ));

        rows.add(new InlineKeyboardRow(
                button(stopText, "find_stop")
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup carCardKeyboard(String lang, Long carId, String url,boolean isFavorite) {
        String openText = switch (lang) {
            case "ru" -> "Открыть объявление";
            case "uk" -> "Відкрити оголошення";
            case "cs" -> "Otevřít inzerát";
            default -> "Open listing";
        };

        String favoriteText = switch (lang) {
            case "ru" -> "⭐ В избранное";
            case "uk" -> "⭐ В обране";
            case "cs" -> "⭐ Do oblíbených";
            default -> "⭐ Add to favorites";
        };

        List<InlineKeyboardRow> rows = new ArrayList<>();

        if (url != null && !url.isBlank()) {
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text(openText)
                            .url(url)
                            .build()
            ));
        }

        if (carId != null) {
            rows.add(new InlineKeyboardRow(
                    button(favoriteText, "fav_add:" + carId)
            ));
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup favoritesKeyboard(String lang) {
        String restartText = switch (lang) {
            case "ru" -> "Новый поиск";
            case "uk" -> "Новий пошук";
            case "cs" -> "Nové hledání";
            default -> "New search";
        };

        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        button(restartText, "find_restart")
                ))
                .build();
    }

    private InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    private String text(String lang, String ru, String uk, String cs, String en) {
        return switch (lang) {
            case "ru" -> ru;
            case "uk" -> uk;
            case "cs" -> cs;
            default -> en;
        };
    }
}
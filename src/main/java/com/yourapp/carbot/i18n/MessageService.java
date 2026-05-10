package com.yourapp.carbot.i18n;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MessageService {

    private final Map<String, Map<String, String>> messages = Map.of(
            "ru", Map.ofEntries(
                    Map.entry("start.welcome", """
                            \uD83D\uDE97 \u041F\u043E\u0438\u0441\u043A \u0430\u0432\u0442\u043E \u043F\u043E \u0432\u0441\u0435\u0439 \u0427\u0435\u0445\u0438\u0438
                            
                            \u042F \u0441\u043E\u0431\u0438\u0440\u0430\u044E \u043E\u0431\u044A\u044F\u0432\u043B\u0435\u043D\u0438\u044F \u0441:
                            \u2022 Bazo\u0161.cz
                            \u2022 Sauto.cz
                            \u2022 TipCars.cz
                            
                            \u041F\u043E\u043C\u043E\u0433\u0443 \u0432\u0430\u043C:
                            
                            \uD83D\uDD0D \u043D\u0430\u0439\u0442\u0438 \u0430\u0432\u0442\u043E \u043F\u043E \u0444\u0438\u043B\u044C\u0442\u0440\u0443
                            \u2B50 \u0441\u043E\u0445\u0440\u0430\u043D\u0438\u0442\u044C \u0438\u0437\u0431\u0440\u0430\u043D\u043D\u044B\u0435 \u043E\u0431\u044A\u044F\u0432\u043B\u0435\u043D\u0438\u044F
                            \uD83C\uDD95 \u043F\u043E\u043B\u0443\u0447\u0430\u0442\u044C \u043D\u043E\u0432\u044B\u0435 \u043E\u0431\u044A\u044F\u0432\u043B\u0435\u043D\u0438\u044F \u0430\u0432\u0442\u043E\u043C\u0430\u0442\u0438\u0447\u0435\u0441\u043A\u0438
                            
                            \u0412\u044B\u0431\u0435\u0440\u0438\u0442\u0435 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435 \u043D\u0438\u0436\u0435 \uD83D\uDC47
                            """),
                    Map.entry("start.welcomeBack", """
                            \u0421 \u0432\u043E\u0437\u0432\u0440\u0430\u0449\u0435\u043D\u0438\u0435\u043C! \uD83D\uDC4B
                            
                            \u0412\u0430\u0448 \u0444\u0438\u043B\u044C\u0442\u0440 \u0443\u0436\u0435 \u043D\u0430\u0441\u0442\u0440\u043E\u0435\u043D.
                            """),

                    Map.entry("carType.choose", """
                            \u0428\u0430\u0433 1/8 \u00B7 \u0422\u0438\u043F \u043A\u0443\u0437\u043E\u0432\u0430
                            \u041C\u043E\u0436\u043D\u043E \u0432\u044B\u0431\u0440\u0430\u0442\u044C \u043D\u0435\u0441\u043A\u043E\u043B\u044C\u043A\u043E \u0432\u0430\u0440\u0438\u0430\u043D\u0442\u043E\u0432.
                            """),
                    Map.entry("carType.selected", "\u0412\u044B\u0431\u0440\u0430\u043D\u043D\u044B\u0435 \u0442\u0438\u043F\u044B \u043A\u0443\u0437\u043E\u0432\u0430:"),
                    Map.entry("carType.chooseAtLeastOne", "\u0412\u044B\u0431\u0435\u0440\u0438\u0442\u0435 \u0445\u043E\u0442\u044F \u0431\u044B \u043E\u0434\u0438\u043D \u0442\u0438\u043F \u043A\u0443\u0437\u043E\u0432\u0430 \u0438\u043B\u0438 \u043D\u0430\u0436\u043C\u0438\u0442\u0435 \u00AB\u041B\u044E\u0431\u043E\u0439\u00BB."),

                    Map.entry("filter.carType.saved", "\u2705 \u0422\u0438\u043F \u043A\u0443\u0437\u043E\u0432\u0430 \u0441\u043E\u0445\u0440\u0430\u043D\u0435\u043D"),
                    Map.entry("filter.brand.saved", "\u2705 \u041C\u0430\u0440\u043A\u0430 \u0441\u043E\u0445\u0440\u0430\u043D\u0435\u043D\u0430"),
                    Map.entry("filter.price.saved", "\u2705 \u041C\u0430\u043A\u0441\u0438\u043C\u0430\u043B\u044C\u043D\u0430\u044F \u0446\u0435\u043D\u0430 \u0441\u043E\u0445\u0440\u0430\u043D\u0435\u043D\u0430"),
                    Map.entry("filter.location.saved", "\u2705 \u0420\u0435\u0433\u0438\u043E\u043D \u043F\u043E\u0438\u0441\u043A\u0430 \u0441\u043E\u0445\u0440\u0430\u043D\u0435\u043D"),
                    Map.entry("filter.mileage.saved", "\u2705 \u041C\u0430\u043A\u0441\u0438\u043C\u0430\u043B\u044C\u043D\u044B\u0439 \u043F\u0440\u043E\u0431\u0435\u0433 \u0441\u043E\u0445\u0440\u0430\u043D\u0435\u043D"),
                    Map.entry("filter.transmission.saved", "\u2705 \u0422\u0438\u043F \u043A\u043E\u0440\u043E\u0431\u043A\u0438 \u043F\u0435\u0440\u0435\u0434\u0430\u0447 \u0441\u043E\u0445\u0440\u0430\u043D\u0435\u043D"),
                    Map.entry("filter.fuelType.saved", "\u2705 \u0422\u0438\u043F \u0442\u043E\u043F\u043B\u0438\u0432\u0430 \u0441\u043E\u0445\u0440\u0430\u043D\u0435\u043D"),
                    Map.entry("filter.saved", "\u2705 \u0424\u0438\u043B\u044C\u0442\u0440 \u0441\u043E\u0445\u0440\u0430\u043D\u0435\u043D"),
                    Map.entry("filter.saved.next", """
                            \u2705 \u0424\u0438\u043B\u044C\u0442\u0440 \u0441\u043E\u0445\u0440\u0430\u043D\u0435\u043D.
                            
                            \u0427\u0442\u043E \u0445\u043E\u0442\u0438\u0442\u0435 \u0441\u0434\u0435\u043B\u0430\u0442\u044C \u0434\u0430\u043B\u044C\u0448\u0435?
                            """),

                    Map.entry("help.text", """
                            \u2139\uFE0F \u0427\u0442\u043E \u0443\u043C\u0435\u0435\u0442 \u0431\u043E\u0442:
                            
                            /filter \u2014 \u043D\u0430\u0441\u0442\u0440\u043E\u0438\u0442\u044C \u0444\u0438\u043B\u044C\u0442\u0440 \u043F\u043E\u0438\u0441\u043A\u0430
                            /myfilter \u2014 \u043F\u043E\u043A\u0430\u0437\u0430\u0442\u044C \u0442\u0435\u043A\u0443\u0449\u0438\u0439 \u0444\u0438\u043B\u044C\u0442\u0440
                            /find \u2014 \u043D\u0430\u0439\u0442\u0438 \u043F\u043E\u0434\u0445\u043E\u0434\u044F\u0449\u0438\u0435 \u0430\u0432\u0442\u043E
                            /services \u2014 \u043E\u0442\u043A\u0440\u044B\u0442\u044C \u0441\u0435\u0440\u0432\u0438\u0441\u044B \u0438 \u043F\u043E\u0434\u0434\u0435\u0440\u0436\u043A\u0443
                            /favorites \u2014 \u043E\u0442\u043A\u0440\u044B\u0442\u044C \u0438\u0437\u0431\u0440\u0430\u043D\u043D\u043E\u0435
                            /language \u2014 \u0438\u0437\u043C\u0435\u043D\u0438\u0442\u044C \u044F\u0437\u044B\u043A
                            /resetfilter \u2014 \u0441\u0431\u0440\u043E\u0441\u0438\u0442\u044C \u0444\u0438\u043B\u044C\u0442\u0440
                            /start \u2014 \u043D\u0430\u0447\u0430\u0442\u044C \u0437\u0430\u043D\u043E\u0432\u043E
                            
                            \u041D\u043E\u0432\u044B\u0435 \u043F\u043E\u0434\u0445\u043E\u0434\u044F\u0449\u0438\u0435 \u043E\u0431\u044A\u044F\u0432\u043B\u0435\u043D\u0438\u044F \u0431\u0443\u0434\u0443\u0442 \u043F\u0440\u0438\u0445\u043E\u0434\u0438\u0442\u044C \u0430\u0432\u0442\u043E\u043C\u0430\u0442\u0438\u0447\u0435\u0441\u043A\u0438.
                            """),

                    Map.entry("command.unknown", """
                            \u042F \u043D\u0435 \u043F\u043E\u043D\u044F\u043B \u044D\u0442\u0443 \u043A\u043E\u043C\u0430\u043D\u0434\u0443.
                            
                            \u0414\u043E\u0441\u0442\u0443\u043F\u043D\u044B\u0435 \u043A\u043E\u043C\u0430\u043D\u0434\u044B:
                            /start
                            /filter
                            /myfilter
                            /resetfilter
                            /services
                            /find
                            /favorites
                            /language
                            /help
                            """),

                    Map.entry("menu.ready", """
                            \uD83D\uDE97 \u0411\u043E\u0442 \u0433\u043E\u0442\u043E\u0432 \u043A \u0440\u0430\u0431\u043E\u0442\u0435.
                            
                            \u042F \u0431\u0443\u0434\u0443 \u043F\u0440\u0438\u0441\u044B\u043B\u0430\u0442\u044C \u043D\u043E\u0432\u044B\u0435 \u043E\u0431\u044A\u044F\u0432\u043B\u0435\u043D\u0438\u044F \u043F\u043E \u0432\u0430\u0448\u0435\u043C\u0443 \u0444\u0438\u043B\u044C\u0442\u0440\u0443.
                            \u041C\u043E\u0436\u043D\u043E \u043D\u0430\u0447\u0430\u0442\u044C \u043F\u043E\u0438\u0441\u043A \u043F\u0440\u044F\u043C\u043E \u0441\u0435\u0439\u0447\u0430\u0441 \uD83D\uDC47
                            """),
                    Map.entry("menu.search", "\u041D\u0430\u0439\u0442\u0438 \u0430\u0432\u0442\u043E"),
                    Map.entry("menu.filter", "\u041D\u0430\u0441\u0442\u0440\u043E\u0438\u0442\u044C \u0444\u0438\u043B\u044C\u0442\u0440"),
                    Map.entry("menu.myFilter", "\u041C\u043E\u0439 \u0444\u0438\u043B\u044C\u0442\u0440"),
                    Map.entry("menu.latest", "Новые объявления"),
                    Map.entry("menu.services", "\u0421\u0435\u0440\u0432\u0438\u0441\u044B"),
                    Map.entry("services.text", "\uD83E\uDDF0 \u0421\u0435\u0440\u0432\u0438\u0441\u044B"),
                    Map.entry("services.housingBot", "\u041F\u043E\u0438\u0441\u043A \u0436\u0438\u043B\u044C\u044F \u0432 \u0427\u0435\u0445\u0438\u0438"),
                    Map.entry("services.feedback", "\u041E\u0431\u0440\u0430\u0442\u043D\u0430\u044F \u0441\u0432\u044F\u0437\u044C"),
                    Map.entry("services.supportProject", "\u041F\u043E\u0434\u0434\u0435\u0440\u0436\u0430\u0442\u044C \u043F\u0440\u043E\u0435\u043A\u0442"),
                    Map.entry("menu.favorites", "Избранное"),
                    Map.entry("menu.language", "Язык"),

                    Map.entry("button.showFilter", "\u041F\u043E\u043A\u0430\u0437\u0430\u0442\u044C \u0444\u0438\u043B\u044C\u0442\u0440"),
                    Map.entry("button.findCars", "\u041D\u0430\u0439\u0442\u0438 \u0430\u0432\u0442\u043E"),
                    Map.entry("button.editFilter", "\u0418\u0437\u043C\u0435\u043D\u0438\u0442\u044C \u0444\u0438\u043B\u044C\u0442\u0440"),
                    Map.entry("button.resetFilter", "\u0421\u0431\u0440\u043E\u0441\u0438\u0442\u044C \u0444\u0438\u043B\u044C\u0442\u0440"),
                    Map.entry("button.createNewFilter", "\u0421\u043E\u0437\u0434\u0430\u0442\u044C \u043D\u043E\u0432\u044B\u0439 \u0444\u0438\u043B\u044C\u0442\u0440"),
                    Map.entry("button.open", "\u041E\u0442\u043A\u0440\u044B\u0442\u044C \u043E\u0431\u044A\u044F\u0432\u043B\u0435\u043D\u0438\u0435"),
                    Map.entry("button.addFavorite", "\u0414\u043E\u0431\u0430\u0432\u0438\u0442\u044C \u0432 \u0438\u0437\u0431\u0440\u0430\u043D\u043D\u043E\u0435"),
                    Map.entry("button.removeFavorite", "\u0423\u0434\u0430\u043B\u0438\u0442\u044C \u0438\u0437 \u0438\u0437\u0431\u0440\u0430\u043D\u043D\u043E\u0433\u043E"),
                    Map.entry("button.prev", "\u041D\u0430\u0437\u0430\u0434"),
                    Map.entry("button.next", "\u0414\u0430\u043B\u0435\u0435"),
                    Map.entry("button.restart", "\u041D\u043E\u0432\u044B\u0439 \u043F\u043E\u0438\u0441\u043A"),
                    Map.entry("button.stop", "\u0412 \u043C\u0435\u043D\u044E"),
                    Map.entry("button.skip", "\u041F\u0440\u043E\u043F\u0443\u0441\u0442\u0438\u0442\u044C"),
                    Map.entry("button.backToMenu", "\u0412 \u043C\u0435\u043D\u044E"),
                    Map.entry("button.newSearch", "\u041D\u043E\u0432\u044B\u0439 \u043F\u043E\u0438\u0441\u043A"),

                    Map.entry("filter.notConfigured", """
                            \u0424\u0438\u043B\u044C\u0442\u0440 \u043F\u043E\u043A\u0430 \u043D\u0435 \u043D\u0430\u0441\u0442\u0440\u043E\u0435\u043D.
                            
                            \u041D\u0430\u0436\u043C\u0438\u0442\u0435 /filter, \u0438 \u044F \u043F\u043E\u043C\u043E\u0433\u0443 \u043D\u0430\u0441\u0442\u0440\u043E\u0438\u0442\u044C \u043F\u043E\u0438\u0441\u043A \u0448\u0430\u0433 \u0437\u0430 \u0448\u0430\u0433\u043E\u043C.
                            """),
                    Map.entry("filter.reset", """
                            \u267B\uFE0F \u0424\u0438\u043B\u044C\u0442\u0440 \u0441\u0431\u0440\u043E\u0448\u0435\u043D.
                            
                            \u0414\u0430\u0432\u0430\u0439\u0442\u0435 \u043D\u0430\u0441\u0442\u0440\u043E\u0438\u043C \u043D\u043E\u0432\u044B\u0439.
                            """),

                    Map.entry("cars.empty", """
                            В базе пока нет объявлений.

                            Попробуйте зайти чуть позже.
                            """),
                    Map.entry("cars.noMatches", "По вашему фильтру пока ничего не найдено."),
                    Map.entry("cars.matchesFound", """
                            🚗 Найдены подходящие объявления:
                            """),
                    Map.entry("cars.latest", """
                            🆕 Последние объявления из базы:
                            """),
                    Map.entry("cars.noMatches.pretty", """
                            😕 По вашему фильтру пока ничего не найдено.

                            Попробуйте:
                            • увеличить максимальную цену
                            • выбрать любой регион
                            • убрать ограничение по пробегу
                            • выбрать больше марок
                            """),
                    Map.entry("cars.noMore", "Это все объявления на сейчас 👌"),
                    Map.entry("cars.morePrompt", "Показать ещё подходящие объявления?"),
                    Map.entry("cars.searchFinished", """
                            ✅ Поиск завершён.

                            Вы можете изменить фильтр или вернуться позже за новыми объявлениями.
                            """),

                    Map.entry("favorites.added", """
                            ⭐ Объявление добавлено в избранное.

                            Откройте /favorites, чтобы посмотреть сохранённые объявления.
                            """),
                    Map.entry("favorites.alreadyExists", """
                            ⭐ Это объявление уже есть в избранном.
                            """),
                    Map.entry("favorites.error", """
                            Не удалось добавить объявление в избранное.
                            Попробуйте ещё раз.
                            """),
                    Map.entry("favorites.empty", """
                            ⭐ В избранном пока ничего нет.
                    
                            Когда найдёте интересное объявление, нажмите «Добавить в избранное» — оно появится здесь.
                            """),
                    Map.entry("favorites.title", "⭐ Ваше избранное:"),
                    Map.entry("favorites.removed", """
                            🗑 Объявление удалено из избранного.
                            """),
                    Map.entry("favorites.notFound", """
                            Объявление не найдено в избранном.
                            """),
                    Map.entry("favorites.removeError", """
                            Не удалось удалить объявление из избранного.
                            Попробуйте ещё раз.
                            """),

                    Map.entry("language.choose", "Выберите язык:"),
                    Map.entry("language.changed", "✅ Язык изменён."),
                    Map.entry("language.nextStep", """
                            Что хотите сделать дальше?

                            • посмотреть текущий фильтр
                            • изменить фильтр
                            • начать поиск
                            """),

                    Map.entry("brand.choose", """
                            \u0428\u0430\u0433 2/8 \u00B7 \u041C\u0430\u0440\u043A\u0430
                            \u041C\u043E\u0436\u043D\u043E \u0432\u044B\u0431\u0440\u0430\u0442\u044C \u043D\u0435\u0441\u043A\u043E\u043B\u044C\u043A\u043E \u0432\u0430\u0440\u0438\u0430\u043D\u0442\u043E\u0432.
                            """),
                    Map.entry("price.choose", """
                            \u0428\u0430\u0433 3/8 \u00B7 \u0411\u044E\u0434\u0436\u0435\u0442
                            \u0412\u044B\u0431\u0435\u0440\u0438\u0442\u0435 \u043C\u0430\u043A\u0441\u0438\u043C\u0430\u043B\u044C\u043D\u0443\u044E \u0446\u0435\u043D\u0443.
                            """),
                    Map.entry("location.choose", """
                            \u0428\u0430\u0433 4/8 \u00B7 \u0420\u0435\u0433\u0438\u043E\u043D
                            \u0412\u044B\u0431\u0435\u0440\u0438\u0442\u0435 \u0440\u0435\u0433\u0438\u043E\u043D \u043F\u043E\u0438\u0441\u043A\u0430.
                            """),
                    Map.entry("mileage.choose", """
                            \u0428\u0430\u0433 5/8 \u00B7 \u041F\u0440\u043E\u0431\u0435\u0433
                            \u0412\u044B\u0431\u0435\u0440\u0438\u0442\u0435 \u043C\u0430\u043A\u0441\u0438\u043C\u0430\u043B\u044C\u043D\u044B\u0439 \u043F\u0440\u043E\u0431\u0435\u0433.
                            """),
                    Map.entry("transmission.choose", """
                            \u0428\u0430\u0433 6/8 \u00B7 \u041A\u043E\u0440\u043E\u0431\u043A\u0430
                            \u0412\u044B\u0431\u0435\u0440\u0438\u0442\u0435 \u0442\u0438\u043F \u043A\u043E\u0440\u043E\u0431\u043A\u0438 \u043F\u0435\u0440\u0435\u0434\u0430\u0447.
                            """),
                    Map.entry("fuelType.choose", """
                            \u0428\u0430\u0433 7/8 \u00B7 \u0422\u043E\u043F\u043B\u0438\u0432\u043E
                            \u0412\u044B\u0431\u0435\u0440\u0438\u0442\u0435 \u0442\u0438\u043F \u0442\u043E\u043F\u043B\u0438\u0432\u0430.
                            """),
                    Map.entry("yearFrom.choose", """
                            \u0428\u0430\u0433 8/8 \u00B7 \u0413\u043E\u0434 \u0432\u044B\u043F\u0443\u0441\u043A\u0430
                            \u0412\u044B\u0431\u0435\u0440\u0438\u0442\u0435 \u043C\u0438\u043D\u0438\u043C\u0430\u043B\u044C\u043D\u044B\u0439 \u0433\u043E\u0434.
                            """),

                    Map.entry("brand.chooseAtLeastOne", "\u0412\u044B\u0431\u0435\u0440\u0438\u0442\u0435 \u0445\u043E\u0442\u044F \u0431\u044B \u043E\u0434\u043D\u0443 \u043C\u0430\u0440\u043A\u0443 \u0438\u043B\u0438 \u043D\u0430\u0436\u043C\u0438\u0442\u0435 \u00AB\u041B\u044E\u0431\u0430\u044F\u00BB."),
                    Map.entry("brand.selected", "\u0412\u044B\u0431\u0440\u0430\u043D\u043D\u044B\u0435 \u043C\u0430\u0440\u043A\u0438:"),
                    Map.entry("summary.settings", "Ваш фильтр:"),
                    Map.entry("summary.currentFilter", "Текущий фильтр"),

                    Map.entry("label.carType", "Тип кузова"),
                    Map.entry("label.brand", "Марка"),
                    Map.entry("label.maxPrice", "Макс. цена"),
                    Map.entry("label.location", "Регион"),
                    Map.entry("label.price", "Цена"),
                    Map.entry("label.source", "Источник"),
                    Map.entry("label.year", "Год"),
                    Map.entry("label.mileage", "Пробег"),
                    Map.entry("label.maxMileage", "Макс. пробег"),
                    Map.entry("label.transmission", "Коробка"),
                    Map.entry("label.fuelType", "Топливо"),
                    Map.entry("label.yearFrom", "Год от"),

                    Map.entry("car.open", "Открыть объявление"),
                    Map.entry("common.any", "Любой"),
                    Map.entry("common.noLimit", "без ограничения"),
                    Map.entry("common.notImportant", "не важно"),
                    Map.entry("common.done", "Готово"),

                    Map.entry("carType.SEDAN", "Седан"),
                    Map.entry("carType.HATCHBACK", "Хэтчбек"),
                    Map.entry("carType.WAGON", "Универсал"),
                    Map.entry("carType.SUV", "SUV"),
                    Map.entry("carType.MINIVAN", "Минивэн"),

                    Map.entry("brand.SKODA", "Škoda"),
                    Map.entry("brand.VOLKSWAGEN", "Volkswagen"),
                    Map.entry("brand.AUDI", "Audi"),
                    Map.entry("brand.BMW", "BMW"),
                    Map.entry("brand.MERCEDES", "Mercedes"),
                    Map.entry("brand.TOYOTA", "Toyota"),
                    Map.entry("brand.FORD", "Ford"),
                    Map.entry("brand.RENAULT", "Renault"),
                    Map.entry("brand.HYUNDAI", "Hyundai"),
                    Map.entry("brand.KIA", "Kia"),
                    Map.entry("brand.PEUGEOT", "Peugeot"),
                    Map.entry("brand.CITROEN", "Citroën"),
                    Map.entry("brand.OPEL", "Opel"),
                    Map.entry("brand.MAZDA", "Mazda"),
                    Map.entry("brand.HONDA", "Honda"),
                    Map.entry("brand.VOLVO", "Volvo"),
                    Map.entry("brand.SEAT", "Seat"),
                    Map.entry("brand.DACIA", "Dacia"),
                    Map.entry("brand.FIAT", "Fiat"),
                    Map.entry("brand.TESLA", "Tesla"),
                    Map.entry("brand.CUPRA", "Cupra"),
                    Map.entry("brand.LEXUS", "Lexus"),
                    Map.entry("brand.BYD", "BYD"),
                    Map.entry("brand.NISSAN", "Nissan"),
                    Map.entry("brand.SUZUKI", "Suzuki"),
                    Map.entry("brand.JEEP", "Jeep"),
                    Map.entry("brand.MINI", "Mini"),
                    Map.entry("brand.PORSCHE", "Porsche"),
                    Map.entry("brand.MITSUBISHI", "Mitsubishi"),
                    Map.entry("brand.SUBARU", "Subaru"),
                    Map.entry("brand.DODGE", "Dodge"),
                    Map.entry("brand.MG", "MG"),
                    Map.entry("brand.LAND_ROVER", "Land Rover"),
                    Map.entry("brand.ALFA_ROMEO", "Alfa Romeo"),
                    Map.entry("brand.DS", "DS"),
                    Map.entry("brand.CHEVROLET", "Chevrolet"),

                    Map.entry("transmission.MANUAL", "Механика"),
                    Map.entry("transmission.AUTOMATIC", "Автомат"),

                    Map.entry("fuelType.PETROL", "Бензин"),
                    Map.entry("fuelType.DIESEL", "Дизель"),
                    Map.entry("fuelType.HYBRID", "Гибрид"),
                    Map.entry("fuelType.PLUGIN_HYBRID", "Подключаемый гибрид"),
                    Map.entry("fuelType.ELECTRIC", "Электро"),
                    Map.entry("fuelType.LPG", "LPG"),
                    Map.entry("fuelType.CNG", "CNG"),

                    Map.entry("location.PRAHA", "Praha"),
                    Map.entry("location.STREDOCESKY", "Středočeský"),
                    Map.entry("location.JIHOMORAVSKY", "Jihomoravský"),
                    Map.entry("location.MORAVSKOSLEZSKY", "Moravskoslezský"),
                    Map.entry("location.USTECKY", "Ústecký"),
                    Map.entry("location.PLZENSKY", "Plzeňský"),
                    Map.entry("location.JIHOCESKY", "Jihočeský"),
                    Map.entry("location.KRALOVEHRADECKY", "Královéhradecký"),
                    Map.entry("location.LIBERECKY", "Liberecký"),
                    Map.entry("location.OLOMOUCKY", "Olomoucký"),
                    Map.entry("location.PARDUBICKY", "Pardubický"),
                    Map.entry("location.ZLINSKY", "Zlínský"),
                    Map.entry("location.VYSOCINA", "Vysočina"),
                    Map.entry("location.KARLOVARSKY", "Karlovarský")
            ),

            "uk", Map.ofEntries(
                    Map.entry("start.welcome", """
                            \uD83D\uDE97 \u041F\u043E\u0448\u0443\u043A \u0430\u0432\u0442\u043E \u043F\u043E \u0432\u0441\u0456\u0439 \u0427\u0435\u0445\u0456\u0457
                            
                            \u042F \u0437\u0431\u0438\u0440\u0430\u044E \u043E\u0433\u043E\u043B\u043E\u0448\u0435\u043D\u043D\u044F \u0437:
                            \u2022 Bazo\u0161.cz
                            \u2022 Sauto.cz
                            \u2022 TipCars.cz
                            
                            \u0414\u043E\u043F\u043E\u043C\u043E\u0436\u0443 \u0432\u0430\u043C:
                            
                            \uD83D\uDD0D \u0437\u043D\u0430\u0439\u0442\u0438 \u0430\u0432\u0442\u043E \u0437\u0430 \u0444\u0456\u043B\u044C\u0442\u0440\u043E\u043C
                            \u2B50 \u0437\u0431\u0435\u0440\u0456\u0433\u0430\u0442\u0438 \u043E\u0431\u0440\u0430\u043D\u0456 \u043E\u0433\u043E\u043B\u043E\u0448\u0435\u043D\u043D\u044F
                            \uD83C\uDD95 \u043E\u0442\u0440\u0438\u043C\u0443\u0432\u0430\u0442\u0438 \u043D\u043E\u0432\u0456 \u043E\u0433\u043E\u043B\u043E\u0448\u0435\u043D\u043D\u044F \u0430\u0432\u0442\u043E\u043C\u0430\u0442\u0438\u0447\u043D\u043E
                            
                            \u041E\u0431\u0435\u0440\u0456\u0442\u044C \u0434\u0456\u044E \u043D\u0438\u0436\u0447\u0435 \uD83D\uDC47
                            """),
                    Map.entry("start.welcomeBack", """
                            \u0420\u0430\u0434\u0438\u0439 \u0431\u0430\u0447\u0438\u0442\u0438 \u0432\u0430\u0441 \u0437\u043D\u043E\u0432\u0443! \uD83D\uDC4B
                            
                            \u0412\u0430\u0448 \u0444\u0456\u043B\u044C\u0442\u0440 \u0443\u0436\u0435 \u043D\u0430\u043B\u0430\u0448\u0442\u043E\u0432\u0430\u043D\u0438\u0439.
                            """),

                    Map.entry("carType.choose", """
                            \u041A\u0440\u043E\u043A 1/8 \u00B7 \u0422\u0438\u043F \u043A\u0443\u0437\u043E\u0432\u0430
                            \u041C\u043E\u0436\u043D\u0430 \u0432\u0438\u0431\u0440\u0430\u0442\u0438 \u043A\u0456\u043B\u044C\u043A\u0430 \u0432\u0430\u0440\u0456\u0430\u043D\u0442\u0456\u0432.
                            """),
                    Map.entry("carType.selected", "\u041E\u0431\u0440\u0430\u043D\u0456 \u0442\u0438\u043F\u0438 \u043A\u0443\u0437\u043E\u0432\u0430:"),
                    Map.entry("carType.chooseAtLeastOne", "\u041E\u0431\u0435\u0440\u0456\u0442\u044C \u0445\u043E\u0447\u0430 \u0431 \u043E\u0434\u0438\u043D \u0442\u0438\u043F \u043A\u0443\u0437\u043E\u0432\u0430 \u0430\u0431\u043E \u043D\u0430\u0442\u0438\u0441\u043D\u0456\u0442\u044C \u00AB\u0411\u0443\u0434\u044C-\u044F\u043A\u0438\u0439\u00BB."),

                    Map.entry("filter.carType.saved", "\u2705 \u0422\u0438\u043F \u043A\u0443\u0437\u043E\u0432\u0430 \u0437\u0431\u0435\u0440\u0435\u0436\u0435\u043D\u043E"),
                    Map.entry("filter.brand.saved", "\u2705 \u041C\u0430\u0440\u043A\u0443 \u0437\u0431\u0435\u0440\u0435\u0436\u0435\u043D\u043E"),
                    Map.entry("filter.price.saved", "\u2705 \u041C\u0430\u043A\u0441\u0438\u043C\u0430\u043B\u044C\u043D\u0443 \u0446\u0456\u043D\u0443 \u0437\u0431\u0435\u0440\u0435\u0436\u0435\u043D\u043E"),
                    Map.entry("filter.location.saved", "\u2705 \u0420\u0435\u0433\u0456\u043E\u043D \u043F\u043E\u0448\u0443\u043A\u0443 \u0437\u0431\u0435\u0440\u0435\u0436\u0435\u043D\u043E"),
                    Map.entry("filter.mileage.saved", "\u2705 \u041C\u0430\u043A\u0441\u0438\u043C\u0430\u043B\u044C\u043D\u0438\u0439 \u043F\u0440\u043E\u0431\u0456\u0433 \u0437\u0431\u0435\u0440\u0435\u0436\u0435\u043D\u043E"),
                    Map.entry("filter.transmission.saved", "\u2705 \u0422\u0438\u043F \u043A\u043E\u0440\u043E\u0431\u043A\u0438 \u043F\u0435\u0440\u0435\u0434\u0430\u0447 \u0437\u0431\u0435\u0440\u0435\u0436\u0435\u043D\u043E"),
                    Map.entry("filter.fuelType.saved", "\u2705 \u0422\u0438\u043F \u043F\u0430\u043B\u044C\u043D\u043E\u0433\u043E \u0437\u0431\u0435\u0440\u0435\u0436\u0435\u043D\u043E"),
                    Map.entry("filter.saved", "\u2705 \u0424\u0456\u043B\u044C\u0442\u0440 \u0437\u0431\u0435\u0440\u0435\u0436\u0435\u043D\u043E"),
                    Map.entry("filter.saved.next", """
                            \u2705 \u0424\u0456\u043B\u044C\u0442\u0440 \u0437\u0431\u0435\u0440\u0435\u0436\u0435\u043D\u043E.
                            
                            \u0429\u043E \u0445\u043E\u0447\u0435\u0442\u0435 \u0437\u0440\u043E\u0431\u0438\u0442\u0438 \u0434\u0430\u043B\u0456?
                            """),

                    Map.entry("help.text", """
                            \u2139\uFE0F \u0429\u043E \u0432\u043C\u0456\u0454 \u0431\u043E\u0442:
                            
                            /filter \u2014 \u043D\u0430\u043B\u0430\u0448\u0442\u0443\u0432\u0430\u0442\u0438 \u0444\u0456\u043B\u044C\u0442\u0440 \u043F\u043E\u0448\u0443\u043A\u0443
                            /myfilter \u2014 \u043F\u043E\u043A\u0430\u0437\u0430\u0442\u0438 \u043F\u043E\u0442\u043E\u0447\u043D\u0438\u0439 \u0444\u0456\u043B\u044C\u0442\u0440
                            /find \u2014 \u0437\u043D\u0430\u0439\u0442\u0438 \u0432\u0456\u0434\u043F\u043E\u0432\u0456\u0434\u043D\u0456 \u0430\u0432\u0442\u043E
                            /services \u2014 \u0432\u0456\u0434\u043A\u0440\u0438\u0442\u0438 \u0441\u0435\u0440\u0432\u0456\u0441\u0438 \u0442\u0430 \u043F\u0456\u0434\u0442\u0440\u0438\u043C\u043A\u0443
                            /favorites \u2014 \u0432\u0456\u0434\u043A\u0440\u0438\u0442\u0438 \u043E\u0431\u0440\u0430\u043D\u0435
                            /language \u2014 \u0437\u043C\u0456\u043D\u0438\u0442\u0438 \u043C\u043E\u0432\u0443
                            /resetfilter \u2014 \u0441\u043A\u0438\u043D\u0443\u0442\u0438 \u0444\u0456\u043B\u044C\u0442\u0440
                            /start \u2014 \u043F\u043E\u0447\u0430\u0442\u0438 \u0437\u0430\u043D\u043E\u0432\u043E
                            
                            \u041D\u043E\u0432\u0456 \u0432\u0456\u0434\u043F\u043E\u0432\u0456\u0434\u043D\u0456 \u043E\u0433\u043E\u043B\u043E\u0448\u0435\u043D\u043D\u044F \u043D\u0430\u0434\u0445\u043E\u0434\u0438\u0442\u0438\u043C\u0443\u0442\u044C \u0430\u0432\u0442\u043E\u043C\u0430\u0442\u0438\u0447\u043D\u043E.
                            """),

                    Map.entry("command.unknown", """
                            \u042F \u043D\u0435 \u0437\u0440\u043E\u0437\u0443\u043C\u0456\u0432 \u0446\u044E \u043A\u043E\u043C\u0430\u043D\u0434\u0443.
                            
                            \u0414\u043E\u0441\u0442\u0443\u043F\u043D\u0456 \u043A\u043E\u043C\u0430\u043D\u0434\u0438:
                            /start
                            /filter
                            /myfilter
                            /resetfilter
                            /services
                            /find
                            /favorites
                            /language
                            /help
                            """),

                    Map.entry("menu.ready", """
                            \uD83D\uDE97 \u0411\u043E\u0442 \u0433\u043E\u0442\u043E\u0432\u0438\u0439 \u0434\u043E \u0440\u043E\u0431\u043E\u0442\u0438.
                            
                            \u042F \u043D\u0430\u0434\u0441\u0438\u043B\u0430\u0442\u0438\u043C\u0443 \u043D\u043E\u0432\u0456 \u043E\u0433\u043E\u043B\u043E\u0448\u0435\u043D\u043D\u044F \u0437\u0430 \u0432\u0430\u0448\u0438\u043C \u0444\u0456\u043B\u044C\u0442\u0440\u043E\u043C.
                            \u041C\u043E\u0436\u043D\u0430 \u043F\u043E\u0447\u0430\u0442\u0438 \u043F\u043E\u0448\u0443\u043A \u043F\u0440\u044F\u043C\u043E \u0437\u0430\u0440\u0430\u0437 \uD83D\uDC47
                            """),
                    Map.entry("menu.search", "\u0417\u043D\u0430\u0439\u0442\u0438 \u0430\u0432\u0442\u043E"),
                    Map.entry("menu.filter", "\u041D\u0430\u043B\u0430\u0448\u0442\u0443\u0432\u0430\u0442\u0438 \u0444\u0456\u043B\u044C\u0442\u0440"),
                    Map.entry("menu.myFilter", "\u041C\u0456\u0439 \u0444\u0456\u043B\u044C\u0442\u0440"),
                    Map.entry("menu.latest", "Нові оголошення"),
                    Map.entry("menu.services", "\u0421\u0435\u0440\u0432\u0456\u0441\u0438"),
                    Map.entry("services.text", "\uD83E\uDDF0 \u0421\u0435\u0440\u0432\u0456\u0441\u0438"),
                    Map.entry("services.housingBot", "\u041F\u043E\u0448\u0443\u043A \u0436\u0438\u0442\u043B\u0430 \u0432 \u0427\u0435\u0445\u0456\u0457"),
                    Map.entry("services.feedback", "\u0417\u0432\u043E\u0440\u043E\u0442\u043D\u0438\u0439 \u0437\u0432\u2019\u044F\u0437\u043E\u043A"),
                    Map.entry("services.supportProject", "\u041F\u0456\u0434\u0442\u0440\u0438\u043C\u0430\u0442\u0438 \u043F\u0440\u043E\u0454\u043A\u0442"),
                    Map.entry("menu.favorites", "Обране"),
                    Map.entry("menu.language", "Мова"),

                    Map.entry("button.showFilter", "\u041F\u043E\u043A\u0430\u0437\u0430\u0442\u0438 \u0444\u0456\u043B\u044C\u0442\u0440"),
                    Map.entry("button.findCars", "\u0417\u043D\u0430\u0439\u0442\u0438 \u0430\u0432\u0442\u043E"),
                    Map.entry("button.editFilter", "\u0417\u043C\u0456\u043D\u0438\u0442\u0438 \u0444\u0456\u043B\u044C\u0442\u0440"),
                    Map.entry("button.resetFilter", "\u0421\u043A\u0438\u043D\u0443\u0442\u0438 \u0444\u0456\u043B\u044C\u0442\u0440"),
                    Map.entry("button.createNewFilter", "\u0421\u0442\u0432\u043E\u0440\u0438\u0442\u0438 \u043D\u043E\u0432\u0438\u0439 \u0444\u0456\u043B\u044C\u0442\u0440"),
                    Map.entry("button.open", "\u0412\u0456\u0434\u043A\u0440\u0438\u0442\u0438 \u043E\u0433\u043E\u043B\u043E\u0448\u0435\u043D\u043D\u044F"),
                    Map.entry("button.addFavorite", "\u0414\u043E\u0434\u0430\u0442\u0438 \u0432 \u043E\u0431\u0440\u0430\u043D\u0435"),
                    Map.entry("button.removeFavorite", "\u0412\u0438\u0434\u0430\u043B\u0438\u0442\u0438 \u0437 \u043E\u0431\u0440\u0430\u043D\u043E\u0433\u043E"),
                    Map.entry("button.prev", "\u041D\u0430\u0437\u0430\u0434"),
                    Map.entry("button.next", "\u0414\u0430\u043B\u0456"),
                    Map.entry("button.restart", "\u041D\u043E\u0432\u0438\u0439 \u043F\u043E\u0448\u0443\u043A"),
                    Map.entry("button.stop", "\u0423 \u043C\u0435\u043D\u044E"),
                    Map.entry("button.skip", "\u041F\u0440\u043E\u043F\u0443\u0441\u0442\u0438\u0442\u0438"),
                    Map.entry("button.backToMenu", "\u0423 \u043C\u0435\u043D\u044E"),
                    Map.entry("button.newSearch", "\u041D\u043E\u0432\u0438\u0439 \u043F\u043E\u0448\u0443\u043A"),

                    Map.entry("filter.notConfigured", """
                            \u0424\u0456\u043B\u044C\u0442\u0440 \u0449\u0435 \u043D\u0435 \u043D\u0430\u043B\u0430\u0448\u0442\u043E\u0432\u0430\u043D\u0438\u0439.
                            
                            \u041D\u0430\u0442\u0438\u0441\u043D\u0456\u0442\u044C /filter, \u0456 \u044F \u0434\u043E\u043F\u043E\u043C\u043E\u0436\u0443 \u043D\u0430\u043B\u0430\u0448\u0442\u0443\u0432\u0430\u0442\u0438 \u043F\u043E\u0448\u0443\u043A \u043A\u0440\u043E\u043A \u0437\u0430 \u043A\u0440\u043E\u043A\u043E\u043C.
                            """),
                    Map.entry("filter.reset", """
                            \u267B\uFE0F \u0424\u0456\u043B\u044C\u0442\u0440 \u0441\u043A\u0438\u043D\u0443\u0442\u043E.
                            
                            \u0414\u0430\u0432\u0430\u0439\u0442\u0435 \u043D\u0430\u043B\u0430\u0448\u0442\u0443\u0454\u043C\u043E \u043D\u043E\u0432\u0438\u0439.
                            """),

                    Map.entry("cars.empty", """
                            У базі поки немає оголошень.

                            Спробуйте зайти трохи пізніше.
                            """),
                    Map.entry("cars.noMatches", "За вашим фільтром поки нічого не знайдено."),
                    Map.entry("cars.matchesFound", """
                            🚗 Знайдено відповідні оголошення:
                            """),
                    Map.entry("cars.latest", """
                            🆕 Останні оголошення з бази:
                            """),
                    Map.entry("cars.noMatches.pretty", """
                            😕 За вашим фільтром поки нічого не знайдено.

                            Що можна спробувати:
                            • збільшити максимальну ціну
                            • прибрати обмеження по пробігу
                            • вибрати будь-який регіон
                            • вибрати більше марок
                            """),
                    Map.entry("cars.noMore", "Це всі оголошення на зараз 👌"),
                    Map.entry("cars.morePrompt", "Показати ще відповідні оголошення?"),
                    Map.entry("cars.searchFinished", """
                            ✅ Пошук завершено.

                            Ви можете змінити фільтр або повернутися пізніше за новими оголошеннями.
                            """),

                    Map.entry("favorites.added", """
                            ⭐ Оголошення додано в обране.

                            Відкрийте /favorites, щоб переглянути збережені оголошення.
                            """),
                    Map.entry("favorites.alreadyExists", """
                            ⭐ Це оголошення вже є в обраному.
                            """),
                    Map.entry("favorites.error", """
                            Не вдалося додати оголошення в обране.
                            Спробуйте ще раз.
                            """),
                    Map.entry("favorites.empty", """
                            ⭐ В обраному поки нічого немає.
                    
                            Коли знайдете цікаве оголошення, натисніть «Додати в обране» — воно з’явиться тут.
                            """),
                    Map.entry("favorites.title", "⭐ Ваше обране:"),
                    Map.entry("favorites.removed", """
                            🗑 Оголошення видалено з обраного.
                            """),
                    Map.entry("favorites.notFound", """
                            Оголошення не знайдено в обраному.
                            """),
                    Map.entry("favorites.removeError", """
                            Не вдалося видалити оголошення з обраного.
                            Спробуйте ще раз.
                            """),

                    Map.entry("language.choose", "Оберіть мову:"),
                    Map.entry("language.changed", "✅ Мову змінено."),
                    Map.entry("language.nextStep", """
                            Що хочете зробити далі?

                            • переглянути поточний фільтр
                            • змінити фільтр
                            • почати пошук
                            """),

                    Map.entry("brand.choose", """
                            \u041A\u0440\u043E\u043A 2/8 \u00B7 \u041C\u0430\u0440\u043A\u0430
                            \u041C\u043E\u0436\u043D\u0430 \u0432\u0438\u0431\u0440\u0430\u0442\u0438 \u043A\u0456\u043B\u044C\u043A\u0430 \u0432\u0430\u0440\u0456\u0430\u043D\u0442\u0456\u0432.
                            """),
                    Map.entry("price.choose", """
                            \u041A\u0440\u043E\u043A 3/8 \u00B7 \u0411\u044E\u0434\u0436\u0435\u0442
                            \u041E\u0431\u0435\u0440\u0456\u0442\u044C \u043C\u0430\u043A\u0441\u0438\u043C\u0430\u043B\u044C\u043D\u0443 \u0446\u0456\u043D\u0443.
                            """),
                    Map.entry("location.choose", """
                            \u041A\u0440\u043E\u043A 4/8 \u00B7 \u0420\u0435\u0433\u0456\u043E\u043D
                            \u041E\u0431\u0435\u0440\u0456\u0442\u044C \u0440\u0435\u0433\u0456\u043E\u043D \u043F\u043E\u0448\u0443\u043A\u0443.
                            """),
                    Map.entry("mileage.choose", """
                            \u041A\u0440\u043E\u043A 5/8 \u00B7 \u041F\u0440\u043E\u0431\u0456\u0433
                            \u041E\u0431\u0435\u0440\u0456\u0442\u044C \u043C\u0430\u043A\u0441\u0438\u043C\u0430\u043B\u044C\u043D\u0438\u0439 \u043F\u0440\u043E\u0431\u0456\u0433.
                            """),
                    Map.entry("transmission.choose", """
                            \u041A\u0440\u043E\u043A 6/8 \u00B7 \u041A\u043E\u0440\u043E\u0431\u043A\u0430
                            \u041E\u0431\u0435\u0440\u0456\u0442\u044C \u0442\u0438\u043F \u043A\u043E\u0440\u043E\u0431\u043A\u0438 \u043F\u0435\u0440\u0435\u0434\u0430\u0447.
                            """),
                    Map.entry("fuelType.choose", """
                            \u041A\u0440\u043E\u043A 7/8 \u00B7 \u041F\u0430\u043B\u044C\u043D\u0435
                            \u041E\u0431\u0435\u0440\u0456\u0442\u044C \u0442\u0438\u043F \u043F\u0430\u043B\u044C\u043D\u043E\u0433\u043E.
                            """),
                    Map.entry("yearFrom.choose", """
                            \u041A\u0440\u043E\u043A 8/8 \u00B7 \u0420\u0456\u043A \u0432\u0438\u043F\u0443\u0441\u043A\u0443
                            \u041E\u0431\u0435\u0440\u0456\u0442\u044C \u043C\u0456\u043D\u0456\u043C\u0430\u043B\u044C\u043D\u0438\u0439 \u0440\u0456\u043A.
                            """),

                    Map.entry("brand.chooseAtLeastOne", "\u041E\u0431\u0435\u0440\u0456\u0442\u044C \u0445\u043E\u0447\u0430 \u0431 \u043E\u0434\u043D\u0443 \u043C\u0430\u0440\u043A\u0443 \u0430\u0431\u043E \u043D\u0430\u0442\u0438\u0441\u043D\u0456\u0442\u044C \u00AB\u0411\u0443\u0434\u044C-\u044F\u043A\u0430\u00BB."),
                    Map.entry("brand.selected", "\u041E\u0431\u0440\u0430\u043D\u0456 \u043C\u0430\u0440\u043A\u0438:"),
                    Map.entry("summary.settings", "Ваш фільтр:"),
                    Map.entry("summary.currentFilter", "Поточний фільтр"),

                    Map.entry("label.carType", "Тип кузова"),
                    Map.entry("label.brand", "Марка"),
                    Map.entry("label.maxPrice", "Макс. ціна"),
                    Map.entry("label.location", "Регіон"),
                    Map.entry("label.price", "Ціна"),
                    Map.entry("label.source", "Джерело"),
                    Map.entry("label.year", "Рік"),
                    Map.entry("label.mileage", "Пробіг"),
                    Map.entry("label.maxMileage", "Макс. пробіг"),
                    Map.entry("label.transmission", "Коробка"),
                    Map.entry("label.fuelType", "Пальне"),
                    Map.entry("label.yearFrom", "Рік від"),

                    Map.entry("car.open", "Відкрити оголошення"),
                    Map.entry("common.any", "Будь-який"),
                    Map.entry("common.noLimit", "без обмеження"),
                    Map.entry("common.notImportant", "не важливо"),
                    Map.entry("common.done", "Готово"),

                    Map.entry("carType.SEDAN", "Седан"),
                    Map.entry("carType.HATCHBACK", "Хетчбек"),
                    Map.entry("carType.WAGON", "Універсал"),
                    Map.entry("carType.SUV", "SUV"),
                    Map.entry("carType.MINIVAN", "Мінівен"),

                    Map.entry("brand.SKODA", "Škoda"),
                    Map.entry("brand.VOLKSWAGEN", "Volkswagen"),
                    Map.entry("brand.AUDI", "Audi"),
                    Map.entry("brand.BMW", "BMW"),
                    Map.entry("brand.MERCEDES", "Mercedes"),
                    Map.entry("brand.TOYOTA", "Toyota"),
                    Map.entry("brand.FORD", "Ford"),
                    Map.entry("brand.RENAULT", "Renault"),
                    Map.entry("brand.HYUNDAI", "Hyundai"),
                    Map.entry("brand.KIA", "Kia"),
                    Map.entry("brand.PEUGEOT", "Peugeot"),
                    Map.entry("brand.CITROEN", "Citroën"),
                    Map.entry("brand.OPEL", "Opel"),
                    Map.entry("brand.MAZDA", "Mazda"),
                    Map.entry("brand.HONDA", "Honda"),
                    Map.entry("brand.VOLVO", "Volvo"),
                    Map.entry("brand.SEAT", "Seat"),
                    Map.entry("brand.DACIA", "Dacia"),
                    Map.entry("brand.FIAT", "Fiat"),
                    Map.entry("brand.TESLA", "Tesla"),
                    Map.entry("brand.CUPRA", "Cupra"),
                    Map.entry("brand.LEXUS", "Lexus"),
                    Map.entry("brand.BYD", "BYD"),
                    Map.entry("brand.NISSAN", "Nissan"),
                    Map.entry("brand.SUZUKI", "Suzuki"),
                    Map.entry("brand.JEEP", "Jeep"),
                    Map.entry("brand.MINI", "Mini"),
                    Map.entry("brand.PORSCHE", "Porsche"),
                    Map.entry("brand.MITSUBISHI", "Mitsubishi"),
                    Map.entry("brand.SUBARU", "Subaru"),
                    Map.entry("brand.DODGE", "Dodge"),
                    Map.entry("brand.MG", "MG"),
                    Map.entry("brand.LAND_ROVER", "Land Rover"),
                    Map.entry("brand.ALFA_ROMEO", "Alfa Romeo"),
                    Map.entry("brand.DS", "DS"),
                    Map.entry("brand.CHEVROLET", "Chevrolet"),

                    Map.entry("transmission.MANUAL", "Механіка"),
                    Map.entry("transmission.AUTOMATIC", "Автомат"),

                    Map.entry("fuelType.PETROL", "Бензин"),
                    Map.entry("fuelType.DIESEL", "Дизель"),
                    Map.entry("fuelType.HYBRID", "Гібрид"),
                    Map.entry("fuelType.PLUGIN_HYBRID", "Плагін-гібрид"),
                    Map.entry("fuelType.ELECTRIC", "Електро"),
                    Map.entry("fuelType.LPG", "LPG"),
                    Map.entry("fuelType.CNG", "CNG"),

                    Map.entry("location.PRAHA", "Praha"),
                    Map.entry("location.STREDOCESKY", "Středočeský"),
                    Map.entry("location.JIHOMORAVSKY", "Jihomoravský"),
                    Map.entry("location.MORAVSKOSLEZSKY", "Moravskoslezský"),
                    Map.entry("location.USTECKY", "Ústecký"),
                    Map.entry("location.PLZENSKY", "Plzeňský"),
                    Map.entry("location.JIHOCESKY", "Jihočeský"),
                    Map.entry("location.KRALOVEHRADECKY", "Královéhradecký"),
                    Map.entry("location.LIBERECKY", "Liberecký"),
                    Map.entry("location.OLOMOUCKY", "Olomoucký"),
                    Map.entry("location.PARDUBICKY", "Pardubický"),
                    Map.entry("location.ZLINSKY", "Zlínský"),
                    Map.entry("location.VYSOCINA", "Vysočina"),
                    Map.entry("location.KARLOVARSKY", "Karlovarský")
            ),

            "cs", Map.ofEntries(
                    Map.entry("start.welcome", """
                            \uD83D\uDE97 Vyhled\u00E1v\u00E1n\u00ED aut po cel\u00E9 \u010CR
                            
                            Sb\u00EDr\u00E1m inzer\u00E1ty z:
                            \u2022 Bazo\u0161.cz
                            \u2022 Sauto.cz
                            \u2022 TipCars.cz
                            
                            Pomohu v\u00E1m:
                            
                            \uD83D\uDD0D naj\u00EDt auto podle filtru
                            \u2B50 ukl\u00E1dat obl\u00EDben\u00E9 inzer\u00E1ty
                            \uD83C\uDD95 automaticky sledovat nov\u00E9 inzer\u00E1ty
                            
                            Vyberte akci n\u00ED\u017Ee \uD83D\uDC47
                            """),
                    Map.entry("start.welcomeBack", """
                            V\u00EDtejte zp\u011Bt! \uD83D\uDC4B
                            
                            V\u00E1\u0161 filtr je u\u017E nastaven.
                            """),

                    Map.entry("carType.choose", """
                            Krok 1/8 \u00B7 Karoserie
                            M\u016F\u017Eete vybrat v\u00EDce mo\u017Enost\u00ED.
                            """),
                    Map.entry("carType.selected", "Vybran\u00E9 typy karoserie:"),
                    Map.entry("carType.chooseAtLeastOne", "Vyberte alespo\u0148 jeden typ karoserie nebo stiskn\u011Bte \u201ELibovoln\u00FD\u201C."),

                    Map.entry("filter.carType.saved", "\u2705 Typ karoserie byl ulo\u017Een"),
                    Map.entry("filter.brand.saved", "\u2705 Zna\u010Dka byla ulo\u017Eena"),
                    Map.entry("filter.price.saved", "\u2705 Maxim\u00E1ln\u00ED cena byla ulo\u017Eena"),
                    Map.entry("filter.location.saved", "\u2705 Region hled\u00E1n\u00ED byl ulo\u017Een"),
                    Map.entry("filter.mileage.saved", "\u2705 Maxim\u00E1ln\u00ED n\u00E1jezd byl ulo\u017Een"),
                    Map.entry("filter.transmission.saved", "\u2705 Typ p\u0159evodovky byl ulo\u017Een"),
                    Map.entry("filter.fuelType.saved", "\u2705 Typ paliva byl ulo\u017Een"),
                    Map.entry("filter.saved", "\u2705 Filtr byl ulo\u017Een"),
                    Map.entry("filter.saved.next", """
                            \u2705 Filtr byl ulo\u017Een.
                            
                            Co chcete ud\u011Blat d\u00E1l?
                            """),

                    Map.entry("help.text", """
                            \u2139\uFE0F Co bot um\u00ED:
                            
                            /filter \u2014 nastavit filtr hled\u00E1n\u00ED
                            /myfilter \u2014 zobrazit aktu\u00E1ln\u00ED filtr
                            /find \u2014 naj\u00EDt vhodn\u00E1 auta z datab\u00E1ze
                            /services \u2014 otev\u0159\u00EDt slu\u017Eby a podporu
                            /favorites \u2014 otev\u0159\u00EDt obl\u00EDben\u00E9
                            /language \u2014 zm\u011Bnit jazyk
                            /resetfilter \u2014 resetovat filtr
                            /start \u2014 za\u010D\u00EDt znovu
                            
                            Nov\u00E9 vhodn\u00E9 inzer\u00E1ty budete dost\u00E1vat automaticky.
                            """),

                    Map.entry("command.unknown", """
                            Tomuto p\u0159\u00EDkazu nerozum\u00EDm.
                            
                            Dostupn\u00E9 p\u0159\u00EDkazy:
                            /start
                            /filter
                            /myfilter
                            /resetfilter
                            /services
                            /find
                            /favorites
                            /language
                            /help
                            """),

                    Map.entry("menu.ready", """
                            \uD83D\uDE97 Bot je p\u0159ipraven.
                            
                            Budu pos\u00EDlat nov\u00E9 inzer\u00E1ty podle va\u0161eho filtru.
                            M\u016F\u017Eete za\u010D\u00EDt hledat hned te\u010F \uD83D\uDC47
                            """),
                    Map.entry("menu.search", "Naj\u00EDt auto"),
                    Map.entry("menu.filter", "Nastavit filtr"),
                    Map.entry("menu.myFilter", "M\u016Fj filtr"),
                    Map.entry("menu.latest", "Nové inzeráty"),
                    Map.entry("menu.services", "Slu\u017Eby"),
                    Map.entry("services.text", "\uD83E\uDDF0 Slu\u017Eby"),
                    Map.entry("services.housingBot", "Hled\u00E1n\u00ED bydlen\u00ED v \u010Cesku"),
                    Map.entry("services.feedback", "Zp\u011Btn\u00E1 vazba"),
                    Map.entry("services.supportProject", "Podpo\u0159it projekt"),
                    Map.entry("menu.favorites", "Oblíbené"),
                    Map.entry("menu.language", "Jazyk"),

                    Map.entry("button.showFilter", "Zobrazit filtr"),
                    Map.entry("button.findCars", "Naj\u00EDt auta"),
                    Map.entry("button.editFilter", "Upravit filtr"),
                    Map.entry("button.resetFilter", "Resetovat filtr"),
                    Map.entry("button.createNewFilter", "Vytvo\u0159it nov\u00FD filtr"),
                    Map.entry("button.open", "Otev\u0159\u00EDt inzer\u00E1t"),
                    Map.entry("button.addFavorite", "P\u0159idat do obl\u00EDben\u00FDch"),
                    Map.entry("button.removeFavorite", "Odebrat z obl\u00EDben\u00FDch"),
                    Map.entry("button.prev", "Zp\u011Bt"),
                    Map.entry("button.next", "Dal\u0161\u00ED"),
                    Map.entry("button.restart", "Nov\u00E9 hled\u00E1n\u00ED"),
                    Map.entry("button.stop", "Do menu"),
                    Map.entry("button.skip", "P\u0159esko\u010Dit"),
                    Map.entry("button.backToMenu", "Do menu"),
                    Map.entry("button.newSearch", "Nov\u00E9 hled\u00E1n\u00ED"),

                    Map.entry("filter.notConfigured", """
                            Filtr je\u0161t\u011B nen\u00ED nastaven.
                            
                            Stiskn\u011Bte /filter a nastav\u00EDme hled\u00E1n\u00ED krok za krokem.
                            """),
                    Map.entry("filter.reset", """
                            \u267B\uFE0F Filtr byl resetov\u00E1n.
                            
                            Nastav\u00EDme nov\u00FD.
                            """),

                    Map.entry("cars.empty", """
                            V databázi zatím nejsou žádné inzeráty.

                            Zkuste to prosím později.
                            """),
                    Map.entry("cars.noMatches", "Pro váš filtr zatím nebyly nalezeny žádné inzeráty."),
                    Map.entry("cars.matchesFound", """
                            🚗 Nalezena vhodná auta:
                            """),
                    Map.entry("cars.latest", """
                            🆕 Nejnovější inzeráty z databáze:
                            """),
                    Map.entry("cars.noMatches.pretty", """
                            😕 Pro váš filtr zatím nebyly nalezeny žádné výsledky.

                            Můžete zkusit:
                            • zvýšit maximální cenu
                            • zrušit omezení nájezdu
                            • vybrat libovolný region
                            • vybrat více značek
                            """),
                    Map.entry("cars.noMore", "To jsou zatím všechna auta 👌"),
                    Map.entry("cars.morePrompt", "Zobrazit další vhodné inzeráty?"),
                    Map.entry("cars.searchFinished", """
                            ✅ Hledání bylo ukončeno.

                            Můžete upravit filtr nebo se vrátit později pro nové nabídky.
                            """),

                    Map.entry("favorites.added", """
                            ⭐ Inzerát byl přidán do oblíbených.

                            Otevřete /favorites pro zobrazení uložených inzerátů.
                            """),
                    Map.entry("favorites.alreadyExists", """
                            ⭐ Tento inzerát už je v oblíbených.
                            """),
                    Map.entry("favorites.error", """
                            Nepodařilo se přidat inzerát do oblíbených.
                            Zkuste to prosím znovu.
                            """),
                    Map.entry("favorites.empty", """
                            ⭐ V oblíbených zatím nic není.
                    
                            Jakmile najdete zajímavý inzerát, klikněte na „Přidat do oblíbených“ — objeví se tady.
                            """),
                    Map.entry("favorites.title", "⭐ Vaše oblíbené inzeráty:"),
                    Map.entry("favorites.removed", """
                            🗑 Inzerát byl odebrán z oblíbených.
                            """),
                    Map.entry("favorites.notFound", """
                            Inzerát nebyl v oblíbených nalezen.
                            """),
                    Map.entry("favorites.removeError", """
                            Nepodařilo se odebrat inzerát z oblíbených.
                            Zkuste to prosím znovu.
                            """),

                    Map.entry("language.choose", "Vyber jazyk:"),
                    Map.entry("language.changed", "✅ Jazyk byl změněn."),
                    Map.entry("language.nextStep", """
                            Co chcete udělat dál?

                            • zobrazit aktuální filtr
                            • upravit filtr
                            • spustit hledání
                            """),

                    Map.entry("brand.choose", """
                            Krok 2/8 \u00B7 Zna\u010Dka
                            M\u016F\u017Eete vybrat v\u00EDce mo\u017Enost\u00ED.
                            """),
                    Map.entry("price.choose", """
                            Krok 3/8 \u00B7 Rozpo\u010Det
                            Vyberte maxim\u00E1ln\u00ED cenu.
                            """),
                    Map.entry("location.choose", """
                            Krok 4/8 \u00B7 Region
                            Vyberte region hled\u00E1n\u00ED.
                            """),
                    Map.entry("mileage.choose", """
                            Krok 5/8 \u00B7 N\u00E1jezd
                            Vyberte maxim\u00E1ln\u00ED n\u00E1jezd.
                            """),
                    Map.entry("transmission.choose", """
                            Krok 6/8 \u00B7 P\u0159evodovka
                            Vyberte typ p\u0159evodovky.
                            """),
                    Map.entry("fuelType.choose", """
                            Krok 7/8 \u00B7 Palivo
                            Vyberte typ paliva.
                            """),
                    Map.entry("yearFrom.choose", """
                            Krok 8/8 \u00B7 Rok v\u00FDroby
                            Vyberte minim\u00E1ln\u00ED rok.
                            """),

                    Map.entry("brand.chooseAtLeastOne", "Vyberte alespo\u0148 jednu zna\u010Dku nebo stiskn\u011Bte \u201ELibovoln\u00E1\u201C."),
                    Map.entry("brand.selected", "Vybran\u00E9 zna\u010Dky:"),
                    Map.entry("summary.settings", "Váš filtr:"),
                    Map.entry("summary.currentFilter", "Aktuální filtr"),

                    Map.entry("label.carType", "Typ karoserie"),
                    Map.entry("label.brand", "Značka"),
                    Map.entry("label.maxPrice", "Max. cena"),
                    Map.entry("label.location", "Region"),
                    Map.entry("label.price", "Cena"),
                    Map.entry("label.source", "Zdroj"),
                    Map.entry("label.year", "Rok"),
                    Map.entry("label.mileage", "Nájezd"),
                    Map.entry("label.maxMileage", "Max. nájezd"),
                    Map.entry("label.transmission", "Převodovka"),
                    Map.entry("label.fuelType", "Palivo"),
                    Map.entry("label.yearFrom", "Rok od"),

                    Map.entry("car.open", "Otevřít inzerát"),
                    Map.entry("common.any", "Libovolný"),
                    Map.entry("common.noLimit", "bez omezení"),
                    Map.entry("common.notImportant", "nezáleží"),
                    Map.entry("common.done", "Hotovo"),

                    Map.entry("carType.SEDAN", "Sedan"),
                    Map.entry("carType.HATCHBACK", "Hatchback"),
                    Map.entry("carType.WAGON", "Kombi"),
                    Map.entry("carType.SUV", "SUV"),
                    Map.entry("carType.MINIVAN", "Minivan"),

                    Map.entry("brand.SKODA", "Škoda"),
                    Map.entry("brand.VOLKSWAGEN", "Volkswagen"),
                    Map.entry("brand.AUDI", "Audi"),
                    Map.entry("brand.BMW", "BMW"),
                    Map.entry("brand.MERCEDES", "Mercedes"),
                    Map.entry("brand.TOYOTA", "Toyota"),
                    Map.entry("brand.FORD", "Ford"),
                    Map.entry("brand.RENAULT", "Renault"),
                    Map.entry("brand.HYUNDAI", "Hyundai"),
                    Map.entry("brand.KIA", "Kia"),
                    Map.entry("brand.PEUGEOT", "Peugeot"),
                    Map.entry("brand.CITROEN", "Citroën"),
                    Map.entry("brand.OPEL", "Opel"),
                    Map.entry("brand.MAZDA", "Mazda"),
                    Map.entry("brand.HONDA", "Honda"),
                    Map.entry("brand.VOLVO", "Volvo"),
                    Map.entry("brand.SEAT", "Seat"),
                    Map.entry("brand.DACIA", "Dacia"),
                    Map.entry("brand.FIAT", "Fiat"),
                    Map.entry("brand.TESLA", "Tesla"),
                    Map.entry("brand.CUPRA", "Cupra"),
                    Map.entry("brand.LEXUS", "Lexus"),
                    Map.entry("brand.BYD", "BYD"),
                    Map.entry("brand.NISSAN", "Nissan"),
                    Map.entry("brand.SUZUKI", "Suzuki"),
                    Map.entry("brand.JEEP", "Jeep"),
                    Map.entry("brand.MINI", "Mini"),
                    Map.entry("brand.PORSCHE", "Porsche"),
                    Map.entry("brand.MITSUBISHI", "Mitsubishi"),
                    Map.entry("brand.SUBARU", "Subaru"),
                    Map.entry("brand.DODGE", "Dodge"),
                    Map.entry("brand.MG", "MG"),
                    Map.entry("brand.LAND_ROVER", "Land Rover"),
                    Map.entry("brand.ALFA_ROMEO", "Alfa Romeo"),
                    Map.entry("brand.DS", "DS"),
                    Map.entry("brand.CHEVROLET", "Chevrolet"),

                    Map.entry("transmission.MANUAL", "Manuální"),
                    Map.entry("transmission.AUTOMATIC", "Automat"),

                    Map.entry("fuelType.PETROL", "Benzín"),
                    Map.entry("fuelType.DIESEL", "Diesel"),
                    Map.entry("fuelType.HYBRID", "Hybrid"),
                    Map.entry("fuelType.PLUGIN_HYBRID", "Plug-in hybrid"),
                    Map.entry("fuelType.ELECTRIC", "Elektro"),
                    Map.entry("fuelType.LPG", "LPG"),
                    Map.entry("fuelType.CNG", "CNG"),

                    Map.entry("location.PRAHA", "Praha"),
                    Map.entry("location.STREDOCESKY", "Středočeský"),
                    Map.entry("location.JIHOMORAVSKY", "Jihomoravský"),
                    Map.entry("location.MORAVSKOSLEZSKY", "Moravskoslezský"),
                    Map.entry("location.USTECKY", "Ústecký"),
                    Map.entry("location.PLZENSKY", "Plzeňský"),
                    Map.entry("location.JIHOCESKY", "Jihočeský"),
                    Map.entry("location.KRALOVEHRADECKY", "Královéhradecký"),
                    Map.entry("location.LIBERECKY", "Liberecký"),
                    Map.entry("location.OLOMOUCKY", "Olomoucký"),
                    Map.entry("location.PARDUBICKY", "Pardubický"),
                    Map.entry("location.ZLINSKY", "Zlínský"),
                    Map.entry("location.VYSOCINA", "Vysočina"),
                    Map.entry("location.KARLOVARSKY", "Karlovarský")
            ),

            "en", Map.ofEntries(
                    Map.entry("start.welcome", """
                            \uD83D\uDE97 Car search across the Czech Republic
                            
                            I collect listings from:
                            \u2022 Bazo\u0161.cz
                            \u2022 Sauto.cz
                            \u2022 TipCars.cz
                            
                            I can help you:
                            
                            \uD83D\uDD0D find cars using filters
                            \u2B50 save favorite listings
                            \uD83C\uDD95 receive new matching listings automatically
                            
                            Choose an action below \uD83D\uDC47
                            """),
                    Map.entry("start.welcomeBack", """
                            Welcome back! \uD83D\uDC4B
                            
                            Your filter is already configured.
                            """),

                    Map.entry("carType.choose", """
                            Step 1/8 \u00B7 Body type
                            You can choose multiple options.
                            """),
                    Map.entry("carType.selected", "Selected body types:"),
                    Map.entry("carType.chooseAtLeastOne", "Choose at least one body type or press \u201CAny\u201D."),

                    Map.entry("filter.carType.saved", "\u2705 Body type saved"),
                    Map.entry("filter.brand.saved", "\u2705 Brand saved"),
                    Map.entry("filter.price.saved", "\u2705 Maximum price saved"),
                    Map.entry("filter.location.saved", "\u2705 Search region saved"),
                    Map.entry("filter.mileage.saved", "\u2705 Maximum mileage saved"),
                    Map.entry("filter.transmission.saved", "\u2705 Transmission type saved"),
                    Map.entry("filter.fuelType.saved", "\u2705 Fuel type saved"),
                    Map.entry("filter.saved", "\u2705 Filter saved"),
                    Map.entry("filter.saved.next", """
                            \u2705 Filter saved.
                            
                            What would you like to do next?
                            """),

                    Map.entry("help.text", """
                            \u2139\uFE0F What this bot can do:
                            
                            /filter \u2014 set up a search filter
                            /myfilter \u2014 show your current filter
                            /find \u2014 find matching cars
                            /services \u2014 open services and support
                            /favorites \u2014 open favorites
                            /language \u2014 change language
                            /resetfilter \u2014 reset filter
                            /start \u2014 start over
                            
                            New matching listings will arrive automatically.
                            """),

                    Map.entry("command.unknown", """
                            I did not understand that command.
                            
                            Available commands:
                            /start
                            /filter
                            /myfilter
                            /resetfilter
                            /services
                            /find
                            /favorites
                            /language
                            /help
                            """),

                    Map.entry("menu.ready", """
                            \uD83D\uDE97 Bot is ready.
                            
                            I will send new listings matching your filter.
                            You can start searching right now \uD83D\uDC47
                            """),
                    Map.entry("menu.search", "Find cars"),
                    Map.entry("menu.filter", "Set filter"),
                    Map.entry("menu.myFilter", "My filter"),
                    Map.entry("menu.latest", "New listings"),
                    Map.entry("menu.services", "Services"),
                    Map.entry("services.text", "\uD83E\uDDF0 Services"),
                    Map.entry("services.housingBot", "Housing search in Czechia"),
                    Map.entry("services.feedback", "Feedback"),
                    Map.entry("services.supportProject", "Support the project"),
                    Map.entry("menu.favorites", "Favorites"),
                    Map.entry("menu.language", "Language"),

                    Map.entry("button.showFilter", "Show filter"),
                    Map.entry("button.findCars", "Find cars"),
                    Map.entry("button.editFilter", "Edit filter"),
                    Map.entry("button.resetFilter", "Reset filter"),
                    Map.entry("button.createNewFilter", "Create new filter"),
                    Map.entry("button.open", "Open listing"),
                    Map.entry("button.addFavorite", "Add to favorites"),
                    Map.entry("button.removeFavorite", "Remove from favorites"),
                    Map.entry("button.prev", "Back"),
                    Map.entry("button.next", "Next"),
                    Map.entry("button.restart", "New search"),
                    Map.entry("button.stop", "Back to menu"),
                    Map.entry("button.skip", "Skip"),
                    Map.entry("button.backToMenu", "Back to menu"),
                    Map.entry("button.newSearch", "New search"),

                    Map.entry("filter.notConfigured", """
                            Filter is not configured yet.
                            
                            Press /filter and I will help you set it up step by step.
                            """),
                    Map.entry("filter.reset", """
                            \u267B\uFE0F Filter has been reset.
                            
                            Let's set up a new one.
                            """),

                    Map.entry("cars.empty", """
                            There are no listings in the database yet.

                            Please check again later.
                            """),
                    Map.entry("cars.noMatches", "No listings found for your filter yet."),
                    Map.entry("cars.matchesFound", """
                            🚗 Matching cars found:
                            """),
                    Map.entry("cars.latest", """
                            🆕 Latest listings from database:
                            """),
                    Map.entry("cars.noMatches.pretty", """
                            😕 No listings found for your filter yet.

                            You can try:
                            • increasing max price
                            • removing mileage limit
                            • choosing any region
                            • selecting more brands
                            """),
                    Map.entry("cars.noMore", "That's all listings for now 👌"),
                    Map.entry("cars.morePrompt", "Show more matching listings?"),
                    Map.entry("cars.searchFinished", """
                            ✅ Search finished.

                            You can edit your filter or come back later for new listings.
                            """),

                    Map.entry("favorites.added", """
                            ⭐ Listing added to favorites.

                            Open /favorites to see your saved listings.
                            """),
                    Map.entry("favorites.alreadyExists", """
                            ⭐ This listing is already in favorites.
                            """),
                    Map.entry("favorites.error", """
                            Failed to add listing to favorites.
                            Please try again.
                            """),
                    Map.entry("favorites.empty", """
                            ⭐ Favorites are empty for now.
                    
                            When you find an interesting listing, tap “Add to favorites” — it will appear here.
                            """),
                    Map.entry("favorites.title", "⭐ Your favorites:"),
                    Map.entry("favorites.removed", """
                            🗑 Listing removed from favorites.
                            """),
                    Map.entry("favorites.notFound", """
                            Listing was not found in favorites.
                            """),
                    Map.entry("favorites.removeError", """
                            Failed to remove listing from favorites.
                            Please try again.
                            """),

                    Map.entry("language.choose", "Choose language:"),
                    Map.entry("language.changed", "✅ Language changed."),
                    Map.entry("language.nextStep", """
                            What would you like to do next?

                            • view current filter
                            • edit filter
                            • start search
                            """),

                    Map.entry("brand.choose", """
                            Step 2/8 \u00B7 Brand
                            You can choose multiple options.
                            """),
                    Map.entry("price.choose", """
                            Step 3/8 \u00B7 Budget
                            Choose the maximum price.
                            """),
                    Map.entry("location.choose", """
                            Step 4/8 \u00B7 Region
                            Choose the search region.
                            """),
                    Map.entry("mileage.choose", """
                            Step 5/8 \u00B7 Mileage
                            Choose the maximum mileage.
                            """),
                    Map.entry("transmission.choose", """
                            Step 6/8 \u00B7 Transmission
                            Choose the transmission type.
                            """),
                    Map.entry("fuelType.choose", """
                            Step 7/8 \u00B7 Fuel
                            Choose the fuel type.
                            """),
                    Map.entry("yearFrom.choose", """
                            Step 8/8 \u00B7 Year
                            Choose the minimum year.
                            """),

                    Map.entry("brand.chooseAtLeastOne", "Choose at least one brand or press \u201CAny\u201D."),
                    Map.entry("brand.selected", "Selected brands:"),
                    Map.entry("summary.settings", "Your filter:"),
                    Map.entry("summary.currentFilter", "Current filter"),

                    Map.entry("label.carType", "Body type"),
                    Map.entry("label.brand", "Brand"),
                    Map.entry("label.maxPrice", "Max price"),
                    Map.entry("label.location", "Region"),
                    Map.entry("label.price", "Price"),
                    Map.entry("label.source", "Source"),
                    Map.entry("label.year", "Year"),
                    Map.entry("label.mileage", "Mileage"),
                    Map.entry("label.maxMileage", "Max mileage"),
                    Map.entry("label.transmission", "Transmission"),
                    Map.entry("label.fuelType", "Fuel"),
                    Map.entry("label.yearFrom", "Year from"),

                    Map.entry("car.open", "Open listing"),
                    Map.entry("common.any", "Any"),
                    Map.entry("common.noLimit", "no limit"),
                    Map.entry("common.notImportant", "not important"),
                    Map.entry("common.done", "Done"),

                    Map.entry("carType.SEDAN", "Sedan"),
                    Map.entry("carType.HATCHBACK", "Hatchback"),
                    Map.entry("carType.WAGON", "Wagon"),
                    Map.entry("carType.SUV", "SUV"),
                    Map.entry("carType.MINIVAN", "Minivan"),

                    Map.entry("brand.SKODA", "Škoda"),
                    Map.entry("brand.VOLKSWAGEN", "Volkswagen"),
                    Map.entry("brand.AUDI", "Audi"),
                    Map.entry("brand.BMW", "BMW"),
                    Map.entry("brand.MERCEDES", "Mercedes"),
                    Map.entry("brand.TOYOTA", "Toyota"),
                    Map.entry("brand.FORD", "Ford"),
                    Map.entry("brand.RENAULT", "Renault"),
                    Map.entry("brand.HYUNDAI", "Hyundai"),
                    Map.entry("brand.KIA", "Kia"),
                    Map.entry("brand.PEUGEOT", "Peugeot"),
                    Map.entry("brand.CITROEN", "Citroën"),
                    Map.entry("brand.OPEL", "Opel"),
                    Map.entry("brand.MAZDA", "Mazda"),
                    Map.entry("brand.HONDA", "Honda"),
                    Map.entry("brand.VOLVO", "Volvo"),
                    Map.entry("brand.SEAT", "Seat"),
                    Map.entry("brand.DACIA", "Dacia"),
                    Map.entry("brand.FIAT", "Fiat"),
                    Map.entry("brand.TESLA", "Tesla"),
                    Map.entry("brand.CUPRA", "Cupra"),
                    Map.entry("brand.LEXUS", "Lexus"),
                    Map.entry("brand.BYD", "BYD"),
                    Map.entry("brand.NISSAN", "Nissan"),
                    Map.entry("brand.SUZUKI", "Suzuki"),
                    Map.entry("brand.JEEP", "Jeep"),
                    Map.entry("brand.MINI", "Mini"),
                    Map.entry("brand.PORSCHE", "Porsche"),
                    Map.entry("brand.MITSUBISHI", "Mitsubishi"),
                    Map.entry("brand.SUBARU", "Subaru"),
                    Map.entry("brand.DODGE", "Dodge"),
                    Map.entry("brand.MG", "MG"),
                    Map.entry("brand.LAND_ROVER", "Land Rover"),
                    Map.entry("brand.ALFA_ROMEO", "Alfa Romeo"),
                    Map.entry("brand.DS", "DS"),
                    Map.entry("brand.CHEVROLET", "Chevrolet"),

                    Map.entry("transmission.MANUAL", "Manual"),
                    Map.entry("transmission.AUTOMATIC", "Automatic"),

                    Map.entry("fuelType.PETROL", "Petrol"),
                    Map.entry("fuelType.DIESEL", "Diesel"),
                    Map.entry("fuelType.HYBRID", "Hybrid"),
                    Map.entry("fuelType.PLUGIN_HYBRID", "Plug-in hybrid"),
                    Map.entry("fuelType.ELECTRIC", "Electric"),
                    Map.entry("fuelType.LPG", "LPG"),
                    Map.entry("fuelType.CNG", "CNG"),

                    Map.entry("location.PRAHA", "Prague"),
                    Map.entry("location.STREDOCESKY", "Central Bohemian"),
                    Map.entry("location.JIHOMORAVSKY", "South Moravian"),
                    Map.entry("location.MORAVSKOSLEZSKY", "Moravian-Silesian"),
                    Map.entry("location.USTECKY", "Ústí Region"),
                    Map.entry("location.PLZENSKY", "Plzeň Region"),
                    Map.entry("location.JIHOCESKY", "South Bohemian"),
                    Map.entry("location.KRALOVEHRADECKY", "Hradec Králové"),
                    Map.entry("location.LIBERECKY", "Liberec Region"),
                    Map.entry("location.OLOMOUCKY", "Olomouc Region"),
                    Map.entry("location.PARDUBICKY", "Pardubice Region"),
                    Map.entry("location.ZLINSKY", "Zlín Region"),
                    Map.entry("location.VYSOCINA", "Vysočina"),
                    Map.entry("location.KARLOVARSKY", "Karlovy Vary")
            )
    );

    public String get(String language, String key) {
        Map<String, String> langMap = messages.getOrDefault(language, messages.get("en"));
        return langMap.getOrDefault(key, messages.get("en").getOrDefault(key, key));
    }

    public String getOrDefault(String language, String key, String fallback) {
        Map<String, String> langMap = messages.getOrDefault(language, messages.get("en"));
        return langMap.getOrDefault(key, fallback);
    }
}
package com.yourapp.carbot.i18n;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MessageService {

    private final Map<String, Map<String, String>> messages = Map.of(
            "ru", Map.ofEntries(
                    Map.entry("start.title", """
                            🚗 Привет!

                            Помогу настроить поиск авто по Чехии.
                            Это займёт меньше минуты.

                            Шаг 1/8 · Тип кузова
                            Можно выбрать несколько вариантов.
                            """),
                    Map.entry("start.welcomeBack", """
                            С возвращением! 👋

                            Ваш фильтр уже настроен.
                            """),

                    Map.entry("carType.choose", """
                            Шаг 1/8 · Тип кузова
                            Можно выбрать несколько вариантов.
                            """),
                    Map.entry("carType.selected", "Выбраны типы кузова:"),
                    Map.entry("carType.chooseAtLeastOne", "Выберите хотя бы один тип кузова или нажмите «Любой»."),

                    Map.entry("filter.carType.saved", "✅ Тип кузова сохранён"),
                    Map.entry("filter.brand.saved", "✅ Марка сохранена"),
                    Map.entry("filter.price.saved", "✅ Максимальная цена сохранена"),
                    Map.entry("filter.location.saved", "✅ Регион поиска сохранён"),
                    Map.entry("filter.mileage.saved", "✅ Максимальный пробег сохранён"),
                    Map.entry("filter.transmission.saved", "✅ Тип коробки передач сохранён"),
                    Map.entry("filter.fuelType.saved", "✅ Тип топлива сохранён"),
                    Map.entry("filter.saved", "✅ Фильтр сохранён"),
                    Map.entry("filter.saved.next", """
                            ✅ Фильтр сохранён.

                            Что хотите сделать дальше?
                            """),

                    Map.entry("help.text", """
                            ℹ️ Что умеет бот:
                    
                            /filter — настроить фильтр поиска
                            /myfilter — показать текущий фильтр
                            /find — найти подходящие авто
                            /latest — показать новые объявления
                            /favorites — открыть избранное
                            /language — сменить язык
                            /resetfilter — сбросить фильтр
                            /start — начать заново
                    
                            Новые объявления будут приходить автоматически.
                            """),

                    Map.entry("command.unknown", """
                            Я не понял эту команду.

                            Доступные команды:
                            /start
                            /filter
                            /myfilter
                            /resetfilter
                            /latest
                            /find
                            /favorites
                            /language
                            /help
                            """),

                    Map.entry("menu.ready", """
                            🚗 Бот готов к работе.

                            Я буду присылать новые объявления по вашему фильтру.
                            Можно начать поиск прямо сейчас 👇
                            """),
                    Map.entry("menu.search", "Найти авто"),
                    Map.entry("menu.filter", "Настроить фильтр"),
                    Map.entry("menu.myFilter", "Мой фильтр"),
                    Map.entry("menu.latest", "Новые объявления"),
                    Map.entry("menu.favorites", "Избранное"),
                    Map.entry("menu.language", "Язык"),

                    Map.entry("button.showFilter", "Показать фильтр"),
                    Map.entry("button.findCars", "Найти авто"),
                    Map.entry("button.editFilter", "Изменить фильтр"),
                    Map.entry("button.resetFilter", "Сбросить фильтр"),
                    Map.entry("button.createNewFilter", "Создать новый фильтр"),
                    Map.entry("button.open", "Открыть объявление"),
                    Map.entry("button.addFavorite", "Добавить в избранное"),
                    Map.entry("button.removeFavorite", "Удалить из избранного"),
                    Map.entry("button.prev", "Назад"),
                    Map.entry("button.next", "Далее"),
                    Map.entry("button.restart", "Новый поиск"),
                    Map.entry("button.stop", "В меню"),
                    Map.entry("button.skip", "Пропустить"),
                    Map.entry("button.backToMenu", "В меню"),
                    Map.entry("button.newSearch", "Новый поиск"),

                    Map.entry("filter.notConfigured", """
                            Фильтр пока не настроен.

                            Нажмите /filter и я помогу настроить поиск шаг за шагом.
                            """),
                    Map.entry("filter.reset", """
                            ♻️ Фильтр сброшен.

                            Давайте настроим новый.
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
                            Шаг 2/8 · Марка
                            Можно выбрать несколько вариантов.
                            """),
                    Map.entry("price.choose", """
                            Шаг 3/8 · Бюджет
                            Выберите максимальную цену.
                            """),
                    Map.entry("location.choose", """
                            Шаг 4/8 · Регион
                            Выберите регион поиска.
                            """),
                    Map.entry("mileage.choose", """
                            Шаг 5/8 · Пробег
                            Выберите максимальный пробег.
                            """),
                    Map.entry("transmission.choose", """
                            Шаг 6/8 · Коробка
                            Выберите тип коробки передач.
                            """),
                    Map.entry("fuelType.choose", """
                            Шаг 7/8 · Топливо
                            Выберите тип топлива.
                            """),
                    Map.entry("yearFrom.choose", """
                            Шаг 8/8 · Год выпуска
                            Выберите минимальный год.
                            """),

                    Map.entry("brand.chooseAtLeastOne", "Выберите хотя бы одну марку или нажмите «Любая»."),
                    Map.entry("brand.selected", "Выбраны марки:"),
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
                    Map.entry("start.title", """
                            🚗 Привіт!

                            Допоможу налаштувати пошук авто по Чехії.
                            Це займе менше хвилини.

                            Крок 1/8 · Тип кузова
                            Можна вибрати кілька варіантів.
                            """),
                    Map.entry("start.welcomeBack", """
                            З поверненням! 👋

                            Ваш фільтр уже налаштований.
                            """),

                    Map.entry("carType.choose", """
                            Крок 1/8 · Тип кузова
                            Можна вибрати кілька варіантів.
                            """),
                    Map.entry("carType.selected", "Обрані типи кузова:"),
                    Map.entry("carType.chooseAtLeastOne", "Оберіть хоча б один тип кузова або натисніть «Будь-який»."),

                    Map.entry("filter.carType.saved", "✅ Тип кузова збережено"),
                    Map.entry("filter.brand.saved", "✅ Марку збережено"),
                    Map.entry("filter.price.saved", "✅ Максимальну ціну збережено"),
                    Map.entry("filter.location.saved", "✅ Регіон пошуку збережено"),
                    Map.entry("filter.mileage.saved", "✅ Максимальний пробіг збережено"),
                    Map.entry("filter.transmission.saved", "✅ Тип коробки передач збережено"),
                    Map.entry("filter.fuelType.saved", "✅ Тип пального збережено"),
                    Map.entry("filter.saved", "✅ Фільтр збережено"),
                    Map.entry("filter.saved.next", """
                            ✅ Фільтр збережено.

                            Що хочете зробити далі?
                            """),

                    Map.entry("help.text", """
                            ℹ️ Що вміє бот:
                    
                            /filter — налаштувати фільтр пошуку
                            /myfilter — показати поточний фільтр
                            /find — знайти відповідні авто
                            /latest — показати нові оголошення
                            /favorites — відкрити обране
                            /language — змінити мову
                            /resetfilter — скинути фільтр
                            /start — почати заново
                    
                            Нові відповідні оголошення приходитимуть автоматично.
                            """),

                    Map.entry("command.unknown", """
                            Я не зрозумів цю команду.

                            Доступні команди:
                            /start
                            /filter
                            /myfilter
                            /resetfilter
                            /latest
                            /find
                            /favorites
                            /language
                            /help
                            """),

                    Map.entry("menu.ready", """
                            🚗 Бот готовий до роботи.

                            Я надсилатиму нові оголошення за вашим фільтром.
                            Можна почати пошук прямо зараз 👇
                            """),
                    Map.entry("menu.search", "Знайти авто"),
                    Map.entry("menu.filter", "Налаштувати фільтр"),
                    Map.entry("menu.myFilter", "Мій фільтр"),
                    Map.entry("menu.latest", "Нові оголошення"),
                    Map.entry("menu.favorites", "Обране"),
                    Map.entry("menu.language", "Мова"),

                    Map.entry("button.showFilter", "Показати фільтр"),
                    Map.entry("button.findCars", "Знайти авто"),
                    Map.entry("button.editFilter", "Змінити фільтр"),
                    Map.entry("button.resetFilter", "Скинути фільтр"),
                    Map.entry("button.createNewFilter", "Створити новий фільтр"),
                    Map.entry("button.open", "Відкрити оголошення"),
                    Map.entry("button.addFavorite", "Додати в обране"),
                    Map.entry("button.removeFavorite", "Видалити з обраного"),
                    Map.entry("button.prev", "Назад"),
                    Map.entry("button.next", "Далі"),
                    Map.entry("button.restart", "Новий пошук"),
                    Map.entry("button.stop", "У меню"),
                    Map.entry("button.skip", "Пропустити"),
                    Map.entry("button.backToMenu", "У меню"),
                    Map.entry("button.newSearch", "Новий пошук"),

                    Map.entry("filter.notConfigured", """
                            Фільтр ще не налаштований.

                            Натисніть /filter і я допоможу налаштувати пошук крок за кроком.
                            """),
                    Map.entry("filter.reset", """
                            ♻️ Фільтр скинуто.

                            Давайте налаштуємо новий.
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
                            Крок 2/8 · Марка
                            Можна вибрати кілька варіантів.
                            """),
                    Map.entry("price.choose", """
                            Крок 3/8 · Бюджет
                            Оберіть максимальну ціну.
                            """),
                    Map.entry("location.choose", """
                            Крок 4/8 · Регіон
                            Оберіть регіон пошуку.
                            """),
                    Map.entry("mileage.choose", """
                            Крок 5/8 · Пробіг
                            Оберіть максимальний пробіг.
                            """),
                    Map.entry("transmission.choose", """
                            Крок 6/8 · Коробка
                            Оберіть тип коробки передач.
                            """),
                    Map.entry("fuelType.choose", """
                            Крок 7/8 · Пальне
                            Оберіть тип пального.
                            """),
                    Map.entry("yearFrom.choose", """
                            Крок 8/8 · Рік випуску
                            Оберіть мінімальний рік.
                            """),

                    Map.entry("brand.chooseAtLeastOne", "Оберіть хоча б одну марку або натисніть «Будь-яка»."),
                    Map.entry("brand.selected", "Обрані марки:"),
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
                    Map.entry("start.title", """
                            🚗 Ahoj!

                            Pomohu nastavit hledání auta po Česku.
                            Zabere to méně než minutu.

                            Krok 1/8 · Karoserie
                            Můžete vybrat více možností.
                            """),
                    Map.entry("start.welcomeBack", """
                            Vítejte zpět! 👋

                            Váš filtr je už nastaven.
                            """),

                    Map.entry("carType.choose", """
                            Krok 1/8 · Karoserie
                            Můžete vybrat více možností.
                            """),
                    Map.entry("carType.selected", "Vybrané typy karoserie:"),
                    Map.entry("carType.chooseAtLeastOne", "Vyberte alespoň jeden typ karoserie nebo stiskněte „Libovolný“."),

                    Map.entry("filter.carType.saved", "✅ Typ karoserie byl uložen"),
                    Map.entry("filter.brand.saved", "✅ Značka byla uložena"),
                    Map.entry("filter.price.saved", "✅ Maximální cena byla uložena"),
                    Map.entry("filter.location.saved", "✅ Region hledání byl uložen"),
                    Map.entry("filter.mileage.saved", "✅ Maximální nájezd byl uložen"),
                    Map.entry("filter.transmission.saved", "✅ Typ převodovky byl uložen"),
                    Map.entry("filter.fuelType.saved", "✅ Typ paliva byl uložen"),
                    Map.entry("filter.saved", "✅ Filtr byl uložen"),
                    Map.entry("filter.saved.next", """
                            ✅ Filtr byl uložen.

                            Co chcete udělat dál?
                            """),

                    Map.entry("help.text", """
                            ℹ️ Co bot umí:
                    
                            /filter — nastavit filtr hledání
                            /myfilter — zobrazit aktuální filtr
                            /find — najít vhodná auta z databáze
                            /latest — ukázat nové inzeráty
                            /favorites — otevřít oblíbené
                            /language — změnit jazyk
                            /resetfilter — resetovat filtr
                            /start — začít znovu
                    
                            Nové vhodné inzeráty budete dostávat automaticky.
                            """),

                    Map.entry("command.unknown", """
                            Tomuto příkazu nerozumím.

                            Dostupné příkazy:
                            /start
                            /filter
                            /myfilter
                            /resetfilter
                            /latest
                            /find
                            /favorites
                            /language
                            /help
                            """),

                    Map.entry("menu.ready", """
                            🚗 Bot je připraven.

                            Budu posílat nové inzeráty podle vašeho filtru.
                            Můžete začít hledat hned teď 👇
                            """),
                    Map.entry("menu.search", "Najít auto"),
                    Map.entry("menu.filter", "Nastavit filtr"),
                    Map.entry("menu.myFilter", "Můj filtr"),
                    Map.entry("menu.latest", "Nové inzeráty"),
                    Map.entry("menu.favorites", "Oblíbené"),
                    Map.entry("menu.language", "Jazyk"),

                    Map.entry("button.showFilter", "Zobrazit filtr"),
                    Map.entry("button.findCars", "Najít auta"),
                    Map.entry("button.editFilter", "Upravit filtr"),
                    Map.entry("button.resetFilter", "Resetovat filtr"),
                    Map.entry("button.createNewFilter", "Vytvořit nový filtr"),
                    Map.entry("button.open", "Otevřít inzerát"),
                    Map.entry("button.addFavorite", "Přidat do oblíbených"),
                    Map.entry("button.removeFavorite", "Odebrat z oblíbených"),
                    Map.entry("button.prev", "Zpět"),
                    Map.entry("button.next", "Další"),
                    Map.entry("button.restart", "Nové hledání"),
                    Map.entry("button.stop", "Do menu"),
                    Map.entry("button.skip", "Přeskočit"),
                    Map.entry("button.backToMenu", "Do menu"),
                    Map.entry("button.newSearch", "Nové hledání"),

                    Map.entry("filter.notConfigured", """
                            Filtr ještě není nastaven.

                            Stiskněte /filter a nastavíme hledání krok za krokem.
                            """),
                    Map.entry("filter.reset", """
                            ♻️ Filtr byl resetován.

                            Nastavíme nový.
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
                            Krok 2/8 · Značka
                            Můžete vybrat více možností.
                            """),
                    Map.entry("price.choose", """
                            Krok 3/8 · Rozpočet
                            Vyberte maximální cenu.
                            """),
                    Map.entry("location.choose", """
                            Krok 4/8 · Region
                            Vyberte region hledání.
                            """),
                    Map.entry("mileage.choose", """
                            Krok 5/8 · Nájezd
                            Vyberte maximální nájezd.
                            """),
                    Map.entry("transmission.choose", """
                            Krok 6/8 · Převodovka
                            Vyberte typ převodovky.
                            """),
                    Map.entry("fuelType.choose", """
                            Krok 7/8 · Palivo
                            Vyberte typ paliva.
                            """),
                    Map.entry("yearFrom.choose", """
                            Krok 8/8 · Rok výroby
                            Vyberte minimální rok.
                            """),

                    Map.entry("brand.chooseAtLeastOne", "Vyberte alespoň jednu značku nebo stiskněte „Libovolná“."),
                    Map.entry("brand.selected", "Vybrané značky:"),
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
                    Map.entry("start.title", """
                            🚗 Hi!

                            I will help you set up car search in the Czech Republic.
                            It will take less than a minute.

                            Step 1/8 · Body type
                            You can choose multiple options.
                            """),
                    Map.entry("start.welcomeBack", """
                            Welcome back! 👋

                            Your filter is already configured.
                            """),

                    Map.entry("carType.choose", """
                            Step 1/8 · Body type
                            You can choose multiple options.
                            """),
                    Map.entry("carType.selected", "Selected body types:"),
                    Map.entry("carType.chooseAtLeastOne", "Choose at least one body type or press “Any”."),

                    Map.entry("filter.carType.saved", "✅ Body type saved"),
                    Map.entry("filter.brand.saved", "✅ Brand saved"),
                    Map.entry("filter.price.saved", "✅ Maximum price saved"),
                    Map.entry("filter.location.saved", "✅ Search region saved"),
                    Map.entry("filter.mileage.saved", "✅ Maximum mileage saved"),
                    Map.entry("filter.transmission.saved", "✅ Transmission type saved"),
                    Map.entry("filter.fuelType.saved", "✅ Fuel type saved"),
                    Map.entry("filter.saved", "✅ Filter saved"),
                    Map.entry("filter.saved.next", """
                            ✅ Filter saved.

                            What would you like to do next?
                            """),

                    Map.entry("help.text", """
                            ℹ️ What this bot can do:
                    
                            /filter — set up your search filter
                            /myfilter — show current filter
                            /find — find matching cars from database
                            /latest — show new listings
                            /favorites — open favorites
                            /language — change language
                            /resetfilter — reset filter
                            /start — start again
                    
                            New matching listings will be sent automatically.
                            """),

                    Map.entry("command.unknown", """
                            I didn't understand that command.

                            Available commands:
                            /start
                            /filter
                            /myfilter
                            /resetfilter
                            /latest
                            /find
                            /favorites
                            /language
                            /help
                            """),

                    Map.entry("menu.ready", """
                            🚗 Bot is ready.

                            I will send new listings matching your filter.
                            You can start searching right now 👇
                            """),
                    Map.entry("menu.search", "Find cars"),
                    Map.entry("menu.filter", "Set filter"),
                    Map.entry("menu.myFilter", "My filter"),
                    Map.entry("menu.latest", "New listings"),
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
                            ♻️ Filter has been reset.

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
                            Step 2/8 · Brand
                            You can choose multiple options.
                            """),
                    Map.entry("price.choose", """
                            Step 3/8 · Budget
                            Choose the maximum price.
                            """),
                    Map.entry("location.choose", """
                            Step 4/8 · Region
                            Choose the search region.
                            """),
                    Map.entry("mileage.choose", """
                            Step 5/8 · Mileage
                            Choose the maximum mileage.
                            """),
                    Map.entry("transmission.choose", """
                            Step 6/8 · Transmission
                            Choose the transmission type.
                            """),
                    Map.entry("fuelType.choose", """
                            Step 7/8 · Fuel
                            Choose the fuel type.
                            """),
                    Map.entry("yearFrom.choose", """
                            Step 8/8 · Year
                            Choose the minimum year.
                            """),

                    Map.entry("brand.chooseAtLeastOne", "Choose at least one brand or press “Any”."),
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
        return langMap.getOrDefault(key, key);
    }

    public String getOrDefault(String language, String key, String fallback) {
        Map<String, String> langMap = messages.getOrDefault(language, messages.get("en"));
        return langMap.getOrDefault(key, fallback);
    }
}
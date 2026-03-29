package com.yourapp.carbot.i18n;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MessageService {

    private final Map<String, Map<String, String>> messages = Map.of(
            "ru", Map.ofEntries(
                    Map.entry("start.title", """
                            🚗 Привет!

                            Я помогу настроить фильтр для поиска авто.

                            Шаг 1/7
                            Выберите тип авто:
                            """),
                    Map.entry("filter.carType.saved", """
                            ✅ Тип авто сохранён

                            Шаг 2/7
                            Выберите марку:
                            """),
                    Map.entry("filter.brand.saved", """
                            ✅ Марка сохранена

                            Шаг 3/7
                            Выберите максимальную цену:
                            """),
                    Map.entry("filter.price.saved", """
                            ✅ Цена сохранена

                            Шаг 4/7
                            Выберите регион:
                            """),
                    Map.entry("filter.location.saved", """
                            ✅ Регион сохранён

                            Шаг 5/7
                            Выберите максимальный пробег:
                            """),
                    Map.entry("filter.mileage.saved", """
                            ✅ Пробег сохранён

                            Шаг 6/7
                            Выберите коробку передач:
                            """),
                    Map.entry("filter.transmission.saved", """
                            ✅ Коробка сохранена

                            Шаг 7/7
                            Выберите минимальный год:
                            """),
                    Map.entry("filter.saved", "✅ Фильтр сохранён"),
                    Map.entry("help.text", """
                            Команды:

                            /start — начать и подписаться
                            /filter — настроить фильтр заново
                            /myfilter — показать текущий фильтр
                            /resetfilter — сбросить фильтр
                            /latest — последние 5 машин
                            /find — показать подходящие машины из базы
                            /favorites — показать избранное
                            /language — сменить язык
                            /help — помощь
                            """),
                    Map.entry("command.unknown", """
                            Не понимаю команду.

                            Доступно:
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
                    Map.entry("menu.ready", "Главное меню готово 👇"),
                    Map.entry("filter.notConfigured", "Фильтр пока не настроен. Напишите /filter"),
                    Map.entry("filter.reset", """
                            Фильтр сброшен.

                            Напишите /filter, чтобы настроить заново.
                            """),
                    Map.entry("cars.empty", "В базе пока нет объявлений."),
                    Map.entry("cars.noMatches", "По вашему фильтру пока ничего не найдено."),
                    Map.entry("cars.matchesFound", "Найдено объявлений:"),
                    Map.entry("cars.noMatches.pretty", """
                            😕 По вашему фильтру пока ничего не найдено.

                            Попробуйте:
                            • увеличить максимальную цену
                            • убрать ограничение по пробегу
                            • выбрать любой регион
                            """),
                    Map.entry("cars.noMore", "Больше объявлений нет."),
                    Map.entry("cars.morePrompt", "Показать ещё подходящие объявления?"),
                    Map.entry("cars.searchFinished", "Поиск завершён."),
                    Map.entry("favorites.added", "⭐ Объявление добавлено в избранное."),
                    Map.entry("favorites.alreadyExists", "Это объявление уже в избранном."),
                    Map.entry("favorites.error", "Не удалось добавить в избранное."),
                    Map.entry("favorites.empty", "В избранном пока ничего нет."),
                    Map.entry("favorites.title", "Избранных объявлений:"),
                    Map.entry("favorites.removed", "🗑 Объявление удалено из избранного."),
                    Map.entry("favorites.notFound", "Объявление не найдено в избранном."),
                    Map.entry("favorites.removeError", "Не удалось удалить из избранного."),
                    Map.entry("language.choose", "Выберите язык:"),
                    Map.entry("language.changed", "✅ Язык изменён."),
                    Map.entry("language.nextStep", "Что хотите сделать дальше?"),
                    Map.entry("summary.settings", "Ваши настройки:"),
                    Map.entry("summary.commands", "Команды:"),
                    Map.entry("label.carType", "Тип авто"),
                    Map.entry("label.brand", "Марка"),
                    Map.entry("label.maxPrice", "Макс. цена"),
                    Map.entry("label.location", "Регион"),
                    Map.entry("label.price", "Цена"),
                    Map.entry("label.source", "Источник"),
                    Map.entry("label.year", "Год"),
                    Map.entry("label.mileage", "Пробег"),
                    Map.entry("label.maxMileage", "Макс. пробег"),
                    Map.entry("label.transmission", "Коробка"),
                    Map.entry("label.yearFrom", "Год от"),
                    Map.entry("car.open", "Открыть объявление"),
                    Map.entry("common.any", "Любой"),
                    Map.entry("common.noLimit", "без ограничения"),
                    Map.entry("common.notImportant", "не важно"),
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
                    Map.entry("transmission.MANUAL", "Механика"),
                    Map.entry("transmission.AUTOMATIC", "Автомат"),
                    Map.entry("location.PRAHA", "Praha"),
                    Map.entry("location.STREDOCESKY", "Středočeský kraj"),
                    Map.entry("location.JIHOMORAVSKY", "Jihomoravský kraj"),
                    Map.entry("location.MORAVSKOSLEZSKY", "Moravskoslezský kraj"),
                    Map.entry("location.USTECKY", "Ústecký kraj"),
                    Map.entry("location.PLZENSKY", "Plzeňský kraj"),
                    Map.entry("location.JIHOCESKY", "Jihočeský kraj"),
                    Map.entry("location.KRALOVEHRADECKY", "Královéhradecký kraj"),
                    Map.entry("location.LIBERECKY", "Liberecký kraj"),
                    Map.entry("location.OLOMOUCKY", "Olomoucký kraj"),
                    Map.entry("location.PARDUBICKY", "Pardubický kraj"),
                    Map.entry("location.ZLINSKY", "Zlínský kraj"),
                    Map.entry("location.VYSOCINA", "Vysočina"),
                    Map.entry("location.KARLOVARSKY", "Karlovarský kraj")
            ),

            "uk", Map.ofEntries(
                    Map.entry("start.title", """
                            🚗 Привіт!

                            Я допоможу налаштувати фільтр для пошуку авто.

                            Крок 1/7
                            Обери тип авто:
                            """),
                    Map.entry("filter.carType.saved", """
                            ✅ Тип авто збережено

                            Крок 2/7
                            Обери марку:
                            """),
                    Map.entry("filter.brand.saved", """
                            ✅ Марку збережено

                            Крок 3/7
                            Обери максимальну ціну:
                            """),
                    Map.entry("filter.price.saved", """
                            ✅ Ціну збережено

                            Крок 4/7
                            Обери регіон:
                            """),
                    Map.entry("filter.location.saved", """
                            ✅ Регіон збережено

                            Крок 5/7
                            Обери максимальний пробіг:
                            """),
                    Map.entry("filter.mileage.saved", """
                            ✅ Пробіг збережено

                            Крок 6/7
                            Обери коробку передач:
                            """),
                    Map.entry("filter.transmission.saved", """
                            ✅ Коробку збережено

                            Крок 7/7
                            Обери мінімальний рік:
                            """),
                    Map.entry("filter.saved", "✅ Фільтр збережено"),
                    Map.entry("help.text", """
                            Команди:

                            /start — почати і підписатися
                            /filter — налаштувати фільтр заново
                            /myfilter — показати поточний фільтр
                            /resetfilter — скинути фільтр
                            /latest — останні 5 авто
                            /find — показати відповідні авто з бази
                            /favorites — показати обране
                            /language — змінити мову
                            /help — допомога
                            """),
                    Map.entry("command.unknown", """
                            Не розумію команду.

                            Доступно:
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
                    Map.entry("menu.ready", "Головне меню готове 👇"),
                    Map.entry("filter.notConfigured", "Фільтр ще не налаштований. Напиши /filter"),
                    Map.entry("filter.reset", """
                            Фільтр скинуто.

                            Напиши /filter, щоб налаштувати заново.
                            """),
                    Map.entry("cars.empty", "У базі поки немає оголошень."),
                    Map.entry("cars.noMatches", "За вашим фільтром поки нічого не знайдено."),
                    Map.entry("cars.matchesFound", "Знайдено оголошень:"),
                    Map.entry("cars.noMatches.pretty", """
                            😕 За вашим фільтром поки нічого не знайдено.

                            Спробуйте:
                            • збільшити максимальну ціну
                            • прибрати обмеження по пробігу
                            • вибрати будь-який регіон
                            """),
                    Map.entry("cars.noMore", "Більше оголошень немає."),
                    Map.entry("cars.morePrompt", "Показати ще відповідні оголошення?"),
                    Map.entry("cars.searchFinished", "Пошук завершено."),
                    Map.entry("favorites.added", "⭐ Оголошення додано в обране."),
                    Map.entry("favorites.alreadyExists", "Це оголошення вже в обраному."),
                    Map.entry("favorites.error", "Не вдалося додати в обране."),
                    Map.entry("favorites.empty", "В обраному поки нічого немає."),
                    Map.entry("favorites.title", "Обраних оголошень:"),
                    Map.entry("favorites.removed", "🗑 Оголошення видалено з обраного."),
                    Map.entry("favorites.notFound", "Оголошення не знайдено в обраному."),
                    Map.entry("favorites.removeError", "Не вдалося видалити з обраного."),
                    Map.entry("language.choose", "Оберіть мову:"),
                    Map.entry("language.changed", "✅ Мову змінено."),
                    Map.entry("language.nextStep", "Що хочете зробити далі?"),
                    Map.entry("summary.settings", "Ваші налаштування:"),
                    Map.entry("summary.commands", "Команди:"),
                    Map.entry("label.carType", "Тип авто"),
                    Map.entry("label.brand", "Марка"),
                    Map.entry("label.maxPrice", "Макс. ціна"),
                    Map.entry("label.location", "Регіон"),
                    Map.entry("label.price", "Ціна"),
                    Map.entry("label.source", "Джерело"),
                    Map.entry("label.year", "Рік"),
                    Map.entry("label.mileage", "Пробіг"),
                    Map.entry("label.maxMileage", "Макс. пробіг"),
                    Map.entry("label.transmission", "Коробка"),
                    Map.entry("label.yearFrom", "Рік від"),
                    Map.entry("car.open", "Відкрити оголошення"),
                    Map.entry("common.any", "Будь-який"),
                    Map.entry("common.noLimit", "без обмеження"),
                    Map.entry("common.notImportant", "не важливо"),
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
                    Map.entry("transmission.MANUAL", "Механіка"),
                    Map.entry("transmission.AUTOMATIC", "Автомат"),
                    Map.entry("location.PRAHA", "Praha"),
                    Map.entry("location.STREDOCESKY", "Středočeský kraj"),
                    Map.entry("location.JIHOMORAVSKY", "Jihomoravský kraj"),
                    Map.entry("location.MORAVSKOSLEZSKY", "Moravskoslezský kraj"),
                    Map.entry("location.USTECKY", "Ústecký kraj"),
                    Map.entry("location.PLZENSKY", "Plzeňský kraj"),
                    Map.entry("location.JIHOCESKY", "Jihočeský kraj"),
                    Map.entry("location.KRALOVEHRADECKY", "Královéhradecký kraj"),
                    Map.entry("location.LIBERECKY", "Liberecký kraj"),
                    Map.entry("location.OLOMOUCKY", "Olomoucký kraj"),
                    Map.entry("location.PARDUBICKY", "Pardubický kraj"),
                    Map.entry("location.ZLINSKY", "Zlínský kraj"),
                    Map.entry("location.VYSOCINA", "Vysočina"),
                    Map.entry("location.KARLOVARSKY", "Karlovarský kraj")
            ),

            "cs", Map.ofEntries(
                    Map.entry("start.title", """
                            🚗 Ahoj!

                            Pomohu ti nastavit filtr pro hledání auta.

                            Krok 1/7
                            Vyber typ auta:
                            """),
                    Map.entry("filter.carType.saved", """
                            ✅ Typ auta byl uložen

                            Krok 2/7
                            Vyber značku:
                            """),
                    Map.entry("filter.brand.saved", """
                            ✅ Značka byla uložena

                            Krok 3/7
                            Vyber maximální cenu:
                            """),
                    Map.entry("filter.price.saved", """
                            ✅ Cena byla uložena

                            Krok 4/7
                            Vyber kraj:
                            """),
                    Map.entry("filter.location.saved", """
                            ✅ Kraj byl uložen

                            Krok 5/7
                            Vyber maximální nájezd:
                            """),
                    Map.entry("filter.mileage.saved", """
                            ✅ Nájezd byl uložen

                            Krok 6/7
                            Vyber převodovku:
                            """),
                    Map.entry("filter.transmission.saved", """
                            ✅ Převodovka byla uložena

                            Krok 7/7
                            Vyber minimální rok:
                            """),
                    Map.entry("filter.saved", "✅ Filtr byl uložen"),
                    Map.entry("help.text", """
                            Příkazy:

                            /start — začít a přihlásit se
                            /filter — nastavit filtr znovu
                            /myfilter — zobrazit aktuální filtr
                            /resetfilter — resetovat filtr
                            /latest — posledních 5 aut
                            /find — zobrazit vhodná auta z databáze
                            /favorites — zobrazit oblíbené
                            /language — změnit jazyk
                            /help — pomoc
                            """),
                    Map.entry("command.unknown", """
                            Nerozumím příkazu.

                            Dostupné:
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
                    Map.entry("menu.ready", "Hlavní menu je připravené 👇"),
                    Map.entry("filter.notConfigured", "Filtr ještě není nastaven. Napiš /filter"),
                    Map.entry("filter.reset", """
                            Filtr byl resetován.

                            Napiš /filter pro nové nastavení.
                            """),
                    Map.entry("cars.empty", "V databázi zatím nejsou žádné inzeráty."),
                    Map.entry("cars.noMatches", "Pro váš filtr zatím nebyly nalezeny žádné inzeráty."),
                    Map.entry("cars.matchesFound", "Nalezeno inzerátů:"),
                    Map.entry("cars.noMatches.pretty", """
                            😕 Pro váš filtr zatím nebyly nalezeny žádné výsledky.

                            Zkuste:
                            • zvýšit maximální cenu
                            • zrušit omezení nájezdu
                            • vybrat libovolný region
                            """),
                    Map.entry("cars.noMore", "Další inzeráty už nejsou."),
                    Map.entry("cars.morePrompt", "Zobrazit další vhodné inzeráty?"),
                    Map.entry("cars.searchFinished", "Hledání bylo ukončeno."),
                    Map.entry("favorites.added", "⭐ Inzerát byl přidán do oblíbených."),
                    Map.entry("favorites.alreadyExists", "Tento inzerát už je v oblíbených."),
                    Map.entry("favorites.error", "Nepodařilo se přidat do oblíbených."),
                    Map.entry("favorites.empty", "V oblíbených zatím nic není."),
                    Map.entry("favorites.title", "Oblíbených inzerátů:"),
                    Map.entry("favorites.removed", "🗑 Inzerát byl odebrán z oblíbených."),
                    Map.entry("favorites.notFound", "Inzerát nebyl v oblíbených nalezen."),
                    Map.entry("favorites.removeError", "Nepodařilo se odebrat z oblíbených."),
                    Map.entry("language.choose", "Vyber jazyk:"),
                    Map.entry("language.changed", "✅ Jazyk byl změněn."),
                    Map.entry("language.nextStep", "Co chcete udělat dál?"),
                    Map.entry("summary.settings", "Vaše nastavení:"),
                    Map.entry("summary.commands", "Příkazy:"),
                    Map.entry("label.carType", "Typ auta"),
                    Map.entry("label.brand", "Značka"),
                    Map.entry("label.maxPrice", "Max. cena"),
                    Map.entry("label.location", "Kraj"),
                    Map.entry("label.price", "Cena"),
                    Map.entry("label.source", "Zdroj"),
                    Map.entry("label.year", "Rok"),
                    Map.entry("label.mileage", "Nájezd"),
                    Map.entry("label.maxMileage", "Max. nájezd"),
                    Map.entry("label.transmission", "Převodovka"),
                    Map.entry("label.yearFrom", "Rok od"),
                    Map.entry("car.open", "Otevřít inzerát"),
                    Map.entry("common.any", "Libovolný"),
                    Map.entry("common.noLimit", "bez omezení"),
                    Map.entry("common.notImportant", "nezáleží"),
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
                    Map.entry("transmission.MANUAL", "Manuální"),
                    Map.entry("transmission.AUTOMATIC", "Automat"),
                    Map.entry("location.PRAHA", "Praha"),
                    Map.entry("location.STREDOCESKY", "Středočeský kraj"),
                    Map.entry("location.JIHOMORAVSKY", "Jihomoravský kraj"),
                    Map.entry("location.MORAVSKOSLEZSKY", "Moravskoslezský kraj"),
                    Map.entry("location.USTECKY", "Ústecký kraj"),
                    Map.entry("location.PLZENSKY", "Plzeňský kraj"),
                    Map.entry("location.JIHOCESKY", "Jihočeský kraj"),
                    Map.entry("location.KRALOVEHRADECKY", "Královéhradecký kraj"),
                    Map.entry("location.LIBERECKY", "Liberecký kraj"),
                    Map.entry("location.OLOMOUCKY", "Olomoucký kraj"),
                    Map.entry("location.PARDUBICKY", "Pardubický kraj"),
                    Map.entry("location.ZLINSKY", "Zlínský kraj"),
                    Map.entry("location.VYSOCINA", "Vysočina"),
                    Map.entry("location.KARLOVARSKY", "Karlovarský kraj")
            ),

            "en", Map.ofEntries(
                    Map.entry("start.title", """
                            🚗 Hi!

                            I will help you set up a car search filter.

                            Step 1/7
                            Choose car type:
                            """),
                    Map.entry("filter.carType.saved", """
                            ✅ Car type saved

                            Step 2/7
                            Choose brand:
                            """),
                    Map.entry("filter.brand.saved", """
                            ✅ Brand saved

                            Step 3/7
                            Choose max price:
                            """),
                    Map.entry("filter.price.saved", """
                            ✅ Price saved

                            Step 4/7
                            Choose region:
                            """),
                    Map.entry("filter.location.saved", """
                            ✅ Region saved

                            Step 5/7
                            Choose max mileage:
                            """),
                    Map.entry("filter.mileage.saved", """
                            ✅ Mileage saved

                            Step 6/7
                            Choose transmission:
                            """),
                    Map.entry("filter.transmission.saved", """
                            ✅ Transmission saved

                            Step 7/7
                            Choose minimum year:
                            """),
                    Map.entry("filter.saved", "✅ Filter saved"),
                    Map.entry("help.text", """
                            Commands:

                            /start — start and subscribe
                            /filter — set filter again
                            /myfilter — show current filter
                            /resetfilter — reset filter
                            /latest — latest 5 cars
                            /find — show matching cars from database
                            /favorites — show favorites
                            /language — change language
                            /help — help
                            """),
                    Map.entry("command.unknown", """
                            Unknown command.

                            Available:
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
                    Map.entry("menu.ready", "Main menu is ready 👇"),
                    Map.entry("filter.notConfigured", "Filter is not configured yet. Type /filter"),
                    Map.entry("filter.reset", """
                            Filter has been reset.

                            Type /filter to configure it again.
                            """),
                    Map.entry("cars.empty", "No listings in the database yet."),
                    Map.entry("cars.noMatches", "No listings found for your filter yet."),
                    Map.entry("cars.matchesFound", "Listings found:"),
                    Map.entry("cars.noMatches.pretty", """
                            😕 No listings found for your filter yet.

                            Try:
                            • increasing max price
                            • removing mileage limit
                            • choosing any region
                            """),
                    Map.entry("cars.noMore", "No more listings."),
                    Map.entry("cars.morePrompt", "Show more matching listings?"),
                    Map.entry("cars.searchFinished", "Search finished."),
                    Map.entry("favorites.added", "⭐ Listing added to favorites."),
                    Map.entry("favorites.alreadyExists", "This listing is already in favorites."),
                    Map.entry("favorites.error", "Failed to add to favorites."),
                    Map.entry("favorites.empty", "Favorites are empty."),
                    Map.entry("favorites.title", "Favorite listings:"),
                    Map.entry("favorites.removed", "🗑 Listing removed from favorites."),
                    Map.entry("favorites.notFound", "Listing was not found in favorites."),
                    Map.entry("favorites.removeError", "Failed to remove from favorites."),
                    Map.entry("language.choose", "Choose language:"),
                    Map.entry("language.changed", "✅ Language changed."),
                    Map.entry("language.nextStep", "What would you like to do next?"),
                    Map.entry("summary.settings", "Your settings:"),
                    Map.entry("summary.commands", "Commands:"),
                    Map.entry("label.carType", "Car type"),
                    Map.entry("label.brand", "Brand"),
                    Map.entry("label.maxPrice", "Max price"),
                    Map.entry("label.location", "Region"),
                    Map.entry("label.price", "Price"),
                    Map.entry("label.source", "Source"),
                    Map.entry("label.year", "Year"),
                    Map.entry("label.mileage", "Mileage"),
                    Map.entry("label.maxMileage", "Max mileage"),
                    Map.entry("label.transmission", "Transmission"),
                    Map.entry("label.yearFrom", "Year from"),
                    Map.entry("car.open", "Open listing"),
                    Map.entry("common.any", "Any"),
                    Map.entry("common.noLimit", "no limit"),
                    Map.entry("common.notImportant", "not important"),
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
                    Map.entry("transmission.MANUAL", "Manual"),
                    Map.entry("transmission.AUTOMATIC", "Automatic"),
                    Map.entry("location.PRAHA", "Prague"),
                    Map.entry("location.STREDOCESKY", "Central Bohemian Region"),
                    Map.entry("location.JIHOMORAVSKY", "South Moravian Region"),
                    Map.entry("location.MORAVSKOSLEZSKY", "Moravian-Silesian Region"),
                    Map.entry("location.USTECKY", "Ústí nad Labem Region"),
                    Map.entry("location.PLZENSKY", "Plzeň Region"),
                    Map.entry("location.JIHOCESKY", "South Bohemian Region"),
                    Map.entry("location.KRALOVEHRADECKY", "Hradec Králové Region"),
                    Map.entry("location.LIBERECKY", "Liberec Region"),
                    Map.entry("location.OLOMOUCKY", "Olomouc Region"),
                    Map.entry("location.PARDUBICKY", "Pardubice Region"),
                    Map.entry("location.ZLINSKY", "Zlín Region"),
                    Map.entry("location.VYSOCINA", "Vysočina Region"),
                    Map.entry("location.KARLOVARSKY", "Karlovy Vary Region")
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
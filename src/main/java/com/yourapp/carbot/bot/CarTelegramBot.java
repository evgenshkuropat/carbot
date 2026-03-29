package com.yourapp.carbot.bot;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.UserFilterEntity;
import com.yourapp.carbot.i18n.MessageService;
import com.yourapp.carbot.repository.CarRepository;
import com.yourapp.carbot.service.CarSearchService;
import com.yourapp.carbot.service.FavoriteCarService;
import com.yourapp.carbot.service.TelegramSubscriberService;
import com.yourapp.carbot.service.UserFilterService;
import com.yourapp.carbot.service.UserStateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CarTelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final int FIND_PAGE_SIZE = 3;

    private final TelegramClient telegramClient;
    private final String botToken;

    private final TelegramSubscriberService subscriberService;
    private final UserStateService userStateService;
    private final UserFilterService userFilterService;
    private final CarRepository carRepository;
    private final CarSearchService carSearchService;
    private final FavoriteCarService favoriteCarService;
    private final CarBotKeyboardFactory keyboardFactory;
    private final MessageService messages;

    private final Map<Long, Integer> findOffsets = new ConcurrentHashMap<>();

    public CarTelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            TelegramSubscriberService subscriberService,
            UserStateService userStateService,
            UserFilterService userFilterService,
            CarRepository carRepository,
            CarSearchService carSearchService,
            FavoriteCarService favoriteCarService,
            CarBotKeyboardFactory keyboardFactory,
            MessageService messages
    ) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.subscriberService = subscriberService;
        this.userStateService = userStateService;
        this.userFilterService = userFilterService;
        this.carRepository = carRepository;
        this.carSearchService = carSearchService;
        this.favoriteCarService = favoriteCarService;
        this.keyboardFactory = keyboardFactory;
        this.messages = messages;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update);
                return;
            }

            if (!update.hasMessage() || !update.getMessage().hasText()) {
                return;
            }

            String text = update.getMessage().getText().trim();
            Long chatId = update.getMessage().getChatId();

            String username = update.getMessage().getFrom() != null
                    ? update.getMessage().getFrom().getUserName()
                    : null;

            String telegramLanguageCode = update.getMessage().getFrom() != null
                    ? update.getMessage().getFrom().getLanguageCode()
                    : null;

            if (text.startsWith("/")) {
                handleCommand(chatId, username, telegramLanguageCode, text);
                return;
            }

            if (handleMenuButton(chatId, username, telegramLanguageCode, text)) {
                return;
            }

            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "command.unknown"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean handleMenuButton(Long chatId, String username, String telegramLanguageCode, String text) {
        return switch (text) {
            case "🔍 Поиск", "🔍 Пошук", "🔍 Hledat", "🔍 Search" -> {
                handleFind(chatId);
                yield true;
            }
            case "⚙️ Фильтр", "⚙️ Фільтр", "⚙️ Filtr", "⚙️ Filter" -> {
                startFilterSetup(chatId);
                yield true;
            }
            case "📝 Мой фильтр", "📝 Мій фільтр", "📝 Můj filtr", "📝 My filter" -> {
                showCurrentFilter(chatId);
                yield true;
            }
            case "🆕 Последние", "🆕 Останні", "🆕 Nejnovější", "🆕 Latest" -> {
                handleLatest(chatId);
                yield true;
            }
            case "⭐ Избранное", "⭐ Обране", "⭐ Oblíbené", "⭐ Favorites" -> {
                handleFavorites(chatId);
                yield true;
            }
            case "🌐 Язык", "🌐 Мова", "🌐 Jazyk", "🌐 Language" -> {
                handleLanguage(chatId);
                yield true;
            }
            default -> false;
        };
    }

    private void handleCommand(Long chatId, String username, String telegramLanguageCode, String text) {
        switch (text.split("\\s+")[0].toLowerCase()) {
            case "/start" -> handleStart(chatId, username, telegramLanguageCode);
            case "/latest" -> handleLatest(chatId);
            case "/find" -> handleFind(chatId);
            case "/favorites" -> handleFavorites(chatId);
            case "/help" -> handleHelp(chatId);
            case "/filter" -> startFilterSetup(chatId);
            case "/myfilter" -> showCurrentFilter(chatId);
            case "/resetfilter" -> resetFilter(chatId);
            case "/language" -> handleLanguage(chatId);
            default -> sendMessage(
                    chatId,
                    messages.get(lang(chatId), "command.unknown"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
        }
    }

    private void handleStart(Long chatId, String username, String telegramLanguageCode) {
        subscriberService.subscribe(chatId, username);
        saveLanguage(chatId, resolveLanguageCode(telegramLanguageCode));

        sendMessage(
                chatId,
                messages.get(lang(chatId), "menu.ready"),
                keyboardFactory.mainMenuKeyboard(lang(chatId))
        );

        startFilterSetup(chatId);
    }

    private void startFilterSetup(Long chatId) {
        String currentLang = lang(chatId);

        userFilterService.clearFilter(chatId);

        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setLanguageCode(currentLang);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.WAITING_CAR_TYPE);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "start.title"),
                keyboardFactory.carTypeKeyboard(lang(chatId))
        );
    }

    private void handleCallback(Update update) throws Exception {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String callbackId = update.getCallbackQuery().getId();

        answerCallback(callbackId);

        if ("find_more".equals(data)) {
            handleFindMore(chatId);
            return;
        }

        if ("find_restart".equals(data)) {
            handleFindRestart(chatId);
            return;
        }

        if ("find_stop".equals(data)) {
            handleFindStop(chatId);
            return;
        }

        if ("myfilter_find".equals(data)) {
            handleFind(chatId);
            return;
        }

        if ("myfilter_edit".equals(data)) {
            startFilterSetup(chatId);
            return;
        }

        if ("myfilter_reset".equals(data)) {
            resetFilter(chatId);
            return;
        }

        if ("show_myfilter".equals(data)) {
            showCurrentFilter(chatId);
            return;
        }

        if (data.startsWith("fav_add:")) {
            handleAddFavorite(chatId, data.substring("fav_add:".length()));
            return;
        }

        if (data.startsWith("fav_remove:")) {
            handleRemoveFavorite(chatId, data.substring("fav_remove:".length()));
            return;
        }

        if (data.startsWith("lang:")) {
            handleLanguageCallback(chatId, data.substring("lang:".length()));
            return;
        }

        if (data.startsWith("car_type:")) {
            handleCarTypeCallback(chatId, data.substring("car_type:".length()));
            return;
        }

        if (data.startsWith("brand:")) {
            handleBrandCallback(chatId, data.substring("brand:".length()));
            return;
        }

        if (data.startsWith("max_price:")) {
            handleMaxPriceCallback(chatId, data.substring("max_price:".length()));
            return;
        }

        if (data.startsWith("location:")) {
            handleLocationCallback(chatId, data.substring("location:".length()));
            return;
        }

        if (data.startsWith("mileage:")) {
            handleMileageCallback(chatId, data.substring("mileage:".length()));
            return;
        }

        if (data.startsWith("transmission:")) {
            handleTransmissionCallback(chatId, data.substring("transmission:".length()));
            return;
        }

        if (data.startsWith("year_from:")) {
            handleYearFromCallback(chatId, data.substring("year_from:".length()));
        }
    }

    private void handleCarTypeCallback(Long chatId, String value) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setCarType(value);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.WAITING_BRAND);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.carType.saved"),
                keyboardFactory.brandKeyboard(lang(chatId))
        );
    }

    private void handleBrandCallback(Long chatId, String value) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setBrand("ANY".equals(value) ? null : value);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.WAITING_MAX_PRICE);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.brand.saved"),
                keyboardFactory.maxPriceKeyboard(lang(chatId))
        );
    }

    private void handleMaxPriceCallback(Long chatId, String value) {
        Integer maxPrice = Integer.parseInt(value);

        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setMaxPrice(maxPrice == 0 ? null : maxPrice);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.WAITING_LOCATION);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.price.saved"),
                keyboardFactory.locationKeyboard(lang(chatId))
        );
    }

    private void handleLocationCallback(Long chatId, String value) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setLocation("ANY".equals(value) ? null : value);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.WAITING_MAX_MILEAGE);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.location.saved"),
                keyboardFactory.mileageKeyboard(lang(chatId))
        );
    }

    private void handleMileageCallback(Long chatId, String value) {
        Integer maxMileage = Integer.parseInt(value);

        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setMaxMileage(maxMileage == 0 ? null : maxMileage);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.WAITING_TRANSMISSION);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.mileage.saved"),
                keyboardFactory.transmissionKeyboard(lang(chatId))
        );
    }

    private void handleTransmissionCallback(Long chatId, String value) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setTransmission("ANY".equals(value) ? null : value);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.WAITING_YEAR_FROM);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.transmission.saved"),
                keyboardFactory.yearFromKeyboard(lang(chatId))
        );
    }

    private void handleYearFromCallback(Long chatId, String value) {
        Integer yearFrom = Integer.parseInt(value);

        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setYearFrom(yearFrom == 0 ? null : yearFrom);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.COMPLETED);

        sendMessage(
                chatId,
                buildFilterSummary(filter),
                keyboardFactory.mainMenuKeyboard(lang(chatId))
        );
    }

    private void handleLatest(Long chatId) {
        List<CarEntity> cars = carRepository.findTop5ByOrderByCreatedAtDesc();

        if (cars.isEmpty()) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "cars.empty"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
            return;
        }

        for (CarEntity car : cars) {
            sendCarCard(chatId, car);
        }
    }

    private void handleFind(Long chatId) {
        List<CarEntity> cars = carSearchService.findMatchingCars(chatId, 50);

        if (cars.isEmpty()) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "cars.noMatches.pretty"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
            return;
        }

        findOffsets.put(chatId, 0);
        sendMessage(chatId, buildFindSummary(chatId, cars.size()));
        sendFindPage(chatId, cars);
    }

    private void handleFindMore(Long chatId) {
        List<CarEntity> cars = carSearchService.findMatchingCars(chatId, 50);

        if (cars.isEmpty()) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "cars.noMatches.pretty"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
            return;
        }

        sendFindPage(chatId, cars);
    }

    private void handleFindRestart(Long chatId) {
        findOffsets.put(chatId, 0);
        handleFind(chatId);
    }

    private void handleFindStop(Long chatId) {
        findOffsets.remove(chatId);
        sendMessage(
                chatId,
                messages.get(lang(chatId), "cars.searchFinished"),
                keyboardFactory.mainMenuKeyboard(lang(chatId))
        );
    }

    private void sendFindPage(Long chatId, List<CarEntity> cars) {
        int offset = findOffsets.getOrDefault(chatId, 0);

        if (offset >= cars.size()) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "cars.noMore"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
            return;
        }

        int end = Math.min(offset + FIND_PAGE_SIZE, cars.size());

        for (int i = offset; i < end; i++) {
            sendCarCard(chatId, cars.get(i));
        }

        if (end < cars.size()) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "cars.morePrompt"),
                    keyboardFactory.findNavigationKeyboard(lang(chatId))
            );
        } else {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "cars.noMore"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
        }

        findOffsets.put(chatId, end);
    }

    private String buildFindSummary(Long chatId, int totalFound) {
        UserFilterEntity filter = userFilterService.findByChatId(chatId).orElse(null);
        String lang = lang(chatId);

        if (filter == null) {
            return messages.get(lang, "cars.matchesFound") + " " + totalFound;
        }

        return """
                %s %s

                %s: %s
                %s: %s
                %s: %s
                %s: %s
                %s: %s
                %s: %s
                """.formatted(
                messages.get(lang, "cars.matchesFound"),
                totalFound,
                messages.get(lang, "label.brand"), formatBrand(lang, filter.getBrand()),
                messages.get(lang, "label.maxPrice"), filter.getMaxPrice() == null ? messages.get(lang, "common.noLimit") : filter.getMaxPrice() + " Kč",
                messages.get(lang, "label.location"), formatLocation(lang, filter.getLocation()),
                messages.get(lang, "label.maxMileage"), filter.getMaxMileage() == null ? messages.get(lang, "common.noLimit") : filter.getMaxMileage() + " km",
                messages.get(lang, "label.transmission"), formatTransmission(lang, filter.getTransmission()),
                messages.get(lang, "label.yearFrom"), filter.getYearFrom() == null ? messages.get(lang, "common.notImportant") : filter.getYearFrom().toString()
        );
    }

    private void handleAddFavorite(Long chatId, String carIdValue) {
        try {
            Long carId = Long.parseLong(carIdValue);
            boolean added = favoriteCarService.addToFavorites(chatId, carId);

            if (added) {
                sendMessage(
                        chatId,
                        messages.get(lang(chatId), "favorites.added"),
                        keyboardFactory.mainMenuKeyboard(lang(chatId))
                );
            } else {
                sendMessage(
                        chatId,
                        messages.get(lang(chatId), "favorites.alreadyExists"),
                        keyboardFactory.mainMenuKeyboard(lang(chatId))
                );
            }
        } catch (Exception e) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "favorites.error"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
        }
    }

    private void handleRemoveFavorite(Long chatId, String carIdValue) {
        try {
            Long carId = Long.parseLong(carIdValue);
            boolean removed = favoriteCarService.removeFromFavorites(chatId, carId);

            if (removed) {
                sendMessage(
                        chatId,
                        messages.get(lang(chatId), "favorites.removed"),
                        keyboardFactory.mainMenuKeyboard(lang(chatId))
                );
            } else {
                sendMessage(
                        chatId,
                        messages.get(lang(chatId), "favorites.notFound"),
                        keyboardFactory.mainMenuKeyboard(lang(chatId))
                );
            }
        } catch (Exception e) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "favorites.removeError"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
        }
    }

    private void handleFavorites(Long chatId) {
        List<CarEntity> favorites = favoriteCarService.getFavorites(chatId);

        if (favorites.isEmpty()) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "favorites.empty"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
            return;
        }

        sendMessage(chatId, messages.get(lang(chatId), "favorites.title") + " " + favorites.size());

        for (CarEntity car : favorites) {
            sendMessage(
                    chatId,
                    formatCar(chatId, car),
                    keyboardFactory.carCardKeyboard(lang(chatId), car.getId(), car.getUrl(), true)
            );
        }
    }

    private void sendCarCard(Long chatId, CarEntity car) {
        sendMessage(
                chatId,
                formatCar(chatId, car),
                keyboardFactory.carCardKeyboard(lang(chatId), car.getId(), car.getUrl(), false)
        );
    }

    private void handleHelp(Long chatId) {
        sendMessage(
                chatId,
                messages.get(lang(chatId), "help.text"),
                keyboardFactory.mainMenuKeyboard(lang(chatId))
        );
    }

    private void handleLanguage(Long chatId) {
        sendMessage(
                chatId,
                messages.get(lang(chatId), "language.choose"),
                keyboardFactory.languageKeyboard()
        );
    }

    private void handleLanguageCallback(Long chatId, String languageCode) {
        saveLanguage(chatId, languageCode);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "language.changed"),
                keyboardFactory.mainMenuKeyboard(lang(chatId))
        );

        sendMessage(
                chatId,
                messages.get(lang(chatId), "language.nextStep"),
                keyboardFactory.afterLanguageChangedKeyboard(lang(chatId))
        );
    }

    private void showCurrentFilter(Long chatId) {
        UserFilterEntity filter = userFilterService.findByChatId(chatId).orElse(null);

        if (filter == null) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "filter.notConfigured"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
            return;
        }

        String lang = lang(chatId);

        sendMessage(
                chatId,
                """
                %s

                %s: %s
                %s: %s
                %s: %s
                %s: %s
                %s: %s
                %s: %s
                %s: %s
                """.formatted(
                        messages.get(lang, "summary.settings"),
                        messages.get(lang, "label.carType"), formatCarType(lang, filter.getCarType()),
                        messages.get(lang, "label.brand"), formatBrand(lang, filter.getBrand()),
                        messages.get(lang, "label.maxPrice"), filter.getMaxPrice() == null ? messages.get(lang, "common.noLimit") : filter.getMaxPrice() + " Kč",
                        messages.get(lang, "label.location"), formatLocation(lang, filter.getLocation()),
                        messages.get(lang, "label.maxMileage"), filter.getMaxMileage() == null ? messages.get(lang, "common.noLimit") : filter.getMaxMileage() + " km",
                        messages.get(lang, "label.transmission"), formatTransmission(lang, filter.getTransmission()),
                        messages.get(lang, "label.yearFrom"), filter.getYearFrom() == null ? messages.get(lang, "common.notImportant") : filter.getYearFrom().toString()
                ),
                keyboardFactory.myFilterActionsKeyboard(lang)
        );
    }

    private void resetFilter(Long chatId) {
        String currentLang = lang(chatId);

        userFilterService.clearFilter(chatId);
        userStateService.reset(chatId);

        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setLanguageCode(currentLang);
        userFilterService.save(filter);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.reset"),
                keyboardFactory.myFilterResetKeyboard(lang(chatId))
        );
    }

    private String buildFilterSummary(UserFilterEntity filter) {
        String lang = filter.getLanguageCode() == null || filter.getLanguageCode().isBlank()
                ? "cs"
                : filter.getLanguageCode();

        return """
                %s

                %s

                %s: %s
                %s: %s
                %s: %s
                %s: %s
                %s: %s
                %s: %s
                %s: %s

                %s
                /myfilter
                /filter
                /latest
                /find
                /favorites
                """.formatted(
                messages.get(lang, "filter.saved"),
                messages.get(lang, "summary.settings"),
                messages.get(lang, "label.carType"), formatCarType(lang, filter.getCarType()),
                messages.get(lang, "label.brand"), formatBrand(lang, filter.getBrand()),
                messages.get(lang, "label.maxPrice"), filter.getMaxPrice() == null ? messages.get(lang, "common.noLimit") : filter.getMaxPrice() + " Kč",
                messages.get(lang, "label.location"), formatLocation(lang, filter.getLocation()),
                messages.get(lang, "label.maxMileage"), filter.getMaxMileage() == null ? messages.get(lang, "common.noLimit") : filter.getMaxMileage() + " km",
                messages.get(lang, "label.transmission"), formatTransmission(lang, filter.getTransmission()),
                messages.get(lang, "label.yearFrom"), filter.getYearFrom() == null ? messages.get(lang, "common.notImportant") : filter.getYearFrom().toString(),
                messages.get(lang, "summary.commands")
        );
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();

        try {
            telegramClient.execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(keyboard)
                .build();

        try {
            telegramClient.execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Long chatId, String text, ReplyKeyboard keyboard) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(keyboard)
                .build();

        try {
            telegramClient.execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void answerCallback(String callbackId) {
        AnswerCallbackQuery answer = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId)
                .build();

        try {
            telegramClient.execute(answer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String resolveLanguageCode(String telegramLanguageCode) {
        if (telegramLanguageCode == null || telegramLanguageCode.isBlank()) {
            return "cs";
        }

        return switch (telegramLanguageCode.toLowerCase()) {
            case "ru" -> "ru";
            case "uk", "ua" -> "uk";
            case "cs", "cz" -> "cs";
            default -> "en";
        };
    }

    private String lang(Long chatId) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        return filter.getLanguageCode() == null || filter.getLanguageCode().isBlank()
                ? "cs"
                : filter.getLanguageCode();
    }

    private void saveLanguage(Long chatId, String languageCode) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setLanguageCode(languageCode);
        userFilterService.save(filter);
    }

    private String formatCar(Long chatId, CarEntity car) {
        String lang = lang(chatId);

        StringBuilder sb = new StringBuilder();
        sb.append("🚗 ").append(safe(car.getTitle())).append("\n\n");

        appendLine(sb, "💰", messages.get(lang, "label.price"), safeOrNull(car.getPrice()));
        appendLine(sb, "📍", messages.get(lang, "label.location"), safeOrNull(car.getLocation()));
        appendLine(sb, "📅", messages.get(lang, "label.year"), formatYear(car.getYear()));
        appendLine(sb, "🛣", messages.get(lang, "label.mileage"), formatMileage(car.getMileage()));
        appendLine(sb, "⚙", messages.get(lang, "label.transmission"), formatTransmissionValue(lang, car.getTransmission()));
        appendLine(sb, "🏷", messages.get(lang, "label.source"), safeOrNull(car.getSource()));

        return sb.toString().trim();
    }

    private void appendLine(StringBuilder sb, String emoji, String label, String value) {
        if (value == null || value.isBlank() || value.equals("-")) {
            return;
        }

        sb.append(emoji)
                .append(" ")
                .append(label)
                .append(": ")
                .append(value)
                .append("\n");
    }

    private String formatYear(Integer year) {
        return year == null ? null : year.toString();
    }

    private String formatMileage(Integer mileage) {
        if (mileage == null) {
            return null;
        }
        return String.format("%,d km", mileage).replace(",", " ");
    }

    private String formatTransmissionValue(String lang, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return messages.getOrDefault(lang, "transmission." + value, value);
    }

    private String formatCarType(String lang, String value) {
        if (value == null || value.isBlank() || "ANY".equals(value)) {
            return messages.get(lang, "common.any");
        }
        return messages.getOrDefault(lang, "carType." + value, value);
    }

    private String formatBrand(String lang, String value) {
        if (value == null || value.isBlank() || "ANY".equals(value)) {
            return messages.get(lang, "common.any");
        }
        return messages.getOrDefault(lang, "brand." + value, value);
    }

    private String formatTransmission(String lang, String value) {
        if (value == null || value.isBlank() || "ANY".equals(value)) {
            return messages.get(lang, "common.any");
        }
        return messages.getOrDefault(lang, "transmission." + value, value);
    }

    private String formatLocation(String lang, String value) {
        if (value == null || value.isBlank() || "ANY".equals(value)) {
            return messages.get(lang, "common.any");
        }
        return messages.getOrDefault(lang, "location." + value, value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String safeOrNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
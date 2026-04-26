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
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CarTelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

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

    private final Map<Long, SearchSession> searchSessions = new ConcurrentHashMap<>();
    private final Set<Long> adminChatIds;

    public CarTelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            TelegramSubscriberService subscriberService,
            UserStateService userStateService,
            UserFilterService userFilterService,
            CarRepository carRepository,
            CarSearchService carSearchService,
            FavoriteCarService favoriteCarService,
            CarBotKeyboardFactory keyboardFactory,
            MessageService messages,
            @Value("${telegram.bot.admin-chat-ids:}") String adminChatIdsRaw
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
        this.adminChatIds = parseAdminChatIds(adminChatIdsRaw);
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

            if (handleMenuButton(chatId, text)) {
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

    private boolean handleMenuButton(Long chatId, String text) {
        String lang = lang(chatId);

        String normalized = text
                .replace("🔍 ", "")
                .replace("⚙️ ", "")
                .replace("📋 ", "")
                .replace("📝 ", "")
                .replace("🆕 ", "")
                .replace("⭐ ", "")
                .replace("🌐 ", "")
                .trim();

        if (normalized.equals(messages.get(lang, "menu.search")) || text.equals("/find")) {
            handleFind(chatId);
            return true;
        }

        if (normalized.equals(messages.get(lang, "menu.filter")) || text.equals("/filter")) {
            startNewFilterSetup(chatId);
            return true;
        }

        if (normalized.equals(messages.get(lang, "menu.myFilter")) || text.equals("/myfilter")) {
            showCurrentFilter(chatId);
            return true;
        }

        if (normalized.equals(messages.get(lang, "menu.latest")) || text.equals("/latest")) {
            handleLatest(chatId);
            return true;
        }

        if (normalized.equals(messages.get(lang, "menu.favorites")) || text.equals("/favorites")) {
            handleFavorites(chatId);
            return true;
        }

        if (normalized.equals(messages.get(lang, "menu.language")) || text.equals("/language")) {
            handleLanguage(chatId);
            return true;
        }

        return false;
    }

    private void handleCommand(Long chatId, String username, String telegramLanguageCode, String text) {
        switch (text.split("\\s+")[0].toLowerCase()) {
            case "/start" -> handleStart(chatId, username, telegramLanguageCode);
            case "/latest" -> handleLatest(chatId);
            case "/find" -> handleFind(chatId);
            case "/favorites" -> handleFavorites(chatId);
            case "/help" -> handleHelp(chatId);
            case "/filter" -> startNewFilterSetup(chatId);
            case "/myfilter" -> showCurrentFilter(chatId);
            case "/resetfilter" -> resetFilter(chatId);
            case "/language" -> handleLanguage(chatId);
            case "/admin" -> handleAdmin(chatId);
            default -> sendMessage(
                    chatId,
                    messages.get(lang(chatId), "command.unknown"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
        }
    }

    private void handleStart(Long chatId, String username, String telegramLanguageCode) {

        subscriberService.subscribe(chatId, username);

        UserFilterEntity filter = userFilterService.getOrCreate(chatId);

        if (filter.getLanguageCode() == null || filter.getLanguageCode().isBlank()) {
            filter.setLanguageCode(resolveLanguageCode(telegramLanguageCode));
            userFilterService.save(filter);
        }

        String lang = lang(chatId);

        sendMessage(
                chatId,
                messages.get(lang, "start.welcome"),
                keyboardFactory.mainMenuKeyboard(lang)
        );
    }

    private boolean isFilterConfigured(UserFilterEntity filter) {
        if (filter == null) {
            return false;
        }

        return (filter.getCarType() != null && !filter.getCarType().isBlank())
                || (filter.getBrand() != null && !filter.getBrand().isBlank())
                || filter.getMaxPrice() != null
                || filter.getMaxMileage() != null
                || (filter.getLocation() != null && !filter.getLocation().isBlank())
                || (filter.getFuelType() != null && !filter.getFuelType().isBlank())
                || (filter.getTransmission() != null && !filter.getTransmission().isBlank())
                || filter.getYearFrom() != null;
    }

    private void startNewFilterSetup(Long chatId) {
        String currentLang = lang(chatId);

        userFilterService.clearFilter(chatId);

        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setLanguageCode(currentLang);
        filter.setCarType(null);
        filter.setBrand(null);
        filter.setFuelType(null);
        filter.setTransmission(null);
        filter.setLocation(null);
        filter.setMaxPrice(null);
        filter.setMaxMileage(null);
        filter.setYearFrom(null);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.WAITING_CAR_TYPE);

        sendMessage(
                chatId,
                messages.get(currentLang, "carType.choose"),
                keyboardFactory.carTypeKeyboard(currentLang, filter.getCarType(), true)
        );
    }

    private void editFilterSetup(Long chatId) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);

        userStateService.setStep(chatId, BotStep.WAITING_CAR_TYPE);

        sendMessage(
                chatId,
                buildCarTypeSelectionText(chatId, filter.getCarType()),
                keyboardFactory.carTypeKeyboard(lang(chatId), filter.getCarType(), true)
        );
    }

    private void handleCallback(Update update) throws Exception {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String callbackId = update.getCallbackQuery().getId();

        answerCallback(callbackId);

        if ("browse_next".equals(data)) {
            handleBrowseNext(chatId);
            return;
        }

        if ("browse_prev".equals(data)) {
            handleBrowsePrev(chatId);
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
            editFilterSetup(chatId);
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

        if (data.startsWith("wizard_back:")) {
            handleWizardBack(chatId, data.substring("wizard_back:".length()));
            return;
        }

        if (data.startsWith("car_type:toggle:")) {
            handleCarTypeToggle(update, chatId, data.substring("car_type:toggle:".length()));
            return;
        }

        if ("car_type:any".equals(data)) {
            handleCarTypeAny(chatId);
            return;
        }

        if ("car_type:done".equals(data)) {
            handleCarTypeDone(update, chatId);
            return;
        }

        if (data.startsWith("brand:toggle:")) {
            handleBrandToggle(update, chatId, data.substring("brand:toggle:".length()));
            return;
        }

        if ("brand:any".equals(data)) {
            handleBrandAny(chatId);
            return;
        }

        if ("brand:done".equals(data)) {
            handleBrandDone(chatId);
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

        if (data.startsWith("fuel_type:")) {
            handleFuelTypeCallback(chatId, data.substring("fuel_type:".length()));
            return;
        }

        if (data.startsWith("year_from:")) {
            handleYearFromCallback(chatId, data.substring("year_from:".length()));
        }
    }

    private void handleWizardBack(Long chatId, String targetStep) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        String lang = lang(chatId);

        switch (targetStep) {
            case "menu" -> {
                userStateService.setStep(chatId, BotStep.NONE);
                sendMessage(
                        chatId,
                        messages.get(lang, "menu.ready"),
                        keyboardFactory.mainMenuKeyboard(lang)
                );
            }

            case "car_type" -> {
                userStateService.setStep(chatId, BotStep.WAITING_CAR_TYPE);
                sendMessage(
                        chatId,
                        buildCarTypeSelectionText(chatId, filter.getCarType()),
                        keyboardFactory.carTypeKeyboard(lang, filter.getCarType(), true)
                );
            }

            case "brand" -> {
                userStateService.setStep(chatId, BotStep.WAITING_BRAND);
                sendMessage(
                        chatId,
                        buildBrandSelectionText(chatId, parseValues(filter.getBrand())),
                        keyboardFactory.brandKeyboard(lang, filter.getBrand(), true)
                );
            }

            case "max_price" -> {
                userStateService.setStep(chatId, BotStep.WAITING_MAX_PRICE);
                sendMessage(
                        chatId,
                        messages.get(lang, "price.choose") + "\n\n" + buildFilterProgress(filter),
                        keyboardFactory.maxPriceKeyboard(lang, true)
                );
            }

            case "location" -> {
                userStateService.setStep(chatId, BotStep.WAITING_LOCATION);
                sendMessage(
                        chatId,
                        messages.get(lang, "location.choose") + "\n\n" + buildFilterProgress(filter),
                        keyboardFactory.locationKeyboard(lang, true)
                );
            }

            case "max_mileage" -> {
                userStateService.setStep(chatId, BotStep.WAITING_MAX_MILEAGE);
                sendMessage(
                        chatId,
                        messages.get(lang, "mileage.choose") + "\n\n" + buildFilterProgress(filter),
                        keyboardFactory.mileageKeyboard(lang, true)
                );
            }

            case "transmission" -> {
                userStateService.setStep(chatId, BotStep.WAITING_TRANSMISSION);
                sendMessage(
                        chatId,
                        messages.get(lang, "transmission.choose") + "\n\n" + buildFilterProgress(filter),
                        keyboardFactory.transmissionKeyboard(lang, true)
                );
            }

            case "fuel_type" -> {
                userStateService.setStep(chatId, BotStep.WAITING_FUEL_TYPE);
                sendMessage(
                        chatId,
                        messages.get(lang, "fuelType.choose") + "\n\n" + buildFilterProgress(filter),
                        keyboardFactory.fuelTypeKeyboard(lang, true)
                );
            }

            case "year_from" -> {
                userStateService.setStep(chatId, BotStep.WAITING_YEAR_FROM);
                sendMessage(
                        chatId,
                        messages.get(lang, "yearFrom.choose") + "\n\n" + buildFilterProgress(filter),
                        keyboardFactory.yearFromKeyboard(lang, true)
                );
            }
        }
    }

    private void handleCarTypeToggle(Update update, Long chatId, String carTypeValue) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);

        Set<String> selected = parseValues(filter.getCarType());

        if (selected.contains(carTypeValue)) {
            selected.remove(carTypeValue);
        } else {
            selected.add(carTypeValue);
        }

        filter.setCarType(joinValues(selected));
        userFilterService.save(filter);

        refreshCarTypeSelectionMessage(update, chatId, filter);
    }

    private void handleCarTypeAny(Long chatId) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setCarType(null);
        filter.setBrand(null);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.WAITING_BRAND);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.carType.saved")
                        + "\n\n"
                        + buildFilterProgress(filter)
                        + "\n\n"
                        + messages.get(lang(chatId), "brand.choose"),
                keyboardFactory.brandKeyboard(lang(chatId), filter.getBrand(), true)
        );
    }

    private void handleCarTypeDone(Update update, Long chatId) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);

        if (filter.getCarType() == null || filter.getCarType().isBlank()) {
            editMessageTextAndKeyboard(
                    chatId,
                    update.getCallbackQuery().getMessage().getMessageId(),
                    messages.get(lang(chatId), "carType.chooseAtLeastOne"),
                    keyboardFactory.carTypeKeyboard(lang(chatId), filter.getCarType(), true)
            );
            return;
        }

        filter.setBrand(null);
        userFilterService.save(filter);
        userStateService.setStep(chatId, BotStep.WAITING_BRAND);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.carType.saved")
                        + "\n\n"
                        + buildFilterProgress(filter)
                        + "\n\n"
                        + messages.get(lang(chatId), "brand.choose"),
                keyboardFactory.brandKeyboard(lang(chatId), filter.getBrand(), true)
        );
    }

    private void refreshCarTypeSelectionMessage(Update update, Long chatId, UserFilterEntity filter) {
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        editMessageTextAndKeyboard(
                chatId,
                messageId,
                buildCarTypeSelectionText(chatId, filter.getCarType()),
                keyboardFactory.carTypeKeyboard(lang(chatId), filter.getCarType(), true)
        );
    }

    private void handleBrandToggle(Update update, Long chatId, String brandValue) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);

        Set<String> selected = parseValues(filter.getBrand());

        if (selected.contains(brandValue)) {
            selected.remove(brandValue);
        } else {
            selected.add(brandValue);
        }

        filter.setBrand(joinValues(selected));
        userFilterService.save(filter);

        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        editMessageTextAndKeyboard(
                chatId,
                messageId,
                buildBrandSelectionText(chatId, selected),
                keyboardFactory.brandKeyboard(lang(chatId), filter.getBrand(), true)
        );
    }

    private void handleBrandAny(Long chatId) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setBrand(null);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.WAITING_MAX_PRICE);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.brand.saved")
                        + "\n\n"
                        + buildFilterProgress(filter)
                        + "\n\n"
                        + messages.get(lang(chatId), "price.choose"),
                keyboardFactory.maxPriceKeyboard(lang(chatId), true)
        );
    }

    private void handleBrandDone(Long chatId) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);

        if (filter.getBrand() == null || filter.getBrand().isBlank()) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "brand.chooseAtLeastOne"),
                    keyboardFactory.brandKeyboard(lang(chatId), filter.getBrand(), true)
            );
            return;
        }

        userFilterService.save(filter);
        userStateService.setStep(chatId, BotStep.WAITING_MAX_PRICE);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.brand.saved")
                        + "\n\n"
                        + buildFilterProgress(filter)
                        + "\n\n"
                        + messages.get(lang(chatId), "price.choose"),
                keyboardFactory.maxPriceKeyboard(lang(chatId), true)
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
                messages.get(lang(chatId), "filter.price.saved")
                        + "\n\n"
                        + buildFilterProgress(filter)
                        + "\n\n"
                        + messages.get(lang(chatId), "location.choose"),
                keyboardFactory.locationKeyboard(lang(chatId), true)
        );
    }

    private void handleLocationCallback(Long chatId, String value) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setLocation("ANY".equals(value) ? null : value);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.WAITING_MAX_MILEAGE);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.location.saved")
                        + "\n\n"
                        + buildFilterProgress(filter)
                        + "\n\n"
                        + messages.get(lang(chatId), "mileage.choose"),
                keyboardFactory.mileageKeyboard(lang(chatId), true)
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
                messages.get(lang(chatId), "filter.mileage.saved")
                        + "\n\n"
                        + buildFilterProgress(filter)
                        + "\n\n"
                        + messages.get(lang(chatId), "transmission.choose"),
                keyboardFactory.transmissionKeyboard(lang(chatId), true)
        );
    }

    private void handleTransmissionCallback(Long chatId, String value) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setTransmission("ANY".equals(value) ? null : value);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.WAITING_FUEL_TYPE);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.transmission.saved")
                        + "\n\n"
                        + buildFilterProgress(filter)
                        + "\n\n"
                        + messages.get(lang(chatId), "fuelType.choose"),
                keyboardFactory.fuelTypeKeyboard(lang(chatId), true)
        );
    }

    private void handleFuelTypeCallback(Long chatId, String value) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setFuelType("ANY".equals(value) ? null : value);
        userFilterService.save(filter);

        userStateService.setStep(chatId, BotStep.WAITING_YEAR_FROM);

        sendMessage(
                chatId,
                messages.get(lang(chatId), "filter.fuelType.saved")
                        + "\n\n"
                        + buildFilterProgress(filter)
                        + "\n\n"
                        + messages.get(lang(chatId), "yearFrom.choose"),
                keyboardFactory.yearFromKeyboard(lang(chatId), true)
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
                buildFilterSummary(filter) + "\n\n" + messages.get(lang(chatId), "filter.saved.next"),
                keyboardFactory.myFilterActionsKeyboard(lang(chatId))
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

        sendMessage(
                chatId,
                messages.get(lang(chatId), "cars.latest")
        );

        for (CarEntity car : cars) {
            sendCarCard(chatId, car);
        }
    }

    private void handleFind(Long chatId) {
        UserFilterEntity filter = userFilterService.findByChatId(chatId).orElse(null);
        String lang = lang(chatId);

        if (!isFilterConfigured(filter)) {
            sendMessage(
                    chatId,
                    messages.get(lang, "filter.notConfigured"),
                    keyboardFactory.mainMenuKeyboard(lang)
            );
            return;
        }

        List<CarEntity> cars = carSearchService.findMatchingCars(chatId, 50);

        if (cars.isEmpty()) {
            sendMessage(
                    chatId,
                    messages.get(lang, "cars.noMatches.pretty")
                            + buildRelaxSuggestion(chatId, 0),
                    keyboardFactory.myFilterActionsKeyboard(lang)
            );
            return;
        }

        SearchSession session = new SearchSession(cars);
        searchSessions.put(chatId, session);

        sendMessage(
                chatId,
                buildFindSummary(chatId, cars.size()) + buildRelaxSuggestion(chatId, cars.size())
        );

        sendCurrentSearchCar(chatId);
    }

    private void handleFindRestart(Long chatId) {
        handleFind(chatId);
    }

    private void handleFindStop(Long chatId) {
        searchSessions.remove(chatId);
        sendMessage(
                chatId,
                messages.get(lang(chatId), "cars.searchFinished"),
                keyboardFactory.mainMenuKeyboard(lang(chatId))
        );
    }

    private void handleBrowseNext(Long chatId) {
        SearchSession session = searchSessions.get(chatId);

        if (session == null || session.isEmpty()) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "cars.searchFinished"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
            return;
        }

        session.next();
        sendCurrentSearchCar(chatId);
    }

    private void handleBrowsePrev(Long chatId) {
        SearchSession session = searchSessions.get(chatId);

        if (session == null || session.isEmpty()) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "cars.searchFinished"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
            return;
        }

        session.prev();
        sendCurrentSearchCar(chatId);
    }

    private void sendCurrentSearchCar(Long chatId) {
        SearchSession session = searchSessions.get(chatId);

        if (session == null || session.isEmpty()) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "cars.searchFinished"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
            return;
        }

        CarEntity car = session.current();

        String text = formatCar(chatId, car, session.currentNumber(), session.total());

        sendCarMessage(
                chatId,
                car,
                text,
                keyboardFactory.searchBrowseKeyboard(
                        lang(chatId),
                        car.getId(),
                        car.getUrl(),
                        session.hasPrev(),
                        session.hasNext()
                )
        );
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
            %s: %s
            %s: %s
            """.formatted(
                messages.get(lang, "cars.matchesFound"),
                totalFound,
                messages.get(lang, "label.carType"), formatCarType(lang, filter.getCarType()),
                messages.get(lang, "label.brand"), formatBrand(lang, filter.getBrand()),
                messages.get(lang, "label.maxPrice"), filter.getMaxPrice() == null ? messages.get(lang, "common.noLimit") : filter.getMaxPrice() + " Kč",
                messages.get(lang, "label.location"), formatLocation(lang, filter.getLocation()),
                messages.get(lang, "label.maxMileage"), filter.getMaxMileage() == null ? messages.get(lang, "common.noLimit") : filter.getMaxMileage() + " km",
                messages.get(lang, "label.transmission"), formatTransmission(lang, filter.getTransmission()),
                messages.get(lang, "label.fuelType"), formatFuelType(lang, filter.getFuelType()),
                messages.get(lang, "label.yearFrom"), filter.getYearFrom() == null ? messages.get(lang, "common.notImportant") : filter.getYearFrom().toString()
        );
    }

    private String buildRelaxSuggestion(Long chatId, int totalFound) {
        String lang = lang(chatId);
        UserFilterEntity filter = userFilterService.findByChatId(chatId).orElse(null);

        if (filter == null) {
            return "";
        }

        if (totalFound > 3) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        switch (lang) {
            case "ru" -> sb.append("\n\n💡 Подсказка:\n");
            case "uk" -> sb.append("\n\n💡 Підказка:\n");
            case "cs" -> sb.append("\n\n💡 Tip:\n");
            default -> sb.append("\n\n💡 Tip:\n");
        }

        if (filter.getMaxMileage() != null && filter.getMaxMileage() <= 150_000) {
            switch (lang) {
                case "ru" -> sb.append("• увеличьте максимальный пробег до 200 000–250 000 км\n");
                case "uk" -> sb.append("• збільште максимальний пробіг до 200 000–250 000 км\n");
                case "cs" -> sb.append("• zvyšte max. nájezd na 200 000–250 000 km\n");
                default -> sb.append("• increase max mileage to 200,000–250,000 km\n");
            }
        }

        if (filter.getYearFrom() != null && filter.getYearFrom() >= 2010) {
            switch (lang) {
                case "ru" -> sb.append("• снизьте минимальный год до 2005–2008\n");
                case "uk" -> sb.append("• зменште мінімальний рік до 2005–2008\n");
                case "cs" -> sb.append("• snižte minimální rok na 2005–2008\n");
                default -> sb.append("• lower minimum year to 2005–2008\n");
            }
        }

        if (filter.getBrand() != null && filter.getBrand().contains(",")) {
            switch (lang) {
                case "ru" -> sb.append("• либо добавьте ещё несколько марок\n");
                case "uk" -> sb.append("• або додайте ще кілька марок\n");
                case "cs" -> sb.append("• nebo přidejte ještě několik značek\n");
                default -> sb.append("• or add a few more brands\n");
            }
        } else if (filter.getBrand() != null && !filter.getBrand().isBlank()) {
            switch (lang) {
                case "ru" -> sb.append("• попробуйте выбрать несколько марок вместо одной\n");
                case "uk" -> sb.append("• спробуйте вибрати кілька марок замість однієї\n");
                case "cs" -> sb.append("• zkuste vybrat více značek místo jedné\n");
                default -> sb.append("• try selecting multiple brands instead of one\n");
            }
        }

        if (filter.getCarType() != null && !filter.getCarType().isBlank()) {
            switch (lang) {
                case "ru" -> sb.append("• можно убрать ограничение по типу кузова\n");
                case "uk" -> sb.append("• можна прибрати обмеження за типом кузова\n");
                case "cs" -> sb.append("• můžete zrušit omezení typu karoserie\n");
                default -> sb.append("• you can remove the body type restriction\n");
            }
        }

        return sb.toString().trim().isEmpty() ? "" : sb.toString();
    }

    private void handleAddFavorite(Long chatId, String carIdValue) {
        try {
            Long carId = Long.parseLong(carIdValue);
            boolean added = favoriteCarService.addToFavorites(chatId, carId);

            if (added) {
                sendMessage(chatId, messages.get(lang(chatId), "favorites.added"));
            } else {
                sendMessage(chatId, messages.get(lang(chatId), "favorites.alreadyExists"));
            }
        } catch (Exception e) {
            sendMessage(chatId, messages.get(lang(chatId), "favorites.error"));
        }
    }

    private void handleRemoveFavorite(Long chatId, String carIdValue) {
        try {
            Long carId = Long.parseLong(carIdValue);
            boolean removed = favoriteCarService.removeFromFavorites(chatId, carId);

            if (removed) {
                sendMessage(chatId, messages.get(lang(chatId), "favorites.removed"));
            } else {
                sendMessage(chatId, messages.get(lang(chatId), "favorites.notFound"));
            }
        } catch (Exception e) {
            sendMessage(chatId, messages.get(lang(chatId), "favorites.removeError"));
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

        sendMessage(
                chatId,
                messages.get(lang(chatId), "favorites.title") + " " + favorites.size()
        );

        for (CarEntity car : favorites) {
            sendCarMessage(
                    chatId,
                    car,
                    keyboardFactory.carCardKeyboard(lang(chatId), car.getId(), car.getUrl(), true)
            );
        }
    }

    private void sendCarCard(Long chatId, CarEntity car) {
        sendCarMessage(
                chatId,
                car,
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

        if (!isFilterConfigured(filter)) {
            sendMessage(
                    chatId,
                    messages.get(lang(chatId), "filter.notConfigured"),
                    keyboardFactory.mainMenuKeyboard(lang(chatId))
            );
            return;
        }

        String lang = lang(chatId);

        String maxPrice = filter.getMaxPrice() == null
                ? messages.get(lang, "common.noLimit")
                : filter.getMaxPrice() + " Kč";

        String maxMileage = filter.getMaxMileage() == null
                ? messages.get(lang, "common.noLimit")
                : filter.getMaxMileage() + " km";

        String yearFrom = filter.getYearFrom() == null
                ? messages.get(lang, "common.notImportant")
                : filter.getYearFrom().toString();

        String text = """
            📋 %s

            %s: %s
            %s: %s
            %s: %s
            %s: %s
            %s: %s
            %s: %s
            %s: %s
            %s: %s
            """.formatted(
                messages.get(lang, "summary.currentFilter"),
                messages.get(lang, "label.carType"), formatCarType(lang, filter.getCarType()),
                messages.get(lang, "label.brand"), formatBrand(lang, filter.getBrand()),
                messages.get(lang, "label.maxPrice"), maxPrice,
                messages.get(lang, "label.location"), formatLocation(lang, filter.getLocation()),
                messages.get(lang, "label.maxMileage"), maxMileage,
                messages.get(lang, "label.transmission"), formatTransmission(lang, filter.getTransmission()),
                messages.get(lang, "label.fuelType"), formatFuelType(lang, filter.getFuelType()),
                messages.get(lang, "label.yearFrom"), yearFrom
        );

        sendMessage(
                chatId,
                text,
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
                keyboardFactory.mainMenuKeyboard(lang(chatId))
        );

        startNewFilterSetup(chatId);
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
                %s: %s
                """.formatted(
                messages.get(lang, "filter.saved"),
                messages.get(lang, "summary.settings"),
                messages.get(lang, "label.carType"), formatCarType(lang, filter.getCarType()),
                messages.get(lang, "label.brand"), formatBrand(lang, filter.getBrand()),
                messages.get(lang, "label.maxPrice"), filter.getMaxPrice() == null ? messages.get(lang, "common.noLimit") : filter.getMaxPrice() + " Kč",
                messages.get(lang, "label.location"), formatLocation(lang, filter.getLocation()),
                messages.get(lang, "label.maxMileage"), filter.getMaxMileage() == null ? messages.get(lang, "common.noLimit") : filter.getMaxMileage() + " km",
                messages.get(lang, "label.transmission"), formatTransmission(lang, filter.getTransmission()),
                messages.get(lang, "label.fuelType"), formatFuelType(lang, filter.getFuelType()),
                messages.get(lang, "label.yearFrom"), filter.getYearFrom() == null ? messages.get(lang, "common.notImportant") : filter.getYearFrom().toString()
        );
    }

    private String buildFilterProgress(UserFilterEntity filter) {
        String lang = filter.getLanguageCode() == null || filter.getLanguageCode().isBlank()
                ? "cs"
                : filter.getLanguageCode();

        StringBuilder sb = new StringBuilder();
        sb.append(messages.get(lang, "summary.currentFilter")).append(":\n");

        if (filter.getCarType() != null && !filter.getCarType().isBlank()) {
            sb.append(messages.get(lang, "label.carType"))
                    .append(": ")
                    .append(formatCarType(lang, filter.getCarType()))
                    .append("\n");
        }

        if (filter.getBrand() != null && !filter.getBrand().isBlank()) {
            sb.append(messages.get(lang, "label.brand"))
                    .append(": ")
                    .append(formatBrand(lang, filter.getBrand()))
                    .append("\n");
        }

        if (filter.getMaxPrice() != null) {
            sb.append(messages.get(lang, "label.maxPrice"))
                    .append(": ")
                    .append(filter.getMaxPrice())
                    .append(" Kč\n");
        }

        if (filter.getLocation() != null && !filter.getLocation().isBlank()) {
            sb.append(messages.get(lang, "label.location"))
                    .append(": ")
                    .append(formatLocation(lang, filter.getLocation()))
                    .append("\n");
        }

        if (filter.getMaxMileage() != null) {
            sb.append(messages.get(lang, "label.maxMileage"))
                    .append(": ")
                    .append(filter.getMaxMileage())
                    .append(" km\n");
        }

        if (filter.getTransmission() != null && !filter.getTransmission().isBlank()) {
            sb.append(messages.get(lang, "label.transmission"))
                    .append(": ")
                    .append(formatTransmission(lang, filter.getTransmission()))
                    .append("\n");
        }

        if (filter.getFuelType() != null && !filter.getFuelType().isBlank()) {
            sb.append(messages.get(lang, "label.fuelType"))
                    .append(": ")
                    .append(formatFuelType(lang, filter.getFuelType()))
                    .append("\n");
        }

        if (filter.getYearFrom() != null) {
            sb.append(messages.get(lang, "label.yearFrom"))
                    .append(": ")
                    .append(filter.getYearFrom())
                    .append("\n");
        }

        return sb.toString().trim();
    }

    private String buildCarTypeSelectionText(Long chatId, String rawCarTypes) {
        String lang = lang(chatId);
        Set<String> selected = parseValues(rawCarTypes);

        if (selected.isEmpty()) {
            return messages.get(lang, "carType.choose");
        }

        String joined = selected.stream()
                .map(value -> messages.getOrDefault(lang, "carType." + value, value))
                .reduce((a, b) -> a + ", " + b)
                .orElse(messages.get(lang, "common.any"));

        return messages.get(lang, "carType.selected") + "\n\n" + joined;
    }

    private String buildBrandSelectionText(Long chatId, Set<String> selected) {
        String lang = lang(chatId);

        if (selected.isEmpty()) {
            return messages.get(lang, "brand.choose");
        }

        String brands = selected.stream()
                .map(value -> messages.getOrDefault(lang, "brand." + value, value))
                .reduce((a, b) -> a + ", " + b)
                .orElse("-");

        return messages.get(lang, "brand.selected") + "\n\n" + brands;
    }

    private Set<String> parseValues(String raw) {
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

    private String joinValues(Set<String> values) {
        return String.join(",", values);
    }

    private void sendCarMessage(Long chatId, CarEntity car, InlineKeyboardMarkup keyboard) {
        sendCarMessage(
                chatId,
                car,
                formatCar(chatId, car, null, null),
                keyboard
        );
    }

    private void sendCarMessage(Long chatId, CarEntity car, String text, InlineKeyboardMarkup keyboard) {
        try {
            if (hasImage(car.getImageUrl())) {
                if (sendPhotoByUrl(chatId, car.getImageUrl(), text, keyboard)) {
                    return;
                }

                if (sendPhotoByDownload(chatId, car, text, keyboard)) {
                    return;
                }
            }

            sendMessage(chatId, text, keyboard);

        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, text, keyboard);
        }
    }

    private boolean sendPhotoByUrl(Long chatId,
                                   String imageUrl,
                                   String caption,
                                   InlineKeyboardMarkup keyboard) {
        try {
            SendPhoto photo = SendPhoto.builder()
                    .chatId(chatId.toString())
                    .photo(new InputFile(imageUrl))
                    .caption(caption)
                    .replyMarkup(keyboard)
                    .build();

            telegramClient.execute(photo);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private boolean sendPhotoByDownload(Long chatId,
                                        CarEntity car,
                                        String caption,
                                        InlineKeyboardMarkup keyboard) {
        File tempFile = null;

        try {
            tempFile = downloadImageToTempFile(car);

            if (tempFile == null || !tempFile.exists() || tempFile.length() == 0) {
                return false;
            }

            SendPhoto photo = SendPhoto.builder()
                    .chatId(chatId.toString())
                    .photo(new InputFile(tempFile))
                    .caption(caption)
                    .replyMarkup(keyboard)
                    .build();

            telegramClient.execute(photo);
            return true;

        } catch (Exception e) {
            return false;

        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                } catch (Exception ignored) {
                }
            }
        }
    }

    private File downloadImageToTempFile(CarEntity car) throws Exception {
        String imageUrl = car.getImageUrl();

        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        URLConnection connection = new java.net.URL(imageUrl).openConnection();

        connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        );
        connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
        connection.setRequestProperty("Referer", resolveReferer(car));
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);

        String extension = guessExtension(imageUrl);

        File tempFile = File.createTempFile("car-photo-" + UUID.randomUUID(), extension);

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {

            inputStream.transferTo(outputStream);
        }

        if (tempFile.length() == 0) {
            Files.deleteIfExists(tempFile.toPath());
            return null;
        }

        return tempFile;
    }

    private String resolveReferer(CarEntity car) {
        String source = safe(car.getSource()).toUpperCase();

        if (source.contains("BAZOS")) {
            return "https://auto.bazos.cz/";
        }

        if (source.contains("SAUTO")) {
            return "https://www.sauto.cz/";
        }

        if (source.contains("TIPCARS") || source.contains("TIP_CARS")) {
            return "https://www.tipcars.com/";
        }

        return "https://www.google.com/";
    }

    private String guessExtension(String imageUrl) {
        String lower = imageUrl.toLowerCase();

        if (lower.contains(".png")) {
            return ".png";
        }
        if (lower.contains(".jpeg")) {
            return ".jpeg";
        }
        if (lower.contains(".webp")) {
            return ".webp";
        }
        return ".jpg";
    }

    private boolean hasImage(String imageUrl) {
        return imageUrl != null
                && !imageUrl.isBlank()
                && !imageUrl.toLowerCase().contains("empty.gif");
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

    private void editMessageTextAndKeyboard(Long chatId,
                                            Integer messageId,
                                            String text,
                                            InlineKeyboardMarkup keyboard) {
        try {
            EditMessageText editText = EditMessageText.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .text(text)
                    .replyMarkup(keyboard)
                    .build();

            telegramClient.execute(editText);
        } catch (Exception e) {
            try {
                EditMessageReplyMarkup editMarkup = EditMessageReplyMarkup.builder()
                        .chatId(chatId.toString())
                        .messageId(messageId)
                        .replyMarkup(keyboard)
                        .build();

                telegramClient.execute(editMarkup);
            } catch (Exception ignored) {
            }
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

    private String formatCar(Long chatId, CarEntity car, Integer current, Integer total) {
        String lang = lang(chatId);

        String title = formatTitle(car.getTitle());
        String price = formatPrice(car);
        String location = safeOrNull(car.getLocation());
        String year = safeYear(car.getYear());
        String mileage = safeMileage(car.getMileage());
        String fuel = formatFuelTypeValue(lang, car.getFuelType());
        String transmission = formatTransmissionValue(lang, car.getTransmission());
        String source = formatSource(car.getSource());
        String freshness = formatFreshness(lang, car.getCreatedAt());

        StringBuilder sb = new StringBuilder();

        sb.append("🚗 ").append(title).append("\n\n");

        if (price != null) {
            sb.append("💰 ").append(price).append("\n");
        }

        if (year != null || mileage != null) {
            sb.append("📅 ").append(year != null ? year : "-")
                    .append(" | 🛣 ").append(mileage != null ? mileage : "-")
                    .append("\n");
        }

        if (fuel != null || transmission != null) {
            sb.append("⛽ ").append(fuel != null ? fuel : "-")
                    .append(" | ⚙️ ").append(transmission != null ? transmission : "-")
                    .append("\n");
        }

        if (location != null) {
            sb.append("📍 ").append(location).append("\n");
        }

        if (source != null) {
            sb.append("🌐 ").append(source).append("\n");
        }

        if (freshness != null) {
            sb.append("🕒 ").append(freshness).append("\n");
        }

        if (current != null && total != null && total > 0) {
            sb.append("\n📊 ").append(current).append(" / ").append(total);
        }

        return sb.toString().trim();
    }

    private String formatTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return "-";
        }

        String cleaned = rawTitle.replaceAll("\\s+", " ").trim();

        if (cleaned.length() <= 55) {
            return cleaned;
        }

        int splitIndex = cleaned.lastIndexOf(" ", 55);
        if (splitIndex < 30) {
            splitIndex = 55;
        }

        String first = cleaned.substring(0, splitIndex).trim();
        String second = cleaned.substring(splitIndex).trim();

        if (second.length() > 45) {
            second = second.substring(0, 42).trim() + "...";
        }

        return first + "\n" + second;
    }

    private String formatPrice(CarEntity car) {
        if (car == null) {
            return null;
        }

        if (car.getPrice() != null && !car.getPrice().isBlank()) {
            return car.getPrice().trim();
        }

        if (car.getPriceValue() != null && car.getPriceValue() > 0) {
            return String.format("%,d Kč", car.getPriceValue()).replace(",", " ");
        }

        return null;
    }

    private String formatSource(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }

        String normalized = source.trim().toUpperCase();

        if (normalized.contains("SAUTO")) {
            return "Sauto.cz";
        }

        if (normalized.contains("BAZOS")) {
            return "Bazoš.cz";
        }

        if (normalized.contains("TIPCARS") || normalized.contains("TIP_CARS")) {
            return "TipCars.cz";
        }

        return source.trim();
    }

    private String formatFreshness(String lang, LocalDateTime createdAt) {
        if (createdAt == null) {
            return null;
        }

        long minutes = Duration.between(createdAt, LocalDateTime.now()).toMinutes();

        if (minutes < 1) {
            return switch (lang) {
                case "ru" -> "только что";
                case "uk" -> "щойно";
                case "cs" -> "právě teď";
                default -> "just now";
            };
        }

        if (minutes < 60) {
            return switch (lang) {
                case "ru" -> minutes + " мин назад";
                case "uk" -> minutes + " хв тому";
                case "cs" -> "před " + minutes + " min";
                default -> minutes + " min ago";
            };
        }

        long hours = minutes / 60;

        if (hours < 24) {
            return switch (lang) {
                case "ru" -> hours + " ч назад";
                case "uk" -> hours + " год тому";
                case "cs" -> "před " + hours + " h";
                default -> hours + " h ago";
            };
        }

        long days = hours / 24;

        if (days < 30) {
            return switch (lang) {
                case "ru" -> days + " дн назад";
                case "uk" -> days + " дн тому";
                case "cs" -> "před " + days + " d";
                default -> days + " d ago";
            };
        }

        long months = days / 30;

        if (months < 12) {
            return switch (lang) {
                case "ru" -> months + " мес назад";
                case "uk" -> months + " міс тому";
                case "cs" -> "před " + months + " měs.";
                default -> months + " mo ago";
            };
        }

        long years = days / 365;

        return switch (lang) {
            case "ru" -> years + " г назад";
            case "uk" -> years + " р тому";
            case "cs" -> "před " + years + " r.";
            default -> years + " y ago";
        };
    }

    private String safeYear(Integer year) {
        return year == null ? null : year.toString();
    }

    private String safeMileage(Integer mileage) {
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

    private String formatFuelTypeValue(String lang, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return messages.getOrDefault(lang, "fuelType." + value, value);
    }

    private String formatCarType(String lang, String value) {
        if (value == null || value.isBlank() || "ANY".equalsIgnoreCase(value)) {
            return messages.get(lang, "common.any");
        }

        String[] parts = value.split(",");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            String carType = part.trim();
            if (carType.isBlank()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(", ");
            }

            result.append(messages.getOrDefault(lang, "carType." + carType, carType));
        }

        return result.isEmpty() ? messages.get(lang, "common.any") : result.toString();
    }

    private String formatBrand(String lang, String value) {
        if (value == null || value.isBlank() || "ANY".equals(value)) {
            return messages.get(lang, "common.any");
        }

        String[] parts = value.split(",");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            String brand = part.trim();
            if (brand.isBlank()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(", ");
            }

            result.append(messages.getOrDefault(lang, "brand." + brand, brand));
        }

        return result.isEmpty() ? messages.get(lang, "common.any") : result.toString();
    }

    private String formatTransmission(String lang, String value) {
        if (value == null || value.isBlank() || "ANY".equals(value)) {
            return messages.get(lang, "common.any");
        }
        return messages.getOrDefault(lang, "transmission." + value, value);
    }

    private String formatFuelType(String lang, String value) {
        if (value == null || value.isBlank() || "ANY".equals(value)) {
            return messages.get(lang, "common.any");
        }
        return messages.getOrDefault(lang, "fuelType." + value, value);
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

    private void handleAdmin(Long chatId) {

        if (!isAdmin(chatId)) {
            sendMessage(chatId, "⛔ Admin access denied.");
            return;
        }

        LocalDateTime last24h = LocalDateTime.now().minusHours(24);

        long totalCars = carRepository.count();

        long bazosCars = carRepository.countBySource("BAZOS");
        long sautoCars = carRepository.countBySource("SAUTO");
        long tipCars = carRepository.countBySource("TIPCARS");

        long newLast24h = carRepository.countByCreatedAtAfter(last24h);

        long bazosLast24h = carRepository.countBySourceAndCreatedAtAfter("BAZOS", last24h);
        long sautoLast24h = carRepository.countBySourceAndCreatedAtAfter("SAUTO", last24h);
        long tipCarsLast24h = carRepository.countBySourceAndCreatedAtAfter("TIPCARS", last24h);

        long usersTotal = subscriberService.countAllSubscribers();
        long usersActive = subscriberService.countActiveSubscribers();
        long favoritesTotal = favoriteCarService.countAllFavorites();

        List<CarEntity> latestCars = carRepository.findTop5ByOrderByCreatedAtDesc();

        StringBuilder latest = new StringBuilder();

        if (latestCars.isEmpty()) {
            latest.append("—");
        } else {
            for (CarEntity car : latestCars) {
                latest.append("• ")
                        .append(formatSource(car.getSource()))
                        .append(" — ")
                        .append(formatTitleForAdmin(car.getTitle()))
                        .append("\n");
            }
        }

        String text = """
        🛠 Admin panel

        ✅ Bot status: running

        👥 Users total: %d
        🔔 Active subscriptions: %d
        ⭐ Favorites saved: %d

        🚗 Cars in DB: %d
        🆕 New last 24h: %d

        📦 Sources total:
        • Bazoš.cz: %d
        • Sauto.cz: %d
        • TipCars.cz: %d

        🕒 Sources last 24h:
        • Bazoš.cz: %d
        • Sauto.cz: %d
        • TipCars.cz: %d

        🧾 Latest cars:
        %s
        """.formatted(
                usersTotal,
                usersActive,
                favoritesTotal,
                totalCars,
                newLast24h,
                bazosCars,
                sautoCars,
                tipCars,
                bazosLast24h,
                sautoLast24h,
                tipCarsLast24h,
                latest.toString().trim()
        );

        sendMessage(chatId, text);
    }

    private boolean isAdmin(Long chatId) {
        return chatId != null && adminChatIds.contains(chatId);
    }

    private Set<Long> parseAdminChatIds(String raw) {
        Set<Long> result = new LinkedHashSet<>();

        if (raw == null || raw.isBlank()) {
            return result;
        }

        for (String part : raw.split(",")) {
            try {
                result.add(Long.parseLong(part.trim()));
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private String formatTitleForAdmin(String title) {
        if (title == null || title.isBlank()) {
            return "-";
        }

        String cleaned = title.replaceAll("\\s+", " ").trim();

        if (cleaned.length() <= 45) {
            return cleaned;
        }

        return cleaned.substring(0, 42).trim() + "...";
    }
}
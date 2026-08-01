package com.yourapp.carbot.bot;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.TelegramSubscriberEntity;
import com.yourapp.carbot.entity.UserFilterEntity;
import com.yourapp.carbot.i18n.MessageService;
import com.yourapp.carbot.repository.CarRepository;
import com.yourapp.carbot.service.CarFilterMatcher;
import com.yourapp.carbot.service.CarSearchService;
import com.yourapp.carbot.service.FavoriteCarService;
import com.yourapp.carbot.service.ParserRunStatsService;
import com.yourapp.carbot.service.TelegramSubscriberService;
import com.yourapp.carbot.service.UserFilterService;
import com.yourapp.carbot.service.UserStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CarTelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(CarTelegramBot.class);

    private final TelegramClient telegramClient;
    private final String botToken;

    private final TelegramSubscriberService subscriberService;
    private final UserStateService userStateService;
    private final UserFilterService userFilterService;
    private final CarRepository carRepository;
    private final CarSearchService carSearchService;
    private final CarFilterMatcher carFilterMatcher;
    private final FavoriteCarService favoriteCarService;
    private final ParserRunStatsService parserRunStatsService;
    private final CarBotKeyboardFactory keyboardFactory;
    private final MessageService messages;

    private final Map<Long, SearchSession> searchSessions = new ConcurrentHashMap<>();
    private final Map<Long, SellDraft> sellDrafts = new ConcurrentHashMap<>();
    private final Map<Long, ListingEditSession> listingEditSessions = new ConcurrentHashMap<>();
    private final Set<Long> editingFilterSessions = ConcurrentHashMap.newKeySet();
    private final Set<Long> adminChatIds;

    public CarTelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            TelegramSubscriberService subscriberService,
            UserStateService userStateService,
            UserFilterService userFilterService,
            CarRepository carRepository,
            CarSearchService carSearchService,
            CarFilterMatcher carFilterMatcher,
            FavoriteCarService favoriteCarService,
            ParserRunStatsService parserRunStatsService,
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
        this.carFilterMatcher = carFilterMatcher;
        this.favoriteCarService = favoriteCarService;
        this.parserRunStatsService = parserRunStatsService;
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

            if (update.hasMessage() && update.getMessage().hasPhoto()) {
                Long chatId = update.getMessage().getChatId();
                String username = update.getMessage().getFrom() != null
                        ? update.getMessage().getFrom().getUserName()
                        : null;

                BotStep step = userStateService.getStep(chatId);
                if (step == BotStep.SELL_PHOTO) {
                    handleSellPhoto(chatId, username, update.getMessage().getPhoto());
                    return;
                }
                if (step == BotStep.SELL_EDIT_VALUE) {
                    handleListingEditPhoto(chatId, update.getMessage().getPhoto());
                    return;
                }
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

            if (userStateService.getStep(chatId) == BotStep.SELL_EDIT_VALUE) {
                handleListingEditText(chatId, text);
                return;
            }

            if (isSellStep(userStateService.getStep(chatId))) {
                handleSellText(chatId, username, text);
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
                .replace("🧰 ", "")
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

        if (normalized.equals(messages.get(lang, "menu.services")) || normalized.equals(messages.get(lang, "menu.latest")) || text.equals("/services") || text.equals("/latest")) {
            showServices(chatId);
            return true;
        }

        if (normalized.equals(messages.get(lang, "menu.favorites")) || text.equals("/favorites")) {
            handleFavorites(chatId);
            return true;
        }

        if (isSellMenuText(normalized) || text.equals("/sell")) {
            startSellFlowSafely(chatId);
            return true;
        }

        if (isMyCarsMenuText(normalized) || text.equals("/mycars")) {
            handleMyCars(chatId);
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
            case "/latest", "/services" -> showServices(chatId);
            case "/find" -> handleFind(chatId);
            case "/favorites" -> handleFavorites(chatId);
            case "/sell" -> startSellFlowSafely(chatId);
            case "/mycars" -> handleMyCars(chatId);
            case "/skip" -> {
                BotStep step = userStateService.getStep(chatId);
                if (step == BotStep.SELL_PHOTO) {
                    handleSellText(chatId, username, text);
                } else if (step == BotStep.SELL_EDIT_VALUE) {
                    handleListingEditText(chatId, text);
                } else {
                    sendMessage(
                            chatId,
                            messages.get(lang(chatId), "command.unknown"),
                            keyboardFactory.mainMenuKeyboard(lang(chatId))
                    );
                }
            }
            case "/cancel" -> {
                if (isSellStep(userStateService.getStep(chatId))) {
                    cancelSellFlow(chatId);
                } else if (userStateService.getStep(chatId) == BotStep.SELL_EDIT_VALUE) {
                    listingEditSessions.remove(chatId);
                    userStateService.reset(chatId);
                    sendMessage(chatId, sellCancelledText(chatId), keyboardFactory.mainMenuKeyboard(lang(chatId)));
                } else {
                    sendMessage(
                            chatId,
                            messages.get(lang(chatId), "command.unknown"),
                            keyboardFactory.mainMenuKeyboard(lang(chatId))
                    );
                }
            }
            case "/help" -> handleHelp(chatId);
            case "/filter" -> startNewFilterSetup(chatId);
            case "/myfilter" -> showCurrentFilter(chatId);
            case "/resetfilter" -> confirmResetFilter(chatId);
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

    private boolean isEditingStep(BotStep step) {
        return step == BotStep.EDITING_CAR_TYPE
                || step == BotStep.EDITING_BRAND
                || step == BotStep.EDITING_MAX_PRICE
                || step == BotStep.EDITING_LOCATION
                || step == BotStep.EDITING_MAX_MILEAGE
                || step == BotStep.EDITING_TRANSMISSION
                || step == BotStep.EDITING_FUEL_TYPE
                || step == BotStep.EDITING_YEAR_FROM;
    }
    private boolean isFilterEditFlow(Long chatId) {
        return editingFilterSessions.contains(chatId) || isEditingStep(userStateService.getStep(chatId));
    }

    private void startNewFilterSetup(Long chatId) {
        String currentLang = lang(chatId);

        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        filter.setLanguageCode(currentLang);
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

    private void showFilterEditMenu(Long chatId) {
        UserFilterEntity filter = userFilterService.findByChatId(chatId).orElse(null);

        if (!isFilterConfigured(filter)) {
            editFilterSetup(chatId);
            return;
        }

        editingFilterSessions.add(chatId);
        sendFilterEditFieldsMenu(chatId);
    }

    private void sendFilterEditFieldsMenu(Long chatId) {
        String currentLang = lang(chatId);

        sendMessage(
                chatId,
                "✏️ " + messages.get(currentLang, "button.editFilter"),
                keyboardFactory.myFilterEditFieldsKeyboard(currentLang)
        );
    }

    private void finishEditField(Long chatId) {
        userStateService.setStep(chatId, BotStep.NONE);
        editingFilterSessions.remove(chatId);
        sendFilterEditFieldsMenu(chatId);
    }

    private void handleCallback(Update update) throws Exception {
        String data = update.getCallbackQuery().getData();
        data = data == null ? "" : data.trim();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        log.info("BOT CALLBACK chatId={} data={}", chatId, data);
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
            showFilterEditMenu(chatId);
            return;
        }

        if (data.startsWith("myfilter_field:")) {
            handleEditFieldSafely(chatId, data.substring("myfilter_field:".length()));
            return;
        }

        if (data.startsWith("edit_field:")) {
            handleEditFieldSafely(chatId, data.substring("edit_field:".length()));
            return;
        }

        if ("myfilter_reset".equals(data)) {
            confirmResetFilter(chatId);
            return;
        }

        if ("myfilter_reset_confirm".equals(data)) {
            resetFilter(chatId);
            return;
        }

        if ("myfilter_reset_cancel".equals(data)) {
            showCurrentFilter(chatId);
            return;
        }

        if ("notif_settings".equals(data)) {
            showNotificationSettings(chatId);
            return;
        }

        if ("notif_pause_toggle".equals(data)) {
            subscriberService.toggleNotificationsPaused(chatId);
            showNotificationSettings(chatId);
            return;
        }

        if (data.startsWith("notif_mode:")) {
            subscriberService.setNotificationMode(chatId, data.substring("notif_mode:".length()));
            showNotificationSettings(chatId);
            return;
        }

        if (data.startsWith("notif_limit:")) {
            subscriberService.setDailyNotificationLimit(chatId, Integer.parseInt(data.substring("notif_limit:".length())));
            showNotificationSettings(chatId);
            return;
        }

        if ("show_myfilter".equals(data)) {
            showCurrentFilter(chatId);
            return;
        }

        if (isSellStartCallback(data)) {
            startSellFlowSafely(chatId);
            return;
        }

        if (isMyCarsCallback(data)) {
            handleMyCars(chatId);
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

        if ("sell_confirm".equals(data)) {
            submitSellDraft(chatId);
            return;
        }

        if ("sell_cancel".equals(data)) {
            cancelSellFlow(chatId);
            return;
        }

        if (data.startsWith("sell_remove:")) {
            handleRemoveUserListing(chatId, data.substring("sell_remove:".length()));
            return;
        }

        if (data.startsWith("sell_delete:")) {
            handleDeleteUserListing(chatId, data.substring("sell_delete:".length()));
            return;
        }

        if (data.startsWith("sell_edit_menu:")) {
            handleListingEditMenu(chatId, data.substring("sell_edit_menu:".length()));
            return;
        }

        if (data.startsWith("sell_edit:")) {
            handleListingEditStart(chatId, data.substring("sell_edit:".length()));
            return;
        }

        if (data.startsWith("sell_admin_approve:")) {
            handleAdminReview(chatId, data.substring("sell_admin_approve:".length()), true);
            return;
        }

        if (data.startsWith("sell_admin_reject:")) {
            handleAdminReview(chatId, data.substring("sell_admin_reject:".length()), false);
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
            return;
        }

        String legacyEditField = legacyEditField(data);
        if (legacyEditField != null) {
            handleEditFieldSafely(chatId, legacyEditField);
            return;
        }

        log.warn("BOT CALLBACK unknown chatId={} data={}", chatId, data);
        showCurrentFilter(chatId);
    }

    private String legacyEditField(String data) {
        return switch (data) {
            case "edit_car_type", "edit:car_type", "filter_edit:car_type" -> "car_type";
            case "edit_brand", "edit:brand", "filter_edit:brand" -> "brand";
            case "edit_max_price", "edit:price", "edit:max_price", "filter_edit:max_price" -> "max_price";
            case "edit_location", "edit:location", "filter_edit:location" -> "location";
            case "edit_max_mileage", "edit:mileage", "edit:max_mileage", "filter_edit:max_mileage" -> "max_mileage";
            case "edit_transmission", "edit:transmission", "filter_edit:transmission" -> "transmission";
            case "edit_fuel_type", "edit:fuel_type", "filter_edit:fuel_type" -> "fuel_type";
            case "edit_year_from", "edit:year_from", "filter_edit:year_from" -> "year_from";
            default -> null;
        };
    }
    private void handleWizardBack(Long chatId, String targetStep) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        String lang = lang(chatId);

        if (isFilterEditFlow(chatId)) {
            finishEditField(chatId);
            return;
        }

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

    private void handleEditFieldSafely(Long chatId, String field) {
        try {
            log.info("BOT FILTER FIELD chatId={} field={}", chatId, field);
            handleEditField(chatId, field);
        } catch (Exception e) {
            log.error("BOT FILTER FIELD failed chatId={} field={}", chatId, field, e);
            sendMessage(
                    chatId,
                    "Не смог открыть настройку фильтра. Поле: " + field,
                    keyboardFactory.myFilterEditFieldsKeyboard(lang(chatId))
            );
        }
    }
    private void handleEditField(Long chatId, String field) {
        UserFilterEntity filter = userFilterService.getOrCreate(chatId);
        String lang = lang(chatId);
        editingFilterSessions.add(chatId);

        switch (field) {
            case "car_type" -> {
                sendMessage(
                        chatId,
                        buildCarTypeSelectionText(chatId, filter.getCarType()),
                        keyboardFactory.carTypeKeyboard(lang, filter.getCarType(), true)
                );
            }
            case "brand" -> {
                sendMessage(
                        chatId,
                        buildBrandSelectionText(chatId, parseValues(filter.getBrand())),
                        keyboardFactory.brandKeyboard(lang, filter.getBrand(), true)
                );
            }
            case "max_price" -> {
                sendMessage(
                        chatId,
                        messages.get(lang, "price.choose") + "\n\n" + buildFilterProgress(filter),
                        keyboardFactory.maxPriceKeyboard(lang, true)
                );
            }
            case "location" -> {
                sendMessage(
                        chatId,
                        messages.get(lang, "location.choose") + "\n\n" + buildFilterProgress(filter),
                        keyboardFactory.locationKeyboard(lang, true)
                );
            }
            case "max_mileage" -> {
                sendMessage(
                        chatId,
                        messages.get(lang, "mileage.choose") + "\n\n" + buildFilterProgress(filter),
                        keyboardFactory.mileageKeyboard(lang, true)
                );
            }
            case "transmission" -> {
                sendMessage(
                        chatId,
                        messages.get(lang, "transmission.choose") + "\n\n" + buildFilterProgress(filter),
                        keyboardFactory.transmissionKeyboard(lang, true)
                );
            }
            case "fuel_type" -> {
                sendMessage(
                        chatId,
                        messages.get(lang, "fuelType.choose") + "\n\n" + buildFilterProgress(filter),
                        keyboardFactory.fuelTypeKeyboard(lang, true)
                );
            }
            case "year_from" -> {
                sendMessage(
                        chatId,
                        messages.get(lang, "yearFrom.choose") + "\n\n" + buildFilterProgress(filter),
                        keyboardFactory.yearFromKeyboard(lang, true)
                );
            }
            default -> showCurrentFilter(chatId);
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
        if (!isFilterEditFlow(chatId)) {
            filter.setBrand(null);
        }
        userFilterService.save(filter);

        if (isFilterEditFlow(chatId)) {
            finishEditField(chatId);
            return;
        }

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

        if (!isFilterEditFlow(chatId)) {
            filter.setBrand(null);
        }
        userFilterService.save(filter);

        if (isFilterEditFlow(chatId)) {
            finishEditField(chatId);
            return;
        }

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

        if (isFilterEditFlow(chatId)) {
            finishEditField(chatId);
            return;
        }

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

        if (isFilterEditFlow(chatId)) {
            finishEditField(chatId);
            return;
        }

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

        if (isFilterEditFlow(chatId)) {
            finishEditField(chatId);
            return;
        }

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

        if (isFilterEditFlow(chatId)) {
            finishEditField(chatId);
            return;
        }

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

        if (isFilterEditFlow(chatId)) {
            finishEditField(chatId);
            return;
        }

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

        if (isFilterEditFlow(chatId)) {
            finishEditField(chatId);
            return;
        }

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

        if (isFilterEditFlow(chatId)) {
            finishEditField(chatId);
            return;
        }

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

        if (isFilterEditFlow(chatId)) {
            finishEditField(chatId);
            return;
        }

        userStateService.setStep(chatId, BotStep.COMPLETED);

        sendMessage(
                chatId,
                buildFilterSummary(filter) + "\n\n" + messages.get(lang(chatId), "filter.saved.next"),
                keyboardFactory.myFilterActionsKeyboard(lang(chatId))
        );
    }

    private void showServices(Long chatId) {
        sendMessage(
                chatId,
                messages.get(lang(chatId), "services.text"),
                keyboardFactory.servicesKeyboard(lang(chatId))
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

    private void startSellFlow(Long chatId) {
        SellDraft draft = new SellDraft();
        draft.ownerChatId = chatId;
        sellDrafts.put(chatId, draft);
        userStateService.setStep(chatId, BotStep.SELL_BRAND);

        sendMessage(chatId, sellPrompt(chatId, "brand"), keyboardFactory.mainMenuKeyboard(lang(chatId)));
    }

    private void startSellFlowSafely(Long chatId) {
        try {
            startSellFlow(chatId);
        } catch (Exception e) {
            log.error("SELL FLOW start failed chatId={}", chatId, e);
            sendMessage(chatId, switch (lang(chatId)) {
                case "ru" -> "Не удалось запустить добавление авто. Попробуйте команду /sell.";
                case "uk" -> "Не вдалося запустити додавання авто. Спробуйте команду /sell.";
                case "cs" -> "Nepodařilo se spustit přidání auta. Zkuste příkaz /sell.";
                default -> "Could not start car listing. Try /sell.";
            });
        }
    }

    private void handleSellText(Long chatId, String username, String text) {
        if ("/cancel".equalsIgnoreCase(text)) {
            cancelSellFlow(chatId);
            return;
        }

        SellDraft draft = sellDrafts.computeIfAbsent(chatId, key -> {
            SellDraft created = new SellDraft();
            created.ownerChatId = chatId;
            return created;
        });
        draft.sellerUsername = username;

        BotStep step = userStateService.getStep(chatId);

        switch (step) {
            case SELL_BRAND -> {
                draft.brand = normalizeSellBrand(text);
                userStateService.setStep(chatId, BotStep.SELL_TITLE);
                sendMessage(chatId, sellPrompt(chatId, "title"));
            }
            case SELL_TITLE -> {
                draft.title = limitText(text, 180);
                userStateService.setStep(chatId, BotStep.SELL_PRICE);
                sendMessage(chatId, sellPrompt(chatId, "price"));
            }
            case SELL_PRICE -> {
                Integer price = parsePositiveInt(text);
                if (price == null || price < 1000 || price > 20_000_000) {
                    sendMessage(chatId, sellInvalid(chatId, "price"));
                    return;
                }
                draft.priceValue = price;
                userStateService.setStep(chatId, BotStep.SELL_YEAR);
                sendMessage(chatId, sellPrompt(chatId, "year"));
            }
            case SELL_YEAR -> {
                Integer year = parsePositiveInt(text);
                int currentYear = LocalDateTime.now().getYear() + 1;
                if (year == null || year < 1980 || year > currentYear) {
                    sendMessage(chatId, sellInvalid(chatId, "year"));
                    return;
                }
                draft.year = year;
                userStateService.setStep(chatId, BotStep.SELL_MILEAGE);
                sendMessage(chatId, sellPrompt(chatId, "mileage"));
            }
            case SELL_MILEAGE -> {
                Integer mileage = parsePositiveInt(text);
                if (mileage == null || mileage < 0 || mileage > 1_000_000) {
                    sendMessage(chatId, sellInvalid(chatId, "mileage"));
                    return;
                }
                draft.mileage = mileage;
                userStateService.setStep(chatId, BotStep.SELL_LOCATION);
                sendMessage(chatId, sellPrompt(chatId, "location"));
            }
            case SELL_LOCATION -> {
                draft.location = limitText(text, 120);
                userStateService.setStep(chatId, BotStep.SELL_FUEL_TYPE);
                sendMessage(chatId, sellPrompt(chatId, "fuel"));
            }
            case SELL_FUEL_TYPE -> {
                draft.fuelType = normalizeSellFuel(text);
                if (draft.fuelType == null) {
                    sendMessage(chatId, sellInvalid(chatId, "fuel"));
                    return;
                }
                userStateService.setStep(chatId, BotStep.SELL_TRANSMISSION);
                sendMessage(chatId, sellPrompt(chatId, "transmission"));
            }
            case SELL_TRANSMISSION -> {
                draft.transmission = normalizeSellTransmission(text);
                if (draft.transmission == null) {
                    sendMessage(chatId, sellInvalid(chatId, "transmission"));
                    return;
                }
                userStateService.setStep(chatId, BotStep.SELL_CAR_TYPE);
                sendMessage(chatId, sellPrompt(chatId, "carType"));
            }
            case SELL_CAR_TYPE -> {
                draft.carType = normalizeSellCarType(text);
                if (draft.carType == null) {
                    sendMessage(chatId, sellInvalid(chatId, "carType"));
                    return;
                }
                userStateService.setStep(chatId, BotStep.SELL_CONTACT);
                sendMessage(chatId, sellPrompt(chatId, "contact"));
            }
            case SELL_CONTACT -> {
                draft.sellerContact = limitText(text, 180);
                userStateService.setStep(chatId, BotStep.SELL_PHOTO);
                sendMessage(chatId, sellPrompt(chatId, "photo"));
            }
            case SELL_PHOTO -> {
                if ("/skip".equalsIgnoreCase(text) || "skip".equalsIgnoreCase(text)) {
                    userStateService.setStep(chatId, BotStep.SELL_CONFIRM);
                    sendMessage(chatId, buildSellPreview(chatId, draft), keyboardFactory.sellConfirmKeyboard(lang(chatId)));
                    return;
                }
                sendMessage(chatId, sellInvalid(chatId, "photo"));
            }
            case SELL_CONFIRM -> {
                userStateService.setStep(chatId, BotStep.SELL_CONFIRM);
                sendMessage(chatId, buildSellPreview(chatId, draft), keyboardFactory.sellConfirmKeyboard(lang(chatId)));
            }
            default -> {
                userStateService.reset(chatId);
                sellDrafts.remove(chatId);
            }
        }
    }

    private void handleSellPhoto(Long chatId, String username, List<PhotoSize> photos) {
        SellDraft draft = sellDrafts.computeIfAbsent(chatId, key -> {
            SellDraft created = new SellDraft();
            created.ownerChatId = chatId;
            return created;
        });
        draft.sellerUsername = username;

        PhotoSize bestPhoto = photos == null ? null : photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize, Comparator.nullsFirst(Integer::compareTo)))
                .orElse(null);

        if (bestPhoto == null || bestPhoto.getFileId() == null || bestPhoto.getFileId().isBlank()) {
            sendMessage(chatId, sellInvalid(chatId, "photo"));
            return;
        }

        draft.imageUrl = bestPhoto.getFileId();
        userStateService.setStep(chatId, BotStep.SELL_CONFIRM);
        sendMessage(chatId, buildSellPreview(chatId, draft), keyboardFactory.sellConfirmKeyboard(lang(chatId)));
    }

    private void submitSellDraft(Long chatId) {
        SellDraft draft = sellDrafts.get(chatId);
        if (draft == null || !draft.isComplete()) {
            startSellFlow(chatId);
            return;
        }

        CarEntity car = new CarEntity();
        car.setSource("USER");
        car.setTitle(buildUserListingTitle(draft));
        car.setPrice(formatCzk(draft.priceValue));
        car.setPriceValue(draft.priceValue);
        car.setLocation(draft.location);
        car.setUrl("user://" + chatId + "/" + UUID.randomUUID());
        car.setBrand(draft.brand);
        car.setYear(draft.year);
        car.setMileage(draft.mileage);
        car.setFuelType(draft.fuelType);
        car.setTransmission(draft.transmission);
        car.setCarType(draft.carType);
        car.setOwnerChatId(chatId);
        car.setSellerUsername(draft.sellerUsername);
        car.setSellerContact(draft.sellerContact);
        car.setImageUrl(draft.imageUrl);
        car.setListingStatus("PENDING");
        car.setDescription(draft.description);
        car.setCreatedAt(LocalDateTime.now());

        CarEntity saved = carRepository.save(car);
        sellDrafts.remove(chatId);
        userStateService.reset(chatId);

        sendMessage(chatId, sellSubmittedText(chatId), keyboardFactory.mainMenuKeyboard(lang(chatId)));
        notifyAdminsAboutUserListing(saved);
    }

    private void cancelSellFlow(Long chatId) {
        sellDrafts.remove(chatId);
        userStateService.reset(chatId);
        sendMessage(chatId, sellCancelledText(chatId), keyboardFactory.mainMenuKeyboard(lang(chatId)));
    }

    private void handleMyCars(Long chatId) {
        List<CarEntity> cars = carRepository.findTop50ByOwnerChatIdOrderByCreatedAtDesc(chatId);
        if (cars.isEmpty()) {
            sendMessage(chatId, myCarsEmptyText(chatId), keyboardFactory.mainMenuKeyboard(lang(chatId)));
            return;
        }

        for (CarEntity car : cars) {
            sendCarMessage(
                    chatId,
                    car,
                    formatUserListing(chatId, car),
                    keyboardFactory.userListingKeyboard(lang(chatId), car.getId(), car.getListingStatus())
            );
        }
    }

    private void handleRemoveUserListing(Long chatId, String carIdValue) {
        Long carId = parseLong(carIdValue);
        if (carId == null) {
            return;
        }

        carRepository.findById(carId).ifPresent(car -> {
            if (!Objects.equals(car.getOwnerChatId(), chatId)) {
                sendMessage(chatId, "This listing belongs to another user.");
                return;
            }

            car.setListingStatus("INACTIVE");
            carRepository.save(car);
            sendMessage(chatId, listingRemovedText(chatId), keyboardFactory.mainMenuKeyboard(lang(chatId)));
        });
    }

    private void handleDeleteUserListing(Long chatId, String carIdValue) {
        Long carId = parseLong(carIdValue);
        if (carId == null) {
            return;
        }

        carRepository.findById(carId).ifPresent(car -> {
            if (!Objects.equals(car.getOwnerChatId(), chatId)) {
                sendMessage(chatId, "This listing belongs to another user.");
                return;
            }

            carRepository.delete(car);
            sendMessage(chatId, listingDeletedText(chatId), keyboardFactory.mainMenuKeyboard(lang(chatId)));
        });
    }

    private void handleListingEditMenu(Long chatId, String carIdValue) {
        Long carId = parseLong(carIdValue);
        if (carId == null) {
            return;
        }

        carRepository.findById(carId).ifPresent(car -> {
            if (!Objects.equals(car.getOwnerChatId(), chatId)) {
                sendMessage(chatId, "This listing belongs to another user.");
                return;
            }

            sendMessage(chatId, editListingMenuText(chatId), keyboardFactory.userListingEditKeyboard(lang(chatId), carId));
        });
    }

    private void handleListingEditStart(Long chatId, String value) {
        String[] parts = value.split(":", 2);
        if (parts.length != 2) {
            return;
        }

        Long carId = parseLong(parts[0]);
        String field = parts[1];
        if (carId == null || !isEditableListingField(field)) {
            return;
        }

        carRepository.findById(carId).ifPresent(car -> {
            if (!Objects.equals(car.getOwnerChatId(), chatId)) {
                sendMessage(chatId, "This listing belongs to another user.");
                return;
            }

            listingEditSessions.put(chatId, new ListingEditSession(carId, field));
            userStateService.setStep(chatId, BotStep.SELL_EDIT_VALUE);
            sendMessage(chatId, editListingPrompt(chatId, field));
        });
    }

    private void handleListingEditText(Long chatId, String text) {
        ListingEditSession session = listingEditSessions.get(chatId);
        if (session == null) {
            userStateService.reset(chatId);
            return;
        }

        if ("/cancel".equalsIgnoreCase(text)) {
            listingEditSessions.remove(chatId);
            userStateService.reset(chatId);
            sendMessage(chatId, sellCancelledText(chatId), keyboardFactory.mainMenuKeyboard(lang(chatId)));
            return;
        }

        if ("photo".equals(session.field())) {
            if ("/skip".equalsIgnoreCase(text) || "skip".equalsIgnoreCase(text)) {
                applyListingEdit(chatId, session, null);
                return;
            }
            sendMessage(chatId, sellInvalid(chatId, "photo"));
            return;
        }

        applyListingEdit(chatId, session, text);
    }

    private void handleListingEditPhoto(Long chatId, List<PhotoSize> photos) {
        ListingEditSession session = listingEditSessions.get(chatId);
        if (session == null || !"photo".equals(session.field())) {
            sendMessage(chatId, sellInvalid(chatId, "photo"));
            return;
        }

        PhotoSize bestPhoto = photos == null ? null : photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize, Comparator.nullsFirst(Integer::compareTo)))
                .orElse(null);

        if (bestPhoto == null || bestPhoto.getFileId() == null || bestPhoto.getFileId().isBlank()) {
            sendMessage(chatId, sellInvalid(chatId, "photo"));
            return;
        }

        applyListingEdit(chatId, session, bestPhoto.getFileId());
    }

    private void applyListingEdit(Long chatId, ListingEditSession session, String value) {
        carRepository.findById(session.carId()).ifPresent(car -> {
            if (!Objects.equals(car.getOwnerChatId(), chatId)) {
                sendMessage(chatId, "This listing belongs to another user.");
                return;
            }

            boolean updated = updateListingField(chatId, car, session.field(), value);
            if (!updated) {
                return;
            }

            car.setListingStatus("PENDING");
            car.setCreatedAt(LocalDateTime.now());
            carRepository.save(car);
            listingEditSessions.remove(chatId);
            userStateService.reset(chatId);

            sendMessage(chatId, listingUpdatedText(chatId));
            sendCarMessage(
                    chatId,
                    car,
                    formatUserListing(chatId, car),
                    keyboardFactory.userListingKeyboard(lang(chatId), car.getId(), car.getListingStatus())
            );
            notifyAdminsAboutUserListing(car);
        });
    }

    private void handleAdminReview(Long adminChatId, String carIdValue, boolean approve) {
        if (!isAdmin(adminChatId)) {
            sendMessage(adminChatId, "Admin access denied.");
            return;
        }

        Long carId = parseLong(carIdValue);
        if (carId == null) {
            return;
        }

        carRepository.findById(carId).ifPresent(car -> {
            if (!"PENDING".equalsIgnoreCase(car.getListingStatus())) {
                sendMessage(adminChatId, "Listing already reviewed: " + safe(car.getListingStatus()));
                return;
            }

            car.setListingStatus(approve ? "ACTIVE" : "REJECTED");
            if (approve) {
                car.setCreatedAt(LocalDateTime.now());
            }
            carRepository.save(car);

            sendMessage(adminChatId, approve ? "Approved." : "Rejected.");
            if (car.getOwnerChatId() != null) {
                log.info("USER LISTING notifying ownerChatId={} carId={} status={}",
                        car.getOwnerChatId(), car.getId(), car.getListingStatus());
                sendMessage(car.getOwnerChatId(), approve ? listingApprovedText(car.getOwnerChatId()) : listingRejectedText(car.getOwnerChatId()));
            }
        });
    }

    private void notifyAdminsAboutUserListing(CarEntity car) {
        if (adminChatIds.isEmpty()) {
            log.warn("USER LISTING pending but no admins configured. carId={}", car.getId());
            return;
        }

        String text = """
                New user car listing

                ID: %s
                Owner chat: %s
                Contact: %s

                %s
                """.formatted(
                car.getId(),
                car.getOwnerChatId(),
                safe(car.getSellerContact()),
                formatUserListing("en", car)
        );

        for (Long adminChatId : adminChatIds) {
            log.info("USER LISTING notifying adminChatId={} carId={}", adminChatId, car.getId());
            sendCarMessage(adminChatId, car, text, keyboardFactory.sellAdminReviewKeyboard(car.getId()));
        }
    }

    private boolean isSellStep(BotStep step) {
        return step == BotStep.SELL_BRAND
                || step == BotStep.SELL_TITLE
                || step == BotStep.SELL_PRICE
                || step == BotStep.SELL_YEAR
                || step == BotStep.SELL_MILEAGE
                || step == BotStep.SELL_LOCATION
                || step == BotStep.SELL_FUEL_TYPE
                || step == BotStep.SELL_TRANSMISSION
                || step == BotStep.SELL_CAR_TYPE
                || step == BotStep.SELL_CONTACT
                || step == BotStep.SELL_PHOTO
                || step == BotStep.SELL_CONFIRM;
    }

    private boolean isSellMenuText(String text) {
        return containsIgnoreCase(text, "продать")
                || containsIgnoreCase(text, "продати")
                || containsIgnoreCase(text, "prodat")
                || containsIgnoreCase(text, "sell car");
    }

    private boolean isMyCarsMenuText(String text) {
        return containsIgnoreCase(text, "мои авто")
                || containsIgnoreCase(text, "мої авто")
                || containsIgnoreCase(text, "moje auta")
                || containsIgnoreCase(text, "my cars");
    }

    private boolean isSellStartCallback(String data) {
        return "sell_start".equals(data)
                || "sell".equals(data)
                || "/sell".equals(data);
    }

    private boolean isMyCarsCallback(String data) {
        return "sell_mycars".equals(data)
                || "mycars".equals(data)
                || "/mycars".equals(data);
    }

    private boolean containsIgnoreCase(String source, String value) {
        return source != null && value != null && source.toLowerCase().contains(value.toLowerCase());
    }

    private String sellPrompt(Long chatId, String field) {
        String lang = lang(chatId);
        return switch (field) {
            case "brand" -> switch (lang) {
                case "ru" -> "🚗 Продажа авто\n\nВведите марку, например: Skoda";
                case "uk" -> "🚗 Продаж авто\n\nВведіть марку, наприклад: Skoda";
                case "cs" -> "🚗 Prodej auta\n\nZadejte značku, např.: Skoda";
                default -> "🚗 Sell car\n\nEnter brand, for example: Skoda";
            };
            case "title" -> switch (lang) {
                case "ru" -> "Введите модель и короткое описание, например: Octavia 1.6 TDI Ambition";
                case "uk" -> "Введіть модель і короткий опис, наприклад: Octavia 1.6 TDI Ambition";
                case "cs" -> "Zadejte model a krátký popis, např.: Octavia 1.6 TDI Ambition";
                default -> "Enter model and short description, for example: Octavia 1.6 TDI Ambition";
            };
            case "price" -> switch (lang) {
                case "ru" -> "Введите цену в Kč, только число. Например: 250000";
                case "uk" -> "Введіть ціну в Kč, тільки число. Наприклад: 250000";
                case "cs" -> "Zadejte cenu v Kč, pouze číslo. Např.: 250000";
                default -> "Enter price in Kč, number only. Example: 250000";
            };
            case "year" -> switch (lang) {
                case "ru" -> "Введите год выпуска. Например: 2018";
                case "uk" -> "Введіть рік випуску. Наприклад: 2018";
                case "cs" -> "Zadejte rok výroby. Např.: 2018";
                default -> "Enter production year. Example: 2018";
            };
            case "mileage" -> switch (lang) {
                case "ru" -> "Введите пробег в км, только число. Например: 145000";
                case "uk" -> "Введіть пробіг у км, тільки число. Наприклад: 145000";
                case "cs" -> "Zadejte nájezd v km, pouze číslo. Např.: 145000";
                default -> "Enter mileage in km, number only. Example: 145000";
            };
            case "location" -> switch (lang) {
                case "ru" -> "Введите город или регион. Например: Praha";
                case "uk" -> "Введіть місто або регіон. Наприклад: Praha";
                case "cs" -> "Zadejte město nebo kraj. Např.: Praha";
                default -> "Enter city or region. Example: Praha";
            };
            case "fuel" -> switch (lang) {
                case "ru" -> "Введите топливо: petrol, diesel, hybrid, plugin, electric, lpg, cng";
                case "uk" -> "Введіть пальне: petrol, diesel, hybrid, plugin, electric, lpg, cng";
                case "cs" -> "Zadejte palivo: petrol, diesel, hybrid, plugin, electric, lpg, cng";
                default -> "Enter fuel: petrol, diesel, hybrid, plugin, electric, lpg, cng";
            };
            case "transmission" -> switch (lang) {
                case "ru" -> "Введите коробку: manual или automatic";
                case "uk" -> "Введіть коробку: manual або automatic";
                case "cs" -> "Zadejte převodovku: manual nebo automatic";
                default -> "Enter transmission: manual or automatic";
            };
            case "carType" -> switch (lang) {
                case "ru" -> "Введите кузов: hatchback, sedan, wagon, suv, minivan, coupe, cabrio, pickup";
                case "uk" -> "Введіть кузов: hatchback, sedan, wagon, suv, minivan, coupe, cabrio, pickup";
                case "cs" -> "Zadejte karoserii: hatchback, sedan, wagon, suv, minivan, coupe, cabrio, pickup";
                default -> "Enter body type: hatchback, sedan, wagon, suv, minivan, coupe, cabrio, pickup";
            };
            case "contact" -> switch (lang) {
                case "ru" -> "Введите контакт для покупателя: телефон или @username";
                case "uk" -> "Введіть контакт для покупця: телефон або @username";
                case "cs" -> "Zadejte kontakt pro kupujícího: telefon nebo @username";
                default -> "Enter buyer contact: phone or @username";
            };
            case "photo" -> switch (lang) {
                case "ru" -> "Пришлите одно фото авто или напишите /skip, чтобы пропустить.";
                case "uk" -> "Надішліть одне фото авто або напишіть /skip, щоб пропустити.";
                case "cs" -> "Pošlete jednu fotku auta nebo napište /skip pro přeskočení.";
                default -> "Send one car photo or type /skip to skip.";
            };
            default -> "/cancel";
        } + "\n\n/cancel";
    }

    private String sellInvalid(Long chatId, String field) {
        String lang = lang(chatId);
        return switch (lang) {
            case "ru" -> "Не похоже на корректное значение. Попробуйте ещё раз.\n\n" + sellPrompt(chatId, field);
            case "uk" -> "Не схоже на коректне значення. Спробуйте ще раз.\n\n" + sellPrompt(chatId, field);
            case "cs" -> "To nevypadá jako správná hodnota. Zkuste to znovu.\n\n" + sellPrompt(chatId, field);
            default -> "This does not look valid. Try again.\n\n" + sellPrompt(chatId, field);
        };
    }

    private String sellSubmittedText(Long chatId) {
        return switch (lang(chatId)) {
            case "ru" -> "✅ Объявление отправлено на проверку. После одобрения оно появится в поиске.";
            case "uk" -> "✅ Оголошення надіслано на перевірку. Після схвалення воно зʼявиться в пошуку.";
            case "cs" -> "✅ Inzerát byl odeslán ke kontrole. Po schválení se objeví ve vyhledávání.";
            default -> "✅ Listing submitted for review. After approval it will appear in search.";
        };
    }

    private String sellCancelledText(Long chatId) {
        return switch (lang(chatId)) {
            case "ru" -> "Продажа отменена.";
            case "uk" -> "Продаж скасовано.";
            case "cs" -> "Prodej zrušen.";
            default -> "Sell flow cancelled.";
        };
    }

    private String myCarsEmptyText(Long chatId) {
        return switch (lang(chatId)) {
            case "ru" -> "У вас пока нет объявлений. Используйте /sell.";
            case "uk" -> "У вас поки немає оголошень. Використайте /sell.";
            case "cs" -> "Zatím nemáte žádné inzeráty. Použijte /sell.";
            default -> "You do not have listings yet. Use /sell.";
        };
    }

    private String listingRemovedText(Long chatId) {
        return switch (lang(chatId)) {
            case "ru" -> "Объявление снято с продажи.";
            case "uk" -> "Оголошення знято з продажу.";
            case "cs" -> "Inzerát byl stažen z prodeje.";
            default -> "Listing removed.";
        };
    }

    private String listingDeletedText(Long chatId) {
        return switch (lang(chatId)) {
            case "ru" -> "Объявление удалено.";
            case "uk" -> "Оголошення видалено.";
            case "cs" -> "Inzerát byl smazán.";
            default -> "Listing deleted.";
        };
    }

    private String listingUpdatedText(Long chatId) {
        return switch (lang(chatId)) {
            case "ru" -> "✅ Объявление обновлено и отправлено на проверку.";
            case "uk" -> "✅ Оголошення оновлено й надіслано на перевірку.";
            case "cs" -> "✅ Inzerát byl upraven a odeslán ke kontrole.";
            default -> "✅ Listing updated and submitted for review.";
        };
    }

    private String editListingMenuText(Long chatId) {
        return switch (lang(chatId)) {
            case "ru" -> "Что изменить?";
            case "uk" -> "Що змінити?";
            case "cs" -> "Co chcete upravit?";
            default -> "What do you want to edit?";
        };
    }

    private String editListingPrompt(Long chatId, String field) {
        return switch (field) {
            case "title" -> sellPrompt(chatId, "title");
            case "price" -> sellPrompt(chatId, "price");
            case "year" -> sellPrompt(chatId, "year");
            case "mileage" -> sellPrompt(chatId, "mileage");
            case "location" -> sellPrompt(chatId, "location");
            case "contact" -> sellPrompt(chatId, "contact");
            case "fuel" -> sellPrompt(chatId, "fuel");
            case "transmission" -> sellPrompt(chatId, "transmission");
            case "carType" -> sellPrompt(chatId, "carType");
            case "photo" -> sellPrompt(chatId, "photo");
            default -> "/cancel";
        };
    }

    private String listingApprovedText(Long chatId) {
        return switch (lang(chatId)) {
            case "ru" -> "✅ Ваше объявление одобрено и появилось в поиске.";
            case "uk" -> "✅ Ваше оголошення схвалено і воно зʼявилося в пошуку.";
            case "cs" -> "✅ Váš inzerát byl schválen a je ve vyhledávání.";
            default -> "✅ Your listing was approved and is now searchable.";
        };
    }

    private String listingRejectedText(Long chatId) {
        return switch (lang(chatId)) {
            case "ru" -> "К сожалению, объявление отклонено модератором.";
            case "uk" -> "На жаль, оголошення відхилено модератором.";
            case "cs" -> "Inzerát byl bohužel zamítnut moderátorem.";
            default -> "The listing was rejected by moderator.";
        };
    }

    private String buildSellPreview(Long chatId, SellDraft draft) {
        return switch (lang(chatId)) {
            case "ru" -> "Проверьте объявление:\n\n" + formatSellDraft(chatId, draft);
            case "uk" -> "Перевірте оголошення:\n\n" + formatSellDraft(chatId, draft);
            case "cs" -> "Zkontrolujte inzerát:\n\n" + formatSellDraft(chatId, draft);
            default -> "Review listing:\n\n" + formatSellDraft(chatId, draft);
        };
    }

    private String formatSellDraft(Long chatId, SellDraft draft) {
        String lang = lang(chatId);
        return """
                🚗 %s

                💰 %s
                📍 %s
                📅 %s
                🛣 %s
                ⛽ %s
                ⚙️ %s
                🚙 %s
                ☎️ %s
                📷 %s
                """.formatted(
                buildUserListingTitle(draft),
                formatCzk(draft.priceValue),
                safe(draft.location),
                draft.year,
                safeMileage(draft.mileage),
                formatFuelTypeValue(lang, draft.fuelType),
                formatTransmissionValue(lang, draft.transmission),
                formatCarType(lang, draft.carType),
                safe(draft.sellerContact),
                draft.imageUrl == null || draft.imageUrl.isBlank() ? "-" : "OK"
        ).trim();
    }

    private String formatUserListing(Long chatId, CarEntity car) {
        return formatUserListing(lang(chatId), car);
    }

    private String formatUserListing(String lang, CarEntity car) {
        String status = car.getListingStatus() == null ? "ACTIVE" : car.getListingStatus();
        return """
                🚗 %s

                Status: %s
                💰 %s
                📍 %s
                📅 %s
                🛣 %s
                ⛽ %s
                ⚙️ %s
                🚙 %s
                ☎️ %s
                """.formatted(
                safe(car.getTitle()),
                status,
                formatPrice(car),
                safe(car.getLocation()),
                car.getYear() == null ? "-" : car.getYear(),
                safeMileage(car.getMileage()),
                formatFuelTypeValue(lang, car.getFuelType()),
                formatTransmissionValue(lang, car.getTransmission()),
                formatCarType(lang, car.getCarType()),
                safe(car.getSellerContact())
        ).trim();
    }

    private String buildUserListingTitle(SellDraft draft) {
        String brand = draft.brand == null ? "" : draft.brand.trim();
        String title = draft.title == null ? "" : draft.title.trim();
        if (title.toLowerCase().startsWith(brand.toLowerCase())) {
            return limitText(title, 240);
        }
        return limitText((brand + " " + title).trim(), 240);
    }

    private boolean isEditableListingField(String field) {
        return "title".equals(field)
                || "price".equals(field)
                || "year".equals(field)
                || "mileage".equals(field)
                || "location".equals(field)
                || "contact".equals(field)
                || "fuel".equals(field)
                || "transmission".equals(field)
                || "carType".equals(field)
                || "photo".equals(field);
    }

    private boolean updateListingField(Long chatId, CarEntity car, String field, String value) {
        switch (field) {
            case "title" -> car.setTitle(limitText(value, 220));
            case "price" -> {
                Integer price = parsePositiveInt(value);
                if (price == null || price < 1000 || price > 20_000_000) {
                    sendMessage(chatId, sellInvalid(chatId, "price"));
                    return false;
                }
                car.setPriceValue(price);
                car.setPrice(formatCzk(price));
            }
            case "year" -> {
                Integer year = parsePositiveInt(value);
                int currentYear = LocalDateTime.now().getYear() + 1;
                if (year == null || year < 1980 || year > currentYear) {
                    sendMessage(chatId, sellInvalid(chatId, "year"));
                    return false;
                }
                car.setYear(year);
            }
            case "mileage" -> {
                Integer mileage = parsePositiveInt(value);
                if (mileage == null || mileage < 0 || mileage > 1_000_000) {
                    sendMessage(chatId, sellInvalid(chatId, "mileage"));
                    return false;
                }
                car.setMileage(mileage);
            }
            case "location" -> car.setLocation(limitText(value, 120));
            case "contact" -> car.setSellerContact(limitText(value, 180));
            case "fuel" -> {
                String fuel = normalizeSellFuel(value);
                if (fuel == null) {
                    sendMessage(chatId, sellInvalid(chatId, "fuel"));
                    return false;
                }
                car.setFuelType(fuel);
            }
            case "transmission" -> {
                String transmission = normalizeSellTransmission(value);
                if (transmission == null) {
                    sendMessage(chatId, sellInvalid(chatId, "transmission"));
                    return false;
                }
                car.setTransmission(transmission);
            }
            case "carType" -> {
                String carType = normalizeSellCarType(value);
                if (carType == null) {
                    sendMessage(chatId, sellInvalid(chatId, "carType"));
                    return false;
                }
                car.setCarType(carType);
            }
            case "photo" -> car.setImageUrl(value);
            default -> {
                return false;
            }
        }
        return true;
    }

    private String normalizeSellBrand(String value) {
        String normalized = limitText(value, 60).trim().toUpperCase().replaceAll("[^A-Z0-9 ]", "");
        return normalized.isBlank() ? "OTHER" : normalized.replace(' ', '_');
    }

    private String normalizeSellFuel(String value) {
        String v = value == null ? "" : value.trim().toLowerCase();
        if (v.contains("diesel") || v.contains("nafta") || v.contains("диз") || v.contains("дизель")) return "DIESEL";
        if (v.contains("plugin") || v.contains("plug") || v.contains("phev")) return "PLUGIN_HYBRID";
        if (v.contains("hybrid") || v.contains("гібр") || v.contains("гибр")) return "HYBRID";
        if (v.contains("electric") || v.contains("ev") || v.contains("елект") || v.contains("элект")) return "ELECTRIC";
        if (v.contains("lpg")) return "LPG";
        if (v.contains("cng")) return "CNG";
        if (v.contains("petrol") || v.contains("benzin") || v.contains("benz") || v.contains("бенз")) return "PETROL";
        return null;
    }

    private String normalizeSellTransmission(String value) {
        String v = value == null ? "" : value.trim().toLowerCase();
        if (v.contains("auto") || v.contains("dsg") || v.contains("авто")) return "AUTOMATIC";
        if (v.contains("manual") || v.contains("man") || v.contains("мех") || v.contains("мех")) return "MANUAL";
        return null;
    }

    private String normalizeSellCarType(String value) {
        String v = value == null ? "" : value.trim().toLowerCase();
        if (v.contains("suv") || v.contains("crossover") || v.contains("крос")) return "SUV";
        if (v.contains("wagon") || v.contains("kombi") || v.contains("combi") || v.contains("универс") || v.contains("універс")) return "WAGON";
        if (v.contains("sedan") || v.contains("седан")) return "SEDAN";
        if (v.contains("minivan") || v.contains("mpv") || v.contains("мінівен") || v.contains("минивен")) return "MINIVAN";
        if (v.contains("coupe") || v.contains("купе")) return "COUPE";
        if (v.contains("cabrio") || v.contains("кабр")) return "CABRIO";
        if (v.contains("pickup") || v.contains("pick-up") || v.contains("пикап") || v.contains("пікап")) return "PICKUP";
        if (v.contains("hatch") || v.contains("хетч")) return "HATCHBACK";
        return null;
    }

    private Integer parsePositiveInt(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String limitText(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLen ? normalized : normalized.substring(0, maxLen).trim();
    }

    private String formatCzk(Integer value) {
        if (value == null) {
            return "-";
        }
        return String.format("%,d Kč", value).replace(",", " ");
    }

    private void showCurrentFilter(Long chatId) {
        editingFilterSessions.remove(chatId);
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

    private void confirmResetFilter(Long chatId) {
        String lang = lang(chatId);

        sendMessage(
                chatId,
                resetConfirmText(lang),
                keyboardFactory.resetConfirmKeyboard(lang)
        );
    }

    private void showNotificationSettings(Long chatId) {
        TelegramSubscriberEntity subscriber = subscriberService.getOrCreate(chatId);
        String lang = lang(chatId);

        sendMessage(
                chatId,
                notificationSettingsText(lang, subscriber),
                keyboardFactory.notificationSettingsKeyboard(lang, subscriber)
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

    private String resetConfirmText(String lang) {
        return switch (lang) {
            case "ru" -> """
                    Сбросить фильтр?

                    Текущие настройки поиска будут очищены.
                    """;
            case "uk" -> """
                    Скинути фільтр?

                    Поточні налаштування пошуку буде очищено.
                    """;
            case "cs" -> """
                    Resetovat filtr?

                    Aktuální nastavení hledání bude vymazáno.
                    """;
            default -> """
                    Reset filter?

                    Your current search settings will be cleared.
                    """;
        };
    }

    private String notificationSettingsText(String lang, TelegramSubscriberEntity subscriber) {
        boolean paused = subscriber != null && subscriber.isNotificationsPaused();
        String mode = subscriber == null || subscriber.getNotificationMode() == null
                ? "INSTANT"
                : subscriber.getNotificationMode();
        Integer limit = subscriber == null ? null : subscriber.getDailyNotificationLimit();
        Integer sentToday = subscriber == null || subscriber.getNotificationsSentToday() == null
                ? 0
                : subscriber.getNotificationsSentToday();

        String status = switch (lang) {
            case "ru" -> paused ? "на паузе" : "активны";
            case "uk" -> paused ? "на паузі" : "активні";
            case "cs" -> paused ? "pozastaveno" : "aktivní";
            default -> paused ? "paused" : "active";
        };

        String modeLabel = switch (lang) {
            case "ru" -> "DIGEST".equalsIgnoreCase(mode) ? "дайджест" : "сразу";
            case "uk" -> "DIGEST".equalsIgnoreCase(mode) ? "дайджест" : "одразу";
            case "cs" -> "DIGEST".equalsIgnoreCase(mode) ? "souhrn" : "ihned";
            default -> "DIGEST".equalsIgnoreCase(mode) ? "digest" : "instant";
        };

        String limitLabel = limit == null
                ? switch (lang) {
                    case "ru" -> "без лимита";
                    case "uk" -> "без ліміту";
                    case "cs" -> "bez limitu";
                    default -> "no limit";
                }
                : limit + " / day";

        return switch (lang) {
            case "ru" -> """
                    🔔 Уведомления

                    Статус: %s
                    Режим: %s
                    Лимит: %s
                    Сегодня отправлено: %s
                    """.formatted(status, modeLabel, limitLabel, sentToday);
            case "uk" -> """
                    🔔 Сповіщення

                    Статус: %s
                    Режим: %s
                    Ліміт: %s
                    Сьогодні надіслано: %s
                    """.formatted(status, modeLabel, limitLabel, sentToday);
            case "cs" -> """
                    🔔 Upozornění

                    Stav: %s
                    Režim: %s
                    Limit: %s
                    Dnes odesláno: %s
                    """.formatted(status, modeLabel, limitLabel, sentToday);
            default -> """
                    🔔 Notifications

                    Status: %s
                    Mode: %s
                    Limit: %s
                    Sent today: %s
                    """.formatted(status, modeLabel, limitLabel, sentToday);
        };
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
            log.warn("BOT SEND MESSAGE failed chatId={}", chatId, e);
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
            log.warn("BOT SEND MESSAGE failed chatId={} withInlineKeyboard=true", chatId, e);
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
            log.warn("BOT SEND MESSAGE failed chatId={} withReplyKeyboard=true", chatId, e);
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

        if ("USER".equalsIgnoreCase(car.getSource()) && car.getSellerContact() != null && !car.getSellerContact().isBlank()) {
            sb.append("☎️ ").append(car.getSellerContact().trim()).append("\n");
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

        if (normalized.contains("SBAZAR")) {
            return "Sbazar.cz";
        }

        if (normalized.contains("TIPCARS") || normalized.contains("TIP_CARS")) {
            return "TipCars.cz";
        }

        if (normalized.contains("TOYOTA_PROVERENE")) {
            return "Toyota prověřené vozy";
        }

        if (normalized.contains("USER")) {
            return "AutoCZ users";
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
        long newLast24h = carRepository.countByCreatedAtAfter(last24h);
        long pendingUserListings = carRepository.countByListingStatus("PENDING");

        String sourcesTotal = formatSourceStats(carRepository.countCarsBySource());
        String sourcesLast24h = formatSourceStats(carRepository.countCarsBySourceAfter(last24h));
        String parserDiagnostics = buildParserDiagnostics();
        String filterDiagnostics = buildAdminFilterDiagnostics(chatId, last24h);

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
        🧾 User listings pending: %d

        📦 Sources total:
        %s

        🕒 Sources last 24h:
        %s

        🧾 Latest cars:
        %s
        """.formatted(
                usersTotal,
                usersActive,
                favoritesTotal,
                totalCars,
                newLast24h,
                pendingUserListings,
                sourcesTotal,
                sourcesLast24h,
                latest.toString().trim()
        );

        text = text
                + "\n\nScheduler last run:\n"
                + parserDiagnostics
                + "\n\nYour filter diagnostics:\n"
                + filterDiagnostics;

        sendMessage(chatId, text);
        sendPendingUserListings(chatId);
    }

    private String buildParserDiagnostics() {
        LocalDateTime lastRunAt = parserRunStatsService.getLastRunAt();

        if (lastRunAt == null) {
            return "- no parser run recorded since app start";
        }

        StringBuilder builder = new StringBuilder();
        boolean running = parserRunStatsService.isRunning();
        LocalDateTime finishedAt = parserRunStatsService.getLastFinishedAt();

        builder.append("- status: ")
                .append(running ? "running (numbers are partial)" : "finished")
                .append("\n");
        builder.append("- last started: ").append(lastRunAt.withNano(0)).append("\n");
        if (finishedAt != null) {
            builder.append("- last finished: ").append(finishedAt.withNano(0)).append("\n");
        }
        builder.append("- parsed unique: ").append(parserRunStatsService.getTotalParsedUnique()).append("\n");
        builder.append("- newly saved: ").append(parserRunStatsService.getTotalSaved()).append("\n");

        Map<String, ParserRunStatsService.ParserStats> stats = parserRunStatsService.getParserStats();
        if (stats == null || stats.isEmpty()) {
            return builder.toString().trim();
        }

        for (Map.Entry<String, ParserRunStatsService.ParserStats> entry : stats.entrySet()) {
            ParserRunStatsService.ParserStats stat = entry.getValue();
            builder.append("- ")
                    .append(formatSource(entry.getKey()))
                    .append(": returned=")
                    .append(stat.returned())
                    .append(", added=")
                    .append(stat.added())
                    .append(", duplicates=")
                    .append(stat.duplicatesSkipped())
                    .append(", invalid=")
                    .append(stat.invalidSkipped());

            if (stat.failed()) {
                builder.append(", FAILED");
            }

            builder.append("\n");
        }

        return builder.toString().trim();
    }

    private String buildAdminFilterDiagnostics(Long chatId, LocalDateTime since) {
        UserFilterEntity filter = userFilterService.findByChatId(chatId).orElse(null);
        if (!isFilterConfigured(filter)) {
            return "- no active filter configured";
        }

        List<CarEntity> recentCars = carRepository.findAllByListingStatusAndCreatedAtAfterOrderByCreatedAtDesc("ACTIVE", since);
        long matched = recentCars.stream()
                .filter(car -> carFilterMatcher.matches(car, filter))
                .count();

        StringBuilder builder = new StringBuilder();
        builder.append("- active filter: ")
                .append("brand=").append(safe(filter.getBrand()))
                .append(", type=").append(safe(filter.getCarType()))
                .append(", maxPrice=").append(filter.getMaxPrice() == null ? "-" : filter.getMaxPrice())
                .append(", location=").append(safe(filter.getLocation()))
                .append(", maxMileage=").append(filter.getMaxMileage() == null ? "-" : filter.getMaxMileage())
                .append(", fuel=").append(safe(filter.getFuelType()))
                .append(", transmission=").append(safe(filter.getTransmission()))
                .append(", yearFrom=").append(filter.getYearFrom() == null ? "-" : filter.getYearFrom())
                .append("\n");
        builder.append("- new ACTIVE cars last 24h: ").append(recentCars.size()).append("\n");
        builder.append("- matched your filter last 24h: ").append(matched);

        return builder.toString();
    }

    private void sendPendingUserListings(Long adminChatId) {
        List<CarEntity> pendingCars = carRepository.findTop10ByListingStatusOrderByCreatedAtDesc("PENDING");
        if (pendingCars.isEmpty()) {
            return;
        }

        sendMessage(adminChatId, "Pending user listings:");
        for (CarEntity car : pendingCars) {
            sendCarMessage(
                    adminChatId,
                    car,
                    formatUserListing("en", car),
                    keyboardFactory.sellAdminReviewKeyboard(car.getId())
            );
        }
    }

    private String formatSourceStats(List<CarRepository.SourceCount> stats) {
        if (stats == null || stats.isEmpty()) {
            return "—";
        }

        StringBuilder builder = new StringBuilder();
        for (CarRepository.SourceCount stat : stats) {
            builder.append("• ")
                    .append(formatSource(stat.getSource()))
                    .append(": ")
                    .append(stat.getTotal())
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private boolean isAdmin(Long chatId) {
        return chatId != null && adminChatIds.contains(chatId);
    }

    private Set<Long> parseAdminChatIds(String raw) {
        Set<Long> result = new LinkedHashSet<>();

        if (raw == null || raw.isBlank()) {
            return result;
        }

        for (String part : raw.split("[,;\\s]+")) {
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

    private static class SellDraft {
        private Long ownerChatId;
        private String sellerUsername;
        private String brand;
        private String title;
        private Integer priceValue;
        private Integer year;
        private Integer mileage;
        private String location;
        private String fuelType;
        private String transmission;
        private String carType;
        private String sellerContact;
        private String imageUrl;
        private String description;

        private boolean isComplete() {
            return ownerChatId != null
                    && brand != null && !brand.isBlank()
                    && title != null && !title.isBlank()
                    && priceValue != null
                    && year != null
                    && mileage != null
                    && location != null && !location.isBlank()
                    && fuelType != null && !fuelType.isBlank()
                    && transmission != null && !transmission.isBlank()
                    && carType != null && !carType.isBlank()
                    && sellerContact != null && !sellerContact.isBlank();
        }
    }

    private record ListingEditSession(Long carId, String field) {
    }
}

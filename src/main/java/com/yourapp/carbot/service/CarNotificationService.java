package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.TelegramSubscriberEntity;
import com.yourapp.carbot.entity.UserFilterEntity;
import com.yourapp.carbot.i18n.MessageService;
import com.yourapp.carbot.repository.TelegramSubscriberRepository;
import com.yourapp.carbot.repository.UserFilterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CarNotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(CarNotificationService.class);

    private static final String PLACEHOLDER_IMAGE_PATH = "static/images/no-car-photo.jpg";
    private static final int MAX_CAPTION_LENGTH = 900;
    private static final int MAX_MESSAGE_LENGTH = 3500;

    private final TelegramSubscriberRepository subscriberRepository;
    private final UserFilterRepository userFilterRepository;
    private final CarFilterMatcher carFilterMatcher;
    private final TelegramClient telegramClient;
    private final MessageService messages;

    public CarNotificationService(
            TelegramSubscriberRepository subscriberRepository,
            UserFilterRepository userFilterRepository,
            CarFilterMatcher carFilterMatcher,
            TelegramClient telegramClient,
            MessageService messages
    ) {
        this.subscriberRepository = subscriberRepository;
        this.userFilterRepository = userFilterRepository;
        this.carFilterMatcher = carFilterMatcher;
        this.telegramClient = telegramClient;
        this.messages = messages;
    }

    public int notifySubscribers(List<CarEntity> cars) {

        if (cars == null || cars.isEmpty()) {
            log.info("No cars to notify");
            return 0;
        }

        List<CarEntity> uniqueCars = deduplicateCars(cars);
        int sentCount = 0;

        List<TelegramSubscriberEntity> subscribers = subscriberRepository.findAll();

        log.info("Start notifications. Subscribers={}, cars={}, uniqueCars={}",
                subscribers.size(), cars.size(), uniqueCars.size());

        for (TelegramSubscriberEntity subscriber : subscribers) {

            if (subscriber == null || subscriber.getChatId() == null) {
                continue;
            }

            Long chatId = subscriber.getChatId();

            UserFilterEntity filter = userFilterRepository
                    .findByChatId(chatId)
                    .orElse(null);

            String lang = resolveLanguage(filter);
            int sentToSubscriber = 0;

            for (CarEntity car : uniqueCars) {

                if (car == null) {
                    continue;
                }

                boolean matches = logFilterDecision(chatId, car, filter);

                if (!matches) {
                    continue;
                }

                boolean sent = sendCar(chatId, car, lang);

                if (sent) {
                    sentCount++;
                    sentToSubscriber++;
                }
            }

            log.info("Subscriber {} received {} notifications",
                    chatId, sentToSubscriber);
        }

        log.info("Notifications finished. Total sent={}", sentCount);

        return sentCount;
    }

    private List<CarEntity> deduplicateCars(List<CarEntity> cars) {
        if (cars == null || cars.isEmpty()) {
            return List.of();
        }

        Map<String, CarEntity> uniqueByKey = new LinkedHashMap<>();

        for (CarEntity car : cars) {
            if (car == null) {
                continue;
            }

            String url = normalizeUrl(car.getUrl());

            if (url != null) {
                uniqueByKey.putIfAbsent(url, car);
                continue;
            }

            String fallbackKey = buildFallbackDedupKey(car);
            uniqueByKey.putIfAbsent(fallbackKey, car);
        }

        return new ArrayList<>(uniqueByKey.values());
    }

    private String buildFallbackDedupKey(CarEntity car) {
        return (safe(car.getSource()) + "|" +
                safe(car.getTitle()) + "|" +
                safe(car.getPrice()) + "|" +
                safe(car.getLocation())).toLowerCase(Locale.ROOT);
    }

    private boolean logFilterDecision(Long chatId, CarEntity car, UserFilterEntity filter) {
        if (filter == null) {
            log.info("FILTER chatId={} title='{}' result=PASS reason=no_filter",
                    chatId, safe(car.getTitle()));
            return true;
        }

        FilterCheckResult check = carFilterMatcher.check(car, filter);

        log.info(
                "FILTER chatId={} title='{}' result={} | " +
                        "filter[carType={}, brand={}, maxPrice={}, location={}, maxMileage={}, fuelType={}, transmission={}, yearFrom={}] | " +
                        "car[carType={}, brand={}, priceValue={}, location={}, mileage={}, fuelType={}, transmission={}, year={}] | " +
                        "checks[carTypeOk={}, brandOk={}, maxPriceOk={}, locationOk={}, mileageOk={}, fuelTypeOk={}, transmissionOk={}, yearOk={}]",
                chatId,
                safe(car.getTitle()),
                check.result() ? "PASS" : "FAIL",

                safe(filter.getCarType()),
                safe(filter.getBrand()),
                filter.getMaxPrice(),
                safe(filter.getLocation()),
                filter.getMaxMileage(),
                safe(filter.getFuelType()),
                safe(filter.getTransmission()),
                filter.getYearFrom(),

                safe(car.getCarType()),
                safe(car.getBrand()),
                car.getPriceValue(),
                safe(car.getLocation()),
                car.getMileage(),
                safe(car.getFuelType()),
                safe(car.getTransmission()),
                car.getYear(),

                check.carTypeOk(),
                check.brandOk(),
                check.maxPriceOk(),
                check.locationOk(),
                check.mileageOk(),
                check.fuelTypeOk(),
                check.transmissionOk(),
                check.yearOk()
        );

        return check.result();
    }

    private boolean sendCar(Long chatId, CarEntity car, String lang) {
        try {
            InlineKeyboardMarkup keyboard = buildOpenUrlKeyboard(car.getUrl(), lang);

            String formatted = formatCarCard(car, lang);
            String caption = limitLength(formatted, MAX_CAPTION_LENGTH);
            String textMessage = limitLength(formatted, MAX_MESSAGE_LENGTH);

            log.info("Sending notification. chatId={} source={} title='{}'",
                    chatId, safe(car.getSource()), safe(car.getTitle()));

            if (hasUsableImage(car.getImageUrl())) {
                if (sendPhotoByUrl(chatId, car, caption, keyboard)) {
                    return true;
                }

                if (sendPhotoByDownload(chatId, car, caption, keyboard)) {
                    return true;
                }
            } else {
                log.debug("No usable image for car. source={} title='{}' url={} imageUrl={}",
                        safe(car.getSource()), safe(car.getTitle()), safe(car.getUrl()), safe(car.getImageUrl()));
            }

            if (sendPlaceholderPhoto(chatId, car, caption, keyboard)) {
                return true;
            }

            SendMessage message = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(textMessage)
                    .replyMarkup(keyboard)
                    .build();

            telegramClient.execute(message);

            log.info("Sent as text message. source={} chatId={} title='{}'",
                    safe(car.getSource()), chatId, safe(car.getTitle()));

            return true;

        } catch (BotBlockedException e) {
            return false;
        } catch (Exception e) {
            removeBlockedSubscriber(chatId, e);

            log.error("Failed to send car notification. chatId={}, url={}",
                    chatId, safe(car.getUrl()), e);

            return false;
        }
    }

    private boolean sendPhotoByUrl(Long chatId,
                                   CarEntity car,
                                   String caption,
                                   InlineKeyboardMarkup keyboard) {
        try {
            String imageUrl = normalizeImageUrl(car.getImageUrl());
            if (imageUrl == null) {
                return false;
            }

            SendPhoto photo = SendPhoto.builder()
                    .chatId(chatId.toString())
                    .photo(new InputFile(imageUrl))
                    .caption(caption)
                    .replyMarkup(keyboard)
                    .build();

            telegramClient.execute(photo);

            log.info("Photo sent by URL. source={} chatId={} title='{}'",
                    safe(car.getSource()), chatId, safe(car.getTitle()));

            return true;

        } catch (Exception e) {
            removeBlockedSubscriber(chatId, e);

            log.warn("Telegram could not fetch image by URL. source={} imageUrl={} carUrl={} reason={}",
                    safe(car.getSource()), safe(car.getImageUrl()), safe(car.getUrl()), e.getMessage());

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
                log.warn("Downloaded image file is empty or missing. source={} title='{}' imageUrl={}",
                        safe(car.getSource()), safe(car.getTitle()), safe(car.getImageUrl()));
                return false;
            }

            SendPhoto photo = SendPhoto.builder()
                    .chatId(chatId.toString())
                    .photo(new InputFile(tempFile))
                    .caption(caption)
                    .replyMarkup(keyboard)
                    .build();

            telegramClient.execute(photo);

            log.info("Photo sent by download. source={} chatId={} title='{}'",
                    safe(car.getSource()), chatId, safe(car.getTitle()));

            return true;

        } catch (Exception e) {
            removeBlockedSubscriber(chatId, e);

            log.warn("Failed to send downloaded image. source={} imageUrl={} carUrl={} reason={}",
                    safe(car.getSource()), safe(car.getImageUrl()), safe(car.getUrl()), e.getMessage());

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

    private boolean sendPlaceholderPhoto(Long chatId,
                                         CarEntity car,
                                         String caption,
                                         InlineKeyboardMarkup keyboard) {
        File tempFile = null;

        try {
            ClassPathResource resource = new ClassPathResource(PLACEHOLDER_IMAGE_PATH);

            if (!resource.exists()) {
                log.warn("Placeholder image not found: {}", PLACEHOLDER_IMAGE_PATH);
                return false;
            }

            tempFile = File.createTempFile("car-placeholder-", ".jpg");

            try (InputStream in = resource.getInputStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                in.transferTo(out);
            }

            SendPhoto photo = SendPhoto.builder()
                    .chatId(chatId.toString())
                    .photo(new InputFile(tempFile))
                    .caption(caption)
                    .replyMarkup(keyboard)
                    .build();

            telegramClient.execute(photo);

            log.info("Placeholder photo sent. source={} chatId={} title='{}'",
                    safe(car.getSource()), chatId, safe(car.getTitle()));

            return true;

        } catch (Exception e) {
            removeBlockedSubscriber(chatId, e);

            log.warn("Failed to send placeholder photo. source={} title='{}' reason={}",
                    safe(car.getSource()), safe(car.getTitle()), e.getMessage());

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
        String imageUrl = normalizeImageUrl(car.getImageUrl());

        if (imageUrl == null) {
            return null;
        }

        URLConnection connection = new URL(imageUrl).openConnection();

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
        String source = safe(car.getSource()).toUpperCase(Locale.ROOT);

        if (source.contains("BAZOS")) {
            return "https://auto.bazos.cz/";
        }

        if (source.contains("SAUTO")) {
            return "https://www.sauto.cz/";
        }

        return "https://www.google.com/";
    }

    private String guessExtension(String imageUrl) {
        String lower = imageUrl.toLowerCase(Locale.ROOT);

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

    private InlineKeyboardMarkup buildOpenUrlKeyboard(String url, String lang) {

        String normalizedUrl = normalizeUrl(url);
        if (normalizedUrl == null) {
            return null;
        }

        InlineKeyboardButton openButton = InlineKeyboardButton.builder()
                .text(messages.get(lang, "car.open"))
                .url(normalizedUrl)
                .build();

        InlineKeyboardRow row = new InlineKeyboardRow(openButton);

        return InlineKeyboardMarkup.builder()
                .keyboardRow(row)
                .build();
    }

    private boolean hasUsableImage(String imageUrl) {
        String normalized = normalizeImageUrl(imageUrl);
        if (normalized == null) {
            return false;
        }

        String lower = normalized.toLowerCase(Locale.ROOT);

        return !lower.contains("empty.gif")
                && !lower.contains("placeholder")
                && !lower.contains("no-image")
                && !lower.contains("no_image")
                && !lower.contains("default");
    }

    private String formatCarCard(CarEntity car, String lang) {
        String title = formatTitle(car.getTitle());
        String price = formatPrice(car);
        String location = safeOrNull(car.getLocation());
        String year = safeYear(car.getYear());
        String mileage = safeMileage(car.getMileage());
        String fuel = safeFuelType(car.getFuelType(), lang);
        String transmission = safeTransmission(car.getTransmission(), lang);
        String source = formatSource(car.getSource());
        String freshness = formatFreshness(lang, car.getCreatedAt());

        StringBuilder sb = new StringBuilder();

        sb.append("🚗 ").append(title).append("\n\n");

        if (price != null) {
            sb.append("💰 ").append(price).append("\n");
        }

        if (location != null) {
            sb.append("📍 ").append(messages.get(lang, "label.location")).append(": ").append(location).append("\n");
        }

        if (year != null) {
            sb.append("📅 ").append(messages.get(lang, "label.year")).append(": ").append(year).append("\n");
        }

        if (mileage != null) {
            sb.append("🛣 ").append(messages.get(lang, "label.mileage")).append(": ").append(mileage).append("\n");
        }

        if (fuel != null) {
            sb.append("⛽ ").append(messages.get(lang, "label.fuelType")).append(": ").append(fuel).append("\n");
        }

        if (transmission != null) {
            sb.append("⚙️ ").append(messages.get(lang, "label.transmission")).append(": ").append(transmission).append("\n");
        }

        if (source != null) {
            sb.append("🌐 ").append(messages.get(lang, "label.source")).append(": ").append(source).append("\n");
        }

        if (freshness != null) {
            sb.append("🕒 ").append(freshness).append("\n");
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
            return String.format(Locale.US, "%,d Kč", car.getPriceValue()).replace(",", " ");
        }

        return null;
    }

    private String formatSource(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }

        return switch (source.trim().toUpperCase(Locale.ROOT)) {
            case "SAUTO" -> "Sauto.cz";
            case "BAZOS" -> "Bazoš.cz";
            default -> source.trim();
        };
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
        return switch (lang) {
            case "ru" -> days + " дн назад";
            case "uk" -> days + " дн тому";
            case "cs" -> "před " + days + " d";
            default -> days + " d ago";
        };
    }

    private String safeYear(Integer year) {
        return year == null ? null : String.valueOf(year);
    }

    private String safeMileage(Integer mileage) {
        if (mileage == null || mileage <= 0) {
            return null;
        }

        return String.format(Locale.US, "%,d km", mileage).replace(",", " ");
    }

    private String safeFuelType(String fuelType, String lang) {
        if (fuelType == null || fuelType.isBlank()) {
            return null;
        }

        return messages.getOrDefault(lang, "fuelType." + fuelType, fuelType);
    }

    private String safeTransmission(String transmission, String lang) {
        if (transmission == null || transmission.isBlank()) {
            return null;
        }

        return messages.getOrDefault(lang, "transmission." + transmission, transmission);
    }

    private String resolveLanguage(UserFilterEntity filter) {

        if (filter == null || filter.getLanguageCode() == null || filter.getLanguageCode().isBlank()) {
            return "cs";
        }

        String lang = filter.getLanguageCode().toLowerCase(Locale.ROOT);

        if (lang.startsWith("ru")) {
            return "ru";
        }

        if (lang.startsWith("uk")) {
            return "uk";
        }

        if (lang.startsWith("en")) {
            return "en";
        }

        return "cs";
    }

    private String limitLength(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        String normalized = text.replaceAll("\\n{3,}", "\n\n").trim();

        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, maxLength - 3).trim() + "...";
    }

    private String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }

        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (!(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
            return null;
        }

        return trimmed;
    }

    private String normalizeImageUrl(String imageUrl) {
        String normalized = normalizeUrl(imageUrl);
        if (normalized == null) {
            return null;
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".svg")) {
            return null;
        }

        return normalized;
    }

    private void removeBlockedSubscriber(Long chatId, Exception e) {
        String message = e.getMessage();

        if (message != null &&
                (message.contains("[403]") ||
                        message.toLowerCase(Locale.ROOT).contains("bot was blocked by the user"))) {

            subscriberRepository.deleteByChatId(chatId);

            log.warn("Subscriber removed because bot was blocked. chatId={}", chatId);

            throw new BotBlockedException();
        }
    }

    private static class BotBlockedException extends RuntimeException {
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String safeOrNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
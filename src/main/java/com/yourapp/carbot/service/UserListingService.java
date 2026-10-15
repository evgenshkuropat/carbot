package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.repository.CarRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserListingService {

    private final CarRepository carRepository;
    private final Map<Long, Draft> drafts = new ConcurrentHashMap<>();
    private final Map<Long, EditSession> editSessions = new ConcurrentHashMap<>();

    public UserListingService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public Draft startDraft(Long chatId) {
        Draft draft = new Draft(chatId);
        drafts.put(chatId, draft);
        return draft;
    }

    public Draft getOrCreateDraft(Long chatId) {
        return drafts.computeIfAbsent(chatId, Draft::new);
    }

    public Optional<Draft> findDraft(Long chatId) {
        return Optional.ofNullable(drafts.get(chatId));
    }

    public void clearDraft(Long chatId) {
        drafts.remove(chatId);
    }

    public Optional<CarEntity> submitDraft(Long chatId, String title) {
        Draft draft = drafts.get(chatId);
        if (draft == null || !draft.isComplete()) {
            return Optional.empty();
        }

        CarEntity car = new CarEntity();
        car.setSource("USER");
        car.setTitle(limitText(title, 240));
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
        drafts.remove(chatId);
        return Optional.of(saved);
    }

    public List<CarEntity> findListings(Long chatId) {
        return carRepository.findTop50ByOwnerChatIdOrderByCreatedAtDesc(chatId);
    }

    public OwnershipResult deactivate(Long chatId, Long carId) {
        return findOwned(chatId, carId).map(car -> {
            car.setListingStatus("INACTIVE");
            carRepository.save(car);
            return OwnershipResult.UPDATED;
        }).orElseGet(() -> exists(carId) ? OwnershipResult.NOT_OWNER : OwnershipResult.NOT_FOUND);
    }

    public OwnershipResult delete(Long chatId, Long carId) {
        return findOwned(chatId, carId).map(car -> {
            carRepository.delete(car);
            return OwnershipResult.UPDATED;
        }).orElseGet(() -> exists(carId) ? OwnershipResult.NOT_OWNER : OwnershipResult.NOT_FOUND);
    }

    public Optional<CarEntity> findOwned(Long chatId, Long carId) {
        return carRepository.findById(carId).filter(car -> Objects.equals(car.getOwnerChatId(), chatId));
    }

    public boolean startEditing(Long chatId, Long carId, String field) {
        if (!isEditableField(field) || findOwned(chatId, carId).isEmpty()) {
            return false;
        }
        editSessions.put(chatId, new EditSession(carId, field));
        return true;
    }

    public Optional<EditSession> findEditSession(Long chatId) {
        return Optional.ofNullable(editSessions.get(chatId));
    }

    public void clearEditSession(Long chatId) {
        editSessions.remove(chatId);
    }

    public UpdateResult applyEdit(Long chatId, EditSession session, String value) {
        Optional<CarEntity> ownedCar = findOwned(chatId, session.carId());
        if (ownedCar.isEmpty()) {
            return exists(session.carId()) ? UpdateResult.notOwner() : UpdateResult.notFound();
        }

        CarEntity car = ownedCar.get();
        if (!updateField(car, session.field(), value)) {
            return UpdateResult.invalidValue();
        }

        car.setListingStatus("PENDING");
        car.setCreatedAt(LocalDateTime.now());
        carRepository.save(car);
        editSessions.remove(chatId);
        return UpdateResult.updated(car);
    }

    public ReviewResult review(Long carId, boolean approve) {
        Optional<CarEntity> found = carRepository.findById(carId);
        if (found.isEmpty()) {
            return ReviewResult.notFound();
        }
        CarEntity car = found.get();
        if (!"PENDING".equalsIgnoreCase(car.getListingStatus())) {
            return ReviewResult.alreadyReviewed(car);
        }

        car.setListingStatus(approve ? "ACTIVE" : "REJECTED");
        if (approve) {
            car.setCreatedAt(LocalDateTime.now());
        }
        return ReviewResult.updated(carRepository.save(car));
    }

    public static boolean isEditableField(String field) {
        return "title".equals(field) || "price".equals(field) || "year".equals(field)
                || "mileage".equals(field) || "location".equals(field) || "contact".equals(field)
                || "description".equals(field) || "fuel".equals(field) || "transmission".equals(field)
                || "carType".equals(field) || "photo".equals(field);
    }

    private boolean exists(Long carId) {
        return carRepository.existsById(carId);
    }

    private boolean updateField(CarEntity car, String field, String value) {
        switch (field) {
            case "title" -> car.setTitle(limitText(value, 220));
            case "price" -> {
                Integer price = parsePositiveInt(value);
                if (price == null || price < 1_000 || price > 20_000_000) return false;
                car.setPriceValue(price);
                car.setPrice(formatCzk(price));
            }
            case "year" -> {
                Integer year = parsePositiveInt(value);
                if (year == null || year < 1980 || year > LocalDateTime.now().getYear() + 1) return false;
                car.setYear(year);
            }
            case "mileage" -> {
                Integer mileage = parsePositiveInt(value);
                if (mileage == null || mileage > 1_000_000) return false;
                car.setMileage(mileage);
            }
            case "location" -> car.setLocation(limitText(value, 120));
            case "contact" -> car.setSellerContact(limitText(value, 180));
            case "description" -> car.setDescription(isSkip(value) ? null : limitText(value, 1000));
            case "fuel" -> {
                String fuel = normalizeFuel(value);
                if (fuel == null) return false;
                car.setFuelType(fuel);
            }
            case "transmission" -> {
                String transmission = normalizeTransmission(value);
                if (transmission == null) return false;
                car.setTransmission(transmission);
            }
            case "carType" -> {
                String carType = normalizeCarType(value);
                if (carType == null) return false;
                car.setCarType(carType);
            }
            case "photo" -> car.setImageUrl(value);
            default -> { return false; }
        }
        return true;
    }

    private static Integer parsePositiveInt(String value) {
        if (value == null) return null;
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;
        try { return Integer.parseInt(digits); } catch (NumberFormatException e) { return null; }
    }

    private static String limitText(String value, int maxLength) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength).trim();
    }

    private static String formatCzk(Integer value) {
        return value == null ? "-" : String.format("%,d Kč", value).replace(",", " ");
    }

    private static boolean isSkip(String value) {
        return value == null || "/skip".equalsIgnoreCase(value) || "skip".equalsIgnoreCase(value);
    }

    private static String normalizeFuel(String value) {
        String v = value == null ? "" : value.trim().toLowerCase();
        if (v.contains("diesel") || v.contains("nafta") || v.contains("диз")) return "DIESEL";
        if (v.contains("plugin") || v.contains("plug") || v.contains("phev")) return "PLUGIN_HYBRID";
        if (v.contains("hybrid") || v.contains("гібр") || v.contains("гибр")) return "HYBRID";
        if (v.contains("electric") || v.contains("ev") || v.contains("елект") || v.contains("элект")) return "ELECTRIC";
        if (v.contains("lpg")) return "LPG";
        if (v.contains("cng")) return "CNG";
        if (v.contains("petrol") || v.contains("benzin") || v.contains("benz") || v.contains("бенз")) return "PETROL";
        return null;
    }

    private static String normalizeTransmission(String value) {
        String v = value == null ? "" : value.trim().toLowerCase();
        if (v.contains("auto") || v.contains("dsg") || v.contains("авто")) return "AUTOMATIC";
        if (v.contains("manual") || v.contains("man") || v.contains("мех")) return "MANUAL";
        return null;
    }

    private static String normalizeCarType(String value) {
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

    public static final class Draft {
        public final Long ownerChatId;
        public String sellerUsername;
        public String brand;
        public String title;
        public Integer priceValue;
        public Integer year;
        public Integer mileage;
        public String location;
        public String fuelType;
        public String transmission;
        public String carType;
        public String sellerContact;
        public String imageUrl;
        public String description;

        private Draft(Long ownerChatId) { this.ownerChatId = ownerChatId; }

        public boolean isComplete() {
            return ownerChatId != null && notBlank(brand) && notBlank(title) && priceValue != null && year != null
                    && mileage != null && notBlank(location) && notBlank(fuelType) && notBlank(transmission)
                    && notBlank(carType) && notBlank(sellerContact);
        }

        private static boolean notBlank(String value) { return value != null && !value.isBlank(); }
    }

    public record EditSession(Long carId, String field) { }

    public enum OwnershipResult { UPDATED, NOT_OWNER, NOT_FOUND }

    public record UpdateResult(Status status, CarEntity car) {
        public static UpdateResult updated(CarEntity car) { return new UpdateResult(Status.UPDATED, car); }
        public static UpdateResult invalidValue() { return new UpdateResult(Status.INVALID_VALUE, null); }
        public static UpdateResult notOwner() { return new UpdateResult(Status.NOT_OWNER, null); }
        public static UpdateResult notFound() { return new UpdateResult(Status.NOT_FOUND, null); }
        public enum Status { UPDATED, INVALID_VALUE, NOT_OWNER, NOT_FOUND }
    }

    public record ReviewResult(Status status, CarEntity car) {
        public static ReviewResult updated(CarEntity car) { return new ReviewResult(Status.UPDATED, car); }
        public static ReviewResult alreadyReviewed(CarEntity car) { return new ReviewResult(Status.ALREADY_REVIEWED, car); }
        public static ReviewResult notFound() { return new ReviewResult(Status.NOT_FOUND, null); }
        public enum Status { UPDATED, ALREADY_REVIEWED, NOT_FOUND }
    }
}

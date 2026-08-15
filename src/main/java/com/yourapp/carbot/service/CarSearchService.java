package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.entity.UserFilterEntity;
import com.yourapp.carbot.repository.CarRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CarSearchService {

    private final CarRepository carRepository;
    private final UserFilterService userFilterService;
    private final CarFilterMatcher carFilterMatcher;

    public CarSearchService(CarRepository carRepository,
                            UserFilterService userFilterService,
                            CarFilterMatcher carFilterMatcher) {
        this.carRepository = carRepository;
        this.userFilterService = userFilterService;
        this.carFilterMatcher = carFilterMatcher;
    }

    public List<CarEntity> findMatchingCars(Long chatId, int limit) {
        UserFilterEntity filter = userFilterService.findByChatId(chatId).orElse(null);

        if (filter == null) {
            System.out.println("DEBUG SEARCH: filter is null for chatId=" + chatId);
            return List.of();
        }

        List<CarEntity> allCars = carRepository.findAllByListingStatusOrderByCreatedAtDesc("ACTIVE");
        List<CarEntity> searchableCars = allCars.stream()
                .filter(this::isSearchableCar)
                .toList();

        long carTypePassed = 0;
        long brandPassed = 0;
        long modelPassed = 0;
        long pricePassed = 0;
        long locationPassed = 0;
        long mileagePassed = 0;
        long fuelPassed = 0;
        long transmissionPassed = 0;
        long yearPassed = 0;
        long finalPassed = 0;

        for (CarEntity car : searchableCars) {
            FilterCheckResult check = carFilterMatcher.check(car, filter);

            if (check.carTypeOk()) carTypePassed++;
            if (check.brandOk()) brandPassed++;
            if (check.modelOk()) modelPassed++;
            if (check.maxPriceOk()) pricePassed++;
            if (check.locationOk()) locationPassed++;
            if (check.mileageOk()) mileagePassed++;
            if (check.fuelTypeOk()) fuelPassed++;
            if (check.transmissionOk()) transmissionPassed++;
            if (check.yearOk()) yearPassed++;
            if (check.result()) finalPassed++;
        }

        System.out.println("========== DEBUG SEARCH ==========");
        System.out.println("ALL CARS IN DB = " + allCars.size());
        System.out.println("SEARCHABLE CARS = " + searchableCars.size());
        System.out.println("FILTER:");
        System.out.println("carType = " + filter.getCarType());
        System.out.println("brand = " + filter.getBrand());
        System.out.println("modelQuery = " + filter.getModelQuery());
        System.out.println("maxPrice = " + filter.getMaxPrice());
        System.out.println("location = " + filter.getLocation());
        System.out.println("maxMileage = " + filter.getMaxMileage());
        System.out.println("fuelType = " + filter.getFuelType());
        System.out.println("transmission = " + filter.getTransmission());
        System.out.println("yearFrom = " + filter.getYearFrom());
        System.out.println("----------------------------------");
        System.out.println("PASSED carType = " + carTypePassed);
        System.out.println("PASSED brand = " + brandPassed);
        System.out.println("PASSED model = " + modelPassed);
        System.out.println("PASSED price = " + pricePassed);
        System.out.println("PASSED location = " + locationPassed);
        System.out.println("PASSED mileage = " + mileagePassed);
        System.out.println("PASSED fuelType = " + fuelPassed);
        System.out.println("PASSED transmission = " + transmissionPassed);
        System.out.println("PASSED year = " + yearPassed);
        System.out.println("FINAL MATCHED = " + finalPassed);
        System.out.println("==================================");


        List<CarEntity> sortedMatched = searchableCars.stream()
                .filter(car -> carFilterMatcher.matches(car, filter))
                .sorted(Comparator.comparing(
                        CarEntity::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .toList();

        List<CarEntity> matched = deduplicateSearchResults(sortedMatched, limit);

        System.out.println("========== FINAL MATCHED CARS ==========");
        matched.forEach(car -> System.out.println(
                "title=" + car.getTitle()
                        + " | brand=" + car.getBrand()
                        + " | price=" + car.getPriceValue()
                        + " | mileage=" + car.getMileage()
                        + " | year=" + car.getYear()
                        + " | fuel=" + car.getFuelType()
                        + " | transmission=" + car.getTransmission()
                        + " | type=" + car.getCarType()
                        + " | location=" + car.getLocation()
                        + " | createdAt=" + car.getCreatedAt()
        ));
        System.out.println("========================================");

        return matched;
    }

    private List<CarEntity> deduplicateSearchResults(List<CarEntity> cars, int limit) {
        List<CarEntity> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (CarEntity car : cars) {
            String key = searchDuplicateKey(car);
            if (!seen.add(key)) {
                continue;
            }

            result.add(car);
            if (result.size() >= limit) {
                break;
            }
        }

        return result;
    }

    private String searchDuplicateKey(CarEntity car) {
        if (car == null) {
            return "";
        }

        String title = normalizeForDuplicateKey(car.getTitle());
        String location = normalizeForDuplicateKey(car.getLocation());

        if (title.isBlank()) {
            return "url:" + normalizeForDuplicateKey(car.getUrl());
        }

        return String.join("|",
                title,
                valueForKey(car.getPriceValue()),
                valueForKey(car.getYear()),
                valueForKey(car.getMileage()),
                location
        );
    }

    private String valueForKey(Object value) {
        return value == null ? "-" : value.toString();
    }

    private String normalizeForDuplicateKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

        return normalized.replaceAll("\\s+", " ");
    }

    private boolean isSearchableCar(CarEntity car) {
        if (car == null) {
            return false;
        }

        if (!"TIPCARS".equalsIgnoreCase(valueForKey(car.getSource()))) {
            return true;
        }

        String url = valueForKey(car.getUrl()).toLowerCase(Locale.ROOT).replaceAll("[?#].*$", "");
        if (!url.matches("https://www\\.tipcars\\.com/.+-\\d+\\.html")) {
            return false;
        }

        String urlBrand = extractTipCarsUrlBrand(url);
        String carBrand = normalizeBrandForComparison(car.getBrand());

        return urlBrand == null || carBrand == null || urlBrand.equals(carBrand);
    }

    private String extractTipCarsUrlBrand(String url) {
        Matcher matcher = Pattern.compile("tipcars\\.com/([^/]+)/").matcher(valueForKey(url).toLowerCase(Locale.ROOT));
        if (!matcher.find()) {
            return null;
        }

        return normalizeTipCarsUrlBrand(matcher.group(1));
    }

    private String normalizeTipCarsUrlBrand(String value) {
        String normalized = normalizeBrandForComparison(value);
        if (normalized == null) {
            return null;
        }

        if (normalized.startsWith("mercedes_benz") || normalized.startsWith("mercedes")) return "mercedes";
        if (normalized.startsWith("volkswagen") || normalized.startsWith("vw")) return "volkswagen";
        if (normalized.startsWith("skoda")) return "skoda";
        if (normalized.startsWith("citroen")) return "citroen";
        if (normalized.startsWith("renault")) return "renault";
        if (normalized.startsWith("peugeot")) return "peugeot";
        if (normalized.startsWith("toyota")) return "toyota";
        if (normalized.startsWith("audi")) return "audi";
        if (normalized.startsWith("bmw")) return "bmw";
        if (normalized.startsWith("ford")) return "ford";
        if (normalized.startsWith("opel")) return "opel";
        if (normalized.startsWith("seat")) return "seat";
        if (normalized.startsWith("hyundai")) return "hyundai";
        if (normalized.startsWith("kia")) return "kia";
        if (normalized.startsWith("nissan")) return "nissan";
        if (normalized.startsWith("fiat")) return "fiat";
        if (normalized.startsWith("honda")) return "honda";
        if (normalized.startsWith("volvo")) return "volvo";
        if (normalized.startsWith("dacia")) return "dacia";
        if (normalized.startsWith("mazda")) return "mazda";
        if (normalized.startsWith("subaru")) return "subaru";
        if (normalized.startsWith("mitsubishi")) return "mitsubishi";
        if (normalized.startsWith("alfa_romeo")) return "alfa_romeo";
        if (normalized.startsWith("land_rover")) return "land_rover";

        return normalized;
    }

    private String normalizeBrandForComparison(String value) {
        String normalized = normalizeForDuplicateKey(value).replace(' ', '_');
        if (normalized.isBlank()) {
            return null;
        }

        return switch (normalized) {
            case "mercedes_benz", "mercedes" -> "mercedes";
            case "volkswagen", "vw" -> "volkswagen";
            case "skoda" -> "skoda";
            case "citroen" -> "citroen";
            case "alfa_romeo" -> "alfa_romeo";
            case "land_rover" -> "land_rover";
            default -> normalized;
        };
    }

    private boolean looksLikeRenault(CarEntity car) {
        if (car == null) {
            return false;
        }

        String title = car.getTitle() == null ? "" : car.getTitle().toUpperCase();
        String brand = car.getBrand() == null ? "" : car.getBrand().toUpperCase();

        return brand.contains("RENAULT")
                || title.contains("RENAULT")
                || title.contains("CLIO")
                || title.contains("MEGANE")
                || title.contains("MÉGANE")
                || title.contains("SCENIC")
                || title.contains("FLUENCE")
                || title.contains("LAGUNA")
                || title.contains("CAPTUR")
                || title.contains("KADJAR")
                || title.contains("KOLEOS")
                || title.contains("TWINGO");
    }
}

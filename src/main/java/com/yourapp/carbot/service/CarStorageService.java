package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import com.yourapp.carbot.repository.CarRepository;
import com.yourapp.carbot.service.dto.CarDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CarStorageService {

    private static final Logger log = LoggerFactory.getLogger(CarStorageService.class);

    private final CarRepository carRepository;

    public CarStorageService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public List<CarEntity> saveNewCars(List<CarDto> cars) {

        if (cars == null || cars.isEmpty()) {
            log.info("No cars to save");
            return List.of();
        }

        Set<String> incomingUrls = extractValidUrls(cars);

        if (incomingUrls.isEmpty()) {
            log.info("No valid URLs found in incoming cars");
            return List.of();
        }

        List<CarEntity> existingCars = carRepository.findByUrlIn(incomingUrls);

        Map<String, CarEntity> existingByUrl = new HashMap<>();
        for (CarEntity car : existingCars) {
            if (car != null && car.getUrl() != null) {
                existingByUrl.put(car.getUrl(), car);
            }
        }

        int alreadyExistingCount = existingByUrl.size();
        int invalidCount = 0;
        int updatedCount = 0;

        List<CarEntity> savedNewCars = new ArrayList<>();

        for (CarDto car : cars) {

            if (car == null) {
                invalidCount++;
                log.warn("STORAGE SKIP reason=null_car");
                continue;
            }

            normalizeIncoming(car);

            String url = clean(car.getUrl());
            Integer priceValue = extractPriceValue(car.getPrice());

            if (!isValidForSave(car, priceValue)) {
                invalidCount++;
                log.warn("STORAGE SKIP reason=invalid_car source={} title={} url={}",
                        safe(car.getSource()),
                        safe(car.getTitle()),
                        safe(url));
                continue;
            }

            CarEntity existing = existingByUrl.get(url);

            if (existing != null) {
                boolean changed = enrichExisting(existing, car, priceValue);

                if (changed) {
                    try {
                        CarEntity updated = carRepository.save(existing);
                        updatedCount++;

                        log.info("STORAGE UPDATED source={} title={} url={}",
                                safe(updated.getSource()),
                                safe(updated.getTitle()),
                                safe(updated.getUrl()));
                    } catch (Exception e) {
                        log.error("Failed to update existing car. url={}", url, e);
                    }
                }

                continue;
            }

            try {
                CarEntity entity = toEntity(car, priceValue);
                CarEntity saved = carRepository.save(entity);

                savedNewCars.add(saved);
                existingByUrl.put(url, saved);

                log.info("STORAGE SAVED source={} title={} url={}",
                        safe(saved.getSource()),
                        safe(saved.getTitle()),
                        safe(saved.getUrl()));

            } catch (DataIntegrityViolationException e) {
                log.warn("Car already saved concurrently. url={}", url);

            } catch (Exception e) {
                log.error("Failed to save car. url={}", url, e);
            }
        }

        log.info("Incoming cars={}, already existing={}, invalid={}, updated={}, newly saved={}",
                cars.size(),
                alreadyExistingCount,
                invalidCount,
                updatedCount,
                savedNewCars.size()
        );

        return savedNewCars;
    }

    private void normalizeIncoming(CarDto car) {
        car.setSource(clean(car.getSource()));
        car.setTitle(clean(car.getTitle()));
        car.setPrice(clean(car.getPrice()));
        car.setLocation(normalizeLocation(car.getLocation()));
        car.setUrl(clean(car.getUrl()));
        car.setImageUrl(normalizeImageUrl(car.getImageUrl()));
        car.setBrand(normalizeBrand(car.getBrand(), car.getTitle()));
        car.setFuelType(normalizeFuelType(car.getFuelType(), car.getTitle()));
        car.setTransmission(normalizeTransmission(car.getTransmission(), car.getTitle(), car.getFuelType()));
        car.setCarType(normalizeCarType(car.getCarType(), car.getTitle()));

        if (car.getYear() != null && (car.getYear() < 1990 || car.getYear() > LocalDateTime.now().getYear())) {
            car.setYear(extractYearFromText(car.getTitle() + " " + safe(car.getPrice()) + " " + safe(car.getLocation())));
            if (car.getYear() != null && (car.getYear() < 1990 || car.getYear() > LocalDateTime.now().getYear())) {
                car.setYear(null);
            }
        }

        if (car.getMileage() != null && (car.getMileage() < 0 || car.getMileage() > 500000)) {
            car.setMileage(null);
        }
    }

    private boolean enrichExisting(CarEntity existing, CarDto incoming, Integer priceValue) {
        boolean changed = false;

        changed |= updateIfDifferent(existing::getSource, existing::setSource, incoming.getSource());
        changed |= updateIfDifferent(existing::getTitle, existing::setTitle, incoming.getTitle());
        changed |= updateIfDifferent(existing::getPrice, existing::setPrice, incoming.getPrice());
        changed |= updateIfDifferent(existing::getPriceValue, existing::setPriceValue, priceValue);
        changed |= updateIfDifferent(existing::getLocation, existing::setLocation, normalizeLocation(incoming.getLocation()));
        changed |= updateIfDifferent(existing::getImageUrl, existing::setImageUrl, normalizeImageUrl(incoming.getImageUrl()));

        changed |= updateIfDifferent(existing::getBrand, existing::setBrand, normalizeBrand(incoming.getBrand(), incoming.getTitle()));
        changed |= updateIfDifferent(existing::getYear, existing::setYear, incoming.getYear());
        changed |= updateIfDifferent(existing::getMileage, existing::setMileage, incoming.getMileage());
        changed |= updateIfDifferent(existing::getFuelType, existing::setFuelType, normalizeFuelType(incoming.getFuelType(), incoming.getTitle()));
        changed |= updateIfDifferent(existing::getTransmission, existing::setTransmission,
                normalizeTransmission(incoming.getTransmission(), incoming.getTitle(), incoming.getFuelType()));
        changed |= updateIfDifferent(existing::getCarType, existing::setCarType, normalizeCarType(incoming.getCarType(), incoming.getTitle()));

        return changed;
    }

    private Set<String> extractValidUrls(List<CarDto> cars) {
        Set<String> urls = new LinkedHashSet<>();

        for (CarDto car : cars) {
            if (car == null) {
                continue;
            }

            String url = clean(car.getUrl());
            if (url != null) {
                urls.add(url);
            }
        }

        return urls;
    }

    private boolean isValidForSave(CarDto car, Integer priceValue) {
        String url = clean(car.getUrl());
        String title = clean(car.getTitle());
        String price = clean(car.getPrice());
        String location = normalizeLocation(car.getLocation());
        String brand = normalizeBrand(car.getBrand(), car.getTitle());
        String fuelType = normalizeFuelType(car.getFuelType(), car.getTitle());
        String transmission = normalizeTransmission(car.getTransmission(), car.getTitle(), car.getFuelType());
        String carType = normalizeCarType(car.getCarType(), car.getTitle());

        if (url == null || title == null || price == null || priceValue == null) {
            return false;
        }

        if (!url.startsWith("http")) {
            return false;
        }

        if (priceValue <= 0) {
            return false;
        }

        if (looksLikeAccessoryOrPart(title) && !looksLikeRealCar(title)) {
            return false;
        }

        if (looksLikeCommercialVehicle(title)) {
            return false;
        }

        if (looksLikeBadTitle(title)) {
            return false;
        }

        if (location != null && looksLikeInvalidLocation(location)) {
            return false;
        }

        return !(brand == null && fuelType == null && transmission == null && carType == null && !looksLikeRealCar(title));
    }

    private CarEntity toEntity(CarDto car, Integer priceValue) {
        CarEntity entity = new CarEntity();
        entity.setSource(clean(car.getSource()));
        entity.setTitle(clean(car.getTitle()));
        entity.setPrice(clean(car.getPrice()));
        entity.setPriceValue(priceValue);
        entity.setLocation(normalizeLocation(car.getLocation()));
        entity.setUrl(clean(car.getUrl()));
        entity.setImageUrl(normalizeImageUrl(car.getImageUrl()));
        entity.setCreatedAt(LocalDateTime.now());

        entity.setBrand(normalizeBrand(car.getBrand(), car.getTitle()));
        entity.setYear(car.getYear());
        entity.setMileage(car.getMileage());
        entity.setFuelType(normalizeFuelType(car.getFuelType(), car.getTitle()));
        entity.setTransmission(normalizeTransmission(car.getTransmission(), car.getTitle(), car.getFuelType()));
        entity.setCarType(normalizeCarType(car.getCarType(), car.getTitle()));

        return entity;
    }

    private Integer extractPriceValue(String price) {
        if (price == null || price.isBlank()) {
            return null;
        }

        String lower = price.toLowerCase(Locale.ROOT);
        if (lower.contains("dohodou") || lower.contains("nabídněte") || lower.contains("nabidnete") || lower.contains("v textu")) {
            Matcher matcher = Pattern.compile("(\\d[\\d\\s]{2,})").matcher(price);
            if (matcher.find()) {
                String digits = matcher.group(1).replaceAll("\\D", "");
                if (!digits.isBlank()) {
                    try {
                        return Integer.parseInt(digits);
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse price value from '{}'", price);
                        return null;
                    }
                }
            }
            return null;
        }

        Matcher matcher = Pattern.compile("\\d+").matcher(price);
        StringBuilder digits = new StringBuilder();

        while (matcher.find()) {
            digits.append(matcher.group());
        }

        if (digits.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse price value from '{}'", price);
            return null;
        }
    }

    private String normalizeLocation(String location) {
        String value = clean(location);
        if (value == null) {
            return null;
        }

        value = value.replace("Lokalita:", "").trim();
        value = value.replaceAll("\\s+", " ").trim();

        if (looksLikeInvalidLocation(value)) {
            return null;
        }

        Matcher matcher = Pattern.compile("\\b\\d{3}\\s?\\d{2}\\s+.+").matcher(value);
        if (matcher.find()) {
            return matcher.group().trim();
        }

        return value;
    }

    private String normalizeImageUrl(String imageUrl) {
        String value = clean(imageUrl);
        if (value == null || "-".equals(value)) {
            return null;
        }

        if (value.startsWith("//")) {
            return "https:" + value;
        }

        return value;
    }

    private boolean looksLikeInvalidLocation(String location) {
        String lower = location.toLowerCase(Locale.ROOT);

        return lower.equals("okolí: km")
                || lower.equals("okoli: km")
                || lower.equals("okolí")
                || lower.equals("okoli")
                || lower.equals("km")
                || lower.equals("možné v barvě")
                || lower.equals("mozne v barve")
                || lower.equals("půjčení auta")
                || lower.equals("pujceni auta")
                || lower.startsWith("- top -")
                || lower.contains("[8.4. 2026]")
                || lower.contains("hlavní stránka")
                || lower.contains("hlavni stranka")
                || lower.contains("inzerát č.")
                || lower.contains("inzerat c.")
                || lower.contains("inzerát č")
                || lower.contains("inzerat c")
                || lower.contains("všechny rubriky")
                || lower.contains("vsechny rubriky");
    }

    private String normalizeBrand(String brand, String title) {
        String value = clean(brand);
        if (value != null) {
            String upper = value.toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');

            Set<String> allowed = Set.of(
                    "AUDI", "BMW", "MERCEDES", "VOLKSWAGEN", "SKODA", "CITROEN",
                    "TOYOTA", "FORD", "RENAULT", "HYUNDAI", "KIA", "PEUGEOT",
                    "OPEL", "MAZDA", "HONDA", "VOLVO", "SEAT", "DACIA", "FIAT",
                    "TESLA", "CUPRA", "DODGE", "SUBARU", "NISSAN", "SUZUKI",
                    "JEEP", "MINI", "LEXUS", "PORSCHE", "MITSUBISHI", "BYD",
                    "MG", "DS", "LAND_ROVER", "ALFA_ROMEO", "CHEVROLET",
                    "SSANGYONG", "CHRYSLER", "LOTUS", "LAMBORGHINI", "FERRARI",
                    "JAECOO", "OMODA", "SWM", "SMART", "INFINITI"
            );

            if (allowed.contains(upper)) {
                return upper;
            }
        }

        String t = safe(title).toLowerCase(Locale.ROOT);

        if (t.contains("škoda") || t.contains("skoda")) return "SKODA";
        if (t.contains("audi")) return "AUDI";
        if (t.contains("bmw")) return "BMW";
        if (t.contains("mercedes")) return "MERCEDES";
        if (t.contains("volkswagen") || t.contains("vw ")) return "VOLKSWAGEN";
        if (t.contains("toyota")) return "TOYOTA";
        if (t.contains("ford")) return "FORD";
        if (t.contains("renault")) return "RENAULT";
        if (t.contains("hyundai")) return "HYUNDAI";
        if (t.contains("kia")) return "KIA";
        if (t.contains("chevrolet") || t.contains("daewoo") || t.contains("calos") || t.contains("corvette") || t.contains("camaro") || t.contains("ssr")) return "CHEVROLET";
        if (t.contains("ssangyong") || t.contains("ssang yong") || t.contains("korando")) return "SSANGYONG";
        if (t.contains("chrysler") || t.contains("pacifica")) return "CHRYSLER";
        if (t.contains("lotus") || t.contains("emira")) return "LOTUS";
        if (t.contains("lamborghini") || t.contains("hurac")) return "LAMBORGHINI";
        if (t.contains("ferrari") || t.contains("california")) return "FERRARI";
        if (t.contains("jaecoo")) return "JAECOO";
        if (t.contains("omoda")) return "OMODA";
        if (t.contains("swm")) return "SWM";
        if (t.contains("smart") || t.contains("fortwo")) return "SMART";
        if (t.contains("infiniti") || t.contains("fx35") || t.contains("fx-35")) return "INFINITI";
        if (t.contains("peugeot")) return "PEUGEOT";
        if (t.contains("opel")) return "OPEL";
        if (t.contains("mazda")) return "MAZDA";
        if (t.contains("honda") || t.contains("acura") || t.contains("rdx")) return "HONDA";
        if (t.contains("volvo")) return "VOLVO";
        if (t.contains("seat")) return "SEAT";
        if (t.contains("dacia") || t.contains("duster") || t.contains("sandero") || t.contains("logan") || t.contains("dokker") || t.contains("lodgy") || t.contains("jogger")) return "DACIA";
        if (t.contains("fiat")) return "FIAT";
        if (t.contains("tesla")) return "TESLA";
        if (t.contains("cupra")) return "CUPRA";
        if (t.contains("dodge")) return "DODGE";
        if (t.contains("subaru")) return "SUBARU";
        if (t.contains("nissan")) return "NISSAN";
        if (t.contains("suzuki")) return "SUZUKI";
        if (t.contains("jeep")) return "JEEP";
        if (t.contains("mini")) return "MINI";
        if (t.contains("lexus")) return "LEXUS";
        if (t.contains("porsche")) return "PORSCHE";
        if (t.contains("mitsubishi")) return "MITSUBISHI";
        if (t.contains("byd")) return "BYD";
        if (t.contains("mg ")) return "MG";
        if (t.contains("ds ")) return "DS";
        if (t.contains("land rover")) return "LAND_ROVER";
        if (t.contains("alfa romeo")) return "ALFA_ROMEO";

        return null;
    }

    private String normalizeFuelType(String fuelType, String title) {
        String lower = (safe(fuelType) + " " + safe(title)).toLowerCase(Locale.ROOT);
        String titleLower = safe(title).toLowerCase(Locale.ROOT);
        String value = clean(fuelType);
        if (value != null) {
            String upper = value.toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');

            if (containsAny(titleLower, "plug-in hybrid", "plug-inhybrid", "plugin hybrid", "pluginhybrid", "plug in hybrid", "plug-in", "plug in", "phev")) {
                return "PLUGIN_HYBRID";
            }
            if ("PETROL".equals(upper) && containsAny(titleLower, "hybrid", "hev", "mhev", "mild hybrid", "e-tec")) {
                return "HYBRID";
            }

            if ("ELECTRIC".equals(upper)
                    && titleLower.contains("carens")
                    && Pattern.compile("\\b1[\\.,]7\\b", Pattern.CASE_INSENSITIVE).matcher(titleLower).find()) {
                return "DIESEL";
            }

            if (("ELECTRIC".equals(upper) || "DIESEL".equals(upper))
                    && containsAny(titleLower, "twinspark", "twin spark", "1.8tbi", "1,8tbi", " tbi", "jts", "giulietta sprint")) {
                return "PETROL";
            }

            if ("ELECTRIC".equals(upper)
                    && containsAny(titleLower, "touareg", "passat", "golf", "tiguan", "t-roc", "touran")
                    && !containsAny(titleLower, "id.3", "id3", "id.4", "id4", "id.5", "id5", "e-golf", "egolf", "electric", "elektro", "kwh", "hybrid", "gte", "phev", "plug-in", "plugin")) {
                return containsAny(titleLower, "tdi", "nafta", "naftov", "touareg") ? "DIESEL" : null;
            }

            if (Set.of("PETROL", "DIESEL", "HYBRID", "PLUGIN_HYBRID", "ELECTRIC", "LPG", "CNG").contains(upper)) {
                return upper;
            }
        }

        if (containsAny(lower, "plug-in hybrid", "plug-inhybrid", "plugin hybrid", "pluginhybrid", "plug in hybrid", "plug-in", "plug in", "phev")) {
            return "PLUGIN_HYBRID";
        }

        if (containsAny(lower, "530xd", "750xd", "220cdi")
                || (containsAny(lower, "v 250", "v250", "v 250l", "v250l", "v250d", "v 250d", "v300d", "v 300d") && !containsAny(lower, "benzin", "benz", "petrol", "lpg"))
                || (lower.contains("allroad") && !containsAny(lower, "tfsi", "tsi", "petrol", "benzin", "benz"))) {
            return "DIESEL";
        }

        if (containsAny(lower, "inster", "puma gen-e", "puma gen e", "cupra born", "electric", "elektro", "bev", "kwh")) {
            return "ELECTRIC";
        }

        if (containsAny(lower, "g-tec", "g tec", "gtec")) {
            return "CNG";
        }

        if (containsAny(lower, "hybrid", "hev", "mhev", "mild hybrid", "tsi iv", "tsi i v", "superb iv", "kodiaq iv")) {
            return "HYBRID";
        }

        if (containsAny(lower, "lpg")) {
            return "LPG";
        }

        if (containsAny(lower, "cng")) {
            return "CNG";
        }

        if (containsAny(lower, "diesel", "nafta", "naftov", "tdi", "tdci", "cdi", "crdi", "hdi", "dci", "multijet")
                || (lower.contains("carens") && Pattern.compile("\\b1[\\.,]7\\b", Pattern.CASE_INSENSITIVE).matcher(lower).find())
                || (lower.contains("sorento") && Pattern.compile("\\b2[\\.,]2\\b", Pattern.CASE_INSENSITIVE).matcher(lower).find())
                || Pattern.compile("\\b\\d[\\.,]\\d\\s*d\\b", Pattern.CASE_INSENSITIVE).matcher(lower).find()) {
            return "DIESEL";
        }

        if (containsAny(lower, "sl 600", "sl600", "slk55", "s500", "gl 500", "gl500", "v12", "v8", "space star", "spacestar", "eclipse cross 1.5", "eclipse cross 1,5")) {
            return "PETROL";
        }

        if (containsAny(lower, "petrol", "benzin", "benz", "benzinovy", "benzín", "benzínový", "tsi", "tfsi", "fsi", "gdi", "tgdi", "sce", "puretech", "ecoboost", "1.0eb", "1,0eb", "1.2eb", "1,2eb", "camaro", "corvette", "chevrolet ssr", "civic 1.8", "civic 1,8", "civic ufo", "legend 2.7", "legend 2,7", "twinspark", "twin spark", "1.8tbi", "1,8tbi", "giulietta sprint", " jts")) {
            return "PETROL";
        }

        return null;
    }

    private String normalizeTransmission(String transmission, String title, String fuelType) {
        String lower = (safe(transmission) + " " + safe(title)).toLowerCase(Locale.ROOT);
        String tokens = " " + lower.replaceAll("[^a-z0-9]+", " ") + " ";

        if (containsAny(tokens, " 6mt ", " 5mt ", " mt ", " 6rychl ", " 6ti ", " mt6 ", " m6 ", " 6m ", " 6mp ")
                || containsAny(lower, "manual", "manuÄ‚Ë‡l", "manualni", "manuÄ‚Ë‡lnÄ‚Â­", "6ti rychl", "6 rychl", "6ti rychlost", "6 rychlost")) {
            if (lower.contains("silverado")
                    && !containsAny(lower, "manual", "manuĂ„â€šĂ‹â€ˇl", "manualni", " man. ", " 5mt ", " 6mt ", "6ti rychl", "6 rychl")) {
                return null;
            }
            return "MANUAL";
        }

        String titleOnly = safe(title).toLowerCase(Locale.ROOT);
        boolean titleHasExplicitAutomatic = containsAny(titleOnly, "selespeed", "automat", "automatic", " aut. ", " a/t ", " at6", " at8", "at7", "7at", "at8", "8at", "dsg", "dct", "cvt", "s-tronic", "s tronic", "stronic", "tiptronic");
        if (!titleHasExplicitAutomatic
                && containsAny(titleOnly, "alfa romeo 159", "alfa 159", "alfa romeo 147", "alfa 147", "giulietta", "giuletta", "alfa romeo sportwagon", "accord", "crx", "delsol", "cr-v", "cr v", "crv")) {
            return null;
        }

        if (containsAny(tokens, " aut ", " at ", " mta ")
                || containsAny(lower, "automat", "automatic", "dsg", "dct", "at7", "7at", "at8", "8at", "atled", "tiptronic", "s tronic", "stronic", "cvt")) {
            return "AUTOMATIC";
        }

        String value = clean(transmission);
        if (value != null) {
            String upper = value.toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');

            if (Set.of("MANUAL", "AUTOMATIC").contains(upper)) {
                return upper;
            }
        }

        String normalizedFuel = normalizeFuelType(fuelType, title);
        if ("ELECTRIC".equals(normalizedFuel)) {
            return null;
        }

        return null;
    }

    private String normalizeCarType(String carType, String title) {
        String lower = safe(title).toLowerCase(Locale.ROOT);

        if (containsAny(lower, "touran", "v 250", "v250", "v 250l", "v250l", "v 250d", "v250d", "v300d", "grandis")) {
            return "MINIVAN";
        }

        if (containsAny(lower, "land cruiser", "korando", "rdx", "e-tron", "etron", "x1", "x2", "x3", "x5", "x6", "x7", "glc", "gle", "gls", "gla", "glb", "gl 500", "gl500", "x-trail", "x trail", "pathfinder", "grandland", "grandal", "cx-5", "cx 5", "cx5", "cx-7", "cx 7", "cx7", "niro", "eqa", "mustang mach-e", "mustang mache", "xc40", "xc 40", "xc60", "xc 60", "xc70", "xc 70", "xc90", "xc 90", "jaecoo", "omoda", "swm g1", "swm g01", "inster", "puma gen-e", "puma gen e", "infiniti fx", "fx35", "fx-35")) {
            return "SUV";
        }

        if (containsAny(lower, "volvo s60", " s60 ", " s 60 ", "honda legend", " legend ")) {
            return "SEDAN";
        }

        if (containsAny(lower, "volvo v40", " v40 ", " v 40 ")) {
            return "HATCHBACK";
        }

        if (containsAny(lower, "f-150", "f150")) {
            return "PICKUP";
        }

        if (containsAny(lower, "3000gt", "3000 gt", "cr-z", "cr z", "clk")) {
            return "COUPE";
        }

        if (containsAny(lower, "smart roadster")) {
            return "CABRIO";
        }

        if (containsAny(lower, "clubman")) {
            return "WAGON";
        }

        if (containsAny(lower, "proceed", "pro ceed")) {
            return "WAGON";
        }

        if (containsAny(lower, "gran coupe", "grand coupe", "gran coupé", "grand coupé")) {
            return "SEDAN";
        }

        if (containsAny(lower, "ceed", "ceeÂ´d", "cee'd", "cee d")
                && containsAny(lower, " sw", "sw ", "wagon", "kombi", "combi")) {
            return "WAGON";
        }

        if (containsAny(lower, "logan mcv", "logan combi", "logan kombi")) {
            return "WAGON";
        }

        if (containsAny(lower, "logan")) {
            return "SEDAN";
        }

        if (containsAny(lower, "opel karl", "astra k", "astra j", "opel astra", "opel corsa", "peugeot 1007", "peugeot 106", "peugeot 206", "peugeot 207", "peugeot 208", "peugeot 308", "mazda 2", "nissan note", "nissan micra", "micra", "kalos", "calos", "ceed", "cee´d", "cee'd", "cee d", "seat leon", "smart fortwo", "citroen c4", "citroën c4")) {
            return "HATCHBACK";
        }

        if (containsAny(lower, "a180", "a 180", "a180d", "a 180d", "a200", "a 200", "a200d", "a 200d", "a35")) {
            return "HATCHBACK";
        }

        if (containsAny(lower, "giulietta", "giuletta")) {
            return "HATCHBACK";
        }

        if (containsAny(lower, "sportwagon")) {
            return "WAGON";
        }

        String value = clean(carType);
        if (value != null) {
            String upper = value.toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');

            if (Set.of("SEDAN", "HATCHBACK", "WAGON", "SUV", "MINIVAN", "COUPE", "CABRIO", "PICKUP").contains(upper)) {
                return upper;
            }
        }

        if (containsAny(lower, "sports tourer", "sport tourer", "combi", "kombi", "avant", "variant", "touring", "wagon", "e61", "f11", "f31", "g31")) {
            return "WAGON";
        }

        if (containsAny(lower, "mazda 6", "oktavia", "mondeo", "insignia", "insignie", "peugeot 408", "c220", "c 220", "c220d", "e220", "e 220", "e220d", "e300", "e 300", "e300cdi", "cla", "cls", "s350", "s 350", "s500", "s 500", "w220", "750xd", "730d", "740d", "740i", "750i", "540ix")) {
            return "SEDAN";
        }

        if (containsAny(lower, "hatchback")) {
            return "HATCHBACK";
        }

        if (containsAny(lower, "sedan", "limuz")) {
            return "SEDAN";
        }

        if (containsAny(lower, "suv", "crossover", "4x4")) {
            return "SUV";
        }

        if (containsAny(lower, "mpv", "minivan")) {
            return "MINIVAN";
        }

        if (containsAny(lower, "coupe", "coupé", "gran coupe", "gran coupé", "amg gt", "mustang", "tt", "supra", "370z", "350z")) {
            return "COUPE";
        }

        if (containsAny(lower, "slk", "slk55", "sl600", "sl 600", "cabrio", "roadster", "spider", "spyder", "convertible", "cabriolet")) {
            return "CABRIO";
        }

        if (containsAny(lower, "pickup", "pick-up", "crew cab", "amarok", "ranger", "hilux", "navara", "l200", "gladiator")) {
            return "PICKUP";
        }

        return null;
    }

    private Integer extractYearFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(text);
        Integer best = null;
        int currentYear = LocalDateTime.now().getYear();

        while (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));
            if (year >= 1990 && year <= currentYear) {
                best = year;
            }
        }

        return best;
    }

    private boolean containsAny(String text, String... values) {
        if (text == null || text.isBlank() || values == null) {
            return false;
        }

        for (String value : values) {
            if (value != null && !value.isBlank() && text.contains(value)) {
                return true;
            }
        }

        return false;
    }

    private boolean looksLikeRealCar(String title) {
        String t = title.toLowerCase(Locale.ROOT);

        return t.contains("škoda")
                || t.contains("skoda")
                || t.contains("audi")
                || t.contains("bmw")
                || t.contains("mercedes")
                || t.contains("volkswagen")
                || t.contains("vw ")
                || t.contains("toyota")
                || t.contains("ford")
                || t.contains("renault")
                || t.contains("hyundai")
                || t.contains("kia")
                || t.contains("chevrolet")
                || t.contains("corvette")
                || t.contains("camaro")
                || t.contains("peugeot")
                || t.contains("opel")
                || t.contains("mazda")
                || t.contains("honda")
                || t.contains("volvo")
                || t.contains("seat")
                || t.contains("dacia")
                || t.contains("fiat")
                || t.contains("tesla")
                || t.contains("cupra")
                || t.contains("subaru")
                || t.contains("nissan")
                || t.contains("suzuki")
                || t.contains("jeep")
                || t.contains("mini")
                || t.contains("lexus")
                || t.contains("porsche")
                || t.contains("mitsubishi")
                || t.contains("land rover")
                || t.contains("alfa romeo")
                || t.contains("combi")
                || t.contains("kombi")
                || t.contains("hatchback")
                || t.contains("sedan")
                || t.contains("suv")
                || t.contains("wagon")
                || t.contains("automat")
                || t.contains("dsg")
                || t.contains("diesel")
                || t.contains("nafta")
                || t.contains("benzin")
                || t.contains("benzín")
                || t.contains("hybrid")
                || t.contains("phev")
                || t.contains("elektro")
                || t.contains("electric")
                || t.contains("octavia")
                || t.contains("superb")
                || t.contains("fabia")
                || t.contains("yeti")
                || t.contains("rapid")
                || t.contains("leon")
                || t.contains("cooper")
                || t.contains("captur")
                || t.contains("yaris")
                || t.contains("corsa")
                || t.contains("punto")
                || t.contains("megane")
                || t.contains("astra")
                || t.contains("thalia")
                || t.contains("sq5")
                || t.contains("gls");
    }

    private boolean looksLikeAccessoryOrPart(String title) {
        String t = title.toLowerCase(Locale.ROOT);

        return t.contains("pneu")
                || t.contains("pneumatik")
                || t.contains("kola")
                || t.contains("disky")
                || t.contains("ráfky")
                || t.contains("rafky")
                || t.contains("bridgestone")
                || t.contains("goodyear")
                || t.contains("continental")
                || t.contains("dunlop")
                || t.contains("michelin")
                || t.contains("pirelli")
                || t.contains("sport maxx")
                || t.contains("efficientgrip")
                || t.contains("turanza")
                || t.contains("ecocontact")
                || t.contains("větrák")
                || t.contains("vetrak")
                || t.contains("střešní okno")
                || t.contains("stresni okno")
                || t.contains("náhradní díly")
                || t.contains("nahradni dily")
                || t.contains("blatník")
                || t.contains("blatnik")
                || t.contains("kapota")
                || t.contains("maska")
                || t.contains("nárazník")
                || t.contains("naraznik")
                || t.contains("světla")
                || t.contains("svetla")
                || t.contains("autorádio")
                || t.contains("autoradio")
                || t.contains("stresni nosic")
                || t.contains("stresni nosnik")
                || t.contains("zamky centralni")
                || t.contains("centralni zamky")
                || t.contains("zamykani zpatecky")
                || t.contains("sterac")
                || t.contains("sterace")
                || t.matches(".*\\b\\d{3}/\\d{2}r\\d{2}\\b.*")
                || t.matches(".*\\b\\d{3}/\\d{2}/r\\d{2}\\b.*");
    }

    private boolean looksLikeCommercialVehicle(String title) {
        String t = title.toLowerCase(Locale.ROOT);

        return t.contains("sprinter")
                || t.contains("dodávka")
                || t.contains("dodavka")
                || t.contains("transit")
                || t.contains("crafter")
                || t.contains("jumper")
                || t.contains("ducato")
                || t.contains("peugeot boxer")
                || t.contains("master")
                || t.contains("vivaro")
                || t.contains("trafic")
                || t.contains("furgon")
                || t.contains("valník")
                || t.contains("valnik")
                || t.contains("plachta")
                || t.contains("užitkové")
                || t.contains("uzitkove")
                || t.contains("karavan")
                || t.contains("obytná auta")
                || t.contains("obytna auta")
                || t.contains("mikrobus")
                || t.contains("autobus")
                || t.contains("pickup")
                || t.contains("pick-up")
                || t.contains("ram 1500")
                || t.contains("ram 2500")
                || t.contains("ram 3500");
    }

    private boolean looksLikeBadTitle(String title) {
        String t = title.toLowerCase(Locale.ROOT);

        return t.contains("hledám auto")
                || t.contains("hledam auto")
                || t.contains("koupím auto")
                || t.contains("koupim auto")
                || t.contains("pronájem auta")
                || t.contains("pronajem auta")
                || t.contains("pronájem")
                || t.contains("pronajem")
                || t.contains("půjčení")
                || t.contains("pujceni")
                || t.contains("hledám")
                || t.contains("hledam")
                || t.contains("motorku")
                || t.contains("motorka")
                || t.contains("motocykl")
                || t.contains("skutr");
    }

    private <T> boolean updateIfDifferent(ValueSupplier<T> getter,
                                          ValueConsumer<T> setter,
                                          T newValue) {
        T current = getter.get();

        if (!Objects.equals(current, newValue)) {
            setter.accept(newValue);
            return true;
        }

        return false;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    @FunctionalInterface
    private interface ValueSupplier<T> {
        T get();
    }

    @FunctionalInterface
    private interface ValueConsumer<T> {
        void accept(T value);
    }
}

package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ToyotaProvereneVozyParser implements CarSourceParser {

    private static final Logger log = LoggerFactory.getLogger(ToyotaProvereneVozyParser.class);

    private static final String BASE_URL = "https://proverenevozy.toyota.cz";
    private static final int REQUEST_TIMEOUT_MS = 20_000;
    private static final int MAX_LIST_PAGES = 10;
    private static final int PER_PAGE = 24;
    private static final int CURRENT_YEAR = Year.now().getValue();
    private static final int MIN_VALID_PRICE = 30_000;
    private static final int MAX_REASONABLE_PRICE = 10_000_000;

    @Override
    public String getSourceName() {
        return "TOYOTA_PROVERENE";
    }

    @Override
    public List<CarDto> fetchCars() {
        List<CarDto> cars = new ArrayList<>();
        Set<String> seenUrls = new LinkedHashSet<>();

        int missingPriceCount = 0;
        int invalidPriceCount = 0;
        int missingTitleCount = 0;
        int commercialVehicleCount = 0;
        int parseExceptionCount = 0;

        for (int page = 1; page <= MAX_LIST_PAGES; page++) {
            String pageUrl = buildPageUrl(page);

            try {
                Document doc = connect(pageUrl).get();
                List<CarDto> pageCars = parseListPage(doc, seenUrls);

                log.info("TOYOTA_PROVERENE list page={} url={} parsed={} total_unique={}",
                        page, pageUrl, pageCars.size(), seenUrls.size());

                cars.addAll(pageCars);

                if (page > 1 && pageCars.isEmpty()) {
                    log.info("TOYOTA_PROVERENE pagination stopped page={} reason=no_new_cars", page);
                    break;
                }

                sleepQuietly(300);
            } catch (Exception e) {
                parseExceptionCount++;
                log.warn("TOYOTA_PROVERENE list page failed page={} url={} error={}",
                        page, pageUrl, safe(e.getMessage()));
            }
        }

        List<CarDto> validCars = new ArrayList<>();

        for (CarDto car : cars) {
            if (car.getTitle() == null || car.getTitle().isBlank()) {
                missingTitleCount++;
                continue;
            }

            if (car.getPriceValue() == null) {
                missingPriceCount++;
                log.warn("TOYOTA_PROVERENE SKIP url={} reason=missing_price title={}",
                        safe(car.getUrl()), safe(car.getTitle()));
                continue;
            }

            if (car.getPriceValue() < MIN_VALID_PRICE || car.getPriceValue() > MAX_REASONABLE_PRICE) {
                invalidPriceCount++;
                log.warn("TOYOTA_PROVERENE SKIP url={} reason=invalid_price title={} price={}",
                        safe(car.getUrl()), safe(car.getTitle()), car.getPriceValue());
                continue;
            }

            if ("VAN".equals(car.getCarType())) {
                commercialVehicleCount++;
                log.info("TOYOTA_PROVERENE SKIP url={} reason=commercial_vehicle title={}",
                        safe(car.getUrl()), safe(car.getTitle()));
                continue;
            }

            log.info("TOYOTA_PROVERENE CAR title='{}' price={} location={} year={} mileage={} fuelType={} transmission={} carType={} brand={} url={}",
                    safe(car.getTitle()),
                    car.getPriceValue(),
                    safe(car.getLocation()),
                    car.getYear(),
                    car.getMileage(),
                    safe(car.getFuelType()),
                    safe(car.getTransmission()),
                    safe(car.getCarType()),
                    safe(car.getBrand()),
                    safe(car.getUrl()));

            validCars.add(car);
        }

        log.info("TOYOTA_PROVERENE parsed {} cars", validCars.size());
        log.info("TOYOTA_PROVERENE SUMMARY parsed={} missing_title={} missing_price={} invalid_price={} commercial_vehicle={} parse_exception={}",
                validCars.size(), missingTitleCount, missingPriceCount, invalidPriceCount, commercialVehicleCount, parseExceptionCount);

        return validCars;
    }

    private Connection connect(String url) {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .referrer(BASE_URL)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "cs-CZ,cs;q=0.9,en;q=0.8")
                .timeout(REQUEST_TIMEOUT_MS)
                .followRedirects(true);
    }

    private String buildPageUrl(int page) {
        return BASE_URL + "/nabidky?na-strone=" + PER_PAGE + "&strona=" + page;
    }

    private List<CarDto> parseListPage(Document doc, Set<String> seenUrls) {
        List<CarDto> cars = new ArrayList<>();

        for (Element titleLink : findTitleLinks(doc)) {
            String url = cleanUrl(titleLink.absUrl("href"));
            if (url == null || !seenUrls.add(url)) {
                continue;
            }

            try {
                Document detailDoc = connect(url).get();
                CarDto car = parseListItem(titleLink, url, detailDoc);
                if (car != null) {
                    cars.add(car);
                }
                sleepQuietly(120);
            } catch (Exception e) {
                log.warn("TOYOTA_PROVERENE list item failed url={} error={}", safe(url), safe(e.getMessage()));
            }
        }

        return cars;
    }

    private List<Element> findTitleLinks(Document doc) {
        Map<String, Element> bestLinkByUrl = new LinkedHashMap<>();
        Map<String, Integer> bestScoreByUrl = new LinkedHashMap<>();

        for (Element link : doc.select("a[href*=/nabidka/]")) {
            String text = normalizeText(link.text());
            String href = cleanUrl(link.absUrl("href"));

            if (href == null || text.isBlank()) {
                continue;
            }

            int score = titleLinkScore(text);
            Integer previousScore = bestScoreByUrl.get(href);

            if (previousScore == null || score > previousScore) {
                bestScoreByUrl.put(href, score);
                bestLinkByUrl.put(href, link);
            }
        }

        return new ArrayList<>(bestLinkByUrl.values());
    }

    private int titleLinkScore(String text) {
        String normalized = " " + normalizeAscii(text).toLowerCase(Locale.ROOT) + " ";
        int score = Math.min(normalized.length(), 120);

        if (looksLikeLocation(text)) {
            score -= 200;
        }

        if (normalized.matches(".*\\d[\\d\\s]*\\s*kc.*")) {
            score -= 100;
        }

        if (normalized.matches(".*\\b(toyota|lexus|skoda|volkswagen|audi|bmw|mercedes|hyundai|kia|ford|peugeot|renault|opel|mazda|suzuki|citroen|seat|cupra|volvo|honda|nissan)\\b.*")) {
            score += 300;
        }

        return score;
    }

    private CarDto parseListItem(Element titleLink, String url, Document detailDoc) {
        Element container = findListingContainer(titleLink);
        String containerText = normalizeText(container != null ? container.text() : titleLink.parent().text());
        String detailText = normalizeText(detailDoc != null ? detailDoc.text() : "");
        String combinedText = normalizeText(containerText + " " + detailText + " " + extractMetaContent(detailDoc, "meta[property=og:description]"));
        String title = firstNonBlank(extractDetailTitle(detailDoc), extractTitle(titleLink, containerText));

        if (title == null || title.isBlank()) {
            return null;
        }

        Integer priceValue = firstNonNull(
                extractPriceFromTitle(extractMetaContent(detailDoc, "meta[property=og:title]")),
                extractPriceFromTitle(detailDoc != null ? detailDoc.title() : null),
                extractMainPrice(detailText),
                extractMainPrice(containerText)
        );
        Integer year = firstNonNull(validYear(parseIntSafe(extractDetailValue(detailDoc, "productionDate"))), extractYear(combinedText));
        Integer mileage = firstNonNull(parseIntSafe(extractDetailValue(detailDoc, "mileageFromOdometer")), extractMileage(combinedText));
        String fuelType = mapFuel(extractValueAfterLabel(containerText, "Palivo"));
        String transmission = mapTransmission(extractValueAfterLabel(containerText, "Převodovka"));
        String carType = extractCarType(title, containerText);
        String location = extractLocation(titleLink, container);
        String brand = extractBrand(title);
        String imageUrl = extractImageUrl(container);

        fuelType = firstNonBlank(
                mapElectrifiedFuel(title),
                mapFuel(extractDetailValue(detailDoc, "fuelType")),
                mapFuel(extractValueAfterLabel(containerText, "Palivo")),
                mapFuel(title),
                mapFuel(extractMetaContent(detailDoc, "meta[property=og:title]"))
        );
        transmission = firstNonBlank(
                mapTransmission(extractDetailValue(detailDoc, "vehicleTransmission")),
                mapTransmission(extractValueAfterLabel(containerText, "Převodovka")),
                mapTransmission(title),
                isAutomaticHybridTitle(title, fuelType) ? "AUTOMATIC" : null,
                "ELECTRIC".equals(fuelType) ? "AUTOMATIC" : null
        );
        carType = firstNonBlank(
                extractCarType(title, ""),
                mapCarType(extractDetailValue(detailDoc, "bodyType")),
                extractCarType(title, containerText)
        );
        location = firstNonBlank(extractDetailLocation(detailDoc), location);
        imageUrl = firstNonBlank(extractMetaContent(detailDoc, "meta[property=og:image]"), imageUrl);

        CarDto car = new CarDto();
        car.setSource(getSourceName());
        car.setTitle(repairMojibake(title));
        car.setPrice(formatPrice(priceValue));
        car.setPriceValue(priceValue);
        car.setLocation(repairMojibake(location));
        car.setUrl(url);
        car.setImageUrl(imageUrl);
        car.setBrand(brand);
        car.setYear(year);
        car.setMileage(mileage);
        car.setFuelType(fuelType);
        car.setTransmission(transmission);
        car.setCarType(carType);

        return car;
    }

    private String extractDetailTitle(Document doc) {
        if (doc == null) {
            return null;
        }

        Element h1 = doc.selectFirst("h1");
        if (h1 == null) {
            return null;
        }

        String title = normalizeText(h1.text());
        Element subtitle = h1.parent() != null ? h1.parent().selectFirst("strong") : null;
        String subtitleText = subtitle != null ? normalizeText(subtitle.text()) : "";

        if (!subtitleText.isBlank() && !title.contains(subtitleText)) {
            title = normalizeText(title + " " + subtitleText);
        }

        return title.isBlank() ? null : title;
    }

    private String extractDetailValue(Document doc, String itemprop) {
        if (doc == null || itemprop == null || itemprop.isBlank()) {
            return null;
        }

        Element value = doc.selectFirst("[itemprop=" + itemprop + "]");
        if (value == null) {
            return null;
        }

        String text = normalizeText(value.text());
        return text.isBlank() ? null : text;
    }

    private String extractDetailLocation(Document doc) {
        if (doc == null) {
            return null;
        }

        for (Element strong : doc.select(".vdp-dealer strong, section[aria-labelledby=dealerInfo-heading] strong")) {
            String dealer = cleanupDealerLocation(strong.text());
            if (dealer != null) {
                return dealer;
            }
        }

        for (Element strong : doc.select("strong")) {
            String text = cleanupLocation(strong.text());
            if (text == null) {
                continue;
            }

            String lower = normalizeAscii(text).toLowerCase(Locale.ROOT);
            if (lower.startsWith("toyota ") || lower.startsWith("lexus ")) {
                return cleanupLocation(text.replaceFirst("(?i)^(Toyota|Lexus)\\s+", ""));
            }
        }

        String title = extractMetaContent(doc, "meta[property=og:title]");
        Matcher matcher = Pattern.compile("\\d+\\s*K(?:č|c)\\s+s\\s+DPH\\s+(.+?)\\s*\\|", Pattern.CASE_INSENSITIVE).matcher(safe(title));
        if (matcher.find()) {
            String dealer = cleanupLocation(matcher.group(1));
            if (dealer != null) {
                return cleanupLocation(dealer.replaceFirst("(?i)^(Toyota|Lexus)\\s+", ""));
            }
        }

        return null;
    }

    private String mapElectrifiedFuel(String value) {
        if (value == null) return null;
        String v = " " + normalizeAscii(value).toLowerCase(Locale.ROOT) + " ";
        String tokens = " " + v.replaceAll("[^a-z0-9+]+", " ") + " ";
        String compact = v.replaceAll("[^a-z0-9+]", "");

        if (containsAny(tokens, " plug in ", " plugin ", " phev ", " 450h+ ", " 450 h+ ")
                || compact.contains("plugin")
                || compact.contains("pluginhybrid")
                || compact.contains("phev")) {
            return "PLUGIN_HYBRID";
        }
        if (containsAny(tokens, " electric ", " elektro ", " kwh ", " 500e ", " rz ")) {
            return "ELECTRIC";
        }
        if (tokens.contains(" lexus ")
                && containsAny(tokens, " rx ", " nx ")
                && containsAny(tokens, " 2 5 ", " 25 ")) {
            return "HYBRID";
        }
        if (tokens.contains(" volvo ")
                && containsAny(tokens, " b3 ", " b4 ", " b5 ", " b6 ")) {
            return "HYBRID";
        }
        if (containsAny(tokens, " c hr ", " chr ")
                && containsAny(tokens, " 1 8 ", " 2 0 ")) {
            return "HYBRID";
        }
        if (containsAny(tokens, " hybrid ", " hev ", " hsd ", " mhev ", " e cvt ", " ecvt ",
                " 350h ", " 450h ", " 500h ", " 1.5h ", " 1,5h ", " 1.8h ", " 1,8h ",
                " 2.0h ", " 2,0h ", " 2.5h ", " 2,5h ")
                || compact.contains("hybrid")) {
            return "HYBRID";
        }

        return null;
    }

    private String cleanupDealerLocation(String value) {
        String text = cleanupLocation(value);
        if (text == null) {
            return null;
        }

        String normalized = normalizeAscii(text).toLowerCase(Locale.ROOT);
        if (normalized.startsWith("toyota ")) {
            return cleanupLocation(text.replaceFirst("(?i)^Toyota\\s+", ""));
        }
        if (normalized.startsWith("lexus ")) {
            return cleanupLocation(text.replaceFirst("(?i)^Lexus\\s+", ""));
        }

        return text;
    }

    private String extractMetaContent(Document doc, String selector) {
        if (doc == null || selector == null || selector.isBlank()) {
            return null;
        }

        Element meta = doc.selectFirst(selector);
        if (meta == null) {
            return null;
        }

        String value = normalizeText(meta.attr("content"));
        return value.isBlank() ? null : value;
    }

    private Element findListingContainer(Element titleLink) {
        Element current = titleLink;

        for (int i = 0; i < 8 && current != null; i++) {
            String text = normalizeText(current.text());

            if (text.contains("Rok výroby")
                    && text.contains("Palivo")
                    && text.contains("Převodovka")
                    && text.matches(".*\\d[\\d\\s\\u00A0]*\\s*Kč.*")) {
                return current;
            }

            current = current.parent();
        }

        return titleLink.parent();
    }

    private String extractTitle(Element titleLink, String containerText) {
        String linkText = normalizeText(titleLink.text());
        if (!linkText.isBlank() && !looksLikeLocation(linkText)) {
            return linkText;
        }

        Matcher matcher = Pattern.compile(
                "(Toyota|Lexus|Abarth|Alfa Romeo|Aston Martin|Audi|BMW|BYD|Chevrolet|Citro[eë]n|Cupra|Dacia|Dodge|Fiat|Ford|Honda|Hyundai|Jaguar|Jeep|Kia|Land Rover|Mazda|Mercedes-Benz|MG|Mini|Mitsubishi|Nissan|Opel|Peugeot|Porsche|Renault|Seat|Škoda|Skoda|Subaru|Suzuki|Tesla|Volkswagen|Volvo)\\s+(.+?)(?=\\s+DPH\\b|\\s+NOVÝ VŮZ\\b|\\s+\\d[\\d\\s\\u00A0]*\\s*Kč|$)",
                Pattern.CASE_INSENSITIVE
        ).matcher(containerText);

        if (matcher.find()) {
            return normalizeText(matcher.group());
        }

        return null;
    }

    private Integer extractMainPrice(String text) {
        List<Integer> values = new ArrayList<>();

        Matcher compactMatcher = Pattern.compile("(\\d{5,8})\\s*K(?:č|c)", Pattern.CASE_INSENSITIVE).matcher(safe(text));
        while (compactMatcher.find()) {
            if (isMonthlyPrice(text, compactMatcher.end())) {
                continue;
            }
            Integer value = parseIntSafe(compactMatcher.group(1));
            if (value != null && value >= MIN_VALID_PRICE && value <= MAX_REASONABLE_PRICE) {
                values.add(value);
            }
        }

        Matcher matcher = Pattern.compile("(\\d[\\d\\s\\u00A0]{1,20})\\s*K(?:č|c)(?:\\s*s DPH)?", Pattern.CASE_INSENSITIVE).matcher(safe(text));
        while (matcher.find()) {
            if (isMonthlyPrice(text, matcher.end())) {
                continue;
            }
            Integer value = parseIntSafe(matcher.group(1));
            if (value != null && value >= MIN_VALID_PRICE && value <= MAX_REASONABLE_PRICE) {
                values.add(value);
            }
        }

        if (values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }

    private Integer extractPriceFromTitle(String text) {
        Integer best = null;
        Matcher looseMatcher = Pattern.compile("(\\d{5,8}).{0,20}DPH", Pattern.CASE_INSENSITIVE).matcher(safe(text));
        while (looseMatcher.find()) {
            Integer value = parseIntSafe(looseMatcher.group(1));
            if (value != null && value >= MIN_VALID_PRICE && value <= MAX_REASONABLE_PRICE
                    && (best == null || value > best)) {
                best = value;
            }
        }
        if (best != null) {
            return best;
        }

        Matcher matcher = Pattern.compile("(\\d{5,8})\\s*K\\S{0,2}\\s+s\\s+DPH", Pattern.CASE_INSENSITIVE).matcher(safe(text));
        if (matcher.find()) {
            Integer value = parseIntSafe(matcher.group(1));
            if (value != null && value >= MIN_VALID_PRICE && value <= MAX_REASONABLE_PRICE) {
                return value;
            }
        }

        return extractMainPrice(text);
    }

    private boolean isMonthlyPrice(String text, int priceEnd) {
        String safeText = safe(text);
        String tail = safeText.substring(Math.min(priceEnd, safeText.length()));
        tail = normalizeAscii(tail.length() > 32 ? tail.substring(0, 32) : tail).toLowerCase(Locale.ROOT);
        return tail.contains("/mes") || tail.contains("/mÄ›s") || tail.contains("mes.") || tail.contains("mÄ›s.");
    }

    private Integer extractYear(String text) {
        Integer year = parseIntSafe(extractValueAfterLabel(text, "Rok výroby"));
        if (year != null && year >= 1990 && year <= CURRENT_YEAR + 1) {
            return year;
        }

        return null;
    }

    private Integer extractMileage(String text) {
        String value = extractValueAfterLabel(text, "Najeto");
        if (value == null || value.equals("-")) {
            return null;
        }

        Integer mileage = parseIntSafe(value);
        if (mileage != null && mileage >= 0 && mileage <= 1_500_000) {
            return mileage;
        }

        return null;
    }

    private String extractValueAfterLabel(String text, String label) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = Pattern.compile(
                Pattern.quote(label) + "\\s*[:\\-]?\\s*(.*?)(?=\\s+(Rok výroby|Rok první registrace|Najeto|Palivo|Objem|Převodovka|Výkon|Počet dveří|Počet sedadel)\\b|$)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);

        if (matcher.find()) {
            String value = normalizeText(matcher.group(1));
            return value.isBlank() ? null : value;
        }

        return null;
    }

    private String extractLocation(Element titleLink, Element container) {
        String href = cleanUrl(titleLink.absUrl("href"));

        if (container != null && href != null) {
            for (Element link : container.select("a[href]")) {
                String linkHref = cleanUrl(link.absUrl("href"));
                String text = normalizeText(link.text());

                if (href.equals(linkHref) && looksLikeLocation(text)) {
                    return cleanupLocation(text);
                }
            }
        }

        return null;
    }

    private String extractImageUrl(Element container) {
        if (container == null) {
            return null;
        }

        Element img = container.selectFirst("img[src], img[data-src], source[srcset]");
        if (img == null) {
            return null;
        }

        String value = firstNonBlank(
                img.hasAttr("src") ? img.absUrl("src") : null,
                img.hasAttr("data-src") ? img.absUrl("data-src") : null,
                firstSrcSetUrl(img.absUrl("srcset"))
        );

        return value == null || value.isBlank() ? null : value;
    }

    private String firstSrcSetUrl(String srcset) {
        if (srcset == null || srcset.isBlank()) {
            return null;
        }

        return srcset.split(",")[0].trim().replaceAll("\\s+.*$", "");
    }

    private String mapFuel(String value) {
        if (value == null) return null;
        String v = normalizeAscii(value).toLowerCase(Locale.ROOT);

        if (v.contains("plug-in") || v.contains("plugin")) return "PLUGIN_HYBRID";
        if (v.contains("hybrid") || v.contains(" hsd") || v.contains(" hev") || v.contains(" mhev") || v.contains(" ima")) return "HYBRID";
        if (v.contains("elektro") || v.contains("electric") || v.contains("vodik")) return "ELECTRIC";
        if (v.contains("diesel") || v.contains("nafta") || v.contains(" d-4d") || v.contains(" d4d")) return "DIESEL";
        if (v.contains("lpg")) return "LPG";
        if (v.contains("cng")) return "CNG";
        if (v.contains("benzin") || v.contains("puretech") || v.contains("vvt-i")
                || v.contains("valvematic") || v.contains("boosterjet") || v.contains("t-gdi")
                || v.contains(" tgdi") || v.contains("turbo")) return "PETROL";

        return null;
    }

    private String mapCarType(String value) {
        if (value == null) return null;
        String v = normalizeAscii(value).toLowerCase(Locale.ROOT);

        if (v.contains("suv")) return "SUV";
        if (v.contains("kombi") || v.contains("wagon") || v.contains("combi")) return "WAGON";
        if (v.contains("sedan")) return "SEDAN";
        if (v.contains("hatchback")) return "HATCHBACK";
        if (v.contains("mpv") || v.contains("minivan")) return "MINIVAN";
        if (v.contains("van")) return "VAN";
        if (v.contains("pickup") || v.contains("pick-up")) return "PICKUP";
        if (v.contains("coupe")) return "COUPE";
        if (v.contains("cabrio")) return "CABRIO";

        return null;
    }

    private String mapTransmission(String value) {
        if (value == null) return null;
        String v = normalizeAscii(value).toLowerCase(Locale.ROOT);

        if (v.contains("automat")
                || v.contains("e-cvt")
                || v.contains("ecvt")
                || v.contains("dct")
                || v.contains("dsg")
                || v.contains("e-dcs")
                || v.contains("edcs")) return "AUTOMATIC";
        if (v.contains("manual")) return "MANUAL";

        return null;
    }

    private boolean isAutomaticHybridTitle(String title, String fuelType) {
        if (!"HYBRID".equals(fuelType) && !"PLUGIN_HYBRID".equals(fuelType)) {
            return false;
        }
        String source = " " + normalizeAscii(safe(title)).toLowerCase(Locale.ROOT) + " ";
        return containsAny(source, " hev ", " hybrid ", " e-cvt ", " ecvt ", " dct ", " dsg ", " e-dcs ", " edcs ");
    }

    private String extractCarType(String title, String text) {
        String source = " " + normalizeAscii(safe(title) + " " + safe(text)).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(source, " corolla sedan ", " corolla sd ")) {
            return "SEDAN";
        }
        if (containsAny(source, " kombi ", " combi ", " wagon ", " variant ", " sports tourer ", " sw ")) {
            return "WAGON";
        }
        if (source.contains(" corolla ")
                && source.contains(" executive ")
                && !containsAny(source, " corolla sedan ", " corolla sd ", " corolla cross ", " corolla ts ", " ts ", " touring ", " kombi ", " combi ", " wagon ")) {
            return "HATCHBACK";
        }
        if (containsAny(source, " hilux ", " pick-up ", " pickup ", " doublecab ", " double cab ")) {
            return "PICKUP";
        }
        if (containsAny(source, " yaris cross ", " aygo x ", " corolla cross ", " rav4 ", " c-hr ", " chr ", " bz4x ",
                " highlander ", " lexus rx ", " lexus nx ", " lexus lbx ", " lbx ", " bj30 ",
                " sportage ", " touareg ", " vitara ", " ignis ", " cr-v ", " crv ", " ecosport ", " 2008 ", " 3008 ")) {
            return "SUV";
        }
        if (containsAny(source, " proace verso ", " proace city verso ", " touran ", " roomster ",
                " berlingo ", " c3 picasso ", " c4 picasso ", " verso ", " b-max ", " b max ", " venga ")) {
            return "MINIVAN";
        }
        if (containsAny(source, " proace max ", " proace city ", " proace ", " transit courier ", " movano ", " boxer ", " jumper ", " ducato ", " uzitkove ")) {
            return "VAN";
        }
        if ((source.contains(" corolla ") && containsAny(source, " ts ", " touring "))
                || (source.contains(" auris ") && containsAny(source, " ts ", " touring "))
                || containsAny(source, " corolla touring ", " corolla ts ", " comfort ts ", " auris ts ", " auris touring ",
                " sports tourer ", " passat ", " octavia combi ")) {
            return "WAGON";
        }
        if (containsAny(source, " cyberster ", " roadster ", " cabrio ", " kabrio ", " convertible ")) {
            return "CABRIO";
        }
        if (containsAny(source, " lexus lc ", " lc 500 ", " coupe ", " sportovni ", " supra ", " gt86 ")) {
            return "COUPE";
        }
        if (containsAny(source, " corolla sd ", " sedan ", " liftback ", " toledo ", " insignia ", " stinger ", " octavia ", " avensis ")) {
            return "SEDAN";
        }
        if (containsAny(source, " yaris ", " aygo ", " fabia ", " ceed ", " mg3 ", " ds 4 ", " auris ", " focus ", " insight ", " 307 ")) {
            return "HATCHBACK";
        }
        if (containsAny(source, " suv ", " crossover ", " rav4 ", " c-hr ", " chr ", " bz4x ", " kuga ", " tiguan ", " kodiaq ", " karoq ", " kamiq ")) {
            return "SUV";
        }
        if (containsAny(source, " kombi ", " touring ", " variant ", " sports tourer ", " sw ")) {
            return "WAGON";
        }
        if (containsAny(source, " sedan ", " liftback ", " corolla sd ")) {
            return "SEDAN";
        }
        if (containsAny(source, " hatchback ", " yaris ", " aygo ", " ix20 ")) {
            return "HATCHBACK";
        }
        if (containsAny(source, " mpv ", " verso ", " proace city verso ")) {
            return "MINIVAN";
        }
        if (containsAny(source, " proace ", " boxer ", " uzitkove ")) {
            return "VAN";
        }
        if (containsAny(source, " coupe ", " sportovni ", " supra ", " gt86 ", " lexus lc ", " lc 500 ")) {
            return "COUPE";
        }

        return null;
    }

    private String extractBrand(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }

        String[] words = normalizeText(title).split("\\s+");
        if (words.length == 0) {
            return null;
        }

        String firstTwo = words.length >= 2 ? (words[0] + " " + words[1]).toUpperCase(Locale.ROOT) : "";
        if (firstTwo.startsWith("LAND ROVER")) return "LAND_ROVER";
        if (firstTwo.startsWith("ALFA ROMEO")) return "ALFA_ROMEO";
        if (firstTwo.startsWith("MERCEDES-BENZ") || firstTwo.startsWith("MERCEDES BENZ")) return "MERCEDES";

        return normalizeBrand(words[0]);
    }

    private String normalizeBrand(String raw) {
        if (raw == null) {
            return null;
        }

        String value = normalizeText(raw).toUpperCase(Locale.ROOT);

        if (value.startsWith("ŠKODA") || value.startsWith("SKODA")) return "SKODA";
        if (value.startsWith("VOLKSWAGEN")) return "VOLKSWAGEN";
        if (value.startsWith("MERCEDES")) return "MERCEDES";
        if (value.startsWith("BMW")) return "BMW";
        if (value.startsWith("AUDI")) return "AUDI";
        if (value.startsWith("FORD")) return "FORD";
        if (value.startsWith("TOYOTA")) return "TOYOTA";
        if (value.startsWith("LEXUS")) return "LEXUS";
        if (value.startsWith("RENAULT")) return "RENAULT";
        if (value.startsWith("PEUGEOT")) return "PEUGEOT";
        if (value.startsWith("OPEL")) return "OPEL";
        if (value.startsWith("HYUNDAI")) return "HYUNDAI";
        if (value.startsWith("KIA")) return "KIA";
        if (value.startsWith("FIAT")) return "FIAT";
        if (value.startsWith("CUPRA")) return "CUPRA";
        if (value.startsWith("DODGE")) return "DODGE";
        if (value.startsWith("VOLVO")) return "VOLVO";
        if (value.startsWith("SEAT")) return "SEAT";
        if (value.startsWith("HONDA")) return "HONDA";
        if (value.startsWith("NISSAN")) return "NISSAN";
        if (value.startsWith("MAZDA")) return "MAZDA";
        if (value.startsWith("SUZUKI")) return "SUZUKI";
        if (value.startsWith("DACIA")) return "DACIA";
        if (value.startsWith("TESLA")) return "TESLA";
        if (value.startsWith("PORSCHE")) return "PORSCHE";
        if (value.startsWith("JEEP")) return "JEEP";
        if (value.startsWith("SUBARU")) return "SUBARU";
        if (value.startsWith("MITSUBISHI")) return "MITSUBISHI";
        if (value.startsWith("CITROËN") || value.startsWith("CITROEN")) return "CITROEN";
        if (value.startsWith("MINI")) return "MINI";
        if (value.startsWith("MG")) return "MG";
        if (value.startsWith("BYD")) return "BYD";

        return value;
    }

    private boolean looksLikeLocation(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = normalizeText(value);
        String lower = normalizeAscii(normalized).toLowerCase(Locale.ROOT);

        if (normalized.length() > 80 || lower.matches(".*\\d{3}\\s?\\d{2}.*")) {
            return false;
        }

        return containsAny(lower,
                "praha", "brno", "ostrava", "hradec kralove", "plzen", "usti nad labem",
                "olomouc", "pardubice", "pribram", "opava", "uherske hradiste", "svinov",
                "hrabova", "vysocany", "dolni herspice", "liberec", "zlin", "jihlava");
    }

    private String cleanupLocation(String value) {
        String normalized = normalizeText(value);
        if (normalized.isBlank()) {
            return null;
        }

        return normalized.replaceAll("\\s+", " ").trim();
    }

    private String cleanUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String clean = url.replaceAll("[?#].*$", "");
        if (!clean.startsWith(BASE_URL + "/nabidka/")) {
            return null;
        }

        return clean;
    }

    private Integer parseIntSafe(String raw) {
        if (raw == null) {
            return null;
        }

        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer validYear(Integer year) {
        if (year != null && year >= 1990 && year <= CURRENT_YEAR + 1) {
            return year;
        }

        return null;
    }

    private String formatPrice(Integer priceValue) {
        if (priceValue == null) {
            return null;
        }
        return String.format(Locale.US, "%,d Kč", priceValue).replace(",", " ");
    }

    private boolean containsAny(String source, String... values) {
        if (source == null || source.isBlank()) {
            return false;
        }

        String lowerSource = source.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value != null && !value.isBlank() && lowerSource.contains(value.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    private String normalizeAscii(String value) {
        return normalizeText(value)
                .replace('á', 'a')
                .replace('č', 'c')
                .replace('ď', 'd')
                .replace('é', 'e')
                .replace('ě', 'e')
                .replace('í', 'i')
                .replace('ň', 'n')
                .replace('ó', 'o')
                .replace('ř', 'r')
                .replace('š', 's')
                .replace('ť', 't')
                .replace('ú', 'u')
                .replace('ů', 'u')
                .replace('ý', 'y')
                .replace('ž', 'z')
                .replace('Á', 'A')
                .replace('Č', 'C')
                .replace('Ď', 'D')
                .replace('É', 'E')
                .replace('Ě', 'E')
                .replace('Í', 'I')
                .replace('Ň', 'N')
                .replace('Ó', 'O')
                .replace('Ř', 'R')
                .replace('Š', 'S')
                .replace('Ť', 'T')
                .replace('Ú', 'U')
                .replace('Ů', 'U')
                .replace('Ý', 'Y')
                .replace('Ž', 'Z');
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String repairMojibake(String value) {
        if (value == null || value.isBlank() || mojibakeScore(value) == 0) {
            return value;
        }

        try {
            byte[] bytes = encodeMojibakeBytes(value);
            String repaired = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes))
                    .toString();

            return mojibakeScore(repaired) < mojibakeScore(value) ? normalizeText(repaired) : value;
        } catch (Exception e) {
            return value;
        }
    }

    private byte[] encodeMojibakeBytes(String value) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(value.length());
        Charset windows1250 = Charset.forName("windows-1250");

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch <= 0xFF) {
                out.write((byte) ch);
                continue;
            }

            byte[] bytes = windows1250
                    .newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(java.nio.CharBuffer.wrap(String.valueOf(ch)))
                    .array();
            out.write(bytes[0]);
        }

        return out.toByteArray();
    }

    private int mojibakeScore(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        int broadScore = 0;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            if (codePoint == '\u0102'
                    || codePoint == '\u00C4'
                    || codePoint == '\u0139'
                    || codePoint == '\u00C2'
                    || codePoint == '\u00E2'
                    || codePoint == '\uFFFD') {
                broadScore++;
            }
            offset += Character.charCount(codePoint);
        }
        if (broadScore > 0) {
            return broadScore;
        }

        int score = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == 'Ă' || ch == 'Ä' || ch == 'Ĺ' || ch == 'Â' || ch == 'â') {
                score++;
            }
        }
        return score;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }

        for (T value : values) {
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

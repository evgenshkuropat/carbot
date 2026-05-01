package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TipCarsParser implements CarSourceParser {

    private static final Logger log = LoggerFactory.getLogger(TipCarsParser.class);

    private static final String BASE_LIST_URL = "https://www.tipcars.com/osobni/";
    private static final String BASE_URL = "https://www.tipcars.com/";
    private static final int MAX_LIST_PAGES = 5;
    private static final int REQUEST_TIMEOUT_MS = 15_000;

    private static final int CURRENT_YEAR = Year.now().getValue();
    private static final int MIN_YEAR = 1990;
    private static final int MAX_REASONABLE_PRICE = 10_000_000;

    @Override
    public String getSourceName() {
        return "TIPCARS";
    }

    @Override
    public List<CarDto> fetchCars() {
        List<CarDto> cars = new ArrayList<>();
        Set<String> detailLinks = new LinkedHashSet<>();

        int missingPriceCount = 0;
        int invalidPriceCount = 0;
        int brokenCount = 0;
        int parseExceptionCount = 0;

        try {
            for (int page = 1; page <= MAX_LIST_PAGES; page++) {
                String pageUrl = buildPageUrl(page);

                try {
                    int before = detailLinks.size();

                    Document listDoc = connect(pageUrl).get();
                    Set<String> pageLinks = extractDetailLinks(listDoc);
                    detailLinks.addAll(pageLinks);

                    int addedOnPage = detailLinks.size() - before;

                    log.info("TIPCARS list page={} url={} detail_links_found={} new_links={} total_unique_links={}",
                            page, pageUrl, pageLinks.size(), addedOnPage, detailLinks.size());

                    if (page > 1 && addedOnPage == 0) {
                        log.info("TIPCARS pagination stopped page={} reason=no_new_links", page);
                        break;
                    }

                    sleepQuietly(300);
                } catch (Exception e) {
                    log.warn("TIPCARS list page failed page={} url={} error={}",
                            page, pageUrl, safe(e.getMessage()));
                }
            }

            log.info("TIPCARS total unique detail links found={}", detailLinks.size());

            for (String url : detailLinks) {
                ParseResult result = parseDetail(url);

                if (result.car() != null) {
                    cars.add(result.car());
                } else {
                    switch (result.reason()) {
                        case "missing_price" -> missingPriceCount++;
                        case "invalid_price" -> invalidPriceCount++;
                        case "broken_listing" -> brokenCount++;
                        case "parse_exception" -> parseExceptionCount++;
                        default -> brokenCount++;
                    }
                }

                sleepQuietly(150);
            }

        } catch (Exception e) {
            log.warn("TIPCARS fetch failed error={}", safe(e.getMessage()));
        }

        log.info("TIPCARS parsed {} cars", cars.size());
        log.info("TIPCARS SUMMARY parsed={} broken_listing={} missing_price={} invalid_price={} parse_exception={}",
                cars.size(), brokenCount, missingPriceCount, invalidPriceCount, parseExceptionCount);

        return cars;
    }

    private ParseResult parseDetail(String url) {
        try {
            Document doc = connect(url).get();

            String title = extractTitle(doc);
            String pageText = normalizeText(doc.text());

            if (title == null || title.isBlank()
                    || isJunkTitle(title)
                    || isJunkUrl(url)
                    || isJunkText(pageText)) {
                log.warn("TIPCARS SKIP url={} reason=broken_listing title={}", safe(url), safe(title));
                return new ParseResult(null, "broken_listing");
            }

            Integer priceValue = extractPriceValue(doc, pageText);
            if (priceValue == null) {
                log.warn("TIPCARS SKIP url={} reason=missing_price title={}", safe(url), safe(title));
                return new ParseResult(null, "missing_price");
            }

            if (priceValue <= 0 || priceValue > MAX_REASONABLE_PRICE) {
                log.warn("TIPCARS SKIP url={} reason=invalid_price title={} price={}",
                        safe(url), safe(title), priceValue);
                return new ParseResult(null, "invalid_price");
            }

            Integer year = extractYear(pageText, title);
            Integer mileage = extractMileage(pageText);
            String location = extractLocation(doc, pageText);
            String imageUrl = extractImageUrl(doc);
            String brand = extractBrand(title, url);
            String combinedText = title + " " + url;
            String fuelType = extractFuelType(combinedText);
            String transmission = extractTransmission(combinedText);
            String carType = extractCarType(title, title, url);

            CarDto car = new CarDto();
            car.setSource("TIPCARS");
            car.setTitle(title);
            car.setPrice(formatPrice(priceValue));
            car.setPriceValue(priceValue);
            car.setLocation(location);
            car.setUrl(url);
            car.setImageUrl(imageUrl);
            car.setBrand(brand);
            car.setYear(year);
            car.setMileage(mileage);
            car.setFuelType(fuelType);
            car.setTransmission(transmission);
            car.setCarType(carType);

            log.info("TIPCARS CAR title='{}' price={} location={} year={} mileage={} fuelType={} transmission={} carType={} brand={} url={}",
                    safe(title),
                    priceValue,
                    safe(location),
                    year,
                    mileage,
                    safe(fuelType),
                    safe(transmission),
                    safe(carType),
                    safe(brand),
                    safe(url));

            return new ParseResult(car, "ok");

        } catch (Exception e) {
            log.warn("TIPCARS SKIP url={} reason=parse_exception error={}", safe(url), safe(e.getMessage()));
            return new ParseResult(null, "parse_exception");
        }
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
        if (page <= 1) {
            return BASE_LIST_URL;
        }
        return BASE_LIST_URL + "?page=" + page;
    }

    private Set<String> extractDetailLinks(Document listDoc) {
        Set<String> links = new LinkedHashSet<>();

        for (Element a : listDoc.select("a[href]")) {
            String href = a.absUrl("href");

            if (!isValidDetailLink(href)) {
                continue;
            }

            href = href.replaceAll("[?#].*$", "");
            links.add(href);
        }

        return links;
    }

    private boolean isValidDetailLink(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        String normalized = url.toLowerCase(Locale.ROOT).replaceAll("[?#].*$", "");

        if (!normalized.startsWith("https://www.tipcars.com/")) {
            return false;
        }

        if (normalized.contains("/hledam/")
                || normalized.contains("/magazin/")
                || normalized.contains("/forum/")
                || normalized.contains("/muj-tipcars/")
                || normalized.contains("/poradna/")
                || normalized.contains("/recenze/")
                || normalized.contains("/testy/")
                || normalized.contains("/aktuality/")
                || normalized.contains("/temata/")
                || normalized.contains("/tiskove-zpravy/")
                || normalized.contains("/operativni-leasing")
                || normalized.contains("/pronajem")
                || normalized.endsWith("/osobni/")
                || normalized.endsWith("/osobni")) {
            return false;
        }

        return normalized.matches("https://www\\.tipcars\\.com/.+-\\d+\\.html");
    }

    private String extractTitle(Document doc) {
        Element og = doc.selectFirst("meta[property=og:title]");
        if (og != null) {
            String value = normalizeText(og.attr("content"));
            if (!value.isBlank()) {
                return cleanupTitle(value);
            }
        }

        Element h1 = doc.selectFirst("h1");
        if (h1 != null) {
            String value = normalizeText(h1.text());
            if (!value.isBlank()) {
                return cleanupTitle(value);
            }
        }

        return cleanupTitle(normalizeText(doc.title()));
    }

    private String cleanupTitle(String title) {
        if (title == null) {
            return null;
        }

        return normalizeText(
                title.replace("| TipCars", "")
                        .replace("- TipCars", "")
                        .trim()
        );
    }

    private Integer extractPriceValue(Document doc, String pageText) {
        List<String> candidates = new ArrayList<>();

        for (Element meta : doc.select("meta[property=product:price:amount], meta[itemprop=price]")) {
            String content = normalizeText(meta.attr("content"));
            if (!content.isBlank()) {
                candidates.add(content);
            }
        }

        for (Element el : doc.select("[class*=price], [id*=price], [data-testid*=price], [data-test*=price]")) {
            String text = normalizeText(el.text());
            if (!text.isBlank()) {
                candidates.add(text);
            }
        }

        candidates.add(pageText);

        for (String raw : candidates) {
            Integer value = extractFirstPrice(raw);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private Integer extractFirstPrice(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = Pattern.compile("(\\d[\\d\\s\\u00A0]{1,20})\\s*Kč", Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) {
            Integer parsed = parseIntSafe(matcher.group(1));
            if (parsed != null && parsed > 0) {
                return parsed;
            }
        }

        return null;
    }

    private Integer extractYear(String text, String title) {
        String combined = normalizeText(safe(title) + " " + safe(text));

        Matcher labeled = Pattern.compile(
                "(?i)(rok výroby|rok vyroby|r\\.v\\.?|první registrace|prvni registrace|uvedení do provozu|uvedeni do provozu)\\s*[:\\- ]\\s*(19\\d{2}|20\\d{2})"
        ).matcher(combined);

        if (labeled.find()) {
            Integer year = parseIntSafe(labeled.group(2));
            if (isValidYear(year)) {
                return year;
            }
        }

        Matcher generic = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(combined);
        while (generic.find()) {
            Integer year = parseIntSafe(generic.group(1));
            if (isValidYear(year)) {
                return year;
            }
        }

        return null;
    }

    private Integer extractMileage(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = Pattern.compile(
                "(?i)(najeto|tachometr|stav tachometru)?\\s*[:\\- ]*([0-9]{1,3}(?:[ \\u00A0][0-9]{3})+|[0-9]{4,7})\\s*km"
        ).matcher(text);

        while (matcher.find()) {
            Integer mileage = parseIntSafe(matcher.group(2));
            if (mileage != null && mileage >= 1000 && mileage <= 1_500_000) {
                return mileage;
            }
        }

        return null;
    }

    private String extractLocation(Document doc, String pageText) {
        for (Element el : doc.select("[class*=locality], [class*=location], [class*=mesto], [class*=city], [data-testid*=location]")) {
            String text = normalizeText(el.text());
            if (isMeaningfulLocation(text)) {
                return cleanupLocation(text);
            }
        }

        Matcher cityMatcher = Pattern.compile("(?i)(praha|brno|ostrava|plzeň|plzen|liberec|olomouc|pardubice|hradec králové|hradec kralove|české budějovice|ceske budejovice|ústí nad labem|usti nad labem|zlin|zlín|jihlava|karlovy vary|opava|kladno|mladá boleslav|mlada boleslav|teplice|most|cheb|trutnov|kolín|kolin|karviná|karvina|blansko)")
                .matcher(normalizeText(pageText));

        if (cityMatcher.find()) {
            return cleanupLocation(cityMatcher.group(1));
        }

        return null;
    }

    private boolean isMeaningfulLocation(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String lower = normalizeText(value).toLowerCase(Locale.ROOT);

        if (lower.length() > 80) {
            return false;
        }

        return !containsAny(lower,
                "reklama",
                "tipcars",
                "magazín",
                "magazin",
                "diskuze",
                "forum",
                "hledam",
                "osobní vozy",
                "osobni vozy",
                "motorky",
                "užitkové",
                "uzitkove",
                "kč",
                "leasing");
    }

    private String cleanupLocation(String value) {
        String normalized = normalizeText(value);
        if (normalized.isBlank()) {
            return null;
        }

        normalized = normalized.replaceAll("(?i)reklama.*$", "").trim();
        normalized = normalized.replaceAll("[,;\\-]+$", "").trim();

        if (normalized.isBlank()) {
            return null;
        }

        return capitalizeWords(normalized);
    }

    private String extractImageUrl(Document doc) {
        Element og = doc.selectFirst("meta[property=og:image]");
        if (og != null) {
            String value = normalizeText(og.attr("content"));
            if (!value.isBlank()) {
                return value;
            }
        }

        Element img = doc.selectFirst("img[src]");
        if (img != null) {
            String value = img.absUrl("src");
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    private String extractBrand(String title, String url) {
        String source = normalizeText(title);

        if (source.isBlank() && url != null) {
            String normalizedUrl = url.toLowerCase(Locale.ROOT);
            Matcher matcher = Pattern.compile("tipcars\\.com/([^/]+)/").matcher(normalizedUrl);
            if (matcher.find()) {
                source = matcher.group(1);
            }
        }

        if (source == null || source.isBlank()) {
            return null;
        }

        String[] words = source.split("\\s+");
        if (words.length == 0) {
            return null;
        }

        if (words.length >= 2) {
            String firstTwo = (words[0] + " " + words[1]).toUpperCase(Locale.ROOT);
            if (firstTwo.startsWith("LAND ROVER")) return "LAND_ROVER";
            if (firstTwo.startsWith("ALFA ROMEO")) return "ALFA_ROMEO";
            if (firstTwo.startsWith("MERCEDES BENZ")) return "MERCEDES";
            if (firstTwo.startsWith("MERCEDES-BENZ")) return "MERCEDES";
        }

        return normalizeBrand(words[0]);
    }

    private String extractFuelType(String text) {
        String normalized = " " + normalizeText(text).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(normalized,
                " diesel ", " nafta ", " tdi ", " hdi ", " dci ", " cdi ",
                " crdi ", " tdci ", " jtd ", " multijet ", " bluehdi ",
                " d4 ", " d3 ")) {
            return "DIESEL";
        }

        if (containsAny(normalized,
                " benzín ", " benzin ", " petrol ", " tsi ", " tfsi ",
                " mpi ", " fsi ", " tce ", " ecoboost ", " vvt-i ",
                " t4 ", " t5 ", " b3 ", " b4 ", " b5 ")) {
            return "PETROL";
        }

        if (containsAny(normalized,
                " plug-in hybrid ", " plug in hybrid ", " plug-in ",
                " phev ", " recharge ")) {
            return "PLUGIN_HYBRID";
        }

        if (containsAny(normalized,
                " hybrid ", " hybridní ", " hybridni ",
                " hev ", " mhev ", " e:hev ", " ehev ")) {
            return "HYBRID";
        }

        if (containsAny(normalized, " lpg ")) {
            return "LPG";
        }

        if (containsAny(normalized, " cng ")) {
            return "CNG";
        }

        if (containsAny(normalized,
                " elektro ", " elektrické ", " elektricke ",
                " electric ", " kwh ", " battery ", " bev ")) {
            return "ELECTRIC";
        }

        return null;
    }

    private String extractTransmission(String text) {
        String normalized = " " + normalizeText(text).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(normalized,
                " automat ",
                " automatická ",
                " automaticka ",
                " automatic ",
                " aut ",
                " aut. ",
                " at ",
                " at ",
                " a/t ",
                " dsg ",
                " tiptronic ",
                " cvt ",
                " e-cvt ",
                " ecvt ",
                " s tronic ",
                " stronic ",
                " powershift ",
                " edc ")) {
            return "AUTOMATIC";
        }

        if (containsAny(normalized,
                " manuál ",
                " manual ",
                " manuální ",
                " manualni ",
                " man ",
                " man. ",
                " 5mt ",
                " 6mt ")) {
            return "MANUAL";
        }

        return null;
    }

    private String extractCarType(String title, String text, String url) {
        String source = " " + normalizeText(safe(title) + " " + safe(text) + " " + safe(url)).toLowerCase(Locale.ROOT) + " ";
        String normalizedUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);

        if (normalizedUrl.contains("/suv/")) {
            return "SUV";
        }

        if (normalizedUrl.contains("/kombi/")) {
            return "WAGON";
        }

        if (normalizedUrl.contains("/pick-up/") || normalizedUrl.contains("/pickup/")) {
            return "PICKUP";
        }

        if (normalizedUrl.contains("/hatchback/")) {
            return "HATCHBACK";
        }

        if (normalizedUrl.contains("/sedan/")) {
            return "SEDAN";
        }

        if (normalizedUrl.contains("/kabriolet/") || normalizedUrl.contains("/cabrio/")) {
            return "CABRIO";
        }

        if (normalizedUrl.contains("/coupe/") || normalizedUrl.contains("/kupe/")) {
            return "COUPE";
        }

        if (containsAny(source,
                " formentor ",
                " compass ",
                " suv ",
                " crossover ",
                " karoq ",
                " kamiq ",
                " kodiaq ",
                " tiguan ",
                " touareg ",
                " q3 ",
                " q5 ",
                " q7 ",
                " q8 ",
                " x1 ",
                " x3 ",
                " x5 ",
                " xc40 ",
                " xc60 ",
                " xc90 ",
                " sportage ",
                " rav4 ",
                " cr-v ",
                " cx-3 ",
                " cx-5 ")) {
            return "SUV";
        }

        if (containsAny(source,
                " shooting brake ",
                " kombi ",
                " combi ",
                " wagon ",
                " variant ",
                " touring ",
                " avant ",
                " estate ")) {
            return "WAGON";
        }

        if (containsAny(source,
                " pickup ",
                " pick-up ",
                " ram 1500 ",
                " ranger ",
                " hilux ",
                " amarok ",
                " navara ")) {
            return "PICKUP";
        }

        if (containsAny(source,
                " hatchback ",
                " fabia ",
                " golf ",
                " ibiza ",
                " 595 ")) {
            return "HATCHBACK";
        }

        if (containsAny(source,
                " sedan ",
                " octavia ",
                " superb ",
                " passat ",
                " arteon ")) {
            return "SEDAN";
        }

        return null;
    }

    private boolean isJunkTitle(String title) {
        String t = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        return t.isBlank()
                || containsAny(t,
                " osobní vozy na prodej ",
                " osobni vozy na prodej ",
                " osobní vozy, výběr dle paliva na prodej ",
                " osobni vozy, vyber dle paliva na prodej ",
                " magazín o autech tipcars ",
                " magazin o autech tipcars ",
                " nejnovější auto/moto diskuze ",
                " nejnovejsi auto/moto diskuze ",
                " motorky na prodej ",
                " užitkové vozy na prodej ",
                " uzitkove vozy na prodej ");
    }

    private boolean isJunkUrl(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }

        String u = url.toLowerCase(Locale.ROOT);
        return containsAny(u,
                "/hledam/",
                "/magazin/",
                "/forum/",
                "/muj-tipcars/",
                "/temata/",
                "/aktuality/",
                "/testy/",
                "/recenze/",
                "/poradna/",
                "/tiskove-zpravy/");
    }

    private boolean isJunkText(String text) {
        String normalized = " " + normalizeText(text).toLowerCase(Locale.ROOT) + " ";
        return containsAny(normalized,
                " magazín o autech tipcars ",
                " magazin o autech tipcars ",
                " nejnovější auto/moto diskuze ",
                " nejnovejsi auto/moto diskuze ");
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

    private boolean isValidYear(Integer year) {
        return year != null && year >= MIN_YEAR && year <= CURRENT_YEAR + 1;
    }

    private String formatPrice(Integer priceValue) {
        if (priceValue == null) {
            return null;
        }
        return String.format(Locale.US, "%,d Kč", priceValue).replace(",", " ");
    }

    private String normalizeBrand(String raw) {
        if (raw == null) {
            return null;
        }

        String value = normalizeText(raw).toUpperCase(Locale.ROOT);

        if (value.startsWith("ŠKODA")) return "SKODA";
        if (value.startsWith("SKODA")) return "SKODA";
        if (value.startsWith("VOLKSWAGEN")) return "VOLKSWAGEN";
        if (value.startsWith("MERCEDES")) return "MERCEDES";
        if (value.startsWith("BMW")) return "BMW";
        if (value.startsWith("AUDI")) return "AUDI";
        if (value.startsWith("FORD")) return "FORD";
        if (value.startsWith("TOYOTA")) return "TOYOTA";
        if (value.startsWith("RENAULT")) return "RENAULT";
        if (value.startsWith("PEUGEOT")) return "PEUGEOT";
        if (value.startsWith("OPEL")) return "OPEL";
        if (value.startsWith("HYUNDAI")) return "HYUNDAI";
        if (value.startsWith("KIA")) return "KIA";
        if (value.startsWith("LEXUS")) return "LEXUS";
        if (value.startsWith("FIAT")) return "FIAT";
        if (value.startsWith("CUPRA")) return "CUPRA";
        if (value.startsWith("DODGE")) return "DODGE";
        if (value.startsWith("VOLVO")) return "VOLVO";
        if (value.startsWith("SEAT")) return "SEAT";
        if (value.startsWith("LAND")) return "LAND_ROVER";
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
        if (value.startsWith("CITROËN")) return "CITROEN";
        if (value.startsWith("CITROEN")) return "CITROEN";
        if (value.startsWith("ALFA")) return "ALFA_ROMEO";
        if (value.startsWith("CHEVROLET")) return "CHEVROLET";

        return value;
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

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String capitalizeWords(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String[] parts = value.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(' ');
            }

            sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }

        return sb.toString();
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

    private record ParseResult(CarDto car, String reason) {
    }
}
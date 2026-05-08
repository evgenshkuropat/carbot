package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BazosParser extends AbstractJsoupParser implements CarSourceParser {

    private static final Logger log = LoggerFactory.getLogger(BazosParser.class);

    private static final List<String> LIST_PAGE_BASE_URLS = List.of(
            "https://auto.bazos.cz/alfa/",
            "https://auto.bazos.cz/audi/",
            "https://auto.bazos.cz/bmw/",
            "https://auto.bazos.cz/citroen/",
            "https://auto.bazos.cz/dacia/",
            "https://auto.bazos.cz/fiat/",
            "https://auto.bazos.cz/ford/",
            "https://auto.bazos.cz/honda/",
            "https://auto.bazos.cz/hyundai/",
            "https://auto.bazos.cz/chevrolet/",
            "https://auto.bazos.cz/kia/",
            "https://auto.bazos.cz/mazda/",
            "https://auto.bazos.cz/mercedes/",
            "https://auto.bazos.cz/mitsubishi/",
            "https://auto.bazos.cz/nissan/",
            "https://auto.bazos.cz/opel/",
            "https://auto.bazos.cz/peugeot/",
            "https://auto.bazos.cz/renault/",
            "https://auto.bazos.cz/seat/",
            "https://auto.bazos.cz/suzuki/",
            "https://auto.bazos.cz/skoda/",
            "https://auto.bazos.cz/toyota/",
            "https://auto.bazos.cz/volkswagen/",
            "https://auto.bazos.cz/volvo/"
    );
    private static final int REQUEST_TIMEOUT_MS = 20_000;
    private static final int MAX_LIST_PAGES_PER_CATEGORY = 2;
    private static final int MAX_DETAIL_LINKS = 1000;
    private static final int MIN_VALID_PRICE = 30_000;
    private static final int MAX_VALID_PRICE = 10_000_000;

    private static final Pattern TYRE_SIZE_PATTERN =
            Pattern.compile("\\b\\d{3}/\\d{2}\\s*[rR]\\s*\\d{2}\\b");

    private static final Pattern TYRE_SIZE_ALT_PATTERN =
            Pattern.compile("\\b\\d{3}/\\d{2}/[rR]?\\d{2}\\b");

    private static final Pattern RIM_SPEC_PATTERN =
            Pattern.compile("\\b\\d{1,2}[jJ]x\\d{2}\\b|\\bET\\s?\\d{2,3}\\b|\\b[45]x\\d{3}\\b");

    private static final Set<String> TYRE_BRANDS = Set.of(
            "hankook", "michelin", "continental", "goodyear", "barum",
            "bridgestone", "pirelli", "dunlop", "nokian", "firestone",
            "kumho", "matador", "yokohama", "toyo", "falken", "sava"
    );

    @Override
    public String getSourceName() {
        return "BAZOS";
    }

    @Override
    public List<CarDto> fetchCars() {
        List<CarDto> cars = new ArrayList<>();
        Set<String> allDetailUrls = new LinkedHashSet<>();

        int emptyTitleCount = 0;
        int demandListingCount = 0;
        int commercialVehicleCount = 0;
        int nonCarListingCount = 0;
        int brokenOrForPartsCount = 0;
        int suspiciousListingCount = 0;
        int invalidPriceCount = 0;
        int parseErrorCount = 0;

        try {
            for (String listPageBaseUrl : LIST_PAGE_BASE_URLS) {
                for (int page = 0; page < MAX_LIST_PAGES_PER_CATEGORY; page++) {
                    if (allDetailUrls.size() >= MAX_DETAIL_LINKS) {
                        break;
                    }

                    String pageUrl = buildListPageUrl(listPageBaseUrl, page);

                    try {
                        Document listDoc = loadDocument(pageUrl);

                        Set<String> pageUrls = extractDetailUrls(listDoc);

                        log.info(
                                "BAZOS page={} url={} detail links found={}",
                                page + 1,
                                pageUrl,
                                pageUrls.size()
                        );

                        if (pageUrls.isEmpty()) {
                            break;
                        }

                        int before = allDetailUrls.size();
                        allDetailUrls.addAll(pageUrls);
                        int added = allDetailUrls.size() - before;

                        if (added == 0) {
                            log.info("BAZOS pagination stopped page={} reason=no_new_links", page + 1);
                            break;
                        }

                    } catch (Exception e) {
                        log.warn("BAZOS list page parse failed url={} error={}", pageUrl, e.getMessage());
                        break;
                    }
                }

                if (allDetailUrls.size() >= MAX_DETAIL_LINKS) {
                    break;
                }
            }

            log.info("BAZOS total detail links collected={}", allDetailUrls.size());

            int count = 0;

            for (String url : allDetailUrls) {
                if (count >= MAX_DETAIL_LINKS) {
                    break;
                }

                try {
                    ParseResult result = parseDetail(url);

                    if (result.car() != null) {
                        cars.add(result.car());
                    } else {
                        switch (result.reason()) {
                            case "empty_title" -> emptyTitleCount++;
                            case "demand_listing" -> demandListingCount++;
                            case "commercial_vehicle" -> commercialVehicleCount++;
                            case "non_car_listing" -> nonCarListingCount++;
                            case "broken_or_for_parts" -> brokenOrForPartsCount++;
                            case "suspicious_listing" -> suspiciousListingCount++;
                            case "invalid_price" -> invalidPriceCount++;
                            case "parse_error" -> parseErrorCount++;
                        }
                    }

                } catch (Exception e) {
                    parseErrorCount++;
                    log.warn("BAZOS SKIP url={} reason=parse_error", safe(url));
                }

                count++;
            }

        } catch (Exception e) {
            log.warn("BAZOS parser failed: {}", e.getMessage());
        }

        log.info("BAZOS parsed {} cars", cars.size());
        log.info(
                "BAZOS SUMMARY parsed={} empty_title={} demand_listing={} commercial_vehicle={} non_car_listing={} broken_or_for_parts={} suspicious_listing={} invalid_price={} parse_error={}",
                cars.size(),
                emptyTitleCount,
                demandListingCount,
                commercialVehicleCount,
                nonCarListingCount,
                brokenOrForPartsCount,
                suspiciousListingCount,
                invalidPriceCount,
                parseErrorCount
        );

        return cars;
    }

    private Set<String> extractDetailUrls(Document listDoc) {
        Set<String> detailUrls = new LinkedHashSet<>();

        Elements links = listDoc.select("a[href*=/inzerat/]");
        for (Element link : links) {
            String href = link.absUrl("href");
            if (href != null && !href.isBlank() && href.contains("/inzerat/")) {
                detailUrls.add(href);
            }
        }

        return detailUrls;
    }

    private String buildListPageUrl(String baseUrl, int page) {
        if (page <= 0) {
            return baseUrl;
        }

        return baseUrl + (page * 20) + "/";
    }

    private String extractNextPageUrl(Document doc) {
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String text = normalizeText(link.text()).toLowerCase(Locale.ROOT);
            String href = link.absUrl("href");

            if (href == null || href.isBlank()) {
                continue;
            }

            if (text.equals("další") ||
                    text.equals("dalsi") ||
                    text.equals(">") ||
                    text.equals(">>") ||
                    text.contains("další") ||
                    text.contains("dalsi")) {
                return href;
            }
        }

        for (Element link : links) {
            String href = link.absUrl("href");

            if (href != null &&
                    href.contains("auto.bazos.cz") &&
                    href.contains("/inzeraty/osobni-auta/")) {
                String rawText = normalizeText(link.text());

                if (rawText.matches("\\d+")) {
                    return href;
                }
            }
        }

        return null;
    }

    private ParseResult parseDetail(String url) {
        try {
            Document doc = loadDocument(url);

            String title = extractTitle(doc);
            String preview = extractPreview(doc);
            String detailInfo = extractDetailInfoText(doc);

            String listingText = normalizeText(title + " " + preview);
            String analysisText = normalizeText(title + " " + preview + " " + detailInfo);
            String priceText = normalizeText(preview + " " + detailInfo);

            if (title.isBlank()) {
                log.info("BAZOS SKIP url={} reason=empty_title title={}", safe(url), safe(title));
                return ParseResult.skip("empty_title");
            }

            if (looksDemandListing(title, listingText, url)) {
                log.info("BAZOS SKIP url={} reason=demand_listing title={}", safe(url), safe(title));
                return ParseResult.skip("demand_listing");
            }

            if (looksCommercialVehicle(title, listingText, url)) {
                log.info("BAZOS SKIP url={} reason=commercial_vehicle title={}", safe(url), safe(title));
                return ParseResult.skip("commercial_vehicle");
            }

            if (looksTyreOrWheelListing(title, preview, analysisText)) {
                log.info("BAZOS SKIP url={} reason=tyre_or_wheel_listing title={}", safe(url), safe(title));
                return ParseResult.skip("non_car_listing");
            }

            if (containsNonCarBrand(title, listingText) && !looksLikeRealCar(title, analysisText)) {
                log.info("BAZOS SKIP url={} reason=non_car_brand title={}", safe(url), safe(title));
                return ParseResult.skip("non_car_listing");
            }

            if (looksNonCarListing(title, listingText, url, analysisText)) {
                log.info("BAZOS SKIP url={} reason=non_car_listing title={}", safe(url), safe(title));
                return ParseResult.skip("non_car_listing");
            }

            if (looksBrokenOrForPartsListing(title, analysisText)) {
                log.info("BAZOS SKIP url={} reason=broken_or_for_parts title={}", safe(url), safe(title));
                return ParseResult.skip("broken_or_for_parts");
            }

            if (looksSuspiciousListing(title, listingText)) {
                log.info("BAZOS SKIP url={} reason=suspicious_listing title={}", safe(url), safe(title));
                return ParseResult.skip("suspicious_listing");
            }

            boolean titleUrlMismatch = looksTitleUrlMismatch(title, url);
            if (titleUrlMismatch) {
                log.info("BAZOS WARN url={} reason=title_url_mismatch title={}", safe(url), safe(title));
            }

            if (looksBrandMismatch(title, url)) {
                log.info("BAZOS SKIP url={} reason=brand_url_mismatch title={}", safe(url), safe(title));
                return ParseResult.skip("non_car_listing");
            }

            Integer priceValue = extractPrice(doc, priceText);
            if (priceValue == null) {
                log.info("BAZOS SKIP url={} reason=missing_price title={}", safe(url), safe(title));
                return ParseResult.skip("invalid_price");
            }
            if (!isValidBazosPrice(priceValue)) {
                log.info("BAZOS SKIP url={} reason=invalid_price title={}", safe(url), safe(title));
                return ParseResult.skip("invalid_price");
            }

            String price = formatPrice(priceValue);
            String location = extractLocation(doc, analysisText);
            Integer year = extractYear(title, listingText);

            if (year == null) {
                year = extractYear("", preview);
            }
            Integer mileage = extractMileage(title, analysisText);
            String fuelType = firstNonBlank(
                    extractFuelType(title),
                    extractFuelType(listingText),
                    extractFuelType(analysisText)
            );
            String transmission = firstNonBlank(
                    extractTransmission(title),
                    extractTransmission(listingText),
                    extractTransmission(analysisText)
            );
            String brand = extractBrand(title, analysisText);
            String carType = extractCarType(title, listingText, url);
            String imageUrl = extractImageUrl(doc);

            if (isSuspiciousCheapCar(title, analysisText, priceValue, year, mileage, brand, carType)) {
                log.info(
                        "BAZOS SKIP url={} reason=suspicious_cheap_car title={} price={} year={} brand={} carType={}",
                        safe(url),
                        safe(title),
                        priceValue,
                        year,
                        safe(brand),
                        safe(carType)
                );
                return ParseResult.skip("suspicious_listing");
            }

            CarDto car = new CarDto();
            car.setSource("BAZOS");
            car.setTitle(title);
            car.setPrice(price);
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

            log.info("BAZOS CAR title='{}' price={} location={} year={} mileage={} fuelType={} transmission={} carType={} brand={} url={}",
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

            return ParseResult.ok(car);

        } catch (Exception e) {
            log.warn("BAZOS SKIP url={} reason=parse_error", safe(url));
            return ParseResult.skip("parse_error");
        }
    }

    private boolean isSuspiciousCheapCar(String title,
                                         String text,
                                         Integer priceValue,
                                         Integer year,
                                         Integer mileage,
                                         String brand,
                                         String carType) {
        if (priceValue == null) {
            return false;
        }

        if (priceValue < MIN_VALID_PRICE) {
            return true;
        }

        if (year != null && year >= 2015 && priceValue < 80_000) {
            return true;
        }

        if (year != null && year >= 2020 && priceValue < 150_000) {
            return true;
        }

        if (priceValue < 70_000 && mileage != null && mileage >= 250_000) {
            return true;
        }

        if (priceValue < 100_000 && mileage != null && mileage >= 320_000) {
            return true;
        }

        return false;
    }

    private String extractTitle(Document doc) {
        Element h1 = doc.selectFirst("h1.nadpisdetail, h1");
        if (h1 != null) {
            return normalizeText(h1.text());
        }
        return "";
    }

    private String extractPreview(Document doc) {
        Element body = doc.selectFirst(".popisdetail");
        if (body != null) {
            return normalizeText(body.text());
        }
        return "";
    }

    private String extractDetailInfoText(Document doc) {
        Element table = doc.selectFirst(".listadvlevo table");
        if (table != null) {
            return normalizeText(table.text());
        }

        Element detail = doc.selectFirst(".listadvlevo");
        if (detail != null) {
            return normalizeText(detail.text());
        }

        return "";
    }

    private Integer extractPrice(Document doc, String text) {
        Integer detailPrice = extractPriceFromDetailTable(doc);
        if (isValidBazosPrice(detailPrice)) {
            return detailPrice;
        }

        Element priceEl = doc.selectFirst(".inzeratycena, .price, b[class*=price], strong[class*=price]");

        if (priceEl != null) {
            String raw = normalizeText(priceEl.text());
            Integer price = parseNumber(raw);
            log.debug("BAZOS PRICE HTML raw='{}' parsed={}", safe(raw), price);

            if (isValidBazosPrice(price)) {
                return price;
            }

            String rawLower = raw.toLowerCase(Locale.ROOT);

            if (containsAny(rawLower, "v textu", "dohodou", "nabidnete", "nabídněte")) {
                Integer fromCenaLabel = extractPriceFromCenaLabel(text);
                if (isValidBazosPrice(fromCenaLabel)) {
                    return fromCenaLabel;
                }

                Integer fromKcPattern = extractPriceFromKcPattern(text);
                if (isValidBazosPrice(fromKcPattern)) {
                    return fromKcPattern;
                }
            }
        } else {
            log.debug("BAZOS PRICE HTML raw=-");
        }

        Integer fromCenaLabel = extractPriceFromCenaLabel(text);
        if (isValidBazosPrice(fromCenaLabel)) {
            return fromCenaLabel;
        }

        Integer fromKcPattern = extractPriceFromKcPattern(text);
        if (isValidBazosPrice(fromKcPattern)) {
            return fromKcPattern;
        }

        log.info("BAZOS PRICE NOT FOUND");
        return null;
    }

    private Integer extractPriceFromDetailTable(Document doc) {
        for (Element row : doc.select(".listadvlevo tr")) {
            Elements cells = row.select("td");
            if (cells.size() < 2) {
                continue;
            }

            String label = normalizeText(cells.get(0).text()).toLowerCase(Locale.ROOT);
            if (!label.startsWith("cena")) {
                continue;
            }

            String raw = normalizeText(cells.text());
            Integer price = extractPriceFromKcPattern(raw);
            log.debug("BAZOS PRICE DETAIL_TABLE raw='{}' parsed={}", safe(raw), price);

            if (isValidBazosPrice(price)) {
                return price;
            }
        }

        return null;
    }

    private Integer extractPriceFromCenaLabel(String text) {
        Matcher matcher = Pattern.compile(
                "(?i)\\bcena\\s*[:\\-]?\\s*([0-9]{2,3}(?:[\\s\\.][0-9]{3})+|[0-9]{5,7})\\s*(?:kč|kc|czk)?\\b"
        ).matcher(text);

        while (matcher.find()) {
            String raw = matcher.group(1);
            Integer candidate = parseNumber(raw);
            log.debug("BAZOS PRICE LABEL raw='{}' parsed={}", safe(raw), candidate);

            if (isValidBazosPrice(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private Integer extractPriceFromKcPattern(String text) {
        Matcher matcher = Pattern.compile(
                "(?i)\\b([0-9]{2,3}(?:[\\s\\.][0-9]{3})+|[0-9]{5,7})\\s*(?:kč|kc|czk)\\b"
        ).matcher(text);

        while (matcher.find()) {
            String raw = matcher.group(1);
            Integer candidate = parseNumber(raw);
            log.debug("BAZOS PRICE TEXT raw='{}' parsed={}", safe(raw), candidate);

            if (isValidBazosPrice(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean isValidBazosPrice(Integer price) {
        return price != null && price >= MIN_VALID_PRICE && price <= MAX_VALID_PRICE;
    }

    private String extractLocation(Document doc, String fullText) {
        String detailLocation = extractLocationFromDetailTable(doc);
        if (isRealLocation(detailLocation)) {
            return cleanLocation(detailLocation);
        }

        String locationFromLink = extractLocationFromDetailLink(doc);
        if (isRealLocation(locationFromLink)) {
            return cleanLocation(locationFromLink);
        }

        String locationFromMeta = extractLocationFromMeta(doc);
        if (isRealLocation(locationFromMeta)) {
            return cleanLocation(locationFromMeta);
        }

        String locationFromTitle = extractLocationFromTitle(doc);
        if (isRealLocation(locationFromTitle)) {
            return cleanLocation(locationFromTitle);
        }

        Element locationEl = doc.selectFirst(".inzeratylokality, .inzeratylok, .lokalita");
        if (locationEl != null) {
            String raw = normalizeText(locationEl.text());
            raw = raw.replaceFirst("(?i)^lokalita\\s*:?\\s*", "").trim();

            if (isRealLocation(raw)) {
                return cleanLocation(raw);
            }
        }

        Matcher matcher = Pattern.compile(
                "(?i)(?:lokalita|okres|město|mesto|kraj)\\s*:?\\s*([A-ZÁČĎÉĚÍŇÓŘŠŤÚŮÝŽa-záčďéěíňóřšťúůýž0-9\\- ]{2,60})"
        ).matcher(fullText);

        if (matcher.find()) {
            String raw = normalizeText(matcher.group(1));
            raw = raw.replaceFirst("(?i)^(lokalita|okres|město|mesto|kraj)\\s*:?\\s*", "").trim();

            if (isRealLocation(raw)) {
                return cleanLocation(raw);
            }
        }

        return null;
    }

    private String extractLocationFromTitle(Document doc) {
        String value = extractLocationFromTitleText(doc.title());
        if (isRealLocation(value)) {
            return value;
        }

        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null) {
            return extractLocationFromTitleText(ogTitle.attr("content"));
        }

        return null;
    }

    private String extractLocationFromTitleText(String titleText) {
        String title = normalizeText(titleText);
        if (title.isBlank()) {
            return null;
        }

        title = title.replaceFirst("(?i)\\s*\\|\\s*Bazoš\\.cz\\s*$", "").trim();

        int separatorIndex = title.lastIndexOf(" - ");
        if (separatorIndex < 0 || separatorIndex + 3 >= title.length()) {
            return null;
        }

        return normalizeText(title.substring(separatorIndex + 3));
    }

    private String extractLocationFromDetailLink(Document doc) {
        Element locationLink = doc.selectFirst(".listadvlevo a[href*=\"/inzeraty/\"][href$=\"/\"]");
        if (locationLink == null) {
            return null;
        }

        String value = normalizeText(locationLink.text());
        if (value.matches("\\d{3}\\s?\\d{2}")) {
            return null;
        }

        return value;
    }

    private String extractLocationFromMeta(Document doc) {
        for (Element meta : doc.select("meta[name=description], meta[property=og:description], meta[property=og:title]")) {
            String content = normalizeText(meta.attr("content"));
            if (content.isBlank()) {
                continue;
            }

            Matcher matcher = Pattern.compile(
                    "(?i)\\bLokalita:\\s*([^.,|]{2,60})"
            ).matcher(content);
            if (matcher.find()) {
                return normalizeText(matcher.group(1));
            }

            String fromTitle = extractLocationFromTitleText(content);
            if (isRealLocation(fromTitle)) {
                return fromTitle;
            }
        }

        return null;
    }

    private String extractLocationFromDetailTable(Document doc) {
        for (Element row : doc.select(".listadvlevo tr")) {
            Elements cells = row.select("td");
            if (cells.size() < 2) {
                continue;
            }

            String label = normalizeText(cells.get(0).text()).toLowerCase(Locale.ROOT);
            if (!label.startsWith("lokalita")) {
                continue;
            }

            String raw = normalizeText(cells.get(cells.size() - 1).text())
                    .replaceFirst("(?i)^lokalita\\s*:?\\s*", "")
                    .trim();

            return raw;
        }

        return null;
    }

    private Integer extractYear(String title, String text) {
        String source = normalizeText(title + " " + text);

        if (containsAny(source, " PRODANO ", " PRODÁNO ", " ZADANO ", " ZADÁNO ")) {
            source = normalizeText(title);
        }

        Matcher matcher = Pattern.compile(
                "(?i)(?:rok výroby|rok vyroby|r\\.v\\.?|rv|první registrace|prvni registrace|do provozu|uvedení do provozu|uvedeni do provozu)\\s*[:\\-]?\\s*(?:\\d{1,2}\\s*/\\s*)?(19\\d{2}|20\\d{2})"
        ).matcher(source);

        if (matcher.find()) {
            return parseYearCandidate(matcher.group(1));
        }

        matcher = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(source);
        while (matcher.find()) {
            String rawYear = matcher.group(1);

            String normalizedLower = normalizeText(source).toLowerCase(Locale.ROOT);

            if (
                    ("2008".equals(rawYear) && normalizedLower.contains("peugeot 2008")) ||
                            ("3008".equals(rawYear) && normalizedLower.contains("peugeot 3008")) ||
                            ("5008".equals(rawYear) && normalizedLower.contains("peugeot 5008"))
            ) {
                continue;
            }

            Integer year = parseYearCandidate(rawYear);

            if (year != null && !isBadYearContext(source, matcher.start(), matcher.end())) {
                return year;
            }
        }

        return null;
    }

    private boolean isBadYearContext(String text, int start, int end) {
        int from = Math.max(0, start - 80);
        int to = Math.min(text.length(), end + 80);

        String context = text.substring(from, to).toLowerCase(Locale.ROOT);

        return context.matches(".*\\[?\\d{1,2}\\.\\d{1,2}\\.\\s*20\\d{2}\\]?.*")
                || context.contains("plati do")
                || context.contains("platĂ­ do")
                || context.contains("vlozen")
                || context.contains("vloĹľen")
                || context.contains("vloĹľeno")
                || context.contains("vlozeno")
                || context.contains("pridano")
                || context.contains("pĹ™idĂˇno")
                || context.contains("aktualiz")
                || context.contains("inzerat")
                || context.contains("inzerĂˇt")
                || context.contains("stk")
                || context.contains("stk:")
                || context.contains("stk do")
                || context.contains("tk ")
                || context.contains("tk:")
                || context.contains("tk do")
                || context.contains("technick")
                || context.contains("platná do")
                || context.contains("platna do")
                || context.contains("do provozu")
                || context.contains("do roku")
                || context.contains("záruka")
                || context.contains("zaruka")
                || context.contains("garance")
                || context.contains("servis do")
                || context.contains("serviska do")
                || context.contains("rezervace")
                || context.contains("rezervováno")
                || context.contains("rezervovano")
                || context.contains("prodáno")
                || context.contains("prodano")
                || context.contains("zadáno")
                || context.contains("zadano")
                || context.contains("model 2025")
                || context.contains("model 2026")
                || context.contains("rok 2025")
                || context.contains("rok 2026");
    }

    private Integer extractMileage(String title, String text) {
        String source = normalizeText(title + " " + text);

        Matcher matcher = Pattern.compile(
                "(?i)(?:najeto|najetých km|najetych km|stav tachometru|počet km|pocet km)\\s*[:\\-]?\\s*([0-9\\s\\.]{2,})\\s*km"
        ).matcher(source);

        if (matcher.find()) {
            Integer value = parseMileageCandidate(matcher.group(1));
            if (value != null) {
                return value;
            }
        }

        matcher = Pattern.compile("(?i)\\b([0-9]{2,3}[\\s\\.][0-9]{3}|[0-9]{5,6})\\s*km\\b").matcher(source);
        while (matcher.find()) {
            Integer value = parseMileageCandidate(matcher.group(1));
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private String extractFuelType(String text) {
        String source = " " + normalizeText(text).toLowerCase(Locale.ROOT) + " ";
        String compact = source.replaceAll("[^a-z0-9]", "");

        if (containsAny(source,
                " palivo: elektro ",
                " palivo elektro ",
                " elektromobil ",
                " elektroauto ",
                " electric vehicle ",
                " electric ",
                " ev ",
                " kwh ",
                " bev ",
                " enyaq ", " id.3 ", " id.4 ", " id.5 ", " tesla ", " leaf ", " kona electric ")) {
            return "ELECTRIC";
        }

        if (containsAny(source,
                " hybrid ",
                " hybridní ",
                " hybridni ",
                " plug-in hybrid ",
                " plugin hybrid ",
                " plug in ",
                " plug-in ",
                " hev ",
                " phev ",
                " superb iv ",
                " ehybrid ",
                " mhev ",
                " e-hybrid ")) {
            return "HYBRID";
        }

        if (source.contains(" superb ")
                && containsAny(source,
                " phev ",
                " plug-in ",
                " plug in ",
                " ehybrid ",
                " e-hybrid ")) {
            return "HYBRID";
        }

        if (containsAny(source, " lpg ")) {
            return "LPG";
        }

        if (containsAny(source, " cng ")) {
            return "CNG";
        }

        if (compact.contains("tdi")
                || compact.contains("tdci")
                || compact.contains("cdi")
                || compact.contains("dci")
                || compact.contains("hdi")
                || compact.contains("crdi")
                || compact.contains("jtd")
                || compact.contains("multijet")
                || compact.contains("bluehdi")
                || compact.contains("cdti")
                || compact.contains("20d")
                || compact.contains("118d")
                || compact.contains("120d")
                || compact.contains("220d")
                || compact.contains("320d")
                || compact.contains("330d")
                || compact.contains("420d")
                || compact.contains("520d")
                || compact.contains("525d")
                || compact.contains("530d")
                || compact.contains("540d")
                || compact.contains("550d")
                || compact.contains("xdrive30d")
                || compact.contains("m550d")
                || compact.contains("sdv6")) {
            return "DIESEL";
        }

        if (compact.contains("tsi")
                || compact.contains("tfsi")
                || compact.contains("mpi")
                || compact.contains("gdi")
                || compact.contains("tgdi")
                || compact.contains("tce")
                || compact.contains("ecoboost")
                || compact.contains("skyactivg")
                || compact.contains("vvti")
                || compact.contains("vvt")) {
            return "PETROL";
        }

        if (containsAny(source,
                " palivo: benzin ",
                " palivo: benzín ",
                " palivo benzin ",
                " palivo benzín ",
                " benzin ",
                " benzín ",
                " benzinove ",
                " benzínové ",
                " fsi ",
                " vti ",
                " vvt-i ",
                " i-vtec ",
                " skyactiv-g ",
                " 1.0i ",
                " 1.2i ",
                " 1.4i ",
                " 1.5i ",
                " 1.6i ",
                " 1.8i ",
                " 2.0i ",
                " 2.4i ",
                " 2.5i ",
                " 2.8i ",
                " 3.0i ",
                " 3.2i ",
                " 3.5i ",
                " 4.0i ",
                " 5.0i ",
                " mustang ",
                " v8 ",
                " 330i ",
                " 320i ",
                " 318i ",
                " 116i ",
                " 118i ",
                " 120i ",
                " 520i ",
                " 523i ",
                " 528i ",
                " n52 ",
                " n53 ",
                " n54 ",
                " n55 ",
                " b48 ",
                " b58 ")) {
            return "PETROL";
        }

        if (containsAny(source,
                " palivo: nafta ",
                " palivo nafta ",
                " diesel ",
                " nafta ",
                " tdi ",
                " tdci ",
                " cdi ",
                " dci ",
                " hdi ",
                " crdi ",
                " jtd ",
                " multijet ",
                " bluehdi ",
                " cdti ",
                " 1.3 cdti ",
                " 1.5 dci ",
                " 1.6 tdi ",
                " 1.9 tdi ",
                " 2.0 tdi ",
                " 2.2 cdi ",
                " 3.0 tdi ")) {
            return "DIESEL";
        }

        return null;
    }

    private String extractTransmission(String text) {
        String source = " " + normalizeText(text).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(source,
                " manuální převodovka ",
                " manualni prevodovka ",
                " manuální ",
                " manualni ",
                " manuál ",
                " manual ",
                " man. ",
                " 5stupňová manuální ",
                " 5stupnova manualni ",
                " 6stupňová manuální ",
                " 6stupnova manualni ",
                " řazení manuální ",
                " razeni manualni ")) {
            return "MANUAL";
        }

        if (containsAny(source,
                " automatická převodovka ",
                " automaticka prevodovka ",
                " automatická ",
                " automaticka ",
                " automat ",
                " automatic ",
                " automatu ",
                " automatem ",
                " aut. ",
                " a/t ",
                " at6 ",
                " at8 ",
                " cvt ",
                " e-cvt ",
                " ecvt ",
                " dsg ",
                " s tronic ",
                " stronic ",
                " tiptronic ",
                " powershift ",
                " multitronic ",
                " steptronic ",
                " x-tronic ",
                " xtronic ")) {
            return "AUTOMATIC";
        }

        return null;
    }

    private String extractBrand(String title, String text) {
        String titleSource = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String source = titleSource + " " + shortenForCheck(normalizeText(text).toLowerCase(Locale.ROOT), 300);

        if (containsAny(titleSource,
                " alfa romeo ", " alfa ", " alfu ", " romeo ",
                " stelvio ", " giulia ", " giulietta ", " mito ", " alfetta ")) return "ALFA_ROMEO";
        if (containsAny(titleSource, " škoda ", " skoda ")) return "SKODA";
        if (containsAny(titleSource, " volkswagen ", " vw ")) return "VOLKSWAGEN";
        if (containsAny(titleSource, " audi ")) return "AUDI";
        if (containsAny(titleSource, " bmw ")) return "BMW";
        if (containsAny(titleSource, " mercedes ", " mercedes-benz ")) return "MERCEDES";
        if (containsAny(titleSource, " toyota ")) return "TOYOTA";
        if (containsAny(titleSource, " ford ")) return "FORD";
        if (containsAny(titleSource, " renault ")) return "RENAULT";
        if (containsAny(titleSource, " seat ")) return "SEAT";
        if (containsAny(titleSource, " peugeot ")) return "PEUGEOT";
        if (containsAny(titleSource, " opel ")) return "OPEL";
        if (containsAny(titleSource, " hyundai ")) return "HYUNDAI";
        if (containsAny(titleSource, " kia ")) return "KIA";
        if (containsAny(titleSource, " volvo ")) return "VOLVO";
        if (containsAny(titleSource, " lexus ")) return "LEXUS";
        if (containsAny(titleSource, " mazda ")) return "MAZDA";
        if (containsAny(titleSource, " citroën ", " citroen ", " citreon ")) return "CITROEN";
        if (containsAny(titleSource, " fiat ")) return "FIAT";
        if (containsAny(titleSource, " dodge ")) return "DODGE";
        if (containsAny(titleSource, " nissan ")) return "NISSAN";
        if (containsAny(titleSource, " honda ")) return "HONDA";
        if (containsAny(titleSource, " suzuki ")) return "SUZUKI";
        if (containsAny(titleSource, " dacia ")) return "DACIA";
        if (containsAny(titleSource, " cupra ")) return "CUPRA";
        if (containsAny(titleSource, " jeep ")) return "JEEP";
        if (containsAny(titleSource, " subaru ")) return "SUBARU";
        if (containsAny(titleSource, " mitsubishi ")) return "MITSUBISHI";
        if (containsAny(titleSource, " porsche ")) return "PORSCHE";
        if (containsAny(titleSource, " mini ")) return "MINI";
        if (containsAny(titleSource, " tesla ")) return "TESLA";
        if (containsAny(titleSource, " chevrolet ")) return "CHEVROLET";
        if (containsAny(titleSource, " land rover ", " range rover ")) return "LAND_ROVER";

        if (containsAny(source,
                " alfa romeo ", " alfa ", " alfu ", " romeo ",
                " stelvio ", " giulia ", " giulietta ", " mito ", " alfetta ")) return "ALFA_ROMEO";
        if (containsAny(source, " škoda ", " skoda ")) return "SKODA";
        if (containsAny(source, " volkswagen ", " vw ")) return "VOLKSWAGEN";
        if (containsAny(source, " audi ")) return "AUDI";
        if (containsAny(source, " bmw ")) return "BMW";
        if (containsAny(source, " mercedes ", " mercedes-benz ")) return "MERCEDES";
        if (containsAny(source, " toyota ")) return "TOYOTA";
        if (containsAny(source, " ford ")) return "FORD";
        if (containsAny(source, " renault ")) return "RENAULT";
        if (containsAny(source, " seat ")) return "SEAT";
        if (containsAny(source, " peugeot ")) return "PEUGEOT";
        if (containsAny(source, " opel ")) return "OPEL";
        if (containsAny(source, " hyundai ")) return "HYUNDAI";
        if (containsAny(source, " kia ")) return "KIA";
        if (containsAny(source, " volvo ")) return "VOLVO";
        if (containsAny(source, " lexus ")) return "LEXUS";
        if (containsAny(source, " mazda ")) return "MAZDA";
        if (containsAny(source, " citroën ", " citroen ", " citreon ")) return "CITROEN";
        if (containsAny(source, " fiat ")) return "FIAT";
        if (containsAny(source, " dodge ")) return "DODGE";
        if (containsAny(source, " nissan ")) return "NISSAN";
        if (containsAny(source, " honda ")) return "HONDA";
        if (containsAny(source, " suzuki ")) return "SUZUKI";
        if (containsAny(source, " dacia ")) return "DACIA";
        if (containsAny(source, " cupra ")) return "CUPRA";
        if (containsAny(source, " jeep ")) return "JEEP";
        if (containsAny(source, " subaru ")) return "SUBARU";
        if (containsAny(source, " mitsubishi ")) return "MITSUBISHI";
        if (containsAny(source, " porsche ")) return "PORSCHE";
        if (containsAny(source, " mini ")) return "MINI";
        if (containsAny(source, " tesla ")) return "TESLA";
        if (containsAny(source, " chevrolet ")) return "CHEVROLET";
        if (containsAny(source, " land rover ", " range rover ")) return "LAND_ROVER";

        // fallback model detection

        if (source.contains(" golf ")) return "VOLKSWAGEN";
        if (source.contains(" passat ")) return "VOLKSWAGEN";
        if (source.contains(" tiguan ")) return "VOLKSWAGEN";
        if (source.contains(" touareg ")) return "VOLKSWAGEN";
        if (source.contains(" sharan ")) return "VOLKSWAGEN";
        if (source.contains(" touran ")) return "VOLKSWAGEN";
        if (source.contains(" caddy ")) return "VOLKSWAGEN";

        if (source.contains(" octavia ")) return "SKODA";
        if (source.contains(" superb ")) return "SKODA";
        if (source.contains(" fabia ")) return "SKODA";
        if (source.contains(" kodiaq ")) return "SKODA";
        if (source.contains(" karoq ")) return "SKODA";
        if (source.contains(" kamiq ")) return "SKODA";
        if (source.contains(" roomster ")) return "SKODA";
        if (source.contains(" scala ")) return "SKODA";
        if (source.contains(" citigo ")) return "SKODA";
        if (source.contains(" enyaq ")) return "SKODA";

        if (source.contains(" focus ")) return "FORD";
        if (source.contains(" mondeo ")) return "FORD";
        if (source.contains(" fiesta ")) return "FORD";
        if (source.contains(" kuga ")) return "FORD";
        if (source.contains(" galaxy ")) return "FORD";
        if (source.contains(" ranger ")) return "FORD";
        if (source.contains(" mustang ")) return "FORD";

        if (source.contains(" megane ")) return "RENAULT";
        if (source.contains(" scenic ")) return "RENAULT";
        if (source.contains(" clio ")) return "RENAULT";
        if (source.contains(" thalia ")) return "RENAULT";
        if (source.contains(" kangoo ")) return "RENAULT";

        if (source.contains(" berlingo ")) return "CITROEN";
        if (source.contains(" picasso ")) return "CITROEN";
        if (source.contains(" c2 ")) return "CITROEN";
        if (source.contains(" c3 ")) return "CITROEN";
        if (source.contains(" c4 ")) return "CITROEN";
        if (source.contains(" c5 ")) return "CITROEN";
        if (source.contains(" ds5 ")) return "CITROEN";
        if (source.contains(" xsara ")) return "CITROEN";

        if (source.contains(" tipo ")) return "FIAT";
        if (source.contains(" panda ")) return "FIAT";
        if (source.contains(" ducato ")) return "FIAT";
        if (source.contains(" fiat 500 ") || source.contains(" 500 lounge ")) return "FIAT";

        if (source.contains(" compass ")) return "JEEP";
        if (source.contains(" cherokee ")) return "JEEP";
        if (source.contains(" renegade ")) return "JEEP";

        if (source.contains(" sportage ")) return "KIA";
        if (source.contains(" ceed ")) return "KIA";
        if (source.contains(" carens ")) return "KIA";

        if (source.contains(" leon ")) return "SEAT";
        if (source.contains(" ibiza ")) return "SEAT";
        if (source.contains(" alhambra ")) return "SEAT";

        if (source.contains(" rav4 ")) return "TOYOTA";
        if (source.contains(" land cruiser ")) return "TOYOTA";
        if (source.contains(" aygo ")) return "TOYOTA";

        if (source.contains(" xc60 ")) return "VOLVO";
        if (source.contains(" xc90 ")) return "VOLVO";
        if (source.contains(" v40 ")) return "VOLVO";
        if (source.contains(" v60 ")) return "VOLVO";
        if (source.contains(" v90 ")) return "VOLVO";
        if (source.contains(" c70 ")) return "VOLVO";

        if (source.contains(" 540d ") || source.contains(" xdrive ")) return "BMW";
        if (containsAny(source,
                " a3 ", " a4 ", " a5 ", " a6 ", " a7 ", " a8 ",
                " a6c7 ", " q3 ", " q4 ", " q5 ", " sq7 ", " rs3 ", " rs 3 ",
                " rs6 ", " rs 6 ", " etron ", " e-tron ")) return "AUDI";
        if (source.contains(" q7 ")) return "AUDI";
        if (source.contains(" sq5 ")) return "AUDI";
        if (source.contains(" 159 ")) return "ALFA_ROMEO";
        if (source.contains(" 156 ")) return "ALFA_ROMEO";
        if (source.contains(" 147 ")) return "ALFA_ROMEO";
        if (source.contains(" brera ")) return "ALFA_ROMEO";
        if (source.contains(" stelvio ")) return "ALFA_ROMEO";
        if (source.contains(" model s ")) return "TESLA";
        if (source.contains(" grand cherokee ")) return "JEEP";
        if (source.contains(" cayenne ")) return "PORSCHE";
        if (source.contains(" 2008 ")) return "PEUGEOT";
        if (source.contains(" 308 ")) return "PEUGEOT";
        if (source.contains(" corsa ")) return "OPEL";
        if (source.contains(" mazda 3 ")) return "MAZDA";
        if (source.contains(" mazda 6 ")) return "MAZDA";
        if (source.contains(" partner ")) return "PEUGEOT";
        if (source.contains(" mazda 5 ")) return "MAZDA";
        if (source.contains(" favorit ")) return "SKODA";
        if (source.contains(" kangoo ")) return "RENAULT";

        return null;
    }

    private String extractCarType(String title, String text) {
        return extractCarType(title, text, null);
    }

    private String extractCarType(String title, String text, String url) {
        String titleSource = " " + normalizeText(safe(title)).toLowerCase(Locale.ROOT) + " ";
        String textSource = " " + normalizeText(safe(text)).toLowerCase(Locale.ROOT) + " ";
        String urlSource = " " + normalizeText(safe(url)).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(titleSource,
                " suv ", " crossover ",
                " x1 ", " x2 ", " x3 ", " x4 ", " x5 ", " x6 ", " x7 ",
                " q2 ", " q3 ", " q4 ", " q5 ", " q7 ", " q8 ",
                " glc ", " gle ", " gls ", " gla ", " glb ",
                " kodiaq ", " karoq ", " kamiq ",
                " tiguan ", " touareg ", " t-roc ", " troc ",
                " qashqai ", " juke ", " x-trail ",
                " kuga ", " puma ", " ecosport ",
                " formentor ", " ateca ", " arona ", " tarraco ",
                " xc40 ", " xc60 ", " xc90 ",
                " ex30 ", " ex40 ", " ex90 ",
                " captur ", " austral ", " rafale ",
                " sportage ", " sorento ", " stonic ",
                " tucson ", " santa fe ", " kona ",
                " duster ", " koleos ", " kadjar ",
                " cr-v ", " hr-v ", " rav4 ", " c-hr ", " chr ",
                " cx-3 ", " cx3 ", " cx-5 ", " cx5 ",
                " macan ", " cayenne ",
                " ux ", " nx ", " rx ",
                " enyaq ", " id.4 ", " id.5 ",
                " range rover ", " evoque ", " velar ",
                " discovery ", " discovery sport ", " defender ",
                " compass ", " cherokee ", " grand cherokee ",
                " grand vitara ", " vitara ",
                " captiva ", " tahoe ", " suburban ",
                " model y ",
                " xv ", " forester ",
                " mokka ",
                " xceed ",
                " ix35 ",
                " stelvio ",
                " durango ",
                " g trieda ", " g-trieda ", " g class ", " g-class ")) {
            return "SUV";
        }

        if (containsAny(titleSource,
                " mpv ", " minivan ",
                " scenic ", " espace ",
                " galaxy ", " s-max ", " smax ",
                " sharan ", " alhambra ", " touran ",
                " caddy ", " berlingo ", " rifter ",
                " partner tepee ",
                " zafira ", " meriva ",
                " roomster ", " lodgy ", " verso ",
                " c-max ", " grand c-max ",
                " tourneo courier ", " tourneo connect ",
                " doblo ", " combo ", " vaneo ",
                " picasso ", " grand c4 picasso ", " c4 picasso ",
                " b 200 ", " b200 ",
                " mazda 5 ",
                " grand scenic ", " grand scénic ",
                " kangoo ", " carens ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource,
                " shooting brake ", " gran tourer ",
                " grandtour ", " grand tour ",
                " kombi ", " combi ", " combi2", " kombi2",
                " wagon ", " avant ", " variant ",
                " touring ", " caravan ", " estate ",
                " alltrack ", " scout ", " outback ",
                " v40 ", " v50 ", " v60 ", " v70 ", " v90 ",
                " g31 ", " tipo sw ")) {
            return "WAGON";
        }

        if (containsAny(titleSource,
                " hatchback ", " hatch ", " spaceback ",
                " fabia ", " focus ", " golf ", " polo ",
                " i20 ", " i30 ", " ceed ",
                " aveo ", " spark ", " picanto ",
                " c2 ", " c3 ",
                " clio ", " megane ", " fiesta ",
                " civic ", " leon ", " swift ",
                " agila ", " 107 ", " 207 ", " 208 ", " 308 ",
                " sandero ", " logan ", " scala ", " citigo ",
                " fiat 500 ", " tipo ", " fiat tipo ",
                " corsa ", " mazda 3 ", " rapid ", " yaris ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource,
                " cabrio ", " kabrio ",
                " roadster ", " spyder ", " spider ",
                " convertible ", " cabriolet ",
                " sl 500 ", " sl500 ", " sl 63 ", " sl63 ",
                " slk ", " z4 ", " mx-5 ", " boxster ")) {
            return "CABRIO";
        }

        if (containsAny(titleSource,
                " gran coupe ", " gran coupé ",
                " coupe ", " coupé ",
                " mustang ", " amg gt ",
                " tt ", " supra ", " brz ",
                " gt86 ", " gr86 ",
                " 370z ", " 350z ",
                " rc f ", " rc 300h ", " lexus rc ",
                " r8 ", " camaro ")) {
            return "COUPE";
        }

        if (containsAny(titleSource,
                " pickup ", " pick-up ",
                " ranger ", " hilux ", " amarok ",
                " navara ", " l200 ",
                " ram ", " gladiator ")) {
            return "PICKUP";
        }

        if (containsAny(titleSource,
                " sportback ", " fastback ", " liftback ",
                " sedan ", " saloon ",
                " limo ", " limousine ", " limuzína ", " limuzina ",
                " charger ",
                " octavia ", " superb ", " passat ", " arteon ",
                " a4 ", " a6 ", " a7 ", " a8 ", " s5 ", " s7 ", " s8 ",
                " s400d ", " s400 ",
                " e90 ", " e60 ", " e39 ",
                " 3 series ", " 5 series ", " 7 series ",
                " řada 3 ", " rada 3 ", " řada 5 ", " rada 5 ", " řada 7 ", " rada 7 ",
                " c5 ", " mondeo sedan ",
                " model 3 ", " model s ",
                " cordoba ", " ds5 ",
                " eqe ", " eqs ",
                " cls ", " cla ",
                " 520 ", " 525 ", " 530 ", " 540 ",
                " c220d ", " c 220 ", " c-class ", " e-class ",
                " thalia ")) {
            return "SEDAN";
        }

        if (containsAny(urlSource,
                "/suv-", "/off-road/", "crossover")) {
            return "SUV";
        }

        if (containsAny(urlSource,
                "/mpv-", "minivan", "partner-tepee", "mazda-5", "grand-scenic")) {
            return "MINIVAN";
        }

        if (containsAny(urlSource,
                "/kombi-", "combi", "kombi", "avant", "variant", "touring",
                "caravan", "shooting-brake", "grandtour", "grand-tour",
                "alltrack", "scout")) {
            return "WAGON";
        }

        if (containsAny(urlSource,
                "/hatchback-", "spaceback")) {
            return "HATCHBACK";
        }

        if (containsAny(urlSource,
                "cabrio", "roadster", "spyder", "spider", "convertible", "cabriolet")) {
            return "CABRIO";
        }

        if (containsAny(urlSource,
                "gran-coupe", "gran-coupé", "coupe", "camaro")) {
            return "COUPE";
        }

        if (containsAny(urlSource,
                "pickup", "pick-up")) {
            return "PICKUP";
        }

        if (containsAny(urlSource,
                "/liftback-", "/sedan-", "sportback", "fastback", "408")) {
            return "SEDAN";
        }

        if (containsAny(textSource,
                " suv ", " crossover ")) {
            return "SUV";
        }

        if (containsAny(textSource,
                " mpv ", " minivan ")) {
            return "MINIVAN";
        }

        if (containsAny(textSource,
                " shooting brake ", " grandtour ",
                " combi ", " kombi ", " wagon ",
                " avant ", " variant ", " touring ",
                " caravan ", " estate ",
                " alltrack ", " scout ")) {
            return "WAGON";
        }

        if (containsAny(textSource,
                " hatchback ", " hatch ", " spaceback ")) {
            return "HATCHBACK";
        }

        if (containsAny(textSource,
                " cabrio ", " kabrio ",
                " roadster ", " spyder ", " spider ",
                " convertible ", " cabriolet ")) {
            return "CABRIO";
        }

        if (containsAny(textSource,
                " gran coupe ", " gran coupé ",
                " coupe ", " coupé ")) {
            return "COUPE";
        }

        if (containsAny(textSource,
                " pickup ", " pick-up ")) {
            return "PICKUP";
        }

        if (containsAny(textSource,
                " sportback ", " fastback ", " liftback ",
                " sedan ", " saloon ", " limo ",
                " limousine ", " limuzína ", " limuzina ")) {
            return "SEDAN";
        }

        return null;
    }

    private String extractImageUrl(Document doc) {
        Element img = doc.selectFirst(
                "meta[property=og:image], " +
                        "link[rel=image_src], " +
                        "#imgmain, " +
                        "img[src*=/img/], " +
                        "img[src*=bazos], " +
                        "img[data-src*=/img/], " +
                        "img[data-src*=bazos], " +
                        "a[href*=/img/]");

        if (img == null) {
            return null;
        }

        String src = firstNonBlank(
                img.hasAttr("content") ? img.absUrl("content") : null,
                img.hasAttr("href") ? img.absUrl("href") : null,
                img.hasAttr("data-src") ? img.absUrl("data-src") : null,
                img.hasAttr("src") ? img.absUrl("src") : null
        );

        if (src == null) {
            return null;
        }

        String lower = src.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "logo", "banner", "icon", "favicon", "bazos.cz/img/bazos")) {
            return null;
        }

        return src;
    }

    private boolean looksTitleUrlMismatch(String title, String url) {
        String t = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String u = " " + normalizeText(url).toLowerCase(Locale.ROOT) + " ";

        if (u.contains("golf") && containsAny(t, " vito ", " viano ", " sprinter ")) return true;
        if (u.contains("vito") && containsAny(t, " golf ", " passat ", " octavia ", " leon ")) return true;
        if (u.contains("octavia") && containsAny(t, " vito ", " sprinter ", " trafic ")) return true;
        if (u.contains("sprinter") && containsAny(t, " golf ", " leon ", " fabia ", " octavia ")) return true;
        if (u.contains("fabia") && containsAny(t, " vito ", " sprinter ")) return true;
        if (u.contains("passat") && containsAny(t, " vito ", " sprinter ")) return true;

        return false;
    }

    private boolean looksBrandMismatch(String title, String url) {
        String titleBrand = extractBrand(title, title);
        String urlLower = normalizeText(url).toLowerCase(Locale.ROOT);

        if (titleBrand == null) {
            return false;
        }

        return switch (titleBrand) {
            case "SKODA" -> containsAny(urlLower, "audi-", "bmw-", "mercedes-", "dacia-", "ford-", "toyota-");
            case "DACIA" -> containsAny(urlLower, "skoda-", "audi-", "bmw-", "mercedes-", "volkswagen-", "seat-");
            case "BMW" -> containsAny(urlLower, "skoda-", "dacia-", "seat-", "renault-");
            case "MERCEDES" -> containsAny(urlLower, "skoda-", "seat-", "dacia-", "ford-");
            case "SEAT" -> containsAny(urlLower, "dacia-", "mercedes-", "bmw-", "audi-");
            case "VOLKSWAGEN" -> containsAny(urlLower, "skoda-", "audi-", "bmw-", "mercedes-", "dacia-", "seat-", "cupra-");
            default -> false;
        };
    }

    private boolean looksDemandListing(String title, String text, String url) {
        String source = " " + normalizeText(title + " " + shortenForCheck(text, 350) + " " + safe(url)).toLowerCase(Locale.ROOT) + " ";

        return containsAny(source,
                " hledám ", " hledam ",
                " poptávám ", " poptavam ",
                " koupím ", " koupim ",
                " sháním ", " shanim ",
                " nabídněte ", " nabidnete ");
    }

    private boolean looksCommercialVehicle(String title, String text, String url) {
        String source = " " + normalizeText(title + " " + shortenForCheck(text, 400) + " " + safe(url))
                .toLowerCase(Locale.ROOT) + " ";

        if (looksClearlyCommercialBody(source)) {
            return true;
        }

        if (looksLikePassengerCarModel(title)) {
            return false;
        }

        String detectedBrand = extractBrand(title, text);
        String detectedType = extractCarType(title, text, url);

        if (detectedBrand != null && detectedType != null) {
            return false;
        }

        return containsAny(source,
                " sprinter ", " vito ", " viano ",
                " transporter ", " caravelle ", " multivan ",
                " trafic ", " traffic ", " vivaro ", " primastar ",
                " expert ", " jumpy ", " scudo ", " proace ",
                " tourneo custom ", " transit custom ",
                " iveco ", " daily ", " boxer ", " ducato ", " jumper ",
                " master ", " movano ", " crafter ", " transit ",
                " dodávka ", " dodavka ", " užitkové ", " uzitkove ",
                " nákladní ", " nakladni ", " autobus ", " mikrobus ",
                " valník ", " valnik ", " sklápěč ", " sklapec ", " sklápěcí ", " sklapeci ",
                " tahač ", " tahac ", " návěs ", " naves ",
                " hákový nosič ", " hakovy nosic ",
                " podvozek ", " plachta ", " skříň ", " skrin ",
                " pracovní stroj ", " pracovni stroj ",
                " actros ", " axor ", " atego ", " scania ", " daf ",
                " volvo fe ", " volvo fl ", " volvo fmx ",
                " man valník ", " man valnik ",
                " lowdeck ", " 5t ", " 5000 kg ",
                " karavan ", " caravan ",
                " obytný vůz ", " obytny vuz ",
                " obytný ", " obytny ", " obytné ", " obytne ",
                " adria ",
                " laika ",
                " matrix ",
                " kosmo ",
                " obytný automobil ",
                " obytny automobil ",
                " přívěs ", " prives ",
                " předstan ", " predstan ",
                " mover ", " markýza ", " markyza ",
                " nosič kol ", " nosic kol ",
                " chausson ", " bailey ", " beyerland ",
                " hobby ", " hobby de luxe ", " knaus ",
                " swift 390 ", " toscane "
        );
    }

    private boolean looksClearlyCommercialBody(String source) {
        return containsAny(source,
                " kasten ", " cargo ", " furgon ", " skrinovy ", " skříňový ",
                " l1h1 ", " l1 h1 ", " l2h1 ", " l2 h1 ", " l2h2 ", " l2 h2 ",
                " l3h2 ", " l3 h2 ", " l3h3 ", " l3 h3 ", " l4h2 ", " l4 h2 ",
                " maxi dodavka ", " dodavka ", " dodávka ",
                " 2 mista ", " 2 místa ", " 3 mista ", " 3 místa ",
                " 2-mist", " 3-mist", " 2 mistne ", " 3 mistne ",
                " pickup pro praci ", " pracovní verze ", " pracovni verze ");
    }

    private boolean looksTyreOrWheelListing(String title, String text, String analysisText) {
        String titleSource = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String source = " " + normalizeText(title + " " + text + " " + shortenForCheck(analysisText, 700))
                .toLowerCase(Locale.ROOT) + " ";

        if (looksLikeRealCar(title, analysisText)
                || extractBrand(title, analysisText) != null) {
            return false;
        }

        if (containsAny(titleSource,
                " pneu ",
                " pneumatiky ",
                " alu kola ",
                " sada kol ",
                " sada pneu ",
                " disky ",
                " ráfky ",
                " rafky ",
                " letní kola ",
                " letni kola ",
                " zimní kola ",
                " zimni kola ",
                " rezervní kolo ",
                " rezervni kolo ")) {
            return true;
        }

        if (startsWithAny(titleSource,
                "pneu ",
                "pneumatiky ",
                "alu kola ",
                "disky ",
                "sada kol ",
                "sada pneu ",
                "kola ",
                "ráfky ",
                "rafky ")) {
            return true;
        }

        boolean hasTyreSize =
                TYRE_SIZE_PATTERN.matcher(source).find()
                        || TYRE_SIZE_ALT_PATTERN.matcher(source).find();

        boolean hasWheelWords = containsAny(source,
                " pneu ",
                " pneumatiky ",
                " sada pneu ",
                " sada pneumatik ",
                " alu kola ",
                " disky ",
                " ráfky ",
                " rafky ",
                " letní pneu ",
                " letni pneu ",
                " zimní pneu ",
                " zimni pneu ");

        if (hasTyreSize && hasWheelWords) {
            return true;
        }

        boolean hasRimSpec = RIM_SPEC_PATTERN.matcher(source).find();
        if (hasRimSpec && hasWheelWords) {
            return true;
        }

        for (String brand : TYRE_BRANDS) {
            if (source.contains(" " + brand + " ") && hasWheelWords) {
                return true;
            }
        }

        return false;
    }

    private boolean containsNonCarBrand(String title, String text) {
        String source = " " + normalizeText(title + " " + text).toLowerCase(Locale.ROOT) + " ";

        return containsAny(source,
                " hankook ", " michelin ", " continental ", " goodyear ", " bridgestone ",
                " pirelli ", " dunlop ", " barum ", " nokian ", " firestone ",
                " thule ", " bosch ", " valeo ", " hella ", " castrol ", " shell ");
    }

    private boolean looksNonCarListing(String title, String text, String url, String analysisText) {
        if (looksLikeRealCar(title, analysisText)
                || extractBrand(title, analysisText) != null) {
            return false;
        }

        if (looksTyreOrWheelListing(title, text, analysisText)) {
            return true;
        }

        String titleValue = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String source = " " + normalizeText(shortenForCheck(text, 450) + " " + safe(url)).toLowerCase(Locale.ROOT) + " ";
        String analysis = " " + shortenForCheck(normalizeText(analysisText).toLowerCase(Locale.ROOT), 900) + " ";

        if (containsAny(titleValue,
                " střešní nosič ", " stresni nosic ",
                " nosič ", " nosic ",
                " thule ",
                " rakev ",
                " box na střechu ", " box na strechu ",
                " střešní box ", " stresni box ",
                " příčníky ", " pricniky ",
                " hagusy ",
                " držák kol ", " drzak kol ",
                " tažné zařízení ", " tazne zarizeni ",
                " koberec ", " koberečky ", " koberecky ",
                " autokoberce ", " gumové koberce ", " gumove koberce ",
                " vana do kufru ",
                " autobaterie ", " baterie ",
                " autorádio ", " autoradio ",
                " rádio ", " radio ",
                " reproduktory ",
                " kamera do auta ",
                " navigace ",
                " disky ", " disky alu ", " alu kola ", " kola ",
                " pneumatiky ", " pneu ", " gumy ",
                " blatník ", " blatnik ",
                " nárazník ", " naraznik ",
                " kapota ",
                " dveře ", " dvere ",
                " světla ", " svetla ",
                " zrcátko ", " zrcatko ",
                " převodovka ", " prevodovka ",
                " prodám motor ",
                " prodam motor ",
                " motor na prodej ",
                " motor z auta ",
                " motor z vozu ",
                " blok motoru ",
                " hlava motoru ",
                " dvouhmota ",
                " setrvačník ",
                " setrvacnik ",
                " turbo ",
                " vstřiky ", " vstriky ",
                " čerpadlo ", " cerpadlo ",
                " filtr pevných částic ", " filtr pevných castic ", " dpf ",
                " spojka ",
                " katalyzátor ", " katalyzator ",
                " křídlo ", " kridlo ",
                " karbon ",
                " spoiler ")) {
            return true;
        }

        if (startsWithAny(titleValue,
                "prodám motor ",
                "prodam motor ",
                "motor na prodej ",
                "motor z auta ",
                "motor z vozu ",
                "motory ",
                "převodovka ",
                "prevodovka ",
                "turbo ",
                "vstřiky ",
                "vstriky ",
                "čerpadlo ",
                "cerpadlo ",
                "blok motoru ",
                "hlava motoru ",
                " blatník ",
                " blatnik ",
                " nárazník ",
                " naraznik ",
                " kapota ",
                " dveře ",
                " dvere ",
                " světla ",
                " svetla ",
                " reproduktory ",
                " pneu ",
                " kola ",
                " alu kola ",
                " disky ",
                " ráfky ",
                " rafky ",
                " střešní nosič ",
                " stresni nosic ",
                " nosič ",
                " nosic ",
                " thule ",
                " rakev ",
                " box na střechu ",
                " box na strechu ",
                " střešní box ",
                " stresni box ",
                " příčníky ",
                " pricniky ",
                " hagusy ")) {
            return true;
        }

        boolean sourceHasStrongPartWord = containsAny(source,
                " náhradní díly ", " nahradni dily ",
                " náhradní díl ", " nahradni dil ",
                " příslušenství ", " prislusenstvi ",
                " doplňky ", " doplnky ",
                " střešní nosič ", " stresni nosic ",
                " nosič ", " nosic ",
                " thule ",
                " rakev ",
                " box na střechu ", " box na strechu ",
                " střešní box ", " stresni box ",
                " příčníky ", " pricniky ",
                " hagusy ",
                " pneu ", " pneumatiky ", " gumy ",
                " alu kola ", " alu-kola ", " elektrony ",
                " kola ", " disky ", " ráfky ", " rafky ",
                " blatník ", " blatníky ", " blatnik ", " blatniky ",
                " nárazník ", " nárazníky ", " naraznik ", " narazniky ",
                " kapota ", " světla ", " svetla ",
                " brzdové kotouče ", " brzdove kotouce ",
                " brzdové destičky ", " brzdove desticky ",
                " prodám motor ", " prodam motor ",
                " motor na prodej ",
                " motor z auta ", " motor z vozu ",
                " blok motoru ", " hlava motoru ",
                " motory ",
                " převodovka ", " prevodovka ",
                " turbo ",
                " dvouhmota ",
                " setrvačník ",
                " setrvacnik ",
                " vstřiky ", " vstriky ",
                " čerpadlo ", " cerpadlo ",
                " spojka ",
                " katalyzátor ", " katalyzator ");

        boolean rentalOrService = containsAny(source,
                " pronájem ", " pronajem ",
                " půjčení ", " pujceni ",
                " zapůjčení ", " zapujceni ",
                " bolt ", " uber ",
                " kurýr ", " kuryr ");

        boolean looksLikeRealCar = looksLikeRealCar(title, analysis);

        if (sourceHasStrongPartWord && !looksLikeRealCar) {
            return true;
        }

        return rentalOrService;
    }

    private boolean looksLikeRealCar(String title, String text) {
        String source = " " + normalizeText(title + " " + shortenForCheck(text, 500)).toLowerCase(Locale.ROOT) + " ";

        int score = 0;

        if (extractBrand(title, text) != null) score += 2;
        if (extractYear(title, text) != null) score += 1;
        if (extractMileage(title, text) != null) score += 1;
        if (extractFuelType(title) != null || extractFuelType(text) != null) score += 1;
        if (extractTransmission(title) != null || extractTransmission(text) != null) score += 1;
        if (extractCarType(title, text) != null) score += 1;

        if (containsAny(source,
                " combi ", " kombi ", " wagon ", " avant ", " variant ", " touring ",
                " caravan ", " grandtour ", " estate ", " hatchback ", " liftback ",
                " sedan ", " suv ", " crossover ", " mpv ", " minivan ")) {
            score += 1;
        }

        if (containsAny(source, " tdi ", " tsi ", " hdi ", " dci ", " cdi ", " crdi ", " 4x4 ", " dsg ")) {
            score += 1;
        }

        if (containsAny(source,
                " kola ", " disky ", " pneu ", " gumy ", " náhradní díly ", " nahradni dily ",
                " blatník ", " blatnik ", " nárazník ", " naraznik ",
                " převodovka ", " prevodovka ",
                " dvouhmota ", " setrvačník ", " setrvacnik ",
                " střešní nosič ", " stresni nosic ", " thule ", " rakev ", " box na střechu ", " box na strechu ")) {
            score -= 3;
        }

        return score >= 4;
    }

    private boolean looksBrokenOrForPartsListing(String title, String text) {
        String source = " " + normalizeText(title + " " + shortenForCheck(text, 500)).toLowerCase(Locale.ROOT) + " ";

        boolean explicitlyDriveable = containsAny(source,
                " plně pojízdný ", " plne pojizdny ",
                " plně pojízdné ", " plne pojizdne ",
                " plně pojízdná ", " plne pojizdna ",
                " pojízdný ", " pojizdny ",
                " pojízdné ", " pojizdne ",
                " pojízdná ", " pojizdna ");

        boolean severeBroken = containsAny(source,
                " na díly ", " na dily ", " na nd ",
                " náhradní díly ", " nahradni dily ",
                " rozprodej na díly ", " rozprodej na dily ",
                " rozprodám ", " rozprodam ",
                " vada motoru ", " závada motoru ", " zavada motoru ",
                " zadřený motor ", " zadreny motor ",
                " nepojízdný ", " nepojizdny ",
                " nepojízdné ", " nepojizdne ",
                " nepojízdná ", " nepojizdna ");

        if (severeBroken) {
            return true;
        }

        if (explicitlyDriveable) {
            return false;
        }

        boolean moderateBroken = containsAny(source,
                " na opravu ",
                " havarovaný ", " havarovany ",
                " havarované ", " havarovane ",
                " havarovaná ", " havarovana ",
                " po bouračce ", " po bouracce ",
                " bouraný ", " bourany ",
                " bourané ", " bourane ",
                " bouraná ", " bourana ",
                " vadný ", " vadne ",
                " vadná ", " vadna ",
                " vadné ",
                " nefunkční ", " nefunkcni ",
                " nefunkční motor ", " nefunkcni motor ",
                " špatný motor ", " spatny motor ",
                " špatná převodovka ", " spatna prevodovka ",
                " špatné turbo ", " spatne turbo ",
                " špatná spojka ", " spatna spojka ",
                " bez baterie ",
                " bez klíčů ", " bez klicu ");

        return moderateBroken;
    }

    private boolean looksSuspiciousListing(String title, String text) {
        String cleanTitle = normalizeText(title).toLowerCase(Locale.ROOT);

        if (cleanTitle.equals("prodam")
                || cleanTitle.equals("prodám")
                || cleanTitle.equals("prodej")) {
            return true;
        }

        String source = " " + normalizeText(title + " " + shortenForCheck(text, 500)).toLowerCase(Locale.ROOT) + " ";

        return containsAny(source,
                " na splátky ", " na splatky ",
                " bez registru ",
                " akontace ",
                " 48 x ", " 60 x ",
                " exekuce ",
                " insolvence ",
                " dražba ", " drazba ",
                " přenechám splátky ", " prenecham splatky ",
                " převezmu leasing ", " prevezmu leasing ",
                " leasing převezmu ", " leasing prevezmu ",
                " bez přepisu ", " bez prepisu ",
                " bez stk ",
                " bez tp ",
                " soubor náhradních dílů ", " soubor nahradnich dilu ",
                " jen celek ",
                " prodáno ", " prodano ",
                " zálohováno ", " zalohovano ",
                " zadáno ", " zadano ");
    }

    private Integer parseYearCandidate(String raw) {
        try {
            int year = Integer.parseInt(raw);
            int currentYear = java.time.Year.now().getValue();

            if (year >= 1990 && year <= currentYear) {
                return year;
            }
        } catch (NumberFormatException ignored) {
        }

        return null;
    }

    private Integer parseMileageCandidate(String raw) {
        Integer value = parseNumber(raw);
        if (value == null) {
            return null;
        }

        if (value >= 1000 && value <= 1_500_000) {
            return value;
        }

        return null;
    }

    private Integer parseNumber(String raw) {
        if (raw == null) {
            return null;
        }

        String cleaned = raw.replaceAll("[^0-9]", "");
        if (cleaned.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatPrice(Integer priceValue) {
        if (priceValue == null) {
            return "";
        }
        return String.format(Locale.US, "%,d Kč", priceValue).replace(",", " ");
    }

    private boolean isRealLocation(String value) {
        if (value == null) {
            return false;
        }

        String normalized = normalizeText(value);
        if (normalized.isBlank()) {
            return false;
        }

        String lower = normalized.toLowerCase(Locale.ROOT);

        if (lower.matches("\\d+")) {
            return false;
        }

        if (lower.length() < 3) {
            return false;
        }

        return !lower.equals("lokalita")
                && !lower.equals("lokalita:")
                && !lower.equals("okres")
                && !lower.equals("město")
                && !lower.equals("mesto")
                && !lower.equals("okolí")
                && !lower.equals("okoli");
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

    private boolean startsWithAny(String source, String... values) {
        if (source == null || source.isBlank()) {
            return false;
        }

        String lowerSource = source.toLowerCase(Locale.ROOT).trim();

        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }

            String lowerValue = value.toLowerCase(Locale.ROOT).trim();

            if (lowerSource.startsWith(lowerValue)) {
                return true;
            }
        }

        return false;
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

    @Override
    protected String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String shortenForCheck(String value, int maxLen) {
        String normalized = normalizeText(value);
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen);
    }

    private boolean looksLikePassengerCarModel(String title) {
        String source = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";

        return containsAny(source,
                " rav4 ",
                " rav 4 ",
                " land cruiser ",
                " x1 ",
                " x3 ",
                " x5 ",
                " discovery sport ",
                " range rover ",
                " evoque ",
                " cherokee ",
                " grand cherokee ",
                " forester ",
                " outback ",
                " xv ",
                " mokka ",
                " tarraco ",
                " sorento ",
                " sportage ",
                " tucson ",
                " santa fe ",
                " kona ",
                " duster ",
                " tiguan ",
                " touareg ",
                " kodiaq ",
                " karoq ",
                " rav ",
                " qashqai ",
                " cx-5 ",
                " cx5 "
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private record ParseResult(CarDto car, String reason) {
        static ParseResult ok(CarDto car) {
            return new ParseResult(car, null);
        }

        static ParseResult skip(String reason) {
            return new ParseResult(null, reason);
        }
    }

    private String cleanLocation(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = normalizeText(value);

        cleaned = cleaned.replaceAll("(?i)\\b(tel|telefon|volejte|volat)\\b.*$", "").trim();
        cleaned = cleaned.replaceAll("\\+?\\d[\\d\\s]{6,}.*$", "").trim();

        // remove Czech/Slovak postal code at the end: "Kolín 280 02" -> "Kolín"
        cleaned = cleaned.replaceAll("\\s+\\d{3}\\s?\\d{2}$", "").trim();

        cleaned = cleaned.replaceAll("^\\d{3}\\s?\\d{2}\\s+", "").trim();
        cleaned = cleaned.replaceAll("(?i)^okoli\\s+", "").trim();
        cleaned = cleaned.replaceAll("(?i)^okolí\\s+", "").trim();
        cleaned = cleaned.replaceAll("(?i)^i\\s+prahy$", "Praha").trim();
        cleaned = cleaned.replaceAll("[,;\\-]+$", "").trim();

        return cleaned.isBlank() ? null : cleaned;
    }
}

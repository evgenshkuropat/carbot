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
        int missingPriceCount = 0;
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
                            case "missing_price" -> missingPriceCount++;
                            case "parse_error" -> parseErrorCount++;
                        }
                    }

                } catch (Exception e) {
                    parseErrorCount++;
                    log.warn("BAZOS SKIP url={} reason=parse_error", safe(url));
                }

                sleepBetweenDetailRequests();

                count++;
            }

        } catch (Exception e) {
            log.warn("BAZOS parser failed: {}", e.getMessage());
        }

        log.info("BAZOS parsed {} cars", cars.size());
        log.info(
                "BAZOS SUMMARY parsed={} empty_title={} demand_listing={} commercial_vehicle={} non_car_listing={} broken_or_for_parts={} suspicious_listing={} invalid_price={} missing_price={} parse_error={}",
                cars.size(),
                emptyTitleCount,
                demandListingCount,
                commercialVehicleCount,
                nonCarListingCount,
                brokenOrForPartsCount,
                suspiciousListingCount,
                invalidPriceCount,
                missingPriceCount,
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
                title = titleFromUrl(url);
            }

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

            if (extractBrand(title, analysisText) == null
                    && containsNonCarBrand(title, listingText)
                    && !looksLikeRealCar(title, analysisText)) {
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

            if (titleUrlMismatch || looksModelUrlMismatch(title, url)) {
                log.info("BAZOS SKIP url={} reason=model_url_mismatch title={}", safe(url), safe(title));
                return ParseResult.skip("non_car_listing");
            }

            Integer priceValue = extractPrice(doc, priceText);
            if (priceValue == null) {
                log.info("BAZOS SKIP url={} reason=missing_price title={}", safe(url), safe(title));
                return ParseResult.skip("missing_price");
            }
            if (!isValidBazosPrice(priceValue)) {
                log.info("BAZOS SKIP url={} reason=invalid_price title={}", safe(url), safe(title));
                return ParseResult.skip("invalid_price");
            }

            String price = formatPrice(priceValue);
            String location = extractLocation(doc, analysisText);
            Integer year = extractYear(title, analysisText);

            if (year == null) {
                year = extractYear("", preview);
            }
            Integer mileage = extractMileage(title, analysisText);
            String fuelType = firstNonBlank(
                    extractFuelType(title),
                    extractFuelType(listingText)
            );
            String transmission = firstNonBlank(
                    extractTransmission(title),
                    extractTransmission(listingText),
                    "ELECTRIC".equals(fuelType) ? "AUTOMATIC" : null
            );
            if (looksLikelyFalseAutomatic(title, transmission)) {
                transmission = null;
            }
            String brand = extractBrand(title, analysisText);
            String carType = extractCarType(title, listingText, url);
            String imageUrl = extractImageUrl(doc);

            if (brand == null && carType == null && !looksLikeRealCar(title, analysisText)) {
                log.info("BAZOS SKIP url={} reason=not_enough_car_signals title={}",
                        safe(url), safe(title));
                return ParseResult.skip("non_car_listing");
            }

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

        String source = " " + normalizeText(title + " " + shortenForCheck(text, 500)).toLowerCase(Locale.ROOT) + " ";

        if (year != null
                && year >= 2020
                && priceValue < 500_000
                && containsAny(source, " land cruiser ", " landcruiser ", " lc300 ", " lc 300 ")) {
            return true;
        }

        if (year != null && year >= 2015 && priceValue < 80_000) {
            return true;
        }

        if (year != null && year >= 2020 && priceValue < 100_000) {
            return true;
        }

        if (priceValue < 40_000 && mileage != null && mileage >= 300_000) {
            return true;
        }

        if (priceValue < 100_000 && mileage != null && mileage >= 320_000) {
            return true;
        }

        return false;
    }

    private String extractTitle(Document doc) {
        Element h1 = doc.selectFirst("h1.nadpisdetail, h1");
        if (h1 != null && !normalizeText(h1.text()).isBlank()) {
            return normalizeText(h1.text());
        }

        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null && !normalizeText(ogTitle.attr("content")).isBlank()) {
            return cleanBazosTitle(ogTitle.attr("content"));
        }

        if (!normalizeText(doc.title()).isBlank()) {
            return cleanBazosTitle(doc.title());
        }

        return "";
    }

    private String cleanBazosTitle(String raw) {
        String title = normalizeText(raw);

        title = title.replaceFirst("(?i)\\s*-\\s*Bazoš\\.cz\\s*$", "");
        title = title.replaceFirst("(?i)\\s*\\|\\s*Bazoš\\.cz\\s*$", "");
        title = title.replaceFirst("(?i)\\s*-\\s*auto\\s*$", "");

        int separator = title.lastIndexOf(" - ");
        if (separator > 5) {
            title = title.substring(0, separator).trim();
        }

        return title.trim();
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

        // Avoid global .inzeratycena on detail pages: it can belong to recommended listings,
        // which causes unrelated cars to inherit the same price.
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
        String locationFromMeta = extractLocationFromMeta(doc);
        if (isRealLocation(locationFromMeta)) {
            return cleanLocation(locationFromMeta);
        }

        String locationFromTitle = extractLocationFromTitle(doc);
        if (isRealLocation(locationFromTitle)) {
            return cleanLocation(locationFromTitle);
        }

        String detailLocation = extractLocationFromDetailTable(doc);
        if (isRealLocation(detailLocation)) {
            return cleanLocation(detailLocation);
        }

        String locationFromLink = extractLocationFromDetailLink(doc);
        if (isRealLocation(locationFromLink)) {
            return cleanLocation(locationFromLink);
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

            Element locationLink = row.selectFirst("a[href*='/inzeraty/']");
            if (locationLink != null) {
                String linkedLocation = normalizeText(locationLink.text());
                if (isRealLocation(linkedLocation)) {
                    return linkedLocation;
                }
            }

            String raw = normalizeText(row.text())
                    .replaceFirst("(?i)^lokalita\\s*:?\\s*", "")
                    .replaceFirst("(?i)^mapa\\s*", "")
                    .replaceAll("\\b\\d{3}\\s?\\d{2}\\b", "")
                    .trim();

            return raw;
        }

        return null;
    }

    private Integer extractYear(String title, String text) {
        String source = normalizeText(title + " " + text);
        String normalizedTitle = normalizeText(title);
        Matcher explicitMatcher = Pattern.compile(
                "(?i)(?:vyrobeno|v provozu od)\\s*[:\\-]?\\s*(?:\\d{1,2}\\s*/\\s*)?(19\\d{2}|20\\d{2})"
        ).matcher(source);

        if (explicitMatcher.find()) {
            Integer year = parseYearCandidate(explicitMatcher.group(1));
            if (year != null && !isBadYearContext(source, explicitMatcher.start(), explicitMatcher.end())) {
                return year;
            }
        }

        if (containsAny(source, " PRODANO ", " PRODÁNO ", " ZADANO ", " ZADÁNO ")) {
            source = normalizeText(title);
        }

        Matcher matcher = Pattern.compile(
                "(?i)(?:rok výroby|rok vyroby|r\\.v\\.?|r\\.|rv|první registrace|prvni registrace|do provozu|uvedení do provozu|uvedeni do provozu)\\s*[:\\-]?\\s*(?:\\d{1,2}\\s*/\\s*)?(19\\d{2}|20\\d{2})"
        ).matcher(source);

        if (matcher.find()) {
            Integer year = parseYearCandidate(matcher.group(1));
            if (year != null && !isBadYearContext(source, matcher.start(), matcher.end())) {
                return year;
            }
        }

        matcher = Pattern.compile("(?i)\\bm\\s*(20\\d{2})\\b").matcher(normalizedTitle);
        while (matcher.find()) {
            Integer year = parseYearCandidate(matcher.group(1));
            if (year != null && !isBadYearContext(normalizedTitle, matcher.start(), matcher.end())) {
                return year;
            }
        }

        matcher = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(normalizedTitle);
        while (matcher.find()) {
            String rawYear = matcher.group(1);

            String normalizedLower = normalizedTitle.toLowerCase(Locale.ROOT);

            if (
                    ("2008".equals(rawYear) && normalizedLower.contains("peugeot 2008")) ||
                            ("3008".equals(rawYear) && normalizedLower.contains("peugeot 3008")) ||
                            ("5008".equals(rawYear) && normalizedLower.contains("peugeot 5008"))
            ) {
                continue;
            }

            Integer year = parseYearCandidate(rawYear);

            if (year != null && !isBadYearContext(normalizedTitle, matcher.start(), matcher.end())) {
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

        matcher = Pattern.compile("(?i)\\b([0-9]{1,3})\\s*(?:tis(?:\\.|ic|Ă­c|\\s)|t\\s*km|tkm)\\b").matcher(source);
        while (matcher.find()) {
            Integer value = parseMileageCandidate(matcher.group(1) + "000");
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private String extractFuelType(String text) {
        String source = " " + normalizeText(text).toLowerCase(Locale.ROOT) + " ";
        String compact = source.replaceAll("[^a-z0-9]", "");

        if (source.contains(" stelvio ")
                && (containsAny(source, " 2.0t ", " 2,0t ", " 2.0 t ", " 2,0 t ", " 2.0 turbo ", " 2,0 turbo ", " turbo ")
                || compact.contains("20tq4")
                || compact.contains("20turbo"))) {
            return "PETROL";
        }

        if (source.contains(" carens ") && containsAny(source, " 1.7 ", " 1,7 ", " 17crdi ", " crdi ")) {
            return "DIESEL";
        }

        if (containsAny(source, " civic type r ", " vtec ", " v-tec ", " i-vtec ", " ivtec ")
                && !containsAny(source, " dtec ", " d-tec ", " i-dtec ", " idtec ", " ctdi ", " ctdi ")) {
            return "PETROL";
        }

        if (source.contains(" orlando ") && containsAny(source, " 2.0 ", " 2,0 ", " 20vcdi ", " vcdi ", " cdti ")) {
            return "DIESEL";
        }

        if (source.contains(" mazda ") && containsAny(source, " 2.2 cd ", " 2,2 cd ", " 22cd ")) {
            return "DIESEL";
        }

        if (containsAny(source, " camaro ", " corvette ", " spark ", " aveo ", " kalos ")
                && containsAny(source, " v8 ", " v6 ", " ls3 ", " 6.2 ", " 6,2 ", " 5.7 ", " 5,7 ", " 3.8 ", " 3,8 ", " 3.6 ", " 3,6 ", " 1.2 ", " 1,2 ", " 1.4 ", " 1,4 ")) {
            return "PETROL";
        }

        if (Pattern.compile("\\b\\d{2,3}\\s*kwh\\b", Pattern.CASE_INSENSITIVE)
                .matcher(source).find()
                || compact.contains("64kwh")
                || compact.contains("62kwh")
                || compact.contains("electric")
                || compact.contains("elektro")) {
            return "ELECTRIC";
        }

        if (containsAny(source,
                " palivo: elektro ",
                " palivo elektro ",
                " elektromobil ",
                " elektroauto ",
                " electric vehicle ",
                " electric ",
                " ev ",
                " bev ",
                " bz4x ",
                " bz 4x ",
                " enyaq ", " id.3 ", " id.4 ", " id.5 ", " tesla ", " leaf ",
                " kona electric ", " kona elektric ",
                " ioniq ", " ioniq 5 ", " ioniq 6 ",
                " e-tron ", " etron ")) {
            return "ELECTRIC";
        }

        if (containsAny(source,
                " hybrid ",
                " hybridní ",
                " hybridni ",
                " hybryd ",
                " hybrydni ",
                " plug-in hybrid ",
                " plugin hybrid ",
                " plug in ",
                " plug-in ",
                " hev ",
                " phev ",
                " tfsi e ",
                " tsi e ",
                " t8 ",
                " recharge ",
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

        if (containsAny(source, " lpg ") || compact.contains("lpg")) {
            return "LPG";
        }

        if (containsAny(source, " cng ")) {
            return "CNG";
        }

        if (containsAny(source,
                " giulia qv ",
                " quadrifoglio ",
                " rs6 ",
                " rs 6 ",
                " m4 competition ",
                " 2.9 v6 ",
                " 29 v6 ")) {
            return "PETROL";
        }

        if (source.contains(" stelvio ")
                && containsAny(source, " 2.2 ", " 22jtd ", " 2,2 ")) {
            return "DIESEL";
        }

        if ((source.contains(" volvo ")
                || containsAny(source, " xc40 ", " xc60 ", " xc70 ", " xc90 ", " v40 ", " v60 ", " v70 ", " v90 "))
                && containsAny(source, " d3 ", " d4 ", " d5 ")) {
            return "DIESEL";
        }

        if (compact.contains("tdi")
                || compact.contains("tdci")
                || compact.contains("cdi")
                || compact.contains("dci")
                || compact.contains("hdi")
                || compact.contains("crdi")
                || compact.contains("dtec")
                || compact.contains("idtec")
                || compact.contains("ictdi")
                || compact.contains("jtd")
                || compact.contains("multijet")
                || compact.contains("bluehdi")
                || compact.contains("cdti")
                || compact.contains("d4d")
                || compact.contains("did")
                || compact.matches(".*\\dtd.*")
                || compact.contains("180d")
                || compact.contains("200d")
                || compact.contains("20d")
                || compact.contains("23d")
                || compact.contains("25d")
                || compact.contains("116d")
                || compact.contains("118d")
                || compact.contains("120d")
                || compact.contains("218d")
                || compact.contains("220d")
                || compact.contains("250d")
                || compact.contains("318d")
                || compact.contains("320d")
                || compact.contains("30d")
                || compact.contains("330d")
                || compact.contains("32d")
                || compact.contains("350d")
                || compact.contains("400d")
                || compact.contains("420d")
                || compact.contains("520d")
                || compact.contains("525d")
                || compact.contains("530d")
                || compact.contains("540d")
                || compact.contains("550d")
                || compact.contains("xdrive30d")
                || compact.contains("m550d")
                || compact.contains("sdv6")
                || compact.contains("13d")
                || compact.contains("16d")){
            return "DIESEL";
        }

        if (compact.contains("tsi")
                || compact.contains("tfsi")
                || compact.contains("jts")
                || compact.contains("vr6")
                || compact.contains("mpi")
                || compact.contains("gdi")
                || compact.contains("tgdi")
                || compact.contains("digt")
                || compact.contains("igt")
                || compact.contains("mivec")
                || compact.contains("tce")
                || compact.contains("tjet")
                || compact.contains("puretech")
                || compact.contains("ecoboost")
                || compact.contains("skyactivg")
                || compact.contains("ivtec")
                || compact.contains("vtec")
                || compact.contains("benzinovy")
                || compact.contains("benzinove")
                || compact.contains("vvti")
                || compact.contains("vvt")
                || compact.contains("10i")
                || compact.contains("12i")
                || compact.contains("14i")
                || compact.contains("15i")
                || compact.contains("16i")
                || compact.contains("16v")
                || compact.contains("18i")
                || compact.contains("20i")
                || compact.contains("24i")
                || compact.contains("25i")
                || compact.contains("28i")
                || compact.contains("30i")
                || compact.contains("32i")
                || compact.contains("35i")
                || compact.contains("40i")
                || compact.contains("50i")
                || compact.contains("318ci")
                || compact.contains("320ci")
                || compact.contains("325ci")
                || compact.contains("330ci")
                || compact.contains("448v")
                || compact.contains("44v8")
                || (compact.contains("v6") && !compact.contains("tdi"))) {
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
                " 4.4 ",
                " 4,4 ",
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

    private boolean looksLikelyFalseAutomatic(String title, String transmission) {
        if (!"AUTOMATIC".equals(transmission)) {
            return false;
        }

        String source = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        return containsAny(source, " twin spark ")
                && !containsAny(source, " selespeed ", " automat ", " automaticka ", " automatickĂˇ ", " automatic ", " aut. ", " a/t ", " at6 ", " at8 ", " at/8 ", " dsg ", " cvt ");
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
                " at/8 ",
                " eat8 ",
                " e-eat8 ",
                " cvt ",
                " e-cvt ",
                " ecvt ",
                " dsg ",
                " dct ",
                " 7dct ",
                " 8g-dct ",
                " 8g dct ",
                " e-dcs ",
                " edcs ",
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
        String compactTitleSource = compactSearchText(title);
        String source = titleSource + " " + shortenForCheck(normalizeText(text).toLowerCase(Locale.ROOT), 300);

        if (containsAny(titleSource,
                " alfa romeo ", " alfa ", " alfu ", " romeo ",
                " stelvio ", " giulia ", " giulietta ", " mito ", " alfetta ")) return "ALFA_ROMEO";
        if (containsAny(titleSource, " škoda ", " skoda ")) return "SKODA";
        if (containsAny(titleSource, " volkswagen ", " vw ")) return "VOLKSWAGEN";
        if (containsAny(titleSource, " audi ")) return "AUDI";
        if (containsAny(titleSource, " bmw ")) return "BMW";
        if (containsAny(titleSource, " mercedes ", " mercedes-benz ")) return "MERCEDES";
        if (containsAny(titleSource, " lexus ")) return "LEXUS";
        if (containsAny(titleSource, " toyota ")) return "TOYOTA";
        if (containsAny(titleSource, " ford ")) return "FORD";
        if (containsAny(titleSource, " renault ")) return "RENAULT";
        if (containsAny(titleSource, " seat ")) return "SEAT";
        if (containsAny(titleSource, " peugeot ")) return "PEUGEOT";
        if (containsAny(titleSource, " opel ")) return "OPEL";
        if (containsAny(titleSource, " hyundai ")) return "HYUNDAI";
        if (containsAny(titleSource, " kia ")) return "KIA";
        if (containsAny(titleSource, " volvo ")) return "VOLVO";
        if (containsAny(titleSource, " mazda ")) return "MAZDA";
        if (containsAny(titleSource, " citroën ", " citroen ", " citreon ")) return "CITROEN";
        if (containsAny(titleSource, " fiat ")) return "FIAT";
        if (containsAny(titleSource, " dodge ")) return "DODGE";
        if (containsAny(titleSource, " nissan ")) return "NISSAN";
        if (containsAny(titleSource, " honda ", " acura ")) return "HONDA";
        if (containsAny(titleSource, " suzuki ")) return "SUZUKI";
        if (containsAny(titleSource, " dacia ", " dacie ")) return "DACIA";
        if (containsAny(titleSource, " cupra ")) return "CUPRA";
        if (containsAny(titleSource, " jeep ")) return "JEEP";
        if (containsAny(titleSource, " subaru ")) return "SUBARU";
        if (containsAny(titleSource, " mitsubishi ", " mitsubushi ", " mitshubishi ", " mizshubishi ")) return "MITSUBISHI";
        if (containsAny(titleSource, " porsche ")) return "PORSCHE";
        if (containsAny(titleSource, " mini ")) return "MINI";
        if (containsAny(titleSource, " tesla ")) return "TESLA";
        if (containsAny(titleSource, " chevrolet ")) return "CHEVROLET";
        if (containsAny(titleSource, " land rover ", " range rover ")) return "LAND_ROVER";
        if (containsAny(compactTitleSource, " nissan ")) return "NISSAN";
        if (containsAny(compactTitleSource, " peugeot ")) return "PEUGEOT";
        if (containsAny(compactTitleSource, " renault ")) return "RENAULT";
        if (containsAny(compactTitleSource, " suzuki ")) return "SUZUKI";

        if (containsAny(source,
                " alfa romeo ", " alfa ", " alfu ", " romeo ",
                " stelvio ", " giulia ", " giulietta ", " mito ", " alfetta ")) return "ALFA_ROMEO";
        if (containsAny(source, " škoda ", " skoda ")) return "SKODA";
        if (containsAny(source, " volkswagen ", " vw ")) return "VOLKSWAGEN";
        if (containsAny(source, " audi ")) return "AUDI";
        if (containsAny(source, " bmw ")) return "BMW";
        if (containsAny(source, " mercedes ", " mercedes-benz ")) return "MERCEDES";
        if (containsAny(source, " lexus ")) return "LEXUS";
        if (containsAny(source, " toyota ")) return "TOYOTA";
        if (containsAny(source, " ford ")) return "FORD";
        if (containsAny(source, " renault ")) return "RENAULT";
        if (containsAny(source, " seat ")) return "SEAT";
        if (containsAny(source, " peugeot ")) return "PEUGEOT";
        if (containsAny(source, " opel ")) return "OPEL";
        if (containsAny(source, " hyundai ")) return "HYUNDAI";
        if (containsAny(source, " kia ")) return "KIA";
        if (containsAny(source, " volvo ")) return "VOLVO";
        if (containsAny(source, " mazda ")) return "MAZDA";
        if (containsAny(source, " citroën ", " citroen ", " citreon ")) return "CITROEN";
        if (containsAny(source, " fiat ")) return "FIAT";
        if (containsAny(source, " dodge ")) return "DODGE";
        if (containsAny(source, " nissan ")) return "NISSAN";
        if (containsAny(source, " honda ", " acura ")) return "HONDA";
        if (containsAny(source, " suzuki ")) return "SUZUKI";
        if (containsAny(source, " dacia ", " dacie ")) return "DACIA";
        if (containsAny(source, " cupra ")) return "CUPRA";
        if (containsAny(source, " jeep ")) return "JEEP";
        if (containsAny(source, " subaru ")) return "SUBARU";
        if (containsAny(source, " mitsubishi ", " mitsubushi ", " mitshubishi ", " mizshubishi ")) return "MITSUBISHI";
        if (containsAny(source, " porsche ")) return "PORSCHE";
        if (containsAny(source, " mini ")) return "MINI";
        if (containsAny(source, " tesla ")) return "TESLA";
        if (containsAny(source, " chevrolet ")) return "CHEVROLET";
        if (containsAny(source, " land rover ", " range rover ")) return "LAND_ROVER";

        // fallback model detection
        if (source.contains(" leon ")) return "SEAT";
        if (source.contains(" ibiza ")) return "SEAT";
        if (source.contains(" alhambra ")) return "SEAT";

        if (source.contains(" golf ")) return "VOLKSWAGEN";
        if (source.contains(" passat ")) return "VOLKSWAGEN";
        if (source.contains(" tiguan ")) return "VOLKSWAGEN";
        if (source.contains(" touareg ")) return "VOLKSWAGEN";
        if (source.contains(" sharan ")) return "VOLKSWAGEN";
        if (source.contains(" touran ")) return "VOLKSWAGEN";
        if (source.contains(" caddy ")) return "VOLKSWAGEN";

        if (source.contains(" octavia ") || source.contains(" oktavia ")) return "SKODA";
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
        if (source.contains(" tourneo ")) return "FORD";

        if (source.contains(" rdx ")) return "HONDA";

        if (source.contains(" megane ")) return "RENAULT";
        if (source.contains(" scenic ")) return "RENAULT";
        if (source.contains(" clio ")) return "RENAULT";
        if (source.contains(" thalia ")) return "RENAULT";
        if (source.contains(" kangoo ")) return "RENAULT";
        if (source.contains(" espace ")) return "RENAULT";
        if (source.contains(" koleos ")) return "RENAULT";
        if (source.contains(" kadjar ")) return "RENAULT";
        if (source.contains(" arkana ")) return "RENAULT";
        if (source.contains(" austral ")) return "RENAULT";

        if (source.contains(" berlingo ")) return "CITROEN";
        if (source.contains(" picasso ")) return "CITROEN";
        if (source.contains(" c2 ")) return "CITROEN";
        if (source.contains(" c3 ")) return "CITROEN";
        if (source.contains(" c4 ")) return "CITROEN";
        if (source.contains(" c5 ")) return "CITROEN";
        if (source.contains(" ds5 ")) return "CITROEN";
        if (source.contains(" xsara ")) return "CITROEN";

        if (source.contains(" tipo ")) return "FIAT";
        if (source.contains(" doblo ")) return "FIAT";
        if (source.contains(" 500l ") || source.contains(" 500 l ")) return "FIAT";
        if (source.contains(" panda ")) return "FIAT";
        if (source.contains(" ducato ")) return "FIAT";
        if (source.contains(" fiat 500 ") || source.contains(" 500 lounge ")) return "FIAT";

        if (source.contains(" compass ")) return "JEEP";
        if (source.contains(" cherokee ")) return "JEEP";
        if (source.contains(" renegade ")) return "JEEP";

        if (source.contains(" sportage ")) return "KIA";
        if (source.contains(" ceed ")) return "KIA";
        if (source.contains(" carens ")) return "KIA";

        if (source.contains(" rav4 ")) return "TOYOTA";
        if (source.contains(" land cruiser ")) return "TOYOTA";
        if (source.contains(" landcruiser ")) return "TOYOTA";
        if (source.contains(" prius ")) return "TOYOTA";
        if (source.contains(" corolla ")) return "TOYOTA";
        if (source.contains(" auris ")) return "TOYOTA";
        if (source.contains(" aygo ")) return "TOYOTA";
        if (source.contains(" bz4x ") || source.contains(" bz 4x ")) return "TOYOTA";

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
        if (source.contains(" outlander ")) return "MITSUBISHI";
        if (source.contains(" pajero ")) return "MITSUBISHI";
        if (source.contains(" l200 ") || source.contains(" l 200 ")) return "MITSUBISHI";
        if (source.contains(" lancer ")) return "MITSUBISHI";
        if (source.contains(" eclipse cross ")) return "MITSUBISHI";
        if (source.contains(" asx ")) return "MITSUBISHI";
        if (source.contains(" colt ")) return "MITSUBISHI";
        if (source.contains(" spacestar ") || source.contains(" space star ")) return "MITSUBISHI";
        if (source.contains(" qashqai ")) return "NISSAN";
        if (source.contains(" juke ")) return "NISSAN";
        if (source.contains(" x-trail ")) return "NISSAN";
        if (source.contains(" navara ")) return "NISSAN";
        if (source.contains(" micra ")) return "NISSAN";
        if (source.contains(" leaf ")) return "NISSAN";
        if (source.contains(" primera ")) return "NISSAN";
        if (source.contains(" swift ")) return "SUZUKI";
        if (source.contains(" vitara ")) return "SUZUKI";
        if (source.contains(" sx 4 ") || source.contains(" sx4 ")) return "SUZUKI";
        if (containsAny(source, " civic ", " jazz ", " accord ", " cr-v ", " cr v ", " crv ", " hr-v ", " hr v ", " hrv ", " fr-v ", " fr v ", " frv ")) return "HONDA";

        if (source.contains(" 1007 ")) return "PEUGEOT";
        if (source.contains(" 107 ")) return "PEUGEOT";
        if (source.contains(" 207 ")) return "PEUGEOT";
        if (source.contains(" 208 ")) return "PEUGEOT";
        if (source.contains(" 308 ")) return "PEUGEOT";
        if (source.contains(" 3008 ")) return "PEUGEOT";
        if (source.contains(" 5008 ")) return "PEUGEOT";
        if (source.contains(" rifter ")) return "PEUGEOT";
        if (source.contains(" rcz ")) return "PEUGEOT";
        if (source.contains(" dokker ")) return "DACIA";
        if (source.contains(" mazda 5 ")) return "MAZDA";
        if (source.contains(" favorit ")) return "SKODA";
        if (source.contains(" kangoo ")) return "RENAULT";
        if (source.contains(" tucson ")) return "HYUNDAI";
        if (source.contains(" santa fe ")) return "HYUNDAI";
        if (source.contains(" ix20 ")) return "HYUNDAI";
        if (source.contains(" ix35 ")) return "HYUNDAI";
        if (source.contains(" i20 ")) return "HYUNDAI";
        if (source.contains(" i30 ")) return "HYUNDAI";
        if (source.contains(" i40 ")) return "HYUNDAI";
        if (source.contains(" kona ")) return "HYUNDAI";
        if (source.contains(" bayon ")) return "HYUNDAI";
        if (source.contains(" ioniq ") || source.contains(" ionig ")) return "HYUNDAI";

        if (source.contains(" soul ")) return "KIA";

        return null;
    }

    private String extractCarType(String title, String text) {
        return extractCarType(title, text, null);
    }

    private String extractCarType(String title, String text, String url) {
        String titleSource = " " + normalizeText(safe(title)).toLowerCase(Locale.ROOT) + " ";
        titleSource = titleSource + " " + compactSearchText(safe(title));
        String textSource = " " + normalizeText(safe(text)).toLowerCase(Locale.ROOT) + " ";
        String urlSource = " " + normalizeText(safe(url)).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(titleSource, " corolla sedan ", " corolla sd ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " a3 ", " a5 ", " a7 ") && titleSource.contains(" sportback ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " multivan ", " nv 200 ", " nv200 ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource, " 730d ", " 730ld ", " 730i ", " 740d ", " 740i ", " 750d ", " 750i ", " 7 series ", " rada 7 ", " 7er ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " logan mcv ", " logan combi ", " logan kombi ")) {
            return "WAGON";
        }

        if (containsAny(titleSource, " jogger ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource, " ford ka ", " fordka ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " gran coupe ", " grand coupe ", " gran coup\u00e9 ", " grand coup\u00e9 ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " giulia ", " alfa 156 ", " romeo 156 ", " alfa 159 ", " romeo 159 ",
                " alfa 166 ", " romeo 166 ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " sportwagon ")) {
            return "WAGON";
        }

        if (containsAny(titleSource, " alfa gt ", " romeo gt ", " gtv ", " brera ")) {
            return "COUPE";
        }

        if (containsAny(titleSource, " giulietta ", " mito ", " alfa 145 ", " romeo 145 ", " alfa 146 ", " romeo 146 ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " stelvio ", " e-tron ", " etron ", " sx4 ", " sx 4 ",
                " aircross ",
                " c3 aircross ",
                " c5 aircross ",
                " q4 ",
                " sq8 ",
                " s7 ",
                " rs3 ")) {
            return "SUV";
        }

        if (containsAny(titleSource, " silverado ")) {
            return "PICKUP";
        }

        if (containsAny(titleSource, " aveo sedan ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " camaro ", " corvette ", " corveta ")) {
            if (containsAny(titleSource, " cabrio ", " convertible ", " t-top ", " targa ")) {
                return "CABRIO";
            }
            return "COUPE";
        }

        if (containsAny(titleSource, " ix20 ", " orlando ", " hhr ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource, " i10 ", " jazz ", " rio ", " ds3 ", " punto ", " panda ", " grande punto ", " kalos ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " civic coupe ", " civic coupĂ© ", " crx ")) {
            return "COUPE";
        }

        if (containsAny(titleSource, " accord ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " i30 wg ", " i30 wagon ", " i30 kombi ", " i30 combi ")) {
            return "WAGON";
        }

        if (containsAny(titleSource, " h1 ", " h-1 ", " h 1 ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource, " ioniq 5 ", " ionig 5 ")) {
            return "SUV";
        }

        if (containsAny(titleSource, " ioniq ", " ionig ")) {
            return "HATCHBACK";
        }

        if (compactSearchText(title).contains("ceed")
                && !compactSearchText(title).contains("proceed")
                && !compactSearchText(title).contains("xceed")
                && !containsAny(titleSource, " sw ", " wagon ", " kombi ", " combi ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource,
                " suv ", " crossover ", " tonale ",
                " x1 ", " x2 ", " x3 ", " x4 ", " x5 ", " x6 ", " x7 ",
                " q2 ", " q3 ", " q4 ", " q5 ", " q7 ", " q8 ",
                " sq5 ", " sq7 ",
                " glc ", " gle ", " gls ", " gla ", " glb ",
                " gl320 ", " gl 63 ", " ml 350 ", " ml350 ",
                " kodiaq ", " karoq ", " kamiq ",
                " tiguan ", " touareg ", " t-roc ", " troc ",
                " pajero ", " outlander ", " eclipse cross ", " asx ",
                " qashqai ", " juke ", " x-trail ",
                " land cruiser ", " landcruiser ", " patrol ", " peugeot 2008 ", " 3008 ", " 5008 ",
                " kuga ", " puma ", " ecosport ",
                " formentor ", " ateca ", " arona ", " tarraco ",
                " xc40 ", " xc60 ", " xc70 ", " xc90 ",
                " ex30 ", " ex40 ", " ex90 ",
                " captur ", " austral ", " arkana ", " rafale ",
                " sportage ", " sorento ", " stonic ",
                " rdx ",
                " tucson ", " santa fe ", " kona ",
                " duster ", " koleos ", " kadjar ",
                " cr-v ", " cr v ", " crv ", " hr-v ", " hr v ", " hrv ", " rav4 ", " c-hr ", " chr ",
                " cx-3 ", " cx3 ", " cx-5 ", " cx 5 ", " cx5 ", " cx-7 ", " cx 7 ", " cx7 ",
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
                " xceed ", " niro ",
                " ix35 ",
                " stelvio ",
                " durango ",
                " g trieda ", " g-trieda ", " g class ", " g-class ", " bayon ")) {
            return "SUV";
        }

        if (containsAny(titleSource,
                " mpv ", " minivan ",
                " scenic ", " espace ", " c8 ", " spacetourer ",
                " gran tourer ",
                " galaxy ", " s-max ", " s max ", " smax ",
                " sharan ", " alhambra ", " touran ",
                " sportsvan ",
                " caddy ", " berlingo ", " rifter ",
                " proace verso ", " proace city verso ",
                " v 250 ", " v250 ", " v 250l ", " v250l ", " v 250d ", " v250d ", " v300d ",
                " partner tepee ", " tepee ", " partner ",
                " zafira ", " meriva ", " dokker ",
                " roomster ", " lodgy ", " verso ",
                " c-max ", " c max ", " grand c-max ", " grand c max ",
                " tourneo custom ", " tourneo courier ", " tourneo connect ",
                " doblo ", " 500l ", " fiat 500l ", " combo ", " vaneo ",
                " picasso ", " grand c4 picasso ", " c4 picasso ",
                " b 200 ", " b200 ",
                " mazda 5 ",
                " grand scenic ", " grand scénic ",
                " kangoo ", " carens ", " fr-v ", " fr v ", " frv ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource,
                " shooting brake ", " gran tourer ",
                " grandtour ", " grand tour ",
                " kombi ", " combi ", " combi2", " kombi2",
                " wagon ", " sportwagon ", " avant ", " variant ", " sw ", " allroad ",
                " touring ", " turnier ", " caravan ", " estate ",
                " rs6 ", " rs 6 ", " f11 ", " f31 ",
                " alltrack ", " scout ", " outback ",
                " proceed ", " pro ceed ",
                " v40 ", " v50 ", " v60 ", " v70 ", " v 70 ", " v90 ",
                " g31 ", " tipo sw ", " 206sw ", " 207sw ", " 307sw ", " 308sw ", " 407sw ", " 508sw ",
                " i40 wg ", " i40 wagon ", " i40 kombi ")) {
            return "WAGON";
        }

        if (containsAny(titleSource,
                " cc ", " 206cc ", " 207cc ", " 307cc ", " 308cc ",
                " cabrio ", " kabrio ",
                " roadster ", " spyder ", " spider ",
                " convertible ", " cabriolet ",
                " sl 500 ", " sl500 ", " sl 63 ", " sl63 ",
                " slk ", " z4 ", " mx-5 ", " boxster ")) {
            return "CABRIO";
        }

        if (containsAny(titleSource, " audi a5 ", " a5 s-line ", " a5 s line ")) {
            return "COUPE";
        }

        if (containsAny(titleSource,
                " hatchback ", " hatch ", " spaceback ",
                " fabia ", " focus ", " golf ", " polo ",
                " i20 ", " i30 ", " ceed ", " mazda 2 ",
                " aveo ", " spark ", " picanto ",
                " c2 ", " c3 ", " c4 ",
                " clio ", " megane ", " fiesta ",
                " rs3 ", " rs 3 ",
                " civic ", " leon ", " swift ", " born ", " punto ", " panda ",
                " leaf ", " micra ", " colt ", " spacestar ", " space star ",
                " f40 ", " řada 1 ", " rada 1 ",
                " a180 ", " a180d ", " a200 ", " a200d ",
                " 116i ", " 118i ", " 120i ", " 116d ", " 118d ", " 120d ",
                " agila ", " 107 ", " 147 ", " 207 ", " 208 ", " 308 ",
                " sandero ", " logan ", " scala ", " citigo ",
                " fiat 500 ", " tipo ", " fiat tipo ",
                " auris ", " prius ", " corolla ", " corsa ", " mazda 3 ", " rapid ", " yaris ", " getz ", " soul ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource,
                " gran coupe ", " gran coupé ",
                " coupe ", " coupé ",
                " alfa gt ", " brera ", " m4 ", " mustang ", " amg gt ",
                " tt ", " scirocco ", " supra ", " brz ",
                " gt86 ", " gr86 ",
                " 370z ", " 350z ",
                " e92 ", " e93 ", " 335i ", " 335d ", " 220d coupe ", " 220i coupe ",
                " rc f ", " rc 300h ", " lexus rc ",
                " r8 ", " camaro ")) {
            return "COUPE";
        }

        if (containsAny(titleSource,
                " pickup ", " pick-up ",
                " ranger ", " hilux ", " amarok ",
                " navara ", " l200 ", " l 200 ",
                " ram ", " gladiator ")) {
            return "PICKUP";
        }

        if (containsAny(titleSource,
                " sportback ", " fastback ", " liftback ",
                " sedan ", " saloon ",
                " limo ", " limousine ", " limuzína ", " limuzina ",
                " charger ",
                " giulia ", " alfa 156 ", " romeo 156 ", " alfa 159 ", " romeo 159 ", " alfa 166 ", " romeo 166 ",
                " cruze ", " lancer ",
                " s60 ",
                " octavia ", " oktavia ", " mazda 6 ", " superb ", " passat ", " arteon ",
                " a4 ", " a6 ", " a7 ", " a8 ", " s5 ", " s7 ", " s8 ",
                " a6c7 ",
                " s400d ", " s400 ",
                " c220 ", " c220d ", " e220 ", " e 220 ", " e280 ", " e 280 ",
                " e90 ", " e60 ", " e39 ", " f07 ",
                " 3 series ", " 5 series ", " 7 series ",
                " řada 3 ", " rada 3 ", " řada 5 ", " rada 5 ", " řada 7 ", " rada 7 ",
                " 318d ", " 320d ", " 330d ", " 318i ", " 320i ", " 330i ",
                " 730d ", " 730i ", " 740d ", " 740i ", " 750d ", " 750i ",
                " 540ix ", " 540i ", " 540d ", " gran turismo ",
                " c5 ", " mondeo ", " mondeo sedan ",
                " 508 ",
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

        String urlBrand = extractBrandFromUrl(urlLower);
        if (urlBrand != null && !titleBrand.equals(urlBrand)) {
            return true;
        }

        return switch (titleBrand) {
            case "SKODA" -> containsAny(urlLower, "audi-", "bmw-", "mercedes-", "dacia-", "ford-", "toyota-");
            case "DACIA" -> containsAny(urlLower, "skoda-", "audi-", "bmw-", "mercedes-", "volkswagen-", "seat-");
            case "BMW" -> containsAny(urlLower, "skoda-", "dacia-", "seat-", "renault-", "volkswagen-", "vw-");
            case "AUDI" -> containsAny(urlLower, "skoda-", "dacia-", "seat-", "renault-", "volkswagen-", "vw-", "bmw-");
            case "MERCEDES" -> containsAny(urlLower, "skoda-", "seat-", "dacia-", "ford-");
            case "SEAT" -> containsAny(urlLower, "dacia-", "mercedes-", "bmw-", "audi-", "skoda-", "opel-");
            case "VOLKSWAGEN" -> containsAny(urlLower, "skoda-", "audi-", "bmw-", "mercedes-", "dacia-", "seat-", "cupra-");
            case "VOLVO" -> containsAny(urlLower, "mercedes-", "mb-", "bmw-", "audi-", "volkswagen-", "vw-", "skoda-");
            case "ALFA_ROMEO" -> containsAny(urlLower, "peugeot-", "boxer-", "citroen-", "fiat-", "ford-", "renault-", "skoda-", "volkswagen-");
            case "TOYOTA" -> containsAny(urlLower, "volkswagen-", "vw-", "sharan-", "passat-", "golf-", "skoda-", "audi-", "bmw-");
            case "PEUGEOT" -> containsAny(urlLower, "alfa-romeo-", "audi-", "bmw-", "skoda-", "volkswagen-");
            default -> false;
        };
    }

    private boolean looksModelUrlMismatch(String title, String url) {
        String titleBrand = extractBrand(title, title);
        String urlBrand = extractBrandFromUrl(url);

        if (titleBrand == null || urlBrand == null || !titleBrand.equals(urlBrand)) {
            return false;
        }

        String model = extractModelForUrlCheck(title);
        if (model == null) {
            return false;
        }

        String slug = "-" + normalizeText(url).toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replaceAll("[^a-z0-9]+", "-") + "-";

        return !urlContainsModel(slug, model);
    }

    private String extractBrandFromUrl(String rawUrl) {
        String url = "-" + normalizeText(rawUrl).toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replaceAll("[^a-z0-9]+", "-") + "-";

        if (url.contains("-skoda-")) return "SKODA";
        if (url.contains("-volkswagen-") || url.contains("-vw-")) return "VOLKSWAGEN";
        if (url.contains("-audi-")) return "AUDI";
        if (url.contains("-bmw-")) return "BMW";
        if (url.contains("-mercedes-") || url.contains("-mercedes-benz-") || url.contains("-mb-")) return "MERCEDES";
        if (url.contains("-volvo-")) return "VOLVO";
        if (url.contains("-toyota-")) return "TOYOTA";
        if (url.contains("-lexus-")) return "LEXUS";
        if (url.contains("-ford-")) return "FORD";
        if (url.contains("-renault-")) return "RENAULT";
        if (url.contains("-seat-")) return "SEAT";
        if (url.contains("-peugeot-")) return "PEUGEOT";
        if (url.contains("-opel-")) return "OPEL";
        if (url.contains("-hyundai-")) return "HYUNDAI";
        if (url.contains("-kia-")
                || containsAny(url, "-ceed-", "-proceed-", "-pro-ceed-", "-sportage-", "-sorento-", "-stonic-", "-xceed-", "-rio-", "-picanto-", "-carens-", "-soul-", "-niro-")) return "KIA";
        if (url.contains("-mazda-")) return "MAZDA";
        if (url.contains("-citroen-") || url.contains("-citreon-")) return "CITROEN";
        if (url.contains("-fiat-")) return "FIAT";
        if (url.contains("-nissan-")) return "NISSAN";
        if (url.contains("-honda-")) return "HONDA";
        if (url.contains("-suzuki-")) return "SUZUKI";
        if (url.contains("-dacia-")) return "DACIA";
        if (url.contains("-cupra-")) return "CUPRA";
        if (url.contains("-jeep-")) return "JEEP";
        if (url.contains("-subaru-")) return "SUBARU";
        if (url.contains("-mitsubishi-")) return "MITSUBISHI";
        if (url.contains("-porsche-")) return "PORSCHE";
        if (url.contains("-tesla-")) return "TESLA";
        if (url.contains("-chevrolet-")) return "CHEVROLET";
        if (url.contains("-land-rover-") || url.contains("-range-rover-")) return "LAND_ROVER";
        if (url.contains("-mini-")) return "MINI";
        return null;
    }

    private String extractModelForUrlCheck(String title) {
        String source = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(source, " passat ")) return "passat";
        if (containsAny(source, " golf ")) return "golf";
        if (containsAny(source, " tiguan ")) return "tiguan";
        if (containsAny(source, " touran ")) return "touran";
        if (containsAny(source, " caddy ")) return "caddy";
        if (containsAny(source, " id.4 ", " id4 ")) return "id4";
        if (containsAny(source, " beetle ")) return "beetle";
        if (containsAny(source, " scirocco ")) return "scirocco";

        if (containsAny(source, " xc40 ")) return "xc40";
        if (containsAny(source, " xc60 ")) return "xc60";
        if (containsAny(source, " xc70 ")) return "xc70";
        if (containsAny(source, " xc90 ")) return "xc90";
        if (containsAny(source, " v40 ")) return "v40";
        if (containsAny(source, " v60 ")) return "v60";
        if (containsAny(source, " v70 ")) return "v70";
        if (containsAny(source, " v90 ")) return "v90";
        if (containsAny(source, " c70 ")) return "c70";

        if (containsAny(source, " q3 ")) return "q3";
        if (containsAny(source, " q4 ")) return "q4";
        if (containsAny(source, " q5 ")) return "q5";
        if (containsAny(source, " q7 ")) return "q7";
        if (containsAny(source, " q8 ")) return "q8";
        if (containsAny(source, " a4 ")) return "a4";
        if (containsAny(source, " a5 ")) return "a5";
        if (containsAny(source, " a6 ")) return "a6";
        if (containsAny(source, " a8 ")) return "a8";
        if (containsAny(source, " tt ")) return "tt";

        if (containsAny(source, " yaris ")) return "yaris";
        if (containsAny(source, " corolla ")) return "corolla";
        if (containsAny(source, " rav4 ", " rav 4 ")) return "rav4";
        if (containsAny(source, " auris ")) return "auris";
        if (containsAny(source, " aygo ")) return "aygo";
        if (containsAny(source, " c-hr ", " chr ")) return "chr";
        if (containsAny(source, " hilux ")) return "hilux";

        if (containsAny(source, " x1 ")) return "x1";
        if (containsAny(source, " x3 ")) return "x3";
        if (containsAny(source, " x4 ")) return "x4";
        if (containsAny(source, " x5 ")) return "x5";
        if (containsAny(source, " x6 ")) return "x6";
        if (containsAny(source, " x7 ")) return "x7";

        if (containsAny(source, " ceed ")) return "ceed";
        if (containsAny(source, " sportage ")) return "sportage";
        if (containsAny(source, " sorento ")) return "sorento";
        if (containsAny(source, " stonic ")) return "stonic";
        if (containsAny(source, " xceed ")) return "xceed";
        if (containsAny(source, " rio ")) return "rio";
        if (containsAny(source, " picanto ")) return "picanto";
        if (containsAny(source, " carens ")) return "carens";

        if (containsAny(source, " mazda 2 ")) return "mazda2";
        if (containsAny(source, " mazda 3 ")) return "mazda3";
        if (containsAny(source, " mazda 5 ")) return "mazda5";
        if (containsAny(source, " mazda 6 ")) return "mazda6";
        if (containsAny(source, " cx-5 ", " cx 5 ", " cx5 ")) return "cx5";
        if (containsAny(source, " cx-7 ", " cx 7 ", " cx7 ")) return "cx7";

        if (containsAny(source, " grand c4 picasso ", " c4 grand picasso ", " grand c4 spacetourer ", " c4 grand spacetourer ")) return "grandc4picasso";
        if (containsAny(source, " c4 picasso ", " picasso ")) return "c4picasso";
        if (containsAny(source, " berlingo ")) return "berlingo";
        if (containsAny(source, " c5 aircross ")) return "c5aircross";
        if (containsAny(source, " c3 aircross ")) return "c3aircross";
        if (containsAny(source, " ds3 ")) return "ds3";
        if (containsAny(source, " c3 ")) return "c3";
        if (containsAny(source, " c4 ")) return "c4";
        if (containsAny(source, " c5 ")) return "c5";

        if (containsAny(source, " i10 ")) return "i10";
        if (containsAny(source, " i20 ")) return "i20";
        if (containsAny(source, " i30 ")) return "i30";
        if (containsAny(source, " i40 ")) return "i40";
        if (containsAny(source, " ix20 ")) return "ix20";
        if (containsAny(source, " ix35 ")) return "ix35";
        if (containsAny(source, " tucson ")) return "tucson";
        if (containsAny(source, " kona ")) return "kona";
        if (containsAny(source, " ioniq ", " ionig ")) return "ioniq";

        if (containsAny(source, " aveo ")) return "aveo";
        if (containsAny(source, " camaro ")) return "camaro";
        if (containsAny(source, " corvette ", " corveta ")) return "corvette";
        if (containsAny(source, " silverado ")) return "silverado";
        if (containsAny(source, " orlando ")) return "orlando";
        if (containsAny(source, " captiva ")) return "captiva";
        if (containsAny(source, " cruze ")) return "cruze";
        if (containsAny(source, " malibu ")) return "malibu";
        if (containsAny(source, " suburban ")) return "suburban";
        if (containsAny(source, " tahoe ")) return "tahoe";
        if (containsAny(source, " hhr ")) return "hhr";
        if (containsAny(source, " spark ")) return "spark";
        if (containsAny(source, " lacetti ")) return "lacetti";

        return null;
    }

    private boolean urlContainsModel(String slug, String model) {
        return switch (model) {
            case "id4" -> slug.contains("-id4-") || slug.contains("-id-4-");
            case "rav4" -> slug.contains("-rav4-") || slug.contains("-rav-4-");
            case "chr" -> slug.contains("-chr-") || slug.contains("-c-hr-");
            case "corvette" -> slug.contains("-corvette-") || slug.contains("-corveta-");
            case "ceed" -> slug.contains("-ceed-") || slug.contains("-cee-d-");
            case "xceed" -> slug.contains("-xceed-") || slug.contains("-x-ceed-");
            case "mazda2" -> slug.contains("-mazda-2-") || slug.contains("-mazda2-");
            case "mazda3" -> slug.contains("-mazda-3-") || slug.contains("-mazda3-");
            case "mazda5" -> slug.contains("-mazda-5-") || slug.contains("-mazda5-");
            case "mazda6" -> slug.contains("-mazda-6-") || slug.contains("-mazda6-");
            case "cx5" -> slug.contains("-cx-5-") || slug.contains("-cx5-");
            case "cx7" -> slug.contains("-cx-7-") || slug.contains("-cx7-");
            case "grandc4picasso" -> (slug.contains("-grand-c4-") && (slug.contains("-picasso-") || slug.contains("-spacetourer-")))
                    || slug.contains("-c4-grand-picasso-") || slug.contains("-c4-grand-spacetourer-");
            case "c4picasso" -> slug.contains("-c4-picasso-") || slug.contains("-picasso-");
            case "berlingo" -> slug.contains("-berlingo-");
            case "c5aircross" -> slug.contains("-c5-aircross-");
            case "c3aircross" -> slug.contains("-c3-aircross-");
            case "ds3" -> slug.contains("-ds3-") || slug.contains("-ds-3-");
            case "c3" -> slug.contains("-c3-");
            case "c4" -> slug.contains("-c4-");
            case "c5" -> slug.contains("-c5-");
            case "a8" -> slug.contains("-a8-") || slug.contains("-a8l-");
            default -> slug.contains("-" + model + "-");
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
        String titleSource = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String wordSource = compactSearchText(title + " " + shortenForCheck(text, 400) + " " + safe(url));

        if (looksLikePassengerCarModel(title)) {
            return false;
        }

        if (containsAny(titleSource,
                " trafic ", " traffic ",
                " master ", " movano ", " peugeot boxer ", " jumper ", " ducato ",
                " expert ", " jumpy ", " scudo ",
                " proace ")
                || containsAny(wordSource,
                " transporter ", " caravelle ", " carawelle ")) {
            if (containsAny(titleSource, " proace verso ", " proace city verso ", " spacetourer ")
                    || containsAny(wordSource, " proace verso ", " proace city verso ", " spacetourer ")) {
                return false;
            }
            return true;
        }

        String detectedBrand = extractBrand(title, text);
        String detectedType = extractCarType(title, text, url);

        if (detectedBrand != null && detectedType != null) {
            return false;
        }

        if (looksClearlyCommercialBody(source)) {
            return true;
        }

        return containsAny(source,
                " sprinter ", " vito ", " viano ",
                " transporter ", " caravelle ", " carawelle ", " multivan ",
                " trafic ", " traffic ", " vivaro ", " primastar ",
                " partner l1 ", " partner l2 ",
                " expert ", " scudo ", " proace ",
                " tourneo custom ", " transit custom ",
                " iveco ", " daily ", " peugeot boxer ", " ducato ", " jumper ",
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
        ) || containsAny(wordSource,
                " transporter ", " caravelle ", " carawelle ", " multivan ",
                " sprinter ", " crafter ", " transit ", " ducato ",
                " jumper ", " peugeot boxer ", " movano ", " master ");
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

        if (containsAny(titleSource,
                " q7 ", " rs3 ", " stelvio ", " giulia ", " giulietta ",
                " a3 ", " a4 ", " a5 ", " a6 ", " q3 ", " q5 ",
                " x1 ", " x3 ", " x5 ",
                " civic ", " duster ", " panda ", " punto ", " mustang ",
                " octavia ", " superb ", " passat ")) {
            return false;
        }

        if (looksLikeRealCar(title, analysisText)) {
            return false;
        }
        String source = " " + normalizeText(title + " " + text + " " + shortenForCheck(analysisText, 700))
                .toLowerCase(Locale.ROOT) + " ";

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
        String strongPartsSource = " " + normalizeText(title + " " + shortenForCheck(analysisText, 500))
                .toLowerCase(Locale.ROOT) + " ";
        String titleValue = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String source = " " + normalizeText(shortenForCheck(text, 450) + " " + safe(url)).toLowerCase(Locale.ROOT) + " ";
        String analysis = " " + shortenForCheck(normalizeText(analysisText).toLowerCase(Locale.ROOT), 900) + " ";
        String compactTitleValue = compactSearchText(title);

        if (startsWithAny(titleValue,
                "motor ",
                "motor alfa ",
                "motor bmw ",
                "motor audi ",
                "motor mercedes ",
                "motor škoda ",
                "motor skoda ")) {
            return true;
        }

        if (looksLikeRealCar(title, analysisText)
                && !startsWithAny(compactTitleValue, "predni ", "zadni ", "svetlomety ", "svetla ")
                && containsAny(compactTitleValue, " svetlomety ", " svetla ")) {
            return false;
        }

        if (looksLikePassengerCarModel(title)
                && !containsAny(titleValue, " na dily ", " na nd ", " nahradni dily ", " rezervace ", " rezervovano ", " prodano ")) {
            return false;
        }

        if (startsWithAny(titleValue,
                "přední halogenové světlomety ",
                "predni halogenove svetlomety ",
                "světlomety ",
                "svetlomety ",
                "světla ",
                "svetla ",
                "sedadla ",
                "sedačky ",
                "sedacky ",
                "kožené sedačky ",
                "kozene sedacky ",
                "klika ",
                "levý práh ",
                "levy prah ",
                "pravý práh ",
                "pravy prah ",
                "středový panel ",
                "stredovy panel ",
                "střešní lyžiny ",
                "stresni lyziny ",
                "střešní nosič ",
                "stresni nosic ",
                "náhradní díly ",
                "nahradni dily ",
                "rozprodej na díly ",
                "rozprodej na dily ",
                "motor ",
                "motor alfa ",
                "motor bmw ",
                "motor audi ",
                "motor mercedes ",
                "motor škoda ",
                "motor skoda ")){
            return true;
        }

        if (containsAny(titleValue,
                " světlomety ", " svetlomety ",
                " klika ",
                " práh ", " prah ",
                " středový panel ", " stredovy panel ",
                " střešní lyžiny ", " stresni lyziny ",
                " náhradní díly ", " nahradni dily ",
                " na díly ", " na dily ")) {
            return true;
        }

        boolean hasExplicitPartSale = containsAny(strongPartsSource,
                " prodám motor ",
                " prodam motor ",
                " prodám dobrý motor ",
                " prodam dobry motor ",
                " motor na prodej ",
                " motor z auta ",
                " motor z vozu ",
                " dobrý motor ",
                " dobry motor ",
                " náhradní díly ",
                " nahradni dily ",
                " náhradní díl ",
                " nahradni dil ",
                " na náhradní díly ",
                " na nahradni dily ");

        if (hasExplicitPartSale) {
            return true;
        }

        boolean realCar = looksLikeRealCar(title, analysisText);

        if (realCar) {
            return false;
        }

        if (looksTyreOrWheelListing(title, text, analysisText)) {
            return true;
        }

        if (extractBrand(title, analysisText) != null) {
            return false;
        }

        if (containsAny(strongPartsSource,
                " alu disky ",
                " alu kola ",
                " sada kol ",
                " sada pneu ",
                " svetlomety ",
                " sedadla ",
                " stresni lyziny ")) {
            return true;
        }

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
        String titleSource = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String source = " " + normalizeText(title + " " + shortenForCheck(text, 500)).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(titleSource, " mustang ", " civic type r ", " duster ", " stelvio ")
                && !containsAny(titleSource, " na dily ", " na nd ", " nahradni dily ", " nepojizdny ", " nepojizdne ", " vadny ", " vadne ")) {
            return false;
        }

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
        if (containsAny(cleanTitle, "prodam nebo vymenim", "prodám nebo vyměním", " rezervováno ", " rezervovano ",
                " rezervace ")) {
            return true;
        }

        if (cleanTitle.equals("prodam")
                || cleanTitle.equals("prodám")
                || cleanTitle.equals("prodej")
                || cleanTitle.equals("na prodej")) {
            return true;
        }

        String source = " " + normalizeText(title + " " + shortenForCheck(text, 500)).toLowerCase(Locale.ROOT) + " ";

        if (looksLikePassengerCarModel(title)
                && !containsAny(cleanTitle, "prodano", "prodĂˇno", "zalohovano", "zĂˇlohovĂˇno", "rezervace", "rezervovano", "rezervovĂˇno")) {
            return false;
        }

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
                " rezervováno ", " rezervovano ", " rezervace ",
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
                " giulia ",
                " giulietta ",
                " mito ",
                " stelvio ",
                " tonale ",
                " brera ",
                " alfetta ",
                " alfa 145 ",
                " alfa 146 ",
                " alfa 147 ",
                " alfa 156 ",
                " alfa 159 ",
                " alfa 166 ",
                " romeo 145 ",
                " romeo 146 ",
                " romeo 147 ",
                " romeo 156 ",
                " romeo 159 ",
                " romeo 166 ",
                " rs3 ",
                " rs 3 ",
                " accord ",
                " civic ",
                " jazz ",
                " cr-v ",
                " crv ",
                " hr-v ",
                " hrv ",
                " rs6 ",
                " rs 6 ",
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
                " juke ",
                " x-trail ",
                " patrol ",
                " 2008 ",
                " 3008 ",
                " 5008 ",
                " koleos ",
                " kadjar ",
                " austral ",
                " arkana ",
                " ateca ",
                " arona ",
                " cx-5 ",
                " cx5 ",
                " megane ",
                " clio ",
                " talisman ",
                " thalia ",
                " e-tron ",
                " etron ",
                " q7 ",
                " q5 ",
                " q4 ",
                " sq8 ",
                " s7 ",
                " rs3 ",
                " c5 aircross ",
                " c3 aircross ",
                " aircross "
        );
    }

    private String compactSearchText(String value) {
        String compact = normalizeText(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return compact.isBlank() ? " " : " " + compact + " ";
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

    private String titleFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }

        String slug = url.substring(url.lastIndexOf("/") + 1);

        slug = slug.replaceFirst("(?i)\\.php$", "");
        slug = slug.replaceAll("^\\d+[-_]?", "");

        String title = slug
                .replace("-", " ")
                .replace("_", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return normalizeText(title);
    }

    private void sleepBetweenDetailRequests() {
        try {
            Thread.sleep(400 + java.util.concurrent.ThreadLocalRandom.current().nextInt(600));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

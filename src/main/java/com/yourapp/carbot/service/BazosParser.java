package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
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

    private record TitleProfile(String fuelType, String transmission, String carType, boolean strongIdentity) {
    }

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
        int tyreOrWheelListingCount = 0;

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
                            case "tyre_or_wheel_listing" -> tyreOrWheelListingCount++;
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
                "BAZOS SUMMARY tyre_or_wheel_listing={} parsed={} empty_title={} demand_listing={} commercial_vehicle={} non_car_listing={} broken_or_for_parts={} suspicious_listing={} invalid_price={} missing_price={} parse_error={}" ,
                tyreOrWheelListingCount,
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
                return ParseResult.skip("tyre_or_wheel_listing");
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
            TitleProfile titleProfile = inferTitleProfile(title, url);
            String fuelType = firstNonBlank(
                    titleProfile.fuelType(),
                    extractFuelType(title),
                    extractFuelType(listingText)
            );
            fuelType = preferExplicitTitleFuelType(title, fuelType);
            fuelType = correctLikelyFalseElectricFuel(title, fuelType);
            fuelType = correctLikelyNoisyFuel(title, fuelType);
            String transmission = firstNonBlank(
                    titleProfile.transmission(),
                    extractTransmission(title),
                    extractTransmission(listingText),
                    ("ELECTRIC".equals(fuelType) || looksAutomaticHybridTitle(title, fuelType)) ? "AUTOMATIC" : null
            );
            if (looksLikelyFalseAutomatic(title, transmission)) {
                transmission = null;
            }
            if (looksLikelyFalseManual(title, transmission)) {
                transmission = null;
            }
            if (transmission == null && looksAutomaticHybridTitle(title, fuelType)) {
                transmission = "AUTOMATIC";
            }
            if ("ELECTRIC".equals(fuelType)) {
                transmission = "AUTOMATIC";
            }
            String brand = extractBrand(title, analysisText);
            String carType = firstNonBlank(
                    titleProfile.carType(),
                    extractCarType(title, "", url),
                    extractCarType(title, listingText, url)
            );
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

            String outputTitle = repairMojibake(title);
            String outputLocation = repairMojibake(location);

            CarDto car = new CarDto();
            car.setSource("BAZOS");
            car.setTitle(outputTitle);
            car.setPrice(price);
            car.setPriceValue(priceValue);
            car.setLocation(outputLocation);
            car.setUrl(url);
            car.setImageUrl(imageUrl);
            car.setBrand(brand);
            car.setYear(year);
            car.setMileage(mileage);
            car.setFuelType(fuelType);
            car.setTransmission(transmission);
            car.setCarType(carType);

            log.info("BAZOS CAR title='{}' price={} location={} year={} mileage={} fuelType={} transmission={} carType={} brand={} url={}",
                    safe(outputTitle),
                    priceValue,
                    safe(outputLocation),
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
        String titleSource = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";

        if (year != null
                && year >= 2020
                && priceValue < 500_000
                && containsAny(source, " land cruiser ", " landcruiser ", " lc300 ", " lc 300 ")) {
            return true;
        }

        if (year != null && year >= 2015 && priceValue < 80_000) {
            if (year <= 2016
                    && priceValue >= 40_000
                    && containsAny(source, " sandero ", " logan ", " punto ", " panda ", " aveo ", " spark ", " c2 ", " c3 ")) {
                return false;
            }
            return true;
        }

        if (year != null && year >= 2020 && priceValue < 100_000) {
            return true;
        }

        if (priceValue < 40_000 && mileage != null && mileage >= 300_000) {
            return true;
        }

        if (priceValue < 100_000 && mileage != null && mileage >= 320_000) {
            if (priceValue >= 50_000
                    && extractMileage(title, title) == null
                    && containsAny(titleSource, " zafira ", " 308 ", " mokka ", " corsa ", " 5008 ")) {
                return false;
            }
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

        Integer titleYear = extractYearFromTitle(normalizedTitle);
        if (titleYear != null) {
            return titleYear;
        }

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

        Matcher shortYearMatcher = Pattern.compile(
                "(?i)(?:r\\.v\\.?|r\\.|rv|rok|model)\\s*[:\\-\\.]?\\s*'?([0-9]{2})\\b"
        ).matcher(source);

        if (shortYearMatcher.find()) {
            Integer year = parseShortYearCandidate(shortYearMatcher.group(1));
            if (year != null && !isBadYearContext(source, shortYearMatcher.start(), shortYearMatcher.end())) {
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

    private Integer extractYearFromTitle(String normalizedTitle) {
        Matcher monthYearMatcher = Pattern.compile(
                "(?i)(?:r\\.v\\.?|rv|rok)?\\s*[:\\-\\.]?\\s*(?:0?[1-9]|1[0-2])\\s*/\\s*'?([0-9]{2})\\b"
        ).matcher(normalizedTitle);
        if (monthYearMatcher.find()) {
            Integer year = parseShortYearCandidate(monthYearMatcher.group(1));
            if (year != null) {
                return year;
            }
        }

        Matcher matcher = Pattern.compile(
                "(?i)(?:rok vĂ˝roby|rok vyroby|r\\.v\\.?|r\\.|rv|rok|model)\\s*[:\\-\\.]?\\s*(?:\\d{1,2}\\s*/\\s*)?(19\\d{2}|20\\d{2})"
        ).matcher(normalizedTitle);

        if (matcher.find()) {
            Integer year = parseYearCandidate(matcher.group(1));
            if (year != null) {
                return year;
            }
        }

        matcher = Pattern.compile("(?i)\\b(19\\d{2}|20\\d{2})\\s*r\\b").matcher(normalizedTitle);
        if (matcher.find()) {
            Integer year = parseYearCandidate(matcher.group(1));
            if (year != null) {
                return year;
            }
        }

        matcher = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(normalizedTitle);
        while (matcher.find()) {
            String rawYear = matcher.group(1);
            String normalizedLower = normalizedTitle.toLowerCase(Locale.ROOT);

            if (("2008".equals(rawYear) && normalizedLower.contains("peugeot 2008"))
                    || ("3008".equals(rawYear) && normalizedLower.contains("peugeot 3008"))
                    || ("5008".equals(rawYear) && normalizedLower.contains("peugeot 5008"))) {
                continue;
            }

            Integer year = parseYearCandidate(rawYear);
            if (year != null) {
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

        matcher = Pattern.compile("(?i)\\b(?:najeto|najezd)\\s*(?:cca|asi)?\\s*[:\\-]?\\s*([0-9\\s\\.]{5,6})\\b").matcher(source);
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

        matcher = Pattern.compile("(?i)\\b([0-9]{1,3})\\s*x{2,3}\\s*km\\b").matcher(source);
        while (matcher.find()) {
            Integer value = parseMileageCandidate(matcher.group(1) + "000");
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private TitleProfile inferTitleProfile(String title, String url) {
        String titleSource = " " + normalizeText(safe(title)).toLowerCase(Locale.ROOT) + " ";
        String fuelType = firstNonBlank(extractFuelType(title), extractFuelType(url));
        String transmission = extractTransmission(title);
        String carType = extractCarType(title, "", url);
        boolean strongIdentity = extractBrand(title, title) != null
                || carType != null
                || looksLikeRealCar(titleSource, titleSource);

        return new TitleProfile(fuelType, transmission, carType, strongIdentity);
    }

    private String extractFuelType(String text) {
        String source = " " + normalizeText(text).toLowerCase(Locale.ROOT) + " ";
        String ascii = asciiSearchText(text);
        String compact = source.replaceAll("[^a-z0-9]", "");

        // LPG / CNG first
        if (containsAny(source, " lpg ", " plyn ") || compact.contains("lpg")) {
            return "LPG";
        }

        if (containsAny(source, " cng ", " cng/", " cng-", " g-tec ", " g tec ", " gtec ")
                || compact.contains("cng")) {
            return "CNG";
        }

        if (containsAny(source,
                " plug-in hybrid ", " plugin hybrid ", " plug in hybrid ",
                " plug-in ", " plug in ", " phev ",
                " tfsi e ", " e-tfsi ", " etfsi ", " tsi e ")
                || ((containsAny(source, " passat ", " golf ", " vw ", " volkswagen ") || compact.contains("volkswagen"))
                && containsAny(source, " gte "))) {
            return "PLUGIN_HYBRID";
        }

        if ((containsAny(source, " volvo ", " xc60 ", " xc90 ", " v60 ", " v90 ", " s60 ", " s90 ")
                || compact.matches(".*(?:volvo|xc60|xc90|v60|v90|s60|s90).*"))
                && (containsAny(source, " t8 ", " t 8 ") || compact.contains("t8"))) {
            return "PLUGIN_HYBRID";
        }

        if ((containsAny(source, " volvo ", " xc40 ", " xc60 ", " xc90 ", " v60 ", " v90 ", " s60 ", " s90 ")
                || compact.matches(".*(?:volvo|xc40|xc60|xc90|v60|v90|s60|s90).*"))
                && containsAny(source, " b3 ", " b4 ", " b5 ", " b6 ")) {
            return "HYBRID";
        }

        if (Pattern.compile("(?i)\\b[0-9][\\.,][0-9]\\s*phev\\b")
                .matcher(source).find()
                || compact.contains("phev")) {
            return "PLUGIN_HYBRID";
        }

        if (containsAny(source,
                " plug-in hybrid ", " plugin hybrid ", " plug in hybrid ",
                " plug-in ", " plug in ", " phev ",
                " mild-hybrid ", " mild hybrid ",
                " hybrid ", " hybridni ", " e-hybrid ", " ehybrid ", " e-tfsi ", " etfsi ", " i-mmd ", " immd ", " hev ", " mhev ",
                " superb iv ", " octavia iv ", " passat gte ", " golf gte ", " prius ")) {
            return "HYBRID";
        }

        if (Pattern.compile("(?i)\\b[0-9][\\.,][0-9]\\s*(?:hev|mhev|phev)\\b")
                .matcher(source).find()) {
            return "HYBRID";
        }

        if (Pattern.compile("(?i)\\b(?:ct|is|gs|ls|nx|rx|ux|es|toyota|lexus)?\\s*[2345][05]0\\s*h\\b")
                .matcher(source).find()) {
            return "HYBRID";
        }

        if (compact.contains("hybrid")
                || compact.contains("phev")
                || compact.contains("mhev")
                || compact.matches(".*(?:ct|is|gs|ls|nx|rx|ux|es)?[2345][05]0h.*")
                || compact.contains("ehev")
                || compact.contains("etfsi")
                || compact.contains("immd")) {
            return "HYBRID";
        }

        // ELECTRIC - only strong EV signals
        if (containsAny(source,
                " elektro ", " elektromobil ", " elektroauto ",
                " electric ", " bev ",
                " e-tron ", " etron ",
                " id.3 ", " id.4 ", " id.5 ",
                " tesla ", " leaf ", " enyaq ",
                " cupra born ", " kona electric ", " ioniq 5 ", " ioniq 6 ",
                " bz4x ", " bz 4x ", " bolt ev ", " mirai ")) {
            return "ELECTRIC";
        }

        if (Pattern.compile("\\b\\d{2,3}\\s*kwh\\b", Pattern.CASE_INSENSITIVE)
                .matcher(source).find()) {
            return "ELECTRIC";
        }

        // HYBRID - only clear hybrid signals
        if (containsAny(source,
                " plug-in hybrid ",
                " plugin hybrid ",
                " plug in hybrid ",
                " plug-in ",
                " plug in ",
                " phev ",
                " mild-hybrid ",
                " mild hybrid ",
                " hybrid ",
                " hybridní ",
                " hybridni ",
                " tfsi e ",
                " e-tfsi ",
                " etfsi ",
                " tsi e ",
                " e-hybrid ",
                " ehybrid ",
                " i-mmd ",
                " immd ",
                " hev ",
                " mhev ",
                " superb iv ",
                " octavia iv ",
                " passat gte ",
                " golf gte ",
                " prius ")) {
            return "HYBRID";
        }

        if (containsAny(source, " jts ", " twinspark ", " twin spark ", " tbi ", " turbo ", " ts ", " sce ",
                " bmw m3 ", " m3 ", " civic type-r ", " civic type r ", " type-r ", " type r ", " typer ", " fn2 ", " ep2 ")
                || compact.contains("typer")
                || compact.contains("fn2")
                || compact.contains("ep2")
                || source.matches(".*\\b[0-9][.,][0-9]\\s*t\\b.*")) {
            return "PETROL";
        }

        if (source.contains(" civic ")
                && Pattern.compile("(?i)\\b1[\\.,]8\\s*[il]?\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (containsAny(source, " alfa romeo giulia ", " giulia ")
                && Pattern.compile("(?i)\\b2[\\.,]2\\b").matcher(source).find()) {
            return "DIESEL";
        }

        if (containsAny(source, " bmw m340d ", " m340d ", " m340 d ")) {
            return "DIESEL";
        }

        if (source.contains(" panda ")
                && Pattern.compile("(?i)\\b1[\\.,][12]\\s*i?\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (containsAny(source, " dacia dokker ", " dokker ")
                && Pattern.compile("(?i)\\b1[\\.,]3\\b").matcher(source).find()
                && !containsAny(source, " diesel ", " nafta ", " dci ", " hdi ", " jtd ", " jtdm ", " tdi ")) {
            return "PETROL";
        }

        if (source.contains(" seat leon ")
                && source.contains(" cupra ")
                && Pattern.compile("\\b300\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (containsAny(source, " nissan 200sx ", " 200sx ", " sr20det ")) {
            return "PETROL";
        }

        if ((containsAny(source, " volvo ", " c30 ", " v40 ", " v60 ", " v90 ", " xc40 ", " xc60 ", " xc90 ")
                || compact.matches(".*(?:volvo|c30|v40|v60|v90|xc40|xc60|xc90).*"))
                && (containsAny(source, " t5 ", " t 5 ") || compact.contains("t5"))
                && !isExplicitHybridTitle(source, compact)) {
            return "PETROL";
        }

        // DIESEL
        if (containsAny(source, " volvo ", " xc40 ", " xc60 ", " xc70 ", " xc90 ",
                " v40 ", " v50 ", " v60 ", " v70 ", " v90 ", " s40 ", " s60 ", " s80 ", " s90 ", " c70 ")
                && Pattern.compile("(?i)\\bd\\s*[2345]\\b|\\b[2345]\\s*d\\b").matcher(source).find()) {
            return "DIESEL";
        }

        if (containsAny(source,
                " diesel ", " nafta ",
                " tdi ", " tdci ", " cdi ", " crdi ", " hdi ", " dci ",
                " jtd ", " jtdm ", " mjt ", " mjt2 ", " multijet ", " multi jet ", " bluehdi ", " bluetec ", " cdti ",
                " dtec ", " d-tec ", " tddi ", " ddis ",
                " d4d ", " d-4d ", " did ", " di-d ", " di d ", " td4 ", " ecoblue ", " crd ")) {
            return "DIESEL";
        }

        if (compact.contains("tdi")
                || compact.contains("diesel")
                || compact.contains("tdci")
                || compact.contains("cdi")
                || compact.contains("crdi")
                || compact.contains("hdi")
                || compact.contains("dci")
                || compact.contains("jtd")
                || compact.contains("jtdm")
                || compact.contains("mjt")
                || compact.contains("mjt2")
                || compact.contains("multijet")
                || compact.contains("multij")
                || compact.contains("bluehdi")
                || compact.contains("cdti")
                || compact.contains("dtec")
                || compact.contains("detec")
                || compact.contains("ddis")
                || compact.contains("tddi")
                || compact.contains("d4d")
                || compact.contains("did")
                || compact.contains("ecoblue")
                || compact.contains("30d")
                || compact.contains("20d")
                || compact.contains("22jtd")
                || compact.contains("22jtdm")
                || compact.contains("19jtd")
                || compact.contains("19jtdm")
                || compact.contains("16jtd")
                || compact.contains("16jtdm")) {
            return "DIESEL";
        }

        if (source.matches(".*\\b[0-9][.,][0-9]\\s*(?:d|td)\\b.*")
                || source.matches(".*\\b[0-9]{3}\\s*d\\b.*")
                || source.matches(".*\\b[0-9]{2,3}x?d\\b.*")) {
            return "DIESEL";
        }

        if (containsAny(source, " a6 allroad ") && containsAny(source, " 235 kw ", " 235kw ")) {
            return "DIESEL";
        }

        if (containsAny(source, " audi s6 ", " s6 ", " s 6 ")
                && compact.contains("257kw")) {
            return "DIESEL";
        }

        if (containsAny(source, " audi 100 ", " 100 c3 ")
                && Pattern.compile("\\b2[.,]2\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (source.contains(" sienna ")
                && !containsAny(source, " diesel ", " nafta ", " hybrid ", " hev ", " phev ", " electric ", " elektro ")) {
            return "PETROL";
        }

        if (containsAny(source, " toyota aygo ", " aygo ")
                && !containsAny(source, " hybrid ", " hev ", " electric ", " elektro ")) {
            return "PETROL";
        }

        // PETROL
        if (containsAny(source,
                " benzin ", " benzín ", " benzinovy ", " benzínový ", " petrol ",
                " tsi ", " tfsi ", " fsi ", " gdi ", " tgdi ", " t-gdi ",
                " dig-t ", " ig-t ", " igt ", " tce ", " sce ", " ecoboost ", " mivec ",
                " vtec ", " vti ", " puretech ", " pt ", " mpi ", " twinair ",
                " jts ", " twinspark ", " twin spark ", " tbi ", " ts ", " vvt ", " 16v ", " boosterjet ", " booster jet ",
                " quadrifoglio ", " qv ",
                " gti ", " v6 ", " v8 ", " hemi ",
                " camaro ", " corvette ", " mustang ",
                " gr86 ", " gr 86 ", " gr yaris ",
                " c63 ", " c 63 ", " e43 ", " e 43 ", " s3 ", " s 3 ", " s63 ", " s 63 ",
                " 500 sec ", " sec amg ")) {
            return "PETROL";
        }

        if (containsAny(ascii, " benzin ", " benzinovy ")) {
            return "PETROL";
        }

        if (compact.contains("benzin") || compact.contains("benzinovy")) {
            return "PETROL";
        }

        if (source.matches(".*\\b[0-9][.,][0-9]\\s*benz\\b.*")) {
            return "PETROL";
        }

        if (compact.contains("tsi")
                || compact.contains("tfsi")
                || compact.contains("fsi")
                || compact.contains("gdi")
                || compact.contains("tgdi")
                || compact.contains("tce")
                || compact.contains("sce")
                || compact.contains("ecoboost")
                || compact.contains("mivec")
                || compact.contains("vtec")
                || compact.contains("vti")
                || compact.contains("puretech")
                || compact.contains("mpi")
                || compact.contains("twinair")
                || compact.contains("jts")
                || compact.contains("twinspark")
                || compact.contains("tbi")
                || compact.contains("20t")
                || compact.contains("29bit")
                || compact.contains("vvt")
                || compact.contains("16v")
                || compact.contains("boosterjet")) {
            return "PETROL";
        }

        if ((containsAny(source, " vitara ", " virara ", " jimny ", " sx4 ", " s-cross ", " s cross ", " alto ")
                || Pattern.compile("\\bs\\s*[x×]\\s*4\\b").matcher(source).find())
                && Pattern.compile("\\b(?:1[.,][0346]|2[.,]4)\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (source.contains(" colt ") && Pattern.compile("\\b1[.,][135]\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (source.contains(" eclipse cross ") && Pattern.compile("\\b1[.,]5\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (source.contains(" lancer evo ")
                || (source.contains(" crossland ") && Pattern.compile("\\b1[.,]2\\s*t\\b").matcher(source).find())
                || (source.contains(" primera ") && Pattern.compile("\\b1[.,]8\\b").matcher(source).find())) {
            return "PETROL";
        }

        if ((source.contains(" fiat 500 ") || source.contains(" fiat 500,") || compact.contains("fiat500"))
                && Pattern.compile("\\b(?:0[.,]9|1[.,][0124])\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (source.contains(" peugeot 301 ")
                && Pattern.compile("\\b1[.,]2\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (containsAny(source, " citroen c3 ", " citroen c 3 ", " fiat tipo ", " peugeot 108 ", " peugeot 107 ", " nissan pixo ", " pixo ", " nissan micra ", " micra ", " opel corsa ", " opel mokka ")
                && Pattern.compile("\\b1[.,][024]\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (containsAny(source, " e-torq ", " etorq ")) {
            return "PETROL";
        }

        if (containsAny(source, " dacia logan ", " logan mcv ", " dacia lodgy ")
                && Pattern.compile("\\b1[.,][236]\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (source.contains(" jazz ") && Pattern.compile("\\b1[.,][24]\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (source.contains(" honda city ")
                && Pattern.compile("\\b1[.,][34]\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (source.contains(" z4 ") && Pattern.compile("\\b3[.,]0\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (source.contains(" giulia ")
                && Pattern.compile("\\b2[.,]0\\b").matcher(source).find()
                && !containsAny(source, " diesel ", " nafta ", " jtd ", " jtdm ", " td ", " tdi ")) {
            return "PETROL";
        }

        if (source.contains(" mini cooper ")
                && !containsAny(source, " cooper d ", " cooper sd ", " diesel ", " nafta ")) {
            return "PETROL";
        }

        if (containsAny(source, " aveo ", " spark ")
                && Pattern.compile("\\b[0-9][.,][0-9]\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (source.contains(" orlando ")
                && Pattern.compile("\\b2[.,]0\\b").matcher(source).find()) {
            return "DIESEL";
        }

        if (Pattern.compile("(?i)\\b[0-9]{3}ci\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (source.matches(".*\\b[0-9][.,][0-9]\\s*i\\b.*")
                || source.matches(".*\\b[0-9]{2,3}i\\b.*")) {
            return "PETROL";
        }

        return null;
    }

    private String correctLikelyFalseElectricFuel(String title, String fuelType) {
        if (!"ELECTRIC".equals(fuelType)) {
            return fuelType;
        }

        String source = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String compact = source.replaceAll("[^a-z0-9]", "");

        if (isExplicitHybridTitle(source, compact)) {
            return "HYBRID";
        }

        if (containsAny(source, " id.3 ", " id.4 ", " id.5 ", " e-golf ", " electric ", " elektro ", " kwh ")
                || compact.contains("id3")
                || compact.contains("id4")
                || compact.contains("id5")
                || compact.contains("egolf")) {
            return fuelType;
        }

        if (containsAny(source, " corolla ", " auris ", " avensis ", " camry ", " yaris ", " rav4 ", " prius ", " c-hr ")
                && !containsAny(source, " hybrid ", " hev ", " phev ", " plug-in ", " plugin ", " mirai ")) {
            return extractFuelType(title);
        }

        if (source.contains(" carens ") && Pattern.compile("\\b1[\\.,]7\\b").matcher(source).find()) {
            return "DIESEL";
        }

        if (containsAny(source, " touareg ", " passat ", " golf ", " tiguan ", " t-roc ", " troc ", " touran ")
                && !containsAny(source, " hybrid ", " ehybrid ", " e-hybrid ", " gte ", " phev ", " plug-in ", " plugin ")) {
            return extractFuelType(title);
        }

        return fuelType;
    }

    private String correctLikelyNoisyFuel(String title, String fuelType) {
        if (fuelType == null || fuelType.isBlank()) {
            return fuelType;
        }

        String source = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        if ("DIESEL".equals(fuelType)
                && source.contains(" kangoo ")
                && Pattern.compile("\\b1[\\.,]2\\b").matcher(source).find()) {
            return "PETROL";
        }
        if (source.contains(" sienna ") && "DIESEL".equals(fuelType)) {
            return extractFuelType(title);
        }

        if (containsAny(source, " sienna ", " sandero stepway ", " navara d22 ", " grandland x ")
                && extractFuelType(title) == null) {
            return null;
        }

        if ("HYBRID".equals(fuelType)
                && containsAny(source, " tucson ")
                && extractFuelType(title) == null) {
            return null;
        }

        return fuelType;
    }

    private String preferExplicitTitleFuelType(String title, String fuelType) {
        String source = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String compact = source.replaceAll("[^a-z0-9]", "");

        if (containsAny(source, " plug-in ", " plug in ", " phev ", " gte ", " e-hybrid ", " ehybrid ")
                || compact.contains("plugin")
                || compact.contains("pluginhybrid")
                || compact.contains("phev")) {
            return "PLUGIN_HYBRID";
        }

        if ("PLUGIN_HYBRID".equals(fuelType)
                && containsAny(source, " california ")
                && containsAny(source, " vw ", " volkswagen ", " t6 ", " t6.1 ")
                && !containsAny(source, " hybrid ", " plug-in ", " plug in ", " plugin ", " phev ", " gte ", " e-hybrid ", " ehybrid ")) {
            return extractFuelType(title);
        }

        if (isExplicitHybridTitle(source, compact)) {
            return "HYBRID";
        }

        if (containsAny(source, " lpg ", " plyn ") || compact.contains("lpg")) {
            return "LPG";
        }

        if (containsAny(source, " cng ", " g-tec ", " g tec ", " gtec ")) {
            return "CNG";
        }

        if (containsAny(source, " benzin ", " benzín ", " petrol ")) {
            return "PETROL";
        }

        if (containsAny(source, " sce ")) {
            return "PETROL";
        }

        if (containsAny(source, " puretech ", " pt ")
                && Pattern.compile("\\b[0-9][\\.,][0-9]\\b").matcher(source).find()) {
            return "PETROL";
        }

        if (containsAny(source, " diesel ", " nafta ")) {
            return "DIESEL";
        }

        return fuelType;
    }

    private boolean looksAutomaticHybridTitle(String title, String fuelType) {
        if (!"HYBRID".equals(fuelType) && !"PLUGIN_HYBRID".equals(fuelType)) {
            return false;
        }

        String source = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String compact = source.replaceAll("[^a-z0-9]", "");
        return containsAny(source, " plug-in ", " plug in ", " phev ", " gte ", " e-hybrid ", " ehybrid ")
                || compact.contains("plugin")
                || compact.contains("pluginhybrid")
                || compact.contains("phev")
                || (containsAny(source, " bmw ")
                && Pattern.compile("(?:225|230|320|330|530|545|745|750|30|40|45|50)e(?:xdrive)?").matcher(compact).find())
                || (containsAny(source, " bigster ") && containsAny(source, " hybrid ", " hev "))
                || containsAny(source, " kuga ")
                || containsAny(source, " prius ")
                || (containsAny(source, " toyota ", " lexus ", " auris ", " yaris ", " corolla ", " rav4 ", " rx ")
                && containsAny(source, " hybrid ", " hev ", " 400h "))
                || (containsAny(source, " cr-v ", " cr v ", " crv ", " hr-v ", " hr v ", " hrv ")
                && containsAny(source, " hybrid ", " hev "));
    }

    private boolean isExplicitHybridTitle(String source, String compact) {
        return containsAny(source, " plug-in ", " plug in ", " phev ", " gte ", " e-hybrid ", " ehybrid ",
                " hybrid ", " hybridni ", " hev ", " mhev ")
                || compact.contains("plugin")
                || compact.contains("pluginhybrid")
                || compact.contains("hybrid")
                || compact.contains("phev")
                || compact.contains("mhev")
                || compact.contains("ehev")
                || compact.contains("immd");
    }

    private boolean looksLikelyFalseAutomatic(String title, String transmission) {
        if (!"AUTOMATIC".equals(transmission)) {
            return false;
        }

        String source = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        boolean hasExplicitAutomatic = containsAny(source, " selespeed ", " automat ", " automaticka ", " automatický ",
                " automatic ", " aut. ", " a/t ", " at6 ", " at8 ", " at/8 ", " dsg ", " dct ", " cvt ", " edc ");
        if (hasExplicitAutomatic || Pattern.compile("(?i)\\bedc\\b").matcher(source).find()) {
            return false;
        }

        return containsAny(source, " twin spark ", " twinspark ", " 6ti rychl ", " 6ti rychlost ", " 6 rychl ", " 6 rychlost ",
                " alfa romeo 159 ", " alfa 159 ", " alfa romeo 147 ", " alfa 147 ", " giulietta ", " giuletta ", " alfa romeo sportwagon ",
                " accord ", " civic ", " crx ", " delsol ", " cr-v ", " cr v ", " crv ",
                " peugeot 107 ", " peugeot 206 ", " peugeot 207 ", " peugeot 208 ", " peugeot 301 ",
                " i20 ", " i30 ", " ix20 ", " tucson ", " aveo ",
                " peugeot 308 ", " 308 ",
                " octavia ", " oktavia ",
                " duster ", " sandero ", " stepway ", " logan ", " jogger ", " dokker ", " berlingo ");
    }

    private boolean looksLikelyFalseManual(String title, String transmission) {
        if (!"MANUAL".equals(transmission)) {
            return false;
        }

        String source = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String compact = source.replaceAll("[^a-z0-9]", "");
        boolean hasExplicitManual = containsAny(source, " manual ", " manuĂˇl ", " manualni ", " man. ", " mt ", " 5mt ", " 6mt ", " 6 rychl ", " 6ti rychl ");
        if (!hasExplicitManual
                && containsAny(source, " bmw ")
                && Pattern.compile("(?:225|230|320|330|530|545|745|750|30|40|45|50)e(?:xdrive)?").matcher(compact).find()) {
            return true;
        }

        return containsAny(source, " silverado ")
                && !hasExplicitManual;
    }

    private String extractTransmission(String text) {
        String repairedText = repairMojibake(text);
        String source = " " + normalizeText(repairedText).toLowerCase(Locale.ROOT) + " ";
        String tokens = " " + normalizeText(repairedText).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim() + " ";

        if (containsAny(source,
                " manuální převodovka ",
                " manualni prevodovka ",
                " manuální ",
                " manualni ",
                " manuál ",
                " manuál",
                " manual ",
                " manual",
                " man. ",
                " mech. ",
                " mechanick",
                " 5stupňová manuální ",
                " 5stupnova manualni ",
                " 6stupňová manuální ",
                " 6stupnova manualni ",
                " řazení manuální ",
                " razeni manualni ",
                " 6ti rychl ",
                " 6ti rychlost ",
                " 6 rychl ",
                " 6 rychlost ")) {
            return "MANUAL";
        }

        if (tokens.contains(" 6rychl ") || tokens.contains(" 6ti ") || tokens.contains(" mech ")) {
            return "MANUAL";
        }

        if (containsAny(tokens, " man ", " man 5 ", " man5 ", " man 6 ", " man6 ")) {
            return "MANUAL";
        }

        if (containsAny(tokens, " automat ", " auto ", " aut ", " at ", " a t ", " at8 ", " at6 ", " mta ", " selespeed ")) {
            return "AUTOMATIC";
        }

        if (containsAny(tokens,
                " dsg ", " dct ", " cvt ", " ecvt ", " eat8 ", " edc ", " edcs ", " i shift ", " ishift ",
                " stronic ", " tiptronic ", " powershift ", " multitronic ", " steptronic ", " xtronic ")) {
            return "AUTOMATIC";
        }

        if (containsAny(source, " bmw ")
                && Pattern.compile("(?i)\\b(?:740|745|750|760)\\s*(?:d|i)?\\b").matcher(source).find()
                && !containsAny(source, " manual ", " manuĂˇl ", " manualni ", " man. ", " mt ", " 6mt ")) {
            return "AUTOMATIC";
        }

        if (Pattern.compile("(?i)\\be\\s*-?\\s*cvt\\b").matcher(source).find()) {
            return "AUTOMATIC";
        }

        if (source.contains(" e:hev ") || tokens.contains(" e hev ")
                || source.contains("i-mmd") || tokens.contains(" i mmd ") || tokens.contains(" immd ")) {
            return "AUTOMATIC";
        }

        if (containsAny(source, " prius ")) {
            return "AUTOMATIC";
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
                " selespeed ",
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
                " 7g-tronic ",
                " 7g tronic ",
                " 7gtronic ",
                " edc ",
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

        if (containsAny(source, " shs ", " edc ", " e-dcs ", " edcs ")) {
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
        if (containsAny(titleSource, " abarth ")) return "ABARTH";
        if (containsAny(titleSource, " škoda ", " skoda ")) return "SKODA";
        if (containsAny(titleSource, " volkswagen ", " vw ")) return "VOLKSWAGEN";
        if (containsAny(titleSource, " id.3 ", " id3 ", " id.4 ", " id4 ", " id.5 ", " id5 ")) return "VOLKSWAGEN";
        if (containsAny(titleSource, " audi ", " s4 quattro ")) return "AUDI";
        if (containsAny(titleSource, " bmw ")) return "BMW";
        if (containsAny(titleSource, " mercedes ", " mercedes-benz ")) return "MERCEDES";
        if (containsAny(titleSource, " lexus ")) return "LEXUS";
        if (containsAny(titleSource, " toyota ", " toyata ")) return "TOYOTA";
        if (containsAny(titleSource, " ford ")) return "FORD";
        if (containsAny(titleSource, " renault ")) return "RENAULT";
        if (containsAny(titleSource, " seat ")) return "SEAT";
        if (containsAny(titleSource, " peugeot ", " prugeot ", " peugeto ")) return "PEUGEOT";
        if (containsAny(titleSource, " opel ")) return "OPEL";
        if (containsAny(titleSource, " hyundai ")) return "HYUNDAI";
        if (containsAny(titleSource, " kia ")) return "KIA";
        if (containsAny(titleSource, " volvo ")) return "VOLVO";
        if (containsAny(titleSource, " mazda ")) return "MAZDA";
        if (containsAny(titleSource, " citroën ", " citroen ", " citreon ")) return "CITROEN";
        if (containsAny(titleSource, " fiat ")) return "FIAT";
        if (containsAny(titleSource, " dodge ")) return "DODGE";
        if (containsAny(titleSource, " nissan ")) return "NISSAN";
        if (containsAny(titleSource, " qashqai ", " juke ", " x-trail ", " x trail ", " navara ", " micra ", " leaf ", " primera ", " terrano ", " pixo ", " pulsar ")) return "NISSAN";
        if (containsAny(titleSource, " honda ", " hondu ", " acura ", " insight ")) return "HONDA";
        if (containsAny(titleSource, " suzuki ")) return "SUZUKI";
        if (containsAny(titleSource, " dacia ", " dacie ", " duster ", " sandero ", " logan ", " dokker ", " lodgy ", " jogger ", " bigster ")) return "DACIA";
        if (containsAny(titleSource, " cupra ")) return "CUPRA";
        if (containsAny(titleSource, " jeep ")) return "JEEP";
        if (containsAny(titleSource, " subaru ")) return "SUBARU";
        if (containsAny(titleSource, " mitsubishi ", " mitsubushi ", " mitshubishi ", " mizshubishi ",
                " outlander ", " pajero ", " l200 ", " l 200 ", " lancer ", " eclipse cross ", " eclipse ",
                " asx ", " colt ", " spacestar ", " space star ", " grandis ", " i-miev ", " i miev ", " imiev ")) return "MITSUBISHI";
        if (containsAny(titleSource, " porsche ")) return "PORSCHE";
        if (containsAny(titleSource, " mini ")) return "MINI";
        if (containsAny(titleSource, " tesla ")) return "TESLA";
        if (containsAny(titleSource, " chevrolet ", " daewoo ", " calos ")) return "CHEVROLET";
        if (containsAny(titleSource, " land rover ", " range rover ")) return "LAND_ROVER";
        if (containsAny(titleSource, " lancia ")) return "LANCIA";
        if (containsAny(titleSource, " jaecoo ")) return "JAECOO";
        if (containsAny(titleSource, " omoda ")) return "OMODA";
        if (containsAny(titleSource, " swm ")) return "SWM";
        if (containsAny(compactTitleSource, " nissan ")) return "NISSAN";
        if (containsAny(compactTitleSource, " peugeot ")) return "PEUGEOT";
        if (containsAny(compactTitleSource, " renault ")) return "RENAULT";
        if (containsAny(compactTitleSource, " seat ")) return "SEAT";
        if (containsAny(compactTitleSource, " suzuki ")) return "SUZUKI";

        if (containsAny(titleSource, " focus ", " mondeo ", " fiesta ", " kuga ", " galaxy ", " ranger ", " mustang ", " tourneo ")) {
            return "FORD";
        }

        if (containsAny(titleSource, " doblo ", " tipo ", " panda ", " multipla ", " 500l ", " 500 l ")) {
            return "FIAT";
        }

        if (containsAny(source,
                " alfa romeo ", " alfa ", " alfu ", " romeo ",
                " stelvio ", " giulia ", " giulietta ", " mito ", " alfetta ")) return "ALFA_ROMEO";
        if (containsAny(source, " abarth ")) return "ABARTH";
        if (containsAny(source, " škoda ", " skoda ")) return "SKODA";
        if (containsAny(source, " volkswagen ", " vw ")) return "VOLKSWAGEN";
        if (containsAny(source, " audi ", " s4 quattro ")) return "AUDI";
        if (containsAny(source, " bmw ")) return "BMW";
        if (containsAny(source, " mercedes ", " mercedes-benz ")) return "MERCEDES";
        if (containsAny(source, " lexus ")) return "LEXUS";
        if (containsAny(source, " toyota ", " toyata ")) return "TOYOTA";
        if (containsAny(source, " ford ")) return "FORD";
        if (containsAny(source, " renault ")) return "RENAULT";
        if (containsAny(source, " seat ")) return "SEAT";
        if (containsAny(source, " peugeot ", " prugeot ", " peugeto ")) return "PEUGEOT";
        if (containsAny(source, " opel ")) return "OPEL";
        if (containsAny(source, " hyundai ")) return "HYUNDAI";
        if (containsAny(source, " kia ")) return "KIA";
        if (containsAny(source, " volvo ")) return "VOLVO";
        if (containsAny(source, " mazda ")) return "MAZDA";
        if (containsAny(source, " citroën ", " citroen ", " citreon ")) return "CITROEN";
        if (containsAny(source, " fiat ")) return "FIAT";
        if (containsAny(source, " dodge ")) return "DODGE";
        if (containsAny(source, " nissan ")) return "NISSAN";
        if (containsAny(source, " qashqai ", " juke ", " x-trail ", " x trail ", " navara ", " micra ", " leaf ", " primera ", " terrano ", " pixo ", " pulsar ")) return "NISSAN";
        if (containsAny(source, " honda ", " acura ", " insight ")) return "HONDA";
        if (containsAny(source, " suzuki ")) return "SUZUKI";
        if (containsAny(source, " dacia ", " dacie ", " duster ", " sandero ", " logan ", " dokker ", " lodgy ", " jogger ", " bigster ")) return "DACIA";
        if (containsAny(source, " cupra ")) return "CUPRA";
        if (containsAny(source, " jeep ")) return "JEEP";
        if (containsAny(source, " subaru ")) return "SUBARU";
        if (containsAny(source, " mitsubishi ", " mitsubushi ", " mitshubishi ", " mizshubishi ",
                " outlander ", " pajero ", " l200 ", " l 200 ", " lancer ", " eclipse cross ", " eclipse ",
                " asx ", " colt ", " spacestar ", " space star ", " grandis ", " i-miev ", " i miev ", " imiev ")) return "MITSUBISHI";
        if (containsAny(source, " porsche ")) return "PORSCHE";
        if (containsAny(source, " mini ")) return "MINI";
        if (containsAny(source, " tesla ")) return "TESLA";
        if (containsAny(source, " chevrolet ", " daewoo ", " calos ")) return "CHEVROLET";
        if (containsAny(source, " land rover ", " range rover ")) return "LAND_ROVER";
        if (containsAny(source, " lancia ")) return "LANCIA";
        if (containsAny(source, " jaecoo ")) return "JAECOO";
        if (containsAny(source, " omoda ")) return "OMODA";
        if (containsAny(source, " swm ")) return "SWM";

        // fallback model detection
        if (source.contains(" leon ")) return "SEAT";
        if (source.contains(" ibiza ")) return "SEAT";
        if (source.contains(" alhambra ")) return "SEAT";
        if (source.contains(" altea ")) return "SEAT";
        if (source.contains(" ateca ")) return "SEAT";
        if (source.contains(" arona ")) return "SEAT";
        if (source.contains(" tarraco ")) return "SEAT";

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
        if (source.contains(" grandis ")) return "MITSUBISHI";

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
        if (Pattern.compile("\\bc[2-5]\\b").matcher(source).find()) return "CITROEN";
        if (source.contains(" ds4 ")) return "CITROEN";
        if (source.contains(" ds5 ")) return "CITROEN";
        if (source.contains(" xsara ")) return "CITROEN";

        if (source.contains(" tipo ")) return "FIAT";
        if (source.contains(" doblo ")) return "FIAT";
        if (source.contains(" multipla ")) return "FIAT";
        if (source.contains(" 500l ") || source.contains(" 500 l ")) return "FIAT";
        if (source.contains(" panda ")) return "FIAT";
        if (source.contains(" ducato ")) return "FIAT";
        if (source.contains(" fiat 500 ") || source.contains(" 500 lounge ")) return "FIAT";
        if (source.contains(" kappa ")) return "LANCIA";

        if (source.contains(" compass ")) return "JEEP";
        if (source.contains(" cherokee ")) return "JEEP";
        if (source.contains(" renegade ")) return "JEEP";

        if (source.contains(" sportage ")) return "KIA";
        if (source.contains(" ceed ")) return "KIA";
        if (source.contains(" carens ")) return "KIA";

        if (source.contains(" rav4 ") || source.contains(" rav 4 ")) return "TOYOTA";
        if (source.contains(" land cruiser ")) return "TOYOTA";
        if (source.contains(" landcruiser ")) return "TOYOTA";
        if (source.contains(" prius ")) return "TOYOTA";
        if (source.contains(" corolla ")) return "TOYOTA";
        if (source.contains(" auris ") || source.contains(" aoris ")) return "TOYOTA";
        if (source.contains(" yaris cross ")) return "TOYOTA";
        if (source.contains(" camry ")) return "TOYOTA";
        if (source.contains(" 4runner ") || source.contains(" 4 runner ")) return "TOYOTA";
        if (source.contains(" aygo ")) return "TOYOTA";
        if (source.contains(" mirai ")) return "TOYOTA";
        if (source.contains(" bz4x ") || source.contains(" bz 4x ")) return "TOYOTA";

        if (source.contains(" xc60 ")) return "VOLVO";
        if (source.contains(" xc90 ")) return "VOLVO";
        if (source.contains(" v40 ")) return "VOLVO";
        if (source.contains(" v60 ")) return "VOLVO";
        if (source.contains(" v90 ")) return "VOLVO";
        if (source.contains(" c70 ")) return "VOLVO";

        if (containsAny(source,
                " 116i ", " 118i ", " 120i ", " 218i ", " 220i ",
                " 318i ", " 320i ", " 330i ", " 335i ", " 540ix ",
                " 116d ", " 118d ", " 120d ", " 218d ", " 220d ",
                " 318d ", " 320d ", " 320xd ", " 330d ", " 335d ",
                " 520d ", " 530d ", " 530xd ", " 540d ", " 730d ", " 740d ", " 750xd ",
                " g20 ", " g21 ", " g30 ", " g31 ", " f10 ", " f11 ", " f30 ", " f31 ",
                " xdrive ")) return "BMW";
        if (containsAny(source,
                " a3 ", " a4 ", " a5 ", " a6 ", " a7 ", " a8 ",
                " s4 quattro ",
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
        if (source.contains(" 508 ")) return "PEUGEOT";
        if (source.contains(" 308 ")) return "PEUGEOT";
        if (source.contains(" corsa ")) return "OPEL";
        if (source.contains(" mazda 3 ")) return "MAZDA";
        if (source.contains(" mazda 6 ")) return "MAZDA";
        if (source.contains(" mazdu 6 ")) return "MAZDA";
        if (source.contains(" partner ")) return "PEUGEOT";
        if (source.contains(" outlander ")) return "MITSUBISHI";
        if (source.contains(" pajero ")) return "MITSUBISHI";
        if (source.contains(" l200 ") || source.contains(" l 200 ")) return "MITSUBISHI";
        if (source.contains(" lancer ")) return "MITSUBISHI";
        if (source.contains(" eclipse cross ")) return "MITSUBISHI";
        if (source.contains(" asx ")) return "MITSUBISHI";
        if (source.contains(" colt ")) return "MITSUBISHI";
        if (source.contains(" spacestar ") || source.contains(" space star ")) return "MITSUBISHI";
        if (source.contains(" grandis ")) return "MITSUBISHI";
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
        if (containsAny(source, " civic ", " jazz ", " accord ", " insight ", " cr-v ", " cr v ", " crv ", " hr-v ", " hr v ", " hrv ", " fr-v ", " fr v ", " frv ")) return "HONDA";

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
        if (source.contains(" inster ")) return "HYUNDAI";
        if (source.contains(" ioniq ") || source.contains(" ionig ")) return "HYUNDAI";

        if (source.contains(" jaecoo ")) return "JAECOO";
        if (source.contains(" omoda ")) return "OMODA";
        if (source.contains(" swm ")) return "SWM";

        if (source.contains(" soul ")) return "KIA";

        return null;
    }

    private String extractCarType(String title, String text) {
        return extractCarType(title, text, null);
    }

    private String extractCarType(String title, String text, String url) {
        String titleSource = " " + normalizeText(safe(title)).toLowerCase(Locale.ROOT) + " ";
        titleSource = titleSource + " " + compactSearchText(safe(title));
        titleSource = titleSource + " " + asciiSearchText(safe(title));
        String textSource = " " + normalizeText(safe(text)).toLowerCase(Locale.ROOT) + " ";
        String urlSource = " " + normalizeText(safe(url)).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(titleSource, " c5 ", " citroen c5 ") && containsAny(titleSource, " break ")) {
            return "WAGON";
        }

        if (containsAny(titleSource, " f-150 ", " f 150 ", " f150 ")) {
            return "PICKUP";
        }

        if (containsAny(titleSource, " fr-v ", " fr v ", " frv ", " f-rv ", " f rv ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource, " corolla sedan ", " corolla sd ", " camry ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " avensis ")
                && !containsAny(titleSource, " kombi ", " combi ", " wagon ", " touring ", " sw ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " corolla st ", " corolla ts ", " corolla touring ", " corolla sports touring ",
                " a4 avant ", " a6 avant ", " arteon sb ", " arteon shooting brake ",
                " passat variant ", " passat varian ")) {
            return "WAGON";
        }

        if (containsAny(titleSource, " passat ") && containsAny(titleSource, " variant ", " varian ")) {
            return "WAGON";
        }

        if (containsAny(titleSource, " avant ")
                && containsAny(titleSource, " audi a4 ", " audi a6 ", " a4 ", " a6 ")) {
            return "WAGON";
        }

        if (containsAny(titleSource, " yaris cross ", " rav4 ", " rav 4 ", " c-hr ", " ch-r ", " chr ", " 4runner ", " 4 runner ", " yeti ",
                " fiat 500x ", " 500x ", " antara ", " bigster ")) {
            return "SUV";
        }

        if (containsAny(titleSource, " jaecoo ", " omoda ", " swm g1 ", " swm g01 ", " inster ", " puma gen-e ", " puma gen e ")) {
            return "SUV";
        }

        if (containsAny(titleSource, " s4 quattro ", " audi s4 ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " jimny ", " samurai ", " virara ", " ignis ", " tarraco ")) {
            return "SUV";
        }

        if (containsAny(titleSource, " sx4 ", " sx 4 ", " s-cross ", " s cross ")
                || Pattern.compile("\\bs\\s*[x×]\\s*4\\b").matcher(titleSource).find()) {
            return "SUV";
        }

        if (containsAny(titleSource, " altea ", " altea xl ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource, " leon st ", " seat leon st ", " ibiza combi ", " ibiza kombi ",
                " ibiza st ", " ibiza sportstourer ", " ibiza sport tourer ")
                || (titleSource.contains(" seat leon ") && Pattern.compile("\\bst\\b").matcher(titleSource).find())
                || (containsAny(titleSource, " ibiza ") && containsAny(titleSource, " combi ", " kombi "))) {
            return "WAGON";
        }

        if (Pattern.compile("\\bleon\\s*[0-9]").matcher(titleSource).find()) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " peugeot 405 ", " 405 sri ")
                && !containsAny(titleSource, " break ", " combi ", " kombi ", " wagon ", " sw ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " ibiza ", " alto ", " twingo ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " audi a1 ", " a1 ", " audi s3 ", " s3 ", " s 3 ", " mini cooper ", " cooper ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " cr-z ", " cr z ")) {
            return "COUPE";
        }

        if (containsAny(titleSource, " volvo c30 ", " c30 ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " accord coupe ", " accord coupé ", " accord coupĂ© ")) {
            return "COUPE";
        }

        if (containsAny(titleSource, " 500c ", " fiat 500c ")) {
            return "CABRIO";
        }

        if (containsAny(titleSource, " colt czc ", " czc ", " kabriolet ")) {
            return "CABRIO";
        }

        if (containsAny(titleSource, " bmw m3 ", " m3 ")) {
            return "COUPE";
        }

        if (containsAny(titleSource, " c63 ", " c 63 ", " c63 amg ", " c 63 amg ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " e43 ", " e 43 ", " e43 amg ", " e 43 amg ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " i-miev ", " i miev ", " imiev ", " id.3 ", " id3 ", " pixo ", " pulsar ", " peugeot 108 ", " ypsilon ", " ampera ",
                " ds4 ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " volkswagen up ", " vw up ", " vw up! ", " up! ", " up 1.0 ", " up 10mpi ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " terrano ")) {
            return "SUV";
        }

        if (containsAny(titleSource, " eclipse ") && !containsAny(titleSource, " eclipse cross ")) {
            return "COUPE";
        }

        if (containsAny(titleSource, " clubman ")) {
            return "WAGON";
        }

        if (containsAny(titleSource,
                " shooting brake ", " all-terrain ", " all terrain ",
                " insignia country tourer ",
                " c 220 cdi t ", " c220 cdi t ", " c 220d t ", " c220d t ")) {
            return "WAGON";
        }

        if ((containsAny(titleSource, " accord ") && containsAny(titleSource, " tourer "))
                || containsAny(titleSource, " g21 ", " e91 ", " c5 tourer ", " citroen c5 tourer ", " c5 x7 ",
                " accord tourer ", " accord kombi ", " accord combi ", " accord wagon ",
                " civic tourer ", " focus tunier ", " focus turnier ",
                " insignia st ", " insignia sports tourer ", " insignia sport tourer ", " insignia sport taurer ",
                " astra sports tourer ", " astra sport tourer ", " astra sports touer ", " astra sport touer ", " astra j sports tourer ", " astra k sports tourer ",
                " astra j sport tourer ", " astra k sport tourer ", " astra sw ", " astra combi ", " astra kombi ", " 308 sw ", " peugeot 308 sw ")) {
            return "WAGON";
        }

        if (Pattern.compile("(?i)\\bastra\\s+(?:j|k)?\\b.*\\bsports?\\s+tourer\\b").matcher(titleSource).find()
                || Pattern.compile("(?i)\\bastra\\s+k\\b.*\\bst\\b").matcher(titleSource).find()) {
            return "WAGON";
        }

        if (containsAny(titleSource, " insignia ", " insignie ", " vectra ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " astra k ", " astra j ", " opel astra ", " astra hatchback ", " opel karl ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " a3 ", " a5 ", " a7 ") && titleSource.contains(" sportback ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " audi a3 ", " a3 ")
                && !containsAny(titleSource, " sedan ", " limousine ", " limuzina ", " cabrio ", " cabriolet ", " kabrio ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " multivan ", " california ", " nv 200 ", " nv200 ", " almera tino ", " grandis ", " elgrand ", " prius plus ",
                " traveller ", " travaller ", " jumpy multispace ", " multispace ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource, " golf plus ", " sienna ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource, " glk ", " pathfinder ", " x-trail ", " x trail ", " grandland ", " grandal ", " crossland ")) {
            return "SUV";
        }

        if (containsAny(titleSource, " a6 avant ", " a6c7 avant ", " a6 allroad ", " a6 combi ", " a6 kombi ")) {
            return "WAGON";
        }

        if (containsAny(titleSource, " audi 100 ", " 100 c3 ", " audi 100 c3 ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " a6 ", " a 6 ", " a7 ", " a 7 ", " a8 ", " a 8 ", " a6c7 ", " a6 c7 ", " a6 c8 ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " a4b6 ", " a4 b6 ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " audi s6 avant ", " s6 avant ")) {
            return "WAGON";
        }

        if (containsAny(titleSource, " s60 ", " s 60 ", " s80 ", " s 80 ", " audi s6 ", " s6 ", " s 6 ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " v40 ", " v 40 ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " s 350 ", " s350 ", " s 500 ", " s500 ", " w220 ", " 730d ", " 730ld ", " 730i ", " 740d ", " 740i ", " 750 ", " 750d ", " 750xd ", " 750i ", " m340d ", " m340 d ", " 7 series ", " rada 7 ", " 7er ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " logan mcv ", " logan combi ", " logan kombi ")) {
            return "WAGON";
        }

        if (containsAny(titleSource, " logan ")) {
            return "SEDAN";
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

        if ((containsAny(titleSource, " bmw ", " 430d ", " 430i ", " 420d ", " 420i ", " 440i ") && containsAny(titleSource, " gc ", " f36 "))
                || containsAny(titleSource, " bmw f36 ", " f36 430d ", " f36 430i ", " f36 420d ", " f36 420i ", " f36 440i ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " 420d ", " 420i ", " 430d ", " 430i ", " 435i ", " 440i ")) {
            return "COUPE";
        }

        if (containsAny(titleSource, " 330xd ", " 330 xd ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " e 220d ", " e220d ", " c 220d ", " c220d ", " c 220 d ", " c220 d ")
                && containsAny(titleSource, " combi ", " kombi ", " wagon ", " estate ")) {
            return "WAGON";
        }

        if (Pattern.compile("(?i).*\\b[ce]\\s*220\\s*d\\s*t\\b.*").matcher(titleSource).matches()) {
            return "WAGON";
        }

        if (containsAny(titleSource,
                " e270 ", " e270cdi ", " e 270 ", " e 270 cdi ",
                " e350 bluetec ", " e 350 bluetec ",
                " c180 ", " c 180 ", " c180k ", " c 180k ", " c180 kompressor ", " c 180 kompressor ",
                " c200 ", " c200cdi ", " c 200 ", " c 200 cdi ",
                " c250d ", " c 250d ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " c220 ", " c 220 ", " c220d ", " c 220d ", " e220 ", " e 220 ", " e220d ", " e 220d ", " e300 ", " e 300 ", " e300cdi ", " e 300 cdi ", " cla ", " cls ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " sportwagon ")) {
            return "WAGON";
        }

        if (containsAny(titleSource, " alfa 156 sw ", " alfa romeo 156 sw ", " romeo 156 sw ",
                " alfa 159 sw ", " alfa romeo 159 sw ", " romeo 159 sw ")) {
            return "WAGON";
        }

        if (containsAny(titleSource, " giulia ", " alfa 75 ", " romeo 75 ", " alfa 156 ", " romeo 156 ", " alfa 159 ", " romeo 159 ",
                " ar 159 ", " alfa 166 ", " romeo 166 ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " alfa gt ", " romeo gt ", " gtv ", " brera ")) {
            return "COUPE";
        }

        if (containsAny(titleSource, " 500 sec ", " 126.500 ", " 126 500 ", " sec amg ")) {
            return "COUPE";
        }

        if (containsAny(titleSource, " giulietta ", " giuletta ", " mito ", " alfa 145 ", " romeo 145 ", " alfa 146 ", " romeo 146 ")) {
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

        if (titleSource.contains(" z4 ") && containsAny(titleSource, " coupe ", " coupé ")) {
            return "COUPE";
        }

        if (containsAny(titleSource, " ix20 ", " ix 20 ", " staria ", " n-box ", " n box ", " orlando ", " hhr ", " freemont ", " venga ", " carnival ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource, " i10 ", " aygo ", " jazz ", " insight ", " rio ", " ds3 ", " punto ", " panda ", " grande punto ", " kalos ", " calos ",
                " fiat 500e ", " fiat e500 ", " 500e ", " e500 ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " civic coupe ", " civic coupĂ© ", " civic si ", " fg2 ", " crx ")) {
            return "COUPE";
        }

        if (containsAny(titleSource, " covic ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " honda city ", " accord ", " acoord ", " legend ", " peugeot 301 ", " c-elysee ", " c elysee ", " celysee ", " talisman ", " magentis ", " optima ", " stinger ")) {
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

        String compactTitle = compactSearchText(title);
        boolean isCeed = compactTitle.contains("ceed") || compactTitle.contains("cee d");
        if (isCeed
                && !compactTitle.contains("proceed")
                && !compactTitle.contains("pro ceed")
                && !compactTitle.contains("xceed")
                && !compactTitle.contains("x ceed")
                && !containsAny(titleSource, " sw ", " wagon ", " kombi ", " combi ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource,
                " suv ", " crossover ", " tonale ",
                " bmw x1 ", " bmw x2 ", " bmw x3 ", " bmw x4 ", " bmw x5 ", " bmw x6 ", " bmw x7 ",
                " q2 ", " q3 ", " q4 ", " q5 ", " q7 ", " q8 ",
                " q 2 ", " q 3 ", " q 4 ", " q 5 ", " q 7 ", " q 8 ",
                " sq5 ", " sq7 ",
                " glc ", " gle ", " gls ", " gla ", " glb ", " eqb ",
                " gl 500 ", " gl500 ", " gl320 ", " gl 63 ", " ml 350 ", " ml350 ",
                " kodiaq ", " karoq ", " kamiq ",
                " tiguan ", " touareg ", " t-roc ", " troc ",
                " pajero ", " outlander ", " eclipse cross ", " asx ",
                " qashqai ", " juke ", " x-trail ", " x trail ", " pathfinder ",
                " land cruiser ", " landcruiser ", " patrol ", " peugeot 2008 ", " 3008 ", " 5008 ",
                " explorer ",
                " kuga ", " puma ", " ecosport ",
                " formentor ", " ateca ", " arona ", " tarraco ", " tavascan ",
                " xc40 ", " xc 40 ", " xc60 ", " xc 60 ", " xc70 ", " xc 70 ", " xc90 ", " xc 90 ",
                " ex30 ", " ex40 ", " ex90 ",
                " captur ", " austral ", " arkana ", " rafale ",
                " sportage ", " sorento ", " stonic ",
                " rdx ",
                " tucson ", " santa fe ", " santafe ", " kona ", " pilot ", " ix55 ",
                " duster ", " bigster ", " koleos ", " kadjar ",
                " cr-v ", " cr v ", " crv ", " hr-v ", " hr v ", " hrv ", " rav4 ", " c-hr ", " ch-r ", " chr ",
                " cx-3 ", " cx3 ", " cx-5 ", " cx 5 ", " cx5 ", " cx-7 ", " cx 7 ", " cx7 ", " tribute ",
                " macan ", " cayenne ",
                " ux ", " nx ", " rx ",
                " enyaq ", " id.4 ", " id.5 ",
                " range rover ", " evoque ", " velar ",
                " discovery ", " discovery sport ", " defender ",
                " compass ", " cherokee ", " grand cherokee ",
                " grand vitara ", " vitara ", " samurai ", " yeti ",
                " captiva ", " tahoe ", " suburban ", " trailblazer ", " bolt ev ",
                " model y ",
                " xv ", " forester ",
                " mokka ",
                " grandland ",
                " grandal ",
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
                " talento kombi ", " talento 8mist ", " talento 8 mist ", " talento 9mist ", " talento 9 mist ",
                " doblo ", " qubo ", " freemont ", " multipla ", " 500l ", " fiat 500l ", " combo ", " vaneo ",
                " active tourer ", " f45 ",
                " picasso ", " grand c4 picasso ", " c4 picasso ",
                " w246 ", " b 180 ", " b180 ", " b 200 ", " b200 ", " b 250e ", " b250e ",
                " mazda 5 ",
                " grand scenic ", " grand scénic ",
                " kangoo ", " carens ", " fr-v ", " fr v ", " frv ", " express ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource,
                " shooting brake ", " gran tourer ",
                " grandtour ", " grand tour ",
                " kombi ", " combi ", " combi2", " kombi2",
                " wagon ", " sportwagon ", " avant ", " variant ", " sw ", " allroad ",
                " touring ", " turnier ", " caravan ", " estate ",
                " rs6 ", " rs 6 ", " e61 ", " f11 ", " f31 ", " g31 ",
                " alltrack ", " scout ", " outback ",
                " proceed ", " pro ceed ",
                " v40 ", " v50 ", " v60 ", " v70 ", " v 70 ", " v90 ", " v 90 ",
                " g31 ", " tipo sw ", " 206sw ", " 207sw ", " 307sw ", " 308sw ", " 407sw ", " 508sw ",
                " i40 wg ", " i40 wagon ", " i40 kombi ",
                " fiat croma ", " croma ")) {
            return "WAGON";
        }

        if (containsAny(titleSource,
                " cc ", " 206cc ", " 207cc ", " 307cc ", " 308cc ",
                " c70 ",
                " cabrio ", " kabrio ",
                " roadster ", " spyder ", " spider ",
                " convertible ", " cabriolet ",
                " sl 500 ", " sl500 ", " sl 600 ", " sl600 ", " sl 63 ", " sl63 ",
                " slk ", " slk55 ", " z4 ", " mx-5 ", " boxster ")) {
            return "CABRIO";
        }

        if (containsAny(titleSource, " audi a5 ", " a5 s-line ", " a5 s line ")) {
            return "COUPE";
        }

        if (containsAny(titleSource,
                " hatchback ", " hatch ", " spaceback ",
                " fabia ", " focus ", " golf ", " polo ",
                " i20 ", " i30 ", " i30n ", " ceed ", " mazda 2 ",
                " aveo ", " spark ", " picanto ",
                " c1 ", " c2 ", " c3 ", " c 3 ", " c4 ",
                " clio ", " megane ", " fiesta ",
                " rs3 ", " rs 3 ",
                " civic ", " insight ", " leon ", " swift ", " born ", " punto ", " panda ",
                " fiat 500e ", " fiat e500 ", " 500e ", " e500 ",
                " ec4 ", " e-c4 ", " e c4 ",
                " leaf ", " micra ", " colt ", " spacestar ", " space star ",
                " f40 ", " řada 1 ", " rada 1 ",
                " a2 ", " audi a2 ", " a180 ", " a 180 ", " a180d ", " a 180d ", " a200 ", " a 200 ", " a200d ", " a 200d ", " a35 ",
                " 116i ", " 118i ", " 120i ", " 116d ", " 118d ", " 120d ",
                " agila ", " karl ", " astra ", " corsa ", " fusion ", " starlet ", " 1007 ", " 107 ", " 147 ", " 206 ", " 207 ", " 208 ", " 308 ",
                " sandero ", " stepway ", " logan ", " scala ", " citigo ", " laguna ",
                " fiat 500 ", " fiat500 ", " tipo ", " fiat tipo ", " bravo ", " stilo ",
                " auris ", " aoris ", " prius ", " corolla ", " corsa ", " mazda 3 ", " rapid ", " yaris ", " getz ", " soul ")) {
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
                " ranger ", " hilux ", " amarok ", " alaskan ",
                " navara ", " l200 ", " l 200 ",
                " ram ", " gladiator ")) {
            return "PICKUP";
        }

        if (containsAny(titleSource,
                " sportback ", " fastback ", " liftback ",
                " sedan ", " saloon ",
                " limo ", " limousine ", " limuzína ", " limuzina ",
                " charger ",
                " giulia ", " alfa 75 ", " romeo 75 ", " alfa 156 ", " romeo 156 ", " alfa 159 ", " romeo 159 ", " alfa 166 ", " romeo 166 ",
                " cruze ", " lancer ", " primera ",
                " lexus is ",
                " s60 ", " peugeot 301 ", " talisman ",
                " octavia ", " oktavia ", " mazda 6 ", " mazdu 6 ", " superb ", " passat ", " arteon ",
                " a4 ", " a6 ", " a7 ", " a8 ", " s5 ", " s7 ", " s8 ",
                " a6c7 ",
                " s400d ", " s400 ",
                " c220 ", " c220d ", " e220 ", " e 220 ", " e280 ", " e 280 ",
                " e90 ", " e60 ", " e39 ", " f07 ", " f10 ",
                " 3 series ", " 5 series ", " 7 series ",
                " bmw 6 gt ", " 6 gt ",
                " řada 3 ", " rada 3 ", " řada 5 ", " rada 5 ", " řada 7 ", " rada 7 ",
                " 318d ", " 320d ", " 320xd ", " 330d ", " 318i ", " 320i ", " 330i ",
                " 730d ", " 730i ", " 740d ", " 740i ", " 750d ", " 750i ",
                " 540ix ", " 540i ", " 540d ", " 545i ", " gran turismo ",
                " c5 ", " mondeo ", " mondeo sedan ", " mirai ",
                " lancia kappa ", " kappa ",
                " 508 ",
                " model 3 ", " model s ",
                " cordoba ", " ds5 ",
                " eqe ", " eqs ",
                " cls ", " cla ",
                " 520 ", " 525 ", " 530 ", " 540 ",
                " 520d ", " 530d ", " 530xd ", " 540d ",
                " c180 ", " c 180 ", " c180k ", " c 180k ", " c220d ", " c 220 ", " c-class ", " e-class ",
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
        if (u.contains("kangoo") && containsAny(t, " doblo ", " fiat doblo ")) return true;

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

        if (url.contains("-lancer-")) return "MITSUBISHI";
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
        if (url.contains("-alfa-romeo-")) return "ALFA_ROMEO";
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
        if (url.contains("-lancia-")) return "LANCIA";
        if (url.contains("-mini-")) return "MINI";
        if (url.contains("-jaecoo-")) return "JAECOO";
        if (url.contains("-omoda-")) return "OMODA";
        if (url.contains("-swm-")) return "SWM";
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

        if (containsAny(source, " twingo ")) return "twingo";
        if (containsAny(source, " clio ")) return "clio";

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
        if (containsAny(source, " mazda 6 ", " mazdu 6 ")) return "mazda6";
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
                    || slug.contains("-c4-grand-picasso-") || slug.contains("-c4-grand-spacetourer-")
                    || slug.contains("-c4-picasso-");
            case "c4picasso" -> slug.contains("-c4-picasso-") || slug.contains("-picasso-");
            case "berlingo" -> slug.contains("-berlingo-");
            case "c5aircross" -> slug.contains("-c5-aircross-");
            case "c3aircross" -> slug.contains("-c3-aircross-");
            case "ds3" -> slug.contains("-ds3-") || slug.contains("-ds-3-");
            case "c3" -> slug.contains("-c3-");
            case "c4" -> slug.contains("-c4-") || slug.contains("-ec4-") || slug.contains("-e-c4-");
            case "c5" -> slug.contains("-c5-");
            case "a8" -> slug.contains("-a8-") || slug.contains("-a8l-");
            case "i10" -> slug.contains("-i10-") || slug.contains("-i-10-");
            case "ix20" -> slug.contains("-ix20-") || slug.contains("-ix-20-");
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
        String repairedTitleSource = asciiSearchText(repairMojibake(title));

        if (containsAny(titleSource, " transit custom ", " transit ")
                && !containsAny(titleSource, " tourneo custom ", " tourneo ")) {
            return true;
        }

        if (containsAny(titleSource, " nt400 ", " cabstar ")) {
            return true;
        }

        if (containsAny(titleSource, " expert ", " jumpy ", " scudo ", " proace ")
                && !containsAny(titleSource, " proace verso ", " proace city verso ", " spacetourer ", " multispace ")) {
            return true;
        }

        if (containsAny(titleSource, " citroen hy ", " citroën hy ")) {
            return true;
        }

        if (containsAny(repairedTitleSource, " nakladni ")
                && containsAny(repairedTitleSource,
                " berlingo ", " partner ", " dokker ", " doblo ", " caddy ", " kangoo ")) {
            return true;
        }

        if (containsAny(repairedTitleSource, " cargo ", " l1h1 ", " l2h1 ")
                && containsAny(repairedTitleSource,
                " berlingo ", " partner ", " dokker ", " doblo ", " caddy ", " kangoo ", " fiorino ")) {
            return true;
        }

        if (containsAny(titleSource, " qubo ")) {
            return false;
        }

        if (looksLikePassengerCarModel(title)) {
            return false;
        }

        if (containsAny(titleSource, " freemont ")) {
            return false;
        }

        if (containsAny(titleSource,
                " trafic ", " traffic ",
                " master ", " movano ", " peugeot boxer ", " jumper ", " ducato ",
                " fiorino ",
                " expert ", " jumpy ", " scudo ",
                " proace ")
                || containsAny(wordSource,
                " transporter ", " caravelle ", " carawelle ")) {
            if (containsAny(titleSource, " proace verso ", " proace city verso ", " spacetourer ", " multispace ")
                    || containsAny(wordSource, " proace verso ", " proace city verso ", " spacetourer ", " multispace ")) {
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
                " sprinter ", " crafter ", " transit ", " ducato ", " jumpy ",
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
        String source = " " + normalizeText(title + " " + text + " " + shortenForCheck(analysisText, 700))
                .toLowerCase(Locale.ROOT) + " ";
        boolean titleHasTyreSize =
                TYRE_SIZE_PATTERN.matcher(titleSource).find()
                        || TYRE_SIZE_ALT_PATTERN.matcher(titleSource).find();
        boolean titleHasRimSpec = RIM_SPEC_PATTERN.matcher(titleSource).find();
        boolean titleHasWheelWords = containsAny(titleSource,
                " pneu ",
                " pneumatiky ",
                " sada pneu ",
                " sada pneumatik ",
                " sada kol ",
                " sada 4 kol ",
                " alu kola ",
                " disky ",
                " rĂˇfky ",
                " rafky ",
                " letnĂ­ pneu ",
                " letni pneu ",
                " zimnĂ­ pneu ",
                " zimni pneu ");

        if (containsAny(titleSource,
                " rs3 ", " rs4 ", " rs5 ", " rs6 ", " rs7 ", " rsq8 ",
                " q7 ", " q5 ", " q3 ",
                " stelvio ", " giulia ", " giulietta ", " mito ", " brera ",
                " a3 ", " a4 ", " a5 ", " a6 ",
                " x1 ", " x3 ", " x5 ",
                " octavia ", " superb ", " passat ",
                " civic ", " duster ", " mustang ",
                " x-trail ", " x trail ",
                " corsa ", " e-corsa ", " e corsa ", " astra ", " insignia ",
                " 208 ", " 5008 ")) {
            return false;
        }

        if (looksLikeRealCar(title, analysisText)) {
            return false;
        }

        if (startsWithAny(titleSource,
                "pneu ",
                "pneumatiky ",
                "alu kola ",
                "disky ",
                "sada kol ",
                "sada 4 kol ",
                "sada pneu ",
                "kola ",
                "ráfky ",
                "rafky ")) {
            return true;
        }

        if (looksLikePassengerCarTitle(title) && !titleHasWheelWords && !titleHasTyreSize && !titleHasRimSpec) {
            return false;
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

    private boolean looksLikePassengerCarTitle(String title) {
        return extractBrand(title, title) != null
                && (looksLikePassengerCarModel(title)
                || extractFuelType(title) != null
                || extractTransmission(title) != null
                || extractCarType(title, title) != null
                || extractYear(title, title) != null
                || extractMileage(title, title) != null);
    }

    private boolean looksNonCarListing(String title, String text, String url, String analysisText) {
        String strongPartsSource = " " + normalizeText(title + " " + shortenForCheck(analysisText, 500))
                .toLowerCase(Locale.ROOT) + " ";
        String titleValue = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String source = " " + normalizeText(shortenForCheck(text, 450) + " " + safe(url)).toLowerCase(Locale.ROOT) + " ";
        String analysis = " " + shortenForCheck(normalizeText(analysisText).toLowerCase(Locale.ROOT), 900) + " ";
        String compactTitleValue = compactSearchText(title);
        String asciiTitleValue = asciiSearchText(title);

        if (startsWithAny(asciiTitleValue, "strecha ")
                || containsAny(asciiTitleValue, " auto pro vozickare ", " auto s rampou ", " ztp ", " plasty do masky ")) {
            return true;
        }

        if (containsAny(asciiTitleValue, " honda cbx ", " cbx 1000 ")) {
            return true;
        }

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

        if (containsAny(titleValue,
                " podtlaky ", " podtlak ",
                " vlnovec ",
                " klouby ", " kloub ",
                " svetlo ", " světlo ",
                " zrcatko ", " zrcĂˇtko ")) {
            return true;
        }

        if (containsAny(asciiTitleValue,
                " dily z ", " dil z ", " na dily ", " nahradni dily ",
                " stresni nosic ", " stresni box ", " nosic ", " pricniky ", " nd ")) {
            return true;
        }

        if (containsAny(asciiTitleValue,
                " blatnik ", " blatniky ",
                " halogen ", " halogeny ",
                " znak ", " znaky ",
                " kryty pedalu ",
                " pruziny ",
                " tlacitka ",
                " ovladace ",
                " hlavice radicky ",
                " palivovy filtr ",
                " filtr ",
                " filtru ",
                " setrvacnik ",
                " podbehy ",
                " stinitka ",
                " manzeta rucky ")) {
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

        if (looksLikePassengerCarTitle(title)
                && !containsAny(asciiTitleValue,
                " na dily ", " na nd ", " nahradni dily ", " rezervace ", " rezervovano ", " prodano ",
                " kola ", " alu kola ", " pneu ", " pneumatiky ", " disky ", " r15 ", " r16 ", " r17 ", " r18 ", " r19 ", " r20 ",
                " splitter ", " koncovky ", " vyfukove koncovky ", " svetlo ", " svetla ", " mlhova svetla ",
                " naraznik ", " mrizky ", " displej ", " infotainment ", " znak ", " listy ")) {
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

        if (containsAny(titleSource, " mustang ", " civic type r ", " cr-v ", " cr v ", " crv ", " duster ", " stelvio ", " chevrolet ssr ", " spark ", " stonic ",
                " ioniq ", " ix55 ", " volvo s90 ", " s90 ",
                " bmw 520d ", " 520d ",
                " passat ", " golf ", " tiguan ", " touareg ", " t-roc ", " troc ", " touran ")
                && !containsAny(titleSource, " na dily ", " na nd ", " nahradni dily ", " nepojizdny ", " nepojizdne ", " vadny ", " vadne ",
                " k oprave ", " na opravu ")) {
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
                " motor k.o ", " motor k.o. ", " motor ko ",
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
        String cleanTitleAscii = asciiSearchText(title);
        String cleanTitleAsciiTrimmed = cleanTitleAscii.trim();
        if (looksLikeBareBrandTitle(cleanTitleAsciiTrimmed)) {
            return true;
        }

        if (cleanTitle.startsWith("rezervace")
                || cleanTitle.startsWith("rezervov")
                || cleanTitle.startsWith("prodano")
                || cleanTitleAsciiTrimmed.startsWith("rezervace")
                || cleanTitleAsciiTrimmed.startsWith("rezervov")
                || cleanTitleAsciiTrimmed.startsWith("prodano")) {
            return true;
        }

        if (containsAny(cleanTitle, "prodam nebo vymenim", "prodám nebo vyměním", " rezervováno ", " rezervovano ",
                " rezervace ")
                || containsAny(cleanTitleAscii, " prodam nebo vymenim ", " rezervovano ", " rezervace ")) {
            return true;
        }

        if (cleanTitle.equals("prodam")
                || cleanTitle.equals("prodám")
                || cleanTitle.equals("prodej")
                || cleanTitle.equals("na prodej")
                || cleanTitle.equals("auto")
                || cleanTitleAsciiTrimmed.equals("prodam avto")
                || cleanTitleAsciiTrimmed.equals("prodam auto")) {
            return true;
        }

        String source = " " + normalizeText(title + " " + shortenForCheck(text, 500)).toLowerCase(Locale.ROOT) + " ";
        String sourceAscii = asciiSearchText(title + " " + shortenForCheck(text, 500));

        if (containsAny(source,
                " na splĂˇtky ", " na splatky ",
                " bez registru ",
                " akontace ",
                " pĹ™enechĂˇm splĂˇtky ", " prenecham splatky ",
                " pĹ™evezmu leasing ", " prevezmu leasing ",
                " leasing pĹ™evezmu ", " leasing prevezmu ")
                || containsAny(sourceAscii,
                " na splatky ",
                " bez registru ",
                " akontace ",
                " prenecham splatky ",
                " prevezmu leasing ",
                " leasing prevezmu ")) {
            return true;
        }

        if (looksLikePassengerCarModel(title)
                && !containsAny(cleanTitleAscii, " prodano ", " zalohovano ", " rezervace ", " rezervovano ", " zadano ")) {
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
                " zadáno ", " zadano ")
                || containsAny(sourceAscii,
                " na splatky ",
                " bez registru ",
                " akontace ",
                " exekuce ",
                " insolvence ",
                " drazba ",
                " prenecham splatky ",
                " prevezmu leasing ",
                " leasing prevezmu ",
                " bez prepisu ",
                " bez stk ",
                " bez tp ",
                " soubor nahradnich dilu ",
                " jen celek ",
                " prodano ",
                " zalohovano ",
                " rezervovano ", " rezervace ",
                " zadano ");
    }

    private boolean looksLikeBareBrandTitle(String cleanTitleAscii) {
        return cleanTitleAscii.equals("alfa")
                || cleanTitleAscii.equals("alfa romeo")
                || cleanTitleAscii.equals("bmw")
                || cleanTitleAscii.equals("audi")
                || cleanTitleAscii.equals("mercedes")
                || cleanTitleAscii.equals("volkswagen")
                || cleanTitleAscii.equals("vw")
                || cleanTitleAscii.equals("toyota")
                || cleanTitleAscii.equals("skoda")
                || cleanTitleAscii.equals("opel")
                || cleanTitleAscii.equals("nissan")
                || cleanTitleAscii.equals("citroen")
                || cleanTitleAscii.equals("renault")
                || cleanTitleAscii.equals("ford")
                || cleanTitleAscii.equals("honda")
                || cleanTitleAscii.equals("hyundai")
                || cleanTitleAscii.equals("kia")
                || cleanTitleAscii.equals("mazda");
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

    private Integer parseShortYearCandidate(String raw) {
        try {
            int value = Integer.parseInt(raw);
            int currentYear = java.time.Year.now().getValue();
            int currentCentury = (currentYear / 100) * 100;
            int year = currentCentury + value;

            if (year > currentYear) {
                year -= 100;
            }

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

    private String repairMojibake(String value) {
        if (value == null || value.isBlank() || (!looksLikeMojibake(value) && mojibakeScore(value) == 0)) {
            return value;
        }

        String current = value;
        try {
            for (int attempt = 0; attempt < 3; attempt++) {
                if (!looksLikeMojibake(current) && mojibakeScore(current) == 0) {
                    break;
                }

                byte[] bytes = encodeMojibakeBytes(current);
                String repaired = StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes))
                        .toString();

                if (mojibakeScore(repaired) < mojibakeScore(current)
                        || (looksLikeMojibake(current) && !looksLikeMojibake(repaired))) {
                    current = normalizeText(repaired);
                } else {
                    break;
                }
            }
            return current;
        } catch (Exception e) {
            return current;
        }
    }

    private byte[] encodeMojibakeBytes(String value) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(value.length());
        Charset windows1250 = Charset.forName("windows-1250");

        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            String ch = new String(Character.toChars(codePoint));

            if (codePoint <= 0xFF) {
                out.write(codePoint);
            } else {
                try {
                    ByteBuffer encoded = windows1250
                            .newEncoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .encode(CharBuffer.wrap(ch));
                    while (encoded.hasRemaining()) {
                        out.write(encoded.get() & 0xFF);
                    }
                } catch (Exception e) {
                    out.write(ch.getBytes(StandardCharsets.UTF_8));
                }
            }

            offset += Character.charCount(codePoint);
        }

        return out.toByteArray();
    }

    private boolean looksLikeMojibake(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            if (codePoint == 0x0102 // Ă
                    || codePoint == 0x0139 // Ĺ
                    || codePoint == 0x00C4 // Ä
                    || codePoint == 0x00C2 // Â
                    || codePoint == 0x015A // Ś
                    || codePoint == 0x017B // Ż
                    || codePoint == 0x013E // ľ
                    || codePoint == 0x0165 // ť
                    || codePoint == 0x02C7 // ˇ
                    || codePoint == 0x02DD // ˝
                    || codePoint == 0x2030 // ‰
                    || (codePoint >= 0x0080 && codePoint <= 0x009F)) {
                return true;
            }
            offset += Character.charCount(codePoint);
        }

        return false;
    }

    private int mojibakeScore(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        int score = 0;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            if (codePoint == '\u0102'
                    || codePoint == '\u00C4'
                    || codePoint == '\u0139'
                    || codePoint == '\u00C2'
                    || codePoint == '\u00E2'
                    || codePoint == '\uFFFD') {
                score++;
            }
            offset += Character.charCount(codePoint);
        }
        return score;
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
        source = source + " " + asciiSearchText(title);

        return containsAny(source,
                " rav4 ",
                " rav 4 ",
                " land cruiser ",
                " yeti ",
                " samurai ",
                " x1 ",
                " x2 ",
                " x3 ",
                " x4 ",
                " x5 ",
                " x6 ",
                " x7 ",
                " giulia ",
                " giulietta ",
                " giuletta ",
                " mito ",
                " stelvio ",
                " tonale ",
                " brera ",
                " alfetta ",
                " alfa 75 ",
                " romeo 75 ",
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
                " insight ",
                " civic ",
                " jazz ",
                " cr-v ",
                " crv ",
                " hr-v ",
                " hrv ",
                " rs6 ",
                " rs 6 ",
                " a180 ", " a 180 ", " a180d ", " a 180d ", " a200 ", " a 200 ", " a200d ", " a 200d ", " a35 ",
                " c180 ", " c 180 ", " c180k ", " c 180k ", " c220 ", " c 220 ", " c220d ", " e220 ", " e 220 ", " e220d ", " e300 ", " e 300 ", " e300cdi ",
                " cla ", " cls ", " slk ", " slk55 ", " sl600 ", " glc ", " gle ", " gls ", " gl500 ", " grandis ",
                " c63 ", " c 63 ",
                " mirai ",
                " outlander ", " pajero ", " l200 ", " l 200 ", " lancer ", " eclipse ", " asx ", " colt ", " spacestar ", " space star ", " i-miev ", " i miev ", " imiev ",
                " m3 ", " e36 ",
                " g20 ", " g21 ", " g30 ", " g31 ", " f10 ", " f11 ", " f30 ", " f31 ",
                " 116i ", " 118i ", " 120i ", " 218i ", " 220i ",
                " 318i ", " 320i ", " 330i ", " 335i ", " 540ix ",
                " 116d ", " 118d ", " 120d ", " 218d ", " 220d ",
                " 318d ", " 320d ", " 320xd ", " 330d ", " 335d ", " 520d ", " 530d ", " 530xd ", " 740d ", " 750xd ",
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
                " i20 ",
                " i30 ",
                " i40 ",
                " ix20 ",
                " ix35 ",
                " h1 ",
                " h-1 ",
                " kona ",
                " freemont ",
                " explorer ",
                " duster ",
                " bigster ",
                " fiat 500 ",
                " 500e ",
                " 500c ",
                " fabia ",
                " scala ",
                " golf ",
                " octavia ",
                " superb ",
                " passat ",
                " arteon ",
                " touran ",
                " t-roc ",
                " troc ",
                " id.4 ",
                " id4 ",
                " id.5 ",
                " id5 ",
                " tiguan ",
                " touareg ",
                " kodiaq ",
                " karoq ",
                " kamiq ",
                " rav ",
                " qashqai ",
                " juke ",
                " x-trail ",
                " pathfinder ",
                " terrano ",
                " pixo ",
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
                " astra ",
                " corsa ",
                " e-corsa ",
                " e corsa ",
                " karl ",
                " 1007 ",
                " 206 ",
                " 207 ",
                " 208 ",
                " talisman ",
                " thalia ",
                " e-tron ",
                " etron ",
                " s4 quattro ",
                " lexus is ",
                " volkswagen up ",
                " vw up ",
                " q7 ",
                " q5 ",
                " q4 ",
                " mazdu 6 ",
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

    private String asciiSearchText(String value) {
        String withoutMarks = Normalizer.normalize(normalizeText(value).toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String compact = withoutMarks
                .replaceAll("[^a-z0-9]+", " ")
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

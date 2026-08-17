package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private static final int MAX_CONSECUTIVE_FORBIDDEN = 5;

    private static final int CURRENT_YEAR = Year.now().getValue();
    private static final int MIN_YEAR = 1990;
    private static final int MIN_TITLE_YEAR = 1900;
    private static final int MAX_REASONABLE_PRICE = 10_000_000;
    private static final int MAX_LIST_FALLBACK_PRICE = 5_000_000;

    @Override
    public String getSourceName() {
        return "TIPCARS";
    }

    @Override
    public List<CarDto> fetchCars() {
        List<CarDto> cars = new ArrayList<>();
        Set<String> detailLinks = new LinkedHashSet<>();
        Map<String, ListListing> listListings = new HashMap<>();
        Map<String, String> cookies = new HashMap<>();

        int missingPriceCount = 0;
        int invalidPriceCount = 0;
        int brokenCount = 0;
        int commercialVehicleCount = 0;
        int parseExceptionCount = 0;
        int forbiddenCount = 0;
        int consecutiveForbiddenCount = 0;

        try {
            for (int page = 1; page <= MAX_LIST_PAGES; page++) {
                String pageUrl = buildPageUrl(page);

                try {
                    int before = detailLinks.size();

                    Connection.Response listResponse = connect(pageUrl, cookies, BASE_URL).execute();
                    cookies.putAll(listResponse.cookies());
                    Document listDoc = listResponse.parse();
                    Map<String, ListListing> pageListings = extractListListings(listDoc);
                    Set<String> pageLinks = extractDetailLinks(listDoc);
                    pageListings.forEach(listListings::putIfAbsent);
                    detailLinks.addAll(pageListings.keySet());
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
            Set<String> ambiguousDetailIds = findAmbiguousDetailIds(detailLinks);
            if (!ambiguousDetailIds.isEmpty()) {
                log.warn("TIPCARS found ambiguous detail ids count={} ids={}",
                        ambiguousDetailIds.size(), ambiguousDetailIds);
            }

            for (String url : detailLinks) {
                String detailId = extractDetailId(url);
                if (detailId != null && ambiguousDetailIds.contains(detailId)) {
                    brokenCount++;
                    log.warn("TIPCARS SKIP url={} reason=ambiguous_detail_id detail_id={}",
                            safe(url), safe(detailId));
                    continue;
                }

                ParseResult result = parseDetail(url, cookies, listListings.get(url));

                if (result.car() != null) {
                    cars.add(result.car());
                    consecutiveForbiddenCount = 0;
                } else {
                    switch (result.reason()) {
                        case "missing_price" -> missingPriceCount++;
                        case "invalid_price" -> invalidPriceCount++;
                        case "broken_listing" -> brokenCount++;
                        case "commercial_vehicle" -> commercialVehicleCount++;
                        case "parse_exception" -> parseExceptionCount++;
                        case "forbidden" -> {
                            forbiddenCount++;
                            consecutiveForbiddenCount++;
                        }
                        default -> brokenCount++;
                    }
                    if (!"forbidden".equals(result.reason())) {
                        consecutiveForbiddenCount = 0;
                    }
                }

                if (consecutiveForbiddenCount >= MAX_CONSECUTIVE_FORBIDDEN) {
                    log.warn("TIPCARS detail parsing stopped reason=too_many_forbidden consecutive_forbidden={} parsed_so_far={}",
                            consecutiveForbiddenCount, cars.size());
                    break;
                }

                sleepQuietly("forbidden".equals(result.reason()) ? 1_500 : 250);
            }

        } catch (Exception e) {
            log.warn("TIPCARS fetch failed error={}", safe(e.getMessage()));
        }

        log.info("TIPCARS parsed {} cars", cars.size());
        log.info("TIPCARS SUMMARY parsed={} broken_listing={} commercial_vehicle={} missing_price={} invalid_price={} parse_exception={} forbidden={}",
                cars.size(), brokenCount, commercialVehicleCount, missingPriceCount, invalidPriceCount, parseExceptionCount, forbiddenCount);

        return cars;
    }

    private ParseResult parseDetail(String url, Map<String, String> cookies, ListListing listListing) {
        try {
            Connection.Response detailResponse = connect(url, cookies, BASE_LIST_URL).execute();
            cookies.putAll(detailResponse.cookies());
            String finalUrl = normalizeDetailUrl(detailResponse.url() == null ? null : detailResponse.url().toString());
            if (!isValidDetailLink(finalUrl)) {
                log.warn("TIPCARS SKIP url={} reason=detail_redirected_to_non_detail final_url={}",
                        safe(url), safe(finalUrl));
                return new ParseResult(null, "broken_listing");
            }

            Document doc = detailResponse.parse();

            String title = extractTitle(doc);
            String pageText = normalizeText(doc.text());

            if (title == null || title.isBlank()
                    || isJunkTitle(title)
                    || isJunkUrl(finalUrl)
                    || isJunkText(pageText)) {
                log.warn("TIPCARS SKIP url={} reason=broken_listing title={}", safe(finalUrl), safe(title));
                return new ParseResult(null, "broken_listing");
            }

            if (looksTitleUrlBrandMismatch(title, finalUrl)) {
                log.warn("TIPCARS SKIP url={} reason=title_url_brand_mismatch title={}", safe(finalUrl), safe(title));
                return new ParseResult(null, "broken_listing");
            }

            if (looksCommercialOrCamperListing(title, finalUrl, pageText)) {
                log.info("TIPCARS SKIP url={} reason=commercial_vehicle title={}", safe(finalUrl), safe(title));
                return new ParseResult(null, "commercial_vehicle");
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
            String imageUrl = extractImageUrl(doc, title);
            String brand = extractBrand(title, finalUrl);
            String fuelType = firstNonBlank(
                    extractFuelType(title),
                    extractFuelType(finalUrl),
                    extractFuelType(pageText)
            );

            String transmission = firstNonBlank(
                    extractTransmission(title),
                    extractTransmissionFromSpecs(doc),
                    extractTransmission(finalUrl),
                    "ELECTRIC".equals(fuelType) ? "AUTOMATIC" : null
            );

            String carType = extractCarType(title, "", finalUrl);
            String outputTitle = repairMojibake(title);
            String outputLocation = repairMojibake(location);

            CarDto car = new CarDto();
            car.setSource("TIPCARS");
            car.setTitle(outputTitle);
            car.setPrice(formatPrice(priceValue));
            car.setPriceValue(priceValue);
            car.setLocation(outputLocation);
            car.setUrl(finalUrl);
            car.setImageUrl(imageUrl);
            car.setBrand(brand);
            car.setYear(year);
            car.setMileage(mileage);
            car.setFuelType(fuelType);
            car.setTransmission(transmission);
            car.setCarType(carType);

            log.info("TIPCARS CAR title='{}' price={} location={} year={} mileage={} fuelType={} transmission={} carType={} brand={} url={}",
                    safe(outputTitle),
                    priceValue,
                    safe(outputLocation),
                    year,
                    mileage,
                    safe(fuelType),
                    safe(transmission),
                    safe(carType),
                    safe(brand),
                    safe(finalUrl));

            return new ParseResult(car, "ok");

        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 403) {
                ParseResult fallback = parseListListingFallback(url, listListing);
                if (fallback.car() != null) {
                    log.warn("TIPCARS detail forbidden url={} status=403 using=list_card_fallback", safe(url));
                    return fallback;
                }
                log.warn("TIPCARS SKIP url={} reason=forbidden status=403", safe(url));
                return new ParseResult(null, "forbidden");
            }
            log.warn("TIPCARS SKIP url={} reason=parse_exception status={} error={}",
                    safe(url), e.getStatusCode(), safe(e.getMessage()));
            return new ParseResult(null, "parse_exception");
        } catch (Exception e) {
            log.warn("TIPCARS SKIP url={} reason=parse_exception error={}", safe(url), safe(e.getMessage()));
            return new ParseResult(null, "parse_exception");
        }
    }

    private ParseResult parseListListingFallback(String url, ListListing listing) {
        if (listing == null) {
            return new ParseResult(null, "forbidden");
        }

        String title = cleanupListTitle(cleanupTitle(normalizeText(listing.title())));
        String listText = normalizeText(listing.text());

        if (title == null || title.isBlank()
                || isJunkTitle(title)
                || isJunkUrl(url)
                || isJunkText(listText)
                || looksTitleUrlBrandMismatch(title, url)
                || looksCommercialOrCamperListing(title, url, listText)) {
            return new ParseResult(null, "forbidden");
        }

        Integer priceValue = extractFirstPrice(listText);
        if (!isValidListFallbackPrice(priceValue)) {
            return new ParseResult(null, "forbidden");
        }

        String fuelType = firstNonBlank(
                extractFuelType(title),
                extractFuelType(url),
                extractFuelType(listText)
        );
        String transmission = firstNonBlank(
                extractTransmission(title),
                extractTransmission(url),
                extractTransmission(listText),
                "ELECTRIC".equals(fuelType) ? "AUTOMATIC" : null
        );

        CarDto car = new CarDto();
        car.setSource("TIPCARS");
        car.setTitle(repairMojibake(title));
        car.setPrice(formatPrice(priceValue));
        car.setPriceValue(priceValue);
        car.setLocation(repairMojibake(extractLocationFromText(listText)));
        car.setUrl(url);
        car.setImageUrl(listing.imageUrl());
        car.setBrand(extractBrand(title, url));
        car.setYear(extractYear(listText, title));
        car.setMileage(extractMileage(listText));
        car.setFuelType(fuelType);
        car.setTransmission(transmission);
        car.setCarType(extractCarType(title, listText, url));

        log.info("TIPCARS CAR title='{}' price={} location={} year={} mileage={} fuelType={} transmission={} carType={} brand={} url={} source=list_card_fallback",
                safe(car.getTitle()),
                priceValue,
                safe(car.getLocation()),
                car.getYear(),
                car.getMileage(),
                safe(car.getFuelType()),
                safe(car.getTransmission()),
                safe(car.getCarType()),
                safe(car.getBrand()),
                safe(url));

        return new ParseResult(car, "ok");
    }

    private Connection connect(String url, Map<String, String> cookies, String referrer) {
        Connection connection = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .referrer(referrer == null || referrer.isBlank() ? BASE_URL : referrer)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "cs-CZ,cs;q=0.9,en;q=0.8")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Upgrade-Insecure-Requests", "1")
                .timeout(REQUEST_TIMEOUT_MS)
                .followRedirects(true);

        if (cookies != null && !cookies.isEmpty()) {
            connection.cookies(cookies);
        }

        return connection;
    }

    private String buildPageUrl(int page) {
        if (page <= 1) {
            return BASE_LIST_URL;
        }
        return BASE_URL + "?str=" + page + "-20";
    }

    private Set<String> extractDetailLinks(Document listDoc) {
        Set<String> links = new LinkedHashSet<>();

        for (Element a : listDoc.select("a[href]")) {
            String href = normalizeDetailUrl(a.absUrl("href"));

            if (!isValidDetailLink(href)) {
                continue;
            }

            href = href.replaceAll("[?#].*$", "");
            links.add(href);
        }

        return links;
    }

    private Map<String, ListListing> extractListListings(Document listDoc) {
        Map<String, ListListing> listings = new HashMap<>();

        for (Element a : listDoc.select("a[href]")) {
            String href = normalizeDetailUrl(a.absUrl("href"));
            if (!isValidDetailLink(href) || listings.containsKey(href)) {
                continue;
            }

            Element card = findListingCard(a);
            if (card == null) {
                continue;
            }

            String cardText = normalizeText(card.text());
            if (extractFirstPrice(cardText) == null) {
                continue;
            }

            String title = extractListTitle(card, a);
            if (title == null || title.isBlank() || isJunkTitle(title)) {
                continue;
            }

            listings.put(href, new ListListing(href, title, cardText, extractListImageUrl(card)));
        }

        return listings;
    }

    private Element findListingCard(Element link) {
        String targetHref = normalizeDetailUrl(link.absUrl("href"));
        Element current = link;
        for (int depth = 0; current != null && depth < 7; depth++, current = current.parent()) {
            String text = normalizeText(current.text());
            if (text.length() >= 30
                    && text.length() <= 2500
                    && extractFirstPrice(text) != null
                    && containsOnlyTargetDetailLink(current, targetHref)) {
                return current;
            }
        }
        return null;
    }

    private boolean containsOnlyTargetDetailLink(Element element, String targetHref) {
        if (element == null || targetHref == null || targetHref.isBlank()) {
            return false;
        }

        Set<String> detailHrefs = new LinkedHashSet<>();
        for (Element nestedLink : element.select("a[href]")) {
            String href = normalizeDetailUrl(nestedLink.absUrl("href"));
            if (isValidDetailLink(href)) {
                detailHrefs.add(href);
            }
        }

        return detailHrefs.size() == 1 && detailHrefs.contains(targetHref);
    }

    private String extractListTitle(Element card, Element link) {
        for (Element el : card.select("h1, h2, h3, [class*=title], [class*=name]")) {
            String text = cleanupListTitle(cleanupTitle(normalizeText(el.text())));
            if (isLikelyListTitle(text)) {
                return text;
            }
        }

        String linkText = cleanupListTitle(cleanupTitle(normalizeText(link.text())));
        if (isLikelyListTitle(linkText)) {
            return linkText;
        }

        return null;
    }

    private boolean isLikelyListTitle(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalizeText(value);
        if (normalized.length() < 6 || normalized.length() > 180) {
            return false;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        return !containsAny(lower, "detail", "vice info", "vĂ­ce info", "zobrazit", "foto", "reklama", "tipcars");
    }

    private String extractListImageUrl(Element card) {
        Element img = card.selectFirst("img[src], img[data-src], img[data-original], img[srcset], img[data-srcset]");
        if (img == null) {
            return null;
        }

        String src = firstNonBlank(
                img.absUrl("src"),
                img.absUrl("data-src"),
                img.absUrl("data-original"),
                firstSrcsetUrl(img.attr("srcset")),
                firstSrcsetUrl(img.attr("data-srcset"))
        );
        return src == null || src.isBlank() ? null : src;
    }

    private String firstSrcsetUrl(String srcset) {
        if (srcset == null || srcset.isBlank()) {
            return null;
        }
        String first = srcset.split(",")[0].trim().split("\\s+")[0].trim();
        return first.isBlank() ? null : first;
    }

    private String normalizeDetailUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("[?#].*$", "");
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

    private Set<String> findAmbiguousDetailIds(Set<String> urls) {
        Map<String, Set<String>> urlsById = new HashMap<>();

        if (urls == null || urls.isEmpty()) {
            return Set.of();
        }

        for (String url : urls) {
            String detailId = extractDetailId(url);
            if (detailId == null) {
                continue;
            }
            urlsById.computeIfAbsent(detailId, ignored -> new LinkedHashSet<>()).add(normalizeDetailUrl(url));
        }

        Set<String> ambiguousIds = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> entry : urlsById.entrySet()) {
            if (entry.getValue().size() > 1) {
                ambiguousIds.add(entry.getKey());
            }
        }

        return ambiguousIds;
    }

    private String extractDetailId(String url) {
        String normalized = normalizeDetailUrl(url);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }

        Matcher matcher = Pattern.compile("-(\\d+)\\.html$", Pattern.CASE_INSENSITIVE).matcher(normalized);
        return matcher.find() ? matcher.group(1) : null;
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

    private String cleanupListTitle(String title) {
        if (title == null) {
            return null;
        }

        return normalizeText(title
                .replaceAll("(?i)(\\b(?:\\S*ada|\\S*idy)\\s+\\d)\\s+\\d{3}(?:[\\s\\u00A0]\\d{3})+\\s*K(?![mw])\\p{L}{0,2}(?:\\s+bez\\s+DPH)?", "$1")
                .replaceAll("(?i)(x\\d)\\s+\\d{3}(?:[\\s\\u00A0]\\d{3})+\\s*K(?![mw])\\p{L}{0,2}(?:\\s+bez\\s+DPH)?", "$1")
                .replaceAll("(?i)\\s+\\d{1,3}(?:[\\s\\u00A0]\\d{3})+\\s*K(?![mw])\\p{L}{0,2}(?:\\s+bez\\s+DPH)?", " ")
                .replaceAll("(?i)\\s+\\d{4,8}\\s*K(?![mw])\\p{L}{0,2}(?:\\s+bez\\s+DPH)?", " ")
                .trim());
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

        Matcher matcher = Pattern.compile("(?<!\\d)(\\d{1,3}(?:[\\s\\u00A0]\\d{3})+|\\d{4,8})\\s*K(?![mw])\\p{L}{0,2}", Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) {
            String rawPrice = matcher.group(1);
            if (startsWithModelSeriesNumber(text, matcher.start(), rawPrice)
                    || startsWithDriveNumber(text, matcher.start(), rawPrice)) {
                rawPrice = rawPrice.replaceFirst("^\\d{1,2}[\\s\\u00A0]+", "");
            }
            Integer parsed = parseIntSafe(rawPrice);
            if (parsed != null && parsed > 0) {
                return parsed;
            }
        }

        return null;
    }

    private boolean isValidListFallbackPrice(Integer priceValue) {
        return priceValue != null && priceValue > 0 && priceValue <= MAX_LIST_FALLBACK_PRICE;
    }

    private boolean startsWithModelSeriesNumber(String text, int matchStart, String rawPrice) {
        if (text == null || rawPrice == null || !rawPrice.matches("^\\d{1,2}[\\s\\u00A0]+\\d{3}.*")) {
            return false;
        }

        String before = normalizeText(text.substring(0, Math.max(0, matchStart))).toLowerCase(Locale.ROOT);
        return before.matches(".*\\b(\\S*ada|\\S*idy)\\s*$");
    }

    private boolean startsWithDriveNumber(String text, int matchStart, String rawPrice) {
        if (text == null || rawPrice == null || !rawPrice.matches("^\\d[\\s\\u00A0]+\\d{3}.*")) {
            return false;
        }

        int beforeIndex = matchStart - 1;
        return beforeIndex >= 0 && Character.toLowerCase(text.charAt(beforeIndex)) == 'x';
    }

    private Integer extractYear(String text, String title) {
        Integer titleYear = extractTitleYear(title);
        if (titleYear != null) {
            return titleYear;
        }

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
            String rawYear = generic.group(1);
            String lower = combined.toLowerCase(Locale.ROOT);

            if (("2008".equals(rawYear) && lower.contains("peugeot 2008"))
                    || ("3008".equals(rawYear) && lower.contains("peugeot 3008"))
                    || ("5008".equals(rawYear) && lower.contains("peugeot 5008"))) {
                continue;
            }

            Integer year = parseIntSafe(rawYear);
            if (isValidYear(year)) {
                return year;
            }
        }

        return null;
    }

    private Integer extractTitleYear(String title) {
        String normalizedTitle = normalizeText(safe(title));
        Matcher matcher = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(normalizedTitle);

        while (matcher.find()) {
            String rawYear = matcher.group(1);
            String lower = normalizedTitle.toLowerCase(Locale.ROOT);
            if (("2008".equals(rawYear) && lower.contains("peugeot 2008"))
                    || ("3008".equals(rawYear) && lower.contains("peugeot 3008"))
                    || ("5008".equals(rawYear) && lower.contains("peugeot 5008"))) {
                continue;
            }

            Integer year = parseIntSafe(rawYear);
            if (year != null && year >= MIN_TITLE_YEAR && year <= CURRENT_YEAR + 1) {
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

    private String extractLocationFromText(String pageText) {
        Matcher cityMatcher = Pattern.compile("(?i)(praha|brno|ostrava|plzen|liberec|olomouc|pardubice|hradec kralove|ceske budejovice|usti nad labem|zlin|jihlava|karlovy vary|opava|kladno|mlada boleslav|teplice|most|cheb|trutnov|kolin|karvina|blansko)")
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

    private String extractImageUrl(Document doc, String title) {
        Element og = doc.selectFirst("meta[property=og:image]");
        if (og != null) {
            String value = normalizeText(og.attr("content"));
            if (isUsableDetailImageUrl(value)) {
                return value;
            }
        }

        for (Element img : doc.select("img[src], img[data-src], img[data-original], img[srcset], img[data-srcset]")) {
            String value = firstNonBlank(
                    img.absUrl("src"),
                    img.absUrl("data-src"),
                    img.absUrl("data-original"),
                    firstSrcsetUrl(img.attr("srcset")),
                    firstSrcsetUrl(img.attr("data-srcset"))
            );

            if (isUsableDetailImageUrl(value) && isImageTextCompatibleWithTitle(img, title)) {
                return value;
            }
        }

        return null;
    }

    private boolean isUsableDetailImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return false;
        }

        String lower = imageUrl.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false;
        }

        if (!lower.matches(".*\\.(jpg|jpeg|png|webp)(\\?.*)?$")) {
            return false;
        }

        return !containsAny(lower,
                "logo",
                "icon",
                "favicon",
                "avatar",
                "placeholder",
                "blank",
                "banner",
                "reklama",
                "advert",
                "sprite");
    }

    private boolean isImageTextCompatibleWithTitle(Element img, String title) {
        String imageText = normalizeText(firstNonBlank(
                img.attr("alt"),
                img.attr("title"),
                img.attr("aria-label")
        ));

        if (imageText.isBlank()) {
            return true;
        }

        String titleBrand = extractBrand(title, null);
        String imageBrand = extractBrand(imageText, null);

        return titleBrand == null || imageBrand == null || titleBrand.equals(imageBrand);
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

        if (containsAny(" " + source.toLowerCase(Locale.ROOT) + " ",
                " lotus ", " emira ")) return "LOTUS";
        if (containsAny(" " + source.toLowerCase(Locale.ROOT) + " ",
                " lamborghini ", " huracán ", " huracan ")) return "LAMBORGHINI";
        if (containsAny(" " + source.toLowerCase(Locale.ROOT) + " ",
                " ferrari ", " california ")) return "FERRARI";

        return normalizeBrand(words[0]);
    }

    private boolean looksTitleUrlBrandMismatch(String title, String url) {
        String titleBrand = extractBrand(title, null);
        String urlBrand = extractBrandFromUrl(url);

        return titleBrand != null
                && urlBrand != null
                && !titleBrand.equals(urlBrand);
    }

    private String extractBrandFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String normalizedUrl = url.toLowerCase(Locale.ROOT);
        Matcher matcher = Pattern.compile("tipcars\\.com/([^/]+)/").matcher(normalizedUrl);
        if (!matcher.find()) {
            return null;
        }

        String slug = matcher.group(1).replace('-', ' ');
        return normalizeBrand(slug);
    }

    private String extractFuelType(String text) {
        String source = " " + normalizeText(safe(text)).toLowerCase(Locale.ROOT) + " ";
        String compact = source.replaceAll("[^a-z0-9]", "");
        String tokens = " " + source.replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim() + " ";

        if (containsAny(source, "/lpg/", " lpg ") || compact.contains("lpg")) {
            return "LPG";
        }

        if (containsAny(source, " eco-g ", " eco g ") || compact.contains("ecog")) {
            return "LPG";
        }

        if (containsAny(source, "/cng/", " cng ") || compact.contains("cng")) {
            return "CNG";
        }

        if (containsAny(source,
                " plug-in ",
                " plug in ",
                " plugin ",
                " phev ",
                " 225xe ",
                " 30e ",
                " 300 de ",
                " 350 de ",
                " bmw xm ",
                " e 300 e ",
                " ehybrid ")
                || compact.contains("plugin")
                || compact.contains("pluginhybrid")
                || compact.contains("phev")
                || (containsAny(tokens, " iv ") && containsAny(source, " superb "))
                || compact.contains("225xe")
                || compact.contains("xdrive30e")
                || compact.contains("sdrive30e")
                || compact.contains("xdrive25e")
                || compact.contains("sdrive25e")
                || compact.contains("350de")
                || compact.contains("300de")
                || compact.contains("30e")
                || compact.contains("e300e")) {
            return "PLUGIN_HYBRID";
        }

        if (containsAny(source,
                "/elektro/",
                " elektro ",
                " electric ",
                " elektromobil ",
                " kwh ",
                " bev ",
                " id.3 ",
                " id.4 ",
                " id.5 ",
                " tesla ",
                " enyaq ",
                " mg4 ")) {
            return "ELECTRIC";
        }

        if (containsAny(source,
                "/hybridni-benzin/",
                "/hybridni-nafta/",
                " hybrid ",
                " hybridni ",
                " hybridní ",
                " plug-in ",
                " plugin ",
                " phev ",
                " hev ",
                " mhev ",
                " m-hev ",
                " e-tec ",
                " etec ",
                " b5 ",
                " b6 ",
                " shs ",
                " e-hybrid ",
                " e hybrid ",
                " e-cvt ",
                " ecvt ")
                || compact.contains("hybrid")
                || compact.contains("400h")
                || compact.contains("mhev")
                || containsAny(tokens, " hev ")
                || (compact.contains("etec") && !compact.contains("puretech"))
                || source.matches(".*\\bvolvo\\b.*\\bb[3-6]\\b.*")
                || compact.contains("ehev")) {
            return "HYBRID";
        }

        if (containsAny(source,
                "/nafta/",
                " nafta ",
                " diesel ",
                " tdi ",
                " tdci ",
                " cdi ",
                " dci ",
                " hdi ",
                " crdi ",
                " cdti ",
                " bluehdi ")) {
            return "DIESEL";
        }

        if (compact.contains("tdi")
                || compact.contains("tdci")
                || compact.contains("cdi")
                || compact.contains("dci")
                || compact.contains("hdi")
                || compact.contains("crdi")
                || compact.contains("cdti")
                || compact.contains("bluehdi")) {
            return "DIESEL";
        }

        if (compact.contains("220d")
                || compact.contains("200d")
                || compact.contains("300d")
                || compact.contains("530d")
                || compact.contains("520d")
                || compact.contains("d4d")) {
            return "DIESEL";
        }

        if (source.matches(".*\\b[0-9][.,][0-9]\\s*d\\b.*")) {
            return "DIESEL";
        }

        if (containsAny(source,
                "/benzin/",
                " benzin ",
                " benzín ",
                " tsi ",
                " tfsi ",
                " mpi ",
                " gdi ",
                " tgdi ",
                " t-gdi ",
                " tce ",
                " ecoboost ",
                " puretech ",
                " skyactiv-g ",
                " vvt-i ")) {
            return "PETROL";
        }

        if (compact.contains("tsi")
                || compact.contains("tfsi")
                || compact.contains("mpi")
                || compact.contains("gdi")
                || compact.contains("tgdi")
                || compact.contains("tce")
                || compact.contains("ecoboost")
                || compact.contains("puretech")
                || compact.contains("skyactivg")) {
            return "PETROL";
        }

        return null;
    }

    private String extractTransmissionFromSpecs(Document doc) {
        for (Element el : doc.select("tr, li, dl, [class*=param], [class*=spec], [class*=tech], [class*=info], [class*=data], [class*=attr]")) {
            String text = normalizeText(el.text());
            if (text.isBlank() || text.length() > 140) {
                continue;
            }

            String lower = text.toLowerCase(Locale.ROOT);
            if (containsAny(lower,
                    "p\u0159evodovka",
                    "prevodovka",
                    "p\u0159evod",
                    "prevod")) {
                String transmission = extractTransmission(text);
                if (transmission != null) {
                    return transmission;
                }
            }
        }

        return null;
    }

    private String extractTransmission(String text) {
        String normalized = " " + normalizeText(text).toLowerCase(Locale.ROOT) + " ";
        String tokens = " " + normalized.replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim() + " ";

        if (containsAny(normalized,
                " automat ",
                " automatic ",
                " dsg ",
                " dsg7 ",
                " eat8 ",
                " eat6 ",
                " e-dcs6 ",
                " edcs6 ",
                " e-dcs ",
                " edcs ",
                " dct ",
                " 7dct ",
                " 8g-dct ",
                " 8g dct ",
                " 7g-tronic ",
                " 9g-tronic ",
                " tiptronic ",
                " s tronic ",
                " stronic ",
                " steptronic ",
                " multitronic ",
                " powershift ",
                " x-tronic ",
                " xtronic ",
                " cvt ",
                " e-cvt ",
                " ecvt ")
                || containsAny(tokens,
                " aut ",
                " automat ",
                " automatic ",
                " autom ",
                " at ",
                " at6 ",
                " at7 ",
                " at8 ",
                " at9 ",
                " 6at ",
                " 7at ",
                " 8at ",
                " 9at ")) {
            return "AUTOMATIC";
        }

        if (containsAny(normalized,
                " manu\u00e1l ",
                " manual ",
                " manu\u00e1ln\u00ed ",
                " manualni ")
                || containsAny(tokens,
                " man ",
                " mt ",
                " 5mt ",
                " 6mt ",
                " mt5 ",
                " mt6 ")) {
            return "MANUAL";
        }

        return null;
    }

    private String extractCarType(String title, String text, String url) {
        String source = " " + normalizeText(safe(title) + " " + safe(text) + " " + safe(url)).toLowerCase(Locale.ROOT) + " ";
        String normalizedUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);

        String titleSource = " " + normalizeText(safe(title)).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(titleSource,
                " enyaq ", " karoq ", " duster ", " tiguan allspace ", " c3 aircross ", " peugeot 5008 ", " taigo ", " xceed ")) {
            return "SUV";
        }

        if (containsAny(titleSource, " multivan ", " marco polo ", " proace verso ", " proace city verso ", " vivaro ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource, " corolla sedan ", " corolla sd ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " model 3 ", " tesla model 3 ")) {
            return "SEDAN";
        }

        if (titleSource.contains(" a3 ") && titleSource.contains(" sportback ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource,
                " q3 ", " q5 ", " q7 ", " q8 ",
                " touareg ", " qashqai ", " pathfinder ", " kona ", " captur ", " puma ", " crossland ", " range rover ", " glc ",
                " gla ", " glb ", " gle ", " gls ", " yaris cross ", " stonic ", " ateca ", " kamiq ",
                " omoda 5 ", " actyon ", " elroq ", " macan ", " 2008 ")) {
            return "SUV";
        }

        if (containsAny(titleSource, " tourneo courier ", " tourneo connect ", " tourneo custom ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource,
                " c-max ", " c max ", " galaxy ", " berlingo ", " caddy ", " roomster ", " sportsvan ", " scudo ",
                " ix20 ", " ix 20 ", " rifter ",
                " tridy v ", " třídy v ", " tĹ™Ă­dy v ", " vito ", " viano ",
                " scenic ", " zafira ", " meriva ", " touran ", " sharan ", " s-max ", " s max ",
                " c4 picasso ", " picasso ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource, " jimny ", " vitara ")) {
            return "SUV";
        }

        if (normalizedUrl.contains("/kombi/") || containsAny(titleSource, " combi ", " kombi ")) {
            return "WAGON";
        }

        if (containsAny(titleSource,
                " fiesta ", " ibiza ", " up! ", " up ", " golf ", " scala ", " fabia ", " i30 ", " zoe ")) {
            return "HATCHBACK";
        }

        // URL от TipCars самый надежный — проверяем первым
        if (normalizedUrl.contains("/suv/") || normalizedUrl.contains("/terenni/") || normalizedUrl.contains("/off-road/")) {
            return "SUV";
        }

        if (normalizedUrl.contains("/mpv/")) {
            return "MINIVAN";
        }

        if (normalizedUrl.contains("/pick-up/") || normalizedUrl.contains("/pickup/")) {
            return "PICKUP";
        }

        if (normalizedUrl.contains("/hatchback/") || normalizedUrl.contains("/elektro/")) {
            return "HATCHBACK";
        }

        if (normalizedUrl.contains("/liftback/") || normalizedUrl.contains("/sedan/") || normalizedUrl.contains("/limuzina/")) {
            return "SEDAN";
        }

        if (normalizedUrl.contains("/kabriolet/") || normalizedUrl.contains("/cabrio/")) {
            return "CABRIO";
        }

        if (normalizedUrl.contains("/coupe/") || normalizedUrl.contains("/kupe/")) {
            return "COUPE";
        }

        if (containsAny(source,
                " mustang mach-e ",
                " mach-e ",
                " puma ",
                " explorer ",
                " bronco ",
                " crossland ",
                " xc40 ",
                " xc60 ",
                " xc90 ",
                " suv ",
                " crossover ",
                " karoq ",
                " kamiq ",
                " kodiaq ",
                " tiguan ",
                " touareg ",
                " qashqai ",
                " q3 ",
                " q5 ",
                " q7 ",
                " q8 ",
                " x1 ",
                " x3 ",
                " x5 ",
                " sportage ",
                " rav4 ",
                " cr-v ",
                " cx-3 ",
                " cx-5 ",
                " wrangler ",
                " compass ",
                " renegade ")) {
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
                " estate ",
                " outback ")) {
            return "WAGON";
        }

        if (containsAny(source,
                " mpv ",
                " minivan ",
                " meriva ",
                " vito ",
                " třídy v ",
                " tridy v ",
                " v 300d ",
                " combo ",
                " tourneo courier ",
                " tourneo connect ",
                " touran ",
                " sharan ",
                " galaxy ",
                " caddy ",
                " berlingo ",
                " roomster ")) {
            return "MINIVAN";
        }

        if (containsAny(source,
                " proace verso ",
                " proace city verso ",
                " spacetourer ",
                " traveller ",
                " třídy v ",
                " tridy v ")) {
            return "MINIVAN";
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
                " scala ",
                " polo ",
                " corsa ",
                " meriva ",
                " 595 ")) {
            return "HATCHBACK";
        }

        if (containsAny(source,
                " sedan ",
                " liftback ",
                " octavia ",
                " superb ",
                " passat ",
                " arteon ")) {
            return "SEDAN";
        }

        if (containsAny(source,
                " coupe ",
                " coupé ",
                " supra ",
                " 370 z ",
                " 370z ",
                " camaro ",
                " mustang ")) {
            return "COUPE";
        }

        if (containsAny(source,
                " cabrio ",
                " kabriolet ",
                " roadster ",
                " spider ",
                " spyder ")) {
            return "CABRIO";
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

    private boolean looksCommercialOrCamperListing(String title, String url, String text) {
        String source = " " + normalizeText(safe(title) + " " + safe(url)).toLowerCase(Locale.ROOT) + " ";
        return containsAny(source,
                " obytný vůz ", " obytny vuz ", " obytný automobil ", " obytny automobil ",
                " obytné ", " obytne ",
                " obytna dodavka ", " obytná dodávka ", " camper ", " karavan ", " caravan ",
                " dodávka ", " dodavka ",
                " l1h1 ", " l2h2 ", " l3h2 ", " l3h3 ", " valník ", " valnik ",
                " movano ", " transporter ", " transit custom ", " combo l1 ", " combo l2 ", " combo xl ",
                "/uzitkove/", "/uzitkova/", "/dodavky/");
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
        if (value.startsWith("LOTUS")) return "LOTUS";
        if (value.startsWith("LAMBORGHINI")) return "LAMBORGHINI";
        if (value.startsWith("FERRARI")) return "FERRARI";
        if (value.startsWith("MINI")) return "MINI";
        if (value.startsWith("MG")) return "MG";
        if (value.startsWith("DS")) return "DS";
        if (value.startsWith("JAECOO")) return "JAECOO";
        if (value.startsWith("CHERY")) return "CHERY";
        if (value.startsWith("XPENG")) return "XPENG";
        if (value.startsWith("SSANGYONG")) return "SSANGYONG";
        if (value.startsWith("SSANG YONG")) return "SSANGYONG";

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

            if (codePoint == 0x0139 && looksLikeNormalizedMojibakeNbsp(value, offset)) {
                out.write(0xC5);
                out.write(0xA0);
                offset += Character.charCount(codePoint) + 1;
                continue;
            }

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

    private boolean looksLikeNormalizedMojibakeNbsp(String value, int offset) {
        int afterCurrent = offset + 1;
        if (afterCurrent >= value.length() || value.charAt(afterCurrent) != ' ') {
            return false;
        }

        int afterSpace = afterCurrent + 1;
        return afterSpace < value.length() && Character.isLetter(value.charAt(afterSpace));
    }

    private boolean looksLikeMojibake(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            if (codePoint == 0x0102
                    || codePoint == 0x0139
                    || codePoint == 0x00C4
                    || codePoint == 0x00C2
                    || codePoint == 0x015A
                    || codePoint == 0x017B
                    || codePoint == 0x013E
                    || codePoint == 0x0165
                    || codePoint == 0x02C7
                    || codePoint == 0x02DD
                    || codePoint == 0x2030
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
                    || codePoint == '\u015A'
                    || codePoint == '\u017B'
                    || codePoint == '\u013E'
                    || codePoint == '\u0165'
                    || codePoint == '\u02C7'
                    || codePoint == '\u02DD'
                    || codePoint == '\u2030'
                    || codePoint == '\uFFFD'
                    || (codePoint >= 0x0080 && codePoint <= 0x009F)) {
                score++;
            }
            offset += Character.charCount(codePoint);
        }
        return score;
    }

    private int mojibakeScoreLegacy(String value) {
        if (value == null || value.isBlank()) {
            return 0;
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

    private String shortenForCheck(String value, int maxLen) {
        String normalized = normalizeText(value);
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen);
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

    private record ListListing(String url, String title, String text, String imageUrl) {
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;

        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }

        return null;
    }
}

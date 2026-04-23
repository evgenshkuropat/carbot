package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SautoParser implements CarSourceParser {

    private static final Logger log = LoggerFactory.getLogger(SautoParser.class);

    private static final String BASE_LIST_URL = "https://www.sauto.cz/inzerce/osobni?razeni=od-nejlevnejsich";
    private static final int MAX_LIST_PAGES = 8;
    private static final int REQUEST_TIMEOUT_MS = 15_000;

    private static final int CURRENT_YEAR = Year.now().getValue();
    private static final int MIN_YEAR = 1990;
    private static final int MAX_REASONABLE_PRICE = 10_000_000;

    @Override
    public String getSourceName() {
        return "SAUTO";
    }

    @Override
    public List<CarDto> fetchCars() {
        List<CarDto> cars = new ArrayList<>();

        int brokenCount = 0;
        int commercialCount = 0;
        int cheapLowQualityCount = 0;
        int invalidPriceCount = 0;
        int missingPriceCount = 0;
        int parseExceptionCount = 0;

        Set<String> detailLinks = new LinkedHashSet<>();

        try {
            for (int page = 1; page <= MAX_LIST_PAGES; page++) {
                String pageUrl = buildPageUrl(page);

                try {
                    Document listDoc = Jsoup.connect(pageUrl)
                            .userAgent("Mozilla/5.0")
                            .timeout(REQUEST_TIMEOUT_MS)
                            .get();

                    Set<String> pageLinks = extractDetailLinks(listDoc);
                    detailLinks.addAll(pageLinks);

                    log.info("SAUTO list page={} url={} detail_links_found={} total_unique_links={}",
                            page, pageUrl, pageLinks.size(), detailLinks.size());

                    sleepQuietly(300);
                } catch (Exception e) {
                    log.warn("SAUTO list page failed page={} url={} error={}",
                            page, pageUrl, safe(e.getMessage()));
                }
            }

            log.info("SAUTO total unique detail links found={}", detailLinks.size());

            for (String url : detailLinks) {
                try {
                    ParseResult result = parseDetailWithReason(url);

                    if (result.car() != null) {
                        cars.add(result.car());
                    } else {
                        switch (result.reason()) {
                            case "broken_listing" -> brokenCount++;
                            case "commercial_vehicle" -> commercialCount++;
                            case "cheap_low_quality_listing" -> cheapLowQualityCount++;
                            case "invalid_price" -> invalidPriceCount++;
                            case "missing_price" -> missingPriceCount++;
                            case "parse_exception" -> parseExceptionCount++;
                        }
                    }
                } catch (Exception e) {
                    parseExceptionCount++;
                    log.warn("SAUTO detail parse failed url={} error={}", safe(url), safe(e.getMessage()));
                }
            }

        } catch (Exception e) {
            log.warn("SAUTO fetch failed error={}", safe(e.getMessage()));
        }

        log.info("SAUTO parsed {} cars", cars.size());
        log.info("SAUTO SUMMARY parsed={} broken_listing={} commercial_vehicle={} cheap_low_quality_listing={} invalid_price={} missing_price={} parse_exception={}",
                cars.size(), brokenCount, commercialCount, cheapLowQualityCount, invalidPriceCount, missingPriceCount, parseExceptionCount);

        return cars;
    }

    private ParseResult parseDetailWithReason(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(REQUEST_TIMEOUT_MS)
                    .get();

            String title = extractTitle(doc);
            String jsonLd = extractJsonLd(doc);
            String description = extractDescription(doc, jsonLd);
            String pageText = normalizeText(doc.text());

            String listingText = normalizeText(title + " " + description);
            String analysisText = normalizeText(title + " " + description + " " + pageText);

            if (looksBrokenListing(title, listingText, analysisText)) {
                log.warn("SAUTO SKIP url={} reason=broken_listing title={}", safe(url), safe(title));
                return new ParseResult(null, "broken_listing");
            }

            if (looksCommercialVehicle(title, listingText, url)) {
                log.warn("SAUTO SKIP url={} reason=commercial_vehicle title={}", safe(url), safe(title));
                return new ParseResult(null, "commercial_vehicle");
            }

            Integer priceValue = extractPriceValueDirect(doc, title, listingText);
            if (priceValue == null) {
                log.warn("SAUTO SKIP url={} reason=missing_price title={}", safe(url), safe(title));
                return new ParseResult(null, "missing_price");
            }

            if (priceValue <= 0 || priceValue > MAX_REASONABLE_PRICE) {
                log.warn("SAUTO SKIP url={} reason=invalid_price title={} price={}", safe(url), safe(title), priceValue);
                return new ParseResult(null, "invalid_price");
            }

            Integer year = extractYearSafely(title, description, analysisText);
            Integer mileage = extractMileage(analysisText);

            if (isClearlyFakeModernCarPrice(priceValue, year, mileage, title, analysisText)) {
                log.warn("SAUTO SKIP url={} reason=invalid_price title={} price={} year={} mileage={}",
                        safe(url), safe(title), priceValue, year, mileage);
                return new ParseResult(null, "invalid_price");
            }

            String location = extractLocation(doc, jsonLd, analysisText);
            String imageUrl = extractImageUrl(doc, jsonLd);
            String brand = extractBrand(title, jsonLd);

            String fuelType = firstNonBlank(
                    extractFuelType(title),
                    extractFuelType(description),
                    extractFuelType(analysisText)
            );

            String transmission = firstNonBlank(
                    extractTransmission(title),
                    extractTransmission(description),
                    extractTransmission(analysisText)
            );

            if ("ELECTRIC".equals(fuelType)) {
                transmission = null;
            }

            String carType = extractCarType(title, analysisText, url);

            if (looksCheapLowQualityListing(title, listingText, analysisText, priceValue, year, mileage)) {
                log.warn("SAUTO SKIP url={} reason=cheap_low_quality_listing title={} price={} year={} mileage={} fuelType={} transmission={} location={}",
                        safe(url), safe(title), priceValue, year, mileage, safe(fuelType), safe(transmission), safe(location));
                return new ParseResult(null, "cheap_low_quality_listing");
            }

            log.info("SAUTO CAR title='{}' price={} location={} year={} mileage={} fuelType={} transmission={} carType={} brand={} url={}",
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

            CarDto car = new CarDto();
            car.setSource("SAUTO");
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

            return new ParseResult(car, "ok");

        } catch (Exception e) {
            log.warn("SAUTO SKIP url={} reason=parse_exception error={}", safe(url), safe(e.getMessage()));
            return new ParseResult(null, "parse_exception");
        }
    }

    private String buildPageUrl(int page) {
        if (page <= 1) {
            return BASE_LIST_URL;
        }
        return BASE_LIST_URL + "&strana=" + page;
    }

    private Set<String> extractDetailLinks(Document listDoc) {
        Set<String> links = new LinkedHashSet<>();

        for (Element a : listDoc.select("a[href]")) {
            String href = a.absUrl("href");
            if (href == null || href.isBlank()) {
                continue;
            }

            if (href.matches("https://www\\.sauto\\.cz/osobni/detail/[^\\s?#]+.*")) {
                href = href.replaceAll("[?#].*$", "");
                links.add(href);
            }
        }

        return links;
    }

    private String extractTitle(Document doc) {
        Element og = doc.selectFirst("meta[property=og:title]");
        if (og != null) {
            String value = normalizeText(og.attr("content"));
            if (!value.isBlank()) {
                return stripSautoSuffix(value);
            }
        }

        Element h1 = doc.selectFirst("h1");
        if (h1 != null) {
            String value = normalizeText(h1.text());
            if (!value.isBlank()) {
                return stripSautoSuffix(value);
            }
        }

        return stripSautoSuffix(normalizeText(doc.title()));
    }

    private String stripSautoSuffix(String text) {
        if (text == null) {
            return null;
        }
        return normalizeText(text.replace("| Sauto.cz", "").trim());
    }

    private String extractJsonLd(Document doc) {
        for (Element script : doc.select("script[type=application/ld+json]")) {
            String raw = script.html();
            if (raw != null && !raw.isBlank()) {
                if (raw.contains("\"price\"") || raw.contains("\"description\"") || raw.contains("\"image\"")) {
                    return raw;
                }
            }
        }
        return "";
    }

    private String extractDescription(Document doc, String jsonLd) {
        String fromJson = extractJsonValue(jsonLd, "description");
        if (fromJson != null && !fromJson.isBlank()) {
            return normalizeText(fromJson);
        }

        Element meta = doc.selectFirst("meta[name=description]");
        if (meta != null) {
            String value = normalizeText(meta.attr("content"));
            if (!value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private Integer extractPriceValueDirect(Document doc, String title, String listingText) {
        PriceCandidate selectorCandidate = extractPriceFromSpecificSelectorsWithRaw(doc);
        Integer jsonLdPrice = extractPriceFromJsonLd(doc);
        Integer visiblePrice = extractVisibleMainPriceFallback(doc);

        Integer directSelectorPrice = selectorCandidate != null ? selectorCandidate.price() : null;
        String selectorRaw = selectorCandidate != null ? selectorCandidate.raw() : null;

        Integer normalizedSelectorPrice = normalizeSautoPrice(directSelectorPrice, selectorRaw, title, listingText);
        Integer normalizedVisiblePrice = normalizeSautoPrice(visiblePrice, null, title, listingText);
        Integer normalizedJsonLdPrice = normalizeSautoPrice(jsonLdPrice, null, title, listingText);

        log.info("SAUTO PRICE CHECK title='{}' directPrice={} normalizedDirectPrice={} visiblePrice={} normalizedVisiblePrice={} jsonLdPrice={} normalizedJsonLdPrice={} rawSelector={}",
                safe(title),
                directSelectorPrice,
                normalizedSelectorPrice,
                visiblePrice,
                normalizedVisiblePrice,
                jsonLdPrice,
                normalizedJsonLdPrice,
                safe(selectorRaw));

        if (looksLikeLeaseTransferOrDeposit(title, listingText, selectorRaw)) {
            log.warn("SAUTO PRICE REJECTED title='{}' reason=lease_transfer_or_deposit rawSelector={}",
                    safe(title), safe(selectorRaw));
            return null;
        }

        if (normalizedSelectorPrice != null && isFinanceBaitRaw(selectorRaw)) {
            log.warn("SAUTO PRICE REJECTED title='{}' reason=finance_bait_raw selectorRaw={}",
                    safe(title), safe(selectorRaw));
            return null;
        }

        if (normalizedSelectorPrice != null
                && !looksLikeFinanceBaitPrice(normalizedSelectorPrice, title, listingText, selectorRaw)) {
            return normalizedSelectorPrice;
        }

        if (normalizedVisiblePrice != null
                && !looksLikeFinanceBaitPrice(normalizedVisiblePrice, title, listingText, null)) {
            return normalizedVisiblePrice;
        }

        if (normalizedJsonLdPrice != null
                && !looksLikeFinanceBaitPrice(normalizedJsonLdPrice, title, listingText, null)) {
            return normalizedJsonLdPrice;
        }

        log.info("SAUTO PRICE REJECTED title='{}' selectorPrice={} visiblePrice={} jsonLdPrice={} reason=finance_bait_or_filtered rawSelector={}",
                safe(title),
                normalizedSelectorPrice,
                normalizedVisiblePrice,
                normalizedJsonLdPrice,
                safe(selectorRaw));

        return null;
    }

    private PriceCandidate extractPriceFromSpecificSelectorsWithRaw(Document doc) {
        String[] selectors = {
                "[data-testid*=price]",
                "[data-test*=price]",
                "[class*=price]",
                "[class*=Price]",
                "[id*=price]",
                "meta[property=product:price:amount]"
        };

        for (String selector : selectors) {
            for (Element el : doc.select(selector)) {
                String raw = normalizeText(el.text());
                if (raw.isBlank()) {
                    raw = normalizeText(el.attr("content"));
                }

                if (raw.isBlank()) {
                    continue;
                }

                log.info("SAUTO RAW SELECTOR PRICE selector={} raw={}", selector, safe(raw));

                List<Integer> prices = extractAllPrices(raw);
                for (Integer value : prices) {
                    log.info("SAUTO PARSED SELECTOR PRICE selector={} raw={} parsed={}", selector, safe(raw), value);
                    if (value != null) {
                        return new PriceCandidate(value, raw);
                    }
                }

                Integer fallback = parsePriceToInt(raw);
                log.info("SAUTO PARSED FALLBACK PRICE selector={} raw={} parsed={}", selector, safe(raw), fallback);

                if (fallback != null) {
                    return new PriceCandidate(fallback, raw);
                }
            }
        }

        return null;
    }

    private Integer extractPriceFromJsonLd(Document doc) {
        String jsonLd = extractJsonLd(doc);
        String raw = extractJsonValue(jsonLd, "price");
        if (raw == null || raw.isBlank()) {
            return null;
        }

        log.info("SAUTO JSONLD RAW PRICE={}", safe(raw));
        return parsePriceToInt(raw);
    }

    private Integer extractVisibleMainPriceFallback(Document doc) {
        List<String> candidates = new ArrayList<>();

        for (Element el : doc.select("body *")) {
            String text = normalizeText(el.ownText());
            if (!text.isBlank() && containsKcPrice(text)) {
                candidates.add(text);
            }
        }

        Integer bestMainPrice = null;
        Integer bestRangeMax = null;

        for (String text : candidates) {
            String lower = text.toLowerCase(Locale.ROOT);
            if (!lower.contains("kč")) {
                continue;
            }

            List<Integer> values = extractAllPrices(text);
            if (values.isEmpty()) {
                continue;
            }

            boolean isRange = text.contains("–") || text.contains("-") || text.contains("—");
            boolean mentionsLeasing = lower.contains("leasing");
            boolean mentionsInstallment = lower.contains("splát") || lower.contains("splat")
                    || lower.contains("měsíč") || lower.contains("mesic");

            if (isRange) {
                Integer max = values.stream().max(Integer::compareTo).orElse(null);
                if (max != null) {
                    bestRangeMax = max;
                }
                continue;
            }

            if (mentionsLeasing || mentionsInstallment) {
                continue;
            }

            Integer first = values.get(0);
            if (first != null) {
                bestMainPrice = first;
                break;
            }
        }

        return bestMainPrice != null ? bestMainPrice : bestRangeMax;
    }

    private Integer normalizeSautoPrice(Integer priceValue, String rawPriceText, String title, String listingText) {
        if (priceValue == null) {
            return null;
        }

        String raw = normalizeText(safe(rawPriceText)).toLowerCase(Locale.ROOT);
        String text = normalizeText(safe(title) + " " + safe(listingText)).toLowerCase(Locale.ROOT);

        if (priceValue >= 25_000) {
            return priceValue;
        }

        Integer extractedYear = extractYearSafely(title, listingText, text);

        boolean looksModernCar =
                !looksBrokenListing(title, text, text)
                        && !looksCommercialVehicle(title, text, "")
                        && !containsAny(text,
                        " na díly ", " na dily ", " na nd ",
                        " závada ", " zavada ",
                        " vada motoru ",
                        " zadřený motor ", " zadreny motor ",
                        " nepojízdné ", " nepojizdne ",
                        " nepojízdný ", " nepojizdny ",
                        " bez stk ", " bez tp ")
                        && (extractedYear == null || extractedYear >= 2008);

        boolean looksLikeThousands =
                priceValue >= 100
                        && priceValue <= 5000
                        && (raw.contains("bez dph")
                        || containsAny(text,
                        " hybrid ", " phev ", " ev ", " awd ", " 4x4 ", " dsg ", " quattro ",
                        " xcellence ", " edition ", " tdi ", " tsi ", " tfsi ",
                        " xc60 ", " xc90 ", " captur ", " leon ", " superb "));

        if (looksModernCar && looksLikeThousands) {
            return priceValue * 1000;
        }

        return priceValue;
    }

    private boolean isFinanceBaitRaw(String rawPriceText) {
        if (rawPriceText == null || rawPriceText.isBlank()) {
            return false;
        }

        String raw = " " + normalizeText(rawPriceText).toLowerCase(Locale.ROOT) + " ";

        return containsAny(raw,
                " měsíčně ", " mesicne ",
                " měsíční ", " mesicni ",
                " splátka ", " splatka ",
                " splátky ", " splatky ",
                " akontace ",
                " již od ", " jiz od ",
                " stačí složit ", " staci slozit ",
                " složit jen ", " slozit jen ",
                " zbytek na splátky ", " zbytek na splatky ",
                " operativní leasing ", " operativni leasing ",
                " přenechání leasingu ", " prenechani leasingu ",
                " převzetí leasingu ", " prevzeti leasingu ");
    }

    private boolean looksLikeFinanceBaitPrice(Integer priceValue, String title, String text, String rawPriceText) {
        if (priceValue == null) {
            return false;
        }

        String normalized = " " + normalizeText(safe(title) + " " + safe(text) + " " + safe(rawPriceText)).toLowerCase(Locale.ROOT) + " ";

        if (priceValue <= 1) {
            return true;
        }

        return priceValue < 20_000 && containsAny(normalized,
                " měsíčně ", " mesicne ",
                " měsíční ", " mesicni ",
                " splátka ", " splatka ",
                " splátky ", " splatky ",
                " akontace ",
                " stačí složit ", " staci slozit ",
                " složit jen ", " slozit jen ",
                " již od ", " jiz od ",
                " nízké splátky ", " nizke splatky ",
                " bez dph ");
    }

    private boolean looksLikeLeaseTransferOrDeposit(String title, String text, String rawPriceText) {
        String normalized = (" " + normalizeText(safe(title) + " " + safe(text) + " " + safe(rawPriceText))
                .toLowerCase(Locale.ROOT) + " ");

        return containsAny(normalized,
                " převzetí leasingu ", " prevzeti leasingu ",
                " přenechání leasingu ", " prenechani leasingu ",
                " operativní leasing ", " operativni leasing ",
                " měsíčně ", " mesicne ",
                " měsíční ", " mesicni ",
                " splátka ", " splatka ",
                " splátky ", " splatky ",
                " akontace ",
                " cena od ",
                " již od ", " jiz od ",
                " stačí složit ", " staci slozit ",
                " složit jen ", " slozit jen ",
                " zbytek na splátky ", " zbytek na splatky ");
    }

    private boolean isClearlyFakeModernCarPrice(Integer price, Integer year, Integer mileage, String title, String text) {
        if (price == null) {
            return false;
        }

        String normalized = (" " + normalizeText(safe(title) + " " + safe(text)).toLowerCase(Locale.ROOT) + " ");

        boolean premiumOrRecentModel = containsAny(normalized,
                " kodiaq ", " karoq ", " kamiq ", " superb iii ", " superb 3 ",
                " xc40 ", " xc60 ", " xc90 ",
                " captur ", " clio techno ", " puma ", " kuga ",
                " q5 ", " q7 ", " q8 ",
                " id.4 ", " id4 ", " enyaq ",
                " formentor ", " explorer ", " range rover ", " defender ",
                " gla ", " glc ", " gle ", " gls ",
                " t-roc ", " tiguan ", " touareg ", " ateca ", " terramar ");

        if ((year != null && year >= 2016) && price < 80_000) {
            return true;
        }

        if ((year != null && year >= 2020) && price < 120_000) {
            return true;
        }

        if ((year != null && year >= 2018) && mileage != null && mileage < 100_000 && price < 100_000) {
            return true;
        }

        if (premiumOrRecentModel && price < 100_000) {
            return true;
        }

        return false;
    }

    private boolean looksCheapLowQualityListing(String title,
                                                String listingText,
                                                String analysisText,
                                                Integer priceValue,
                                                Integer year,
                                                Integer mileage) {
        if (priceValue == null) {
            return false;
        }

        String normalized = " " + normalizeText(
                safe(title) + " " + safe(listingText) + " " + safe(analysisText)
        ).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(normalized,
                " na díly ", " na dily ",
                " náhradní díly ", " nahradni dily ",
                " pouze na nd ",
                " nepojízdné ", " nepojizdne ",
                " nepojízdný ", " nepojizdny ",
                " nepojízdná ", " nepojizdna ",
                " zadřený motor ", " zadreny motor ",
                " závada motoru ", " zavada motoru ",
                " vada motoru ",
                " motor ko ",
                " prasklá převodovka ", " praskla prevodovka ",
                " prsklá převodovka ", " prskla prevodovka ",
                " havarované ", " havarovane ",
                " havarovaný ", " havarovany ",
                " havarovaná ", " havarovana ",
                " bourané ", " bourane ",
                " bouraný ", " bourany ",
                " bouraná ", " bourana ",
                " poškozeno ", " poskozeno ",
                " poškození požárem ", " poskozeni pozarem ",
                " bez dokladů ", " bez dokladu ",
                " bez tp ",
                " bez stk ",
                " anglie ",
                " nutná oprava ", " nutna oprava ",
                " špatná spojka ", " spatna spojka ",
                " vada turba ",
                " k opravě ", " k oprave ",
                " na opravu ",
                " plně pojízdné ", " plne pojizdne ")) {
            return true;
        }

        if (year != null && mileage != null) {
            if (year <= 1999 && mileage >= 180_000) return true;
            if (year <= 2003 && mileage >= 230_000) return true;
            if (year <= 2006 && mileage >= 280_000) return true;
            if (year <= 2009 && mileage >= 330_000) return true;
        }

        if (year != null && priceValue <= 18_000) {
            if (year <= 1998) return true;
            if (year <= 2001 && mileage != null && mileage >= 150_000) return true;
        }

        if (priceValue <= 15_000 && mileage != null && mileage >= 250_000) {
            return true;
        }

        if (priceValue <= 18_000 && mileage != null && mileage >= 350_000) {
            return true;
        }

        if (priceValue <= 20_000 && year != null && mileage != null) {
            if (year <= 2005 && mileage >= 300_000) return true;
            if (year <= 2008 && mileage >= 380_000) return true;
        }

        if (containsAny(normalized, " lpg ") && mileage != null && mileage >= 350_000) {
            return true;
        }

        if (containsAny(normalized,
                " storia ",
                " felicia ",
                " favorit ",
                " forman ",
                " saxo ",
                " xsara ",
                " almera ",
                " getz ",
                " cordoba ",
                " golf iv ",
                " golf 4 ",
                " passat b5 ")
                && priceValue <= 18_000
                && mileage != null
                && mileage >= 170_000) {
            return true;
        }

        return false;
    }

    private boolean looksBrokenListing(String title, String listingText, String analysisText) {
        String normalized = " " + normalizeText(
                safe(title) + " " + safe(listingText) + " " + shortenForCheck(analysisText, 800)
        ).toLowerCase(Locale.ROOT) + " ";

        return containsAny(normalized,
                " na díly ", " na dily ", " na nd ", " náhradní díly ", " nahradni dily ",
                " zadřený motor ", " zadreny motor ",
                " závada motoru ", " zavada motoru ",
                " vada motoru ",
                " motor ko ",
                " nepojízdné ", " nepojizdne ",
                " nepojízdný ", " nepojizdny ",
                " nepojízdná ", " nepojizdna ",
                " bez tp ",
                " bez stk ",
                " prasklá převodovka ", " praskla prevodovka ",
                " prsklá převodovka ", " prskla prevodovka ",
                " k opravě ", " k oprave ",
                " poškozeno ", " poskozeno ",
                " poškozeni požárem ", " poskozeni pozarem ",
                " havarované ", " havarovane ",
                " havarovaný ", " havarovany ",
                " havarovaná ", " havarovana ",
                " bourané ", " bourane ",
                " bouraný ", " bourany ",
                " bouraná ", " bourana ",
                " ze zadu poškozeno ", " ze zadu poskozeno ",
                " pouze na nd ",
                " bez dokladů ", " bez dokladu ",
                " rezervace ",
                " zamluveno ");
    }

    private boolean looksCommercialVehicle(String title, String text, String url) {
        String source = " " + normalizeText(title + " " + shortenForCheck(text, 500) + " " + safe(url)).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(source,
                " caddy ",
                " sharan ",
                " alhambra ",
                " galaxy ",
                " s-max ",
                " b-max ",
                " c-max ",
                " grand c-max ",
                " zafira ",
                " meriva ",
                " scenic ",
                " espace ",
                " lodgy ",
                " verso ",
                " roomster ",
                " vaneo ",
                " ram ")) {
            return false;
        }

        return containsAny(source,
                " sprinter ", " crafter ", " ducato ", " boxer ", " jumper ", " master ", " movano ",
                " daily ", " iveco daily ", " vivaro ", " trafic ", " traffic ", " primastar ",
                " nv300 ", " nv400 ", " expert ", " jumpy ", " scudo ", " proace ",
                " transit ", " transit custom ", " tourneo custom ",
                " tourneo courier ", " courier ",
                " transporter ", " caravelle ", " multivan ",
                " dodávka ", " dodavka ", " užitkový ", " uzitkovy ", " užitkové ", " uzitkove ",
                " nákladní ", " nakladni ", " furgon ", " skříň ", " skrin ",
                " valník ", " valnik ", " plachta ", " podvozek ",
                " chladírenský ", " chladirensky ",
                " autobus ", " mikrobus ", " minibus ",
                " obytný vůz ", " obytny vuz ", " karavan ",
                " l1 ", " l2 ");
    }

    private String extractLocation(Document doc, String jsonLd, String text) {
        String jsonLocation = firstNonBlank(
                extractJsonValue(jsonLd, "addressLocality"),
                extractJsonValue(jsonLd, "addressRegion"),
                extractJsonValue(jsonLd, "areaServed")
        );

        if (isMeaningfulLocation(jsonLocation)) {
            return normalizeLocation(jsonLocation);
        }

        for (Element el : doc.select("[data-testid*=location], [data-test*=location], [class*=location], [class*=Location], [href*=/mapa], [href*=okres], [href*=kraj]")) {
            String candidate = normalizeText(el.text());
            if (isMeaningfulLocation(candidate)) {
                String cleaned = normalizeLocation(candidate);
                if (cleaned != null) {
                    return cleaned;
                }
            }
        }

        String normalized = normalizeText(text);

        Matcher cityMatcher = Pattern.compile("(?i)(praha|brno|ostrava|plzeň|plzen|liberec|olomouc|pardubice|hradec králové|hradec kralove|české budějovice|ceske budejovice|ústí nad labem|usti nad labem|zlín|zlin|jihlava|karlovy vary|opava|kladno|mladá boleslav|mlada boleslav|teplice|most|cheb|trutnov|kolín|kolin|louny|karviná|karvina|blansko)")
                .matcher(normalized);

        if (cityMatcher.find()) {
            return normalizeLocation(cityMatcher.group(1));
        }

        if (normalized.toLowerCase(Locale.ROOT).contains("hlavní město praha")
                || normalized.toLowerCase(Locale.ROOT).contains("hlavni mesto praha")) {
            return "Praha";
        }

        return null;
    }

    private boolean isMeaningfulLocation(String value) {
        if (value == null) {
            return false;
        }

        String normalized = normalizeText(value);
        if (normalized.isBlank()) {
            return false;
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        return !containsAny(lower,
                "zobrazit", "kontakt", "prodejce", "telefon", "email", "www.", "http",
                "kč", "leasing", "měsíčně", "mesicne",
                "prověřit", "proverit", "technikem", "vůz", "vuz");
    }

    private String normalizeLocation(String value) {
        String normalized = normalizeText(value);
        if (normalized.isBlank()) {
            return null;
        }

        String lower = normalized.toLowerCase(Locale.ROOT);

        if (lower.startsWith("hlavn")) return "Praha";
        if (lower.startsWith("brno-m")) return "Brno";
        if (lower.equals("hlavní město praha") || lower.equals("hlavni mesto praha")) return "Praha";

        normalized = normalized.replaceAll("(?i)\\b(dnes|top|skladem)\\b.*$", "").trim();
        normalized = normalized.replaceAll("(?i)\\b(citro[eë]n|škoda|skoda|renault|ford|peugeot|opel|kia|hyundai|seat|audi|bmw|mercedes|volkswagen|vw|toyota|lexus|volvo|porsche|jeep|dacia|fiat|mazda|nissan|suzuki|honda|land rover)\\b.*$", "").trim();
        normalized = normalized.replaceAll("[,;\\-]+$", "").trim();

        if (normalized.equalsIgnoreCase("Plze")) return "Plzeň";
        if (normalized.equalsIgnoreCase("Hradec Kr")) return "Hradec Králové";
        if (normalized.equalsIgnoreCase("Ostrava-m")) return "Ostrava";
        if (normalized.equalsIgnoreCase("Karvin")) return "Karviná";

        if (normalized.length() < 2) {
            return null;
        }

        return capitalizeWords(normalized);
    }

    private String extractImageUrl(Document doc, String jsonLd) {
        String image = extractJsonValue(jsonLd, "image");
        if (image != null && !image.isBlank()) {
            return image.startsWith("//") ? "https:" + image : image;
        }

        Element meta = doc.selectFirst("meta[property=og:image]");
        if (meta != null) {
            String value = meta.attr("content");
            if (value != null && !value.isBlank()) {
                return value.startsWith("//") ? "https:" + value : value;
            }
        }

        return null;
    }

    private String extractBrand(String title, String jsonLd) {
        String source = normalizeText(title);
        if (source.isBlank()) {
            source = normalizeText(extractJsonValue(jsonLd, "name"));
        }

        if (source.isBlank()) {
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

    private Integer extractYearSafely(String title, String description, String analysisText) {
        String safeTitle = normalizeText(title);
        String safeDescription = normalizeText(description);
        String safeAnalysis = normalizeText(analysisText);
        String combined = normalizeText(safeTitle + " " + safeDescription + " " + safeAnalysis);

        List<Integer> candidates = new ArrayList<>();

        Integer explicitFromDescription = extractYearFromLabeledText(safeDescription);
        if (isSafeParsedYear(safeTitle, explicitFromDescription, safeAnalysis)) {
            candidates.add(explicitFromDescription);
        }

        Integer explicitFromAnalysis = extractYearFromLabeledText(safeAnalysis);
        if (isSafeParsedYear(safeTitle, explicitFromAnalysis, safeAnalysis)) {
            candidates.add(explicitFromAnalysis);
        }

        Integer fromAttributes = extractYearFromAttributesText(safeAnalysis);
        if (isSafeParsedYear(safeTitle, fromAttributes, safeAnalysis)) {
            candidates.add(fromAttributes);
        }

        Integer fromTitle = extractRealYear(safeTitle, safeAnalysis);
        if (isSafeParsedYear(safeTitle, fromTitle, safeAnalysis)) {
            candidates.add(fromTitle);
        }

        Integer fromCombined = extractRealYear(combined, safeAnalysis);
        if (isSafeParsedYear(safeTitle, fromCombined, safeAnalysis)) {
            candidates.add(fromCombined);
        }

        Integer fromMyCode = extractYearFromModelYearCode(combined);
        if (isSafeParsedYear(safeTitle, fromMyCode, safeAnalysis)) {
            candidates.add(fromMyCode);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        Integer best = chooseMostReasonableYear(candidates, safeTitle, safeAnalysis);
        if (!isSafeParsedYear(safeTitle, best, safeAnalysis)) {
            return null;
        }

        return best;
    }

    private Integer chooseMostReasonableYear(List<Integer> candidates, String title, String analysisText) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        String normalizedTitle = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String normalizedAnalysis = " " + normalizeText(analysisText).toLowerCase(Locale.ROOT) + " ";
        String source = normalizedTitle + " " + normalizedAnalysis;

        boolean clearlyOldModel = looksClearlyOldCarTitle(normalizedTitle)
                || containsAny(source,
                " clio ",
                " storia ",
                " megane ",
                " scenic ",
                " xsara ",
                " c5 ",
                " c4 ",
                " c2 ",
                " fabia ",
                " octavia i ",
                " octavia 1 ",
                " octavia ii ",
                " octavia 2 ",
                " passat b5 ",
                " passat ",
                " bora ",
                " almera ",
                " micra ",
                " getz ",
                " accent ",
                " meriva ",
                " zafira ",
                " astra ",
                " corsa ",
                " felicia ",
                " forman ",
                " favorita ",
                " yaris ",
                " rio ",
                " kalos ",
                " punto ",
                " bravo ",
                " cordoba ");

        Integer smallest = candidates.stream().min(Integer::compareTo).orElse(null);
        Integer biggest = candidates.stream().max(Integer::compareTo).orElse(null);

        if (smallest == null) {
            return null;
        }

        if (clearlyOldModel) {
            return smallest;
        }

        if (biggest != null && biggest >= CURRENT_YEAR - 1) {
            boolean modernSignals = containsAny(source,
                    " electric ",
                    " elektro ",
                    " ev ",
                    " phev ",
                    " plug-in ",
                    " hybrid ",
                    " gen-e ",
                    " techno ",
                    " iconic ",
                    " performance ",
                    " facelift ",
                    " mild hybrid ",
                    " mhev ",
                    " id.4 ",
                    " es90 ",
                    " r4 ",
                    " r5 ");

            if (!modernSignals) {
                List<Integer> olderCandidates = candidates.stream()
                        .filter(y -> y != null && y <= CURRENT_YEAR - 2)
                        .sorted()
                        .toList();

                if (!olderCandidates.isEmpty()) {
                    return olderCandidates.get(olderCandidates.size() - 1);
                }
            }
        }

        List<Integer> sorted = candidates.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();

        return sorted.isEmpty() ? null : sorted.get(0);
    }

    private Integer extractRealYear(String text, String analysisText) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String normalized = normalizeText(text);
        String analysis = normalizeText(analysisText);

        Matcher matcher = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(normalized);
        while (matcher.find()) {
            Integer year = parseIntSafe(matcher.group(1));
            if (!isValidYearExtended(year)) {
                continue;
            }

            int start = matcher.start();
            int end = matcher.end();

            String left = normalized.substring(Math.max(0, start - 45), start).toLowerCase(Locale.ROOT);
            String right = normalized.substring(end, Math.min(normalized.length(), end + 45)).toLowerCase(Locale.ROOT);

            if (looksLikeStkContext(left, right)) {
                continue;
            }

            if (isKnownModelNumber(String.valueOf(year), normalized)) {
                continue;
            }

            if (looksSuspiciousFutureLikeYearInOldCarTitle(normalized, year)) {
                continue;
            }

            if (looksOldCarByTitleOrAnalysis(normalized, analysis) && year >= 2020) {
                continue;
            }

            if (looksVeryOldCarByTitleOrAnalysis(normalized, analysis) && year >= 2015) {
                continue;
            }

            if (looksYearLabelNearby(left, right)) {
                return year;
            }

            return year;
        }

        return null;
    }

    private boolean isSafeParsedYear(String title, Integer year, String analysisText) {
        if (!isValidYearExtended(year)) {
            return false;
        }

        String normalizedTitle = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String normalizedAnalysis = " " + normalizeText(analysisText).toLowerCase(Locale.ROOT) + " ";

        if (looksSuspiciousFutureLikeYearInOldCarTitle(normalizedTitle, year)) {
            return false;
        }

        if (looksOldCarByTitleOrAnalysis(normalizedTitle, normalizedAnalysis) && year >= 2020) {
            return false;
        }

        if (looksVeryOldCarByTitleOrAnalysis(normalizedTitle, normalizedAnalysis) && year >= 2015) {
            return false;
        }

        return true;
    }

    private boolean isValidYearExtended(Integer year) {
        return year != null && year >= MIN_YEAR && year <= CURRENT_YEAR + 1;
    }

    private boolean looksLikeStkContext(String left, String right) {
        String around = (" " + safe(left) + " " + safe(right) + " ").toLowerCase(Locale.ROOT);

        return containsAny(around,
                " stk ",
                " tk ",
                " technická ",
                " technicka ",
                " platnost ",
                " platná do ",
                " platna do ",
                " konec stk ",
                " konec tk ",
                " evidenční kontrola ",
                " evidencni kontrola ",
                " do 202",
                " do 203");
    }

    private boolean looksYearLabelNearby(String left, String right) {
        String around = (" " + safe(left) + " " + safe(right) + " ").toLowerCase(Locale.ROOT);

        return containsAny(around,
                " rok výroby ",
                " rok vyroby ",
                " r.v",
                " rv ",
                " první registrace ",
                " prvni registrace ",
                " uvedení do provozu ",
                " uvedeni do provozu ",
                " modelový rok ",
                " modelovy rok ",
                " vyrobeno ",
                " registrace ");
    }

    private boolean looksOldCarByTitleOrAnalysis(String title, String analysis) {
        String source = (" " + safe(title) + " " + safe(analysis) + " ").toLowerCase(Locale.ROOT);

        return containsAny(source,
                " passat b5 ", " passat b5.5 ",
                " golf iv ", " golf 4 ", " golf basis ",
                " fabia i ", " fabia 1 ",
                " octavia i ", " octavia 1 ", " octavia ii ", " octavia 2 ",
                " superb i ", " superb 1 ",
                " felicia ", " favorit ", " forman ",
                " xsara ", " xantia ", " saxo ", " c2 ", " c3 ",
                " clio 1 ", " clio 2 ", " clio storia ",
                " megane i ", " megane ii ",
                " scenic 1 ", " scenic i ",
                " mondeo iii ",
                " focus i ",
                " micra 1.2 ", " micra 1,2 ",
                " almera ",
                " astra g ",
                " meriva ",
                " zafira ",
                " bora 1.9 tdi ",
                " peugeot 206 ",
                " peugeot 307 ",
                " seat cordoba ",
                " getz ",
                " yaris 1.0 ", " yaris 1,0 ",
                " v50 1.6d ",
                " alfa romeo 156 ");
    }

    private boolean looksVeryOldCarByTitleOrAnalysis(String title, String analysis) {
        String source = (" " + safe(title) + " " + safe(analysis) + " ").toLowerCase(Locale.ROOT);

        return containsAny(source,
                " passat b5 ",
                " golf iv ",
                " fabia i ",
                " octavia i ",
                " felicia ",
                " favorit ",
                " forman ",
                " saxo ",
                " xsara ",
                " clio 1 ",
                " clio 2 ",
                " micra 1.2 ",
                " micra 1,2 ",
                " astra g ",
                " bora ",
                " yaris 1.0 ",
                " yaris 1,0 ",
                " cordoba ");
    }

    private Integer extractYearFromModelYearCode(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = Pattern.compile("(?i)\\bMY\\s?(\\d{2})\\b").matcher(text);
        while (matcher.find()) {
            int yy = Integer.parseInt(matcher.group(1));
            int year = 2000 + yy;

            if (year >= 2018 && year <= CURRENT_YEAR + 1) {
                return year;
            }
        }

        return null;
    }

    private Integer extractYearFromLabeledText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher m = Pattern.compile(
                "(?i)(rok výroby|rok vyroby|r\\.v\\.?|rv|první registrace|prvni registrace|uvedení do provozu|uvedeni do provozu|do provozu|modelový rok|modelovy rok)\\s*[:\\- ]\\s*(19\\d{2}|20\\d{2})"
        ).matcher(text);

        if (m.find()) {
            Integer year = parseIntSafe(m.group(2));
            if (isValidYear(year)) {
                return year;
            }
        }

        Matcher m2 = Pattern.compile("\\b(0?[1-9]|1[0-2])[./](19\\d{2}|20\\d{2})\\b").matcher(text);
        while (m2.find()) {
            Integer year = parseIntSafe(m2.group(2));
            if (isValidYear(year)) {
                return year;
            }
        }

        Matcher m3 = Pattern.compile(
                "(?i)(vyrobeno|registrace|uvedeno do provozu|datum registrace|rok)\\s*[:\\- ]\\s*(19\\d{2}|20\\d{2})"
        ).matcher(text);

        if (m3.find()) {
            Integer year = parseIntSafe(m3.group(2));
            if (isValidYear(year)) {
                return year;
            }
        }

        return null;
    }

    private Integer extractYearFromAttributesText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String normalized = normalizeText(text);

        String[] patterns = {
                "(?i)rok výroby\\s*[:\\- ]\\s*(19\\d{2}|20\\d{2})",
                "(?i)rok vyroby\\s*[:\\- ]\\s*(19\\d{2}|20\\d{2})",
                "(?i)první registrace\\s*[:\\- ]\\s*(19\\d{2}|20\\d{2})",
                "(?i)prvni registrace\\s*[:\\- ]\\s*(19\\d{2}|20\\d{2})",
                "(?i)uvedení do provozu\\s*[:\\- ]\\s*(19\\d{2}|20\\d{2})",
                "(?i)uvedeni do provozu\\s*[:\\- ]\\s*(19\\d{2}|20\\d{2})",
                "(?i)modelový rok\\s*[:\\- ]\\s*(19\\d{2}|20\\d{2})",
                "(?i)modelovy rok\\s*[:\\- ]\\s*(19\\d{2}|20\\d{2})",
                "(?i)vyrobeno\\s*[:\\- ]\\s*(19\\d{2}|20\\d{2})",
                "(?i)registrace\\s*[:\\- ]\\s*(19\\d{2}|20\\d{2})"
        };

        for (String regex : patterns) {
            Matcher matcher = Pattern.compile(regex).matcher(normalized);
            if (matcher.find()) {
                Integer year = parseIntSafe(matcher.group(1));
                if (isValidYear(year)) {
                    return year;
                }
            }
        }

        return null;
    }

    private boolean looksSuspiciousFutureLikeYearInOldCarTitle(String title, Integer year) {
        if (title == null || title.isBlank() || year == null) {
            return false;
        }

        String t = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";

        boolean clearlyOldModel = containsAny(t,
                " cordoba ",
                " ibiza 2 ", " ibiza ii ",
                " felicia ",
                " favorit ",
                " forman ",
                " fabia i ", " fabia 1 ",
                " octavia i ", " octavia 1 ", " octavia tour ",
                " xsara ",
                " xantia ",
                " saxo ",
                " thalia ",
                " fiesta 1.3 ",
                " escort ",
                " astra g ",
                " scenic 1 ",
                " scenic i ",
                " clio 1 ", " clio 2 ",
                " megane i ", " megane ii ",
                " passat b5 ",
                " bora 1.9 tdi ",
                " golf iv ", " golf 4 ",
                " e39 ", " e46 ",
                " focus i ",
                " mondeo iii ",
                " fox 1.2 ",
                " 1.4 basis ",
                " 1.9d ",
                " 1,9d ",
                " 1.6i ",
                " 1,6i ",
                " 1.4i ",
                " 1,4i ",
                " 1.2i ",
                " 1,2i ");

        boolean suspiciousFutureYear = year >= 2024;

        return clearlyOldModel && suspiciousFutureYear;
    }

    private boolean isKnownModelNumber(String rawYearLike, String title) {
        String t = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";
        String y = rawYearLike;

        return ("2008".equals(y) && t.contains(" peugeot 2008 "))
                || ("3008".equals(y) && t.contains(" peugeot 3008 "))
                || ("5008".equals(y) && t.contains(" peugeot 5008 "))
                || ("1007".equals(y) && t.contains(" peugeot 1007 "));
    }

    private Integer extractMileage(String text) {
        String normalized = normalizeText(text);

        Matcher kmMatcher = Pattern.compile(
                "(tachometr|najeto|najetých km|najetych km|stav tachometru)?\\s*[:\\- ]*([0-9]{1,3}(?:[ \\u00A0][0-9]{3})+|[0-9]{4,7})\\s*km",
                Pattern.CASE_INSENSITIVE
        ).matcher(normalized);

        while (kmMatcher.find()) {
            Integer km = parseIntSafe(kmMatcher.group(2));
            if (km != null && km >= 1000 && km <= 1_500_000) {
                return km;
            }
        }

        return null;
    }

    private String extractFuelType(String text) {
        String normalized = " " + normalizeText(text).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(normalized, " elektro ", " elektrické ", " elektricke ", " electric ", " ev ", " kwh ", " soh ")) {
            return "ELECTRIC";
        }
        if (containsAny(normalized, " plug-in-hybrid ", " plug in hybrid ", " phev ")) {
            return "PLUGIN_HYBRID";
        }
        if (containsAny(normalized, " hybridní ", " hybridni ", " hybrid ", " mhev ", " hev ", " 300h ", " 450h ", " 250h ", " xdrive30e ", " t8 ")) {
            return "HYBRID";
        }
        if (containsAny(normalized, " nafta ", " diesel ", " tdi ", " tdci ", " hdi ", " dci ", " cdi ", " crdi ", " jtd ")) {
            return "DIESEL";
        }
        if (containsAny(normalized, " benzín ", " benzin ", " tsi ", " tfsi ", " gdi ", " t-gdi ", " tgdi ", " mpi ", " vvt-i ", " fsi ", " i-vtec ", " tce ", " ecoboost ")) {
            return "PETROL";
        }
        if (containsAny(normalized, " lpg ")) {
            return "LPG";
        }
        if (containsAny(normalized, " cng ")) {
            return "CNG";
        }

        return null;
    }

    private String extractTransmission(String text) {
        String normalized = " " + normalizeText(text).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(normalized,
                " dsg ",
                " automat ",
                " automatická ",
                " automaticka ",
                " automatická převodovka ",
                " automaticka prevodovka ",
                " tiptronic ",
                " cvt ",
                " s tronic ",
                " stronic ",
                " powershift ",
                " edc ",
                " 7g-tronic ",
                " 9g-tronic ",
                " g-tronic ",
                " 7dct ",
                " 8at ",
                " 6at ",
                " 5at ")) {
            return "AUTOMATIC";
        }

        if (containsAny(normalized,
                " manuál ",
                " manual ",
                " manuální ",
                " manualni ",
                " manuální převodovka ",
                " manualni prevodovka ",
                " 5mt ",
                " 6mt ",
                " manuál, ",
                " manual, ")) {
            return "MANUAL";
        }

        return null;
    }

    private String extractCarType(String title, String text, String url) {
        String titleSource = " " + normalizeText(safe(title)).toLowerCase(Locale.ROOT) + " ";
        String textSource = " " + normalizeText(safe(text)).toLowerCase(Locale.ROOT) + " ";
        String urlSource = " " + normalizeText(safe(url)).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(titleSource,
                " kombi ", " combi ", " wagon ", " estate ", " touring ", " avant ", " variant ",
                " caravan ", " sw ", " shooting brake ", " sport tourer ", " sports tourer ",
                " grandtour ", " grand tour ")) {
            return "WAGON";
        }

        if (containsAny(titleSource, " hatchback ", " spaceback ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " sedan ", " limuzína ", " limuzina ", " liftback ", " fastback ", " sportback ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource, " suv ", " crossover ", " off-road ", " offroad ")) {
            return "SUV";
        }

        if (containsAny(titleSource, " mpv ", " minivan ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource, " pickup ", " pick-up ", " crew cab ")) {
            return "PICKUP";
        }

        if (containsAny(titleSource,
                " coupé ", " coupe ", " gran coupe ", " gran coupé ",
                " amg gt ", " gt c ", " gt s ", " gt r ",
                " cla ", " cla 45 ", " cla45 ",
                " rs5 ", " m4 ", " supra ", " brz ", " gt86 ", " 86 ")) {
            return "COUPE";
        }

        if (containsAny(titleSource,
                " cabrio ", " roadster ", " spider ", " spyder ",
                " convertible ", " cabriolet ", " targa ")) {
            return "CABRIO";
        }

        if (containsAny(titleSource,
                " puma ", " formentor ", " captur ", " austral ", " kadjar ", " koleos ", " arkana ",
                " kuga ", " ecosport ", " edge ", " tiguan ", " touareg ", " t-roc ", " t-cross ",
                " kodiaq ", " karoq ", " kamiq ", " yeti ", " qashqai ", " x-trail ", " juke ",
                " rav4 ", " chr ", " c-hr ", " hr-v ", " cr-v ", " tucson ", " kona ",
                " santa fe ", " sportage ", " sorento ", " stonic ", " x1 ", " x2 ", " x3 ",
                " x4 ", " x5 ", " x6 ", " x7 ", " gla ", " glb ", " glc ", " gle ", " gls ",
                " q2 ", " q3 ", " q4 ", " q5 ", " q7 ", " q8 ", " xc40 ", " xc60 ",
                " xc90 ", " ex30 ", " ex40 ", " ex90 ", " ux ", " nx ", " rx ",
                " duster ", " macan ", " cayenne ", " model x ", " model y ", " defender ",
                " ateca ", " ev6 ", " 2008 ", " eqs suv ", " eqb ", " enyq ", " terramar ")) {
            return "SUV";
        }

        if (containsAny(titleSource,
                " v40 ", " v50 ", " v60 ", " v70 ", " v90 ",
                " octavia combi ", " superb combi ", " passat variant ", " golf variant ",
                " focus kombi ", " focus wagon ", " astra caravan ", " astra sports tourer ",
                " ceed sw ", " proceed sw ", " leon st ", " leon sp ",
                " peugeot 308 sw ", " peugeot 508 sw ",
                " bmw 3 touring ", " bmw 5 touring ", " audi a4 avant ", " audi a6 avant ")) {
            return "WAGON";
        }

        if (containsAny(titleSource,
                " clio ", " fabia ", " scala ", " polo ", " golf ", " fiesta ",
                " corsa ", " i20 ", " i30 ", " ceed ", " mazda 3 ", " a-class ",
                " třídy a ", " tridy a ", " civic ", " megane ", " c2 ", " c3 ",
                " xsara ", " agila ", " 207 ", " 208 ", " i3 ", " r5 ", " id.3 ", " id3 ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource,
                " charger ", " octavia ", " superb ", " passat ", " arteon ", " a4 ",
                " a6 ", " a8 ", " e90 ", " e60 ", " e39 ", " 3 series ", " 5 series ",
                " c5 ", " mondeo sedan ", " model 3 ", " model s ", " cordoba ")) {
            return "SEDAN";
        }

        if (containsAny(titleSource,
                " caddy ", " sharan ", " alhambra ", " touran ", " scenic ", " espace ",
                " zafira ", " meriva ", " s-max ", " galaxy ", " b-max ", " c-max ",
                " grand c-max ", " roomster ", " lodgy ", " verso ", " rifter ",
                " berlingo ", " combo ", " doblo ", " vaneo ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource,
                " ram 1500 ", " ram 2500 ", " ram 3500 ", " dodge ram ",
                " ranger ", " hilux ", " amarok ", " navara ", " l200 ", " gladiator ")) {
            return "PICKUP";
        }

        if (containsAny(urlSource, "/ram/", "/gladiator/", "/amarok/", "/hilux/", "/ranger/")) {
            return "PICKUP";
        }

        if (containsAny(textSource, " suv ", " crossover ", " off-road ", " offroad ")) {
            return "SUV";
        }

        if (containsAny(textSource,
                " kombi ", " combi ", " wagon ", " estate ", " touring ", " avant ",
                " variant ", " caravan ", " shooting brake ", " sport tourer ",
                " sports tourer ", " grandtour ")) {
            return "WAGON";
        }

        if (containsAny(textSource, " hatchback ", " spaceback ")) {
            return "HATCHBACK";
        }

        if (containsAny(textSource, " sedan ", " liftback ", " fastback ", " sportback ", " limuzína ", " limuzina ")) {
            return "SEDAN";
        }

        if (containsAny(textSource, " mpv ", " minivan ")) {
            return "MINIVAN";
        }

        if (containsAny(textSource,
                " coupé ", " coupe ", " gran coupe ", " gran coupé ",
                " roadster ", " cabrio ", " convertible ", " cabriolet ",
                " spider ", " spyder ")) {
            if (containsAny(textSource, " roadster ", " cabrio ", " convertible ", " cabriolet ", " spider ", " spyder ")) {
                return "CABRIO";
            }
            return "COUPE";
        }

        if (containsAny(titleSource,
                " scenic ", " scénic ",
                " zafira ",
                " mazda 5 ",
                " touran ",
                " sharan ",
                " c-max ",
                " s-max ",
                " meriva ")) {
            return "MINIVAN";
        }

        if (containsAny(titleSource, " megane ")
                && !containsAny(titleSource, " kombi ", " combi ", " grandtour ", " estate ", " wagon ")) {
            return "HATCHBACK";
        }

        if (containsAny(titleSource, " rio ")
                && !containsAny(titleSource, " kombi ", " combi ", " wagon ", " estate ")) {
            return "HATCHBACK";
        }

        return null;
    }

    private String extractJsonValue(String json, String key) {
        if (json == null || json.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return normalizeText(unescapeJson(matcher.group(1)));
        }

        return null;
    }

    private String unescapeJson(String value) {
        if (value == null) {
            return null;
        }

        return value
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ");
    }

    private boolean containsKcPrice(String text) {
        return text != null && text.matches(".*\\d[\\d\\s]*\\s*Kč.*");
    }

    private List<Integer> extractAllPrices(String text) {
        List<Integer> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        Matcher matcher = Pattern.compile("(\\d[\\d ]{0,15})\\s*Kč", Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) {
            Integer value = parsePriceToInt(matcher.group(1));
            if (value != null) {
                result.add(value);
            }
        }

        return result;
    }

    private Integer parsePriceToInt(String raw) {
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

    private String formatPrice(Integer priceValue) {
        if (priceValue == null) {
            return null;
        }
        return String.format(Locale.US, "%,d Kč", priceValue).replace(",", " ");
    }

    private boolean isValidYear(Integer year) {
        return year != null && year >= MIN_YEAR && year <= CURRENT_YEAR;
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
        if (value.startsWith("LANCIA")) return "LANCIA";

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
            if (part.isBlank()) continue;

            if (sb.length() > 0) {
                sb.append(' ');
            }

            sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }

        return sb.toString();
    }

    private String shortenForCheck(String value, int maxLen) {
        String normalized = normalizeText(value);
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen);
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

    private boolean looksClearlyOldCarTitle(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }

        String t = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";

        return containsAny(t,
                " clio ",
                " storia ",
                " megane ",
                " scenic ",
                " xsara ",
                " c5 ",
                " c4 ",
                " c2 ",
                " fabia ",
                " octavia i ",
                " octavia 1 ",
                " octavia ii ",
                " octavia 2 ",
                " passat b5 ",
                " passat ",
                " bora ",
                " almera ",
                " micra ",
                " getz ",
                " accent ",
                " meriva ",
                " zafira ",
                " astra ",
                " corsa ",
                " felicia ",
                " forman ",
                " favorita ",
                " yaris ",
                " rio ",
                " kalos ",
                " punto ",
                " bravo ",
                " cordoba ",
                " golf iv ",
                " golf 4 ",
                " peugeot 206 ",
                " peugeot 307 ",
                " v50 ",
                " alfa romeo 156 ");
    }

    private record PriceCandidate(Integer price, String raw) {}
    private record ParseResult(CarDto car, String reason) {}
}
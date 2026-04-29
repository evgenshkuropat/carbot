package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SbazarParser implements CarSourceParser {

    private static final Logger log = LoggerFactory.getLogger(SbazarParser.class);

    private static final String BASE_URL = "https://www.sbazar.cz/170-osobni-auta";
    private static final int REQUEST_TIMEOUT_MS = 20_000;
    private static final int MAX_LIST_PAGES = 10;
    private static final int MAX_DETAIL_LINKS = 250;
    private static final int MIN_VALID_PRICE = 30_000;
    private static final int MAX_VALID_PRICE = 10_000_000;

    @Override
    public String getSourceName() {
        return "SBAZAR";
    }

    @Override
    public List<CarDto> fetchCars() {
        log.warn("SBAZAR temporarily disabled");
        return List.of();
    }

    private String buildListPageUrl(int page) {
        if (page <= 1) {
            return BASE_URL;
        }

        return BASE_URL + "/" + page;
    }

    private Set<String> extractDetailUrls(Document doc) {
        Set<String> urls = new LinkedHashSet<>();

        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String href = link.absUrl("href");

            if (href == null || href.isBlank()) {
                continue;
            }

            if (!href.contains("sbazar.cz")) {
                continue;
            }

            if (href.contains("/detail/") || href.matches(".*/[0-9]+-[^/?#]+.*")) {
                if (!href.contains("/170-osobni-auta")) {
                    urls.add(stripUrlParams(href));
                }
            }
        }

        return urls;
    }

    private ParseResult parseDetail(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(REQUEST_TIMEOUT_MS)
                    .get();

            String title = extractTitle(doc);
            String description = extractDescription(doc);
            String fullText = normalizeText(doc.text());
            String analysisText = normalizeText(title + " " + description + " " + fullText);

            if (title.isBlank()) {
                log.warn("SBAZAR SKIP url={} reason=empty_title", safe(url));
                return ParseResult.skip("empty_title");
            }

            if (looksNonCarListing(title, analysisText)) {
                log.warn("SBAZAR SKIP url={} reason=non_car_listing title='{}'", safe(url), safe(title));
                return ParseResult.skip("non_car_listing");
            }

            Integer priceValue = extractPrice(doc, analysisText);

            if (!isValidPrice(priceValue)) {
                log.warn("SBAZAR SKIP url={} reason=invalid_price title='{}' price={}",
                        safe(url), safe(title), priceValue);
                return ParseResult.skip("invalid_price");
            }

            String price = formatPrice(priceValue);
            String location = extractLocation(doc, analysisText);
            Integer year = extractYear(title, analysisText);
            Integer mileage = extractMileage(title, analysisText);
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
            String brand = extractBrand(title, analysisText);
            String carType = extractCarType(title, analysisText, url);
            String imageUrl = extractImageUrl(doc);

            CarDto car = new CarDto();
            car.setSource("SBAZAR");
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

            log.info("SBAZAR CAR title='{}' price={} location={} year={} mileage={} fuelType={} transmission={} carType={} brand={} url={}",
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
            log.warn("SBAZAR SKIP url={} reason=parse_error error={}", safe(url), e.getMessage());
            return ParseResult.skip("parse_error");
        }
    }

    private String extractTitle(Document doc) {
        Element h1 = doc.selectFirst("h1");

        if (h1 != null) {
            return normalizeText(h1.text());
        }

        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null) {
            return normalizeText(ogTitle.attr("content"));
        }

        return "";
    }

    private String extractDescription(Document doc) {
        Element meta = doc.selectFirst("meta[property=og:description], meta[name=description]");
        if (meta != null) {
            String value = normalizeText(meta.attr("content"));
            if (!value.isBlank()) {
                return value;
            }
        }

        Element description = doc.selectFirst("[data-testid*=description], .description, article, main");
        if (description != null) {
            return normalizeText(description.text());
        }

        return "";
    }

    private Integer extractPrice(Document doc, String text) {
        Element priceEl = doc.selectFirst(
                "[data-testid*=price], " +
                        "[class*=price], " +
                        "[class*=Price], " +
                        "span:contains(Kč), " +
                        "div:contains(Kč)"
        );

        if (priceEl != null) {
            Integer price = parseNumber(priceEl.text());
            if (isValidPrice(price)) {
                return price;
            }
        }

        Matcher matcher = Pattern.compile(
                "(?i)\\b([0-9]{2,3}(?:[\\s\\.][0-9]{3})+|[0-9]{5,8})\\s*(?:kč|kc|czk)\\b"
        ).matcher(text);

        while (matcher.find()) {
            Integer price = parseNumber(matcher.group(1));

            if (isValidPrice(price)) {
                return price;
            }
        }

        return null;
    }

    private String extractLocation(Document doc, String text) {
        Element locationEl = doc.selectFirst(
                "[data-testid*=locality], " +
                        "[data-testid*=location], " +
                        "[class*=locality], " +
                        "[class*=location], " +
                        "[class*=Location]"
        );

        if (locationEl != null) {
            String raw = normalizeText(locationEl.text());
            raw = raw.replaceFirst("(?i)^v\\s+", "").trim();

            if (isRealLocation(raw)) {
                return raw;
            }
        }

        Matcher matcher = Pattern.compile(
                "(?i)\\b(?:v|lokalita|město|mesto|okres)\\s+([A-ZÁČĎÉĚÍŇÓŘŠŤÚŮÝŽa-záčďéěíňóřšťúůýž0-9\\- ,]{2,60})"
        ).matcher(text);

        if (matcher.find()) {
            String raw = normalizeText(matcher.group(1));

            if (isRealLocation(raw)) {
                return raw;
            }
        }

        return null;
    }

    private Integer extractYear(String title, String text) {
        String source = normalizeText(title + " " + text);

        Matcher matcher = Pattern.compile(
                "(?i)(?:rok výroby|rok vyroby|r\\.v\\.?|rv|první registrace|prvni registrace|do provozu)\\s*[:\\-]?\\s*(?:\\d{1,2}\\s*/\\s*)?(19\\d{2}|20\\d{2})"
        ).matcher(source);

        if (matcher.find()) {
            return parseYearCandidate(matcher.group(1));
        }

        matcher = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(source);

        while (matcher.find()) {
            String rawYear = matcher.group(1);

            String lower = source.toLowerCase(Locale.ROOT);
            if ("2008".equals(rawYear) && lower.contains("peugeot 2008")) {
                continue;
            }
            if ("3008".equals(rawYear) && lower.contains("peugeot 3008")) {
                continue;
            }
            if ("5008".equals(rawYear) && lower.contains("peugeot 5008")) {
                continue;
            }

            Integer year = parseYearCandidate(rawYear);

            if (year != null && !isBadYearContext(source, matcher.start(), matcher.end())) {
                return year;
            }
        }

        return null;
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

        // LPG
        if (containsAny(source, " lpg ")) {
            return "LPG";
        }

        // CNG
        if (containsAny(source, " cng ")) {
            return "CNG";
        }

        // DIESEL (ставим раньше ELECTRIC!)
        if (containsAny(source,
                " diesel ", " nafta ", " tdi ", " tdci ", " cdi ", " dci ", " hdi ",
                " crdi ", " jtd ", " multijet ", " bluehdi ", " cdti ")) {
            return "DIESEL";
        }

        // PETROL
        if (containsAny(source,
                " benzin ", " benzín ", " tsi ", " tfsi ", " fsi ", " mpi ", " gdi ",
                " tgdi ", " tce ", " ecoboost ", " vti ", " vvt-i ", " i-vtec ",
                " skyactiv-g ", " 1.0i ", " 1.2i ", " 1.4i ", " 1.6i ", " 2.0i ")) {
            return "PETROL";
        }

        // HYBRID
        if (containsAny(source,
                " hybrid ", " hybridní ", " hybridni ",
                " plug-in ", " plugin ", " phev ", " hev ")) {
            return "HYBRID";
        }

        // ELECTRIC (самый последний!)
        if (containsAny(source,
                " elektro ",
                " elektromobil ",
                " elektroauto ",
                " electric ",
                " bev ",
                " kwh ")) {
            return "ELECTRIC";
        }

        return null;
    }

    private String extractTransmission(String text) {
        String source = " " + normalizeText(text).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(source,
                " automatická ", " automaticka ", " automat ", " automatic ", " automatu ",
                " aut. ", " a/t ", " cvt ", " e-cvt ", " dsg ", " s tronic ",
                " tiptronic ", " powershift ", " steptronic ", " x-tronic ")) {
            return "AUTOMATIC";
        }

        if (containsAny(source,
                " manuální ", " manualni ", " manuál ", " manual ", " man. ",
                " 5mt ", " 6mt ", " 5q ", " 6q ")) {
            return "MANUAL";
        }

        return null;
    }

    private String extractBrand(String title, String text) {
        String source = " " + normalizeText(title + " " + shortenForCheck(text, 300)).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(source, " škoda ", " skoda ", " octavia ", " superb ", " fabia ", " kodiaq ", " karoq ", " kamiq ")) return "SKODA";
        if (containsAny(source, " volkswagen ", " vw ", " golf ", " passat ", " tiguan ", " touareg ", " touran ")) return "VOLKSWAGEN";
        if (containsAny(source, " audi ")) return "AUDI";
        if (containsAny(source, " bmw ")) return "BMW";
        if (containsAny(source, " mercedes ", " mercedes-benz ")) return "MERCEDES";
        if (containsAny(source, " toyota ")) return "TOYOTA";
        if (containsAny(source, " ford ", " focus ", " mondeo ", " fiesta ")) return "FORD";
        if (containsAny(source, " renault ", " megane ", " scenic ", " clio ")) return "RENAULT";
        if (containsAny(source, " seat ")) return "SEAT";
        if (containsAny(source, " peugeot ")) return "PEUGEOT";
        if (containsAny(source, " opel ")) return "OPEL";
        if (containsAny(source, " hyundai ")) return "HYUNDAI";
        if (containsAny(source, " kia ")) return "KIA";
        if (containsAny(source, " volvo ")) return "VOLVO";
        if (containsAny(source, " lexus ")) return "LEXUS";
        if (containsAny(source, " mazda ")) return "MAZDA";
        if (containsAny(source, " citroën ", " citroen ")) return "CITROEN";
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
        if (containsAny(source, " land rover ", " range rover ")) return "LAND_ROVER";
        if (containsAny(source, " chevrolet ")) return "CHEVROLET";

        return null;
    }

    private String extractCarType(String title, String text, String url) {
        String source = " " + normalizeText(title + " " + shortenForCheck(text, 500) + " " + url)
                .toLowerCase(Locale.ROOT) + " ";

        if (containsAny(source,
                " kombi ", " combi ", " wagon ", " variant ", " touring ", " avant ",
                " estate ", " caravan ", " grandtour ", " shooting brake ", " alltrack ", " scout ")) {
            return "WAGON";
        }

        if (containsAny(source,
                " suv ", " crossover ", " kodiaq ", " karoq ", " kamiq ", " tiguan ",
                " touareg ", " t-roc ", " qashqai ", " kuga ", " x1 ", " x3 ", " x5 ",
                " q3 ", " q5 ", " q7 ", " xc40 ", " xc60 ", " xc90 ", " glc ", " gle ",
                " sportage ", " tucson ", " santa fe ", " duster ", " rav4 ", " cx-5 ",
                " range rover ", " evoque ", " velar ", " enyaq ", " id.4 ", " id.5 ")) {
            return "SUV";
        }

        if (containsAny(source,
                " mpv ", " minivan ", " scenic ", " espace ", " touran ", " sharan ",
                " alhambra ", " galaxy ", " s-max ", " c-max ", " berlingo ", " caddy ",
                " zafira ", " roomster ")) {
            return "MINIVAN";
        }

        if (containsAny(source,
                " hatchback ", " hatch ", " spaceback ", " fabia ", " golf ", " polo ",
                " fiesta ", " corsa ", " i20 ", " i30 ", " ceed ", " clio ", " civic ",
                " leon ", " citigo ")) {
            return "HATCHBACK";
        }

        if (containsAny(source,
                " cabrio ", " kabrio ", " roadster ", " spider ", " spyder ",
                " convertible ", " cabriolet ")) {
            return "CABRIO";
        }

        if (containsAny(source,
                " pickup ", " pick-up ", " ranger ", " hilux ", " amarok ", " navara ",
                " l200 ", " ram 1500 ")) {
            return "PICKUP";
        }

        if (containsAny(source,
                " coupe ", " coupé ", " gran coupe ", " gran coupé ", " mustang ",
                " tt ", " supra ", " brz ", " gt86 ", " r8 ")) {
            return "COUPE";
        }

        if (containsAny(source,
                " sedan ", " saloon ", " limousine ", " limuzina ", " limuzína ",
                " liftback ", " fastback ", " octavia ", " superb ", " passat ",
                " arteon ", " a4 ", " a6 ", " model 3 ", " model s ")) {
            return "SEDAN";
        }

        return null;
    }

    private String extractImageUrl(Document doc) {
        Element img = doc.selectFirst(
                "meta[property=og:image], " +
                        "img[src], " +
                        "img[data-src], " +
                        "source[srcset]"
        );

        if (img == null) {
            return null;
        }

        String src = firstNonBlank(
                img.hasAttr("content") ? img.absUrl("content") : null,
                img.hasAttr("data-src") ? img.absUrl("data-src") : null,
                img.hasAttr("src") ? img.absUrl("src") : null,
                img.hasAttr("srcset") ? firstSrcsetUrl(img.attr("srcset")) : null
        );

        if (src == null) {
            return null;
        }

        String lower = src.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "logo", "favicon", "icon", "placeholder")) {
            return null;
        }

        return src;
    }

    private boolean looksNonCarListing(String title, String text) {
        String source = " " + normalizeText(title + " " + shortenForCheck(text, 700)).toLowerCase(Locale.ROOT) + " ";

        boolean hasCarBrand = extractBrand(title, text) != null;

        boolean hasCarSignals = containsAny(source,
                " najeto ", " km ", " tdi ", " tsi ", " dsg ", " automat ", " manual ",
                " benzin ", " benzín ", " diesel ", " nafta ", " rok výroby ", " rv ",
                " stk ", " combi ", " kombi ", " hatchback ", " sedan ", " suv ");

        if (hasCarBrand && hasCarSignals) {
            return false;
        }

        return containsAny(source,
                " pneu ", " pneumatiky ", " alu kola ", " disky ", " ráfky ", " rafky ",
                " náhradní díly ", " nahradni dily ", " motor na prodej ",
                " převodovka ", " prevodovka ", " světlo ", " svetlo ", " světla ", " svetla ",
                " nárazník ", " naraznik ", " blatník ", " blatnik ",
                " střešní nosič ", " stresni nosic ", " thule ",
                " pronájem ", " pronajem ", " půjčení ", " pujceni ",
                " operativní leasing ", " operativni leasing ");
    }

    private boolean isValidPrice(Integer price) {
        return price != null && price >= MIN_VALID_PRICE && price <= MAX_VALID_PRICE;
    }

    private Integer parseYearCandidate(String raw) {
        try {
            int year = Integer.parseInt(raw);
            int currentYear = java.time.Year.now().getValue();

            if (year >= 1990 && year <= currentYear) {
                return year;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private boolean isBadYearContext(String text, int start, int end) {
        int from = Math.max(0, start - 60);
        int to = Math.min(text.length(), end + 60);
        String context = text.substring(from, to).toLowerCase(Locale.ROOT);

        return context.contains("stk do")
                || context.contains("technick")
                || context.contains("platná do")
                || context.contains("platna do")
                || context.contains("záruka do")
                || context.contains("zaruka do");
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
        if (value == null || value.isBlank()) {
            return false;
        }

        String lower = normalizeText(value).toLowerCase(Locale.ROOT);

        return !lower.equals("lokalita")
                && !lower.equals("město")
                && !lower.equals("mesto")
                && !lower.equals("okres")
                && !lower.equals("kraj")
                && lower.length() >= 2
                && lower.length() <= 80;
    }

    private String stripUrlParams(String url) {
        if (url == null) {
            return null;
        }

        int questionIndex = url.indexOf('?');
        if (questionIndex >= 0) {
            return url.substring(0, questionIndex);
        }

        return url;
    }

    private String firstSrcsetUrl(String srcset) {
        if (srcset == null || srcset.isBlank()) {
            return null;
        }

        String first = srcset.split(",")[0].trim();
        String url = first.split("\\s+")[0].trim();

        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        return null;
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

    private boolean containsAny(String source, String... values) {
        if (source == null || source.isBlank()) {
            return false;
        }

        String normalizedSource = source.toLowerCase(Locale.ROOT);

        for (String value : values) {
            if (value != null && !value.isBlank()
                    && normalizedSource.contains(value.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized
                .replace('\u00A0', ' ')
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
}
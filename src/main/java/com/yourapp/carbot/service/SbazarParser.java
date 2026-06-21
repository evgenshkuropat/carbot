package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import jakarta.annotation.PostConstruct;
import org.jsoup.Connection;
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
import java.text.Normalizer;
import java.time.Year;
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

    private static final String BASE_HOST = "https://www.sbazar.cz";
    private static final String BASE_LIST_URL = BASE_HOST + "/170-osobni-auta";
    private static final int MAX_LIST_PAGES = 3;
    private static final int REQUEST_TIMEOUT_MS = 20_000;
    private static final int DETAIL_DELAY_MS = 150;
    private static final int MIN_VALID_PRICE = 30_000;
    private static final int MAX_REASONABLE_PRICE = 10_000_000;

    private static final Pattern PRICE_PATTERN = Pattern.compile("(?i)(\\d[\\d\\s.]{2,})\\s*(?:k\\u010d|kc|czk)");
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b");
    private static final Pattern MILEAGE_KM_PATTERN = Pattern.compile("(?i)(\\d[\\d\\s.]{2,})\\s*(?:km|kilometru)");
    private static final Pattern MILEAGE_NAJETO_PATTERN = Pattern.compile("(?i)(?:najeto|najezd)\\s*(\\d[\\d\\s.]{2,})\\s*(?:km|kilometru)?");
    private static final Pattern MILEAGE_TIS_PATTERN = Pattern.compile("(?i)\\b(\\d{2,3})\\s*(?:tis\\.?|tkm)\\b");

    @PostConstruct
    public void init() {
        log.info("SBAZAR parser bean initialized baseUrl={} maxPages={}", BASE_LIST_URL, MAX_LIST_PAGES);
    }

    @Override
    public String getSourceName() {
        return "SBAZAR";
    }

    @Override
    public List<CarDto> fetchCars() {
        List<CarDto> cars = new ArrayList<>();
        Set<String> detailUrls = new LinkedHashSet<>();
        Stats stats = new Stats();

        for (int page = 1; page <= MAX_LIST_PAGES; page++) {
            String pageUrl = buildListPageUrl(page);

            try {
                Document doc = connect(pageUrl).get();
                Set<String> pageLinks = extractDetailUrls(doc);
                int before = detailUrls.size();
                detailUrls.addAll(pageLinks);
                int newLinks = detailUrls.size() - before;

                log.info("SBAZAR list page={} url={} links={} new_links={} total_unique={}",
                        page, pageUrl, pageLinks.size(), newLinks, detailUrls.size());

                if (pageLinks.isEmpty()) {
                    log.info("SBAZAR list page={} no detail links sample_hrefs={}", page, sampleHrefs(doc));
                }

                if (page > 1 && (pageLinks.isEmpty() || newLinks == 0)) {
                    log.info("SBAZAR list page={} produced no new links, stopping pagination", page);
                    break;
                }
            } catch (Exception e) {
                stats.listFetchFailed++;
                log.warn("SBAZAR list page={} url={} fetch_failed={}", page, pageUrl, e.toString());
            }
        }

        log.info("SBAZAR collected detail links={}", detailUrls.size());

        for (String url : detailUrls) {
            try {
                ParseResult result = parseDetail(url);

                if (result.car() == null) {
                    stats.countSkip(result.reason());
                    log.info("SBAZAR SKIP url={} reason={} title={}", url, result.reason(), safeLog(result.title()));
                    continue;
                }

                cars.add(result.car());
                stats.parsed++;

                CarDto car = result.car();
                log.info("SBAZAR CAR title='{}' price={} location={} year={} mileage={} fuelType={} transmission={} carType={} brand={} url={}",
                        safeLog(car.getTitle()),
                        car.getPriceValue(),
                        safeLog(car.getLocation()),
                        car.getYear(),
                        car.getMileage(),
                        safeLog(car.getFuelType()),
                        safeLog(car.getTransmission()),
                        safeLog(car.getCarType()),
                        safeLog(car.getBrand()),
                        car.getUrl());
            } catch (Exception e) {
                stats.parseException++;
                log.warn("SBAZAR SKIP url={} reason=parse_exception error={}", url, e.toString());
            }

            sleepQuietly(DETAIL_DELAY_MS);
        }

        log.info("SBAZAR parsed {} cars", cars.size());
        log.info("SBAZAR SUMMARY parsed={} list_fetch_failed={} broken_listing={} non_car_listing={} demand_listing={} commercial_vehicle={} cheap_low_quality_listing={} missing_price={} invalid_price={} parse_exception={}",
                stats.parsed,
                stats.listFetchFailed,
                stats.brokenListing,
                stats.nonCarListing,
                stats.demandListing,
                stats.commercialVehicle,
                stats.cheapLowQualityListing,
                stats.missingPrice,
                stats.invalidPrice,
                stats.parseException);

        return cars;
    }

    private ParseResult parseDetail(String url) throws Exception {
        Document doc = connect(url).get();
        String title = firstNonBlank(
                textOf(doc, "h1"),
                attrOf(doc, "meta[property=og:title]", "content"),
                doc.title()
        );
        title = cleanTitle(title);

        if (title == null || title.isBlank()) {
            return ParseResult.skip("broken_listing", title);
        }

        String listingText = extractListingText(doc);
        String pageText = doc.text();
        String searchable = asciiSearchText(title + " " + listingText + " " + url);
        String listingIdentity = asciiSearchText(title + " " + url);

        if (looksDemandListing(listingIdentity)) {
            return ParseResult.skip("demand_listing", title);
        }

        if (looksNonCarListing(listingIdentity)) {
            return ParseResult.skip("non_car_listing", title);
        }

        if (looksCommercialVehicle(listingIdentity)) {
            return ParseResult.skip("commercial_vehicle", title);
        }

        Integer priceValue = extractPriceValue(doc, pageText);
        if (priceValue == null) {
            log.info("SBAZAR PRICE NOT FOUND url={} title={}", url, safeLog(title));
            return ParseResult.skip("missing_price", title);
        }

        if (priceValue <= 0 || priceValue > MAX_REASONABLE_PRICE) {
            return ParseResult.skip("invalid_price", title);
        }

        if (priceValue < MIN_VALID_PRICE) {
            return ParseResult.skip("cheap_low_quality_listing", title);
        }

        String location = extractLocation(doc);
        String outputTitle = repairMojibake(title);
        String outputLocation = repairMojibake(location);

        CarDto car = new CarDto();
        car.setSource(getSourceName());
        car.setTitle(outputTitle);
        car.setPrice(formatPrice(priceValue));
        car.setPriceValue(priceValue);
        car.setLocation(outputLocation);
        car.setUrl(url);
        car.setImageUrl(extractImageUrl(doc));
        String identityText = asciiSearchText(title + " " + url);
        String scopedText = asciiSearchText(title + " " + listingText + " " + url);

        car.setBrand(firstDetected(detectBrand(identityText), detectBrand(scopedText)));
        car.setYear(resolveYear(asciiSearchText(title), scopedText));
        car.setMileage(firstInteger(extractMileage(identityText), extractMileage(scopedText)));
        car.setFuelType(resolveFuelType(identityText, scopedText));
        car.setTransmission(resolveTransmission(identityText, scopedText, car.getFuelType()));
        car.setCarType(resolveCarType(identityText, scopedText));

        return ParseResult.car(car, title);
    }

    private String extractListingText(Document doc) {
        List<String> parts = new ArrayList<>();

        addIfPresent(parts, attrOf(doc, "meta[name=description]", "content"));
        addIfPresent(parts, attrOf(doc, "meta[property=og:description]", "content"));

        String[] selectors = {
                "[data-testid*=description]",
                "[data-testid*=parameter]",
                "[data-testid*=locality]",
                "[data-testid*=location]",
                "[class*=description]",
                "[class*=param]"
        };

        for (String selector : selectors) {
            for (Element element : doc.select(selector)) {
                String text = normalizeText(element.text());
                if (text != null && !text.isBlank() && text.length() <= 2_000) {
                    parts.add(text);
                }
            }
        }

        return String.join(" ", parts);
    }

    private void addIfPresent(List<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value);
        }
    }

    private Connection connect(String url) {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                .referrer(BASE_HOST)
                .header("Accept-Language", "cs-CZ,cs;q=0.9,en;q=0.8")
                .timeout(REQUEST_TIMEOUT_MS)
                .followRedirects(true);
    }

    private String buildListPageUrl(int page) {
        if (page <= 1) {
            return "https://www.sbazar.cz/170-osobni-auta";
        }

        return "https://www.sbazar.cz/170-osobni-auta/cela-cr/cena-neomezena/nejnovejsi/" + page;
    }

    private Set<String> extractDetailUrls(Document doc) {
        Set<String> links = new LinkedHashSet<>();

        for (Element a : doc.select("a[href]")) {
            String href = a.absUrl("href");
            href = stripUrlParams(href);

            if (href == null || href.isBlank()) {
                continue;
            }

            if (isLikelyDetailUrl(href)) {
                links.add(href);
            }
        }

        return links;
    }

    private List<String> sampleHrefs(Document doc) {
        List<String> samples = new ArrayList<>();

        for (Element a : doc.select("a[href]")) {
            String href = a.absUrl("href");
            if (href == null || href.isBlank()) {
                href = a.attr("href");
            }

            if (href != null && !href.isBlank()) {
                samples.add(stripUrlParams(href));
            }

            if (samples.size() >= 8) {
                break;
            }
        }

        return samples;
    }

    private boolean isLikelyDetailUrl(String href) {
        String lower = href.toLowerCase(Locale.ROOT);

        if (!lower.startsWith(BASE_HOST) || lower.contains("/170-osobni-auta")) {
            return false;
        }

        if (lower.contains("/inzerat/") || lower.contains("/detail/")) {
            return true;
        }

        return lower.matches("https://www\\.sbazar\\.cz/.*/[0-9]{6,}.*");
    }

    private Integer extractPriceValue(Document doc, String pageText) {
        String[] selectors = {
                "meta[property=product:price:amount]",
                "[itemprop=price]",
                "[class*=price]",
                "[data-testid*=price]"
        };

        for (String selector : selectors) {
            for (Element element : doc.select(selector)) {
                String candidate = firstNonBlank(element.attr("content"), element.attr("value"), element.text());
                Integer price = parsePrice(candidate);
                if (price != null) {
                    return price;
                }
            }
        }

        return parsePrice(pageText);
    }

    private Integer parsePrice(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String normalized = text.replace('\u00a0', ' ');
        Matcher matcher = PRICE_PATTERN.matcher(normalized);

        while (matcher.find()) {
            Integer value = parseInteger(matcher.group(1));
            if (value != null && value > 0 && value <= MAX_REASONABLE_PRICE) {
                return value;
            }
        }

        Integer value = parseInteger(normalized);
        if (value != null && value >= 1_000 && value <= MAX_REASONABLE_PRICE) {
            return value;
        }

        return null;
    }

    private String extractLocation(Document doc) {
        String[] selectors = {
                "[data-testid*=locality]",
                "[data-testid*=location]",
                "[class*=locality]",
                "[class*=location]",
                "[class*=address]"
        };

        for (String selector : selectors) {
            String value = textOf(doc, selector);
            if (value != null && !value.isBlank() && value.length() <= 80) {
                return value;
            }
        }

        return "-";
    }

    private String extractImageUrl(Document doc) {
        String ogImage = attrOf(doc, "meta[property=og:image]", "content");
        if (ogImage != null && !ogImage.isBlank()) {
            return ogImage;
        }

        Element img = doc.selectFirst("img[src]");
        if (img == null) {
            return null;
        }

        String src = img.absUrl("src");
        return src == null || src.isBlank() ? null : src;
    }

    private Integer extractYear(String searchable) {
        Matcher matcher = YEAR_PATTERN.matcher(searchable);

        List<Integer> validYears = new ArrayList<>();
        int currentYear = Year.now().getValue();

        while (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));

            // пропускаем модель Peugeot 2008
            if (year == 2008 && searchable.contains("peugeot 2008")) {
                continue;
            }

            if (year >= 1980
                    && year < currentYear
                    && (isExplicitVehicleYearContext(searchable, matcher.start(), matcher.end())
                    || !isBadYearContext(searchable, matcher.start(), matcher.end()))) {
                validYears.add(year);
            }
        }

        if (validYears.isEmpty()) {
            return null;
        }

        // берем наиболее реалистичный год машины
        return validYears.stream()
                .filter(y -> y < currentYear)
                .max(Integer::compareTo)
                .orElse(validYears.get(0));
    }

    private Integer resolveYear(String identityText, String scopedText) {
        Integer identityYear = extractYear(identityText);
        if (identityYear != null) {
            return identityYear;
        }

        return extractExplicitVehicleYear(scopedText);
    }

    private Integer extractExplicitVehicleYear(String searchable) {
        Matcher matcher = YEAR_PATTERN.matcher(searchable);
        List<Integer> validYears = new ArrayList<>();
        int currentYear = Year.now().getValue();

        while (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));
            if (year >= 1980
                    && year < currentYear
                    && isExplicitVehicleYearContext(searchable, matcher.start(), matcher.end())
                    && !isBadYearContext(searchable, matcher.start(), matcher.end())) {
                validYears.add(year);
            }
        }

        return validYears.stream()
                .max(Integer::compareTo)
                .orElse(null);
    }

    private Integer extractMileage(String searchable) {
        Integer mileage = extractMileageValue(searchable, MILEAGE_NAJETO_PATTERN.matcher(searchable), false);
        if (mileage != null) {
            return mileage;
        }

        mileage = extractMileageValue(searchable, MILEAGE_KM_PATTERN.matcher(searchable), false);
        if (mileage != null) {
            return mileage;
        }

        return extractMileageValue(searchable, MILEAGE_TIS_PATTERN.matcher(searchable), true);
    }

    private boolean isBadYearContext(String searchable, int start, int end) {
        String context = searchable.substring(Math.max(0, start - 80), Math.min(searchable.length(), end + 80));
        return containsAny(context,
                "zaruka", "zaruky", "garance", "stk", "emise",
                "servis do", "platne do", "platnost", "do roku",
                "vlozen", "vlozeno", "pridano", "aktualiz", "inzerat");
    }

    private boolean isExplicitVehicleYearContext(String searchable, int start, int end) {
        String context = searchable.substring(Math.max(0, start - 16), Math.min(searchable.length(), end + 16));
        return containsAny(context, "r.v.", "r v", "rok", "rv.", "rv ");
    }

    private Integer extractMileageValue(String searchable, Matcher matcher, boolean thousands) {
        while (matcher.find()) {
            Integer mileage = parseInteger(matcher.group(1));
            if (mileage == null) {
                continue;
            }

            if (thousands) {
                mileage *= 1_000;
            }

            if (!thousands && looksLikeYearMileageFalsePositive(searchable, matcher.start(1), matcher.end(1), mileage)) {
                continue;
            }

            if (mileage >= 1_000 && mileage <= 1_500_000) {
                return mileage;
            }
        }

        return null;
    }

    private boolean looksLikeYearMileageFalsePositive(String searchable, int start, int end, int mileage) {
        int currentYear = Year.now().getValue();
        if (mileage < 1980 || mileage > currentYear) {
            return false;
        }

        String context = searchable.substring(Math.max(0, start - 24), Math.min(searchable.length(), end + 24));
        return containsAny(context, "rok", "r.v.", "rv.", "rv ", "vyroby", "uvedeni");
    }

    private String detectBrand(String searchable) {
        if (containsWord(searchable, "seat")) {
            return "SEAT";
        }
        if (containsWord(searchable, "cupra")) {
            return "CUPRA";
        }
        if (containsWord(searchable, "wolkswagen")) {
            return "VOLKSWAGEN";
        }
        if (containsWord(searchable, "mokka")) {
            return "OPEL";
        }

        String[][] brands = {
                {"ALFA_ROMEO", "alfa romeo", "giulia", "giulietta", "stelvio"},
                {"LAND_ROVER", "land rover", "range rover", "discovery", "defender"},
                {"LEXUS", "lexus", "nx350h", "nx 350h"},
                {"BENTLEY", "bentley", "continental gt"},
                {"JAGUAR", "jaguar", "e-pace", "e pace", "xj"},
                {"JEEP", "jeep", "wrangler", "cherokee"},
                {"MERCEDES", "mercedes", "benz", "amg", "glc", "gle", "gls", "slk", "e270", "g320"},
                {"VOLKSWAGEN", "volkswagen", "wolkswagen", "vw", "passat", "golf", "tiguan", "touran", "touareg", "sharan", "california", "t-cross"},
                {"SKODA", "skoda", "fabia", "octavia", "superb", "kodiaq", "karoq"},
                {"CHEVROLET", "chevrolet", "corvette", "camaro", "captiva"},
                {"DODGE", "dodge", "challenger"},
                {"SAAB", "saab"},
                {"KTM", "ktm", "x-bow", "x bow"},
                {"CHRYSLER", "chrysler", "pacifica"},
                {"TESLA", "tesla", "model 3", "model y", "model s", "model x"},
                {"LAMBORGHINI", "lamborghini", "urus"},
                {"PORSCHE", "porsche", "carrera", "911", "macan", "cayenne"},
                {"ISUZU", "isuzu", "d-max", "d max"},
                {"CUPRA", "cupra", "formentor", "born"},
                {"INFINITI", "infiniti", "fx35", "fx-35"},
                {"OMODA", "omoda"},
                {"JAECOO", "jaecoo"},
                {"DONGFENG", "dongfeng", "u-tour", "u tour", "t5 evo"},
                {"MINI", "mini", "cooper"},
                {"MITSUBISHI", "mitsubishi", "outlander", "eclipse cross", "l200"},
                {"HYUNDAI", "hyundai", "i20", "i30", "ix20", "ix35", "ioniq", "tucson", "santa fe", "bayon", "inster"},
                {"SMART", "smart", "fortwo", "forfour", "roadster"},
                {"PEUGEOT", "peugeot", "rifter", "partner"},
                {"CITROEN", "citroen", "berlingo", "picasso"},
                {"RENAULT", "renault", "clio", "megane", "scenic"},
                {"TOYOTA", "toyota", "yaris", "corolla", "rav4"},
                {"NISSAN", "nissan", "qashqai", "x-trail", "micra"},
                {"SUZUKI", "suzuki", "vitara", "sx4", "ignis"},
                {"DACIA", "dacia", "duster", "logan", "dokker", "lodgy", "sandero"},
                {"VOLVO", "volvo", "xc40", "xc60", "xc90", "v50", "v60", "v90", "s40"},
                {"MAZDA", "mazda", "cx-3", "cx3", "cx-5", "cx5", "mazda 5"},
                {"HONDA", "honda", "civic", "accord", "cr-v", "hr-v"},
                {"FORD", "ford", "focus", "mondeo", "kuga", "s-max", "galaxy", "ranger"},
                {"SUBARU", "subaru", "legacy", "forester", "outback", "xv"},
                {"AUDI", "audi", "a3", "a4", "a5", "a6", "s5", "q3", "q5", "q7", "q8"},
                {"BMW", "bmw", "bmw x1", "bmw x 1", "x3", "x4", "x5", "x6", "i5", "e90", "325xi"},
                {"KIA", "kia", "ceed", "sportage", "sorento", "picanto"},
                {"SEAT", "seat", "ibiza", "leon", "altea"},
                {"OPEL", "opel", "astra", "corsa", "zafira", "mokka"},
                {"FIAT", "fiat", "punto", "stilo", "fiat 500", "500c"},
                {"SSANGYONG", "ssangyong", "ssang yong", "tivoli", "korando"},
                {"MG", "mg", "mgs5", "mg zs"}
        };

        for (String[] brand : brands) {
            for (int i = 1; i < brand.length; i++) {
                if (containsWord(searchable, brand[i])) {
                    return brand[0];
                }
            }
        }

        return "-";
    }

    private String detectFuelType(String searchable) {
        if (hasElectricSignal(searchable)) {
            return "ELECTRIC";
        }

        if (containsAny(searchable, "g-tec", "g tec", "gtec")
                || searchable.matches(".*\\b[0-9][.,][0-9]\\s*tgi\\b.*")) {
            return "CNG";
        }

        if (containsAny(searchable, "lpg")) {
            return "LPG";
        }

        if (containsAny(searchable, "cng")) {
            return "CNG";
        }

        if (containsAny(searchable,
                "plug-in", "plugin", "phev", " gte ",
                "tfsi e", " e-performance", " e performance",
                "superb iv", "kodiaq iv", "225xe", "iperformance",
                "300e", " 300 e ", "330e", " 330 e ", "530e", " 530 e ")) {
            return "PLUGIN_HYBRID";
        }

        if (containsAny(searchable, "volvo", "xc40", "xc60", "xc90", "v60", "v90", "s60", "s90")
                && containsAny(searchable, " b3 ", " b4 ", " b5 ", " b6 ", "2.0b3", "2.0b4", "2.0b5", "2.0b6")) {
            return "HYBRID";
        }

        if (containsAny(searchable, "superb", "octavia", "kodiaq")
                && containsAny(searchable, " iv ", " iv,", " iv.")
                && !containsAny(searchable, "tdi", "diesel", "nafta", "dci", "hdi", "cdti")) {
            return "PLUGIN_HYBRID";
        }

        if (containsAny(searchable,
                "plug-in", "plugin", "phev", "hybrid", " hev ", " mhev ", " etsi ", " gte ",
                "tfsi e", " shs ", "e-power", "epower", "225xe", "iperformance",
                " t8 ", "recharge",
                "330e", " 330 e ", "530e", " 530 e ", " b5 ")) {
            return "HYBRID";
        }

        if (containsAny(searchable, "superb iv", "kodiaq iv")
                && !containsAny(searchable, "tdi", "diesel", "nafta", "dci", "hdi", "cdti")) {
            return "HYBRID";
        }

        if (containsAny(searchable, "eco-g", "eco g")) {
            return "LPG";
        }

        if (containsAny(searchable,
                "diesel", "nafta", "tdi", "tdci", "cdi", "crdi", "hdi", "dci",
                "jtd", "jtdm", "multijet", "bluehdi", "cdti", "d4d", "d-4d", "tid",
                "did", "di-d", "di d", "td4", " td ", " td,", "ecoblue", "crd",
                " d3 ", " d4 ", " d5 ", "20d", "24d5", "25d", "40d", "30sd", "3 0sd", "xdrive25d", "xdrive40d",
                "skyactiv-d", "skyactiv d", "skyactive d",
                " 3,0 di ", " 3.0 di ")) {
            return "DIESEL";
        }

        if (containsAny(searchable, "jeep cherokee")
                && searchable.matches(".*\\b2[.,]2\\b.*")) {
            return "DIESEL";
        }

        if (searchable.matches(".*\\b[0-9][.,][0-9]\\s*td\\b.*")) {
            return "DIESEL";
        }

        if (containsAny(searchable, "zafira a") && searchable.matches(".*\\b2[.,]0\\b.*\\b74\\s*kw\\b.*")) {
            return "DIESEL";
        }

        if (containsAny(searchable, "mazda cx-5", "mazda cx5", "cx-5", "cx5")
                && containsAny(searchable, "skyactiv", "skyactive")
                && searchable.matches(".*\\b2[.,]2\\b.*")) {
            return "DIESEL";
        }

        if (searchable.matches(".*\\b[a-z]?[0-9]{2,3}d\\b.*")
                || searchable.matches(".*\\b[0-9][.,][0-9]\\s*d\\b.*")
                || searchable.matches(".*\\bd\\s*[0-9]\\b.*")
                || searchable.matches(".*\\bd\\s*4m.*")) {
            return "DIESEL";
        }

        if (containsAny(searchable,
                "benzin", "benzín", "petrol", "tsi", "tfsi", "tsfi", "fsi",
                "gdi", "tgdi", "dig-t", "tce", "ecoboost", "mivec", "vtec",
                "vti", "vvt", "vvt-i", "puretech", "pt", "mpi", "dpi", "jts", "16v", "18i", "20i", "2 0i",
                "30i", "40i", "50i", "850i", "14tsi", "20tsi", "turbo", "ti-vct", "pentastar", "hemi",
                "challenger", "pacifica", "carrera", "cooper s", "kompressor", "gti", "gr86", "gr 86",
                "x-bow", "x bow", "cayman",
                "t5", " t6 ", "s5", "amg gt", "v8", "v12", "camaro", "corvette", "macan",
                "cayenne", "750li", "325xi", "fx35", "fx-35", "fx37", "fx-37",
                "2.5t", "2,5t", "focus st", "fiesta st", "skyactiv-g", "skyactiv g", "sky-g", "sky g",
                "boosterjet", "u-tour 1,5", "u tour 1,5", "mage 1,5", "t5 evo 1,5",
                "busso", "gta", "matiz", "s-max 1.5", "s max 1.5", "s-max 1,5", "s max 1,5",
                "b-max 1.0", "b max 1.0", "b-max 1,0", "b max 1,0",
                "focus 1.0", "focus 1,0", "focus combi 1.6", "focus combi 1,6", "e-thp", "ethp", "htp", "sce",
                "escort 1.6", "escort 1,6", "c43", "c 43", "a45", "a 45", "sl320",
                "fiesta 1.4", "fiesta 1,4", "golf 1.6", "golf 1,6",
                "golf 1.4", "golf 1,4", "octavia 1.6", "octavia 1,6",
                "clio 1.2", "clio 1,2", "fiesta 1.25", "fiesta 1,25", "meriva 1.4", "meriva 1,4",
                "a6 c4 1.8", "a6 c4 1,8", "mazda 6 2.0", "mazda 6 2,0", "mazda cx-5 2.0", "mazda cx-5 2,0", "cx-5 2.0", "cx-5 2,0",
                "focus st 2.0", "focus st 2,0", "glc 43", "cla 45",
                "octavia 1.8 t", "octavia 1,8 t", "cruze 1.6", "cruze 1,6", "samurai 1.3", "samurai 1,3",
                "e240", "e 240", "c200 1.6", "c200 1,6", "c 200 1.6", "c 200 1,6")) {
            return "PETROL";
        }

        if (containsAny(searchable, "jeep compass")
                && searchable.matches(".*\\b1[.,]3\\b.*")) {
            return "PETROL";
        }

        if ((containsAny(searchable, "octavia")
                && searchable.matches(".*\\b1[.,]8\\s*t\\b.*"))
                || (containsAny(searchable, "peugeot 3008", "fiat 500x", "b-max", "b max")
                && searchable.matches(".*\\b1[.,][026]\\b.*"))
                || (containsAny(searchable, "peugeot 2008", "dacia duster", "grand vitara", "volkswagen golf", "vw golf")
                && searchable.matches(".*\\b(?:1[.,][246]|2[.,][04])\\b.*"))
                || (containsAny(searchable, "suzuki swift")
                && searchable.matches(".*\\b1[.,]2\\b.*"))
                || containsAny(searchable, "cl 500", "cl500", "xk8")) {
            return "PETROL";
        }

        if (searchable.matches(".*\\b[0-9][.,][0-9]\\s*i\\b.*")
                || searchable.matches(".*\\b[0-9]{2,3}i\\b.*")
                || searchable.matches(".*\\b[0-9]{2,3}\\s+i\\b.*")
                || searchable.matches(".*\\b(?:golf|octavia|fiesta)\\b[^a-z0-9]{0,8}\\b1[.,][46]\\b.*")
                || searchable.matches(".*\\b[0-9][.,][0-9]\\b.*\\b(?:jts|vti|tsi|tfsi|mpi)\\b.*")
                || searchable.matches(".*\\b(?:outback|forester)\\b.*\\b2[.,]5\\b.*")) {
            return "PETROL";
        }

        return "-";
    }

    private boolean hasElectricSignal(String searchable) {
        return containsAny(searchable,
                "tesla", "model 3", "model y", "model s", "model x",
                "bmw i3", " i3 ", "bmw i5", " inster ", " id 3 ", "id.3", " id3 ",
                " e-2008 ", " e 2008 ", "e-up", " e up ", " id 4 ", "id.4", " id4 ", " id 5 ", "id.5", " id5 ",
                "eq comfort", " smart eq", "b-class 250 edrive", "b 250 edrive", "ioniq 5", "elektro", "electric", "bev",
                "enyaq", "cupra born", "e-tron", "etron")
                || searchable.matches(".*\\b[0-9]{2,3}(?:[.,][0-9])?\\s*kwh\\b.*");
    }

    private String resolveFuelType(String identityText, String scopedText) {
        String identityFuel = detectFuelType(identityText);
        if (!"-".equals(identityFuel)) {
            return identityFuel;
        }

        String scopedFuel = detectFuelType(scopedText);
        if ("ELECTRIC".equals(scopedFuel) && !hasElectricSignal(identityText)) {
            return "-";
        }

        if (!"-".equals(scopedFuel) && !hasFuelSignal(identityText, scopedFuel)) {
            return "-";
        }

        return scopedFuel;
    }

    private boolean hasFuelSignal(String identityText, String fuelType) {
        return switch (fuelType) {
            case "PETROL" -> containsAny(identityText, "benzin", "benzĂ­n", "petrol", "tsi", "tfsi", "fsi", "gdi",
                    "tgdi", "dig-t", "tce", "ecoboost", "mivec", "vtec", "vti", "puretech", "mpi", "jts",
                    "16v", "18i", "20i", "30i", "40i", "50i", "850i", "v8", "v12", "gti")
                    || identityText.matches(".*\\b[0-9][.,][0-9]\\s*i\\b.*")
                    || identityText.matches(".*\\b[0-9]{2,3}i\\b.*");
            case "DIESEL" -> containsAny(identityText, "diesel", "nafta", "tdi", "tdci", "cdi", "crdi", "hdi",
                    "dci", "jtd", "jtdm", "multijet", "bluehdi", "cdti", "d4d", "d-4d", " d3 ", " d4 ", " d5 ",
                "20d", "25d", "40d", "30sd", "xdrive25d", "xdrive40d")
                    || identityText.matches(".*\\b[a-z]?[0-9]{2,3}d\\b.*")
                    || identityText.matches(".*\\b[0-9][.,][0-9]\\s*d\\b.*")
                    || identityText.matches(".*\\bd\\s*[0-9]\\b.*");
            case "HYBRID", "PLUGIN_HYBRID" -> containsAny(identityText, "plug-in", "plugin", "phev", "hybrid", " hev ", " mhev ",
                    " gte ", "tfsi e", " shs ", "e-power", "epower", "225xe", "iperformance",
                    " t8 ", "recharge", "superb iv", "kodiaq iv", " b5 ", "e-performance", "e performance",
                    "300e", " 300 e ", "330e", " 330 e ", "530e", " 530 e ");
            case "LPG" -> containsAny(identityText, "lpg");
            case "CNG" -> containsAny(identityText, "cng", "g-tec", "g tec", "gtec");
            default -> true;
        };
    }

    private String detectTransmission(String searchable) {
        if (containsAny(searchable,
                "automat", "automatic", "automatik", "aut.",
                " aut ",
                "dsg", "dct", "edc", "e-dcs",
                "tiptronic", "s-tronic", "stronic",
                "steptronic", "g-tronic",
                "cvt", "e-cvt", "ecvt",
                "powershift",
                " at ", " at/", " at,", " at.", " a/t ", " a/t",
                "at6", "6at", "at7", "7at", "at8", "8at", "at9", "9at",
                "10at", "10st", " 10a")) {
            return "AUTOMATIC";
        }

        if (containsAny(searchable,
                "manual", "manuál", "manualni", "manuální",
                " kvalt ", " kvalt,",
                " man ", " man,", " man.",
                " mt ", " mt,", " mt.",
                "mt5", "5mt", "mt6", "6mt",
                "5man", "6man",
                "5rychl", "6rychl",
                "m5", "5m", "m6", "6m",
                "6mp", "6°mp")) {
            return "MANUAL";
        }

        if ("ELECTRIC".equals(detectFuelType(searchable))) {
            return "AUTOMATIC";
        }

        return "-";
    }

    private String resolveTransmission(String identityText, String scopedText, String fuelType) {
        String identityTransmission = detectTransmission(identityText);
        if (!"-".equals(identityTransmission)) {
            return identityTransmission;
        }

        String scopedTransmission = detectTransmission(scopedText);
        if ("AUTOMATIC".equals(scopedTransmission)
                && !"ELECTRIC".equals(fuelType)
                && !containsAny(identityText, "automat", "automatic", "automatik", "aut.", "dsg", "dct", "edc", "tiptronic", "s-tronic", "stronic", "cvt", " at ")) {
            return "-";
        }

        if (("HYBRID".equals(fuelType) || "PLUGIN_HYBRID".equals(fuelType))
                && containsAny(identityText, "honda crv", "honda cr-v", "honda hr-v", "honda hrv", "e:hev", "ehev", "full hybrid")
                && !containsAny(identityText, "manual", "manualni", "man.", " mt ", "6mt", "5mt")) {
            return "AUTOMATIC";
        }

        if (("HYBRID".equals(fuelType) || "PLUGIN_HYBRID".equals(fuelType))
                && containsAny(identityText, "e-hybrid", "e hybrid", "plug-in", "plugin", "phev", " gte ", "tfsi e", "tsi e", " e-performance", " e performance")
                && !containsAny(identityText, "manual", "manualni", "man.", " mt ", "6mt", "5mt", "6m/t", "5m/t")) {
            return "AUTOMATIC";
        }

        if (("HYBRID".equals(fuelType) || "PLUGIN_HYBRID".equals(fuelType))
                && containsAny(identityText, "camry", "corolla", "rav4", "yaris", "300e", " 300 e ")
                && !containsAny(identityText, "manual", "manualni", "man.", " mt ", "6mt", "5mt")) {
            return "AUTOMATIC";
        }

        return scopedTransmission;
    }

    private String detectCarType(String searchable) {
        if (containsAny(searchable, "pickup", "pick-up", "ranger", "hilux", "navara", "l200", "amarok", "dodge ram", " ram ", "hcpu")) {
            return "PICKUP";
        }
        if (containsAny(searchable, "volkswagen cc", "vw cc", "passat cc")) {
            return "SEDAN";
        }
        if (containsAny(searchable, "mazda 6", "mazda6")) {
            return "SEDAN";
        }
        if (containsAny(searchable, "citroen c5", "citroen c 5", " c5 ")
                && !containsAny(searchable, "aircross", "tourer", "kombi", "combi")) {
            return "SEDAN";
        }
        if (containsAny(searchable, "peugeot 301", " 301 ")) {
            return "SEDAN";
        }
        if (containsAny(searchable, "peugeot 508", " 508 ")
                && !containsAny(searchable, " sw ", "kombi", "combi", "wagon")) {
            return "SEDAN";
        }
        if (containsAny(searchable, "mercedes-benz cle", "mercedes benz cle", " mercedes cle ", " cle ")
                && !containsAny(searchable, "cabrio", "kabrio", "convertible")) {
            return "COUPE";
        }
        if (containsAny(searchable, "magentis")) {
            return "SEDAN";
        }
        if (containsAny(searchable, "peugeot 308", " 308 ")
                && containsAny(searchable, " sw ", "combi", "kombi", "wagon")) {
            return "WAGON";
        }
        if (containsAny(searchable, "peugeot 308", " 308 ")) {
            return "HATCHBACK";
        }
        if (containsAny(searchable, "insignia")) {
            return "SEDAN";
        }
        if (containsAny(searchable, "touran", "sharan", "alhambra", "altea", "s-max", "c-max", "b-max", "galaxy", "zafira", "scenic", "modus", "picasso", "roomster", "berlingo", "rifter", "caddy", "citan", "vito", "viano", "mercedes v ", "v 250", "v250", "w447", "tridy v", "tridy r", "proace verso", "proace city", "proace city verso", "multivan", "california", "volkswagen t5", "volkswagen t6", "vw t5", "vw t6", "jumpy", "trafic", "traffic", "sportsvan", "golf plus", "ford fusion", "tourneo custom", "tourneo connect", "tourneo courier", "u-tour", "u tour", "225xe", "active tourer", " f45 ", "kangoo", "dokker", "lodgy", "jogger", "b180", "b 180", "b200", "b 200", "peugeot 807", " 807 ", "mazda 5", "grandis", "voyager", "town country", "town & country", "pacifica", "grand caravan", "sienna", "corolla verso", "ix20", "meriva", "partner tepee", "peugeot partner", "fiat ulysse", "ulysse", "fiat doblo", "doblo")) {
            return "MINIVAN";
        }
        if (containsAny(searchable, "model 3", "model s", "toyota camry", "camry", "bmw i5", "eqe", "audi a7", " a7 ", "jetta", "w220", "w 220", "w211", "w 211", "fluence", "audi s8", " s8 ", "jaguar xf", " xf ", "jaguar xe", "mercedes 211")) {
            return "SEDAN";
        }
        if (searchable.matches(".*\\bforman\\b.*")) {
            return "WAGON";
        }
        if (containsAny(searchable, "leon sportstourer", "leon sport tourer", "leon st ", "leon, st", "leon st,", "leon combi", "leon kombi", "leon wagon")) {
            return "WAGON";
        }
        if (containsAny(searchable, "seat leon", " leon ")) {
            return "HATCHBACK";
        }
        if (containsAny(searchable, "mokka", "koleos", "freelander", "bigster", "fiat 500x", "500x", "outlender", "sedici", "samurai")) {
            return "SUV";
        }
        if (containsAny(searchable, "dodge caliber", "caliber")) {
            return "HATCHBACK";
        }
        if (containsAny(searchable, "xk8", "cl 500", "cl500")) {
            return "COUPE";
        }
        if (containsAny(searchable, "gran kupe", "gran coupe", "gran kup")) {
            return "SEDAN";
        }
        if (containsAny(searchable, "cabrio", "kabriolet", "cabriolet", "convertible", "eos", "roadster", "slk", " sl ", "207 cc", "peugeot 207 cc", "308 cc", "peugeot 308 cc", "500c", "cascada")) {
            return "CABRIO";
        }
        if (containsAny(searchable, "octavia scout", "i30 cw", "logan mcv", "avensis com", "avensis combi", "avensis kombi", "avensis wagon", "proceed", "pro ceed", "combi", "kombi", "variant", "turnier", "shooting brake", "touring", " avant ", "allroad", " sw ", "wagon", "estate", "outback", "v50", "v60", "v70", "v90")
                || searchable.matches(".*\\ba[46]\\b.*\\bavant\\b.*")) {
            return "WAGON";
        }
        if (containsAny(searchable, "audi s6", " s6 ", "audi s7", " s7 ")
                && !containsAny(searchable, "avant", "combi", "kombi", "variant", "wagon", "shooting brake")) {
            return "SEDAN";
        }
        if (containsAny(searchable, "agila", "citigo", "rapid", "favorit", "sandero", " rio ", "swift", "starlet", "auris", "mazda 3", "v40", "tridy a", "a 160", "a160", "a 45", "a45", "c3", "citroen c4", "citroen c 4", "fiesta", "i10", "splash", "prius", "insight", "a1", "a3", "panda", "bravo", "matiz", "fortwo", "fourtwo", "forfour", "cupra born", "jazz", "kia k4", " k4 ", " hb ",
                "id.3", " id 3 ", " id3 ",
                "peugeot 206", " 206 ", "fabia", "fabie", "bmw 116i", " 116i ", "bmw f20", " f20 ", "escort", "colt")) {
            return "HATCHBACK";
        }
        if (containsAny(searchable, "cruze", "superb", "s40", "s90", "mazda 6", "logan", "toledo")) {
            return "SEDAN";
        }
        if (containsAny(searchable, "suv", "kodiaq", "karoq", "kamiq", "yeti", "enyaq", "tiguan", "touareg", "taigo", "t-roc", "t roc", "troc", "t-cross", "t cross", "kadjar", "captur", "qashqai", "x-trail", "terrano", "pathfinder", "padfinder", "patrol", "pajero", "pinin", "land cruiser", "defender", "bmw x1", "bmw x 1", "bmw x2", "bmw x 2", "bmw x3", "bmw x4", "bmw x5", "bmw x6", "bmw x7", "fx35", "fx-35", "fx37", "fx-37", "q3", "q5", "q7", "q8", "wrangler", "compass", "cherokee", "discovery", "sportage", "sorento", "stonic", "xceed", "xcee", "ev6", "tucson", "santa fe", "kona", "bayon", "inster", "ix35", "rav4", "c-hr", " chr ", "urban cruiser", "cr-v", "hr-v", "cx-3", "cx3", "cx-5", "cx-7", "cx7", "c-crosser", "ioniq 5", "outlander", "eclipse cross", "formentor", "edge", "kuga", "puma", "crossland", "grandland", "glk", "glb", "gla", "gl 420", "gl420", "gl 450", "ml 320", "ml320", "tridy m", "glc", "gle", "gls", "tridy g", "g320", "c5 aircross", "evoque", "range rover", "tarraco", "arona", "stelvio", "xc40", "xc60", "xc90", "xc 70", "xc70", "duster", "tivoli", "korando", "rexton", "ateca", "sx4", "s-cross", "s cross", "sedici", "samurai", "jimny", "dongfeng mage", "t5 evo", "dongfeng t5", "omoda 5", "omoda 9", "jaecoo", "mg zs", "mgs5", "peugeot 2008", "peugeot 3008", "3008", "peugeot 5008", "5008", "ignis", "vitara", "macan", "cayenne", "urus", "lexus nx", "lexus gx", "nx350h", "gx 460", "forester", "subaru xv", " xv ", "asx", "austral", "ix55", "id.4", " id 4 ", " id4 ", "id.5", " id 5 ", " id5 ")) {
            return "SUV";
        }
        if (containsAny(searchable, "coupe", "mustang", " tt ", "370z", "350z", "brz", "challenger", "carrera", "911", "cayman", "continental gt", "s5", "amg gt", "mercedes-benz cl,", " tridy cl", "clk", "camaro", "f-type", "f type", "x-bow", "x bow", "bmw m2", " m2 ", "rada 4", "bmw 4", "m440i", "440i")) {
            return "COUPE";
        }
        if (containsAny(searchable, "sedan", "limuz", "passat", "octavia", "superb", "arteon", "corolla", "saab 9-3", "saab 9 3", "saab 9-5", "saab 9 5", "a4", "a6", "a8", "bmw 3", "rada 3", "bmw 5", "rada 5", "bmw 7", "rada 7", "750d", "750li", "e90", "325xi", "mondeo", "cla", "cls", "c250", "c 250", "e220", "e 220", "220cdi", "e270", "e350", "e 350", "c220", "c43", "c 43", "tridy c", "tridy e", "s 320", "s 350", "tridy s")) {
            return "SEDAN";
        }
        if (containsAny(searchable, "hatchback", "mini", "cooper", "208", "peugeot 308", "punto", "fiat 500", "citigo", "fabia", "scala", "clio", "megane", "golf", "focus", "a 180", "i20", "i30", "ceed", "leon", "civic", "astra", "corsa", "polo", "yaris", "micra", "picanto")) {
            return "HATCHBACK";
        }

        return "-";
    }

    private String resolveCarType(String identityText, String scopedText) {
        String identityType = detectCarType(identityText);
        if (!"-".equals(identityType)) {
            return identityType;
        }

        String scopedType = detectCarType(scopedText);
        if ("COUPE".equals(scopedType)
                && !containsAny(identityText, "coupe", "coup", "mustang", " tt ", "370z", "350z", "brz", "challenger", "carrera", "911", "camaro")) {
            return "-";
        }

        if ("CABRIO".equals(scopedType)
                && !containsAny(identityText, "cabrio", "kabrio", "cabriolet", "convertible", "roadster", "spider", "spyder", " cc ", "207 cc", "308 cc", "500c")) {
            return "-";
        }

        if ("SUV".equals(scopedType)
                && !containsAny(identityText, "suv", "4x4", "kodiaq", "karoq", "yeti", "tiguan", "touareg", "qashqai", "x-trail", "bmw x", "q3", "q5", "q7", "q8", "kuga", "duster", "korando", "xv", "kona", "bayon", "inster", "omoda", "jaecoo", "3008", "5008", "vitara", "lexus gx", "gx 460", "crossover")) {
            return "-";
        }

        if ("WAGON".equals(scopedType)
                && !containsAny(identityText, "combi", "kombi", "variant", "shooting brake", "touring", " avant ", "allroad", " sw ", "cw", "wagon", "estate", "mcv", "proceed", "pro ceed")) {
            return "-";
        }

        if ("PICKUP".equals(scopedType)
                && !containsAny(identityText, "pickup", "pick-up", "ranger", "hilux", "navara", "l200", "amarok", " ram ", "hcpu")) {
            return "-";
        }

        return scopedType;
    }

    private boolean looksDemandListing(String searchable) {
        return containsAny(searchable, "koupim", "hledam", "shanim", "vymenim za", "popoptavam");
    }

    private boolean looksNonCarListing(String searchable) {
        if ("osobni vuz".equals(searchable)
                || "osobni auto".equals(searchable)
                || "auto".equals(searchable)
                || ((searchable.startsWith("osobni vuz ") || searchable.startsWith("osobni auto ") || searchable.startsWith("auto "))
                && "-".equals(detectBrand(searchable))
                && "-".equals(detectFuelType(searchable))
                && "-".equals(detectCarType(searchable)))) {
            return true;
        }

        return containsAny(searchable,
                "nahradni dily", "nahradni dil", "dily na", "rozprodavam", "bouracka na dily",
                "nabourany", "nabourane", "havarovany", "havarovane", "palubni deska", "airbag",
                "interierove plasty", "plasty smart", "chladic klimatizace", "prerusovac", "smerovych svetel",
                "pneu", "pneumatik", "elektrony", "alu kola", "sada kol", "disky", "naraznik",
                "motor na", "prevodovka", "svetlo", "svetel", "sedacky", "volant", "katalyzator", "servisni knizka",
                "autoradio", "auto radio", "navigace tomtom", "stresni nosic", "stresni nosnik", "zamky centralni",
                "centralni zamky", "zamykani zpatecky", "sterac", "sterace", "pc pocitac", "pocitac",
                "parkovaci senzor", "sklo zrcatka", "zadni sklo",
                "posilovac krouticiho momentu",
                " padlo ", "rucni pumpicka", "pumpicka", "cmx", "rebel", "triumph america");
    }

    private boolean looksCommercialVehicle(String searchable) {
        return containsAny(searchable,
                "transit", "vivaro", "jumper", "boxer", "ducato", "sprinter", "crafter", "valnik", "sklapec",
                "dodavka", "nakladni", "furgon", "l2h2", "l3h2", "dvojmontaz", "celni mech", "fuso", "transporter", "master", "sanitni vuz");
    }

    private boolean containsAny(String text, String... needles) {
        if (text == null) {
            return false;
        }

        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsWord(String text, String word) {
        if (text == null || word == null || word.isBlank()) {
            return false;
        }

        String normalizedWord = asciiSearchText(word);
        return Pattern.compile("(^|[^a-z0-9])" + Pattern.quote(normalizedWord) + "([^a-z0-9]|$)")
                .matcher(text)
                .find();
    }

    private String asciiSearchText(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        return normalized.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    private String textOf(Document doc, String selector) {
        Element element = doc.selectFirst(selector);
        return element == null ? null : normalizeText(element.text());
    }

    private String attrOf(Document doc, String selector, String attr) {
        Element element = doc.selectFirst(selector);
        if (element == null) {
            return null;
        }

        String value = element.attr(attr);
        return value == null || value.isBlank() ? null : normalizeText(value);
    }

    private String cleanTitle(String title) {
        if (title == null) {
            return null;
        }

        return normalizeText(title)
                .replace(" - Sbazar.cz", "")
                .replace(" | Sbazar.cz", "");
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }

        return text.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
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
                    || codePoint == '\uFFFD') {
                score++;
            }
            offset += Character.charCount(codePoint);
        }
        return score;
    }

    private Integer parseInteger(String raw) {
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return normalizeText(value);
            }
        }

        return null;
    }

    private String firstDetected(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"-".equals(value)) {
                return value;
            }
        }

        return "-";
    }

    private Integer firstInteger(Integer... values) {
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private String formatPrice(Integer priceValue) {
        if (priceValue == null) {
            return null;
        }

        return priceValue + " CZK";
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

    private String safeLog(String value) {
        return value == null || value.isBlank() ? "-" : value.replace("'", "\\'");
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record ParseResult(CarDto car, String reason, String title) {
        private static ParseResult car(CarDto car, String title) {
            return new ParseResult(car, null, title);
        }

        private static ParseResult skip(String reason, String title) {
            return new ParseResult(null, reason, title);
        }
    }

    private static final class Stats {
        private int parsed;
        private int listFetchFailed;
        private int brokenListing;
        private int nonCarListing;
        private int demandListing;
        private int commercialVehicle;
        private int cheapLowQualityListing;
        private int missingPrice;
        private int invalidPrice;
        private int parseException;

        private void countSkip(String reason) {
            switch (reason) {
                case "broken_listing" -> brokenListing++;
                case "non_car_listing" -> nonCarListing++;
                case "demand_listing" -> demandListing++;
                case "commercial_vehicle" -> commercialVehicle++;
                case "cheap_low_quality_listing" -> cheapLowQualityListing++;
                case "missing_price" -> missingPrice++;
                case "invalid_price" -> invalidPrice++;
                default -> parseException++;
            }
        }
    }
}

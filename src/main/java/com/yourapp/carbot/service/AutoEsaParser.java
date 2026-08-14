package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
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
public class AutoEsaParser extends AbstractJsoupParser implements CarSourceParser {

    private static final Logger log = LoggerFactory.getLogger(AutoEsaParser.class);

    private static final String BASE_URL = "https://www.autoesa.cz";
    private static final String BASE_LIST_URL = BASE_URL + "/vsechna-auta";
    private static final int MAX_LIST_PAGES = 10;
    private static final int CURRENT_YEAR = Year.now().getValue();
    private static final int MIN_VALID_PRICE = 30_000;
    private static final int MAX_REASONABLE_PRICE = 10_000_000;

    @Override
    public String getSourceName() {
        return "AUTOESA";
    }

    @Override
    public List<CarDto> fetchCars() {
        List<CarDto> cars = new ArrayList<>();
        Set<String> seenUrls = new LinkedHashSet<>();

        int missingPriceCount = 0;
        int invalidPriceCount = 0;
        int commercialVehicleCount = 0;
        int parseExceptionCount = 0;

        for (int page = 1; page <= MAX_LIST_PAGES; page++) {
            String pageUrl = buildPageUrl(page);

            try {
                Document doc = loadDocument(pageUrl);
                int pageCards = 0;
                int pageNewCars = 0;

                for (Element card : doc.select("a.car_item[href]")) {
                    pageCards++;
                    String url = cleanAutoEsaUrl(card.absUrl("href"));
                    if (url == null || !seenUrls.add(url)) {
                        continue;
                    }

                    try {
                        CarDto car = parseCard(card, url);
                        if (car == null) {
                            parseExceptionCount++;
                            continue;
                        }

                        if (car.getPriceValue() == null) {
                            missingPriceCount++;
                            log.warn("AUTOESA SKIP url={} reason=missing_price title={}", safe(url), safe(car.getTitle()));
                            continue;
                        }

                        if (car.getPriceValue() < MIN_VALID_PRICE || car.getPriceValue() > MAX_REASONABLE_PRICE) {
                            invalidPriceCount++;
                            log.warn("AUTOESA SKIP url={} reason=invalid_price title={} price={}",
                                    safe(url), safe(car.getTitle()), car.getPriceValue());
                            continue;
                        }

                        if ("VAN".equals(car.getCarType())) {
                            commercialVehicleCount++;
                            log.info("AUTOESA SKIP url={} reason=commercial_vehicle title={}", safe(url), safe(car.getTitle()));
                            continue;
                        }

                        enrichFromDetailQuietly(car);
                        normalizeParsedText(car);

                        log.info("AUTOESA CAR title='{}' price={} location={} year={} mileage={} fuelType={} transmission={} carType={} brand={} url={}",
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

                        cars.add(car);
                        pageNewCars++;
                    } catch (Exception e) {
                        parseExceptionCount++;
                        log.warn("AUTOESA SKIP url={} reason=parse_exception error={}", safe(url), safe(e.getMessage()));
                    }
                }

                log.info("AUTOESA list page={} url={} cards={} new_cars={} total_unique={}",
                        page, pageUrl, pageCards, pageNewCars, seenUrls.size());

                if (page > 1 && pageNewCars == 0) {
                    log.info("AUTOESA pagination stopped page={} reason=no_new_cars", page);
                    break;
                }

                sleepQuietly(300);
            } catch (Exception e) {
                parseExceptionCount++;
                log.warn("AUTOESA list page failed page={} url={} error={}", page, pageUrl, safe(e.getMessage()));
            }
        }

        log.info("AUTOESA parsed {} cars", cars.size());
        log.info("AUTOESA SUMMARY parsed={} missing_price={} invalid_price={} commercial_vehicle={} parse_exception={}",
                cars.size(), missingPriceCount, invalidPriceCount, commercialVehicleCount, parseExceptionCount);

        return cars;
    }

    private void normalizeParsedText(CarDto car) {
        if (car == null) {
            return;
        }

        car.setTitle(repairMojibake(car.getTitle()));
        car.setLocation(repairMojibake(car.getLocation()));
    }

    private String buildPageUrl(int page) {
        if (page <= 1) {
            return BASE_LIST_URL;
        }
        return BASE_LIST_URL + "?stranka=" + page;
    }

    private CarDto parseCard(Element card, String url) {
        String titleBase = extractTitleBase(card);
        Integer year = extractYear(card);
        String engine = text(card, ".car_item__icon.icon_year");
        String drivetrain = text(card, ".car_item__icon.icon_4x4");
        String title = joinTitle(titleBase, engine, drivetrain);

        Integer priceValue = extractSalePrice(card);
        Integer mileage = parseIntSafe(text(card, ".car_item__icon.icon_range"));
        String fuelType = mapFuel(text(card, ".car_item__icon.icon_fuel"));
        String carType = mapCarType(extractPathPart(url, 2), title);
        String brand = normalizeBrand(extractPathPart(url, 0), title);
        String imageUrl = cleanImageUrl(firstNonBlank(
                card.selectFirst(".car_item__image img") != null ? card.selectFirst(".car_item__image img").absUrl("src") : null,
                extractStyleImageUrl(card)
        ));

        CarDto car = new CarDto();
        car.setSource(getSourceName());
        car.setTitle(title);
        car.setPrice(formatPrice(priceValue));
        car.setPriceValue(priceValue);
        car.setLocation(null);
        car.setUrl(url);
        car.setImageUrl(imageUrl);
        car.setBrand(brand);
        car.setYear(validYear(year));
        car.setMileage(mileage);
        car.setFuelType(fuelType);
        car.setTransmission(null);
        car.setCarType(carType);

        return car;
    }

    private void enrichFromDetailQuietly(CarDto car) {
        if (car == null || car.getUrl() == null || car.getUrl().isBlank()) {
            return;
        }

        try {
            Document doc = loadDocument(car.getUrl());

            String detailTitle = firstNonBlank(text(doc, ".detail_fixed_content__head__title"), text(doc, "h1"));
            if (detailTitle != null && detailTitle.length() > car.getTitle().length()) {
                car.setTitle(repairMojibake(detailTitle));
            }

            car.setTransmission(firstNonBlank(car.getTransmission(), mapTransmission(extractDetailValue(doc, "Prevodovka"))));
            car.setCarType(firstNonBlank(car.getCarType(), mapCarType(extractDetailValue(doc, "Karoserie"), car.getTitle())));
            car.setFuelType(firstNonBlank(car.getFuelType(), mapFuel(extractDetailValue(doc, "Palivo"))));
            car.setMileage(firstNonNull(car.getMileage(), parseIntSafe(extractDetailValue(doc, "Stav tachometru"))));
            car.setYear(firstNonNull(car.getYear(), validYear(parseIntSafe(extractDetailValue(doc, "Rok")))));
            car.setImageUrl(firstNonBlank(extractMetaContent(doc, "meta[property=og:image]"), car.getImageUrl()));

            sleepQuietly(150);
        } catch (Exception e) {
            log.debug("AUTOESA detail enrichment skipped url={} error={}", safe(car.getUrl()), safe(e.getMessage()));
        }
    }

    private String extractTitleBase(Element card) {
        Element title = card.selectFirst(".car_item__title");
        if (title == null) {
            return null;
        }

        Element clone = title.clone();
        clone.select("span").remove();
        return repairMojibake(normalizeText(clone.text()));
    }

    private Integer extractYear(Element card) {
        return validYear(parseIntSafe(text(card, ".car_item__title span")));
    }

    private Integer extractSalePrice(Element card) {
        for (Element block : card.select(".car_item__price_block")) {
            String label = normalizeAscii(block.select(".text").text()).toLowerCase(Locale.ROOT);
            if (label.contains("akcni cena") || label.contains("cena v hotovosti")) {
                Integer price = parseIntSafe(block.select(".price").text());
                if (price != null) {
                    return price;
                }
            }
        }

        return null;
    }

    private String extractDetailValue(Document doc, String label) {
        if (doc == null || label == null || label.isBlank()) {
            return null;
        }

        String normalizedLabel = normalizeAscii(label).toLowerCase(Locale.ROOT);
        for (Element item : doc.select(".detail_attr_inner li")) {
            String itemLabel = normalizeAscii(item.selectFirst("strong") != null ? item.selectFirst("strong").text() : "")
                    .toLowerCase(Locale.ROOT);
            if (!itemLabel.equals(normalizedLabel)) {
                continue;
            }

            Element value = item.selectFirst("span");
            String text = normalizeText(value != null ? value.text() : "");
            return text.isBlank() ? null : text;
        }

        return null;
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

    private String extractStyleImageUrl(Element card) {
        Element image = card.selectFirst(".car_item__image[style]");
        if (image == null) {
            return null;
        }

        Matcher matcher = Pattern.compile("url\\(([^)]+)\\)").matcher(image.attr("style"));
        if (!matcher.find()) {
            return null;
        }

        String path = matcher.group(1).replace("\"", "").replace("'", "").trim();
        if (path.isBlank()) {
            return null;
        }

        if (path.startsWith("http")) {
            return path;
        }

        return BASE_URL + (path.startsWith("/") ? path : "/" + path);
    }

    private String extractPathPart(String url, int index) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String path = url.replaceFirst("^https?://[^/]+/?", "");
        String[] parts = path.split("/");
        if (index < 0 || index >= parts.length) {
            return null;
        }

        return parts[index];
    }

    private String cleanAutoEsaUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String clean = url.replaceAll("[?#].*$", "");
        if (clean.startsWith("/")) {
            clean = BASE_URL + clean;
        }

        return clean.startsWith(BASE_URL + "/") && clean.matches("https://www\\.autoesa\\.cz/.+/\\d+$")
                ? clean
                : null;
    }

    private String cleanImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String clean = url.replaceAll("[?#].*$", "");
        if (clean.startsWith("/")) {
            clean = BASE_URL + clean;
        }
        return clean;
    }

    private String joinTitle(String titleBase, String engine, String drivetrain) {
        String title = normalizeText(String.join(" ",
                safeBlank(titleBase),
                safeBlank(engine),
                safeBlank(drivetrain)));
        return title.isBlank() ? null : repairMojibake(title);
    }

    private String mapFuel(String value) {
        String source = " " + normalizeAscii(value).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(source, " lpg ")) return "LPG";
        if (containsAny(source, " cng ")) return "CNG";
        if (containsAny(source, " elektro ", " electric ", " kwh ")) return "ELECTRIC";
        if (containsAny(source, " plug-in ", " plugin ", " phev ")) return "PLUGIN_HYBRID";
        if (containsAny(source, " hybrid ", " hev ")) return "HYBRID";
        if (containsAny(source, " nafta ", " diesel ", " tdi ", " dci ", " hdi ", " cdti ")) return "DIESEL";
        if (containsAny(source, " benzin ", " petrol ", " tsi ", " tfsi ", " tce ", " mpi ")) return "PETROL";

        return null;
    }

    private String mapTransmission(String value) {
        String source = " " + normalizeAscii(value).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(source, " automat ", " automatic ", " dsg ", " dct ", " e-cvt ", " ecvt ")) return "AUTOMATIC";
        if (containsAny(source, " manual ", " manual/")) return "MANUAL";

        return null;
    }

    private String mapCarType(String value, String title) {
        String source = " " + normalizeAscii(safeBlank(value) + " " + safeBlank(title)).toLowerCase(Locale.ROOT) + " ";

        if (containsAny(source, " suv ", " crossover ", " duster ", " kuga ", " tiguan ", " kodiaq ", " karoq ", " sportage ")) return "SUV";
        if (containsAny(source, " mpv ", " minivan ", " galaxy ", " s-max ", " b-max ", " c-max ", " touran ", " sharan ")) return "MINIVAN";
        if (containsAny(source, " kombi ", " combi ", " wagon ", " variant ", " sw ")) return "WAGON";
        if (containsAny(source, " liftback ", " sedan ", " limousine ")) return "SEDAN";
        if (containsAny(source, " hatchback ", " hb ")) return "HATCHBACK";
        if (containsAny(source, " coupe ", " kupe ")) return "COUPE";
        if (containsAny(source, " cabrio ", " kabrio ", " convertible ")) return "CABRIO";
        if (containsAny(source, " pickup ", " pick-up ")) return "PICKUP";
        if (containsAny(source, " van ", " dodavka ", " uzitkove ")) return "VAN";

        return null;
    }

    private String normalizeBrand(String raw, String title) {
        String source = " " + normalizeAscii(firstNonBlank(raw, title)).toLowerCase(Locale.ROOT).replace('-', ' ') + " ";

        if (containsAny(source, " alfa romeo ")) return "ALFA_ROMEO";
        if (containsAny(source, " land rover ")) return "LAND_ROVER";
        if (containsAny(source, " mercedes benz ", " mercedes ")) return "MERCEDES";
        if (containsAny(source, " volkswagen ", " vw ")) return "VOLKSWAGEN";
        if (containsAny(source, " skoda ")) return "SKODA";
        if (containsAny(source, " citroen ")) return "CITROEN";
        if (containsAny(source, " peugeot ")) return "PEUGEOT";
        if (containsAny(source, " toyota ")) return "TOYOTA";
        if (containsAny(source, " renault ")) return "RENAULT";
        if (containsAny(source, " dacia ")) return "DACIA";
        if (containsAny(source, " ford ")) return "FORD";
        if (containsAny(source, " audi ")) return "AUDI";
        if (containsAny(source, " bmw ")) return "BMW";
        if (containsAny(source, " hyundai ")) return "HYUNDAI";
        if (containsAny(source, " kia ")) return "KIA";
        if (containsAny(source, " opel ")) return "OPEL";
        if (containsAny(source, " mazda ")) return "MAZDA";
        if (containsAny(source, " honda ")) return "HONDA";
        if (containsAny(source, " volvo ")) return "VOLVO";
        if (containsAny(source, " seat ")) return "SEAT";
        if (containsAny(source, " fiat ")) return "FIAT";
        if (containsAny(source, " tesla ")) return "TESLA";
        if (containsAny(source, " jeep ")) return "JEEP";
        if (containsAny(source, " nissan ")) return "NISSAN";
        if (containsAny(source, " suzuki ")) return "SUZUKI";
        if (containsAny(source, " mitsubishi ")) return "MITSUBISHI";
        if (containsAny(source, " porsche ")) return "PORSCHE";
        if (containsAny(source, " mini ")) return "MINI";
        if (containsAny(source, " ds ")) return "DS";
        if (containsAny(source, " maserati ")) return "MASERATI";
        if (containsAny(source, " jaguar ")) return "JAGUAR";

        return null;
    }

    private Integer parseIntSafe(String raw) {
        if (raw == null || raw.isBlank()) {
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
        return String.format(Locale.US, "%,d Kc", priceValue).replace(",", " ");
    }

    private String text(Element root, String selector) {
        if (root == null || selector == null || selector.isBlank()) {
            return null;
        }

        Element element = root.selectFirst(selector);
        String text = normalizeText(element != null ? element.text() : "");
        return text.isBlank() ? null : text;
    }

    private boolean containsAny(String source, String... values) {
        if (source == null || source.isBlank()) {
            return false;
        }

        for (String value : values) {
            if (value != null && !value.isBlank() && source.contains(value.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    private String normalizeAscii(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(normalizeText(value), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }

    private String repairMojibake(String value) {
        if (value == null || value.isBlank() || (!looksLikeMojibake(value) && mojibakeScore(value) == 0)) {
            return value;
        }

        String current = repairCommonMojibake(value);
        try {
            for (int attempt = 0; attempt < 5; attempt++) {
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

                String normalizedRepaired = normalizeText(repaired);
                int currentScore = mojibakeScore(current);
                int repairedScore = mojibakeScore(normalizedRepaired);

                if (repairedScore < currentScore
                        || (repairedScore <= currentScore && !normalizedRepaired.equals(current))
                        || (looksLikeMojibake(current) && !looksLikeMojibake(normalizedRepaired))) {
                    current = normalizedRepaired;
                } else {
                    break;
                }
            }
            return repairFinalMojibake(current);
        } catch (Exception e) {
            return repairFinalMojibake(current);
        }
    }

    private String repairCommonMojibake(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return value
                .replace("\u0139\u00A0", "Š")
                .replace("\u0139 ", "Š")
                .replace("\u0139\u0098", "Ř")
                .replace("\u0139\u2122", "ř")
                .replace("\u0102\u00AB", "ë")
                .replace("\u0102\u00A9", "é")
                .replace("\u0102\u00AD", "í");
    }

    private String repairFinalMojibake(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return value
                .replace("\u00C4\u015A", "\u010C")
                .replace("\u00C4\u0164", "\u010D")
                .replace("\u00C4\u203A", "\u011B")
                .replace("\u00C4\u010F", "\u010F")
                .replace("\u00C4\u013E", "\u017E")
                .replace("\u0139\u00A0", "\u0160")
                .replace("\u0139?", "\u0158")
                .replace("\u0139\uFFFD", "\u0158")
                .replace("\u0139\u0098", "\u0158")
                .replace("\u0139\u02DC", "\u0158")
                .replace("\u0139\u2122", "\u0159")
                .replace("\u0139\u013E", "\u017E")
                .replace("\u0139\u02DD", "\u017D")
                .replace("\u0139\u02C7", "\u0161")
                .replace("\u0139\u017B", "\u016F")
                .replace("\u0139\u0088", "\u0148")
                .replace("\u0102\u0081", "\u00C1")
                .replace("\u0102\u2030", "\u00C9")
                .replace("\u0102\u00AB", "\u00EB")
                .replace("\u00C3\u00AB", "\u00EB")
                .replace("\u0102\u00A9", "\u00E9")
                .replace("\u0102\u00AD", "\u00ED")
                .replace("\u0102\u00BD", "\u00FD")
                .replace("\u0102\u02C7", "\u00E1")
                .replace("\u0102\u02DD", "\u00FD")
                .replace("\u0102\u0164", "\u00CD")
                .replace("\u0102\u0165", "\u00DD")
                .replace("\u0102\u0161", "\u00DA")
                .replace("\u0102\u017A", "\u00FA")
                .replace("\u0102\u00A4", "\u00E4")
                .replace("\u0102\u00B6", "\u00F6")
                .replace("\u0102\u013D", "\u00FC")
                .replace("\u0102\u2014", "\u00D7")
                .replace("\u0102\u201E", "\u00C4");
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

            if (codePoint == 0x02D8) {
                out.write(0xA2);
                offset += Character.charCount(codePoint);
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
                    || codePoint == '\uFFFD') {
                score++;
            }
            offset += Character.charCount(codePoint);
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

    private String safeBlank(String value) {
        return value == null ? "" : value;
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

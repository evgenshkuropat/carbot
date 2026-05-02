package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ToyotaProvereneVozyParser implements CarSourceParser {

    private static final Logger log = LoggerFactory.getLogger(ToyotaProvereneVozyParser.class);

    private static final String BASE_URL = "https://proverenevozy.toyota.cz";
    private static final int REQUEST_TIMEOUT_MS = 20_000;
    private static final int MAX_PAGES = 10;
    private static final int PER_PAGE = 24;

    @Override
    public String getSourceName() {
        return "TOYOTA_PROVERENE";
    }

    @Override
    public List<CarDto> fetchCars() {
        List<CarDto> cars = new ArrayList<>();
        Set<String> urls = new LinkedHashSet<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            String pageUrl = BASE_URL + "/nabidky?na-strone=" + PER_PAGE + "&strona=" + page;

            try {
                Document doc = Jsoup.connect(pageUrl)
                        .userAgent("Mozilla/5.0")
                        .timeout(REQUEST_TIMEOUT_MS)
                        .get();

                Elements links = doc.select("a[href^=/nabidka/], a[href*=/nabidka/]");
                int before = urls.size();

                for (Element link : links) {
                    String href = link.absUrl("href");
                    if (href != null && href.contains("/nabidka/")) {
                        urls.add(href);
                    }
                }

                int added = urls.size() - before;
                log.info("TOYOTA_PROVERENE page={} url={} links_added={}", page, pageUrl, added);

                if (added == 0) {
                    break;
                }

            } catch (Exception e) {
                log.warn("TOYOTA_PROVERENE page failed url={} error={}", pageUrl, e.getMessage());
                break;
            }
        }

        for (String url : urls) {
            try {
                CarDto car = parseDetail(url);
                if (car != null) {
                    cars.add(car);
                }
            } catch (Exception e) {
                log.warn("TOYOTA_PROVERENE SKIP url={} reason=parse_error error={}", url, e.getMessage());
            }
        }

        log.info("TOYOTA_PROVERENE parsed {} cars", cars.size());
        return cars;
    }

    private CarDto parseDetail(String url) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(REQUEST_TIMEOUT_MS)
                .get();

        String fullText = normalizeText(doc.text());

        String brandModel = textOf(doc.selectFirst("h1"));
        String subtitle = "";

        Element h1 = doc.selectFirst("h1");
        if (h1 != null) {
            Element next = h1.nextElementSibling();
            if (next != null) {
                subtitle = normalizeText(next.text());
            }
        }

        String title = normalizeText(brandModel + " " + subtitle);
        if (title.isBlank()) {
            return null;
        }

        Integer priceValue = extractPrice(fullText);
        if (priceValue == null) {
            log.warn("TOYOTA_PROVERENE SKIP url={} reason=missing_price title={}", url, title);
            return null;
        }

        Integer year = extractIntAfterLabel(fullText, "Rok výroby");
        Integer mileage = extractMileage(fullText);

        String fuelType = mapFuel(extractValueAfterLabel(fullText, "Typ paliva"));
        String transmission = mapTransmission(extractValueAfterLabel(fullText, "Převodovka"));
        String carType = mapCarType(extractValueAfterLabel(fullText, "Typ karoserie"));
        String location = extractDealerLocation(fullText);
        String brand = extractBrand(title);
        String imageUrl = extractImageUrl(doc);

        CarDto car = new CarDto();
        car.setSource(getSourceName());
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

        log.info("TOYOTA_PROVERENE CAR title='{}' price={} location={} year={} mileage={} fuelType={} transmission={} carType={} brand={} url={}",
                safe(title), priceValue, safe(location), year, mileage, safe(fuelType),
                safe(transmission), safe(carType), safe(brand), url);

        return car;
    }

    private Integer extractPrice(String text) {
        Matcher matcher = Pattern.compile("(?i)\\b([0-9]{2,3}(?:\\s[0-9]{3})+)\\s*Kč\\b").matcher(text);
        Integer best = null;

        while (matcher.find()) {
            Integer value = parseNumber(matcher.group(1));
            if (value != null && value >= 30_000 && value <= 10_000_000) {
                if (best == null || value > best) {
                    best = value;
                }
            }
        }

        return best;
    }

    private Integer extractMileage(String text) {
        Matcher matcher = Pattern.compile("(?i)Najeto\\s+([0-9\\s\\.]+)\\s*km").matcher(text);
        if (matcher.find()) {
            Integer value = parseNumber(matcher.group(1));
            if (value != null && value >= 0 && value <= 1_500_000) {
                return value;
            }
        }
        return null;
    }

    private Integer extractIntAfterLabel(String text, String label) {
        String value = extractValueAfterLabel(text, label);
        return parseNumber(value);
    }

    private String extractValueAfterLabel(String text, String label) {
        Matcher matcher = Pattern.compile(
                Pattern.quote(label) + "\\s+([^\\n]+?)(?=\\s+(Rok výroby|Datum první registrace|Najeto|Typ karoserie|Objem motoru|Výkon|Typ paliva|Převodovka|Barva karoserie|VIN|Země původu)\\b|$)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);

        if (matcher.find()) {
            return normalizeText(matcher.group(1));
        }

        return null;
    }

    private String extractDealerLocation(String text) {
        Matcher matcher = Pattern.compile("(?i)Informace o prodejci\\s+(.+?)\\s+(\\d{3}\\s?\\d{2})\\s+([^\\n]+)").matcher(text);
        if (matcher.find()) {
            return normalizeText(matcher.group(3));
        }

        if (text.contains("Praha")) return "Praha";
        if (text.contains("Brno")) return "Brno";
        if (text.contains("Ostrava")) return "Ostrava";
        if (text.contains("Plzeň")) return "Plzeň";
        if (text.contains("Olomouc")) return "Olomouc";

        return null;
    }

    private String extractImageUrl(Document doc) {
        Element img = doc.selectFirst("meta[property=og:image], img[src*=/media/], img[src*=/uploads/], img[src*=/images/]");
        if (img == null) {
            return null;
        }

        String src = firstNonBlank(
                img.hasAttr("content") ? img.absUrl("content") : null,
                img.hasAttr("src") ? img.absUrl("src") : null
        );

        return src == null || src.isBlank() ? null : src;
    }

    private String mapFuel(String value) {
        if (value == null) return null;
        String v = value.toLowerCase(Locale.ROOT);

        if (v.contains("plug")) return "PLUGIN_HYBRID";
        if (v.contains("hybrid")) return "HYBRID";
        if (v.contains("elektro")) return "ELECTRIC";
        if (v.contains("diesel") || v.contains("nafta")) return "DIESEL";
        if (v.contains("lpg")) return "LPG";
        if (v.contains("cng")) return "CNG";
        if (v.contains("benz")) return "PETROL";

        return null;
    }

    private String mapTransmission(String value) {
        if (value == null) return null;
        String v = value.toLowerCase(Locale.ROOT);

        if (v.contains("automat")) return "AUTOMATIC";
        if (v.contains("manu")) return "MANUAL";

        return null;
    }

    private String mapCarType(String value) {
        if (value == null) return null;
        String v = value.toLowerCase(Locale.ROOT);

        if (v.contains("suv")) return "SUV";
        if (v.contains("kombi")) return "WAGON";
        if (v.contains("sedan")) return "SEDAN";
        if (v.contains("hatch")) return "HATCHBACK";
        if (v.contains("mpv")) return "MINIVAN";
        if (v.contains("sport")) return "COUPE";
        if (v.contains("užit")) return "VAN";

        return null;
    }

    private String extractBrand(String title) {
        String s = " " + normalizeText(title).toLowerCase(Locale.ROOT) + " ";

        if (s.contains(" toyota ")) return "TOYOTA";
        if (s.contains(" lexus ")) return "LEXUS";
        if (s.contains(" škoda ") || s.contains(" skoda ")) return "SKODA";
        if (s.contains(" volkswagen ") || s.contains(" vw ")) return "VOLKSWAGEN";
        if (s.contains(" audi ")) return "AUDI";
        if (s.contains(" bmw ")) return "BMW";
        if (s.contains(" mercedes ")) return "MERCEDES";
        if (s.contains(" hyundai ")) return "HYUNDAI";
        if (s.contains(" kia ")) return "KIA";
        if (s.contains(" opel ")) return "OPEL";
        if (s.contains(" ford ")) return "FORD";
        if (s.contains(" peugeot ")) return "PEUGEOT";
        if (s.contains(" renault ")) return "RENAULT";
        if (s.contains(" suzuki ")) return "SUZUKI";
        if (s.contains(" dacia ")) return "DACIA";

        return null;
    }

    private Integer parseNumber(String raw) {
        if (raw == null) return null;

        String cleaned = raw.replaceAll("[^0-9]", "");
        if (cleaned.isBlank()) return null;

        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatPrice(Integer priceValue) {
        if (priceValue == null) return "";
        return String.format(Locale.US, "%,d Kč", priceValue).replace(",", " ");
    }

    private String textOf(Element element) {
        return element == null ? "" : normalizeText(element.text());
    }

    private String normalizeText(String value) {
        if (value == null) return "";
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SautoParser extends AbstractJsoupParser implements CarSourceParser {

    private static final Logger log = LoggerFactory.getLogger(SautoParser.class);

    private static final String URL =
            "https://www.sauto.cz/inzerce/osobni?razeni=od-nejlevnejsich";

    @Override
    public String getSourceName() {
        return "SAUTO";
    }

    @Override
    public List<CarDto> fetchCars() {
        List<CarDto> cars = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        try {
            Document doc = loadDocument(URL);

            log.info("SAUTO title={}", doc.title());

            Elements links = doc.select("a[href*=/osobni/detail/]");
            log.info("SAUTO detail links found={}", links.size());

            int debugCount = 0;

            for (Element link : links) {
                String detailUrl = link.absUrl("href").trim();
                if (detailUrl.isBlank() || !seenUrls.add(detailUrl)) {
                    continue;
                }

                Element container = findContainer(link);
                if (container == null) {
                    continue;
                }

                String title = extractTitle(link, container);
                if (title.isBlank()) {
                    continue;
                }

                String fullText = normalizeText(container.text());
                String price = extractPrice(fullText);
                String location = extractLocation(fullText);
                String imageUrl = extractImageUrl(container, link);

                if (price.isBlank()) {
                    continue;
                }

                if (isBadSautoListing(title, price)) {
                    continue;
                }

                CarDto dto = new CarDto(
                        getSourceName(),
                        title,
                        price,
                        location,
                        detailUrl,
                        imageUrl
                );

                dto.setBrand(extractBrand(title));
                dto.setYear(extractYear(fullText));
                dto.setMileage(extractMileage(fullText));
                dto.setTransmission(extractTransmission(fullText));

                if (debugCount < 5) {
                    log.debug(
                            "SAUTO title={} url={} price={} location={} image={} brand={} year={} mileage={} transmission={} text={}",
                            title,
                            detailUrl,
                            price,
                            location,
                            imageUrl,
                            dto.getBrand(),
                            dto.getYear(),
                            dto.getMileage(),
                            dto.getTransmission(),
                            preview(fullText, 400)
                    );
                    debugCount++;
                }

                cars.add(dto);

                if (cars.size() >= 20) {
                    break;
                }
            }

            log.info("SAUTO parsed {} cars", cars.size());

        } catch (Exception e) {
            log.error("SAUTO parser failed", e);
        }

        return cars;
    }

    private Element findContainer(Element link) {
        Element current = link;

        for (int i = 0; i < 10 && current != null; i++) {
            current = current.parent();
            if (current == null) {
                return null;
            }

            String text = normalizeText(current.text());

            if (!text.isBlank()
                    && text.contains("Kč")
                    && text.length() >= 30
                    && text.length() <= 2500) {
                return current;
            }
        }

        return link.parent();
    }

    private String extractTitle(Element link, Element container) {
        String title = normalizeText(link.text());
        if (!title.isBlank()) {
            return title;
        }

        Element titleElement = container.selectFirst("h1, h2, h3, h4, [class*=title], [class*=name]");
        if (titleElement != null) {
            return normalizeText(titleElement.text());
        }

        return "";
    }

    private String extractPrice(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        Pattern[] patterns = new Pattern[]{
                Pattern.compile("\\d{1,3}(?:[\\s\\u00A0]\\d{3})*\\s*Kč"),
                Pattern.compile("\\d{1,3}(?:[\\s\\u00A0]\\d{3})*Kč"),
                Pattern.compile("\\d{1,3}(?:[\\s\\u00A0]\\d{3})*,-")
        };

        String best = "";
        long bestValue = -1;

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                String whole = matcher.group().trim();
                String digits = whole.replaceAll("[^\\d]", "");

                if (digits.isBlank()) {
                    continue;
                }

                long value = Long.parseLong(digits);

                if (value > bestValue) {
                    bestValue = value;
                    best = whole;
                }
            }
        }

        return best;
    }

    private String extractLocation(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String[] known = {
                "Praha",
                "Hlavní město Praha",
                "Brno",
                "Brno-město",
                "Ostrava",
                "Plzeň",
                "Liberec",
                "Olomouc",
                "Pardubice",
                "Hradec Králové",
                "České Budějovice",
                "Ústí nad Labem",
                "Jihlava",
                "Zlín",
                "Kladno",
                "Jindřichův Hradec",
                "Mladá Boleslav",
                "Kolín",
                "Domažlice",
                "Klatovy",
                "Benešov",
                "Beroun",
                "Tábor",
                "Teplice",
                "Most",
                "Chomutov",
                "Karlovy Vary",
                "Frýdek-Místek",
                "Opava",
                "Havířov",
                "Děčín",
                "Louny",
                "Cheb",
                "Trutnov",
                "Náchod",
                "Prostějov",
                "Přerov",
                "Uherské Hradiště",
                "Třebíč",
                "Žďár nad Sázavou"
        };

        for (String city : known) {
            if (text.contains(city)) {
                return city;
            }
        }

        Matcher districtMatcher = Pattern.compile("okr\\.\\s*([A-ZÁ-Ža-zá-ž\\- ]{2,50})").matcher(text);
        if (districtMatcher.find()) {
            return "okr. " + districtMatcher.group(1).trim();
        }

        return "";
    }

    private String extractImageUrl(Element container, Element link) {
        if (container != null) {
            String fromContainer = extractImageFromElement(container);
            if (!fromContainer.isBlank()) {
                return fromContainer;
            }
        }

        Element current = link;
        for (int i = 0; i < 6 && current != null; i++) {
            String img = extractImageFromElement(current);
            if (!img.isBlank()) {
                return img;
            }
            current = current.parent();
        }

        return "";
    }

    private String extractImageFromElement(Element element) {
        if (element == null) {
            return "";
        }

        Element img = element.selectFirst("img");
        if (img == null) {
            return "";
        }

        if (img.hasAttr("src")) {
            String src = img.absUrl("src");
            if (!src.isBlank()) {
                return src;
            }
        }

        if (img.hasAttr("data-src")) {
            String src = img.absUrl("data-src");
            if (!src.isBlank()) {
                return src;
            }
        }

        if (img.hasAttr("srcset")) {
            String srcset = img.attr("srcset");
            if (!srcset.isBlank()) {
                String first = srcset.split(",")[0].trim().split("\\s+")[0];
                if (first.startsWith("http")) {
                    return first;
                }
            }
        }

        return "";
    }

    private boolean isBadSautoListing(String title, String price) {
        String t = title.toLowerCase();

        String[] banned = {
                "na nd",
                "na díly",
                "na dily",
                "závada",
                "zadřený motor",
                "zadreny motor"
        };

        for (String word : banned) {
            if (t.contains(word)) {
                return true;
            }
        }

        if (price == null || price.isBlank()) {
            return true;
        }

        String digits = price.replaceAll("[^\\d]", "");
        if (digits.isBlank()) {
            return true;
        }

        long value = Long.parseLong(digits);
        return value < 10000;
    }

    private String extractBrand(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String upper = text.toUpperCase();

        if (upper.contains("ŠKODA") || upper.contains("SKODA")) return "SKODA";
        if (upper.contains("VOLKSWAGEN")) return "VOLKSWAGEN";
        if (upper.contains("AUDI")) return "AUDI";
        if (upper.contains("BMW")) return "BMW";
        if (upper.contains("MERCEDES")) return "MERCEDES";
        if (upper.contains("TOYOTA")) return "TOYOTA";
        if (upper.contains("FORD")) return "FORD";
        if (upper.contains("RENAULT")) return "RENAULT";

        return null;
    }

    private Integer extractYear(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(text);

        while (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));
            if (year >= 1990 && year <= Year.now().getValue()) {
                return year;
            }
        }

        return null;
    }

    private Integer extractMileage(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Pattern[] patterns = new Pattern[]{
                Pattern.compile("(\\d{1,3}(?:[\\s\\u00A0]\\d{3})+)\\s*km", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(\\d{2,6})\\s*km", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                String digits = matcher.group(1).replaceAll("[^\\d]", "");
                try {
                    return Integer.parseInt(digits);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return null;
    }

    private String extractTransmission(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String lower = text.toLowerCase();

        if (lower.contains("automat") || lower.contains("automatic")) {
            return "AUTOMATIC";
        }

        if (lower.contains("manuál") || lower.contains("manual") || lower.contains("manuální")) {
            return "MANUAL";
        }

        return null;
    }
}
package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BazosParser extends AbstractJsoupParser implements CarSourceParser {

    private static final String BASE_URL = "https://auto.bazos.cz";
    private static final String URL = "https://auto.bazos.cz/inzeraty/osobni-auta/";

    @Override
    public String getSourceName() {
        return "BAZOS";
    }

    @Override
    public List<CarDto> fetchCars() {

        List<CarDto> cars = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        try {

            Document doc = loadDocument(URL);

            Elements links = doc.select("a[href*='/inzerat/']");

            int debugCount = 0;

            for (Element link : links) {

                String listTitle = normalizeText(link.text());
                String detailUrl = toAbsoluteUrl(link.attr("href"));

                if (listTitle.isBlank() || detailUrl.isBlank()) {
                    continue;
                }

                if (!seenUrls.add(detailUrl)) {
                    continue;
                }

                if (isAccessoryOrPart(listTitle) || isBadTitle(listTitle)) {
                    continue;
                }

                String imageUrl = extractImage(link);

                DetailData detail = fetchDetail(detailUrl);

                if (detail.price().isBlank()) {
                    continue;
                }

                String finalTitle = !detail.title().isBlank()
                        ? detail.title()
                        : listTitle;

                if (isBadTitle(finalTitle)) {
                    continue;
                }

                CarDto dto = new CarDto(
                        getSourceName(),
                        finalTitle,
                        detail.price(),
                        detail.location(),
                        detailUrl,
                        imageUrl
                );

                dto.setBrand(extractBrand(finalTitle));
                dto.setYear(extractYear(detail.fullText()));
                dto.setMileage(extractMileage(detail.fullText()));
                dto.setTransmission(extractTransmission(detail.fullText()));

                if (debugCount < 5) {

                    System.out.println("---- BAZOS DEBUG ----");
                    System.out.println("TITLE = " + finalTitle);
                    System.out.println("PRICE = " + detail.price());
                    System.out.println("LOCATION = " + detail.location());
                    System.out.println("YEAR = " + dto.getYear());
                    System.out.println("MILEAGE = " + dto.getMileage());
                    System.out.println("TRANSMISSION = " + dto.getTransmission());
                    System.out.println("URL = " + detailUrl);

                    debugCount++;
                }

                cars.add(dto);

                if (cars.size() >= 15) {
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return cars;
    }

    private DetailData fetchDetail(String detailUrl) {

        try {

            Document doc = loadDocument(detailUrl);

            String title = extractDetailTitle(doc);
            String text = normalizeText(doc.text());

            String price = extractPrice(text);
            String location = extractLocation(text);

            return new DetailData(title, price, location, text);

        } catch (Exception e) {

            return new DetailData("", "", "", "");
        }
    }

    private String extractDetailTitle(Document doc) {

        Element h1 = doc.selectFirst("h1");

        if (h1 != null && !h1.text().isBlank()) {
            return normalizeText(h1.text());
        }

        String title = doc.title();

        if (title != null && !title.isBlank()) {

            int dash = title.indexOf(" - ");

            if (dash > 0) {
                return title.substring(0, dash).trim();
            }

            return title.trim();
        }

        return "";
    }

    private String extractPrice(String text) {

        Matcher m = Pattern.compile("Cena:\\s*(\\d{1,3}(?:[\\s\\u00A0]\\d{3})+\\s*Kč)")
                .matcher(text);

        if (m.find()) {
            return m.group(1).trim();
        }

        return "";
    }

    private String extractLocation(String text) {

        Matcher villageMatcher =
                Pattern.compile("obec\\s+([A-ZÁ-Ža-zá-ž\\- ]{2,60})")
                        .matcher(text);

        if (villageMatcher.find()) {
            return villageMatcher.group(1).trim();
        }

        Matcher districtMatcher =
                Pattern.compile("okr\\.\\s*([A-ZÁ-Ža-zá-ž\\- ]{2,60})")
                        .matcher(text);

        if (districtMatcher.find()) {
            return "okr. " + districtMatcher.group(1).trim();
        }

        return "";
    }

    private String extractBrand(String text) {

        if (text == null) {
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

        Matcher matcher =
                Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b")
                        .matcher(text);

        while (matcher.find()) {

            int year = Integer.parseInt(matcher.group(1));

            if (year >= 1990 && year <= Year.now().getValue()) {
                return year;
            }
        }

        return null;
    }

    private Integer extractMileage(String text) {

        Pattern[] patterns = new Pattern[]{

                Pattern.compile("(\\d{1,3}(?:[\\s\\u00A0]\\d{3})+)\\s*km",
                        Pattern.CASE_INSENSITIVE),

                Pattern.compile("(\\d{2,6})\\s*km",
                        Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : patterns) {

            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {

                String digits =
                        matcher.group(1).replaceAll("[^\\d]", "");

                try {

                    return Integer.parseInt(digits);

                } catch (NumberFormatException ignored) {
                }
            }
        }

        return null;
    }

    private String extractTransmission(String text) {

        String lower = text.toLowerCase();

        if (lower.contains("automat")) {
            return "AUTOMATIC";
        }

        if (lower.contains("manuál")
                || lower.contains("manual")
                || lower.contains("manuální")) {

            return "MANUAL";
        }

        return null;
    }

    private boolean isAccessoryOrPart(String title) {

        String t = title.toLowerCase();

        return t.contains("kola")
                || t.contains("disky")
                || t.contains("pneu")
                || t.contains("motor")
                || t.contains("převodovka")
                || t.contains("světla")
                || t.contains("nárazník");
    }

    private boolean isBadTitle(String title) {

        String t = title.toLowerCase();

        return t.contains("hledám auto")
                || t.contains("koupím auto")
                || t.contains("pronájem auta");
    }

    private String extractImage(Element link) {

        Element current = link;

        for (int i = 0; i < 6 && current != null; i++) {

            Element img = current.selectFirst("img");

            if (img != null && img.hasAttr("src")) {

                String src = toAbsoluteUrl(img.attr("src"));

                if (!src.isBlank()) {
                    return src;
                }
            }

            current = current.parent();
        }

        return "";
    }

    private String toAbsoluteUrl(String url) {

        if (url.startsWith("http")) {
            return url;
        }

        if (url.startsWith("//")) {
            return "https:" + url;
        }

        if (url.startsWith("/")) {
            return BASE_URL + url;
        }

        return BASE_URL + "/" + url;
    }

    private record DetailData(
            String title,
            String price,
            String location,
            String fullText
    ) {
    }
}
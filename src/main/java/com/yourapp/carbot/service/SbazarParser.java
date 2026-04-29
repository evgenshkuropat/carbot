package com.yourapp.carbot.service;

import com.yourapp.carbot.service.dto.CarDto;
import jakarta.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SbazarParser implements CarSourceParser {

    private static final Logger log = LoggerFactory.getLogger(SbazarParser.class);

    private static final String BASE_URL = "https://www.sbazar.cz/170-osobni-auta";
    private static final int REQUEST_TIMEOUT_MS = 20_000;

    @PostConstruct
    public void init() {
        log.warn("SBAZAR parser bean initialized");
    }

    @Override
    public String getSourceName() {
        return "SBAZAR";
    }

    @Override
    public List<CarDto> fetchCars() {
        log.warn("SBAZAR link debug mode enabled. No cars will be saved or notified.");

        try {
            Document doc = Jsoup.connect(BASE_URL)
                    .userAgent("Mozilla/5.0")
                    .timeout(REQUEST_TIMEOUT_MS)
                    .get();

            Set<String> links = extractDetailUrls(doc);

            log.warn("SBAZAR DEBUG collected links count={}", links.size());

            int i = 0;
            for (String link : links) {
                if (i >= 30) {
                    break;
                }

                log.warn("SBAZAR DEBUG link {}: {}", i + 1, link);
                i++;
            }

        } catch (Exception e) {
            log.warn("SBAZAR DEBUG failed: {}", e.getMessage());
        }

        return List.of();
    }

    private Set<String> extractDetailUrls(Document doc) {
        Set<String> urls = new LinkedHashSet<>();

        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String href = link.absUrl("href");

            if (href == null || href.isBlank()) {
                continue;
            }

            href = stripUrlParams(href);

            if (!href.contains("sbazar.cz")) {
                continue;
            }

            log.warn("SBAZAR DEBUG raw href: {}", href);

            if (href.contains("/detail/") || href.matches(".*/[0-9]+-[^/?#]+.*")) {
                if (!href.contains("/170-osobni-auta")) {
                    urls.add(href);
                }
            }
        }

        return urls;
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
}
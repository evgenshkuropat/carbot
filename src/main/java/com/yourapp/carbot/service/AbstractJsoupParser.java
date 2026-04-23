package com.yourapp.carbot.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public abstract class AbstractJsoupParser {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/123.0.0.0 Safari/537.36";

    protected Document loadDocument(String url) throws IOException {
        Connection connection = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .referrer("https://www.google.com/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "cs-CZ,cs;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .timeout(30000)
                .maxBodySize(0)
                .followRedirects(true)
                .ignoreContentType(false)
                .ignoreHttpErrors(true);

        Document doc = connection.get();

        if (doc == null) {
            throw new IOException("Document is null for url: " + url);
        }

        String title = normalizeText(doc.title());
        String body = normalizeText(doc.text());

        if (body.isBlank()) {
            throw new IOException("Empty body for url: " + url);
        }

        if (title.toLowerCase().contains("403")
                || title.toLowerCase().contains("access denied")
                || body.toLowerCase().contains("access denied")
                || body.toLowerCase().contains("forbidden")) {
            throw new IOException("Access blocked for url: " + url + ", title=" + title);
        }

        return doc;
    }

    protected String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    protected String preview(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + "...";
    }
}
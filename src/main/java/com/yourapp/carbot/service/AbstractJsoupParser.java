package com.yourapp.carbot.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public abstract class AbstractJsoupParser {

    protected Document loadDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .header("Accept-Language", "cs-CZ,cs;q=0.9,en;q=0.8")
                .timeout(15000)
                .get();
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
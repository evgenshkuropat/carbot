package com.yourapp.carbot.service;

import com.yourapp.carbot.entity.CarEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class CarCardFormatter {

    public String formatTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) return "-";
        String cleaned = rawTitle.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= 55) return cleaned;

        int splitIndex = cleaned.lastIndexOf(" ", 55);
        if (splitIndex < 30) splitIndex = 55;
        String first = cleaned.substring(0, splitIndex).trim();
        String second = cleaned.substring(splitIndex).trim();
        if (second.length() > 45) second = second.substring(0, 42).trim() + "...";
        return first + "\n" + second;
    }

    public String formatPrice(CarEntity car) {
        if (car == null) return null;
        if (car.getPrice() != null && !car.getPrice().isBlank()) return car.getPrice().trim();
        if (car.getPriceValue() != null && car.getPriceValue() > 0) {
            return String.format("%,d Kč", car.getPriceValue()).replace(",", " ");
        }
        return null;
    }

    public String formatSource(String source) {
        if (source == null || source.isBlank()) return null;
        String normalized = source.trim().toUpperCase();
        if (normalized.contains("SAUTO")) return "Sauto.cz";
        if (normalized.contains("BAZOS")) return "Bazoš.cz";
        if (normalized.contains("SBAZAR")) return "Sbazar.cz";
        if (normalized.contains("TIPCARS") || normalized.contains("TIP_CARS")) return "TipCars.cz";
        if (normalized.contains("TOYOTA_PROVERENE")) return "Toyota prověřené vozy";
        if (normalized.contains("AUTOESA")) return "Auto ESA";
        if (normalized.contains("USER")) return "AutoCZ users";
        return source.trim();
    }

    public String formatFreshness(String lang, LocalDateTime createdAt) {
        if (createdAt == null) return null;
        long minutes = Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return localized(lang, "только что", "щойно", "právě teď", "just now");
        if (minutes < 60) return switch (lang) {
            case "ru" -> minutes + " мин назад";
            case "uk" -> minutes + " хв тому";
            case "cs" -> "před " + minutes + " min";
            default -> minutes + " min ago";
        };
        long hours = minutes / 60;
        if (hours < 24) return switch (lang) {
            case "ru" -> hours + " ч назад";
            case "uk" -> hours + " год тому";
            case "cs" -> "před " + hours + " h";
            default -> hours + " h ago";
        };
        long days = hours / 24;
        if (days < 30) return switch (lang) {
            case "ru" -> days + " дн назад";
            case "uk" -> days + " дн тому";
            case "cs" -> "před " + days + " d";
            default -> days + " d ago";
        };
        long months = days / 30;
        if (months < 12) return switch (lang) {
            case "ru" -> months + " мес назад";
            case "uk" -> months + " міс тому";
            case "cs" -> "před " + months + " měs.";
            default -> months + " mo ago";
        };
        long years = days / 365;
        return switch (lang) {
            case "ru" -> years + " г назад";
            case "uk" -> years + " р тому";
            case "cs" -> "před " + years + " r.";
            default -> years + " y ago";
        };
    }

    private String localized(String lang, String russian, String ukrainian, String czech, String english) {
        return switch (lang) {
            case "ru" -> russian;
            case "uk" -> ukrainian;
            case "cs" -> czech;
            default -> english;
        };
    }
}

package com.yourapp.carbot.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class FilterValueUtils {

    private static final String ANY = "ANY";

    private FilterValueUtils() {
    }

    public static boolean isAny(String value) {
        return value == null || value.isBlank() || ANY.equalsIgnoreCase(value.trim());
    }

    public static Set<String> toSet(String value) {
        if (value == null || value.isBlank()) {
            return new LinkedHashSet<>();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String toStoredValue(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return ANY;
        }

        Set<String> cleaned = values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (cleaned.isEmpty() || cleaned.contains(ANY)) {
            return ANY;
        }

        return String.join(",", cleaned);
    }

    public static String toggleMultiValue(String currentValue, String clickedValue) {
        String clicked = normalize(clickedValue);

        if (clicked == null) {
            return isAny(currentValue) ? ANY : currentValue;
        }

        if (ANY.equals(clicked)) {
            return ANY;
        }

        Set<String> values = toSet(currentValue);
        values.remove(ANY);

        if (values.contains(clicked)) {
            values.remove(clicked);
        } else {
            values.add(clicked);
        }

        return toStoredValue(values);
    }

    public static boolean containsValue(String currentValue, String wantedValue) {
        String wanted = normalize(wantedValue);

        if (wanted == null) {
            return false;
        }

        if (isAny(currentValue)) {
            return ANY.equals(wanted);
        }

        return toSet(currentValue).contains(wanted);
    }

    public static String normalizeStoredValue(String value) {
        if (isAny(value)) {
            return ANY;
        }

        return toStoredValue(toSet(value));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}
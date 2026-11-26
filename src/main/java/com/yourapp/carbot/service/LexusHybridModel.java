package com.yourapp.carbot.service;

import java.util.regex.Pattern;

final class LexusHybridModel {
    private static final Pattern GS_300H = Pattern.compile("\\blexus[\\s-]+gs[\\s-]*300h\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern UX_250H = Pattern.compile("\\blexus[\\s-]+ux[\\s-]*250h\\b", Pattern.CASE_INSENSITIVE);

    private LexusHybridModel() {
    }

    static String bodyType(String title) {
        if (title == null) return null;
        if (GS_300H.matcher(title).find()) return "SEDAN";
        if (UX_250H.matcher(title).find()) return "SUV";
        return null;
    }
}

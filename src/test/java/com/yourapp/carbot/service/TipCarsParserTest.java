package com.yourapp.carbot.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class TipCarsParserTest {

    private final TipCarsParser parser = new TipCarsParser();

    @Test
    void resolvesBodyTypesFromTipCarsTitleAndUrl() throws Exception {
        assertThat(extractCarType(
                "Hyundai i30 START PLUS 1.5i 80kW CZ DPH",
                "",
                "https://www.tipcars.com/hyundai-i30/kombi/benzin/hyundai-i30-start-plus.html"))
                .isEqualTo("WAGON");

        assertThat(extractCarType(
                "Skoda Fabia Combi 1.0 TSI Ambition",
                "",
                "https://www.tipcars.com/skoda-fabia/kombi/benzin/skoda-fabia-combi.html"))
                .isEqualTo("WAGON");

        assertThat(extractCarType(
                "Renault Captur techno mildhybrid 140 EDC MY25",
                "",
                "https://www.tipcars.com/renault-captur/hatchback/hybridni-benzin/renault-captur-techno.html"))
                .isEqualTo("SUV");
    }

    private String extractCarType(String title, String text, String url) throws Exception {
        Method method = TipCarsParser.class.getDeclaredMethod("extractCarType", String.class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, title, text, url);
    }
}
